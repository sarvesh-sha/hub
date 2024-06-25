/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.obdii;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.interop.mediaaccess.SerialAccess;
import com.optio3.logging.Logger;
import com.optio3.protocol.common.ServiceWorker;
import com.optio3.protocol.common.ServiceWorkerWithWatchdog;
import com.optio3.protocol.model.obdii.Iso15765MessageType;
import com.optio3.protocol.model.obdii.ObdiiObjectModel;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.FunctionWithException;
import org.apache.commons.lang3.StringUtils;

public abstract class ObdiiManager implements AutoCloseable
{
    public enum RequestStatus
    {
        GotSomethingButWaitForMore,
        Success,
        Failure,
        Timeout
    }

    enum EldMessage
    {
        J1708(1),
        CAN11bits(2),
        CAN29bits(4);

        private final int m_length;

        EldMessage(int length)
        {
            m_length = length;
        }

        public int getIdLength()
        {
            return m_length;
        }
    }

    //--//

    public static abstract class Message
    {
        public final int          length;
        final        OutputBuffer m_buffer;

        Message(int length)
        {
            this.length = length;

            m_buffer = new OutputBuffer();
        }

        RequestStatus append(int[] buf,
                             int offset,
                             int count)
        {
            for (int pos = 0; pos < count; pos++)
            {
                if (!fitsInByte(buf, offset))
                {
                    LoggerInstance.debugObnoxious("append: !fitsInByte, %d : %d", buf.length, offset);

                    return RequestStatus.Failure;
                }

                if (m_buffer.getPosition() >= length)
                {
                    LoggerInstance.debugObnoxious("append: m_buffer.getPosition() >= length, %d : %d", m_buffer.getPosition(), length);
                    return RequestStatus.Failure; // Too many bytes in messages.
                }

                m_buffer.emit1Byte(buf[offset++]);

                if (m_buffer.getPosition() == length)
                {
                    LoggerInstance.debugObnoxious("append: m_buffer.getPosition() == length, %d : %d", m_buffer.getPosition(), length);
                    return RequestStatus.GotSomethingButWaitForMore;
                }
            }

            LoggerInstance.debugObnoxious("append: m_buffer.getPosition() < length, %d : %d", m_buffer.getPosition(), length);
            return null;
        }

        RequestStatus append(InputBuffer ib)
        {
            int available = ib.remainingLength();
            int needed    = Math.min(length - m_buffer.getPosition(), available);

            m_buffer.emit(ib.readByteArray(needed));

            if (m_buffer.getPosition() == length)
            {
                LoggerInstance.debugObnoxious("append: m_buffer.getPosition() == length, %d : %d", m_buffer.getPosition(), length);
                return RequestStatus.GotSomethingButWaitForMore;
            }

            LoggerInstance.debugObnoxious("append: m_buffer.getPosition() < length, %d : %d", m_buffer.getPosition(), length);
            return null;
        }

        String getDump()
        {
            StringBuilder sb = new StringBuilder();

            for (byte c : m_buffer.toByteArray())
            {
                if (sb.length() > 0)
                {
                    sb.append(", ");
                }
                sb.append(String.format("%02X", c));
            }

            return sb.toString();
        }
    }

    public static class MessageCAN extends Message
    {
        public final int sourceAddress;

        MessageCAN(int length,
                   int sourceAddress)
        {
            super(length);

            this.sourceAddress = sourceAddress;
        }

        @Override
        public String toString()
        {
            return "MessageCAN{" + "length=" + length + ", sourceAddress=" + sourceAddress + ", content=" + getDump() + '}';
        }
    }

    public static class MessageJ1939 extends Message
    {
        public final int priority;
        public final int pduFormat;
        public final int destinationAddress;
        public final int sourceAddress;

        MessageJ1939(int length,
                     int priority,
                     int pduFormat,
                     int destinationAddress,
                     int sourceAddress)
        {
            super(length);

            this.priority           = priority;
            this.pduFormat          = pduFormat;
            this.destinationAddress = destinationAddress;
            this.sourceAddress      = sourceAddress;
        }

