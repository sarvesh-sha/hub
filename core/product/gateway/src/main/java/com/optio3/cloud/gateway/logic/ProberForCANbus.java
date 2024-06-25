/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.Formatter;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.cloud.client.gateway.model.prober.ProberObjectCANbus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForCANbus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForCANbusToDecodedRead;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationForCANbusToRawRead;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.interop.util.ClassBasedAsyncDispatcherWithContext;
import com.optio3.logging.Logger;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.ipn.IpnDeviceDescriptor;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.TimeUtils;

public class ProberForCANbus
{
    public static final Logger LoggerInstance = new Logger(ProberForCANbus.class);

//    static
//    {
//        LoggerInstance.enable(Severity.Debug);
//        LoggerInstance.enable(Severity.DebugVerbose);
//    }

    private final GatewayApplication                   m_app;
    private final ProberOperationForCANbus             m_input;
    private       ProberOperationForCANbus.BaseResults m_output;

    private final ClassBasedAsyncDispatcherWithContext<ProberForCANbus> m_dispatcher;

    public ProberForCANbus(GatewayApplication app,
                           ProberOperationForCANbus input)
    {

        m_app   = app;
        m_input = input;

        m_dispatcher = new ClassBasedAsyncDispatcherWithContext<>();
        m_dispatcher.add(ProberOperationForCANbusToRawRead.class, true, ProberForCANbus::executeRawRead);
        m_dispatcher.add(ProberOperationForCANbusToDecodedRead.class, true, ProberForCANbus::executeDecodedRead);
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
            Class<? extends ProberOperationForCANbus> clz = m_input.getClass();
            LoggerInstance.error("Execution of %s failed with %s", clz.getName(), t);

            throw t;
        }

        return wrapAsync(m_output);
    }

    //--//

    private CompletableFuture<Void> executeRawRead(ProberOperationForCANbusToRawRead input) throws
                                                                                            Exception
    {
        ProberOperationForCANbusToRawRead.Results output = new ProberOperationForCANbusToRawRead.Results();

        try (CanManager mgr = new CanManager(m_input.port, m_input.frequency, m_input.noTermination, m_input.invert)
        {
            @Override
            protected void notifyGoodMessage(CanObjectModel val)
            {
            }

            @Override
            protected void notifyUnknownMessage(CanAccess.BaseFrame frame)
            {
                Map<String, String> properties = Maps.newHashMap();

                try (Formatter formatter = new Formatter())
                {
                    for (byte b : frame.data)
                    {
                        formatter.format("%02X ", b);
                    }

                    properties.put("raw",
                                   formatter.toString()
                                            .trim());
                }

                IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                desc.name = String.format("0x%04x", frame.encodeId());

                ProberObjectCANbus frameOut = new ProberObjectCANbus();
                frameOut.timestamp  = TimeUtils.now();
                frameOut.device     = desc;
                frameOut.properties = ObjectMappers.SkipNulls.valueToTree(properties);

                synchronized (output)
                {
                    output.frames.add(frameOut);
                }
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }
        })
        {
            mgr.start();

            await(sleep(input.samplingSeconds, TimeUnit.SECONDS));
        }

        LoggerInstance.info("Raw Read: got %d entries", output.frames.size());

        m_output = output;
        return wrapAsync(null);
    }

    private CompletableFuture<Void> executeDecodedRead(ProberOperationForCANbusToDecodedRead input) throws
                                                                                                    Exception
    {
        ProberOperationForCANbusToDecodedRead.Results output = new ProberOperationForCANbusToDecodedRead.Results();

        try (CanManager mgr = new CanManager(m_input.port, m_input.frequency, m_input.noTermination, m_input.invert)
        {
            @Override
            protected void notifyGoodMessage(CanObjectModel val)
            {
                IpnDeviceDescriptor desc = new IpnDeviceDescriptor();
                desc.name = val.extractId();

                ProberObjectCANbus frame = new ProberObjectCANbus();
                frame.timestamp  = TimeUtils.now();
                frame.device     = desc;
                frame.properties = ObjectMappers.SkipNulls.valueToTree(val);

                synchronized (output)
                {
                    output.frames.add(frame);
                }
            }

            @Override
            protected void notifyUnknownMessage(CanAccess.BaseFrame frame)
            {
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
            }
        })
        {
            mgr.start();

            await(sleep(input.samplingSeconds, TimeUnit.SECONDS));
        }

        LoggerInstance.info("Decoded Read: got %d entries", output.frames.size());

        m_output = output;
        return wrapAsync(null);
    }
}