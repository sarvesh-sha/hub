/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.persistence.criteria.Root;

import com.google.common.collect.Lists;
import com.optio3.collection.MapWithSoftValues;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.text.AnsiParser;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.BiConsumerWithException;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

public final class LogHandler<H extends LogHandler.ILogHost<E>, E extends CommonLogRecord> implements AutoCloseable
{
    public interface ILogHost<E extends CommonLogRecord>
    {
        byte[] getLogRanges();

        void setLogRanges(byte[] payload,
                          ZonedDateTime lastOutput,
                          int lastOffset);

        void refineLogQuery(JoinHelper<?, E> jh);

        E allocateNewLogInstance();
    }

    static class CachedLogRangeAndData
    {
        private static final MapWithSoftValues<String, CachedLogRangeAndData> s_cachedBlocks = new MapWithSoftValues<>();

        private ZonedDateTime  m_timestampEnd;
        private List<LogEntry> m_items;

        public static void set(SummaryLogRange range,
                               List<LogEntry> items)
        {
            CachedLogRangeAndData cache = new CachedLogRangeAndData();
            cache.m_timestampEnd = range.timestampEnd;
            cache.m_items        = Lists.newArrayList(items);

            synchronized (s_cachedBlocks)
            {
                s_cachedBlocks.put(range.sysId, cache);
            }
        }

        public static List<LogEntry> get(SummaryLogRange range)
        {
            synchronized (s_cachedBlocks)
            {
                CachedLogRangeAndData cache = s_cachedBlocks.get(range.sysId);
                if (cache != null)
                {
                    if (TimeUtils.compare(cache.m_timestampEnd, range.timestampEnd) == 0)
                    {
                        LoggerInstance.debugVerbose("Cache Hit for %s", range.sysId);
                        return cache.m_items;
                    }

                    LoggerInstance.debugVerbose("Cache Invalid for %s", range.sysId);
                }
                else
                {
                    LoggerInstance.debugVerbose("Cache Miss for %s", range.sysId);
                }

                return null;
            }
        }
    }

    public static class SummaryLogRange
    {
        public String        sysId;
        public int           sequenceStart;
        public int           sequenceEnd;
        public ZonedDateTime timestampStart;
        public ZonedDateTime timestampEnd;
        public int           numberLines;

        public void update(CommonLogRecord rec,
                           LogBlock block)
        {
            ZonedDateTime timeStart = null;
            ZonedDateTime timeEnd   = null;

            for (LogEntry item : block.items)
            {
                timeStart = TimeUtils.updateIfBefore(timeStart, item.timestamp);
                timeEnd   = TimeUtils.updateIfAfter(timeEnd, item.timestamp);
            }

            sysId          = rec.getSysId();
            sequenceStart  = rec.getSequenceStart();
            sequenceEnd    = rec.getSequenceEnd();
            timestampStart = timeStart;
            timestampEnd   = timeEnd;
            numberLines    = block.items.size();

            CachedLogRangeAndData.set(this, block.items);
        }

        public boolean match(CommonLogRecord rec_log)
        {
            return StringUtils.equals(sysId, rec_log.getSysId());
        }

        public List<LogEntry> fetchLines(RecordHelper<? extends CommonLogRecord> helper) throws
                                                                                         IOException
        {
            List<LogEntry> lines = CachedLogRangeAndData.get(this);
            if (lines == null)
            {
                CommonLogRecord rec_log = helper.get(sysId);

                LogBlock logBlock = rec_log.decodeBlock(ObjectMappers.SkipNulls);
                if (logBlock == null)
                {
                    return Collections.emptyList();
                }

                lines = logBlock.items;

                CachedLogRangeAndData.set(this, lines);
            }

            return lines;
        }
    }

    public static class SummaryLogRanges
    {
        public final List<SummaryLogRange> ranges = Lists.newArrayList();

        public ZonedDateTime lastOutput;
        public int           lastOffset;

        void sanitize()
        {
            ranges.sort(Comparator.comparing(a -> a.sequenceStart));

            SummaryLogRange range = CollectionUtils.lastElement(ranges);

            lastOffset = range != null ? range.sequenceEnd + 1 : 0;
            lastOutput = range != null ? range.timestampEnd : null;
            if (lastOutput != null)
            {
                //
                // MySQL only stores timestamps with microsecond precision.
                // Round up the 'lastOutput' value, or we'll keep seeing the same output line.
                //
                lastOutput = lastOutput.plus(999, ChronoUnit.NANOS)
                                       .truncatedTo(ChronoUnit.MICROS);
            }
        }

