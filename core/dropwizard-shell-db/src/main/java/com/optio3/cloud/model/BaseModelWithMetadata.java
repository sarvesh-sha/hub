/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.lang.ref.WeakReference;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.annotation.Optio3DontMap;
import com.optio3.cloud.persistence.MetadataMap;

public abstract class BaseModelWithMetadata extends BaseModel
{
    @Optio3DontMap
    @JsonIgnore // Only for raw queries
    public byte[] metadataCompressed;

    private WeakReference<MetadataMap> m_decodedMetadata;

    public MetadataMap decodeMetadata()
    {
        WeakReference<MetadataMap> ref = m_decodedMetadata;
        MetadataMap                map = ref != null ? ref.get() : null;

        if (map == null)
        {
            map = MetadataMap.decodeMetadata(metadataCompressed);
            ref = new WeakReference<>(map);
            m_decodedMetadata = ref;
        }

        return map;
    }
}
