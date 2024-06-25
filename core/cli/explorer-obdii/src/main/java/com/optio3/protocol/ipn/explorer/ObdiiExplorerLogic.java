/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn.explorer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.concurrency.Executors;
import com.optio3.infra.NetworkHelper;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.model.obdii.iso15765.CalculatedEngineLoad;
import com.optio3.protocol.model.obdii.iso15765.EngineCoolantTemperature;
import com.optio3.protocol.model.obdii.iso15765.EngineRPM;
import com.optio3.protocol.model.obdii.iso15765.FuelPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelRailGaugePressure;
import com.optio3.protocol.model.obdii.iso15765.FuelRailPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelSystemStatus;
import com.optio3.protocol.model.obdii.iso15765.IntakeAirTemperature;
import com.optio3.protocol.model.obdii.iso15765.IntakeManifoldAbsolutePressure;
import com.optio3.protocol.model.obdii.iso15765.MassAirFlowRate;
import com.optio3.protocol.model.obdii.iso15765.RunTimeSinceEngineStart;
import com.optio3.protocol.model.obdii.iso15765.SupportedPIDs;
import com.optio3.protocol.model.obdii.iso15765.ThrottlePosition;
import com.optio3.protocol.model.obdii.iso15765.TimingAdvance;
import com.optio3.protocol.model.obdii.iso15765.VIN;
import com.optio3.protocol.model.obdii.iso15765.VehicleSpeed;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.Resources;
import org.apache.commons.io.FileUtils;
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

public class ObdiiExplorerLogic
{
    enum ConfigVariable implements IConfigVariable
    {
        ClassName("CLASS_NAME"),
        Pgn("PGN"),
        ShouldIgnore("SHOULD_IGNORE"),
        Body("BODY");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator     = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_sourcecode = s_configValidator.newTemplate(ObdiiExplorerLogic.class, "model_template.txt", "${", "}");

    //--//

    public enum Log
    {
        InputBuffer(com.optio3.stream.InputBuffer.LoggerInstance),
        OutputBuffer(com.optio3.stream.OutputBuffer.LoggerInstance),

        CanManager(com.optio3.protocol.can.CanManager.LoggerInstance),
        Manager(J1939Manager.LoggerInstance),
        Types(J1939Manager.LoggerInstanceTypes);

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
    @Option(name = "--tcp", usage = "Tcp Address to use", metaVar = "<tcp address>")
    private String tcp = "192.168.4.1";

    @Option(name = "--tcpRedirect", usage = "Port to redirect", metaVar = "<tcp port>")
    private Integer tcpRedirect;

    @Option(name = "--enableLog", usage = "Enable logging", handler = LogOptionHandler.class)
    private Map<Log, Set<Severity>> enableLog;

    @Option(name = "--emitSourceCode", usage = "Generate source code for J1939 messages", metaVar = "<dir>")
    private String emitSourceCode;

    @Option(name = "--obdii", usage = "OBD-II Port to use", metaVar = "<can port>")
    private String obdiiPort = "can0";

    @Option(name = "--can", usage = "CAN Port to use", metaVar = "<can port>")
    private String canPort;

    @Option(name = "--inject", usage = "Replay frames from file", metaVar = "<recorded file>")
    private String injectFile;
    // @formatter:on

    //--//

    private J1939Manager m_mgr;

    private int failures;

    public static void main(String[] args) throws
                                           Exception
    {
        new ObdiiExplorerLogic().doMain(args);
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

        if (tcpRedirect != null)
        {
            ServerSocket serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(tcpRedirect));

            while (true)
            {
                Socket socket = serverSocket.accept();

                Executors.getDefaultThreadPool()
                         .execute(() -> redirect(socket, tcp));
            }
        }

