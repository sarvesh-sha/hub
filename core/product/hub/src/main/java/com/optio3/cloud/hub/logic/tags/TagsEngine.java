/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.tags;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.AbstractApplicationWithDatabase;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationEngine;
import com.optio3.cloud.hub.logic.normalizations.NormalizationRules;
import com.optio3.cloud.hub.model.asset.Asset;
import com.optio3.cloud.hub.model.asset.AssetRelationship;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsJoin;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsJoinTerm;
import com.optio3.cloud.hub.model.tags.TagsSummary;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.hub.persistence.asset.AssetRecord_;
import com.optio3.cloud.hub.persistence.asset.RelationshipRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.DatabaseActivity;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.MetadataTagsMap;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.cloud.search.HibernateSearch;
import com.optio3.collection.ExpandableArrayOf;
import com.optio3.collection.MapWithSoftValues;
import com.optio3.collection.Memoizer;
import com.optio3.concurrency.AsyncGate;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerPeriodic;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.stream.MemoryMappedHeap;
import com.optio3.util.BitSets;
import com.optio3.util.Encryption;
import com.optio3.util.Exceptions;
import com.optio3.util.FileSystem;
import com.optio3.util.ResourceAutoCleaner;

public class TagsEngine
{
    public static class Gate extends AbstractApplicationWithDatabase.GateClass
    {
    }

    public static class Snapshot
    {
        public class AssetSet
        {
            final BitSet m_bitSet;

            AssetSet(BitSet bitSet)
            {
                m_bitSet = bitSet;
            }

            public boolean isEmpty()
            {
                return m_bitSet.isEmpty();
            }

            public Set<TypedRecordIdentity<? extends AssetRecord>> resolve()
            {
                return m_state.lookupAssets(m_bitSet);
            }

            public TagsStreamNextAction streamResolved(Function<TypedRecordIdentity<? extends AssetRecord>, TagsStreamNextAction> callback)
            {
                return m_state.streamAssets(m_bitSet, callback);
            }

            public AssetSet union(AssetSet r)
            {
                return new AssetSet(BitSets.or(m_bitSet, r.m_bitSet));
            }

            public AssetSet intersection(AssetSet r)
            {
                return new AssetSet(BitSets.and(m_bitSet, r.m_bitSet));
            }

            public boolean contains(AssetRecord rec)
            {
                Integer index = m_state.m_assets.get(rec.getSysId());

                return index != null && m_bitSet.get(index);
            }
        }

        public class AssetTuple
        {
            final static int c_missing = -2;
            final static int c_null    = -1;

            private final int[] tuple;

            AssetTuple(int width)
            {
                tuple = new int[width];
                Arrays.fill(tuple, c_null);
            }

            public String[] asSysIds()
            {
                String[] tupleRes = new String[tuple.length];

                for (int i = 0; i < tuple.length; i++)
                {
                    int index = getColumn(i);

                    tupleRes[i] = index >= 0 ? m_state.getSlotSysId(index) : null;
                }

                return tupleRes;
            }

            public TypedRecordIdentity<? extends AssetRecord>[] asRecordIdentities()
            {
                TypedRecordIdentity<? extends AssetRecord>[] res = new TypedRecordIdentity[tuple.length];

                for (int i = 0; i < tuple.length; i++)
                {
                    int index = getColumn(i);

                    res[i] = index >= 0 ? m_state.getSlot(index) : null;
                }

                return res;
            }

            //--//

            int getColumn(int column)
            {
                return tuple[column];
            }

            void setColumn(int column,
                           int index)
            {
                tuple[column] = index;
            }
        }

        private final State m_state;

        Snapshot(State state)
        {
            m_state = state;
        }

        public int getVersion()
        {
            return m_state.m_version;
        }

        //--//

        public Set<String> resolveTagValues(String tag)
        {
            return Collections.unmodifiableSet(m_state.m_tagsValues.get(tag));
        }

        public AssetSet resolveRelations(String sysId,
                                         AssetRelationship relationship,
                                         boolean fromChild)
        {
            BitSet bs = new BitSet();

            OfflineRelationship.Cursor cursor = m_state.m_offlineRelations.resource.locateAsset(m_state.m_assets, sysId);
            if (cursor != null)
            {
                while (cursor.advance())
                {
                    if (cursor.fromChild == fromChild && cursor.relation == relationship)
                    {
                        bs.set(cursor.assetIndex);
                    }
                }
            }

            return new AssetSet(bs);
        }

        public int countRelations(String sysId,
                                  AssetRelationship relationship,
                                  boolean fromChild)
        {
            int num = 0;

            OfflineRelationship.Cursor cursor = m_state.m_offlineRelations.resource.locateAsset(m_state.m_assets, sysId);
            if (cursor != null)
            {
                while (cursor.advance())
                {
                    if (cursor.fromChild == fromChild && cursor.relation == relationship)
                    {
                        num++;
                    }
                }
            }

            return num;
        }

        //--//

        public int getFrequencyOfKeyValuePairs(String tag,
                                               String value)
        {
            BitSet bs = m_state.lookupTag(tag, value);

            return bs.cardinality();
        }

        public TagsSummary computeSummary(SessionProvider sessionProvider,
                                          Integer recomputeIfChanged,
                                          NormalizationRules rules)
        {
            TagsSummary summary = new TagsSummary();
            summary.version = getVersion();

            if (recomputeIfChanged == null || recomputeIfChanged != summary.version)
            {
                KeyValuePair[] pairs = m_state.m_tagsLookup.accessPairs();
                for (int i = 0; i < pairs.length; i++)
                {
                    KeyValuePair pair = pairs[i];

                    if (pair != null && pair.value == null)
                    {
                        String tag = pair.key;

                        if (!AssetRecord.WellKnownTags.isSystemTag(tag))
                        {
                            BitSet bt = m_state.m_tagsMatrix.get(i);

                            int frequency = bt.cardinality();

                            summary.tagFrequency.put(tag, frequency);
                        }
                    }
                }

                var offlineRel = m_state.m_offlineRelations.resource;
                for (int ordinal = 0; ordinal < offlineRel.m_lookup.length; ordinal++)
                {
                    String rel       = offlineRel.m_lookup[ordinal].name();
                    int    frequency = offlineRel.m_lookupFrequency[ordinal];

                    summary.relationFrequency.put(rel, frequency);
                }

                if (rules != null)
                {
                    NormalizationEngine engine = new NormalizationEngine(sessionProvider, rules, false);

                    engine.populateTagsSummary(this, summary);
                }
            }

            return summary;
        }

