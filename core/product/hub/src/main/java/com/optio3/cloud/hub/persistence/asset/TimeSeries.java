/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.asset;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesExtract;
import com.optio3.collection.ExpandableArrayOf;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.collection.ExpandableArrayOfDoubles;
import com.optio3.collection.ExpandableArrayOfLongs;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.model.FieldTemporalResolution;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.TypedBitSet;
import com.optio3.stream.InputBitBuffer;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBitBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BitSets;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;

public class TimeSeries implements AutoCloseable
{
    public static class ForCompaction
    {
    }

    public static class ForLazy
    {
    }

    public static final Logger LoggerInstance              = new Logger(TimeSeries.class);
    public static final Logger LoggerInstanceForCompaction = LoggerInstance.createSubLogger(ForCompaction.class);
    public static final Logger LoggerInstanceForLazy       = LoggerInstance.createSubLogger(ForLazy.class);

    //
    // MariaDB doesn't handle ever-growing records that well.
    //
    // Each InnoDB B-tree page contains some header/trailer fields, about 200 bytes.
    //
    // Also, a page (16KB) cannot contains less than two records, which means a maximum in-page record of 8000 bytes.
    // After that, the large blob is sent to an extend page, which is *not* shared with other records.
    //
    // Because of that, variable-sized records behave very poorly.
    // Instead, we stick to three fixed-sized record: 3000, 5000, and 16000.
    // The first two will fit multiple whole records in a page.
    // The last is going to spill to an extend page and completely use it.
    //
    public static final int c_smallArchiveSize       = 3000;
    public static final int c_mediumArchiveSize      = 5000;
    public static final int c_maxArchiveSize         = 16000;
    public static final int c_archiveSizeRounding    = 100;
    public static final int c_perSampleSizeThreshold = 8;

    //--//

    private final static ValueEncoding[]     s_lookupValue     = new ValueEncoding[8];
    private final static ScaleEncoding[]     s_lookupScale     = new ScaleEncoding[8];
    private final static TimestampEncoding[] s_lookupTimestamp = new TimestampEncoding[8];

    private final static int c_integerMaxShortDeltaLog = 3;
    private final static int c_integerMaxLongDeltaLog  = 6;
    private final static int c_integerMaxDeltaLog      = 18;

    private final static int c_decimalVariableMaxShortDeltaLog = 4;
    private final static int c_decimalVariableMaxLongDeltaLog  = 8;
    private final static int c_decimalVariableMaxDeltaLog      = 32;

    static
    {
        for (ValueEncoding value : ValueEncoding.values())
        {
            s_lookupValue[(int) value.m_encoding] = value;
        }

        for (ScaleEncoding value : ScaleEncoding.values())
        {
            s_lookupScale[(int) value.m_encoding] = value;
        }

        for (TimestampEncoding value : TimestampEncoding.values())
        {
            s_lookupTimestamp[(int) value.m_encoding] = value;
        }
    }

    public static class Encoded implements AutoCloseable
    {
        private       OutputBuffer m_data;
        private final int          m_paddedLength;

        Encoded(OutputBuffer data,
                int paddedLength)
        {
            m_data         = data;
            m_paddedLength = paddedLength;
        }

        @Override
        public void close()
        {
            if (m_data != null)
            {
                m_data.close();
                m_data = null;
            }
        }

        public byte[] toByteArray()
        {
            return m_data.toByteArray(m_paddedLength);
        }

        public int getUnpaddedLength()
        {
            return m_data.size();
        }

        public int getPaddedLength()
        {
            return m_paddedLength;
        }
    }

    public enum Version
    {
        // @formatter:off
        Legacy(-1),
        V1    ( 1),
        V2    ( 2);
        // @formatter:on

        public final int encoding;

        Version(int encoding)
        {
            this.encoding = encoding;
        }

        public static Version parse(InputBuffer ib)
        {
            if (ib.remainingLength() > 4 && ib.read1ByteUnsigned() == 'T' && ib.read1ByteUnsigned() == 'S' && ib.read1ByteUnsigned() == 'V')
            {
                int v = ib.read1ByteUnsigned();

                for (Version value : values())
                {
                    if (value.encoding == v)
                    {
                        return value;
                    }
                }
            }

            return Legacy;
        }
    }

    public static class Lazy
    {
        private static final AtomicInteger s_hits        = new AtomicInteger();
        private static final AtomicInteger s_misses      = new AtomicInteger();
        private static final AtomicLong    s_rebuildTime = new AtomicLong();
        private static       ZonedDateTime s_lastReport;

        private final byte[]                    m_encoded;
        private       WeakReference<TimeSeries> m_ts;

        public Lazy(byte[] encoded)
        {
            m_encoded = encoded;
        }

        public TimeSeries get()
        {
            TimeSeries ts = m_ts != null ? m_ts.get() : null;
            if (ts == null)
            {
                Stopwatch sw = Stopwatch.createStarted();
                ts = TimeSeries.decode(m_encoded);
                sw.stop();

                if (ts != null)
                {
                    ts.setReadonly();

                    m_ts = new WeakReference<>(ts);

                    s_misses.incrementAndGet();
                    s_rebuildTime.addAndGet(sw.elapsed(TimeUnit.MICROSECONDS));
                }
            }
            else
            {
                s_hits.incrementAndGet();
            }

            if (LoggerInstanceForLazy.isEnabled(Severity.Debug))
            {
                if (!TimeUtils.wasUpdatedRecently(s_lastReport, 10, TimeUnit.SECONDS))
                {
                    s_lastReport = TimeUtils.now();
                    long hits   = s_hits.get();
                    long misses = s_misses.get();
                    long time   = s_rebuildTime.get();
                    LoggerInstanceForLazy.debug("Rebuild: hits %,d / Misses %,d in %,d usec (%,d per rebuild)", hits, misses, time, time / Math.max(1, misses));
                }
            }

            return ts;
        }
    }

    //--//

    public static abstract class Comparison
    {
        public boolean areIdenticalRaw(TimeSeries tsA,
                                       TimeSeries tsB)
        {
            OutputBitBuffer blobA     = tsA.encodeUncompressed();
            byte[]          inflatedA = blobA.toByteArray();

            OutputBitBuffer blobB     = tsB.encodeUncompressed();
            byte[]          inflatedB = blobB.toByteArray();

            if (inflatedA.length != inflatedB.length)
            {
                report("Failed: length mismatch %d != %d", inflatedA.length, inflatedB.length);
                return false;
            }

            int pos = Arrays.mismatch(inflatedA, inflatedB);
            if (pos >= 0)
            {
                report("Failed: mismatch at %d: %d != %d", pos, inflatedA[pos], inflatedB[pos]);
                return false;
            }

            return true;
        }

        protected abstract boolean report(String line);

        private boolean report(String fmt,
                               Object... args)
        {
            return report(String.format(fmt, args));
        }

        public boolean areIdentical(TimeSeries tsA,
                                    TimeSeries tsB)
        {
            return areIdentical(tsA.m_decoded, tsB.m_decoded);
        }

        boolean areIdentical(State tsA,
                             State tsB)
        {
            if (tsA.m_timestamps.size() != tsB.m_timestamps.size())
            {
                report("Failed: m_numberOfTimestamps %d <=> %d", tsA.m_timestamps.size(), tsB.m_timestamps.size());
                return false;
            }

            tsA.ensureSchemaLookup();
            tsB.ensureSchemaLookup();

            if (!tsA.m_schemaLookup.keySet()
                                   .equals(tsB.m_schemaLookup.keySet()))
            {
                report("Failed: m_schemaLookup: %s <=> %s", tsA.m_schemaLookup.keySet(), tsB.m_schemaLookup.keySet());
                return false;
            }

            double[] timestampsA = tsA.m_timestamps.toArray();
            double[] timestampsB = tsB.m_timestamps.toArray();

            int pos = Arrays.mismatch(timestampsA, timestampsB);
            if (pos >= 0)
            {
                report("Failed: timestamps mismatch at %d: %f != %f", pos, timestampsA[pos], timestampsB[pos]);
                return false;
            }

            boolean identical = true;

            for (String key : tsA.m_schemaLookup.keySet())
            {
                TimeSeries.SampleSchema s1 = tsA.m_schemaLookup.get(key);
                TimeSeries.SampleSchema s2 = tsB.m_schemaLookup.get(key);

                if (s1.m_valuesLong != null && s2.m_valuesLong != null)
                {
                    long[] a1 = s1.m_valuesLong.toArray();
                    long[] a2 = s2.m_valuesLong.toArray();

                    pos = Arrays.mismatch(a1, a2);
                    if (pos >= 0)
                    {
                        if (!report("Failed: value mismatch at %d: %d != %d", pos, a1[pos], a2[pos]))
                        {
                            return false;
                        }

                        identical = false;
                    }
                }
                else if (s1.m_valuesDouble != null && s2.m_valuesDouble != null)
                {
                    double[] a1 = s1.m_valuesDouble.toArray();
                    double[] a2 = s2.m_valuesDouble.toArray();

                    pos = Arrays.mismatch(a1, a2);
                    if (pos >= 0)
                    {
                        double diff = a1[pos] - a2[pos];
                        if (Math.abs(diff) > 0.1)
                        {
                            if (!report("Failed: value mismatch at %d: %f != %f", pos, a1[pos], a2[pos]))
                            {
                                return false;
                            }

                            identical = false;
                        }
                    }
                }
                else if (s1.m_valuesSet != null && s2.m_valuesSet != null)
                {
                    BitSet[] a1 = s1.m_valuesSet.toArray();
                    BitSet[] a2 = s2.m_valuesSet.toArray();

                    for (int i = 0; i < a1.length; i++)
                    {
                        BitSet a1b = a1[i];
                        BitSet a2b = a2[i];

                        if (a1b != null && a2b != null)
                        {
                            if (!a1b.equals(a2b))
                            {
                                if (!report("Failed: value mismatch at %d/%d: %d != %d", i, pos, a1b, a2b))
                                {
                                    return false;
                                }

                                identical = false;
                            }
                        }
                    }
                }
                else
                {
                    if (!report("Failed: schema mismatch: %s", key))
                    {
                        return false;
                    }

                    identical = false;
                }
            }

            return identical;
        }
    }

    //--//

    public static class NumericValueRange
    {
        public double timestamp;
        public double delta;
        public double valueLeft;
        public double valueRight;
    }

    public static class NumericValueRanges
    {
        public ZonedDateTime firstTimestamp;
        public ZonedDateTime lastTimestamp;

        public int numberOfMissingSamples;
        public int numberOfSamples;

        public NumericValueRange[] ranges;

        public void importValues(TimeSeriesExtract<?> extract)
        {
            List<NumericValueRange> rangesList           = Lists.newArrayList();
            double                  lastTimestampSeconds = Double.NaN;

            ExpandableArrayOfDoubles timestamps = extract.timestamps;
            ExpandableArrayOfDoubles values     = extract.values;
            int                      samples    = timestamps.size();

            for (int position = 0; position < samples; position++)
            {
                lastTimestampSeconds = timestamps.get(position, Double.NaN);

                if (firstTimestamp == null)
                {
                    firstTimestamp = TimeUtils.fromTimestampToUtcTime(lastTimestampSeconds);
                }

                double val = values.get(position, Double.NaN);
                if (!Double.isNaN(val))
                {
                    NumericValueRange previousRange = CollectionUtils.lastElement(rangesList);
                    if (previousRange == null)
                    {
                        //
                        // No previous range => create a zero-width range.
                        //
                        NumericValueRange range = new NumericValueRange();
                        range.timestamp  = lastTimestampSeconds;
                        range.delta      = 0;
                        range.valueLeft  = val;
                        range.valueRight = val;
                        rangesList.add(range);
                    }
                    else if (previousRange.valueRight == val)
                    {
                        //
                        // Same value => extend range.
                        //
                        previousRange.delta = lastTimestampSeconds - previousRange.timestamp;
                    }
                    else if (previousRange.delta == 0)
                    {
                        //
                        // Different value, but previous range is zero-width => adjust width and right value.
                        //
                        previousRange.delta      = lastTimestampSeconds - previousRange.timestamp;
                        previousRange.valueRight = val;
                    }
                    else
                    {
                        //
                        // Different value, non-zero width => create new range from the end of previous one.
                        //
                        NumericValueRange range = new NumericValueRange();
                        range.timestamp  = previousRange.timestamp + previousRange.delta;
                        range.delta      = lastTimestampSeconds - range.timestamp;
                        range.valueLeft  = previousRange.valueRight;
                        range.valueRight = val;
                        rangesList.add(range);
                    }
                }
                else
                {
                    numberOfMissingSamples++;
                }

                numberOfSamples++;
            }

            if (firstTimestamp != null)
            {
                lastTimestamp = TimeUtils.fromTimestampToUtcTime(lastTimestampSeconds);
            }

            ranges = new NumericValueRange[rangesList.size()];
            rangesList.toArray(ranges);
        }
    }