        if (canPort != null)
        {
            Map<Class<? extends ObdiiObjectModel>, AtomicInteger> countKnown   = Maps.newHashMap();
            Map<Integer, AtomicInteger>                           countUnknown = Maps.newHashMap();
            Set<String>                                           seen         = Sets.newHashSet();

            J1939Manager mgr = new J1939Manager(canPort, 250000, false)
            {
                @Override
                protected void notifyDecoded(ObdiiObjectModel obj) throws
                                                                   Exception
                {
                    countKnown.computeIfAbsent(obj.getClass(), (key) -> new AtomicInteger())
                              .incrementAndGet();

                    String json = ObjectMappers.prettyPrintAsJson(obj);

                    if (seen.add(json) || J1939Manager.LoggerInstance.isEnabled(Severity.Debug))
                    {
                        J1939Manager.LoggerInstance.info("Got: %s = %s", obj.extractId(), json);
                    }
                }

                @Override
                protected void notifyNonDecoded(CanAccess.BaseFrame frame) throws
                                                                           Exception
                {
                    CanAccess.ExtendedFrame ef = Reflection.as(frame, CanAccess.ExtendedFrame.class);
                    if (ef != null)
                    {
                        countUnknown.computeIfAbsent(ef.pgn, (key) -> new AtomicInteger())
                                    .incrementAndGet();

                        J1939Manager.LoggerInstance.info("Got unknown CAN message: ID=%x PGN=%x PDU=%x", frame.encodeId(), ef.pgn, ef.pduFormat);
                        return;
                    }

                    J1939Manager.LoggerInstance.info("Got unknown CAN message: %x", frame.encodeId());
                }

                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {

                }
            };

            mgr.start();

            if (injectFile != null)
            {
                List<CanAccess.can_frame> frames = parseFile(injectFile);

                CanManager.LoggerInstance.info("Injecting %d frames from %s", frames.size(), injectFile);

                for (CanAccess.can_frame frame : frames)
                {
                    mgr.injectFrame(frame);
                }

                J1939Manager.LoggerInstance.info("Injected %d known and %d unknown message classes", countKnown.size(), countUnknown.size());

                List<Class<? extends ObdiiObjectModel>> classes = Lists.newArrayList(countKnown.keySet());
                classes.sort((a, b) -> StringUtils.compareIgnoreCase(a.getSimpleName(), b.getSimpleName()));

                for (Class<? extends ObdiiObjectModel> clz : classes)
                {
                    J1939Manager.LoggerInstance.info("Known   %s: %d",
                                                     clz.getSimpleName(),
                                                     countKnown.get(clz)
                                                               .get());
                }

                if (!countUnknown.isEmpty())
                {
                    J1939Decoder pgnDecoder = new J1939Decoder();

                    for (Integer pgn : countUnknown.keySet())
                    {
                        final int count = countUnknown.get(pgn)
                                                      .get();

                        J1939Decoder.Pgn details = pgnDecoder.get(pgn);

                        if (details != null)
                        {
                            J1939Manager.LoggerInstance.info("NotProcessed %d (%s/%s/%s): %d", pgn, details.label, details.acronym, details.description, count);
                        }
                        else
                        {
                            J1939Manager.LoggerInstance.info("Unknown %d: %d", pgn, count);
                        }
                    }
                }
            }

            Executors.safeSleep(1000);

            try
            {
                System.console()
                      .readLine();
                System.out.println("Shutting down...");
            }
            catch (Throwable t)
            {
                Executors.safeSleep(1000);
            }
            finally
            {
                mgr.close();
            }

            Runtime.getRuntime()
                   .exit(0);
        }

        if (emitSourceCode != null)
        {
            J1939CodeGen codeGen = new J1939CodeGen();
            codeGen.analyzeTypes();

            Path root = Paths.get(emitSourceCode);

            codeGen.emitTypeSourceCode((className, pgn, shouldIgnore, body) ->
                                       {
                                           ConfigVariables<ConfigVariable> parameters = s_template_sourcecode.allocate();

                                           parameters.setValue(ConfigVariable.ClassName, className);
                                           parameters.setValue(ConfigVariable.Pgn, Integer.toString(pgn));
                                           parameters.setValue(ConfigVariable.ShouldIgnore, shouldIgnore);
                                           parameters.setValue(ConfigVariable.Body, body);

                                           Path file = root.resolve(className + ".java");
                                           FileUtils.writeStringToFile(file.toFile(), parameters.convert(), "UTF8");
                                       });

            Runtime.getRuntime()
                   .exit(0);
        }

