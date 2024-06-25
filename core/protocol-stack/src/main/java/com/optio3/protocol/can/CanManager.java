/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.can;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.can.CanExtendedMessageType;
import com.optio3.protocol.model.can.CanExtendedMessageTypes;
import com.optio3.protocol.model.can.CanMessageType;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.obdii.pgn.BasePgnObjectModel;
import com.optio3.protocol.model.obdii.pgn.sys.SysIsoTransportLayer;
import com.optio3.protocol.model.obdii.pgn.sys.SysIsoTransportLayerRequest;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;

public abstract class CanManager implements AutoCloseable
{
    static class MessageDetails
    {
        Class<? extends CanObjectModel> clz;
        CanMessageType                  anno;
        CanExtendedMessageType          annoExt;
    }

    public static final Logger LoggerInstance = new Logger(CanManager.class);

    private static final Set<Class<? extends CanObjectModel>> s_subTypes;
    private static final Map<Integer, MessageDetails>         s_lookupClz;
    public static final  int                                  NULL_ADDRESS   = 0xFE;
    public static final  int                                  GLOBAL_ADDRESS = 0xFF;

    static
    {
        s_subTypes = Reflection.collectJsonSubTypes(CanObjectModel.class);

        s_lookupClz = Maps.newHashMap();

        for (Class<? extends CanObjectModel> clz : s_subTypes)
        {
            CanMessageType anno = clz.getAnnotation(CanMessageType.class);
            if (anno != null)
            {
                addCanMessageType(clz, anno);
            }

            CanExtendedMessageType annoExt = clz.getAnnotation(CanExtendedMessageType.class);
            if (annoExt != null)
            {
                addCanExtendedMessageType(clz, annoExt);
            }

            CanExtendedMessageTypes annoExts = clz.getAnnotation(CanExtendedMessageTypes.class);
            if (annoExts != null)
            {
                for (CanExtendedMessageType annoExtSub : annoExts.value())
                {
                    addCanExtendedMessageType(clz, annoExtSub);
                }
            }
        }
    }

    private static void addCanMessageType(Class<? extends CanObjectModel> clz,
                                          CanMessageType anno)
    {
        CanAccess.StandardFrame f = new CanAccess.StandardFrame();

        f.sourceAddress = anno.sourceAddress();

        var details = new MessageDetails();
        details.clz  = clz;
        details.anno = anno;
        s_lookupClz.put(f.encodeId(), details);
    }

    private static void addCanExtendedMessageType(Class<? extends CanObjectModel> clz,
                                                  CanExtendedMessageType annoExt)
    {
        CanAccess.ExtendedFrame f = new CanAccess.ExtendedFrame();

        f.priority           = annoExt.priority();
        f.extendedDataPage   = annoExt.extendedDataPage();
        f.dataPage           = annoExt.dataPage();
        f.pduFormat          = annoExt.pduFormat();
        f.destinationAddress = annoExt.destinationAddress();
        f.sourceAddress      = annoExt.sourceAddress();

        var details = new MessageDetails();
        details.clz     = clz;
        details.annoExt = annoExt;
        s_lookupClz.put(f.encodeId(), details);
    }

    //--//

    private final String  m_deviceName;
    private final int     m_frequency;
    private final boolean m_noTermination;
    private final boolean m_invert;

    private final ServiceWorker m_canWorker;

    private CanAccess m_transport;

    //--//

    public CanManager(String deviceName,
                      int frequency,
                      boolean noTermination,
                      boolean invert)
    {
        m_deviceName    = deviceName;
        m_frequency     = frequency;
        m_noTermination = noTermination;
        m_invert        = invert;

        m_canWorker = new ServiceWorkerWithWatchdog(LoggerInstance, "CANbus", 60, 2000, 30)
        {
            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void fireWatchdog()
            {
                closeTransport();
            }

            @Override
            protected void worker()
            {
                while (canContinue())
                {
                    CanAccess transport = m_transport;
                    if (transport == null)
                    {
                        try
                        {
                            FirmwareHelper f = FirmwareHelper.get();
                            f.selectPort(deviceName, FirmwareHelper.PortFlavor.CANbus, m_invert, !m_noTermination);

                            m_transport = new CanAccess(f.mapPort(m_deviceName), m_frequency);

                            notifyTransport(m_deviceName, true, false);
                            reportErrorResolution("Reconnected to CANbus!");
                            resetWatchdog();
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start CANbus", t);

                            workerSleep(10000);
                        }
                    }
                    else
                    {
                        try
                        {
                            CanAccess.BaseFrame frame = transport.read(1000, ChronoUnit.MILLIS);
                            if (frame != null)
                            {
                                resetWatchdog();
                                handleFrame(frame);
                            }
                        }
                        catch (Exception e)
                        {
                            if (!canContinue())
                            {
                                // The manager has been stopped, exit.
                                return;
                            }

                            closeTransport();

                            reportDebug("Received error: %s", e);

                            workerSleep(10000);
                        }
                    }
                }
            }

            private synchronized void closeTransport()
            {
                if (m_transport != null)
                {
                    try
                    {
                        m_transport.close();
                    }
                    catch (Throwable t)
                    {
                        // Ignore failures.
                    }

                    m_transport = null;
                }

                notifyTransport(m_deviceName, false, true);
            }
        };
    }

