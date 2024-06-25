/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus.explorer;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.ModbusManager;
import com.optio3.protocol.modbus.ModbusManagerBuilder;
import com.optio3.protocol.modbus.transport.AbstractTransport;
import com.optio3.protocol.modbus.transport.SerialTransportBuilder;
import com.optio3.protocol.modbus.transport.TcpTransportBuilder;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Charger;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_EEPROM;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_FilteredADC;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_MPPT;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Status;
import com.optio3.protocol.model.ipn.objects.morningstar.TriStar_Temperatures;
import com.optio3.protocol.model.modbus.ModbusObjectIdentifier;
import com.optio3.protocol.model.modbus.ModbusObjectModelRaw;
import com.optio3.protocol.model.modbus.ModbusObjectType;
import com.optio3.protocol.tristar.TriStarManager;
import com.optio3.serialization.ObjectMappers;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Localizable;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionDef;
import org.kohsuke.args4j.spi.FieldSetter;
import org.kohsuke.args4j.spi.OptionHandler;
import org.kohsuke.args4j.spi.Parameters;
import org.kohsuke.args4j.spi.Setter;

public class ModbusExplorerLogic
{
    public enum Log
    {
        NetworkHelper(com.optio3.infra.NetworkHelper.LoggerInstance),

        InputBuffer(com.optio3.stream.InputBuffer.LoggerInstance),
        OutputBuffer(com.optio3.stream.OutputBuffer.LoggerInstance),

        AbstractTransport(com.optio3.protocol.modbus.transport.AbstractTransport.LoggerInstance),

        Manager(ModbusManager.LoggerInstance),

        ServiceRequestHandle(com.optio3.protocol.modbus.ServiceRequestHandle.LoggerInstance);

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

    public enum Mode
    {
        ReadCoils,
        ReadDiscreteInputs,
        ReadHoldingRegisters,
        ReadInputRegisters,
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
    @Option(name = "--target", aliases = { "-t" }, usage = "IP address of target device", metaVar = "<IP>")
    private String targetAddress;

    @Option(name = "--targetPort", aliases = { "-p" }, usage = "Use specific port for client socket", metaVar = "<port>")
    private Integer targetPort;

    @Option(name = "--unit", aliases = { "-u" }, usage = "Unit number of target device", metaVar = "<num>")
    private Integer targetUnit = 1;

    @Option(name = "--register", aliases = { "-r" }, usage = "Register number on target device", metaVar = "<num>")
    private int register = 0;

    @Option(name = "--count", aliases = { "-c" }, usage = "Number of registers to process", metaVar = "<num>")
    private int count = 1;

    @Option(name = "--timeout", usage = "Timeout for BACnet messages", metaVar = "<milliseconds>")
    private Integer timeout;

    @Option(name = "--serial", usage = "Use Serial as transport")
    private boolean useSerial;

    @Option(name = "--baudRate", usage = "Baud rate to use for serial", metaVar = "<baud rate>")
    private Integer baudRate;

    @Option(name = "--useDevice", usage = "Use specific network interface for transport", metaVar = "<interface name>")
    private String useDevice;

    @Option(name = "--mode", usage = "Operation mode")
    private Mode mode = Mode.ReadHoldingRegisters;

    @Option(name = "--tristar", usage = "TriStar mode")
    private boolean tristar;

    @Option(name = "--showStackTrace", usage = "Show stack traces of failures")
    private boolean showStackTrace;

    @Option(name = "--enableLog", usage = "Enable logging", handler = LogOptionHandler.class)
    private Map<Log, Set<Severity>> enableLog;
    // @formatter:on

    //--//

    private int failures;

    public static void main(String[] args) throws
                                           Exception
    {
        new ModbusExplorerLogic().doMain(args);
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

        if (tristar)
        {
            TriStarManager mgr = new TriStarManager(useDevice)
            {
                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {
                }
            };

            mgr.start();

            {
                TriStar_FilteredADC obj = mgr.read(TriStar_FilteredADC.class)
                                             .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            {
                TriStar_Temperatures obj = mgr.read(TriStar_Temperatures.class)
                                              .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            {
                TriStar_Status obj = mgr.read(TriStar_Status.class)
                                        .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            {
                TriStar_Charger obj = mgr.read(TriStar_Charger.class)
                                         .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            {
                TriStar_MPPT obj = mgr.read(TriStar_MPPT.class)
                                      .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            {
                TriStar_EEPROM obj = mgr.read(TriStar_EEPROM.class)
                                        .get();

                System.out.println(ObjectMappers.prettyPrintAsJson(obj));
            }

            mgr.close();
            return;
        }

        AbstractTransport transport;

        if (useSerial)
        {
            SerialTransportBuilder transportBuilder = SerialTransportBuilder.newBuilder();

            if (baudRate != null)
            {
                transportBuilder.setBaudRate(baudRate);
            }

            if (useDevice != null)
            {
                transportBuilder.setPort(useDevice);
            }

            transport = transportBuilder.build();
        }
        else
        {
            TcpTransportBuilder transportBuilder = TcpTransportBuilder.newBuilder();

            if (targetAddress != null)
            {
                transportBuilder.setAddress(targetAddress);
            }

            if (targetPort != null)
            {
                transportBuilder.setPort(targetPort);
            }

            transport = transportBuilder.build();
        }

        ModbusManagerBuilder builder = ModbusManagerBuilder.newBuilder();

        builder.setTransport(transport);

        if (timeout != null)
        {
            builder.setDefaultTimeout(timeout);
        }

        try (ModbusManager mgr = builder.build())
        {
            mgr.start(null);

            execute(mgr).get();
        }
    }

    private CompletableFuture<Void> execute(ModbusManager mgr) throws
                                                               Exception
    {
//        Map<Unsigned8, String> id = await(mgr.getIds(targetUnit));

        switch (mode)
        {
            case ReadDiscreteInputs:
                await(read(mgr, ModbusObjectType.DiscreteInput));
                break;

            case ReadCoils:
                await(read(mgr, ModbusObjectType.Coil));
                break;

            case ReadHoldingRegisters:
                await(read(mgr, ModbusObjectType.HoldingRegister));
                break;

            case ReadInputRegisters:
                await(read(mgr, ModbusObjectType.InputRegister));
                break;
        }

        return wrapAsync(null);
    }

    private CompletableFuture<Void> read(ModbusManager mgr,
                                         ModbusObjectType type) throws
                                                                Exception
    {
        List<ModbusObjectIdentifier> ids = Lists.newArrayList();

        for (int i = 0; i < count; i++)
        {
            ids.add(type.allocateIdentifier(register + i));
        }

        ModbusObjectModelRaw obj = await(mgr.read(targetUnit, ids));
        System.out.println(ObjectMappers.prettyPrintAsJson(obj));
        System.out.println();

        obj.BooleanValues.forEach((key, val) -> System.out.printf("%s = %s%n", key, val));
        obj.IntegerValues.forEach((key, val) -> System.out.printf("%s = %s%n", key, val));

        return wrapAsync(null);
    }
}