        public int computeFrequency(List<String> tags)
        {
            BitSet resultBitSet = null;

            for (String tag : tags)
            {
                BitSet bs = m_state.lookupTag(tag);

                if (resultBitSet == null)
                {
                    resultBitSet = BitSets.copy(bs);
                }
                else
                {
                    resultBitSet.and(bs);
                }
            }

            return resultBitSet != null ? resultBitSet.cardinality() : 0;
        }

        public AssetSet evaluateCondition(TagsCondition query)
        {
            TagsCondition.validate(query);

            BitSet resultBitSet = query.evaluate(m_state);

            return new AssetSet(resultBitSet);
        }

        //--//

        public TypedRecordIdentity<? extends AssetRecord> resolveAsset(String sysId)
        {
            Integer index = m_state.m_assets.get(sysId);
            return index != null ? m_state.getSlot(index) : null;
        }

        //--//

        public <T> T evaluateJoin(TagsJoinQuery query,
                                  Function<AssetTuple, T> callback)
        {
            class TermSummary
            {
                final int     index;
                final BitSet  candidates;
                final boolean optional;

                public TermSummary(int index,
                                   BitSet candidates,
                                   boolean optional)
                {
                    this.index      = index;
                    this.candidates = candidates;
                    this.optional   = optional;
                }
            }

            class LeftSideSummary
            {
                int termIndex;

                BitSet isParent;

                boolean optional;
            }

            class JoinSummary
            {
                final TagsJoin        join;
                final LeftSideSummary leftSide;
                final TermSummary     rightSide;
                final int             cardinality;

                private JoinSummary(TagsJoin join,
                                    LeftSideSummary leftSide,
                                    TermSummary rightSide)
                {
                    int cardinality = 0;

                    Set<TypedRecordIdentity<? extends AssetRecord>> assets = m_state.lookupAssets(leftSide.isParent);
                    for (TypedRecordIdentity<? extends AssetRecord> asset : assets)
                    {
                        var cursor = m_state.m_offlineRelations.resource.locateAsset(m_state.m_assets, asset.sysId);
                        if (cursor != null)
                        {
                            while (cursor.advance())
                            {
                                if (!cursor.fromChild && join.relation == cursor.relation)
                                {
                                    cardinality++;
                                }
                            }
                        }
                    }

                    this.join        = join;
                    this.leftSide    = leftSide;
                    this.rightSide   = rightSide;
                    this.cardinality = cardinality;
                }
            }

            class JoinContext
            {
                private int m_skip;
                private int m_maxResults;
                private int m_results;

                final boolean isDebugLog = LoggerInstanceForQuery.isEnabled(Severity.Debug);

                final TermSummary[]     terms;
                final LeftSideSummary[] leftSides;
                final List<JoinSummary> joinsSortedByCardinality = Lists.newArrayList();
                final AssetTuple        tuple;

                private JoinContext(int startOffset,
                                    int maxResults)
                {
                    m_skip       = startOffset;
                    m_maxResults = maxResults > 0 ? maxResults : Integer.MAX_VALUE;

                    Map<String, TermSummary> termsLookup = Maps.newHashMap();

                    //
                    // 1) process the terms and assign an index to each of them.
                    //
                    {
                        int numTerms = query.terms.size();
                        terms     = new TermSummary[numTerms];
                        leftSides = new LeftSideSummary[numTerms];

                        for (int i = 0; i < numTerms; i++)
                        {
                            TagsJoinTerm term = query.terms.get(i);
                            BitSet       termBitSet;

                            if (term.conditions != null)
                            {
                                termBitSet = term.conditions.evaluate(m_state);
                            }
                            else
                            {
                                termBitSet = new BitSet();
                                termBitSet.set(0, m_state.m_assets.size());
                            }

                            TermSummary termSummary = new TermSummary(i, termBitSet, term.optional);
                            terms[i] = termSummary;
                            if (termsLookup.put(term.id, termSummary) != null)
                            {
                                throw Exceptions.newRuntimeException("Multiple terms with same '%s' name", term.id);
                            }
                        }
                    }

                    //
                    // 2) Analyze left side of joins.
                    //
                    for (TagsJoin join : query.joins)
                    {
                        TermSummary rightTermSummary = termsLookup.get(join.rightSide);
                        if (rightTermSummary == null)
                        {
                            throw Exceptions.newRuntimeException("Missing definition for join right term '%s'", join.rightSide);
                        }

                        TermSummary leftTermSummary = termsLookup.get(join.leftSide);
                        if (leftTermSummary == null)
                        {
                            throw Exceptions.newRuntimeException("Missing definition for join left term '%s'", join.leftSide);
                        }

                        LeftSideSummary leftSideSummary = leftSides[leftTermSummary.index];
                        if (leftSideSummary == null)
                        {
                            leftSideSummary           = new LeftSideSummary();
                            leftSideSummary.termIndex = leftTermSummary.index;
                            leftSideSummary.isParent  = BitSets.and(leftTermSummary.candidates, m_state.m_indexIsGroup);
                            leftSideSummary.optional  = leftTermSummary.optional;

                            leftSides[leftTermSummary.index] = leftSideSummary;
                        }

                        JoinSummary joinSummary = new JoinSummary(join, leftSideSummary, rightTermSummary);
                        joinsSortedByCardinality.add(joinSummary);
                    }

                    //
                    // 3) Sort joins by increasing cardinality.
                    //
                    joinsSortedByCardinality.sort((a, b) ->
                                                  {
                                                      boolean aOptional = a.leftSide.optional;
                                                      boolean bOptional = b.leftSide.optional;

                                                      if (aOptional != bOptional)
                                                      {
                                                          // Sort non-optional before optional.
                                                          return aOptional ? 1 : -1;
                                                      }

                                                      return Integer.compare(a.cardinality, b.cardinality);
                                                  });

                    //
                    // 4) Prepare result cursor.
                    //
                    tuple = new AssetTuple(query.terms.size());
                }

                private T process()
                {
                    if (joinsSortedByCardinality.isEmpty() && terms.length == 1)
                    {
                        //
                        // Special case for single-term query.
                        //
                        BitSet mask = terms[0].candidates;
                        for (int indexOfMatch = mask.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = mask.nextSetBit(indexOfMatch + 1))
                        {
                            if (m_results >= m_maxResults)
                            {
                                break;
                            }

                            if (m_skip > 0)
                            {
                                m_skip--;
                                continue;
                            }

                            //
                            // All the joins have been processed, publish result!
                            //
                            m_results++;
                            tuple.setColumn(0, indexOfMatch);

                            if (isDebugLog)
                            {
                                LoggerInstanceForQuery.debug("process: >> Emitting result %s", Lists.newArrayList(tuple.asSysIds()));
                            }

                            T res = callback.apply(tuple);
                            if (res != null)
                            {
                                return res;
                            }
                        }

                        return null;
                    }

                    return process(0);
                }

                private T process(int nextJoin)
                {
                    if (m_results >= m_maxResults)
                    {
                        return null;
                    }

                    if (nextJoin == joinsSortedByCardinality.size())
                    {
                        if (m_skip > 0)
                        {
                            m_skip--;
                            return null;
                        }

                        //
                        // All the joins have been processed, publish result!
                        //
                        m_results++;

                        if (isDebugLog)
                        {
                            LoggerInstanceForQuery.debug("process: >> Emitting result %s", Lists.newArrayList(tuple.asSysIds()));
                        }

                        return callback.apply(tuple);
                    }

                    JoinSummary joinSummary = joinsSortedByCardinality.get(nextJoin++);
                    T           res;

                    int leftSide = tuple.getColumn(joinSummary.leftSide.termIndex);
                    if (leftSide == AssetTuple.c_null)
                    {
                        //
                        // Left side has not been selected by a previous join, we have free rein.
                        //
                        res = processLeftSides(nextJoin, joinSummary, joinSummary.leftSide.isParent);
                        if (res != null)
                        {
                            return res;
                        }
                    }
                    else
                    {
                        //
                        // This is a case where the left side had been selected by a previous join.
                        //
                        res = processRightSide(nextJoin, joinSummary, leftSide);
                        if (res != null)
                        {
                            return res;
                        }
                    }

                    return null;
                }

                private T processLeftSides(int nextJoin,
                                           JoinSummary joinSummary,
                                           BitSet assetIndices)
                {
                    try (var indent = LoggerFactory.indent(">L> "))
                    {
                        if (assetIndices.isEmpty() && joinSummary.leftSide.optional)
                        {
                            T res = processLeftSide(nextJoin, joinSummary, AssetTuple.c_missing);
                            if (res != null)
                            {
                                return res;
                            }
                        }
                        else
                        {
                            for (int assetIndex = assetIndices.nextSetBit(0); assetIndex >= 0; assetIndex = assetIndices.nextSetBit(assetIndex + 1))
                            {
                                T res = processLeftSide(nextJoin, joinSummary, assetIndex);
                                if (res != null)
                                {
                                    return res;
                                }
                            }
                        }

                        return null;
                    }
                }

                private T processLeftSide(int nextJoin,
                                          JoinSummary joinSummary,
                                          int assetIndex)
                {
                    tuple.setColumn(joinSummary.leftSide.termIndex, assetIndex);

                    T res = processRightSide(nextJoin, joinSummary, assetIndex);
                    if (res != null)
                    {
                        return res;
                    }

                    tuple.setColumn(joinSummary.leftSide.termIndex, AssetTuple.c_null);
                    return null;
                }

                private T processRightSide(int nextJoin,
                                           JoinSummary joinSummary,
                                           int assetIndex)
                {
                    try (var indent = LoggerFactory.indent(">R> "))
                    {
                        if (assetIndex == AssetTuple.c_missing)
                        {
                            // Outer join and left side was not found
                            tuple.setColumn(joinSummary.rightSide.index, AssetTuple.c_missing);

                            T res = process(nextJoin);
                            if (res != null)
                            {
                                return res;
                            }

                            tuple.setColumn(joinSummary.rightSide.index, AssetTuple.c_null);
                        }
                        else
                        {
                            AtomicBoolean found = new AtomicBoolean();

                            var cursor = m_state.m_offlineRelations.resource.locateAsset(assetIndex);
                            if (cursor != null)
                            {
                                T res = processGroup(nextJoin, joinSummary, cursor, found);
                                if (res != null)
                                {
                                    return res;
                                }
                            }

                            if (!found.get() && joinSummary.rightSide.optional)
                            {
                                int previous = tuple.getColumn(joinSummary.rightSide.index);
                                tuple.setColumn(joinSummary.rightSide.index, AssetTuple.c_missing);

                                T res = process(nextJoin);
                                if (res != null)
                                {
                                    return res;
                                }

                                tuple.setColumn(joinSummary.rightSide.index, previous);
                            }
                        }

                        return null;
                    }
                }

                private T processGroup(int nextJoin,
                                       JoinSummary joinSummary,
                                       OfflineRelationship.Cursor groupDetails,
                                       AtomicBoolean found)
                {
                    if (isDebugLog)
                    {
                        List<String> lst = Lists.newArrayList();

                        while (groupDetails.advance())
                        {
                            if (joinSummary.join.relation == groupDetails.relation && !groupDetails.fromChild)
                            {
                                TypedRecordIdentity<? extends AssetRecord> ri = m_state.getSlot(groupDetails.assetIndex);
                                lst.add(ri.sysId);
                            }
                        }
                        groupDetails.rewind();

                        lst.sort(String::compareTo);
                        LoggerInstanceForQuery.debug("processGroup: >> Join:%d Term:%d %s %s", nextJoin, joinSummary.leftSide.termIndex, groupDetails.relation, lst);
                    }

                    boolean dumpState = isDebugLog;

                    while (groupDetails.advance())
                    {
                        if (joinSummary.join.relation == groupDetails.relation && !groupDetails.fromChild)
                        {
                            if (dumpState)
                            {
                                dumpState = false;

                                List<String> lst = Lists.newArrayList();

                                int fromIndex = 0;
                                while (true)
                                {
                                    int index = joinSummary.rightSide.candidates.nextSetBit(fromIndex);
                                    if (index < 0)
                                    {
                                        break;
                                    }

                                    TypedRecordIdentity<? extends AssetRecord> ri = m_state.getSlot(index);
                                    lst.add(ri.sysId);

                                    fromIndex = index + 1;
                                }

                                lst.sort(String::compareTo);
                                LoggerInstanceForQuery.debug("processGroup: check against Term:%d %s", joinSummary.rightSide.index, lst);
                            }

                            int index = groupDetails.assetIndex;
                            if (joinSummary.rightSide.candidates.get(index))
                            {
                                int previous = tuple.getColumn(joinSummary.rightSide.index);

                                //
                                // If the outer join had already selected this term, make sure it's the same.
                                //
                                if (previous != AssetTuple.c_null && previous != index)
                                {
                                    continue;
                                }

                                tuple.setColumn(joinSummary.rightSide.index, index);
                                found.set(true);

                                if (isDebugLog)
                                {
                                    LoggerInstanceForQuery.debug("processGroup: got %s", m_state.getSlotSysId(index));
                                }

                                T res = process(nextJoin);
                                if (res != null)
                                {
                                    return res;
                                }

                                tuple.setColumn(joinSummary.rightSide.index, previous);
                            }
                        }
                    }

                    LoggerInstanceForQuery.debug("processGroup: << Join:%d %s", nextJoin, groupDetails.relation);

                    return null;
                }
            }

            TagsJoinQuery.validate(query);

            JoinContext ctx = new JoinContext(query.startOffset, query.maxResults);

            return ctx.process();
        }
    }