        void updateBlock(CommonLogRecord rec_log,
                         LogBlock block)
        {
            for (var range : ranges)
            {
                if (range.match(rec_log))
                {
                    range.update(rec_log, block);
                    return;
                }
            }

            var rangeNew = new SummaryLogRange();
            rangeNew.update(rec_log, block);
            ranges.add(rangeNew);
        }

        boolean shouldCompact()
        {
            int offset = 0;

            for (var range : ranges)
            {
                if (range.sequenceStart != offset)
                {
                    return true;
                }

                offset += range.numberLines;

                if (range.sequenceEnd + 1 != offset)
                {
                    return true;
                }
            }

            return false;
        }
    }

    private static final int c_maxLinesPerBlock = 5000;

    public static final Logger LoggerInstance = new Logger(LogHandler.class, true);

    //--//

    private final   H                m_host;
    protected final RecordHelper<E>  m_helper;
    private final   SummaryLogRanges m_logRanges;

    public LogHandler(RecordLocked<H> lock,
                      Class<E> entityClass)
    {
        this(requireNonNull(lock).getSessionHolder(), lock.get(), entityClass);
    }

    public LogHandler(SessionHolder sessionHolder,
                      H host,
                      Class<E> entityClass)
    {
        m_host   = requireNonNull(host);
        m_helper = sessionHolder.createHelper(entityClass);

        SummaryLogRanges logRanges;

        byte[] logRangesRaw = m_host.getLogRanges();
        if (logRangesRaw != null)
        {
            try
            {
                logRanges = ObjectMappers.deserializeFromGzip(ObjectMappers.SkipNulls, logRangesRaw, SummaryLogRanges.class);
            }
            catch (Throwable t)
            {
                logRanges = null;
            }
        }
        else
        {
            logRanges = null;
        }

        if (logRanges != null)
        {
            m_logRanges = logRanges;
        }
        else
        {
            JoinHelper<E, E> jh = new JoinHelper<>(m_helper, entityClass, entityClass);

            //--//

            jh.cq.select(jh.rootLog);
            m_host.refineLogQuery(jh);

            //--//

            m_logRanges = new SummaryLogRanges();

            for (E rec : jh.list())
            {
                try
                {
                    LogBlock block = rec.decodeBlock(ObjectMappers.SkipNulls);
                    m_logRanges.updateBlock(rec, block);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed to parse log block '%s', due to %s", rec.getSysId(), t);
                }
            }

            flushSummary();
        }
    }

    @Override
    public void close()
    {
        flushSummary();
    }

    private void flushSummary()
    {
        m_logRanges.sanitize();

        if (m_logRanges.shouldCompact())
        {
            try
            {
                int currentOffset = 0;

                for (var it = m_logRanges.ranges.iterator(); it.hasNext(); )
                {
                    var range = it.next();

                    E rec_log = m_helper.get(range.sysId);

                    LogBlock logBlock = rec_log.decodeBlock(ObjectMappers.SkipNulls);

                    int count = logBlock.items.size();
                    if (count == 0)
                    {
                        it.remove();

                        m_helper.delete(rec_log);
                    }
                    else
                    {
                        rec_log.setSequenceStart(currentOffset);
                        currentOffset += count;
                        rec_log.setSequenceEnd(currentOffset - 1);

                        range.update(rec_log, logBlock);
                    }
                }
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to compact log, due to %s", t);
            }

            m_logRanges.sanitize();
        }

        m_host.setLogRanges(ObjectMappers.serializeToGzip(ObjectMappers.SkipNulls, m_logRanges), m_logRanges.lastOutput, m_logRanges.lastOffset);
    }

    //--//

    public LogHolder newLogHolder()
    {
        return new LogHolder(this);
    }

    //--//