    //--//

    public static class SizeEstimator implements AutoCloseable
    {
        public        TimeSeries timeSeries;
        private       TimeSeries m_lastSnapshot;
        private       int        m_lastSnapshotByteSize;
        private       int        m_lastFailedArchiveSize;
        private final float      m_averageBytesPerSampleHint;
        private       float      m_averageBytesPerSample;
        private       int        m_estimatedSamplesCapacity;

        public SizeEstimator(TimeSeries ts,
                             float averageBytesPerSampleHint)
        {
            timeSeries                  = ts;
            m_averageBytesPerSampleHint = averageBytesPerSampleHint;

            checkpoint();
        }

        @Override
        public void close()
        {
            if (timeSeries != null)
            {
                timeSeries.close();
                timeSeries = null;
            }

            if (m_lastSnapshot != null)
            {
                m_lastSnapshot.close();
                m_lastSnapshot = null;
            }
        }

        public void checkpoint()
        {
            Encoded tse = timeSeries.encode();
            if (tse != null)
            {
                int storageSize = tse.getUnpaddedLength();

                m_lastSnapshot          = timeSeries.createCopy();
                m_lastSnapshotByteSize  = storageSize;
                m_averageBytesPerSample = ((float) storageSize) / timeSeries.numberOfSamples();
            }
            else
            {
                m_lastSnapshot          = null;
                m_lastSnapshotByteSize  = 0;
                m_averageBytesPerSample = 1;
            }

            m_lastFailedArchiveSize = 0;
        }

        public boolean wouldFit()
        {
            Encoded tse = timeSeries.encode();
            if (tse == null)
            {
                return true;
            }

            int storageSize = tse.getUnpaddedLength();
            LoggerInstanceForCompaction.debugVerbose("  WouldFit: %d", storageSize);

            if (storageSize <= c_maxArchiveSize + c_archiveSizeRounding)
            {
                checkpoint();
                return true;
            }

            // Rollback
            timeSeries.close();

            if (m_lastSnapshot != null)
            {
                timeSeries = m_lastSnapshot.createCopy();
            }
            else
            {
                timeSeries = TimeSeries.newInstance();
            }

            if (m_lastFailedArchiveSize == storageSize)
            {
                // No size change since last attempt, prepare to give up, by selecting a really large per-sample size.
                m_averageBytesPerSample = 2 * c_perSampleSizeThreshold;
            }
            else
            {
                m_lastFailedArchiveSize = storageSize;

                int alreadyUsed = m_lastSnapshotByteSize;
                int actualSize  = storageSize - alreadyUsed;

                var before = m_averageBytesPerSample;

                // Adjust the average based on actual size needed, adding a 30% margin.
                m_averageBytesPerSample = actualSize / (0.7f * m_estimatedSamplesCapacity);

                LoggerInstanceForCompaction.debugVerbose("  WouldFit: %d %d %d : %f -> %f", storageSize, actualSize, m_estimatedSamplesCapacity, before, m_averageBytesPerSample);
            }

            return false;
        }

        public int estimateSpareSamples()
        {
            int alreadyUsed = m_lastSnapshotByteSize;
            int available   = c_maxArchiveSize - alreadyUsed;

            float averageBytesPerSample = Math.max(m_averageBytesPerSample, m_averageBytesPerSampleHint);
            if (alreadyUsed > 0 && averageBytesPerSample > c_perSampleSizeThreshold)
            {
                // If the average sample size grows too much, just flush the archive.
                return -1;
            }

            m_estimatedSamplesCapacity = (int) (available / averageBytesPerSample);

            LoggerInstanceForCompaction.debugVerbose("  EstimateSpareSamples: %d %d : %f %f : %d",
                                                     m_lastSnapshotByteSize,
                                                     available,
                                                     m_averageBytesPerSample,
                                                     m_averageBytesPerSampleHint,
                                                     m_estimatedSamplesCapacity);

            if (m_estimatedSamplesCapacity < 10)
            {
                // Ensure we make forward progress even in the presence of large samples.
                if (alreadyUsed > (c_maxArchiveSize / 20))
                {
                    return -1;
                }
            }

            if (timeSeries.hasTooManySamples())
            {
                // Break into another archive.
                return -1;
            }

            // Limit the maximum batch of samples.
            return Math.min(4_000, m_estimatedSamplesCapacity);
        }
    }

    //--//

    enum ValueEncoding
    {
        Missing(0),
        Same(1),
        PositiveShortDelta(2),
        NegativeShortDelta(3),
        PositiveLongDelta(4),
        NegativeLongDelta(5),
        Delta(6),
        Absolute(7);

        private final long m_encoding;

        ValueEncoding(int encoding)
        {
            m_encoding = encoding;
        }

        public static ValueEncoding decode(InputBitBuffer ib)
        {
            long value = ib.readUnsignedFixedLength(3);
            return s_lookupValue[(int) value];
        }

        public void encode(OutputBitBuffer ob)
        {
            ob.emitFixedLength(m_encoding, 3);
        }
    }

    enum ScaleEncoding
    {
        P1(0, 10000.0),
        P2(1, 100.0),
        P3(2, 1.0),
        P4(3, 0.1),
        P5(4, 0.01),
        P6(5, 0.001),
        P7(6, 0.00001),
        P8(7, 0.0000001);

        private final long   m_encoding;
        private final double m_scale;

        ScaleEncoding(int encoding,
                      double scale)
        {
            m_encoding = encoding;
            m_scale    = scale;
        }

        public static ScaleEncoding decode(InputBitBuffer ib)
        {
            long value = ib.readUnsignedFixedLength(3);
            return s_lookupScale[(int) value];
        }

        public void encode(OutputBitBuffer ob)
        {
            ob.emitFixedLength(m_encoding, 3);
        }
    }

    enum TimestampEncoding
    {
        Delta(0)
                {
                    @Override
                    public void decodeValues(InputBitBuffer ib,
                                             ExpandableArrayOfDoubles timestamps,
                                             double inverseScalingFactor)
                    {
                        int numberOfTimestamps = (int) ib.readUnsignedVariableLength();

                        timestamps.clear();
                        timestamps.grow(numberOfTimestamps);

                        long currentTimestamp = 0;

                        for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            long timeDelta = ib.readUnsignedVariableLength();
                            currentTimestamp += timeDelta;

                            timestamps.set(timestampIndex, currentTimestamp * inverseScalingFactor);
                        }
                    }

                    @Override
                    public OutputBitBuffer encodeValues(ExpandableArrayOfLongs timestamps)
                    {
                        OutputBitBuffer ob = new OutputBitBuffer();
                        encode(ob);

                        int numberOfTimestamps = timestamps.size();
                        ob.emitUnsignedVariableLength(numberOfTimestamps);

                        long previousTimestamp = 0;

                        for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            long currentTimestamp = timestamps.get(timestampIndex, 0);

                            long diff = currentTimestamp - previousTimestamp;
                            ob.emitUnsignedVariableLength(diff);

                            previousTimestamp = currentTimestamp;
                        }