    private static class RawAssetModel
    {
        public String sysId;
        public String location;
        public byte[] metadataCompressed;
        public int    count;

        MetadataMap m_metadataMap;

        MetadataMap getMetadata()
        {
            if (m_metadataMap == null)
            {
                m_metadataMap = MetadataMap.decodeMetadata(metadataCompressed);
            }

            return m_metadataMap;
        }

        public void releaseMetadata()
        {
            m_metadataMap = null;
        }

        void fromRecord(AssetRecord rec)
        {
            sysId              = rec.getSysId();
            location           = RecordWithCommonFields.getSysIdSafe(rec.getLocation());
            metadataCompressed = rec.peekMetadata();
        }

        byte[] getHash()
        {
            try
            {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();

                stream.write(sysId.getBytes());

                if (location != null)
                {
                    stream.write(location.getBytes());
                }

                var metadata = getMetadata();
                metadata.toStream(AssetRecord.WellKnownMetadata.tags, stream);

                return Encryption.computeSha1(stream.toByteArray());
            }
            catch (Throwable t)
            {
                return null;
            }
        }
    }

    private static class CompactRelationship
    {
        public Integer parentIndex;
        public Integer childIndex;
        public int     relationOrdinal;
    }

    static class ExpandableArrayOfCompactRelationship extends ExpandableArrayOf<CompactRelationship>
    {
        ExpandableArrayOfCompactRelationship()
        {
            super(CompactRelationship.class);
        }

