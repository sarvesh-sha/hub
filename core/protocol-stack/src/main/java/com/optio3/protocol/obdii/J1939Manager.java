/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.obdii;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.interop.mediaaccess.CanAccess;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.can.CanManager;
import com.optio3.protocol.model.can.CanObjectModel;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.protocol.model.obdii.PgnMessageType;
import com.optio3.protocol.model.obdii.pgn.BasePgnObjectModel;
import com.optio3.protocol.model.obdii.pgn.enums.PgnIsoTransportLayerType;
import com.optio3.protocol.model.obdii.pgn.sys.BaseSysPgnObjectModel;
import com.optio3.protocol.model.obdii.pgn.sys.Proprietary;
import com.optio3.protocol.model.obdii.pgn.sys.SysAddressClaimed;
import com.optio3.protocol.model.obdii.pgn.sys.SysIsoTransportLayer;
import com.optio3.protocol.model.obdii.pgn.sys.SysIsoTransportLayerRequest;
import com.optio3.protocol.model.obdii.pgn.sys.SysRequest;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class J1939Manager implements AutoCloseable
{
    public static class PgnMessageClass
    {
        public final Class<? extends ObdiiObjectModel> clz;
        public final int                               pgn;
        public final boolean                           littleEndian;
        public final boolean                           shouldIgnore;

        PgnMessageClass(Class<? extends ObdiiObjectModel> clz,
                        PgnMessageType annoPgn)
        {
            this.clz          = clz;
            this.pgn          = annoPgn.pgn();
            this.littleEndian = annoPgn.littleEndian();
            this.shouldIgnore = annoPgn.ignoreWhenReceived();
        }
    }

    public static class Results
    {
        public final Map<Integer, ObdiiObjectModel> bySource = Maps.newHashMap();

        @Override
        public String toString()
        {
            return "Results{" + "bySource=" + bySource + '}';
        }
    }

    //--//

    public static class LogTypes
    {
    }

    public static final Logger LoggerInstance      = new Logger(J1939Manager.class);
    public static final Logger LoggerInstanceTypes = new Logger(LogTypes.class);

    private static final Set<Class<? extends ObdiiObjectModel>> s_subTypes;
    private static final Map<Integer, PgnMessageClass>          s_lookupClzPgn;

    static
    {
        s_subTypes = Reflection.collectJsonSubTypes(ObdiiObjectModel.class);

        s_lookupClzPgn = Maps.newHashMap();

        for (Class<? extends ObdiiObjectModel> clz : s_subTypes)
        {
            PgnMessageType annoPgn = clz.getAnnotation(PgnMessageType.class);
            if (annoPgn != null)
            {
                PgnMessageClass messageClass = new PgnMessageClass(clz, annoPgn);
                s_lookupClzPgn.put(annoPgn.pgn(), messageClass);
            }
        }
    }

    public static Map<Integer, PgnMessageClass> getPgns()
    {
        return s_lookupClzPgn;
    }

    //--//

    private static class RequestFragment
    {
        int          totalLength;
        OutputBuffer ob;
    }

    private class RequestState<T extends ObdiiObjectModel>
    {
        final Class<T>                      targetClz;
        final Map<Integer, RequestFragment> fragments = Maps.newHashMap();
        final Map<Integer, T>               messages  = Maps.newHashMap();

        RequestState(Class<T> clz)
        {
            targetClz = clz;
        }

        void handleReply(SysIsoTransportLayer reply)
        {
            try
            {
                switch (reply.type)
                {
                    case Single:
                    {
                        RequestFragment rf = startNewMessage(reply.sourceAddress);
                        rf.totalLength = reply.extra;
//                    LoggerInstance.info("Single: %d", rf.totalLength);
                        rf.ob.emit(reply.payload, 0, reply.extra);

                        decodeMessage(reply.sourceAddress, rf);
                        break;
                    }

                    case First:
                    {
                        RequestFragment rf = startNewMessage(reply.sourceAddress);
                        rf.totalLength = reply.extra * 256 + reply.payload[0];
//                    LoggerInstance.info("First: %d", rf.totalLength);
                        rf.ob.emit(reply.payload, 1, reply.payload.length - 1);

                        SysIsoTransportLayer ack = new SysIsoTransportLayer();
                        ack.type    = PgnIsoTransportLayerType.FlowControl;
                        ack.payload = new byte[7];

                        send(reply.destinationAddress, reply.sourceAddress, ack);
                        break;
                    }

                    case Consecutive:
                    {
                        RequestFragment rf = fragments.get(reply.sourceAddress);
                        if (rf != null)
                        {
                            final int nextPos = 6 + (reply.extra - 1) * 7;

//                        LoggerInstance.info("Consecutive: %d extra=%d pos=%d nextPos=%d", rf.totalLength, reply.extra, rf.ob.getPosition(), nextPos);
                            if (rf.ob.getPosition() == nextPos)
                            {
                                rf.ob.emit(reply.payload, 0, Math.min(rf.totalLength - nextPos, reply.payload.length));
                            }

                            if (rf.ob.getPosition() == rf.totalLength)
                            {
                                decodeMessage(reply.sourceAddress, rf);
                            }
                        }
                    }
                    break;

                    case FlowControl:
                        break;
                }
            }
            catch (Throwable t)
            {
                LoggerInstance.debug("Failed to decode '%s', due to %s", ObjectMappers.toJsonNoThrow(null, reply), t);
            }

            synchronized (m_lockRequest)
            {
                m_lockRequest.notifyAll();
            }
        }

        private RequestFragment startNewMessage(int sourceAddress)
        {
            RequestFragment rq = new RequestFragment();
            rq.ob = new OutputBuffer();
            fragments.put(sourceAddress, rq);
            return rq;
        }

        private void decodeMessage(int sourceAddress,
                                   RequestFragment rf)
        {
            try (InputBuffer ib = new InputBuffer(rf.ob))
            {
                ib.read1ByteUnsigned(); // Skip service
                ib.read1ByteUnsigned(); // Skip PDU

                T obj = Reflection.newInstance(targetClz);
                SerializationHelper.read(ib, obj);

                messages.put(sourceAddress, obj);
            }
        }
    }

    private final Object       m_lockRequest = new Object();
    private       RequestState m_requestState;

    private final String     m_port;
    private final int        m_frequency;
    private final boolean    m_invert;
    private       CanManager m_canManager;

    //--//

    public J1939Manager(String port,
                        int frequency,
                        boolean invert)
    {
        m_port      = port;
        m_frequency = frequency;
        m_invert    = invert;
    }

    public void start()
    {
        m_canManager = new CanManager(m_port, m_frequency, false, m_invert)
        {
            @Override
            protected void notifyGoodMessage(CanObjectModel val) throws
                                                                 Exception
            {
                if (val instanceof ObdiiObjectModel)
                {
                    dispatch((ObdiiObjectModel) val);
                    return;
                }

                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    String json = ObjectMappers.prettyPrintAsJson(val);
                    LoggerInstance.debugVerbose("Got unexpected CAN message: %s\n", json);
                }
            }

            @Override
            protected void notifyUnknownMessage(CanAccess.BaseFrame frame) throws
                                                                           Exception
            {
                CanAccess.ExtendedFrame ef = Reflection.as(frame, CanAccess.ExtendedFrame.class);
                if (ef != null)
                {
                    if (ef.pgn == 61184 || (ef.pgn >= 65280 && ef.pgn <= 65535))
                    {
                        Proprietary prop = new Proprietary();
                        prop.pgn     = ef.pgn;
                        prop.payload = BufferUtils.convertToHex(ef.data, 0, ef.data.length, 0, true, true);

                        dispatch(prop);
                        return;
                    }

                    PgnMessageClass messageClass = s_lookupClzPgn.get(ef.pgn);
                    if (messageClass != null)
                    {
                        ObdiiObjectModel instance = Reflection.newInstance(messageClass.clz);
                        instance.sourceAddress      = ef.sourceAddress;
                        instance.destinationAddress = ef.destinationAddress;

                        try (InputBuffer ib = InputBuffer.createFrom(frame.data))
                        {
                            ib.littleEndian = messageClass.littleEndian;
                            SerializationHelper.read(ib, instance);

                            dispatch(instance);
                            return;
                        }
                    }
                }

                CanAccess.StandardFrame sf = Reflection.as(frame, CanAccess.StandardFrame.class);
                if (sf != null)
                {
                    switch (sf.sourceAddress & 0xFF8)
                    {
                        case 0x7E8:
                            SysIsoTransportLayer instance = new SysIsoTransportLayer();
                            instance.sourceAddress = sf.sourceAddress & 0x7;

                            try (InputBuffer ib = InputBuffer.createFrom(frame.data))
                            {
                                SerializationHelper.read(ib, instance);

                                dispatch(instance);
                                return;
                            }
                    }
                }

                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    LoggerInstance.debugVerbose("Got unknown CAN message: %x\n", frame.encodeId());
                }

                notifyNonDecoded(frame);
            }

            @Override
            protected void notifyTransport(String port,
                                           boolean opened,
                                           boolean closed)
            {
                J1939Manager.this.notifyTransport(port, opened, closed);
            }
        };

        m_canManager.start();

        SysRequest req = new SysRequest();
        req.pgn = new SysAddressClaimed().extractPgn();
        sendGlobal(req);
    }

    public void close() throws
                        Exception
    {
        if (m_canManager != null)
        {
            m_canManager.close();
            m_canManager = null;
        }
    }

    //--//

    protected abstract void notifyDecoded(ObdiiObjectModel obj) throws
                                                                Exception;

    protected abstract void notifyNonDecoded(CanAccess.BaseFrame frame) throws
                                                                        Exception;

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);

    //--//

    public <T extends ObdiiObjectModel> T requestSinglePdu(Class<T> clz)
    {
        Map<Integer, T> messages = requestPdu(clz);
        return CollectionUtils.firstElement(messages.values());
    }

    public <T extends ObdiiObjectModel> Map<Integer, T> requestPdu(Class<T> clz)
    {

        Iso15765MessageType anno = clz.getAnnotation(Iso15765MessageType.class);
        RequestState<T>     rs   = new RequestState<>(clz);

        synchronized (m_lockRequest)
        {
            m_requestState = rs;

            boolean        sent       = false;
            MonotonousTime expiration = TimeUtils.computeTimeoutExpiration(1, TimeUnit.SECONDS);

            while (true)
            {
                if (TimeUtils.isTimeoutExpired(expiration))
                {
                    m_requestState = null;

                    return rs.messages;
                }

                if (!sent)
                {
                    try
                    {
                        SysIsoTransportLayerRequest req = new SysIsoTransportLayerRequest();
                        req.length  = 2;
                        req.service = (byte) anno.service();
                        req.pdu     = (byte) anno.pdu();

                        LoggerInstance.debug("requestPdu: %s (service=%d pdu=%d)", clz.getSimpleName(), req.service, req.pdu);

                        send(0xF1, 0x33, req);
                        sent = true;

                        expiration = TimeUtils.computeTimeoutExpiration(500, TimeUnit.MILLISECONDS);
                    }
                    catch (Throwable t)
                    {
                        // Unable to send, keep trying...
                    }
                }

                try
                {
                    m_lockRequest.wait(500);
                }
                catch (InterruptedException e)
                {
                    // Ignore
                }
            }
        }
    }

    public void sendGlobal(BaseSysPgnObjectModel msg)
    {
        if (m_canManager != null)
        {
            m_canManager.sendGlobal(msg);
        }
    }

    public void send(int sourceAddress,
                     int destinationAddress,
                     BasePgnObjectModel msg)
    {
        if (m_canManager != null)
        {
            m_canManager.send(sourceAddress, destinationAddress, msg);
        }
    }

    public void injectFrame(CanAccess.can_frame frame) throws
                                                       Exception
    {
        if (m_canManager != null)
        {
            m_canManager.injectFrame(frame);
        }
    }

    private void dispatch(ObdiiObjectModel obj) throws
                                                Exception
    {
        obj.postDecodeFixup();

        SysIsoTransportLayer obdiiReply = Reflection.as(obj, SysIsoTransportLayer.class);
        if (obdiiReply != null)
        {
            synchronized (m_lockRequest)
            {
                if (m_requestState != null)
                {
                    m_requestState.handleReply(obdiiReply);
                }
            }
        }

        notifyDecoded(obj);
    }
}