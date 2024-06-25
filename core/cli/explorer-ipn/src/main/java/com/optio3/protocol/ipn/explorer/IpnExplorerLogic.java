/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn.explorer;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.FieldModel;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.BufferUtils;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class IpnExplorerLogic
{
    public enum Log
    {
        InputBuffer(com.optio3.stream.InputBuffer.LoggerInstance),
        OutputBuffer(com.optio3.stream.OutputBuffer.LoggerInstance),

        Manager(IpnManager.LoggerInstance),

        ManagerForCan(CanManager.LoggerInstance);

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

    public static class LogOptionHandler extends OptionHandler<Map<Log, Set<Severity>>>
    {
        public enum Messages implements Localizable
        {
            FORMAT_ERROR_FOR_MAP("An argument for setting a Map must contain a '='"),
            UNKNOWN_LOG("'%s' is not a valid Logger name, must be one of %s"),
            UNKNOWN_SEVERITY("'%s' is not a valid Severity value, must be one of %s");

            private final String m_fmt;

            Messages(String fmt)
            {
                m_fmt = fmt;
            }

            @Override
            public String formatWithLocale(Locale locale,
                                           Object... args)
            {
                return String.format(m_fmt, args);
            }

            @Override
            public String format(Object... args)
            {
                return formatWithLocale(Locale.getDefault(), args);
            }
        }

        public LogOptionHandler(CmdLineParser parser,
                                OptionDef option,
                                Setter<? super Map<Log, Set<Severity>>> setter)
        {
            super(parser, option, setter);
        }

        @Override
        public int parseArguments(Parameters params) throws
                                                     CmdLineException
        {
            FieldSetter fs = setter.asFieldSetter();

            @SuppressWarnings("unchecked") Map<Log, Set<Severity>> v = (Map<Log, Set<Severity>>) fs.getValue();
            if (v == null)
            {
                v = Maps.newHashMap();
                fs.addValue(v);
            }

            addToMap(params.getParameter(0), v);

            return 1;
        }

        @Override
        public String getDefaultMetaVariable()
        {
            return "<logger name>=<severity>";
        }

        //--//

        protected void addToMap(String argument,
                                Map<Log, Set<Severity>> m) throws
                                                           CmdLineException
        {
            int idx = argument.indexOf('=');
            if (idx < 0)
            {
                throw new CmdLineException(owner, Messages.FORMAT_ERROR_FOR_MAP);
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
                    throw new CmdLineException(owner, Messages.UNKNOWN_SEVERITY, mapValue2, toString(Severity.values()));
                }

                if ("*".equals(mapKey))
                {
                    for (Log key : Log.values())
                        put(m, key, value);
                }
                else
                {
                    Log key;

                    try
                    {
                        key = Enum.valueOf(Log.class, mapKey);
                    }
                    catch (Exception e)
                    {
                        throw new CmdLineException(owner, Messages.UNKNOWN_LOG, mapKey, toString(Log.values()));
                    }

                    put(m, key, value);
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

    // @formatter:off
    @Option(name = "--serial", usage = "Serial Port to use", metaVar = "<serial device>")
    private String serialPort;

    @Option(name = "--gps", usage = "GPS Port to use", metaVar = "<gps device>")
    private String gpsPort;

    @Option(name = "--obd", usage = "OBD-II Port to use", metaVar = "<obd port>")
    private String obdiiPort;

    @Option(name = "--can", usage = "CAN Port to use", metaVar = "<can port>")
    private String canPort;

    @Option(name = "--canFrequency", usage = "CAN frequency to use", metaVar = "<can frequency>")
    private int canFrequency = 25000;

    @Option(name = "--canNoTermination", usage = "Turn off CAN termination", metaVar = "<true/false>")
    private boolean canNoTermination;

    @Option(name = "--tristar", usage = "TriStar Port to use", metaVar = "<RS232 port>")
    private String tristarPort;

    @Option(name = "--onlyDiff", usage = "Only show different messages")
    private boolean onlyDiff;

    @Option(name = "--enableLog", usage = "Enable logging", handler = LogOptionHandler.class)
    private Map<Log, Set<Severity>> enableLog;
    // @formatter:on

    //--//

    public static void main(String[] args) throws
                                           Exception
    {
        new IpnExplorerLogic().doMain(args);
    }

    public void doMain(String[] args) throws
                                      Exception
    {
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println();

            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        if (enableLog != null)
        {
            for (Log en : enableLog.keySet())
            {
                for (Severity severity : enableLog.get(en))
                {
                    en.getLogger()
                      .enable(severity);
                }
            }
        }

        if (onlyDiff)
        {
            Map<Class<?>, CanObjectModel> goodMessages = Maps.newHashMap();

            try (CanManager canMgr = new CanManager(canPort, 250000, false, false)
            {
                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {
                }

                @Override
                protected void notifyGoodMessage(CanObjectModel val) throws
                                                                     Exception
                {
                    String json = ObjectMappers.prettyPrintAsJson(val);

                    Class<? extends CanObjectModel> clz    = val.getClass();
                    CanObjectModel                  valOld = goodMessages.get(clz);
                    if (valOld == null)
                    {
                        valOld = Reflection.newInstance(clz);
                    }

                    String jsonOld = ObjectMappers.prettyPrintAsJson(valOld);
                    if (StringUtils.equals(json, jsonOld))
                    {
                        return;
                    }

                    goodMessages.put(clz, val);

                    for (FieldModel fieldModel : val.getDescriptors())
                    {
                        String fieldName = fieldModel.name;

                        Object fieldVal    = val.getField(fieldName);
                        Object fieldValOld = valOld.getField(fieldName);

                        if (Objects.equals(fieldVal, fieldValOld))
                        {
                            continue;
                        }

                        CanManager.LoggerInstance.info("Diff: %s : %s : %s => %s\n", clz.getSimpleName(), fieldName, fieldVal, fieldValOld);
                    }
                }

                @Override
                protected void notifyUnknownMessage(CanAccess.BaseFrame frame)
                {
                }
            })
            {

                canMgr.start();

                System.console()
                      .readLine();
                System.out.println("Shutting down...");
            }
        }
        else
        {
            ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
            cfg.ipnPort = serialPort;
            cfg.gpsPort = gpsPort;
            cfg.obdiiPort = obdiiPort;
            cfg.canPort = canPort;
            cfg.canFrequency = canFrequency;
            cfg.canNoTermination = canNoTermination;
            cfg.tristarPort = tristarPort;

            try (IpnManager ipnMgr = new IpnManager(cfg)
            {
                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {
                }

                @Override
                protected void streamSamples(IpnObjectModel obj) throws
                                                                 Exception
                {
                    System.out.println(ObjectMappers.prettyPrintAsJson(obj));
                }

                @Override
                protected void notifySamples(IpnObjectModel obj,
                                             String field)
                {
                }

                @Override
                protected byte[] detectedStealthPowerBootloader(byte bootloadVersion,
                                                                byte hardwareVersion,
                                                                byte hardwareRevision)
                {
                    return null;
                }

                @Override
                protected void completedStealthPowerBootloader(int statusCode)
                {
                }
            })
            {
                ipnMgr.start();

                System.console()
                      .readLine();
                System.out.println("Shutting down...");
            }
        }
    }
}