        @Override
        protected ExpandableArrayOf<CompactRelationship> allocate()
        {
            return new ExpandableArrayOfCompactRelationship();
        }

        @Override
        protected int compare(CompactRelationship o1,
                              CompactRelationship o2)
        {
            throw new RuntimeException("Not implemented");
        }
    }

    static class OfflineIntBuffer
    {
        private final File             m_file;
        private final RandomAccessFile m_fileHandler;
        private final IntBuffer        m_buffer;

        OfflineIntBuffer(long size,
                         File file) throws
                                    IOException
        {
            m_file        = file;
            m_fileHandler = new RandomAccessFile(file, "rw");
            m_fileHandler.setLength(size * 4);

            m_buffer = m_fileHandler.getChannel()
                                    .map(FileChannel.MapMode.READ_WRITE, 0, size * 4)
                                    .asIntBuffer();
        }

        void close() throws
                     IOException
        {
            if (m_fileHandler != null)
            {
                m_fileHandler.close();
            }

            m_file.delete();
        }

        void set(int pos,
                 int value)
        {
            m_buffer.put(pos, value);
        }

        int get(int pos)
        {
            return m_buffer.get(pos);
        }
    }

    //
    // The offline relationship structure starts with a lookup table, accessed through the Asset index.
    // It's followed by pairs of relationOrdinal + Asset index of the other side of the relationship.
    // The chain ends with a -1 value.
    //
    static class OfflineRelationship implements AutoCloseable
    {
        private static final int c_fromChild    = 0x4000_0000;
        private static final int c_valid        = 0x2000_0000;
        private static final int c_relationMask = 0x0000_FFFF;

        class Cursor
        {
            private final int m_head;
            private       int m_pos;

            boolean           fromChild;
            AssetRelationship relation;
            int               assetIndex;

            Cursor(int pos)
            {
                m_head = pos;
                m_pos  = pos;
            }

            void rewind()
            {
                m_pos = m_head;
            }

            boolean advance()
            {
                if (m_pos > 0)
                {
                    int marker = m_buffer.get(m_pos++);
                    if ((marker & c_valid) != 0)
                    {
                        fromChild  = (marker & c_fromChild) != 0;
                        relation   = m_lookup[marker & c_relationMask];
                        assetIndex = m_buffer.get(m_pos++);
                        return true;
                    }

                    m_pos = -1;
                }

                return false;
            }
        }

        private final AssetRelationship[] m_lookup;
        private final int[]               m_lookupFrequency;
        private       OfflineIntBuffer    m_buffer;

        OfflineRelationship()
        {
            AssetRelationship[] values = AssetRelationship.values();
            m_lookup          = new AssetRelationship[values.length];
            m_lookupFrequency = new int[values.length];

            for (AssetRelationship value : values)
            {
                m_lookup[value.ordinal()] = value;
            }
        }