    public void saveBlock(List<LogEntry> items) throws
                                                IOException
    {
        requireNonNull(items);

        if (items.size() > 0)
        {
            if (LoggerInstance.isEnabled(Severity.Debug))
            {
                AnsiParser    ansiParser = new AnsiParser();
                StringBuilder sb         = new StringBuilder();

                for (LogEntry en : items)
                {
                    List<Object> parsedLine = ansiParser.parse(en.line);
                    sb.setLength(0);
                    for (Object o : parsedLine)
                    {
                        if (o instanceof String)
                        {
                            sb.append((String) o);
                        }
                    }

                    String text = sb.toString();
                    if (text.endsWith("\n"))
                    {
                        text = text.substring(0, text.length() - 1);
                    }

                    LoggerInstance.debug("%d: %s - %s", en.fd, en.timestamp.toLocalDateTime(), text);
                }
            }

            //--//

            int numItemsNew = items.size();
            int offset;

            E rec_log = findLastChunk();
            if (rec_log == null)
            {
                offset = 0;
            }
            else
            {
                offset = rec_log.getSequenceEnd() + 1;

                int numItemsExisting = offset - rec_log.getSequenceStart();

                if (numItemsExisting + numItemsNew > c_maxLinesPerBlock)
                {
                    rec_log = null; // The concatenation would be too large, create new block.
                }
            }

            LogBlock block;

            boolean createNew = (rec_log == null);
            if (createNew)
            {
                rec_log = m_host.allocateNewLogInstance();

                rec_log.setSequenceStart(offset);

                block = new LogBlock();
            }
            else
            {
                block = rec_log.decodeBlock(ObjectMappers.SkipNulls);
            }

            block.items.addAll(items);

            rec_log.encodeBlock(ObjectMappers.SkipNulls, block);

            offset += numItemsNew;
            rec_log.setSequenceEnd(offset - 1);

            //--//

            if (createNew)
            {
                m_helper.persist(rec_log);
            }

            m_logRanges.updateBlock(rec_log, block);

            m_helper.flushAndEvict(rec_log); // To reduce memory pressure.
        }
    }

    //--//

    public static class JoinHelper<T, E extends CommonLogRecord> extends AbstractSelectHelper<T, E>
    {
        public final Root<E> rootLog;

        JoinHelper(RecordHelper<E> helper,
                   Class<T> clz,
                   Class<E> entityClz)
        {
            super(helper, clz);

            rootLog = cq.from(entityClz);
        }
    }

    //--//

    public List<E> getBatch(List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(m_helper, ids);
    }

    public int getLastOffset()
    {
        return m_logRanges.lastOffset;
    }

    public ZonedDateTime getLastOutput()
    {
        return m_logRanges.lastOutput;
    }

    public ZonedDateTime getFirstOutput()
    {
        SummaryLogRange range = CollectionUtils.firstElement(m_logRanges.ranges);

        return range != null ? range.timestampStart : null;
    }

    public E findLastChunk()
    {
        SummaryLogRange range = CollectionUtils.lastElement(m_logRanges.ranges);

        return range != null ? m_helper.get(range.sysId) : null;
    }

    public void enumerate(Integer fromOffset,
                          Integer toOffset,
                          FunctionWithException<E, Boolean> callback) throws
                                                                      Exception
    {
        enumerateRange(fromOffset, toOffset, (range) ->
        {
            E rec_log = m_helper.get(range.sysId);

            return callback.apply(rec_log);
        });
    }

    private void enumerateRange(Integer fromOffset,
                                Integer toOffset,
                                FunctionWithException<SummaryLogRange, Boolean> callback) throws
                                                                                          Exception
    {
        int startOffset = BoxingUtils.get(fromOffset, 0);
        int endOffset   = BoxingUtils.get(toOffset, Integer.MAX_VALUE);

        for (var range : m_logRanges.ranges)
        {
            boolean includeStart = (range.sequenceStart <= startOffset && startOffset <= range.sequenceEnd) || (range.sequenceStart >= startOffset);
            boolean includeEnd   = (range.sequenceStart <= endOffset && endOffset <= range.sequenceEnd) || (range.sequenceEnd <= endOffset);

            if (includeStart && includeEnd)
            {
                if (!callback.apply(range))
                {
                    break;
                }
            }
        }
    }

    public List<LogRange> filter(LogEntryFilterRequest filters) throws
                                                                Exception
    {
        List<LogRange> ranges = Lists.newArrayList();
        class State
        {
            int      fromOffset;
            int      toOffset;
            int      limit;
            int      got;
            LogRange currentRange;

            void clearRangeIfNeeded(int offset)
            {
                if (currentRange != null && currentRange.endOffset != offset)
                {
                    currentRange = null;
                }
            }
        }

        State st = new State();
        st.fromOffset = BoxingUtils.get(filters.fromOffset, 0);
        st.toOffset   = BoxingUtils.get(filters.toOffset, Integer.MAX_VALUE);
        st.limit      = BoxingUtils.get(filters.limit, Integer.MAX_VALUE);

        enumerateRange(st.fromOffset, st.toOffset, (range) ->
        {
            int offset = range.sequenceStart;
            st.clearRangeIfNeeded(offset);

            for (LogEntry item : range.fetchLines(m_helper))
            {
                if (offset >= st.fromOffset && offset <= st.toOffset && filters.matches(item))
                {
                    if (st.currentRange == null)
                    {
                        st.currentRange             = new LogRange();
                        st.currentRange.startOffset = offset;
                        st.currentRange.endOffset   = offset;
                        ranges.add(st.currentRange);
                    }
                    else
                    {
                        st.currentRange.endOffset++;
                    }
                    st.got++;
                    if (st.got >= st.limit)
                    {
                        return false;
                    }
                }

                st.clearRangeIfNeeded(offset);

                offset++;
            }

            return true;
        });

        return ranges;
    }

