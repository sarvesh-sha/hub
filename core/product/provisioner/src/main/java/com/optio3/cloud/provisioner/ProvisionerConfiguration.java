/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.provisioner;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.infra.waypoint.BootConfig;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.util.Exceptions;

public class ProvisionerConfiguration extends AbstractConfiguration
{
    public enum Log
    {
        InputBuffer(com.optio3.stream.InputBuffer.LoggerInstance),
        OutputBuffer(com.optio3.stream.OutputBuffer.LoggerInstance);

        private final Logger m_logger;

        Log(Logger logger)
        {
            m_logger = logger;
        }

        public Logger getLogger()
        {
            return m_logger;
        }
    }

    //--//

    public String hostId;

    public String bootConfig = "/optio3-boot/optio3_config.txt";

    public String flashSource = "/tmp/optio3-latestFirmwareImage";
    public String flashLocal;

    public String printerZD420t = "/optio3-dev/optio3_zd420t";

    // Used for production server, to talk to the builder.
    public String  connectionUrl;
    public boolean productionMode;
    public boolean factoryFloorMode;

    //--//

    public String getLocalArchiveLocation()
    {
        String archiveLocation = flashLocal;
        return archiveLocation != null && new File(archiveLocation).isFile() ? archiveLocation : null;
    }

    public String getArchiveLocation()
    {
        String archiveLocation = getLocalArchiveLocation();
        return archiveLocation != null ? archiveLocation : flashSource;
    }

    public static void enableLogLevel(String argument)
    {
        int idx = argument.indexOf('=');
        if (idx < 0)
        {
            throw Exceptions.newIllegalArgumentException("An argument for setting a Map must contain a '='");
        }

        String mapKey   = argument.substring(0, idx);
        String mapValue = argument.substring(idx + 1);

        String[] mapValues = mapValue.split(",");
        for (String mapValue2 : mapValues)
        {
            Severity value;

            try
            {
                if ("*".equals(mapValue2))
                {
                    value = null;
                }
                else
                {
                    value = Enum.valueOf(Severity.class, mapValue2);
                }
            }
            catch (Exception e)
            {
                throw Exceptions.newIllegalArgumentException("'%s' is not a valid Severity value, must be one of %s", mapValue2, toString(Severity.values()));
            }

            if ("*".equals(mapKey))
            {
                for (Log key : Log.values())
                {
                    key.getLogger()
                       .enable(value);
                }
            }
            else
            {
                try
                {
                    Log key = Enum.valueOf(Log.class, mapKey);

                    key.getLogger()
                       .enable(value);
                }
                catch (Exception e)
                {
                    throw Exceptions.newIllegalArgumentException("'%s' is not a valid Logger name, must be one of %s", mapKey, toString(Log.values()));
                }
            }
        }
    }

    private static void put(Map<Log, Set<Severity>> m,
                            Log key,
                            Severity value)
    {
        Set<Severity> set = m.computeIfAbsent(key, k -> Sets.newHashSet());

        if (value == null)
        {
            Collections.addAll(set, Severity.values());
        }
        else
        {
            set.add(value);
        }
    }

    private static <T extends Enum<T>> String toString(T[] values)
    {
        StringBuilder sb = new StringBuilder();
        for (T v : values)
        {
            if (sb.length() > 0)
            {
                sb.append(", ");
            }

            sb.append(v.name());
        }

        return sb.toString();
    }

    public BootConfig parseBootConfig()
    {
        return BootConfig.parse(bootConfig);
    }
}