        @Override
        public void close() throws
                            Exception
        {
            if (m_buffer != null)
            {
                m_buffer.close();
                m_buffer = null;
            }
        }

        void compute(LookupForAssets assets,
                     ExpandableArrayOfCompactRelationship relations,
                     String file) throws
                                  IOException
        {
            ArrayListMultimap<Integer, CompactRelationship> relBySysId = ArrayListMultimap.create();

            for (int i = 0; i < relations.size(); i++)
            {
                var rel = relations.get(i, null);
                relBySysId.put(rel.parentIndex, rel);
                relBySysId.put(rel.childIndex, rel);
            }

            Set<Integer> roots = relBySysId.keySet();

            int numAssets    = assets.size();
            int numRelations = relBySysId.size();
            int numChains    = roots.size();

            int maxSize = numAssets + 2 * (numRelations + numChains);

            Path tmpFile = FileSystem.resolveTempDirectory(file);
            m_buffer = new OfflineIntBuffer(maxSize, tmpFile.toFile());

            // Start writing the relation blocks after the asset lookup.
            int cursor = numAssets;

            for (int root : roots)
            {
                m_buffer.set(root, cursor);

                for (CompactRelationship cr : relBySysId.get(root))
                {
                    if (cr.parentIndex == root)
                    {
                        // Only count from the parent side.
                        m_lookupFrequency[cr.relationOrdinal]++;

                        m_buffer.set(cursor++, cr.relationOrdinal | c_valid); // From parent to child
                        m_buffer.set(cursor++, cr.childIndex);
                    }
                    else
                    {
                        m_buffer.set(cursor++, cr.relationOrdinal | c_fromChild | c_valid); // From child to parent
                        m_buffer.set(cursor++, cr.parentIndex);
                    }
                }

                m_buffer.set(cursor++, 0);
            }
        }

        Cursor locateAsset(LookupForAssets assets,
                           String sysId)
        {
            Integer index = assets.get(sysId);
            if (index == null)
            {
                return null;
            }

            return locateAsset(index);
        }

        Cursor locateAsset(int index)
        {
            int pos = m_buffer.get(index);
            if (pos == 0)
            {
                return null;
            }

            return new Cursor(pos);
        }
    }

    private static class State implements TagsQueryContext
    {
        private final int                                      m_version;
        private final ResourceAutoCleaner<MemoryMappedHeap>    m_heap;
        private final ResourceAutoCleaner<OfflineRelationship> m_offlineRelations;
        private final LookupForTags                            m_tagsLookup;
        private final OfflineBitSetArray                       m_tagsMatrix;
        private final HashMultimap<String, String>             m_tagsValues = HashMultimap.create();
        private       int                                      m_tagsProcessed;

        private final LookupForAssets m_assets;
        private final BitSet          m_indexIsGroup = new BitSet(); // True if asset at index is a group.

        private final ExpandableArrayOfCompactRelationship m_relations = new ExpandableArrayOfCompactRelationship();

        //--//

        State(int version)
        {
            m_version          = version;
            m_heap             = new ResourceAutoCleaner<>(this, new MemoryMappedHeap(String.format("TagsEngine-%d", version), 16 * 1024 * 1024, 0));
            m_offlineRelations = new ResourceAutoCleaner<>(this, new OfflineRelationship());

            m_tagsLookup = new LookupForTags(m_heap.resource);
            m_tagsMatrix = new OfflineBitSetArray(m_heap.resource);

            m_assets = new LookupForAssets(m_heap.resource);
        }

        void close()
        {
            m_heap.clean();
            m_offlineRelations.clean();
        }

        void freeze() throws
                      IOException
        {
            m_offlineRelations.resource.compute(m_assets, m_relations, String.format("TagsEngine-%d-Relations", m_version));
            m_relations.close();

            m_tagsLookup.ensureSerialized();
            m_tagsMatrix.ensureSerialized();
            m_assets.ensureSerialized();
        }

        void processRelation(RelationshipRecord.Raw model)
        {
            Integer parentIndex = m_assets.get(model.parent);
            if (parentIndex == null)
            {
                return;
            }

            m_indexIsGroup.set(parentIndex);

            Integer childIndex = m_assets.get(model.child);
            if (childIndex == null)
            {
                return;
            }

            CompactRelationship cr = new CompactRelationship();
            cr.parentIndex     = parentIndex;
            cr.childIndex      = childIndex;
            cr.relationOrdinal = model.relation.ordinal();

            m_relations.add(cr);
        }

        void processTags(Map<String, String> lookupEntityClass,
                         Multimap<String, KeyValuePair> lookupEntityClasses,
                         Map<String, String> childToParentLocationLookup,
                         RawAssetModel model)
        {
            KeyValuePair keyWithTable = new KeyValuePair(model.sysId, lookupEntityClass.get(model.sysId));
            int          index        = m_assets.put(keyWithTable);

            for (KeyValuePair keyForEntity : lookupEntityClasses.get(model.sysId))
            {
                markTag(keyForEntity, index);
            }

            MetadataMap metadata = model.getMetadata();

            MetadataTagsMap tags = AssetRecord.accessTags(metadata);
            if (!tags.isEmpty())
            {
                for (String tag : tags.listTags())
                {
                    m_tagsProcessed++;

                    KeyValuePair keyForTag = new KeyValuePair(tag, null);
                    markTag(keyForTag, index);

                    for (String tagValue : tags.getValuesForTag(tag))
                    {
                        KeyValuePair keyForTagWithValue = new KeyValuePair(tag, tagValue);
                        markTag(keyForTagWithValue, index);
                    }
                }
            }

            for (String locationSysId = model.location; locationSysId != null; locationSysId = childToParentLocationLookup.get(locationSysId))
            {
                m_tagsProcessed++;

                if (index < 0)
                {
                    index = m_assets.put(keyWithTable);
                }

                KeyValuePair keyForLocation = new KeyValuePair(AssetRecord.WellKnownTags.sysLocation, locationSysId);
                markTag(keyForLocation, index);
            }
        }

        //--//

        @Override
        public int getNumberOrRecords()
        {
            return m_assets.size();
        }