    public void start()
    {
        m_canWorker.start();
    }

    public void close() throws
                        Exception
    {
        m_canWorker.close();
    }

    public void send(CanAccess.BaseFrame frame)
    {
        try
        {
            if (LoggerInstance.isEnabled(Severity.DebugVerbose))
            {
                LoggerInstance.debugVerbose("Sending: %08x", frame.encodeId());

                BufferUtils.convertToHex(frame.data, 0, frame.data.length, 32, true, LoggerInstance::debugVerbose);
            }

            CanAccess transport = m_transport;
            if (transport != null)
            {
                transport.write(frame);
            }
        }
        catch (Exception e)
        {
            LoggerInstance.debug("Failed to send CAN frame, due to %s", e);
        }
    }

    public void send(int sourceAddress,
                     int destinationAddress,
                     BasePgnObjectModel msg)
    {
        try (OutputBuffer ob = new OutputBuffer())
        {
            ob.littleEndian = true;
            SerializationHelper.write(ob, msg);
            byte[] payload = ob.toByteArray();

            CanAccess.ExtendedFrame frame = new CanAccess.ExtendedFrame();
            frame.configureForRequest(msg.extractPgn(), destinationAddress, sourceAddress);
            frame.data = payload;

            send(frame);

            if (msg instanceof SysIsoTransportLayerRequest)
            {
                CanAccess.StandardFrame frame2 = new CanAccess.StandardFrame();
                frame2.sourceAddress = 0x7DF; // ID for OBD-II requests.
                frame2.data          = payload;

                send(frame2);
            }

            if (msg instanceof SysIsoTransportLayer)
            {
                CanAccess.StandardFrame frame2 = new CanAccess.StandardFrame();
                frame2.sourceAddress = 0x7E0 | sourceAddress; // ID for OBD-II requests.
                frame2.data          = payload;

                send(frame2);
            }
        }
    }

    public void sendGlobal(BasePgnObjectModel msg)
    {
        send(NULL_ADDRESS, GLOBAL_ADDRESS, msg);
    }

    //--//

    protected abstract void notifyGoodMessage(CanObjectModel val) throws
                                                                  Exception;

    protected abstract void notifyUnknownMessage(CanAccess.BaseFrame frame) throws
                                                                            Exception;

    protected boolean shouldProcessFrame(CanAccess.BaseFrame frame)
    {
        return true;
    }

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    //--//

    public void injectFrame(CanAccess.can_frame frameRaw) throws
                                                          Exception
    {
        handleFrame(CanAccess.parseRawFrame(frameRaw));
    }

    private void handleFrame(CanAccess.BaseFrame frame) throws
                                                        Exception
    {
        if (LoggerInstance.isEnabled(Severity.DebugVerbose))
        {
            LoggerInstance.debugVerbose("Received: %08x", frame.encodeId());

            BufferUtils.convertToHex(frame.data, 0, frame.data.length, 32, true, LoggerInstance::debugVerbose);
        }

        if (shouldProcessFrame(frame))
        {
            CanAccess.StandardFrame frame_std = Reflection.as(frame, CanAccess.StandardFrame.class);
            if (frame_std != null)
            {
                MessageDetails details = s_lookupClz.get(frame_std.encodeId());
                if (details != null && details.anno != null)
                {
                    try (InputBuffer ib = InputBuffer.createFrom(frame_std.data))
                    {
                        ib.littleEndian = details.anno.littleEndian();

                        CanObjectModel obj = Reflection.newInstance(details.clz);
                        SerializationHelper.read(ib, obj);

                        obj.initializeFromAnnotation(details.anno);

                        notifyGoodMessage(obj);
                        return;
                    }
                }
            }

            CanAccess.ExtendedFrame frame_ext = Reflection.as(frame, CanAccess.ExtendedFrame.class);
            if (frame_ext != null)
            {
                MessageDetails details = s_lookupClz.get(frame_ext.encodeId());
                if (details != null && details.annoExt != null)
                {
                    try (InputBuffer ib = InputBuffer.createFrom(frame_ext.data))
                    {
                        ib.littleEndian = details.annoExt.littleEndian();

                        CanObjectModel obj = Reflection.newInstance(details.clz);
                        SerializationHelper.read(ib, obj);

                        obj.initializeFromAnnotation(details.annoExt);

                        notifyGoodMessage(obj);
                        return;
                    }
                }
            }

            notifyUnknownMessage(frame);
        }
    }
}