        @Override
        public String toString()
        {
            return "MessageJ1939{" + "length=" + length + ", priority=" + priority + ", pduFormat=" + pduFormat + ", destinationAddress=" + destinationAddress + ", sourceAddress=" + sourceAddress + ", content=" + getDump() + '}';
        }
    }

    public static class Results
    {
        public final Map<Integer, Message> bySource = Maps.newHashMap();
        public       boolean               notSupported;

        @Override
        public String toString()
        {
            return "Results{" + "bySource=" + bySource + '}';
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(ObdiiManager.class);

    //--//

    private final Object m_lockRequest = new Object();

    private final ServiceWorker m_obdiiWorker;
    private       boolean       m_isReady;
    private       boolean       m_gotResponse;

    private FunctionWithException<String, RequestStatus> m_requestHandler;
    private RequestStatus                                m_requestProgress;
    private SerialAccess                                 m_transport;

    //--//

    public ObdiiManager(String serialPort,
                        int baudRate)
    {
        m_obdiiWorker = new ServiceWorkerWithWatchdog(LoggerInstance, "OBDII", 60, 2000, 30)
        {
            @Override
            protected void shutdown()
            {
                closeTransport();
            }

            @Override
            protected void fireWatchdog()
            {
                reportError("No responses to requests, closing transport...");

                closeTransport();
            }

            @Override
            protected void worker()
            {
                final StringBuilder sb    = new StringBuilder();
                final byte[]        input = new byte[512];

                while (canContinue())
                {
                    SerialAccess transport = m_transport;
                    if (transport == null)
                    {
                        m_isReady = false;

                        try
                        {
                            FirmwareHelper f = FirmwareHelper.get();
                            f.selectPort(serialPort, FirmwareHelper.PortFlavor.RS232, false, false);

                            m_transport = new SerialAccess(f.mapPort(serialPort), baudRate > 0 ? baudRate : 115200, 8, 'N', 1);

                            notifyOfProgress();

                            notifyTransport(serialPort, true, false);
                            resetWatchdog();
                        }
                        catch (Throwable t)
                        {
                            reportFailure("Failed to start OBD-II", t);

                            workerSleep(10000);
                        }
                    }
                    else
                    {
                        try
                        {
                            int len = transport.read(input, 1000);
                            if (len <= 0)
                            {
                                sb.setLength(0);

                                if (len < 0)
                                {
                                    closeTransport();
                                    workerSleep(500);
                                }
                            }
                            else
                            {
                                resetWatchdog();

                                for (int i = 0; i < len; i++)
                                {
                                    char c = (char) input[i];

                                    switch (c)
                                    {
                                        case '>': // It's the prompt, ignoring it.
                                        case '\n':
                                        case '\r':
                                            if (sb.length() > 0)
                                            {
                                                if (m_gotResponse)
                                                {
                                                    reportErrorResolution("Reconnected to OBD-II!");
                                                }

                                                notifyLine(sb.toString());
                                                sb.setLength(0);
                                            }
                                            break;

                                        default:
                                            sb.append(c);
                                            break;
                                    }
                                }
                            }
                        }
                        catch (Throwable t)
                        {
                            if (!canContinue())
                            {
                                // The manager has been stopped, exit.
                                return;
                            }

                            closeTransport();

                            reportDebug("Received error: %s", t);

                            workerSleep(10000);
                        }
                    }
                }
            }

            private synchronized void closeTransport()
            {
                m_isReady     = false;
                m_gotResponse = false;
                notifyOfProgress();

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

                notifyTransport(serialPort, false, true);
            }
        };
    }

    public void start()
    {
        m_obdiiWorker.start();
    }

    public void close()
    {
        m_obdiiWorker.stop();
    }

    //--//

    protected abstract void notifyTransport(String port,
                                            boolean opened,
                                            boolean closed);
    //--//

    private void notifyOfProgress()
    {
        synchronized (m_lockRequest)
        {
            m_lockRequest.notifyAll();
        }
    }

    private void waitForProgress(int timeout)
    {
        try
        {
            m_lockRequest.wait(timeout);
        }
        catch (InterruptedException e)
        {
            // Ignore
        }
    }

    //--//

    public <T extends ObdiiObjectModel> T requestSinglePdu(Class<T> clz,
                                                           long timeout,
                                                           TimeUnit unit)
    {
        Map<Integer, T> messages = requestPdu(clz, timeout, unit);
        return CollectionUtils.firstElement(messages.values());
    }

    public <T extends ObdiiObjectModel> Map<Integer, T> requestPdu(Class<T> clz,
                                                                   long timeout,
                                                                   TimeUnit unit)
    {
        Iso15765MessageType anno     = clz.getAnnotation(Iso15765MessageType.class);
        Map<Integer, T>     messages = Maps.newHashMap();

        for (int retry = 0; retry < 3; retry++)
        {
            Results received = sendPduRequest(anno, timeout, unit);
            if (received != null)
            {
                if (received.notSupported)
                {
                    break;
                }

                for (Integer sourceAddress : received.bySource.keySet())
                {
                    Message msg = received.bySource.get(sourceAddress);

                    try
                    {
                        try (InputBuffer buffer = new InputBuffer(msg.m_buffer))
                        {
                            int serviceReply = buffer.read1ByteUnsigned();
                            int pduReply     = anno.pdu() >= 0 ? buffer.read1ByteUnsigned() : anno.pdu();

                            if (serviceReply == (0x40 | anno.service()) && pduReply == anno.pdu())
                            {
                                T instance = Reflection.newInstance(clz);
                                SerializationHelper.read(buffer, instance);
                                instance.sourceAddress = sourceAddress;

                                instance.postDecodeFixup();

                                messages.put(sourceAddress, instance);
                            }
                        }

                        msg.m_buffer.close();
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.debug("Failed decoding: %s", t);
                    }
                }

                if (!messages.isEmpty())
                {
                    break;
                }
            }
        }

        return messages;
    }

    private Results sendPduRequest(Iso15765MessageType anno,
                                   long timeout,
                                   TimeUnit unit)
    {
        try
        {
            if (!m_isReady)
            {
                synchronized (m_lockRequest)
                {
                    makeHardRequest("ATZ", "ELM327", false, 1, TimeUnit.SECONDS, "Failed to reset OBD-II adapter");

                    makeHardRequest("ATE0", "OK", true, 1, TimeUnit.SECONDS, "Failed to turn off ECHO");
                    makeHardRequest("ATH1", "OK", true, 1, TimeUnit.SECONDS, "Failed to turn on headers");
                    makeHardRequest("ATAL", "OK", true, 1, TimeUnit.SECONDS, "Failed to configure long messages");
                    makeHardRequest("ATSP0", "OK", true, 1, TimeUnit.SECONDS, "Failed to reset protocol");

                    m_isReady = true;

                    // First time we need to wait for protocol detection.
                    timeout = 10;
                    unit    = TimeUnit.SECONDS;
                }
            }

            Results res = new Results();

            RequestStatus status = sendRequest(String.format(anno.pdu() >= 0 ? "%02X%02X" : "%02X", anno.service(), anno.pdu()), timeout, unit, (line) ->
            {
                if (isError(line))
                {
                    res.notSupported = true;
                    return RequestStatus.Success;
                }

                int[] chunk = decodeChunk(line);
                if (chunk == null)
                {
                    return null;
                }

                LoggerInstance.debugVerbose("Decoded Chunk: %s", chunk.length);

                m_gotResponse = true;

                return parseChunk(res, chunk, anno.hasMultipleFrames());
            });

            return status == RequestStatus.Success ? res : null;
        }
        catch (Throwable e)
        {
            return null;
        }
    }

    //--//

    private void makeHardRequest(String request,
                                 String expectedResponse,
                                 boolean exactResponse,
                                 long timeout,
                                 TimeUnit unit,
                                 String failureMessage)
    {
        RequestStatus result = sendRequest(request, timeout, unit, (line) ->
        {
            if (exactResponse && StringUtils.equals(line, expectedResponse))
            {
                return RequestStatus.Success;
            }
            else if (line.startsWith(expectedResponse))
            {
                return RequestStatus.Success;
            }

            return null;
        });

        if (result != RequestStatus.Success)
        {
            throw new RuntimeException(failureMessage);
        }
    }

    private RequestStatus sendRequest(String request,
                                      long timeout,
                                      TimeUnit unit,
                                      FunctionWithException<String, RequestStatus> handler)
    {
        LoggerInstance.debugVerbose("HandlerForElm327.sendRequest: %s", request);

        synchronized (m_lockRequest)
        {
            m_requestHandler = handler;
            try
            {
                MonotonousTime expiration  = TimeUtils.computeTimeoutExpiration(timeout, unit);
                boolean        requestSent = false;

                while (true)
                {
                    if (!requestSent)
                    {
                        SerialAccess transport = m_transport;
                        if (transport != null)
                        {
                            transport.write(request);
                            transport.write("\r\n");
                            requestSent = true;
                        }
                    }

                    waitForProgress(10);

                    if (m_requestProgress == RequestStatus.GotSomethingButWaitForMore)
                    {
                        break;
                    }

                    if (m_requestHandler == null)
                    {
                        return m_requestProgress;
                    }

                    if (TimeUtils.isTimeoutExpired(expiration))
                    {
                        return RequestStatus.Timeout;
                    }
                }

                m_requestProgress = RequestStatus.Success;
                MonotonousTime smallWait = null;
                while (true)
                {
                    if (smallWait == null)
                    {
                        smallWait = TimeUtils.computeTimeoutExpiration(100, TimeUnit.MILLISECONDS);
                    }

                    waitForProgress(10);

                    if (m_requestHandler == null)
                    {
                        return m_requestProgress;
                    }

                    if (m_requestProgress == RequestStatus.GotSomethingButWaitForMore)
                    {
                        m_requestProgress = RequestStatus.Success;
                        smallWait         = null; // Wait a little longer.
                    }
                    else if (TimeUtils.isTimeoutExpired(smallWait))
                    {
                        return RequestStatus.Success;
                    }
                }
            }
            finally
            {
                m_requestHandler = null;
            }
        }
    }

    private void notifyLine(String line)
    {
        LoggerInstance.debugVerbose("notifyLine: %s", line);

        synchronized (m_lockRequest)
        {
            if (m_requestHandler != null)
            {
                try
                {
                    RequestStatus progress = m_requestHandler.apply(line);
                    if (progress != null)
                    {
                        m_requestProgress = progress;
                        switch (m_requestProgress)
                        {
                            case GotSomethingButWaitForMore:
                                break;

                            case Failure:
                            case Success:
                            case Timeout:
                                m_requestHandler = null;
                                break;
                        }
                    }
                }
                catch (Exception e)
                {
                    LoggerInstance.debug("Error while dispatching line '%s': %s", line, e);
                }
            }

            m_lockRequest.notifyAll();
        }
    }

    //--//

    RequestStatus parseChunk(Results results,
                             int[] chunk,
                             boolean asMultiple)
    {
        if (isCAN(chunk, asMultiple))
        {
            int sourceAddress = chunk[0] & 0xF;

            LoggerInstance.debugVerbose("Got CAN message: source:%d", sourceAddress);

            if (asMultiple)
            {
                int selector = chunk[1];
                int seq      = selector & 0xF;
                int frame    = selector & 0xF0;

                LoggerInstance.debugVerbose("Frame: %x Seq:%d", frame, seq);

                if (frame == 0x10)
                {
                    int length = (seq << 8) | chunk[2];

                    MessageCAN res = new MessageCAN(length, sourceAddress);
                    results.bySource.put(sourceAddress, res);

                    return res.append(chunk, 3, chunk.length - 3);
                }
                else if (frame == 0x20)
                {
                    MessageCAN res = (MessageCAN) results.bySource.get(sourceAddress);
                    if (res == null)
                    {
                        return null;
                    }

                    return res.append(chunk, 2, chunk.length - 2);
                }
            }
            else
            {
                int length = chunk[1];

                Message res = new MessageCAN(length, sourceAddress);
                results.bySource.put(sourceAddress, res);

                return res.append(chunk, 2, length);
            }
        }

        if (isJ1939(chunk, asMultiple))
        {
            int priority           = chunk[0];
            int pduFormat          = chunk[1];
            int destinationAddress = chunk[2];
            int sourceAddress      = chunk[3];

            LoggerInstance.debugVerbose("Got J1939 message: source:%d", sourceAddress);

            if (asMultiple)
            {
                int selector = chunk[4];
                int seq      = selector & 0xF;
                int frame    = selector & 0xF0;

                LoggerInstance.debugVerbose("Frame: %x Seq:%d", frame, seq);

                if (frame == 0x10)
                {
                    int length = (seq << 8) | chunk[5];

                    LoggerInstance.debugVerbose("Length: %d", length);
                    MessageJ1939 res = new MessageJ1939(length, priority, pduFormat, destinationAddress, sourceAddress);
                    results.bySource.put(sourceAddress, res);

                    return res.append(chunk, 6, chunk.length - 6);
                }
                else if (frame == 0x20)
                {
                    LoggerInstance.debugVerbose("Sequence: %d", seq);
                    MessageJ1939 res = (MessageJ1939) results.bySource.get(sourceAddress);
                    if (res == null)
                    {
                        return null;
                    }

                    return res.append(chunk, 5, chunk.length - 5);
                }
            }
            else
            {
                int length = chunk[4];

                Message res = new MessageJ1939(length, priority, pduFormat, destinationAddress, sourceAddress);
                results.bySource.put(sourceAddress, res);

                return res.append(chunk, 5, chunk.length - 5);
            }
        }

        return null;
    }

    private static boolean isError(String line)
    {
        return StringUtils.equals(line, "NODATA") || StringUtils.equals(line, "NO DATA");
    }

    private static int[] decodeChunk(String line)
    {
        String[] parts = StringUtils.split(line, ' ');
        if (parts.length < 4)
        {
            return null;
        }

        int[] res = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
        {
            try
            {
                res[i] = Integer.valueOf(parts[i], 16);
            }
            catch (NumberFormatException e)
            {
                return null;
            }
        }

        return res;
    }

    private static boolean isCAN(int[] chunk,
                                 boolean asMultiple)
    {
        if (chunk.length < 1)
        {
            return false;
        }

        int mask = chunk[0] & 0xFF0;
        switch (mask)
        {
            case 0x7E0:
                break;

            default:
                return false;
        }

        return areSingleBytes(chunk, 1, asMultiple ? 4 : 3);
    }

    private static boolean isJ1939(int[] chunk,
                                   boolean asMultiple)
    {
        return areSingleBytes(chunk, 0, asMultiple ? 6 : 5);
    }

    private static boolean areSingleBytes(int[] chunk,
                                          int offset,
                                          int count)
    {
        for (int i = 0; i < count; i++)
        {
            if (!fitsInByte(chunk, offset + i))
            {
                return false;
            }
        }

        return true;
    }

    private static boolean fitsInByte(int[] chunk,
                                      int offset)
    {
        return offset < chunk.length && fitsInByte(chunk[offset]);
    }

    private static boolean fitsInByte(int val)
    {
        return val >= 0x00 && val <= 0xFF;
    }
}