        @Override
        public BitSet lookupTag(String tag,
                                String value)
        {
            KeyValuePair pair = new KeyValuePair(tag, value);

            Integer index = m_tagsLookup.get(pair);
            if (index == null)
            {
                return s_empty;
            }

            return m_tagsMatrix.get(index);
        }

        @Override
        public Set<TypedRecordIdentity<? extends AssetRecord>> lookupAssets(BitSet mask)
        {
            Set<TypedRecordIdentity<? extends AssetRecord>> results = Collections.emptySet();

            for (int indexOfMatch = mask.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = mask.nextSetBit(indexOfMatch + 1))
            {
                TypedRecordIdentity<? extends AssetRecord> ri = getSlot(indexOfMatch);
                if (ri != null)
                {
                    if (results.isEmpty())
                    {
                        results = Sets.newHashSet();
                    }

                    results.add(ri);
                }
            }

            return results;
        }

        @Override
        public TagsStreamNextAction streamAssets(BitSet mask,
                                                 Function<TypedRecordIdentity<? extends AssetRecord>, TagsStreamNextAction> callback)
        {
            for (int indexOfMatch = mask.nextSetBit(0); indexOfMatch >= 0; indexOfMatch = mask.nextSetBit(indexOfMatch + 1))
            {
                TypedRecordIdentity<? extends AssetRecord> ri = getSlot(indexOfMatch);
                if (ri != null)
                {
                    TagsStreamNextAction action = callback.apply(ri);

                    if (action != TagsStreamNextAction.Continue)
                    {
                        return action;
                    }
                }
            }

            return null;
        }

        @Override
        public BitSet lookupAsset(String sysId)
        {
            BitSet bs = new BitSet();

            Integer index = m_assets.get(sysId);
            if (index != null)
            {
                bs.set(index);
            }

            return bs;
        }

        //--//

        @SuppressWarnings("unchecked")
        private TypedRecordIdentity<? extends AssetRecord> getSlot(int index)
        {
            KeyValuePair pair = m_assets.getReverse(index);
            if (pair == null)
            {
                return null;
            }

            Class<? extends AssetRecord> clz;

            String table = pair.value;
            if (table == null)
            {
                clz = AssetRecord.class;
            }
            else
            {
                clz = (Class<? extends AssetRecord>) RecordHelper.resolveEntityClass(table);
            }

            return RecordIdentity.newTypedInstance(clz, pair.key);
        }

        private String getSlotSysId(int index)
        {
            KeyValuePair pair = m_assets.getReverse(index);
            return pair != null ? pair.key : null;
        }

        //--//

        private void markTag(KeyValuePair key,
                             int index)
        {
            Integer tagIndex = m_tagsLookup.get(key);
            if (tagIndex != null)
            {
                BitSet bs = m_tagsMatrix.get(tagIndex);
                if (bs == null)
                {
                    throw Exceptions.newRuntimeException("Internal Error: m_tagsLookup and m_tagsMatrix out of sync at index %d", tagIndex);
                }

                bs.set(index);

                m_tagsMatrix.update(tagIndex, bs);
            }
            else
            {
                BitSet bs = new BitSet();
                bs.set(index);

                int newTagIndex    = m_tagsLookup.put(key);
                int newBitsetIndex = m_tagsMatrix.put(bs);
                if (newTagIndex != newBitsetIndex)
                {
                    throw Exceptions.newRuntimeException("Internal Error: m_tagsLookup and m_tagsMatrix out of sync: %d != %d", newTagIndex, newBitsetIndex);
                }

                if (key.value != null)
                {
                    m_tagsValues.put(key.key, key.value);
                }
            }
        }
    }

    //--//

    public static class ForQuery
    {
    }

    public static class ForAnalysis
    {
    }

    public static final Logger LoggerInstance            = new Logger(TagsEngine.class);
    public static final Logger LoggerInstanceForQuery    = LoggerInstance.createSubLogger(ForQuery.class);
    public static final Logger LoggerInstanceForAnalysis = LoggerInstance.createSubLogger(ForAnalysis.class);

    private static final BitSet s_empty = new BitSet();

    public final AsyncGate gate;

    private final HubApplication                    m_app;
    private final Object                            m_lock          = new Object();
    private final AtomicBoolean                     m_keepRunning   = new AtomicBoolean(true);
    private final AtomicBoolean                     m_rebuildNeeded = new AtomicBoolean(true);
    private final MapWithSoftValues<String, byte[]> m_messageDigest = new MapWithSoftValues<>();
    private       int                               m_version       = 0;
    private       Set<String>                       m_invalidation;

    private DatabaseActivity.LocalSubscriber         m_regDbActivity;
    private State                                    m_state                  = new State(0);
    private int                                      m_pendingWorkerLazyDelay = 10;
    private ScheduledFuture<CompletableFuture<Void>> m_pendingWorkerLazy;
    private CompletableFuture<Void>                  m_pendingWorker;

    private NormalizationRules m_activeNormalizationRules;

    private int  m_runs;
    private long m_runs_time;
    private long m_runs_uniqueTags;
    private long m_runs_assets;
    private long m_runs_tagsProcessed;

    private final LoggerPeriodic m_periodicDump = new LoggerPeriodic(LoggerInstanceForAnalysis, Severity.Info, 1, TimeUnit.HOURS)
    {
        @Override
        protected void onActivation()
        {
            LoggerInstanceForAnalysis.info("%,d run in %,d millisec (%,d per run): found avg. %,d unique tags in %,d records, %,d total tags",
                                           m_runs,
                                           m_runs_time,
                                           m_runs_time / m_runs,
                                           m_runs_uniqueTags / m_runs,
                                           m_runs_assets / m_runs,
                                           m_runs_tagsProcessed / m_runs);

            m_runs               = 0;
            m_runs_time          = 0;
            m_runs_uniqueTags    = 0;
            m_runs_assets        = 0;
            m_runs_tagsProcessed = 0;
        }
    };

    //--//

    public TagsEngine(HubApplication app)
    {
        m_app = app;

        HubConfiguration cfg = app.getServiceNonNull(HubConfiguration.class);
        gate = new AsyncGate(cfg.isRunningUnitTests() ? 0 : 10, TimeUnit.SECONDS);

        app.registerService(TagsEngine.class, () -> this);
    }

