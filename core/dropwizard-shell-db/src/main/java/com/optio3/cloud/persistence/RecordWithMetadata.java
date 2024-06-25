/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import javax.persistence.Column;
import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import javax.persistence.Tuple;

import com.google.common.collect.Lists;
import com.optio3.logging.Logger;

@MappedSuperclass
public abstract class RecordWithMetadata extends RecordWithCommonFields implements IRecordWithMetadata
{
    @Lob
    @Column(name = "metadata_compressed")
    private byte[] metadataCompressed;

    @Transient
    private MetadataMap m_metadataDecompressed;

    //--//

    public byte[] peekMetadata()
    {
        return metadataCompressed;
    }

    @Override
    public MetadataMap getMetadata()
    {
        if (m_metadataDecompressed == null)
        {
            m_metadataDecompressed = MetadataMap.decodeMetadata(metadataCompressed);
        }

        return m_metadataDecompressed.copy();
    }

    @Override
    public boolean setMetadata(MetadataMap metadata)
    {
        byte[] metadataCompressedNew = MetadataMap.encodeMetadata(metadata);

        boolean modified = !Arrays.equals(metadataCompressed, metadataCompressedNew);
        if (modified)
        {
            metadataCompressed     = metadataCompressedNew;
            m_metadataDecompressed = null;
        }

        return modified;
    }

    public <T> T modifyMetadata(Function<MetadataMap, T> callback)
    {
        MetadataMap metadata = getMetadata();

        T res = callback.apply(metadata);

        setMetadata(metadata);

        return res;
    }

    //--//

    public static void fixupFormat(Logger logger,
                                   SessionHolder sessionHolder) throws
                                                                Exception
    {
        List<Class<?>> tables = Lists.newArrayList(RecordHelper.getEntityTables()
                                                               .iterator());
        tables.sort(Comparator.comparing(Class::getSimpleName));
        for (Class<?> clz : tables)
        {
            if (clz.getSuperclass() == RecordWithMetadata.class)
            {
                @SuppressWarnings("unchecked") Class<? extends RecordWithMetadata> clz2 = (Class<? extends RecordWithMetadata>) clz;

                try (var subSessionHolder = sessionHolder.spawnNewSessionWithTransaction())
                {
                    RecordHelper<? extends RecordWithMetadata> helper = subSessionHolder.createHelper(clz2);
                    convertFormat(logger, helper);

                    subSessionHolder.commit();
                }
            }
        }
    }

    private static <R extends RecordWithMetadata> void convertFormat(Logger logger,
                                                                     RecordHelper<R> helper) throws
                                                                                             Exception
    {
        Class<R> clz = helper.getEntityClass();

        logger.info("Processing Metadata for %s...", clz.getSimpleName());

        AtomicInteger counter         = new AtomicInteger();
        AtomicInteger counterModified = new AtomicInteger();

        QueryHelperWithCommonFields<Tuple, R> jh = new QueryHelperWithCommonFields<>(helper, Tuple.class);

        QueryHelperWithCommonFields.stream(true, -1, jh, (rec) ->
        {
            var     metadata = rec.getMetadata();
            boolean modified = rec.setMetadata(metadata);

            rec.dontRefreshUpdatedOn();

            counter.incrementAndGet();

            if (modified)
            {
                int count = counterModified.incrementAndGet();
                if ((count % 100) == 0)
                {
                    if ((count % 1000) == 0)
                    {
                        logger.info("   %,d records...", counter.get());
                    }

                    return StreamHelperNextAction.Continue_Flush_Evict_Commit;
                }

                return StreamHelperNextAction.Continue_Flush_Evict;
            }
            else
            {
                return StreamHelperNextAction.Continue_Evict;
            }
        });

        logger.info("Processed Metadata for %s, %,d records in total, %,d modified", clz.getSimpleName(), counter.get(), counterModified.get());
    }
}
