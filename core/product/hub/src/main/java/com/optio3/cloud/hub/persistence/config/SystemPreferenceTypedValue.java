/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.persistence.config;

import java.io.IOException;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

public enum SystemPreferenceTypedValue
{
    DataConnectionSite(null, "tableau-site", com.optio3.cloud.hub.DataConnectionSite.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec)
                {

                }
            },
    DigineousDeviceTemplate("digineous_device_template", null, com.optio3.cloud.hub.model.customization.digineous.model.DigineousDeviceLibrary.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                }
            },
    DigineousMachineTemplate("digineous_machine_template", null, com.optio3.cloud.hub.model.customization.digineous.model.DigineousMachineLibrary.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                }
            },
    DisableServiceWorker(null, "sys_disableServiceWorker", Boolean.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                    HubConfiguration cfg = helper.getServiceNonNull(HubConfiguration.class);
                    cfg.disableServiceWorker = rec.getTypedValue(Boolean.class);
                }
            },
    InstanceConfiguration(null, "sys_instanceConfiguration", com.optio3.cloud.hub.model.customization.InstanceConfiguration.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                    com.optio3.cloud.hub.model.customization.InstanceConfiguration cfg = rec.getTypedValue(com.optio3.cloud.hub.model.customization.InstanceConfiguration.class);
                    if (cfg != null)
                    {
                        cfg.updateNormalizationRules(helper.currentSessionHolder());

                        HubApplication app = helper.getServiceNonNull(HubApplication.class);
                        app.setInstanceConfiguration(cfg);
                    }
                }
            },
    PaneConfig("sys_paneConfig", null, com.optio3.cloud.hub.model.dashboard.panes.configuration.PaneConfiguration.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                }
            },
    PrivateValues("sys_privateValues", null, com.optio3.cloud.hub.model.config.PrivateValues.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                }
            },
    ProtocolConfig(null, "sys_networkConfig", com.optio3.protocol.model.config.ProtocolConfig.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec)
                {

                }
            },
    SamplingTemplate(null, "sys_samplingTemplate", com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
                {
                    com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate settings = rec.getTypedValue(com.optio3.cloud.hub.model.asset.DevicesSamplingTemplate.class);
                    if (settings != null)
                    {
                        com.optio3.cloud.hub.orchestration.tasks.TaskForSamplingTemplate.scheduleTask(helper.currentSessionHolder(), settings);
                    }
                }
            },
    SharedAssetGraphConfig("sys_assetGraphConfig", null, com.optio3.cloud.hub.model.asset.graph.SharedAssetGraph.class)
            {
                @Override
                public void onUpdate(RecordHelper<SystemPreferenceRecord> helper,
                                     SystemPreferenceRecord rec) throws
                                                                 Exception
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
