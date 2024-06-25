/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Sets;
import com.optio3.cloud.client.gateway.model.prober.ProberObjectIpn;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForIpn;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForIpnToDecodedRead;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForIpnToObdiiRead;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.interop.util.ClassBasedAsyncDispatcherWithContext;
import com.optio3.logging.Logger;
import com.optio3.protocol.ipn.IpnManager;
import com.optio3.protocol.model.config.ProtocolConfigForIpn;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.protocol.model.ipn.IpnObjectModel;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.model.obdii.iso15765.CalculatedEngineLoad;
import com.optio3.protocol.model.obdii.iso15765.EngineCoolantTemperature;
import com.optio3.protocol.model.obdii.iso15765.EngineRPM;
import com.optio3.protocol.model.obdii.iso15765.FuelPressure;
import com.optio3.protocol.model.obdii.iso15765.FuelSystemStatus;
import com.optio3.protocol.model.obdii.iso15765.IntakeAirTemperature;
import com.optio3.protocol.model.obdii.iso15765.IntakeManifoldAbsolutePressure;
import com.optio3.protocol.model.obdii.iso15765.MassAirFlowRate;
import com.optio3.protocol.model.obdii.iso15765.RunTimeSinceEngineStart;
import com.optio3.protocol.model.obdii.iso15765.SupportedPIDs;
import com.optio3.protocol.model.obdii.iso15765.TimeRunWithMalfunction;
import com.optio3.protocol.model.obdii.iso15765.TimingAdvance;
import com.optio3.protocol.model.obdii.iso15765.VIN;
import com.optio3.protocol.model.obdii.iso15765.VehicleSpeed;
import com.optio3.protocol.obdii.J1939Manager;
import com.optio3.protocol.obdii.ObdiiManager;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.TimeUtils;

public class ProberForIpn
{
    public static final Logger LoggerInstance = new Logger(ProberForIpn.class);

//    static
//    {
//        LoggerInstance.enable(Severity.Debug);
//        LoggerInstance.enable(Severity.DebugVerbose);
//    }

    private final GatewayApplication                m_app;
    private final ProberOperationForIpn             m_input;
    private       ProberOperationForIpn.BaseResults m_output;

    private final ClassBasedAsyncDispatcherWithContext<ProberForIpn> m_dispatcher;

    public ProberForIpn(GatewayApplication app,
                        ProberOperationForIpn input)
    {

        m_app   = app;
        m_input = input;

        m_dispatcher = new ClassBasedAsyncDispatcherWithContext<>();
        m_dispatcher.add(ProberOperationForIpnToDecodedRead.class, true, ProberForIpn::executeDecodedRead);
        m_dispatcher.add(ProberOperationForIpnToObdiiRead.class, true, ProberForIpn::executeObdiiRead);
    }

    public CompletableFuture<ProberOperation.BaseResults> execute() throws
                                                                    Exception
    {
        try
        {
            await(m_dispatcher.dispatch(this, m_input));
        }
        catch (Throwable t)
        {
            Class<? extends ProberOperationForIpn> clz = m_input.getClass();
            LoggerInstance.error("Execution of %s failed with %s", clz.getName(), t);

            throw t;
        }

        return wrapAsync(m_output);
    }

    //--//

    private CompletableFuture<Void> executeDecodedRead(ProberOperationForIpnToDecodedRead input) throws
                                                                                                 Exception
    {
        ProberOperationForIpnToDecodedRead.Results output = new ProberOperationForIpnToDecodedRead.Results();

        ProtocolConfigForIpn cfg = new ProtocolConfigForIpn();
        cfg.accelerometerFrequency = input.accelerometerFrequency;
        cfg.accelerometerRange     = input.accelerometerRange;
        cfg.accelerometerThreshold = input.accelerometerThreshold;

        cfg.canPort          = input.canPort;
        cfg.canFrequency     = input.canFrequency;
        cfg.canNoTermination = input.canNoTermination;

        cfg.epsolarPort   = input.epsolarPort;
        cfg.epsolarInvert = input.epsolarInvert;

        cfg.holykellPort   = input.holykellPort;
        cfg.holykellInvert = input.holykellInvert;

        cfg.ipnPort     = input.ipnPort;
        cfg.ipnBaudrate = input.ipnBaudrate;
        cfg.ipnInvert   = input.ipnInvert;

        cfg.gpsPort = input.gpsPort;

        cfg.obdiiPort = input.obdiiPort;

        cfg.stealthpowerPort = input.stealthpowerPort;

        cfg.tristarPort = input.tristarPort;

        cfg.victronPort = input.victronPort;

        cfg.montageBluetoothGatewayPort = input.montageBluetoothGatewayPort;

        IpnManager manager = new IpnManager(cfg)
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
                IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                desc.name = obj.extractId();

                ProberObjectIpn frame = new ProberObjectIpn();
                frame.timestamp  = TimeUtils.now();
                frame.device     = desc;
                frame.properties = ObjectMappers.SkipNulls.valueToTree(obj);

                synchronized (output)
                {
                    output.frames.add(frame);
                }
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
        };