    public void extract(Integer fromOffset,
                        Integer toOffset,
                        Integer limit,
                        BiConsumerWithException<LogEntry, Integer> callback) throws
                                                                             Exception
    {
        class State
        {
            int fromOffset;
            int toOffset;
            int limit;
            int got;
        }

        State st = new State();
        st.fromOffset = BoxingUtils.get(fromOffset, 0);
        st.toOffset   = BoxingUtils.get(toOffset, Integer.MAX_VALUE);
        st.limit      = BoxingUtils.get(limit, Integer.MAX_VALUE);

        enumerateRange(fromOffset, toOffset, (range) ->
        {
            int offset = range.sequenceStart;
            for (LogEntry item : range.fetchLines(m_helper))
            {
                if (offset >= st.fromOffset && offset <= st.toOffset)
                {
                    callback.accept(item, offset);
                    st.got++;
                    if (st.got >= st.limit)
                    {
                        return false;
                    }
                }

                offset++;
            }

            return true;
        });
    }

    public void removeDuplicates(List<LogEntry> listToPurge) throws
                                                             Exception
    {
        extract(null, null, null, (line, offset) ->
        {
            while (listToPurge.remove(line))
            {
                // Keep removing duplicates.
            }
        });
    }

    public int delete(ZonedDateTime olderThan) throws
                                               Exception
    {
        //
        // Since we deleted some parts of the log, we need to re-normalize the offsets.
        //
        int deleteCount   = 0;
        int currentOffset = 0;

        for (var it = m_logRanges.ranges.iterator(); it.hasNext(); )
        {
            var range = it.next();

            E rec_log = m_helper.get(range.sysId);

            LogBlock logBlock = rec_log.decodeBlock(ObjectMappers.SkipNulls);

            deleteCount += logBlock.items.size();

            List<LogEntry> remaining = Lists.newArrayList();

            if (olderThan != null)
            {
                for (LogEntry entry : logBlock.items)
                {
                    if (!entry.timestamp.isBefore(olderThan))
                    {
                        remaining.add(entry);
                    }
                }
            }

            int count = remaining.size();

            if (count == 0)
            {
                it.remove();

                m_helper.delete(rec_log);
            }
            else
            {
                logBlock.items = remaining;
                deleteCount -= count;

                rec_log.encodeBlock(ObjectMappers.SkipNulls, logBlock);

                rec_log.setSequenceStart(currentOffset);
                currentOffset += count;
                rec_log.setSequenceEnd(currentOffset - 1);

                m_logRanges.updateBlock(rec_log, logBlock);
            }
        }

        flushSummary();

        return deleteCount;
    }

    public int delete(int entriesToDelete) throws
                                           Exception
    {
        AtomicReference<ZonedDateTime> threshold = new AtomicReference<>();

        extract(entriesToDelete, null, 1, (entry, offset) -> threshold.set(entry.timestamp));

        return delete(threshold.get());
    }

    public void trim(int maxEntriesToKeep,
                     int entriesThreshold,
                     Duration maxTimeToKeep,
                     Duration timeThreshold) throws
                                             Exception
    {
        int entries = getLastOffset();
        if (entries > (maxEntriesToKeep + entriesThreshold))
        {
            delete(entries - maxEntriesToKeep);
        }

        ZonedDateTime now          = TimeUtils.now();
        ZonedDateTime maxToKeep    = now.minus(maxTimeToKeep);
        ZonedDateTime maxThreshold = maxToKeep.minus(timeThreshold);
        ZonedDateTime firstEntry   = getFirstOutput();
        if (firstEntry != null && firstEntry.isBefore(maxThreshold))
        {
            delete(maxToKeep);
        }
    }
}
