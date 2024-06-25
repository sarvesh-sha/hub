/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Sets;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.util.Exceptions;

public class WaypointConfiguration extends AbstractConfiguration
{
    public enum Log
    {
        InputBuffer(com.optio3.stream.InputBuffer.LoggerInstance),
        OutputBuffer(com.optio3.stream.OutputBuffer.LoggerInstance),

        Programmer(ProgrammerCommand.LoggerInstance);

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

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionUsername(String connectionUsername)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    public void setConnectionPassword(String connectionPassword)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    @JsonProperty("IMSI")
    public void setIMSI(String IMSI)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    @JsonProperty("IMEI")
    public void setIMEI(String IMEI)
    {
    }

    // TODO: UPGRADE PATCH: Legacy fixup for removed field
    @JsonProperty("ICCID")
    public void setICCID(String ICCID)
    {
    }

    //--//

    public String hostId;

    public String bootConfig    = "/optio3-boot/optio3_config.txt";
    public String flashInfo     = "/sys/block/sdb/size";
    public String flashDevice   = "/optio3-dev/sdb";
    public String flashSource   = "/tmp/optio3-latestFirmwareImage";
    public String printerZD420t = "/optio3-dev/optio3_zd420t";

    // Used for production server, to talk to the builder.
    public String  connectionUrl;
    public boolean productionMode;

    //--//

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
}