        try
        {
            manager.start();

            await(sleep(input.samplingSeconds, TimeUnit.SECONDS));
        }
        finally
        {
            manager.close();
        }

        LoggerInstance.info("Decoded Read: got %d entries", output.frames.size());

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> executeObdiiRead(ProberOperationForIpnToObdiiRead input) throws
                                                                                             Exception
    {
        ProberOperationForIpnToObdiiRead.Results output = new ProberOperationForIpnToObdiiRead.Results();
        AtomicInteger                            count  = new AtomicInteger();
        Set<String>                              seen   = Sets.newHashSet();

        if (input.obdiiPort.startsWith("can"))
        {
            J1939Manager manager = new J1939Manager(input.obdiiPort, input.obdiiFrequency, input.obdiiInvert)
            {
                @Override
                protected void notifyDecoded(ObdiiObjectModel obj) throws
                                                                   Exception
                {
                    notifyObdii(output, count, seen, obj);
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

            try
            {
                manager.start();

                notifyObdii(output, count, seen, request(manager, SupportedPIDs.Req00.class));
                notifyObdii(output, count, seen, request(manager, VIN.class));
                notifyObdii(output, count, seen, request(manager, EngineRPM.class));
                notifyObdii(output, count, seen, request(manager, VehicleSpeed.class));

                await(sleep(input.samplingSeconds, TimeUnit.SECONDS));
            }
            finally
            {
                manager.close();
            }
        }
        else
        {
            ObdiiManager manager = new ObdiiManager(input.obdiiPort, input.obdiiFrequency)
            {
                @Override
                protected void notifyTransport(String port,
                                               boolean opened,
                                               boolean closed)
                {
                }
            };

            try
            {
                manager.start();

                notifyObdii(output, count, seen, request(manager, SupportedPIDs.Req00.class));
                notifyObdii(output, count, seen, request(manager, VIN.class));

                notifyObdii(output, count, seen, request(manager, CalculatedEngineLoad.class));
                notifyObdii(output, count, seen, request(manager, EngineCoolantTemperature.class));
                notifyObdii(output, count, seen, request(manager, EngineRPM.class));
                notifyObdii(output, count, seen, request(manager, FuelPressure.class));
                notifyObdii(output, count, seen, request(manager, FuelSystemStatus.class));
                notifyObdii(output, count, seen, request(manager, IntakeAirTemperature.class));
                notifyObdii(output, count, seen, request(manager, IntakeManifoldAbsolutePressure.class));
                notifyObdii(output, count, seen, request(manager, MassAirFlowRate.class));
                notifyObdii(output, count, seen, request(manager, RunTimeSinceEngineStart.class));
                notifyObdii(output, count, seen, request(manager, TimeRunWithMalfunction.class));
                notifyObdii(output, count, seen, request(manager, TimingAdvance.class));
                notifyObdii(output, count, seen, request(manager, VehicleSpeed.class));

                await(sleep(input.samplingSeconds, TimeUnit.SECONDS));
            }
            finally
            {
                manager.close();
            }
        }

        LoggerInstance.info("Raw Read: got %d entries", output.frames.size());

        m_output = output;
        return wrapAsync(null);
    }

    //--//

    private static <T extends ObdiiObjectModel> T request(J1939Manager manager,
                                                          Class<T> clz)
    {
        for (int retries = 0; retries < 3; retries++)
        {
            T obj = manager.requestSinglePdu(clz);
            if (obj != null)
            {
                return obj;
            }
        }

        return null;
    }

    private static <T extends ObdiiObjectModel> T request(ObdiiManager manager,
                                                          Class<T> clz)
    {
        for (int retries = 0; retries < 3; retries++)
        {
            T obj = manager.requestSinglePdu(clz, 1, TimeUnit.SECONDS);
            if (obj != null)
            {
                return obj;
            }
        }

        return null;
    }

    private void notifyObdii(ProberOperationForIpnToObdiiRead.Results output,
                             AtomicInteger count,
                             Set<String> seen,
                             ObdiiObjectModel obj) throws
                                                   Exception
    {
        if (obj == null)
        {
            return;
        }

        String json = ObjectMappers.SkipNulls.writeValueAsString(obj);

        if (seen.add(json))
        {
            IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
            desc.name = String.format("%s/%04x/%d",
                                      obj.getClass()
                                         .getSimpleName(),
                                      obj.sourceAddress,
                                      count.incrementAndGet());

            ProberObjectIpn frame = new ProberObjectIpn();
            frame.timestamp  = TimeUtils.now();
            frame.device     = desc;
            frame.properties = ObjectMappers.SkipNulls.valueToTree(obj);

            synchronized (output)
            {
                output.frames.add(frame);
            }
        }
    }
}