        //--//

        m_mgr = new J1939Manager(obdiiPort, 250000, false)
        {
            @Override
            protected void notifyDecoded(ObdiiObjectModel obj) throws
                                                               Exception
            {
            }

            @Override
            protected void notifyNonDecoded(CanAccess.BaseFrame frame) throws
                                                                       Exception
            {
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }
        };

        m_mgr.start();

        Executors.safeSleep(1000);

        {
            SupportedPIDs res = m_mgr.requestSinglePdu(SupportedPIDs.class);
            J1939Manager.LoggerInstance.info("SupportedPIDs => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelSystemStatus res = m_mgr.requestSinglePdu(FuelSystemStatus.class);
            J1939Manager.LoggerInstance.info("FuelSystemStatus => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            CalculatedEngineLoad res = m_mgr.requestSinglePdu(CalculatedEngineLoad.class);
            J1939Manager.LoggerInstance.info("CalculatedEngineLoad => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            EngineCoolantTemperature res = m_mgr.requestSinglePdu(EngineCoolantTemperature.class);
            J1939Manager.LoggerInstance.info("EngineCoolantTemperature => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelPressure res = m_mgr.requestSinglePdu(FuelPressure.class);
            J1939Manager.LoggerInstance.info("FuelPressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            IntakeManifoldAbsolutePressure res = m_mgr.requestSinglePdu(IntakeManifoldAbsolutePressure.class);
            J1939Manager.LoggerInstance.info("IntakeManifoldAbsolutePressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            EngineRPM res = m_mgr.requestSinglePdu(EngineRPM.class);
            J1939Manager.LoggerInstance.info("EngineRPM => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            VehicleSpeed res = m_mgr.requestSinglePdu(VehicleSpeed.class);
            J1939Manager.LoggerInstance.info("VehicleSpeed => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            TimingAdvance res = m_mgr.requestSinglePdu(TimingAdvance.class);
            J1939Manager.LoggerInstance.info("TimingAdvance => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            IntakeAirTemperature res = m_mgr.requestSinglePdu(IntakeAirTemperature.class);
            J1939Manager.LoggerInstance.info("IntakeAirTemperature => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            MassAirFlowRate res = m_mgr.requestSinglePdu(MassAirFlowRate.class);
            J1939Manager.LoggerInstance.info("MassAirFlowRate => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            ThrottlePosition res = m_mgr.requestSinglePdu(ThrottlePosition.class);
            J1939Manager.LoggerInstance.info("ThrottlePosition => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            RunTimeSinceEngineStart res = m_mgr.requestSinglePdu(RunTimeSinceEngineStart.class);
            J1939Manager.LoggerInstance.info("RunTimeSinceEngineStart => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelRailPressure res = m_mgr.requestSinglePdu(FuelRailPressure.class);
            J1939Manager.LoggerInstance.info("FuelRailPressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            FuelRailGaugePressure res = m_mgr.requestSinglePdu(FuelRailGaugePressure.class);
            J1939Manager.LoggerInstance.info("FuelRailGaugePressure => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        {
            VIN res = m_mgr.requestSinglePdu(VIN.class);
            J1939Manager.LoggerInstance.info("VIN => %s", ObjectMappers.prettyPrintAsJson(res));
        }

        for (int i = 0; i < 6 * 60; i++)
        {
            EngineCoolantTemperature res1 = m_mgr.requestSinglePdu(EngineCoolantTemperature.class);
            EngineRPM                res2 = m_mgr.requestSinglePdu(EngineRPM.class);
            VehicleSpeed             res3 = m_mgr.requestSinglePdu(VehicleSpeed.class);

            if (res1 != null)
            {
                J1939Manager.LoggerInstance.info("[%d] %s => %s", i, res1.getClass(), res1.value);
            }
            if (res2 != null)
            {
                J1939Manager.LoggerInstance.info("[%d] %s => %s", i, res2.getClass(), res2.value);
            }
            if (res3 != null)
            {
                J1939Manager.LoggerInstance.info("[%d] %s => %s", i, res3.getClass(), res3.value);
            }

            Executors.safeSleep(10000);
        }

        try
        {
            System.console()
                  .readLine();
            System.out.println("Shutting down...");
        }
        finally
        {
            m_mgr.close();
        }
    }

    private static void redirect(Socket fromClient,
                                 String tcp)
    {
        J1939Manager.LoggerInstance.info("Redirection request!");
        try (Socket toTarget = new Socket())
        {
            toTarget.setSoTimeout(1000);
            toTarget.setKeepAlive(true);
            toTarget.connect(NetworkHelper.parseInetAddressWithPort(tcp, 23), 1000);

            fromClient.setSoTimeout(1000);
            fromClient.setKeepAlive(true);

            ThreadPoolExecutor defaultThreadPool  = Executors.getDefaultThreadPool();
            Future<?>          fromClientToTarget = defaultThreadPool.submit(() -> transfer(fromClient, toTarget));
            Future<?>          fromTargetToClient = defaultThreadPool.submit(() -> transfer(toTarget, fromClient));

            fromClientToTarget.get();
            fromTargetToClient.get();
        }
        catch (Throwable t)
        {
            // Ignore failure.
            J1939Manager.LoggerInstance.debug("Redirection error: %s", t);
        }

        try
        {
            fromClient.close();
        }
        catch (Throwable t)
        {
            // Ignore failure.
        }

        J1939Manager.LoggerInstance.debug("Redirection done");
    }

    private static void transfer(Socket from,
                                 Socket to)
    {
        try
        {
            byte[] buf = new byte[256];

            InputStream  input  = from.getInputStream();
            OutputStream output = to.getOutputStream();

            while (true)
            {
                try
                {
                    int read = input.read(buf);
                    if (read < 0)
                    {
                        break;
                    }

                    J1939Manager.LoggerInstance.debugVerbose("Got %d from %s", read, from.getLocalSocketAddress());

                    output.write(buf, 0, read);
                }
                catch (SocketTimeoutException e)
                {
                    // These are fine...
                }
            }
        }
        catch (IOException e)
        {
            J1939Manager.LoggerInstance.debug("transfer failed on %s with %s", from.getLocalSocketAddress(), e);
        }

        try
        {
            from.close();
        }
        catch (Throwable t)
        {
            // Ignore failure.
        }

        try
        {
            to.close();
        }
        catch (Throwable t)
        {
            // Ignore failure.
        }
    }

    private List<CanAccess.can_frame> parseFile(String file) throws
                                                             IOException
    {
        Pattern                   lineDecoder = Pattern.compile(" *can[0-9] +([0-9A-F]+) +\\[([0-9])] +([0-9A-F ]+).*");
        List<CanAccess.can_frame> frames      = Lists.newArrayList();

        for (String line : Resources.loadLines(file, false))
        {
            Matcher matcher = lineDecoder.matcher(line);
            if (matcher.matches())
            {
                String id   = matcher.group(1);
                String len  = matcher.group(2);
                String data = matcher.group(3);

                int    parsedId      = Integer.parseInt(id, 16);
                int    parsedLen     = Integer.parseInt(len, 16);
                byte[] parsedPayload = decodePayload(data, parsedLen);

                frames.add(CanAccess.buildRawFrame(parsedId, id.length() == 8, parsedLen, parsedPayload));
            }
        }

        return frames;
    }

    private static byte[] decodePayload(String payload,
                                        int len)
    {
        String[] parts = StringUtils.split(payload, ' ');

        byte[] res = new byte[len];
        for (int i = 0; i < len; i++)
        {
            res[i] = (byte) Integer.parseInt(parts[i], 16);
        }

        return res;
    }
}