                        return ob;
                    }
                },
        DeltaOfDelta(1)
                {
                    @Override
                    public void decodeValues(InputBitBuffer ib,
                                             ExpandableArrayOfDoubles timestamps,
                                             double inverseScalingFactor)
                    {
                        int numberOfTimestamps = (int) ib.readUnsignedVariableLength();

                        timestamps.clear();
                        timestamps.grow(numberOfTimestamps);

                        long currentTimestamp = 0;
                        long previousDelta    = 0;

                        for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            long deltaOfDelta = ib.readSignedVariableLength();
                            long delta        = previousDelta + deltaOfDelta;
                            currentTimestamp += delta;
                            previousDelta = delta;

                            timestamps.set(timestampIndex, currentTimestamp * inverseScalingFactor);
                        }
                    }

                    @Override
                    public OutputBitBuffer encodeValues(ExpandableArrayOfLongs timestamps)
                    {
                        OutputBitBuffer ob = new OutputBitBuffer();
                        encode(ob);

                        int numberOfTimestamps = timestamps.size();
                        ob.emitUnsignedVariableLength(numberOfTimestamps);

                        long previousTimestamp = 0;
                        long previousDelta     = 0;

                        for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            long currentTimestamp = timestamps.get(timestampIndex, 0);

                            long delta        = currentTimestamp - previousTimestamp;
                            long deltaOfDelta = delta - previousDelta;
                            ob.emitSignedVariableLength(deltaOfDelta);

                            previousTimestamp = currentTimestamp;
                            previousDelta     = delta;
                        }

                        return ob;
                    }
                },
        RunLength(2)
                {
                    @Override
                    public void decodeValues(InputBitBuffer ib,
                                             ExpandableArrayOfDoubles timestamps,
                                             double inverseScalingFactor)
                    {
                        int numberOfTimestamps = (int) ib.readUnsignedVariableLength();

                        timestamps.clear();
                        timestamps.grow(numberOfTimestamps);

                        long currentTimestamp = ib.readUnsignedVariableLength();
                        timestamps.set(0, currentTimestamp * inverseScalingFactor);

                        for (int timestampIndex = 1; timestampIndex < numberOfTimestamps; )
                        {
                            int  runLength = (int) ib.readUnsignedVariableLength();
                            long delta     = ib.readUnsignedVariableLength();

                            while (runLength-- > 0)
                            {
                                currentTimestamp += delta;

                                timestamps.set(timestampIndex++, currentTimestamp * inverseScalingFactor);
                            }
                        }
                    }

                    @Override
                    public OutputBitBuffer encodeValues(ExpandableArrayOfLongs timestamps)
                    {
                        OutputBitBuffer ob = new OutputBitBuffer();
                        encode(ob);

                        int numberOfTimestamps = timestamps.size();
                        ob.emitUnsignedVariableLength(numberOfTimestamps);

                        long previousTimestamp = timestamps.get(0, 0);
                        long lastDelta         = 0;
                        int  runLength         = 0;

                        // First timestamp is sent as absolute value.
                        ob.emitUnsignedVariableLength(previousTimestamp);

                        for (int timestampIndex = 1; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            long currentTimestamp = timestamps.get(timestampIndex, 0);

                            long delta = currentTimestamp - previousTimestamp;

                            if (delta == lastDelta)
                            {
                                runLength++;
                            }
                            else
                            {
                                if (runLength > 0)
                                {
                                    ob.emitUnsignedVariableLength(runLength);
                                    ob.emitUnsignedVariableLength(lastDelta);
                                }

                                lastDelta = delta;
                                runLength = 1;
                            }

                            previousTimestamp = currentTimestamp;
                        }

                        if (runLength > 0)
                        {
                            ob.emitUnsignedVariableLength(runLength);
                            ob.emitUnsignedVariableLength(lastDelta);
                        }

                        return ob;
                    }
                },
        Reserved3(3),
        Reserved4(4),
        Reserved5(5),
        Reserved6(6),
        Reserved7(7);

        private final long m_encoding;

        TimestampEncoding(int encoding)
        {
            m_encoding = encoding;
        }

        public static TimestampEncoding decode(InputBitBuffer ib)
        {
            long value = ib.readUnsignedFixedLength(3);
            return s_lookupTimestamp[(int) value];
        }

        public void encode(OutputBitBuffer ob)
        {
            ob.emitFixedLength(m_encoding, 3);
        }

        //--//

        public void decodeValues(InputBitBuffer ib,
                                 ExpandableArrayOfDoubles timestamps,
                                 double inverseScalingFactor)
        {
            throw new RuntimeException("Not Implemented");
        }

        public OutputBitBuffer encodeValues(ExpandableArrayOfLongs timestamps)
        {
            return null;
        }
    }

    public enum SampleType
    {
        Integer,
        Decimal,
        BitSet,
        Enumerated,
        EnumeratedSet
    }

    public enum SampleResolution
    {
        Max1Hz(FieldTemporalResolution.Max1Hz, null),
        Max100Hz(FieldTemporalResolution.Max100Hz, "Res_10msec/"),
        Max1000Hz(FieldTemporalResolution.Max1000Hz, "Res_1msec/");

        public final FieldTemporalResolution details;
        public final String                  prefix;

        SampleResolution(FieldTemporalResolution resolution,
                         String prefix)
        {
            this.details = resolution;
            this.prefix  = prefix;
        }

        public static SampleResolution convert(FieldTemporalResolution temporalResolution)
        {
            for (SampleResolution value : values())
            {
                if (value.details == temporalResolution)
                {
                    return value;
                }
            }

            return Max1Hz;
        }

        public SampleResolution maxResolution(SampleResolution other)
        {
            return other == null || details.scalingFactor > other.details.scalingFactor ? this : other;
        }
    }

    public static class SampleSchema implements AutoCloseable
    {
        static class ExpandableArrayOfBitSet extends ExpandableArrayOf<BitSet>
        {
            ExpandableArrayOfBitSet()
            {
                super(BitSet.class);
            }

            @Override
            protected ExpandableArrayOf<BitSet> allocate()
            {
                return new ExpandableArrayOfBitSet();
            }

            @Override
            protected int compare(BitSet o1,
                                  BitSet o2)
            {
                throw new RuntimeException("Not implemented");
            }
        }

        private static final String HIGH_PRECISION    = "HiPrec/"; // Legacy, equivalent to variablePrecision = 5
        private static final String MINIMAL_PRECISION = "VarPrec/"; // Legacy, it's really a minimum precision
        private static final String DYNAMIC_PRECISION = "DynPrec/";

        private final State            m_holder;
        public final  String           identifier;
        public final  SampleType       type;
        private       SampleResolution m_resolution;
        private       int              m_minimumPrecision;
        private       int              m_dynamicPrecisionTimesTen;

        private ExpandableArrayOfDoubles m_valuesDouble;
        private ExpandableArrayOfLongs   m_valuesLong;
        private ExpandableArrayOfBitSet  m_valuesSet;
        private List<String>             m_enumLookups = Collections.emptyList();
        private Object[]                 m_enumLookupsResolved;
        private Class<?>                 m_enumLookupsResolvedForClass;
        private int                      m_missing     = -1;

        SampleSchema(State holder,
                     SampleSchema src)
        {
            m_holder                   = holder;
            identifier                 = src.identifier;
            type                       = src.type;
            m_resolution               = src.m_resolution;
            m_minimumPrecision         = src.m_minimumPrecision;
            m_dynamicPrecisionTimesTen = src.m_dynamicPrecisionTimesTen;

            switch (type)
            {
                case BitSet:
                case EnumeratedSet:
                    if (src.m_valuesSet != null)
                    {
                        m_valuesSet = (ExpandableArrayOfBitSet) src.m_valuesSet.copy();
                    }
                    else
                    {
                        m_valuesSet = new ExpandableArrayOfBitSet();
                    }
                    break;

                case Decimal:
                    if (src.m_valuesDouble != null)
                    {
                        m_valuesDouble = src.m_valuesDouble.copy();
                    }
                    else
                    {
                        m_valuesDouble = ExpandableArrayOfDoubles.create();
                    }
                    break;

                default:
                    if (src.m_valuesLong != null)
                    {
                        m_valuesLong = src.m_valuesLong.copy();
                    }
                    else
                    {
                        m_valuesLong = ExpandableArrayOfLongs.create();
                    }
                    break;
            }

            if (!src.m_enumLookups.isEmpty())
            {
                m_enumLookups         = Lists.newArrayList(src.m_enumLookups);
                m_enumLookupsResolved = null;
            }
        }

        SampleSchema(State holder,
                     InputBitBuffer ib)
        {
            m_holder   = holder;
            identifier = ib.readString();

            String typeEncoded = ib.readString();

            for (SampleResolution value : SampleResolution.values())
            {
                String prefix = value.prefix;
                if (prefix != null && typeEncoded.startsWith(prefix))
                {
                    typeEncoded  = typeEncoded.substring(prefix.length());
                    m_resolution = value;
                }
            }

            if (m_resolution == null)
            {
                m_resolution = SampleResolution.Max1Hz;
            }

            if (typeEncoded.startsWith(HIGH_PRECISION))
            {
                typeEncoded                = typeEncoded.substring(HIGH_PRECISION.length());
                m_minimumPrecision         = 5;
                m_dynamicPrecisionTimesTen = m_minimumPrecision * 10;
            }
            else if (typeEncoded.startsWith(MINIMAL_PRECISION))
            {
                typeEncoded                = typeEncoded.substring(MINIMAL_PRECISION.length());
                m_minimumPrecision         = (int) ib.readUnsignedVariableLength();
                m_dynamicPrecisionTimesTen = m_minimumPrecision * 10;
            }
            else if (typeEncoded.startsWith(DYNAMIC_PRECISION))
            {
                typeEncoded                = typeEncoded.substring(DYNAMIC_PRECISION.length());
                m_minimumPrecision         = 0;
                m_dynamicPrecisionTimesTen = (int) ib.readSignedVariableLength();
            }
            else
            {
                m_minimumPrecision         = 0;
                m_dynamicPrecisionTimesTen = Integer.MIN_VALUE; // Legacy settings.
            }

            type = SampleType.valueOf(typeEncoded);
            initializeValueArrays();

            switch (type)
            {
                case Enumerated:
                case EnumeratedSet:
                    m_enumLookups = Lists.newArrayList();
                    m_enumLookupsResolved = null;

                    int numberOfValues = (int) ib.readUnsignedVariableLength();
                    for (int i = 0; i < numberOfValues; i++)
                    {
                        m_enumLookups.add(ib.readString());
                    }
                    break;
            }
        }

        SampleSchema(State holder,
                     String identifier,
                     SampleType type)
        {
            m_holder        = holder;
            this.identifier = identifier;
            this.type       = type;

            m_resolution               = SampleResolution.Max1Hz;
            m_minimumPrecision         = 0; // Zero value means dynamic precision
            m_dynamicPrecisionTimesTen = 0;

            initializeValueArrays();
        }

        @Override
        public void close()
        {
            if (m_valuesDouble != null)
            {
                m_valuesDouble.close();
                m_valuesDouble = null;
            }

            if (m_valuesLong != null)
            {
                m_valuesLong.close();
                m_valuesLong = null;
            }

            if (m_valuesSet != null)
            {
                m_valuesSet.close();
                m_valuesSet = null;
            }
        }

        private void initializeValueArrays()
        {
            switch (type)
            {
                case BitSet:
                case EnumeratedSet:
                    m_valuesSet = new ExpandableArrayOfBitSet();
                    break;

                case Decimal:
                    m_valuesDouble = ExpandableArrayOfDoubles.create();
                    break;

                default:
                    m_valuesLong = ExpandableArrayOfLongs.create();
                    break;
            }
        }

        void ensureLength()
        {
            int length = m_holder.m_timestamps.size();

            switch (type)
            {
                case Integer:
                case Enumerated:
                {
                    ExpandableArrayOfLongs values = m_valuesLong;

                    int currentLength = values.size();

                    values.growTo(length);
                    while (currentLength < length)
                    {
                        values.set(currentLength++, Long.MAX_VALUE);
                    }
                    break;
                }

                case Decimal:
                {
                    ExpandableArrayOfDoubles values = m_valuesDouble;

                    int currentLength = values.size();

                    values.growTo(length);
                    while (currentLength < length)
                    {
                        values.set(currentLength++, Double.NaN);
                    }
                    break;
                }

                case BitSet:
                case EnumeratedSet:
                {
                    ExpandableArrayOf<BitSet> values = m_valuesSet;

                    int currentLength = values.size();

                    values.growTo(length);
                    while (currentLength < length)
                    {
                        values.set(currentLength++, null);
                    }
                    break;
                }
            }
        }

        //--//

        public double truncateTimestamp(double timestamp)
        {
            return m_resolution.details.truncateTimestamp(timestamp);
        }

        public SampleResolution getResolution()
        {
            return m_resolution;
        }

        void setResolution(SampleResolution res)
        {
            res = m_resolution.maxResolution(res);

            if (m_resolution != res)
            {
                m_resolution = res;
            }
        }

        public int getMinimumPrecision()
        {
            return m_minimumPrecision;
        }

        public void setMinimumPrecision(int minimumPrecision)
        {
            if (m_minimumPrecision < minimumPrecision)
            {
                m_minimumPrecision         = minimumPrecision;
                m_dynamicPrecisionTimesTen = minimumPrecision * 10;
            }
        }

        void encodeMetadata(OutputBitBuffer ob)
        {
            ob.emitString(identifier);

            boolean emitMinimumPrecision = false;
            boolean emitDynamicPrecision = false;

            if (type == SampleType.Decimal && m_minimumPrecision == 0)
            {
                //
                // Create an histogram of precision, to find the resolution where we include 95% of the samples.
                //
                ExpandableArrayOfDoubles values         = m_valuesDouble;
                int[]                    population     = new int[100];
                int                      numValidValues = 0;
                int                      maxPopulation  = 0;
                double                   previousValue  = 0;

                for (int position = 0; position < values.size(); position++)
                {
                    double value = values.get(position, Double.NaN);
                    if (!Double.isNaN(value))
                    {
                        double diff = Math.abs(value - previousValue);
                        int    dynamicPrecision;

                        if (diff == 0)
                        {
                            dynamicPrecision = 1 * 10;
                        }
                        else
                        {
                            double orderOfMagnitude = Math.log10(diff);

                            dynamicPrecision = (int) Math.ceil(10 * BoxingUtils.bound(-orderOfMagnitude, 0, 6));
                        }

                        numValidValues++;
                        previousValue = value;
                        maxPopulation = Math.max(maxPopulation, dynamicPrecision);

                        while (dynamicPrecision < population.length)
                        {
                            population[dynamicPrecision++]++;
                        }
                    }
                }

                m_dynamicPrecisionTimesTen = 0;

                for (int pos = 0; pos <= maxPopulation; pos++)
                {
                    m_dynamicPrecisionTimesTen = pos;

                    if (population[pos] >= numValidValues * 0.95)
                    {
                        break;
                    }
                }
            }

            {
                StringBuilder id = new StringBuilder();

                String prefix = m_resolution.prefix;
                if (prefix != null)
                {
                    id.append(prefix);
                }

                if (type == SampleType.Decimal)
                {
                    if (m_minimumPrecision != 0)
                    {
                        id.append(MINIMAL_PRECISION);
                        emitMinimumPrecision = true;
                    }
                    else
                    {
                        id.append(DYNAMIC_PRECISION);
                        emitDynamicPrecision = true;
                    }
                }

                id.append(type.name());
                ob.emitString(id.toString());
            }

            if (emitMinimumPrecision)
            {
                ob.emitUnsignedVariableLength(m_minimumPrecision);
            }

            if (emitDynamicPrecision)
            {
                ob.emitSignedVariableLength(m_dynamicPrecisionTimesTen);
            }

            switch (type)
            {
                case Enumerated:
                case EnumeratedSet:
                    ob.emitUnsignedVariableLength(m_enumLookups.size());
                    for (String value : m_enumLookups)
                    {
                        ob.emitString(value);
                    }
                    break;
            }
        }

        //--//

        void decodeValues(InputBitBuffer ib,
                          int numberOfTimestamps)
        {
            switch (type)
            {
                case Integer:
                case Enumerated: // These are encoded as integers.
                {
                    ExpandableArrayOfLongs values = m_valuesLong;
                    long                   value  = 0;

                    values.clear();
                    values.grow(numberOfTimestamps);

                    for (int valueIndex = 0; valueIndex < numberOfTimestamps; valueIndex++)
                    {
                        ValueEncoding enc = ValueEncoding.decode(ib);
                        switch (enc)
                        {
                            case Missing:
                                values.set(valueIndex, Long.MAX_VALUE);
                                continue;

                            case Same:
                                break;

                            case PositiveShortDelta:
                                value += ib.readUnsignedFixedLength(c_integerMaxShortDeltaLog);
                                break;

                            case NegativeShortDelta:
                                value -= ib.readUnsignedFixedLength(c_integerMaxShortDeltaLog);
                                break;

                            case PositiveLongDelta:
                                value += ib.readUnsignedFixedLength(c_integerMaxLongDeltaLog);
                                break;

                            case NegativeLongDelta:
                                value -= ib.readUnsignedFixedLength(c_integerMaxLongDeltaLog);
                                break;

                            case Delta:
                                value += ib.readSignedVariableLength();
                                break;

                            case Absolute:
                                value = ib.readSignedVariableLength();
                                break;
                        }

                        values.set(valueIndex, value);
                    }
                    break;
                }

                case Decimal:
                {
                    ExpandableArrayOfDoubles values = m_valuesDouble;
                    double                   value  = 0.0;

                    values.clear();
                    values.grow(numberOfTimestamps);

                    if (m_dynamicPrecisionTimesTen != Integer.MIN_VALUE)
                    {
                        for (int valueIndex = 0; valueIndex < numberOfTimestamps; valueIndex++)
                        {
                            ScaleEncoding scaleEnc;
                            ValueEncoding valueEnc = ValueEncoding.decode(ib);
                            switch (valueEnc)
                            {
                                case Missing:
                                    values.set(valueIndex, Double.NaN);
                                    continue;

                                case Same:
                                    break;

                                case PositiveShortDelta:
                                    scaleEnc = ScaleEncoding.decode(ib);
                                    value += ib.readUnsignedFixedLength(c_decimalVariableMaxShortDeltaLog) * scaleEnc.m_scale;
                                    break;

                                case NegativeShortDelta:
                                    scaleEnc = ScaleEncoding.decode(ib);
                                    value -= ib.readUnsignedFixedLength(c_decimalVariableMaxShortDeltaLog) * scaleEnc.m_scale;
                                    break;

                                case PositiveLongDelta:
                                    scaleEnc = ScaleEncoding.decode(ib);
                                    value += ib.readUnsignedFixedLength(c_decimalVariableMaxLongDeltaLog) * scaleEnc.m_scale;
                                    break;

                                case NegativeLongDelta:
                                    scaleEnc = ScaleEncoding.decode(ib);
                                    value -= ib.readUnsignedFixedLength(c_decimalVariableMaxLongDeltaLog) * scaleEnc.m_scale;
                                    break;

                                case Delta:
                                    scaleEnc = ScaleEncoding.decode(ib);
                                    value += ib.readSignedVariableLength() * scaleEnc.m_scale;
                                    break;

                                case Absolute:
                                    value = ib.readDouble();
                                    break;
                            }

                            values.set(valueIndex, value);
                        }
                    }
                    else
                    {
                        //
                        // This is just legacy encoding, before dynamic resolution was introduced.
                        //
                        final int c_decimalMaxShortDeltaLog = 8;
                        final int c_decimalMaxLongDeltaLog  = 16;

                        final double c_decimalShortScaleFactor = 10.0;   // This translates into a +/-      25.5       range.
                        final double c_decimalLongScaleFactor  = 100.0;  // This translates into a +/-      65.535     range.
                        final double c_decimalScaleFactor      = 1000.0; // This translates into a +/- 549,755.813_887 range.

                        for (int valueIndex = 0; valueIndex < numberOfTimestamps; valueIndex++)
                        {
                            ValueEncoding enc = ValueEncoding.decode(ib);
                            switch (enc)
                            {
                                case Missing:
                                    values.set(valueIndex, Double.NaN);
                                    continue;

                                case Same:
                                    break;

                                case PositiveShortDelta:
                                    value += ib.readUnsignedFixedLength(c_decimalMaxShortDeltaLog) / c_decimalShortScaleFactor;
                                    break;

                                case NegativeShortDelta:
                                    value -= ib.readUnsignedFixedLength(c_decimalMaxShortDeltaLog) / c_decimalShortScaleFactor;
                                    break;

                                case PositiveLongDelta:
                                    value += ib.readUnsignedFixedLength(c_decimalMaxLongDeltaLog) / c_decimalLongScaleFactor;
                                    break;

                                case NegativeLongDelta:
                                    value -= ib.readUnsignedFixedLength(c_decimalMaxLongDeltaLog) / c_decimalLongScaleFactor;
                                    break;

                                case Delta:
                                    value += ib.readSignedVariableLength() / c_decimalScaleFactor;
                                    break;

                                case Absolute:
                                    value = ib.readDouble();
                                    break;
                            }

                            // For legacy reasons, we had some NaN and Infinite in the stream. Ignore them.
                            if (Double.isNaN(value))
                            {
                                values.set(valueIndex, Double.NaN);
                                continue;
                            }

                            values.set(valueIndex, value);
                        }
                    }
                    break;
                }

                case BitSet:
                case EnumeratedSet:
                {
                    BitSet                  value  = new BitSet();
                    ExpandableArrayOfBitSet values = m_valuesSet;

                    values.clear();
                    values.grow(numberOfTimestamps);

                    for (int valueIndex = 0; valueIndex < numberOfTimestamps; valueIndex++)
                    {
                        ValueEncoding enc = ValueEncoding.decode(ib);
                        switch (enc)
                        {
                            case Missing:
                                continue;

                            case Same:
                                // No need to copy here, bitsets are copied when accessed.
                                break;

                            case Delta:
                                BitSet deltaBitSet = ib.readBitSet();
                                deltaBitSet.xor(value);
                                value = deltaBitSet;
                                break;

                            case Absolute:
                                value = ib.readBitSet();
                                break;
                        }

                        values.set(valueIndex, value);
                    }
                    break;
                }
            }
        }

        void encodeValues(OutputBitBuffer ob)
        {
            switch (type)
            {
                case Integer:
                case Enumerated: // These are encoded as integers.
                {
                    long                   valuePrevious = 0;
                    ExpandableArrayOfLongs values        = m_valuesLong;

                    for (int position = 0; position < values.size(); position++)
                    {
                        long value = values.get(position, Long.MAX_VALUE);

                        if (value == Long.MAX_VALUE)
                        {
                            ValueEncoding.Missing.encode(ob);
                            continue;
                        }

                        if (value == valuePrevious)
                        {
                            ValueEncoding.Same.encode(ob);
                            continue;
                        }

                        if (isSafeToSubtract(value, valuePrevious))
                        {
                            long valueDiff    = value - valuePrevious;
                            long absValueDiff = Math.abs(valueDiff);

                            if (absValueDiff < (1 << c_integerMaxShortDeltaLog))
                            {
                                if (valueDiff > 0)
                                {
                                    ValueEncoding.PositiveShortDelta.encode(ob);
                                }
                                else
                                {
                                    ValueEncoding.NegativeShortDelta.encode(ob);
                                }

                                ob.emitFixedLength(absValueDiff, c_integerMaxShortDeltaLog);
                                valuePrevious = value;
                                continue;
                            }

                            if (absValueDiff < (1 << c_integerMaxLongDeltaLog))
                            {
                                if (valueDiff > 0)
                                {
                                    ValueEncoding.PositiveLongDelta.encode(ob);
                                }
                                else
                                {
                                    ValueEncoding.NegativeLongDelta.encode(ob);
                                }

                                ob.emitFixedLength(absValueDiff, c_integerMaxLongDeltaLog);
                                valuePrevious = value;
                                continue;
                            }

                            if (absValueDiff < (1 << c_integerMaxDeltaLog))
                            {
                                ValueEncoding.Delta.encode(ob);
                                ob.emitSignedVariableLength(valueDiff);
                                valuePrevious = value;
                                continue;
                            }
                        }

                        ValueEncoding.Absolute.encode(ob);
                        ob.emitSignedVariableLength(value);
                        valuePrevious = value;
                    }
                    break;
                }

                case Decimal:
                {
                    double                   valuePrevious = 0.0;
                    ExpandableArrayOfDoubles values        = m_valuesDouble;

                    final double maxError = Math.pow(10, -0.1 * m_dynamicPrecisionTimesTen);

                    for (int position = 0; position < values.size(); position++)
                    {
                        double value = values.get(position, Double.NaN);

                        if (Double.isNaN(value))
                        {
                            ValueEncoding.Missing.encode(ob);
                            continue;
                        }

                        double diff = value - valuePrevious;

                        if (Math.abs(diff) < maxError)
                        {
                            ValueEncoding.Same.encode(ob);
                            continue;
                        }

                        ScaleEncoding shortestScale          = null;
                        ValueEncoding shortestValue          = ValueEncoding.Absolute;
                        long          shortestDiff           = 0;
                        int           shortestLength         = 64;
                        double        shortestValueEstimated = 0;

                        for (ScaleEncoding scaleEncoding : s_lookupScale)
                        {
                            double diffScaled = diff / scaleEncoding.m_scale;

                            if (Math.abs(diffScaled) < (double) (Long.MAX_VALUE / 2)) // Compare in the floating-point domain, to avoid overflow issues.
                            {
                                long   diffTruncated  = Math.round(diffScaled);
                                double valueEstimated = (valuePrevious + (diffTruncated * scaleEncoding.m_scale));
                                double diffError      = Math.abs(valueEstimated - value);

                                if (diffError < maxError)
                                {
                                    long absDiffTruncated = Math.abs(diffTruncated);

                                    if (absDiffTruncated < (1 << c_decimalVariableMaxShortDeltaLog))
                                    {
                                        ValueEncoding valueEncoding = diffTruncated > 0 ? ValueEncoding.PositiveShortDelta : ValueEncoding.NegativeShortDelta;
                                        int           len           = 3 + c_decimalVariableMaxShortDeltaLog;

                                        if (shortestLength > len)
                                        {
                                            shortestScale          = scaleEncoding;
                                            shortestValue          = valueEncoding;
                                            shortestDiff           = Math.abs(diffTruncated);
                                            shortestLength         = len;
                                            shortestValueEstimated = valueEstimated;
                                        }
                                    }

                                    if (absDiffTruncated < (1 << c_decimalVariableMaxLongDeltaLog))
                                    {
                                        ValueEncoding valueEncoding = diffTruncated > 0 ? ValueEncoding.PositiveLongDelta : ValueEncoding.NegativeLongDelta;
                                        int           len           = 3 + c_decimalVariableMaxLongDeltaLog;

                                        if (shortestLength > len)
                                        {
                                            shortestScale          = scaleEncoding;
                                            shortestValue          = valueEncoding;
                                            shortestDiff           = Math.abs(diffTruncated);
                                            shortestLength         = len;
                                            shortestValueEstimated = valueEstimated;
                                        }
                                    }

                                    if (absDiffTruncated < (1L << c_decimalVariableMaxDeltaLog))
                                    {
                                        ValueEncoding valueEncoding = ValueEncoding.Delta;
                                        int           len           = 3 + ob.computeSignedVariableLength(diffTruncated);

                                        if (shortestLength > len)
                                        {
                                            shortestScale          = scaleEncoding;
                                            shortestValue          = valueEncoding;
                                            shortestDiff           = diffTruncated;
                                            shortestLength         = len;
                                            shortestValueEstimated = valueEstimated;
                                        }
                                    }
                                }
                            }
                        }

                        shortestValue.encode(ob);

                        switch (shortestValue)
                        {
                            case PositiveShortDelta:
                            case NegativeShortDelta:
                            case PositiveLongDelta:
                            case NegativeLongDelta:
                                shortestScale.encode(ob);
                                ob.emitFixedLength(shortestDiff, shortestLength - 3); // Subtract the length of the scale that we added before.

                                valuePrevious = shortestValueEstimated;
                                break;

                            case Delta:
                                shortestScale.encode(ob);
                                ob.emitSignedVariableLength(shortestDiff);

                                valuePrevious = shortestValueEstimated;
                                break;

                            case Absolute:
                                ob.emitDouble(value);

                                valuePrevious = value;
                                break;
                        }
                    }
                    break;
                }

                case BitSet:
                case EnumeratedSet:
                {
                    BitSet                  valuePrevious = new BitSet();
                    BitSet                  valueDelta    = new BitSet();
                    ExpandableArrayOfBitSet values        = m_valuesSet;

                    for (int position = 0; position < values.size(); position++)
                    {
                        BitSet value = values.get(position, null);

                        if (value == null)
                        {
                            ValueEncoding.Missing.encode(ob);
                            continue;
                        }

                        //
                        // Clone and compare differences.
                        //
                        valueDelta.clear();
                        valueDelta.or(valuePrevious);
                        valueDelta.xor(value);

                        if (valueDelta.isEmpty())
                        {
                            ValueEncoding.Same.encode(ob);
                            continue;
                        }

                        if (valueDelta.length() < value.length())
                        {
                            ValueEncoding.Delta.encode(ob);
                            ob.emitBitSet(valueDelta);
                        }
                        else
                        {
                            ValueEncoding.Absolute.encode(ob);
                            ob.emitBitSet(value);
                        }

                        valuePrevious = value;
                    }
                    break;
                }
            }
        }

        //--//

        private int encodeEnum(String key)
        {
            if (key == null)
            {
                return -1;
            }

            int pos = m_enumLookups.indexOf(key);
            if (pos < 0)
            {
                pos = m_enumLookups.size();
                if (pos == 0)
                {
                    m_enumLookups = Lists.newArrayList();
                }

                m_enumLookups.add(key);
                m_enumLookupsResolved = null;
            }

            return pos;
        }

        String decodeEnum(Object value)
        {
            Number num = Reflection.as(value, Number.class);
            if (num != null)
            {
                // 'v' could be "-1", which means "null". No need to check here, getNthElement will return null, since it's out of bound.
                int v = num.intValue();

                return CollectionUtils.getNthElement(m_enumLookups, v);
            }

            return null;
        }

        Object decodeEnum(Object value,
                          Class<?> clz)
        {
            String enumValue = decodeEnum(value);

            if (enumValue == null)
            {
                return null;
            }

            if (clz == String.class || clz == Object.class)
            {
                return enumValue;
            }

            return resolveEnum(enumValue, clz);
        }

        private <T extends Enum<T>> T resolveEnum(String enumValue,
                                                  Class<?> clz)
        {
            @SuppressWarnings("unchecked") Class<T> clzEnum = (Class<T>) clz;

            return Enum.valueOf(clzEnum, enumValue);
        }

        @SuppressWarnings("unchecked")
        private <T extends Enum<T>> T[] resolveEnums(Class<?> clz)
        {
            if (m_enumLookupsResolvedForClass == clz && m_enumLookupsResolved != null)
            {
                return (T[]) m_enumLookupsResolved;
            }

            Class<T> clzEnum = (Class<T>) clz;

            int num = m_enumLookups.size();
            T[] res = (T[]) Array.newInstance(clz, num);

            for (int i = 0; i < num; i++)
            {
                res[i] = Enum.valueOf(clzEnum, m_enumLookups.get(i));
            }

            m_enumLookupsResolved         = res;
            m_enumLookupsResolvedForClass = clz;

            return res;
        }

        //--//

        public int countMissing()
        {
            if (m_missing < 0)
            {
                int missing = 0;

                switch (type)
                {
                    case Integer:
                    case Enumerated: // These are encoded as integers.
                    {
                        ExpandableArrayOfLongs values = m_valuesLong;

                        for (int position = 0; position < values.size(); position++)
                        {
                            long value = values.get(position, Long.MAX_VALUE);

                            if (value == Long.MAX_VALUE)
                            {
                                missing++;
                            }
                        }
                        break;
                    }

                    case Decimal:
                    {
                        ExpandableArrayOfDoubles values = m_valuesDouble;

                        for (int position = 0; position < values.size(); position++)
                        {
                            double value = values.get(position, Double.NaN);
                            if (Double.isNaN(value))
                            {
                                missing++;
                            }
                        }
                        break;
                    }

                    case BitSet:
                    case EnumeratedSet:
                    {
                        ExpandableArrayOfBitSet values = m_valuesSet;

                        for (int position = 0; position < values.size(); position++)
                        {
                            BitSet valueRaw = values.get(position, null);

                            if (valueRaw == null)
                            {
                                missing++;
                            }
                        }
                        break;
                    }
                }

                m_missing = missing;
            }

            return m_missing;
        }

        public boolean hasValue(int position)
        {
            switch (type)
            {
                case Integer:
                case Enumerated:
                {
                    long valueTyped = m_valuesLong.get(position, Long.MAX_VALUE);
                    return valueTyped != Long.MAX_VALUE;
                }

                case Decimal:
                {
                    double valueTyped = m_valuesDouble.get(position, Double.NaN);
                    return !Double.isNaN(valueTyped);
                }

                case BitSet:
                case EnumeratedSet:
                {
                    BitSet valueTyped = m_valuesSet.get(position, null);
                    return valueTyped != null;
                }

                default:
                    return false;
            }
        }

        public <T> T getRaw(int position,
                            Class<T> clz)
        {
            Object value;

            switch (type)
            {
                case Integer:
                case Enumerated:
                {
                    ExpandableArrayOfLongs values     = m_valuesLong;
                    long                   valueTyped = values.get(position, Long.MAX_VALUE);
                    if (valueTyped == Long.MAX_VALUE)
                    {
                        value = null;
                    }
                    else
                    {
                        value = valueTyped;

                        if (clz == Long.class)
                        {
                            value = valueTyped;
                        }
                        else if (clz == Integer.class)
                        {
                            value = (int) valueTyped;
                        }
                        else if (clz != Object.class)
                        {
                            value = Reflection.coerceNumber(value, clz);
                        }
                    }
                    break;
                }

                case Decimal:
                {
                    ExpandableArrayOfDoubles values     = m_valuesDouble;
                    double                   valueTyped = values.get(position, Double.NaN);
                    if (Double.isNaN(valueTyped))
                    {
                        value = null;
                    }
                    else
                    {
                        value = valueTyped;

                        if (clz == Double.class)
                        {
                            value = valueTyped;
                        }
                        else if (clz == Float.class)
                        {
                            value = (float) valueTyped;
                        }
                        else if (clz != Object.class)
                        {
                            value = Reflection.coerceNumber(value, clz);
                        }
                    }
                    break;
                }

                case BitSet:
                case EnumeratedSet:
                {
                    ExpandableArrayOfBitSet values     = m_valuesSet;
                    BitSet                  valueTyped = values.get(position, null);

                    if (valueTyped == null)
                    {
                        value = null;
                    }
                    else
                    {
                        if (Reflection.isSubclassOf(TypedBitSet.class, clz))
                        {
                            value = Reflection.newInstance(clz, valueTyped);
                        }
                        else if (type == SampleType.EnumeratedSet)
                        {
                            String[] array     = new String[valueTyped.cardinality()];
                            int      outputPos = 0;

                            for (int indexOfMatch = valueTyped.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = valueTyped.nextSetBit(indexOfMatch + 1))
                            {
                                array[outputPos++] = m_enumLookups.get(indexOfMatch);
                            }

                            value = array;
                        }
                        else
                        {
                            value = valueTyped;
                        }
                    }
                    break;
                }

                default:
                    value = null;
                    break;
            }

            return clz.cast(value);
        }

        public <T> T get(int position,
                         Class<T> clz)
        {
            if (type == SampleType.Enumerated)
            {
                Long index = getRaw(position, Long.class);
                if (index == null)
                {
                    return null;
                }

                if (clz == Object.class)
                {
                    return clz.cast(decodeEnum(index));
                }

                long      index2 = index;
                Enum<?>[] lookup = resolveEnums(clz);

                return index2 >= 0 && index2 < lookup.length ? clz.cast(lookup[(int) index2]) : null;
            }

            return clz.cast(getRaw(position, clz));
        }

        void set(int position,
                 Object value)
        {
            m_missing = -1;

            switch (type)
            {
                case Enumerated:
                    if (value != null)
                    {
                        String keyEncoded;

                        if (value instanceof Enum)
                        {
                            Enum<?> key2 = (Enum<?>) value;
                            keyEncoded = key2.name();
                        }
                        else
                        {
                            keyEncoded = value.toString();
                        }

                        value = encodeEnum(keyEncoded);
                    }
                    // Fallthrough...

                case Integer:
                {
                    ExpandableArrayOfLongs values     = m_valuesLong;
                    Long                   valueTyped = Reflection.coerceNumber(value, Long.class);

                    values.set(position, BoxingUtils.get(valueTyped, Long.MAX_VALUE));
                    break;
                }

                case Decimal:
                {
                    ExpandableArrayOfDoubles values     = m_valuesDouble;
                    Double                   valueTyped = Reflection.coerceNumber(value, Double.class);

                    values.set(position, BoxingUtils.get(valueTyped, Double.NaN));
                    break;
                }

                case BitSet:
                {
                    ExpandableArrayOfBitSet values     = m_valuesSet;
                    BitSet                  valueTyped = TypedBitSet.extract(value);

                    values.set(position, valueTyped != null ? BitSets.copy(valueTyped) : null);
                    break;
                }

                case EnumeratedSet:
                {
                    ExpandableArrayOfBitSet values = m_valuesSet;

                    if (value instanceof String[])
                    {
                        String[] set = (String[]) value;
                        BitSet   bs  = new BitSet();

                        for (String s : set)
                        {
                            bs.set(encodeEnum(s));
                        }

                        values.set(position, bs);
                    }
                    else
                    {
                        values.set(position, null);
                    }

                    break;
                }
            }
        }

        <T> void extractSamples(TimeSeriesExtract<T> res,
                                boolean skipMissingValues,
                                ZonedDateTime rangeStart,
                                ZonedDateTime rangeEnd)
        {
            ExpandableArrayOfDoubles timestamps = m_holder.m_timestamps;

            //
            // Locate range start.
            //
            int posStart;

            if (rangeStart != null)
            {
                posStart = timestamps.binarySearch(TimeUtils.fromUtcTimeToTimestamp(rangeStart));

                if (posStart < 0)
                {
                    posStart = ~posStart;
                }
            }
            else
            {
                posStart = 0;
            }

            //
            // Locate range end.
            //
            int posEnd;

            if (rangeEnd != null)
            {
                posEnd = timestamps.binarySearch(TimeUtils.fromUtcTimeToTimestamp(rangeEnd));

                if (posEnd < 0)
                {
                    posEnd = ~posEnd;
                }
                else
                {
                    // The element at posEnd matches the rangeEnd, so we need to include it.
                    posEnd++;
                }
            }
            else
            {
                posEnd = timestamps.size();
            }

            //
            // Only append values, so skip all the samples with a timestamp already in the extract.
            //
            if (res.size() > 0)
            {
                double lastTimestamp = res.getLastTimestamp();
                while (posStart < posEnd && lastTimestamp >= timestamps.get(posStart, 0))
                {
                    posStart++;
                }
            }

            res.prepareForGrowth(posEnd - posStart);

            try (var batch = res.prepareBatch())
            {
                switch (type)
                {
                    case Integer:
                    {
                        ExpandableArrayOfLongs values = m_valuesLong;

                        for (int pos = posStart; pos < posEnd; pos++)
                        {
                            double timestamp  = timestamps.get(pos, 0);
                            long   valueTyped = values.get(pos, Long.MAX_VALUE);
                            double outputValue;

                            if (valueTyped == Long.MAX_VALUE)
                            {
                                if (skipMissingValues)
                                {
                                    continue;
                                }

                                outputValue = Double.NaN;
                            }
                            else
                            {
                                outputValue = valueTyped;
                            }

                            batch.add(timestamp, outputValue);
                        }
                    }
                    break;

                    case Enumerated:
                    {
                        ExpandableArrayOfLongs values = m_valuesLong;
                        int[]                  lookup = res.prepareEnumLookup(m_enumLookups);

                        for (int pos = posStart; pos < posEnd; pos++)
                        {
                            double timestamp  = timestamps.get(pos, 0);
                            long   valueTyped = values.get(pos, Long.MAX_VALUE);
                            double outputValue;

                            if (valueTyped < 0 || valueTyped >= lookup.length)
                            {
                                if (skipMissingValues)
                                {
                                    continue;
                                }

                                outputValue = Double.NaN;
                            }
                            else
                            {
                                outputValue = lookup[(int) valueTyped];
                            }

                            batch.add(timestamp, outputValue);
                        }
                    }
                    break;

                    case Decimal:
                    {
                        ExpandableArrayOfDoubles values = m_valuesDouble;

                        for (int pos = posStart; pos < posEnd; pos++)
                        {
                            double timestamp  = timestamps.get(pos, 0);
                            double valueTyped = values.get(pos, Double.NaN);
                            double outputValue;

                            if (Double.isNaN(valueTyped))
                            {
                                if (skipMissingValues)
                                {
                                    continue;
                                }

                                outputValue = Double.NaN;
                            }
                            else
                            {
                                outputValue = valueTyped;
                            }

                            batch.add(timestamp, outputValue);
                        }
                    }
                    break;

                    case BitSet:
                    {
                        ExpandableArrayOfBitSet values = m_valuesSet;

                        for (int pos = posStart; pos < posEnd; pos++)
                        {
                            double timestamp  = timestamps.get(pos, 0);
                            BitSet valueTyped = values.get(pos, null);
                            double outputValue;

                            if (valueTyped == null)
                            {
                                if (skipMissingValues)
                                {
                                    continue;
                                }

                                outputValue = Double.NaN;
                            }
                            else
                            {
                                outputValue = res.mapBitset(valueTyped);
                            }

                            batch.add(timestamp, outputValue);
                        }
                    }
                    break;

                    case EnumeratedSet:
                    {
                        ExpandableArrayOfBitSet values = m_valuesSet;
                        int[]                   lookup = res.prepareEnumLookup(m_enumLookups);

                        for (int pos = posStart; pos < posEnd; pos++)
                        {
                            double timestamp  = timestamps.get(pos, 0);
                            BitSet valueTyped = values.get(pos, null);
                            double outputValue;

                            if (valueTyped == null)
                            {
                                if (skipMissingValues)
                                {
                                    continue;
                                }

                                outputValue = Double.NaN;
                            }
                            else
                            {
                                outputValue = res.mapEnumSet(lookup, valueTyped);
                            }

                            batch.add(timestamp, outputValue);
                        }
                    }
                    break;
                }

                batch.flush();
            }
        }
    }

    //--//

    static class State implements AutoCloseable
    {
        private final ExpandableArrayOfDoubles  m_timestamps = ExpandableArrayOfDoubles.create();
        private       SampleSchema[]            m_schema     = new SampleSchema[10];
        private       int                       m_schemaInUse;
        private       Map<String, SampleSchema> m_schemaLookup;

        private boolean m_modified;
        private boolean m_readonly;

        State(Version version,
              InputBitBuffer ib)
        {
            if (ib != null)
            {
                switch (version)
                {
                    case Legacy:
                    case V1:
                    {
                        //
                        // Extract the schema.
                        //
                        int              schemaLen  = (int) ib.readUnsignedVariableLength();
                        SampleResolution resolution = SampleResolution.Max1Hz;

                        for (int schemaIdx = 0; schemaIdx < schemaLen; schemaIdx++)
                        {
                            SampleSchema schema = new SampleSchema(this, ib);
                            linkSchema(schema);

                            resolution = resolution.maxResolution(schema.m_resolution);
                        }

                        //
                        // First allocate the snapshots and decode the timestamps
                        //
                        TimestampEncoding.Delta.decodeValues(ib, m_timestamps, resolution.details.inverseScalingFactor);
                        break;
                    }

                    case V2:
                    {
                        //
                        // Extract the schema.
                        //
                        int              schemaLen  = (int) ib.readUnsignedVariableLength();
                        SampleResolution resolution = SampleResolution.Max1Hz;

                        for (int schemaIdx = 0; schemaIdx < schemaLen; schemaIdx++)
                        {
                            SampleSchema schema = new SampleSchema(this, ib);
                            linkSchema(schema);

                            resolution = resolution.maxResolution(schema.m_resolution);
                        }

                        TimestampEncoding timestampEncoding = TimestampEncoding.decode(ib);
                        timestampEncoding.decodeValues(ib, m_timestamps, resolution.details.inverseScalingFactor);
                        break;
                    }

                    default:
                        throw Exceptions.newIllegalArgumentException("Unsupported version '%s'", version);
                }

                //
                // Then read all the values for a property, one property at a time.
                //
                for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema = m_schema[schemaIndex];

                    schema.decodeValues(ib, m_timestamps.size());
                }

                //
                // In case we had some duplicates, remove them.
                //
                for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema  = m_schema[schemaIndex];
                    SampleSchema schema2 = findSchema(schema.identifier);

                    if (schema2 != schema)
                    {
                        // Found a duplicate, remove it.
                        removeSchema(schema2);
                        schemaIndex--;
                    }
                }
            }
        }

        @Override
        public void close()
        {
            m_timestamps.close();

            for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
            {
                SampleSchema schema = m_schema[schemaIndex];

                schema.close();

                m_schema[schemaIndex] = null;
            }

            m_schemaInUse = 0;
        }

        public OutputBitBuffer encode()
        {
            OutputBitBuffer ob = new OutputBitBuffer();

            int numberOfTimestamps = m_timestamps.size();
            if (numberOfTimestamps > 0)
            {
                //
                // Save the schema.
                //
                ob.emitUnsignedVariableLength(m_schemaInUse);

                SampleResolution resolution = SampleResolution.Max1Hz;

                for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema = m_schema[schemaIndex];

                    schema.encodeMetadata(ob);

                    resolution = resolution.maxResolution(schema.m_resolution);
                }

                //
                // Encode the timestamps.
                //
                try (ExpandableArrayOfLongs timestamps = ExpandableArrayOfLongs.create())
                {
                    timestamps.grow(numberOfTimestamps);

                    //
                    // Scale and convert all timestamps to integer.
                    //
                    {
                        double scalingFactor = resolution.details.scalingFactor;

                        for (int timestampIndex = 0; timestampIndex < numberOfTimestamps; timestampIndex++)
                        {
                            timestamps.set(timestampIndex, (long) (m_timestamps.get(timestampIndex, 0) * scalingFactor));
                        }
                    }

                    OutputBitBuffer   obTimestamp = null;
                    TimestampEncoding enTimestamp = null;

                    for (TimestampEncoding timestampEncoding : TimestampEncoding.values())
                    {
                        OutputBitBuffer obSub = timestampEncoding.encodeValues(timestamps);
                        if (obSub != null)
                        {
                            if (LoggerInstance.isEnabled(Severity.DebugObnoxious))
                            {
                                InputBitBuffer ibSub = new InputBitBuffer(obSub);
                                if (timestampEncoding != TimestampEncoding.decode(ibSub))
                                {
                                    throw new RuntimeException("Encoding header error");
                                }

                                try (ExpandableArrayOfDoubles timestamps2 = ExpandableArrayOfDoubles.create())
                                {
                                    timestampEncoding.decodeValues(ibSub, timestamps2, resolution.details.inverseScalingFactor);
                                    if (!Arrays.equals(m_timestamps.toArray(), timestamps2.toArray()))
                                    {
                                        throw new RuntimeException("Encoding values error");
                                    }
                                }
                            }

                            if (obTimestamp == null || obTimestamp.sizeInBits() > obSub.sizeInBits())
                            {
                                if (LoggerInstance.isEnabled(Severity.DebugVerbose) && enTimestamp != null)
                                {
                                    LoggerInstance.debugVerbose("Switched from %s to %s, %,d -> %,d bits\n", enTimestamp, timestampEncoding, obTimestamp.sizeInBits(), obSub.sizeInBits());
                                }

                                if (obTimestamp != null)
                                {
                                    obTimestamp.close();
                                }

                                obTimestamp = obSub;
                                enTimestamp = timestampEncoding;
                            }
                            else
                            {
                                obSub.close();

                                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                                {
                                    LoggerInstance.debugVerbose("Ignored %s, %,d -> %,d bits\n", timestampEncoding, obTimestamp.sizeInBits(), obSub.sizeInBits());
                                }
                            }
                        }
                    }

                    if (obTimestamp == null)
                    {
                        throw new RuntimeException("No timestamp encoding available!!");
                    }

                    if (LoggerInstance.isEnabled(Severity.Debug))
                    {
                        LoggerInstance.debug("Selected %s, %,d bits\n", enTimestamp, obTimestamp.sizeInBits());
                    }

                    ob.emitNested(obTimestamp);

                    obTimestamp.close();
                }

                //
                // Then write all the values for a property, one property at a time.
                //
                for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema = m_schema[schemaIndex];

                    schema.encodeValues(ob);
                }
            }

            return ob;
        }

        //--//

        State copy()
        {
            State s = new State(null, null);

            s.m_timestamps.fromArray(m_timestamps.toArray());

            s.m_modified = m_modified;
            s.m_readonly = m_readonly;

            for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
            {
                SampleSchema schema = m_schema[schemaIndex];

                SampleSchema schema2 = new SampleSchema(s, schema);
                s.linkSchema(schema2);
            }

            return s;
        }

        SampleSchema newSchema(String prop,
                               SampleType sampleType)
        {
            SampleSchema schema = new SampleSchema(this, prop, sampleType);
            schema.ensureLength();

            linkSchema(schema);

            return schema;
        }

        SampleSchema findSchema(String identifier)
        {
            if (m_schemaLookup == null)
            {
                if (m_schemaInUse < 10)
                {
                    for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                    {
                        SampleSchema schema = m_schema[schemaIndex];

                        if (schema.identifier.equals(identifier))
                        {
                            return schema;
                        }
                    }

                    return null;
                }

                ensureSchemaLookup();
            }

            return m_schemaLookup.get(identifier);
        }

        void ensureSchemaLookup()
        {
            if (m_schemaLookup == null)
            {
                Map<String, SampleSchema> lookup = Maps.newHashMap();

                for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema = m_schema[schemaIndex];
                    lookup.put(schema.identifier, schema);
                }

                m_schemaLookup = lookup;
            }
        }

        void linkSchema(SampleSchema schema)
        {
            if (m_schemaInUse >= m_schema.length)
            {
                m_schema = Arrays.copyOf(m_schema, m_schemaInUse * 2);
            }

            m_schemaLookup            = null;
            m_schema[m_schemaInUse++] = schema;
        }

        void removeSchema(SampleSchema schema)
        {
            for (int pos = 0; pos < m_schemaInUse; pos++)
            {
                if (m_schema[pos] == schema)
                {
                    removeSchemaAt(pos);
                    return;
                }
            }
        }

        void removeSchemaAt(int schemaIndex)
        {
            while (++schemaIndex < m_schemaInUse)
            {
                m_schema[schemaIndex - 1] = m_schema[schemaIndex];
            }

            m_schemaLookup            = null;
            m_schema[--m_schemaInUse] = null;
        }

        //--//

        public void addSample(SampleResolution resolution,
                              double timestampEpochSeconds,
                              String prop,
                              SampleType sampleType,
                              int digitsOfPrecision,
                              Object value)
        {
            if (prop == null)
            {
                // We just wanted to have an entry at this timestamp. We are done.
                locateSnapshot(timestampEpochSeconds, false, false, true);
                return;
            }

            SampleSchema schema = findCompatibleSchema(prop, resolution, sampleType, digitsOfPrecision);

            timestampEpochSeconds = schema.truncateTimestamp(timestampEpochSeconds);

            int target = locateSnapshot(timestampEpochSeconds, false, false, true);
            schema.set(target, value);
        }

        public SampleSchema findCompatibleSchema(String prop,
                                                 SampleResolution resolution,
                                                 SampleType sampleType,
                                                 int digitsOfPrecision)
        {
            SampleSchema schema = findSchema(prop);
            if (schema == null)
            {
                schema = newSchema(prop, sampleType);
            }
            else if (schema.type != sampleType)
            {
                //
                // Allow some upgrade/downgrade of schema.
                //
                if (schema.type == SampleType.Integer && sampleType == SampleType.Decimal)
                {
                    SampleSchema schema2 = newSchema(prop, sampleType);

                    ExpandableArrayOfLongs   src = schema.m_valuesLong;
                    ExpandableArrayOfDoubles dst = schema2.m_valuesDouble;

                    for (int i = 0; i < src.size(); i++)
                    {
                        dst.set(i, src.get(i, 0));
                    }

                    removeSchema(schema);
                    schema = schema2;
                }
                else if (schema.type == SampleType.Decimal && sampleType == SampleType.Integer)
                {
                    SampleSchema schema2 = newSchema(prop, sampleType);

                    ExpandableArrayOfDoubles src = schema.m_valuesDouble;
                    ExpandableArrayOfLongs   dst = schema2.m_valuesLong;

                    for (int i = 0; i < src.size(); i++)
                    {
                        dst.set(i, (long) src.get(i, 0));
                    }

                    removeSchema(schema);
                    schema = schema2;
                }
            }

            schema.setResolution(resolution);
            schema.setMinimumPrecision(digitsOfPrecision);
            return schema;
        }

        int locateSnapshot(double epochSeconds,
                           boolean findNearest,
                           boolean onlyBeforeTarget,
                           boolean createIfMissing)
        {
            if (createIfMissing)
            {
                //
                // Quick check if the target snapshot is also the newest.
                //
                int    lastPos      = m_timestamps.size() - 1;
                double lastSnapshot = m_timestamps.get(lastPos, TimeUtils.maxEpochSeconds());
                if (lastPos < 0 || lastSnapshot < epochSeconds)
                {
                    return insertNewTimestamp(lastPos + 1, epochSeconds);
                }

                //
                // ... or the last one.
                //
                if (lastSnapshot == epochSeconds)
                {
                    return lastPos;
                }
            }

            int pos = m_timestamps.binarySearch(epochSeconds);
            if (pos >= 0)
            {
                return pos;
            }

            int insertionPoint = ~pos;

            if (createIfMissing)
            {
                return insertNewTimestamp(insertionPoint, epochSeconds);
            }

            if (!findNearest)
            {
                return -1;
            }

            int leftPosition  = insertionPoint - 1;
            int rightPosition = insertionPoint;

            double left  = m_timestamps.get(leftPosition, TimeUtils.maxEpochSeconds());
            double right = m_timestamps.get(rightPosition, TimeUtils.maxEpochSeconds());

            boolean validLeft  = TimeUtils.isValid(left);
            boolean validRight = TimeUtils.isValid(right);

            if (validLeft)
            {
                if (onlyBeforeTarget || !validRight)
                {
                    return leftPosition;
                }

                double leftDiff  = epochSeconds - left;
                double rightDiff = right - epochSeconds;

                return leftDiff < rightDiff ? leftPosition : rightPosition;
            }
            else
            {
                if (onlyBeforeTarget || !validRight)
                {
                    return -1;
                }

                return rightPosition;
            }
        }

        private int insertNewTimestamp(int position,
                                       double epochSeconds)
        {
            m_timestamps.insert(position, epochSeconds);

            for (int schemaIndex = 0; schemaIndex < m_schemaInUse; schemaIndex++)
            {
                SampleSchema schema = m_schema[schemaIndex];

                schema.ensureLength();
            }

            return position;
        }
    }

    private static final Version c_targetVersion = Version.V2;

    private final Version m_version;
    private       Encoded m_encoded;
    private       State   m_decoded;

    //--//

    private TimeSeries(Version version,
                       Encoded encoded,
                       State state)
    {
        m_version = version;
        m_encoded = encoded;
        m_decoded = state;
    }

    @Override
    public void close()
    {
        if (m_encoded != null)
        {
            m_encoded.close();
            m_encoded = null;
        }

        if (m_decoded != null)
        {
            m_decoded.close();
            m_decoded = null;
        }
    }

    public static boolean isKnownSize(int length)
    {
        int lengthRounded = (length / c_archiveSizeRounding) * c_archiveSizeRounding;
        switch (lengthRounded)
        {
            case c_smallArchiveSize:
            case c_mediumArchiveSize:
            case c_maxArchiveSize:
            case c_maxArchiveSize + c_archiveSizeRounding: // Allow for spillover on last size.
                return true;

            default:
                return false;
        }
    }

    public static TimeSeries newInstance()
    {
        return decode(null, null);
    }

    public TimeSeries createCopy()
    {
        return new TimeSeries(m_version, null, m_decoded.copy());
    }

    public Version getSourceVersion()
    {
        return m_version;
    }

    public Encoded encode()
    {
        ensureEncoded();

        return m_encoded;
    }

    private void ensureEncoded()
    {
        if (m_encoded == null)
        {
            try (OutputBitBuffer blob = encodeUncompressed())
            {
                if (blob.sizeInBits() > 0)
                {
                    try (OutputBuffer compressed = blob.compress())
                    {
                        int lengthUncompressed = blob.sizeInBytes();
                        int lengthCompressed   = compressed.size();

                        OutputBuffer ob = new OutputBuffer();
                        ob.emit1Byte('T');
                        ob.emit1Byte('S');
                        ob.emit1Byte('V');
                        ob.emit1Byte(c_targetVersion.encoding);
                        ob.emit4Bytes(lengthCompressed);
                        ob.emit4Bytes(lengthUncompressed);

                        ob.emitNestedBlock(compressed);

                        int totalLength = ob.size();
                        int roundedLength;

                        if (totalLength < c_smallArchiveSize)
                        {
                            roundedLength = c_smallArchiveSize;
                        }
                        else if (totalLength < c_mediumArchiveSize)
                        {
                            roundedLength = c_mediumArchiveSize;
                        }
                        else if (totalLength < c_maxArchiveSize)
                        {
                            roundedLength = c_maxArchiveSize;
                        }
                        else
                        {
                            // Make it a multiple of the rounding size.
                            roundedLength = ((totalLength + c_archiveSizeRounding - 1) / c_archiveSizeRounding) * c_archiveSizeRounding;
                        }

                        m_encoded = new Encoded(ob, roundedLength);
                    }
                }
            }
        }
    }

    public static TimeSeries decode(Version version,
                                    InputBitBuffer ib)
    {
        if (version == null)
        {
            version = c_targetVersion;
        }

        return new TimeSeries(version, null, new State(version, ib));
    }

    public static TimeSeries decode(byte[] contents)
    {
        if (contents == null)
        {
            return null;
        }

        try (InputBuffer ib = InputBuffer.createFrom(contents))
        {
            Version version = Version.parse(ib);
            switch (version)
            {
                case Legacy:
                {
                    ib.setPosition(0);

                    InputBitBuffer ibUncompressed = new InputBitBuffer(ib);
                    return decode(version, ibUncompressed);
                }

                case V1:
                case V2:
                {
                    int lengthCompressed   = ib.read4BytesSigned();
                    int lengthUncompressed = ib.read4BytesSigned(); // Legacy field, still need to read it...

                    int paddedLength   = ib.size();
                    int unpaddedLength = ib.getPosition() + lengthCompressed;

                    try (ExpandableArrayOfBytes compressed = ExpandableArrayOfBytes.create())
                    {
                        compressed.addRange(contents, ib.getPosition(), lengthCompressed);

                        ExpandableArrayOfBytes uncompressed = ExpandableArrayOfBytes.create();
                        compressed.decompressTo(uncompressed);

                        try (InputBitBuffer ibUncompressed = new InputBitBuffer(InputBuffer.takeOwnership(uncompressed)))
                        {
                            TimeSeries ts = decode(version, ibUncompressed);

                            OutputBuffer ob = new OutputBuffer();
                            ob.emit(contents, 0, unpaddedLength);
                            ts.m_encoded = new Encoded(ob, paddedLength);
                            return ts;
                        }
                    }
                }

                default:
                    return null;
            }
        }
    }

    //--//

    public boolean shouldUpgrade()
    {
        return m_version != c_targetVersion;
    }

    public OutputBitBuffer encodeUncompressed()
    {
        return m_decoded.encode();
    }

    public boolean wasModified()
    {
        return m_decoded.m_modified;
    }

    public void resetModified()
    {
        m_decoded.m_modified = false;
    }

    public void setReadonly()
    {
        m_decoded.m_readonly = true;
    }

    public int numberOfProperties()
    {
        return m_decoded.m_schemaInUse;
    }

    public int numberOfSamples()
    {
        return m_decoded.m_timestamps.size();
    }

    public boolean hasTooManySamples()
    {
        //
        // Even if an archive would hold more samples, best not to put too many samples in one archive.
        //
        return numberOfSamples() > 20_000;
    }

    //--//

    public void addSample(SampleResolution resolution,
                          ZonedDateTime timestamp,
                          String prop,
                          SampleType sampleType,
                          int digitsOfPrecision,
                          Object value)
    {
        addSample(resolution, TimeUtils.fromUtcTimeToTimestamp(timestamp), prop, sampleType, digitsOfPrecision, value);
    }

    public void addSample(SampleResolution resolution,
                          double timestampEpochSeconds,
                          String prop,
                          SampleType sampleType,
                          int digitsOfPrecision,
                          Object value)
    {
        ensureWritableState().addSample(resolution, timestampEpochSeconds, prop, sampleType, digitsOfPrecision, value);
    }

    private State ensureWritableState()
    {
        State state = m_decoded;

        if (state.m_readonly)
        {
            throw new RuntimeException("Archive is read-only");
        }

        state.m_modified = true;

        if (m_encoded != null)
        {
            m_encoded.close();
            m_encoded = null;
        }

        return state;
    }

    public double getStartTimestamp()
    {
        return m_decoded.m_timestamps.get(0, TimeUtils.maxEpochSeconds());
    }

    public double getEndTimestamp()
    {
        return m_decoded.m_timestamps.get(m_decoded.m_timestamps.size() - 1, TimeUtils.maxEpochSeconds());
    }

    public ExpandableArrayOfDoubles getTimeStampsAsEpochSeconds()
    {
        return m_decoded.m_timestamps;
    }

    public List<ZonedDateTime> getTimeStamps()
    {
        int             numberOfTimestamps = m_decoded.m_timestamps.size();
        ZonedDateTime[] timestamps         = new ZonedDateTime[numberOfTimestamps];

        for (int position = 0; position < numberOfTimestamps; position++)
        {
            timestamps[position] = TimeUtils.fromTimestampToUtcTime(m_decoded.m_timestamps.get(position, TimeUtils.maxEpochSeconds()));
        }

        return Lists.newArrayList(timestamps);
    }

    //--//

    public List<SampleSchema> getSchema()
    {
        List<SampleSchema> lst = Lists.newArrayList();

        for (int schemaIndex = 0; schemaIndex < m_decoded.m_schemaInUse; schemaIndex++)
        {
            lst.add(m_decoded.m_schema[schemaIndex]);
        }

        return lst;
    }

    public void copySnapshots(int startOffset,
                              int endOffset,
                              TimeSeries dst)
    {
        State srcState = m_decoded;
        State dstState = dst.ensureWritableState();

        startOffset = BoxingUtils.bound(startOffset, 0, srcState.m_timestamps.size() - 1);
        endOffset   = BoxingUtils.bound(endOffset, 0, srcState.m_timestamps.size());

        double noValue = TimeUtils.maxEpochSeconds();

        int numTimestamps = endOffset - startOffset;
        if (numTimestamps > 0)
        {
            SampleResolution resolution = null;
            SampleSchema[]   dstSchemas = new SampleSchema[srcState.m_schemaInUse];

            for (int schemaIndex = 0; schemaIndex < srcState.m_schemaInUse; schemaIndex++)
            {
                SampleSchema srcSchema = srcState.m_schema[schemaIndex];
                SampleSchema dstSchema = dstState.findCompatibleSchema(srcSchema.identifier, srcSchema.m_resolution, srcSchema.type, srcSchema.m_minimumPrecision);

                resolution = dstSchema.m_resolution.maxResolution(resolution);

                dstSchemas[schemaIndex] = dstSchema;
            }

            int[] timestamps = new int[numTimestamps];

            //
            // First create all the timestamps. Even if we don't have any values, timestamps will mark missing data.
            //
            for (int pos = 0; pos < numTimestamps; pos++)
            {
                double timestamp = srcState.m_timestamps.get(pos + startOffset, noValue);

                if (resolution != null)
                {
                    timestamp = resolution.details.truncateTimestamp(timestamp);
                }

                timestamps[pos] = dstState.locateSnapshot(timestamp, false, false, true);
            }

            for (int schemaIndex = 0; schemaIndex < srcState.m_schemaInUse; schemaIndex++)
            {
                SampleSchema srcSchema = srcState.m_schema[schemaIndex];
                SampleSchema dstSchema = dstSchemas[schemaIndex];

                boolean processed = false;

                if (srcSchema.type == dstSchema.type)
                {
                    switch (srcSchema.type)
                    {
                        case Enumerated:
                        {
                            ExpandableArrayOfLongs srcValues = srcSchema.m_valuesLong;
                            ExpandableArrayOfLongs dstValues = dstSchema.m_valuesLong;

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                int target = timestamps[pos];

                                long value = srcValues.get(pos + startOffset, Long.MAX_VALUE);

                                if (value != Long.MAX_VALUE)
                                {
                                    String valueEnum = CollectionUtils.getNthElement(srcSchema.m_enumLookups, (int) value);

                                    value = dstSchema.encodeEnum(valueEnum);
                                }

                                dstValues.set(target, value);
                            }

                            processed = true;
                            break;
                        }

                        case Integer:
                        {
                            ExpandableArrayOfLongs srcValues = srcSchema.m_valuesLong;
                            ExpandableArrayOfLongs dstValues = dstSchema.m_valuesLong;

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                int target = timestamps[pos];

                                dstValues.set(target, srcValues.get(pos + startOffset, Long.MAX_VALUE));
                            }

                            processed = true;
                            break;
                        }

                        case Decimal:
                        {
                            ExpandableArrayOfDoubles srcValues = srcSchema.m_valuesDouble;
                            ExpandableArrayOfDoubles dstValues = dstSchema.m_valuesDouble;

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                int target = timestamps[pos];

                                dstValues.set(target, srcValues.get(pos + startOffset, Double.NaN));
                            }

                            processed = true;
                            break;
                        }

                        case BitSet:
                        {
                            SampleSchema.ExpandableArrayOfBitSet srcValues = srcSchema.m_valuesSet;
                            SampleSchema.ExpandableArrayOfBitSet dstValues = dstSchema.m_valuesSet;

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                int target = timestamps[pos];

                                dstValues.set(target, srcValues.get(pos + startOffset, null));
                            }

                            processed = true;
                            break;
                        }
                    }
                }

                if (!processed)
                {
                    for (int pos = 0; pos < numTimestamps; pos++)
                    {
                        int target = timestamps[pos];

                        Object value = srcSchema.get(pos + startOffset, Object.class);
                        dstSchema.set(target, value);
                    }
                }
            }
        }
    }

    public void removeCloseIdenticalValues(double maxTimeSeparation)
    {
        State                    state         = m_decoded;
        ExpandableArrayOfDoubles timestamps    = state.m_timestamps;
        int                      numTimestamps = timestamps.size();

        if (numTimestamps > 2)
        {
            boolean[] keepTimestamps = new boolean[numTimestamps];

            // We always keep the first and last timestamps;
            keepTimestamps[0]                 = true;
            keepTimestamps[numTimestamps - 1] = true;

            for (int schemaIndex = 0; schemaIndex < state.m_schemaInUse; schemaIndex++)
            {
                SampleSchema schema = state.m_schema[schemaIndex];

                ExpandableArrayOfDoubles valuesDouble = schema.m_valuesDouble;
                if (valuesDouble != null)
                {
                    double valueA = valuesDouble.get(0, Long.MAX_VALUE);

                    for (int pos = 1; pos < numTimestamps; pos++)
                    {
                        double valueB = valuesDouble.get(pos, Double.MAX_VALUE);

                        if (valueA != valueB)
                        {
                            keepTimestamps[pos - 1] = true;
                            keepTimestamps[pos]     = true;
                        }

                        valueA = valueB;
                    }
                }

                ExpandableArrayOfLongs valuesLong = schema.m_valuesLong;
                if (valuesLong != null)
                {
                    long valueA = valuesLong.get(0, Long.MAX_VALUE);

                    for (int pos = 1; pos < numTimestamps; pos++)
                    {
                        long valueB = valuesLong.get(pos, Long.MAX_VALUE);

                        if (valueA != valueB)
                        {
                            keepTimestamps[pos - 1] = true;
                            keepTimestamps[pos]     = true;
                        }

                        valueA = valueB;
                    }
                }

                SampleSchema.ExpandableArrayOfBitSet valuesSet = schema.m_valuesSet;
                if (valuesSet != null)
                {
                    BitSet valueA = valuesSet.get(0, null);

                    for (int pos = 1; pos < numTimestamps; pos++)
                    {
                        BitSet valueB = valuesSet.get(pos, null);

                        if (!Objects.equals(valueA, valueB))
                        {
                            keepTimestamps[pos - 1] = true;
                            keepTimestamps[pos]     = true;
                        }

                        valueA = valueB;
                    }
                }
            }

            double timestampA         = timestamps.get(0, Double.MAX_VALUE);
            int    timestampsToDelete = 0;

            for (int pos = 1; pos < numTimestamps; pos++)
            {
                double timestampB = timestamps.get(pos, Double.MAX_VALUE);

                if (!keepTimestamps[pos])
                {
                    // Gap between samples small enough, we can remove the snapshot.
                    if ((timestampB - timestampA) < maxTimeSeparation)
                    {
                        timestampsToDelete++;
                        continue;
                    }

                    keepTimestamps[pos] = true;
                }

                timestampA = timestampB;
            }

            if (timestampsToDelete > 0)
            {
                state = ensureWritableState();

                try (ExpandableArrayOfDoubles timestampsNew = ExpandableArrayOfDoubles.create())
                {
                    timestampsNew.prepareForGrowth(numTimestamps - timestampsToDelete);

                    for (int pos = 0; pos < numTimestamps; pos++)
                    {
                        if (keepTimestamps[pos])
                        {
                            timestampsNew.add(timestamps.get(pos, Double.MAX_VALUE));
                        }
                    }

                    timestamps.clear();
                    timestampsNew.copyTo(0, timestampsNew.size(), timestamps, 0);
                }

                for (int schemaIndex = 0; schemaIndex < state.m_schemaInUse; schemaIndex++)
                {
                    SampleSchema schema = state.m_schema[schemaIndex];

                    ExpandableArrayOfDoubles valuesDouble = schema.m_valuesDouble;
                    if (valuesDouble != null)
                    {
                        try (ExpandableArrayOfDoubles valuesDoubleNew = ExpandableArrayOfDoubles.create())
                        {
                            valuesDoubleNew.prepareForGrowth(numTimestamps - timestampsToDelete);

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                if (keepTimestamps[pos])
                                {
                                    valuesDoubleNew.add(valuesDouble.get(pos, Double.MAX_VALUE));
                                }
                            }

                            valuesDouble.clear();
                            valuesDoubleNew.copyTo(0, valuesDoubleNew.size(), valuesDouble, 0);
                        }
                    }

                    ExpandableArrayOfLongs valuesLong = schema.m_valuesLong;
                    if (valuesLong != null)
                    {
                        try (ExpandableArrayOfLongs valuesLongNew = ExpandableArrayOfLongs.create())
                        {
                            valuesLongNew.prepareForGrowth(numTimestamps - timestampsToDelete);

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                if (keepTimestamps[pos])
                                {
                                    valuesLongNew.add(valuesLong.get(pos, Long.MAX_VALUE));
                                }
                            }

                            valuesLong.clear();
                            valuesLongNew.copyTo(0, valuesLongNew.size(), valuesLong, 0);
                        }
                    }

                    SampleSchema.ExpandableArrayOfBitSet valuesSet = schema.m_valuesSet;
                    if (valuesSet != null)
                    {
                        try (SampleSchema.ExpandableArrayOfBitSet valuesSetNew = new SampleSchema.ExpandableArrayOfBitSet())
                        {
                            valuesSetNew.prepareForGrowth(numTimestamps - timestampsToDelete);

                            for (int pos = 0; pos < numTimestamps; pos++)
                            {
                                if (keepTimestamps[pos])
                                {
                                    valuesSetNew.add(valuesSet.get(pos, null));
                                }
                            }

                            valuesSet.clear();
                            valuesSetNew.copyTo(0, valuesSetNew.size(), valuesSet, 0);
                        }
                    }
                }
            }
        }
    }

    public <T> T getSample(int index,
                           String prop,
                           Class<T> clz)
    {
        SampleSchema schema = m_decoded.findSchema(prop);
        if (schema == null)
        {
            return null;
        }

        return schema.get(index, clz);
    }

    public <T> T getSample(ZonedDateTime timestamp,
                           String prop,
                           boolean findNearest,
                           boolean onlyBeforeTarget,
                           Class<T> clz)
    {
        SampleSchema schema = m_decoded.findSchema(prop);
        if (schema == null)
        {
            return null;
        }

        double epochSeconds = TimeUtils.fromUtcTimeToTimestamp(timestamp);

        epochSeconds = schema.truncateTimestamp(epochSeconds);

        int target = m_decoded.locateSnapshot(epochSeconds, findNearest, onlyBeforeTarget, false);
        return target >= 0 ? schema.get(target, clz) : null;
    }

    public void extractSamples(TimeSeriesExtract<?> extract,
                               String prop,
                               boolean skipMissingValues,
                               ZonedDateTime rangeStart,
                               ZonedDateTime rangeEnd)
    {
        SampleSchema schema = m_decoded.findSchema(prop);
        if (schema != null)
        {
            schema.extractSamples(extract, skipMissingValues, rangeStart, rangeEnd);
        }
    }

    //--//

    private static boolean isSafeToSubtract(long x,
                                            long y)
    {
        long r = x - y;

        if ((x ^ y) < 0)
        {
            // X and Y have different signs, so the subtraction should add to the absolute magnitude of X, not change its sign

            if ((x ^ r) < 0)
            {
                // Opposite signs => overflow.
                return false;
            }
        }

        return true;
    }
}
