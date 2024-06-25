/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.util.List;

import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.optio3.cloud.model.RecordIdentity;

public abstract class MetadataSortExtension<T> extends QueryHelperWithCommonFields.SortExtension<byte[]>
{
    private final Root<? extends RecordWithMetadata> m_root;
    private final Multimap<T, RecordIdentity>        m_map = ArrayListMultimap.create();

    protected MetadataSortExtension(Root<? extends RecordWithMetadata> root)
    {
        m_root = root;
    }

    protected abstract T extractValue(MetadataMap metadata);

    protected abstract void sort(List<T> values);

    @Override
    public Path<byte[]> getPath()
    {
        return m_root.get(RecordWithMetadata_.metadataCompressed);
    }

    @Override
    public void processValue(RecordIdentity ri,
                             byte[] compressed)
    {
        try
        {
            MetadataMap metadataMap = MetadataMap.decodeMetadata(compressed);

            T value = extractValue(metadataMap);

            m_map.put(value, ri);
        }
        catch (Throwable t)
        {
        }
    }

    @Override
    public void processResults(List<RecordIdentity> results)
    {
        results.clear();
        List<T> values = Lists.newArrayList(m_map.keySet());
        sort(values);

        for (T value : values)
        {
            results.addAll(m_map.get(value));
        }
    }
}
