/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.config;

import java.io.IOException;

import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public enum SystemPreferenceTypedValue
{
    HostSequenceNumber(null, "sys_sequenceNumber", DeploymentHostRecord.SequenceNumbers.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec)
                {
                }
            };

    private final String   m_path;
    private final String   m_name;
    private final Class<?> m_clz;

    SystemPreferenceTypedValue(String path,
                               String name,
                               Class<?> clz)
    {
        m_path = path;
        m_name = name;
        m_clz  = clz;
    }

    public String getPath()
    {
        return m_path;
    }

    public String getName()
    {
        return m_name;
    }

    public Class<?> getTypeInfo()
    {
        return m_clz;
    }

    public <T> T decode(Class<T> clz,
                        String value) throws
                                      IOException
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }

        if (!Reflection.isSubclassOf(m_clz, clz))
        {
            throw Exceptions.newGenericException(ClassCastException.class, "Can't cast '%s' to '%s'", m_clz, clz);
        }

        return clz.cast(ObjectMappers.RestDefaults.readValue(value, m_clz));
    }

    public String roundtrip(String value) throws
                                          IOException
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }

        Object obj = ObjectMappers.RestDefaults.readValue(value, m_clz);
        return ObjectMappers.RestDefaults.writeValueAsString(obj);
    }

    public static SystemPreferenceTypedValue find(String path,
                                                  String name)
    {
        for (SystemPreferenceTypedValue value : values())
        {
            if (value.m_path != null && !StringUtils.equals(value.m_path, path))
            {
                continue;
            }

            if (value.m_name != null && !StringUtils.equals(value.m_name, name))
            {
                continue;
            }

            return value;
        }

        return null;
    }

    public abstract void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                  SystemPreferenceRecord rec) throws
                                                              Exception;
}
