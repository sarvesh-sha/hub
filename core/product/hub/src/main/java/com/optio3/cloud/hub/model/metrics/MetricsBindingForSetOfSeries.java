/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.metrics;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.serialization.Reflection;

public class MetricsBindingForSetOfSeries
{
    public TypedRecordIdentityList<DeviceElementRecord> records;

    @JsonIgnore
    private Set<String> m_cachedIds;

    //--//

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        MetricsBindingForSetOfSeries that = Reflection.as(o, MetricsBindingForSetOfSeries.class);
        if (that == null)
        {
            return false;
        }

        Set<String> thisIds = collectSysIds();
        Set<String> thatIds = that.collectSysIds();

        return thisIds.equals(thatIds);
    }

    @Override
    public int hashCode()
    {
        if (records == null)
        {
            return 0;
        }

        Set<String> ids = records.collectSysIds(Sets.newHashSet());
        return Objects.hashCode(ids);
    }

    //--//

    public Set<String> collectSysIds()
    {
        if (m_cachedIds == null)
        {
            Set<String> ids = Sets.newHashSet();

            if (records != null)
            {
                records.collectSysIds(ids);
            }

            m_cachedIds = Collections.unmodifiableSet(ids);
        }

        return m_cachedIds;
    }
}