    public void initialize()
    {
        m_regDbActivity = DatabaseActivity.LocalSubscriber.create(m_app.getServiceNonNull(MessageBusBroker.class));

        m_regDbActivity.subscribeToTable(AssetRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                    if (!m_rebuildNeeded.getAndSet(true))
                    {
                        queueLazySynchronization(dbEvent);
                    }
                    break;

                case UPDATE_DIRECT: // We don't invalidate for indirect updates.
                    queueInvalidation(dbEvent.context.sysId);
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(RelationshipRecord.class, (dbEvent) ->
        {
            switch (dbEvent.action)
            {
                case INSERT:
                case DELETE:
                    if (!m_rebuildNeeded.getAndSet(true))
                    {
                        queueLazySynchronization(dbEvent);
                    }
                    break;
            }
        });

        m_regDbActivity.subscribeToTable(NormalizationRecord.class, (dbEvent) ->
        {
            m_activeNormalizationRules = null;
        });

        // If a fixup runs at startup, make sure we reprocess.
        m_rebuildNeeded.set(true);

        startTagSynchronization(null);
    }

    public void close()
    {
        if (m_regDbActivity != null)
        {
            m_regDbActivity.close();
            m_regDbActivity = null;
        }

        m_keepRunning.set(false);

        // Wait for worker to finish.
        synchronizeTags(true);

        m_state.close();
    }

    //--//

    public Snapshot acquireSnapshot(boolean waitForSynchronization)
    {
        State state = synchronizeTags(waitForSynchronization);

        return new Snapshot(state);
    }

    public int getVersion()
    {
        return m_state.m_version;
    }

    public NormalizationRules getActiveNormalizationRules(SessionHolder sessionHolder)
    {
        if (m_activeNormalizationRules == null)
        {
            try
            {
                NormalizationRecord rec = NormalizationRecord.findActive(sessionHolder.createHelper(NormalizationRecord.class));
                if (rec != null)
                {
                    m_activeNormalizationRules = rec.getRules();
                }
            }
            catch (Throwable t)
            {
                LoggerInstance.error("Failed to process Normalization Rules: %s", t);
            }
        }

        return m_activeNormalizationRules;
    }

    private State synchronizeTags(boolean wait)
    {
        try
        {
            AtomicBoolean           shouldWait      = new AtomicBoolean(wait);
            CompletableFuture<Void> synchronization = startTagSynchronization(shouldWait);

            if (shouldWait.get())
            {
                synchronization.get();
            }
        }
        catch (Exception e)
        {
            // Never going to happen...
        }

        return m_state;
    }

    private void queueInvalidation(String sysId)
    {
        synchronized (m_lock)
        {
            if (m_invalidation == null)
            {
                m_invalidation = Sets.newHashSet();

                Executors.scheduleOnDefaultPool(this::checkInvalidation, 100, TimeUnit.MILLISECONDS);
            }

            m_invalidation.add(sysId);
        }
    }

    private void checkInvalidation()
    {
        Set<String> set;

        synchronized (m_lock)
        {
            set            = m_invalidation;
            m_invalidation = null;
        }

        if (set != null)
        {
            boolean skip = true;

            try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
            {
                RecordHelper<AssetRecord> helper = sessionHolder.createHelper(AssetRecord.class);

                var singletonModel = new RawAssetModel();

                for (String sysId : set)
                {
                    AssetRecord rec = helper.getOrNull(sysId);
                    if (rec != null)
                    {
                        try
                        {
                            singletonModel.fromRecord(rec);

                            byte[] hash = singletonModel.getHash();

                            synchronized (m_messageDigest)
                            {
                                byte[] hashOld = m_messageDigest.get(sysId);
                                if (Arrays.equals(hash, hashOld))
                                {
                                    continue;
                                }

                                LoggerInstanceForAnalysis.debugVerbose("Invalidating due to %s/%s, hash mismatch%s",
                                                                       RecordHelper.getEntityTable(rec.getClass()),
                                                                       sysId,
                                                                       hashOld == null ? " (old hash missing)" : "");

                                m_messageDigest.put(sysId, hash);
                            }

                            skip = false;
                            break;
                        }
                        catch (Throwable e)
                        {
                            // Just in case something goes wrong with message digest computation.
                            skip = false;
                            break;
                        }
                        finally
                        {
                            helper.evict(rec);
                        }
                    }
                }
            }

            if (!skip)
            {
                if (!m_rebuildNeeded.getAndSet(true))
                {
                    queueLazySynchronization(null);
                }
            }
        }
    }

    private void queueLazySynchronization(DbEvent dbEvent)
    {
        synchronized (m_lock)
        {
            if (m_keepRunning.get())
            {
                if (m_pendingWorkerLazy != null && m_pendingWorkerLazy.isDone())
                {
                    m_pendingWorkerLazy = null;
                }

                if (m_pendingWorkerLazy == null)
                {
                    if (dbEvent == null)
                    {
                        LoggerInstanceForAnalysis.debug("Detected DB activity, scheduling analysis in %d seconds...", m_pendingWorkerLazyDelay);
                    }
                    else
                    {
                        LoggerInstanceForAnalysis.debug("Detected DB activity (%s:%s %s), scheduling analysis in %d seconds...",
                                                        dbEvent.context.getTable(),
                                                        dbEvent.context.sysId,
                                                        dbEvent.action,
                                                        m_pendingWorkerLazyDelay);
                    }

                    m_pendingWorkerLazy = Executors.scheduleOnDefaultPool(() -> startTagSynchronization(null), m_pendingWorkerLazyDelay, TimeUnit.SECONDS);
                }
            }
        }
    }

    private CompletableFuture<Void> startTagSynchronization(AtomicBoolean shouldWait)
    {
        synchronized (m_lock)
        {
            if (shouldWait != null && m_state.m_version == 0)
            {
                shouldWait.set(true);
            }

            checkInvalidation();

            if (!m_keepRunning.get())
            {
                return AsyncRuntime.NullResult;
            }

            if (m_pendingWorker == null)
            {
                if (!m_rebuildNeeded.get())
                {
                    // Shortcut if no rebuild needed and no worker pending.
                    return AsyncRuntime.NullResult;
                }

                m_pendingWorker = m_app.waitForAllGatesToOpenThenExecuteLongRunningTask(this::synchronizeTags, TagsEngine.Gate.class);
            }

            return m_pendingWorker;
        }
    }

    private void synchronizeTags()
    {
        boolean shouldReschedule = false;

        try
        {
            if (m_rebuildNeeded.getAndSet(false))
            {
                try (AsyncGate.Holder ignored = m_app.closeGate(HibernateSearch.Gate.class))
                {
                    Stopwatch st = Stopwatch.createStarted();

                    LocationsEngine          locationsEngine   = m_app.getServiceNonNull(LocationsEngine.class);
                    LocationsEngine.Snapshot locationsSnapshot = locationsEngine.acquireSnapshot(true);
                    Map<String, String>      locationHierarchy = locationsSnapshot.extractReverseHierarchy();

                    List<AssetRecord> entities = createSortedListOfAssetClasses();

                    try (SessionHolder sessionHolder = SessionHolder.createWithNewReadOnlySession(m_app, null, Optio3DbRateLimiter.System))
                    {
                        LoggerInstanceForAnalysis.debug("Starting analysis...");

                        State    state    = new State(++m_version);
                        Memoizer memoizer = new Memoizer();

                        //--//

                        // Reuse the same instance, since we don't store the individual models.
                        final var singletonModel = new RawAssetModel();

                        Map<String, String>            lookupEntityClass   = Maps.newHashMap();
                        Multimap<String, KeyValuePair> lookupEntityClasses = ArrayListMultimap.create();

                        for (AssetRecord entity : entities)
                        {
                            Class<? extends AssetRecord> entityClass = entity.getClass();
                            Class<? extends Asset>       modelClass  = ModelMapper.getModelClass(entityClass);
                            String                       entityTable = RecordHelper.getEntityTable(entityClass);
                            KeyValuePair                 entityTag   = new KeyValuePair(AssetRecord.WellKnownTags.encodeEntityTable(entityTable), null);

                            RawQueryHelper<? extends AssetRecord, RawAssetModel> qhSub = new RawQueryHelper<>(sessionHolder, entityClass);

                            qhSub.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);

                            singletonModel.count = 0;

                            qhSub.stream(() -> singletonModel, (model) ->
                            {
                                lookupEntityClass.put(model.sysId, entityTable);
                                lookupEntityClasses.put(model.sysId, entityTag);
                                model.count++;
                            });

                            LoggerInstanceForAnalysis.debugVerbose("  Found %d %s records...", singletonModel.count, modelClass.getSimpleName());
                        }

                        //--//

                        // We need to use LogicalAsset instead of Asset, because Asset is abstract...
                        RawQueryHelper<AssetRecord, RawAssetModel> qh = new RawQueryHelper<>(sessionHolder, AssetRecord.class);

                        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
                        qh.addReferenceRaw(AssetRecord_.location, (obj, val) -> obj.location = val);
                        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);

                        qh.stream(() -> singletonModel, (model) ->
                        {
                            state.processTags(lookupEntityClass, lookupEntityClasses, locationHierarchy, model);

                            byte[] hash = model.getHash();

                            synchronized (m_messageDigest)
                            {
                                m_messageDigest.put(model.sysId, hash);
                            }

                            model.releaseMetadata();
                        });

                        var singletonRelationModel = new RelationshipRecord.Raw();

                        RelationshipRecord.streamAllRelations(sessionHolder, () -> singletonRelationModel, state::processRelation);

                        LoggerInstanceForAnalysis.debug("Completed analysis: found %d unique tags in %d records, %d total tags",
                                                        state.m_tagsLookup.size(),
                                                        state.m_assets.size(),
                                                        state.m_tagsProcessed);
                        m_runs++;
                        m_runs_time += st.elapsed(TimeUnit.MILLISECONDS);
                        m_runs_uniqueTags += state.m_tagsLookup.size();
                        m_runs_assets += state.m_assets.size();
                        m_runs_tagsProcessed += state.m_tagsProcessed;
                        m_periodicDump.process();

                        state.freeze();
                        LoggerInstanceForAnalysis.debug("MemoryMappedHeap size: %,d", state.m_heap.resource.length());

                        synchronized (m_lock)
                        {
                            m_state = state;

                            if (m_rebuildNeeded.get())
                            {
                                m_pendingWorkerLazyDelay = Math.min(10 * 60, Math.min(60, m_pendingWorkerLazyDelay * 2));
                                shouldReschedule         = true;
                            }
                            else
                            {
                                m_pendingWorkerLazyDelay = 10;
                            }
                        }
                    }
                }
            }

            synchronized (m_messageDigest)
            {
                for (String sysId : m_messageDigest.keySet())
                {
                    // Walk through the soft map just to purge dead entries.
                }
            }
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to update TagsEngine state, due to %s", e);
            shouldReschedule = true;
        }
        finally
        {
            synchronized (m_lock)
            {
                m_pendingWorker.complete(null);
                m_pendingWorker = null;
            }
        }

        if (shouldReschedule)
        {
            queueLazySynchronization(null);
        }
    }

    private List<AssetRecord> createSortedListOfAssetClasses()
    {
        List<AssetRecord> entities = Lists.newArrayList();
        Set<Class<?>>     seen     = Sets.newHashSet();

        for (Class<?> entityClass : m_app.getDataSourceEntities(null))
        {
            if (Reflection.isSubclassOf(AssetRecord.class, entityClass))
            {
                appendInClassHierarchyOrder(entities, seen, entityClass);
            }
        }

        return entities;
    }

    private void appendInClassHierarchyOrder(List<AssetRecord> entities,
                                             Set<Class<?>> seen,
                                             Class<?> entityClass)
    {
        if (Reflection.isAbstractClass(entityClass))
        {
            return;
        }

        if (seen.add(entityClass))
        {
            Class<?> parentEntityClass = entityClass.getSuperclass();
            appendInClassHierarchyOrder(entities, seen, parentEntityClass);

            entities.add((AssetRecord) Reflection.newInstance(entityClass));
        }
    }
}
