/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.crypto.SecretKey;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.MessageBusStatistics;
import com.optio3.cloud.messagebus.payload.MbControl_UpgradeToUDP_Reply;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.concurrency.DelayedAction;
import com.optio3.concurrency.Executors;
import com.optio3.logging.ILogger;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerPeriodic;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationScaling;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.Encryption;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class JsonDatagram<T> extends JsonConnection<T>
{
    public static class ForBandwidth
    {
    }

    public static class ForStatistics
    {
    }

    public static class ForMessage
    {
    }

    public static final Logger LoggerInstance              = JsonConnection.LoggerInstance.createSubLogger(JsonDatagram.class);
    public static final Logger LoggerInstanceForBandwidth  = LoggerInstance.createSubLogger(ForBandwidth.class);
    public static final Logger LoggerInstanceForStatistics = LoggerInstance.createSubLogger(ForStatistics.class);
    public static final Logger LoggerInstanceForMessage    = LoggerInstance.createSubLogger(ForMessage.class);

    public static final  int PROTOCOL_V1     = 1;
    private static final int MAX_PACKET_SIZE = 600;
    private static final int FRAGMENT_SIZE   = 180;

    //--//

    public static class SessionConfiguration
    {
        public       List<InetAddress> hosts;
        public final int               port;

        public final boolean isClientSide;

        public final short  headerId;
        public final byte[] headerKey;

        public final long           sessionId;
        public final byte[]         sessionKey;
        public final MonotonousTime sessionExpiration;

        public final String endpointId;

        public CookiePrincipal principal = CookiePrincipal.createEmpty();

        int m_lastMessageId;
        int m_counter;

        private boolean        m_stale;
        private MonotonousTime m_noActivityTimeout;

        private final Encryption.AES128SingleBlock.Encrypt m_headerEncrypt;
        private final Encryption.AES128SingleBlock.Decrypt m_headerDecrypt;
        private final Encryption.AES128SingleBlock.Encrypt m_sessionEncrypt;
        private final Encryption.AES128SingleBlock.Decrypt m_sessionDecrypt;

        private final byte[]                 m_src        = new byte[Encryption.AES128SingleBlock.BLOCKSIZE];
        private final byte[]                 m_dst        = new byte[Encryption.AES128SingleBlock.BLOCKSIZE];
        private final OutputBuffer           m_obSrc      = new OutputBuffer();
        private final ExpandableArrayOfBytes m_ibDstArray = ExpandableArrayOfBytes.create();
        private final InputBuffer            m_ibDst      = InputBuffer.takeOwnership(m_ibDstArray);

        public SessionConfiguration(int port)
        {
            this.port = port;

            isClientSide = false;

            headerId  = (short) Encryption.generateRandomValue32Bit();
            headerKey = Encryption.generateRandomValues(Encryption.AES128SingleBlock.BLOCKSIZE);

            sessionId         = 0;
            sessionKey        = null;
            sessionExpiration = null;

            endpointId = null;

            SecretKey key = Encryption.AES128SingleBlock.prepareKey(headerKey);
            m_headerEncrypt  = new Encryption.AES128SingleBlock.Encrypt(key);
            m_headerDecrypt  = new Encryption.AES128SingleBlock.Decrypt(key);
            m_sessionEncrypt = null;
            m_sessionDecrypt = null;
        }

        public SessionConfiguration(MbControl_UpgradeToUDP_Reply reply,
                                    boolean isClientSide)
        {
            port = reply.port;

            this.isClientSide = isClientSide;

            m_lastMessageId = Encryption.generateRandomValue32Bit() & 0x0FFF_FFFF;

            if (isClientSide)
            {
                m_lastMessageId |= 0x1000_0000; // A bias to make it easier to distinguish server/client messages.
            }

            headerId  = reply.headerId;
            headerKey = reply.headerKey;

            sessionId         = reply.sessionId;
            sessionKey        = reply.sessionKey;
            sessionExpiration = TimeUtils.computeTimeoutExpiration(reply.sessionValidity, TimeUnit.SECONDS);

            endpointId = reply.endpointId;

            {
                SecretKey key = Encryption.AES128SingleBlock.prepareKey(headerKey);
                m_headerEncrypt = new Encryption.AES128SingleBlock.Encrypt(key);
                m_headerDecrypt = new Encryption.AES128SingleBlock.Decrypt(key);
            }

            {
                SecretKey key = Encryption.AES128SingleBlock.prepareKey(sessionKey);
                m_sessionEncrypt = new Encryption.AES128SingleBlock.Encrypt(key);
                m_sessionDecrypt = new Encryption.AES128SingleBlock.Decrypt(key);
            }

            markActivity();
        }

        public void markAsStale()
        {
            m_stale = true;
        }

        public boolean isValid()
        {
            return !m_stale;
        }

        public boolean hasActivity()
        {
            return m_noActivityTimeout != null && !TimeUtils.isTimeoutExpired(m_noActivityTimeout);
        }

        //--//

        public void markActivity()
        {
            m_noActivityTimeout = TimeUtils.computeTimeoutExpiration(6, TimeUnit.HOURS);
        }

        public PublicHeader decodePublicHeader(InputBuffer ib)
        {
            return fromInputBuffer(ib, new PublicHeader());
        }

        public boolean matchPublicHeader(PublicHeader publicHeader)
        {
            return publicHeader != null && publicHeader.version == PROTOCOL_V1 && publicHeader.transportId == headerId;
        }

        public synchronized void encodeSharedHeader(OutputBuffer ob,
                                                    SharedHeader header) throws
                                                                         Exception
        {
            m_obSrc.reset();
            SerializationHelper.write(m_obSrc, header);

            m_obSrc.toByteArray(0, m_src, 0, m_dst.length);

            m_headerEncrypt.exec(m_src, 0, m_dst, 0);

            ob.emit(m_dst);
        }

        public synchronized SharedHeader decodeSharedHeader(InputBuffer ib)
        {
            try
            {
                ib.read(m_src, 0, Encryption.AES128SingleBlock.BLOCKSIZE);

                m_headerDecrypt.exec(m_src, 0, m_dst, 0);

                m_ibDstArray.fromArray(m_dst);
                m_ibDst.setPosition(0);

                return fromInputBuffer(m_ibDst, new SharedHeader());
            }
            catch (Exception e)
            {
                LoggerInstance.debug("Failed to decrypt shared header, due to %s", e);

                return null;
            }
        }

        public synchronized void encodeFrame(OutputBuffer ob,
                                             SharedHeader header,
                                             OutputBuffer obFrame) throws
                                                                   Exception
        {
            int len = obFrame.size();

            header.frameLength = len;
            encodeSharedHeader(ob, header);

            for (int pos = 0; pos < len; pos += Encryption.AES128SingleBlock.BLOCKSIZE)
            {
                obFrame.toByteArray(pos, m_src, 0, Math.min(Encryption.AES128SingleBlock.BLOCKSIZE, len - pos));
                m_sessionEncrypt.exec(m_src, 0, m_dst, 0);
                ob.emit(m_dst);
            }
        }

        public synchronized InputBuffer decodeFrame(InputBuffer ib,
                                                    SharedHeader header)
        {
            try
            {
                ExpandableArrayOfBytes frame = ExpandableArrayOfBytes.create();

                int len = header.frameLength;
                if (len > 0)
                {
                    for (int pos = 0; pos < len; pos += Encryption.AES128SingleBlock.BLOCKSIZE)
                    {
                        ib.read(m_src, 0, Encryption.AES128SingleBlock.BLOCKSIZE);
                        m_sessionDecrypt.exec(m_src, 0, m_dst, 0);

                        frame.addRange(m_dst, 0, Math.min(Encryption.AES128SingleBlock.BLOCKSIZE, len - pos));
                    }
                }

                return InputBuffer.takeOwnership(frame);
            }
            catch (Exception e)
            {
                LoggerInstance.debug("Failed to decrypt frame, due to %s", e);

                return null;
            }
        }
    }

    public static abstract class DatagramSocketWorker
    {
        private DatagramSocket m_socket;

        protected DatagramSocketWorker(int port)
        {
            try
            {
                if (port == 0)
                {
                    m_socket = new DatagramSocket();
                }
                else
                {
                    m_socket = new DatagramSocket(port);

                    LoggerInstance.info("Starting UDP Server...");
                }
            }
            catch (Throwable t)
            {
                throw new RuntimeException(t);
            }

            Thread worker = new Thread(this::worker);
            worker.setDaemon(true);
            worker.setName("JsonDatagram");
            worker.start();
        }

        public void close()
        {
            DatagramSocket socket = m_socket;
            if (socket != null)
            {
                m_socket = null;
                socket.close();
            }
        }

        private void worker()
        {
            int            sleepBetweenFailures = 1;
            DatagramSocket socket               = m_socket;

            final byte[]         data      = new byte[1500];
            final DatagramPacket udpPacket = new DatagramPacket(data, data.length);

            while (!socket.isClosed())
            {
                try
                {
                    if (socket.getSoTimeout() == 0)
                    {
                        socket.setSoTimeout(10000);
                    }

                    socket.receive(udpPacket);

                    InetSocketAddress source = new InetSocketAddress(udpPacket.getAddress(), udpPacket.getPort());
                    InputBuffer       ib     = InputBuffer.createFrom(udpPacket.getData(), 0, udpPacket.getLength());

                    if (LoggerInstanceForMessage.isEnabled(Severity.DebugObnoxious))
                    {
                        LoggerInstanceForMessage.debugObnoxious("Got packet from %s", source);
                        BufferUtils.convertToHex(ib.toArray(), 0, ib.size(), 16, true, (line) -> LoggerInstanceForMessage.debugObnoxious(" %s", line));
                    }

                    Executors.getDefaultThreadPool()
                             .execute(() -> processPacket(source, ib));
                }
                catch (SocketTimeoutException se)
                {
                    if (m_socket == null)
                    {
                        // The manager has been stopped, exit.
                        break;
                    }
                }
                catch (Exception e)
                {
                    if (m_socket == null)
                    {
                        // The manager has been stopped, exit.
                        break;
                    }

                    Executors.safeSleep(sleepBetweenFailures);

                    // Exponential backoff if something goes wrong.
                    sleepBetweenFailures = 2 * sleepBetweenFailures;
                    continue;
                }

                sleepBetweenFailures = 1;
            }
        }

        //--//

        protected abstract void processPacket(InetSocketAddress source,
                                              InputBuffer ib);

        public void sendPacket(InetSocketAddress target,
                               byte[] data)
        {
            try
            {
                DatagramSocket socket = m_socket;
                if (socket != null)
                {
                    DatagramPacket p = new DatagramPacket(data, data.length, target.getAddress(), target.getPort());
                    socket.send(p);
                }
            }
            catch (Throwable t)
            {
                // Ignore send failures.
            }
        }
    }

    //--//

    public static class PublicHeader
    {
        public static final int SIZE = 3;

        @SerializationTag(number = 1, width = 8)
        public byte version;

        @SerializationTag(number = 2, width = 16)
        public short transportId;
    }

    public static class SharedHeader
    {
        public static final int SIZE = 16;

        @SerializationTag(number = 1, bitOffset = 0, width = 12, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int frameLength;

        @SerializationTag(number = 1, bitOffset = 12, width = 29 - 12, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int spare1;

        @SerializationTag(number = 1, bitOffset = 30, width = 1)
        public boolean close;

        @SerializationTag(number = 1, bitOffset = 31, width = 1)
        public boolean keepAlive;

        @SerializationTag(number = 2)
        public int counter;

        @SerializationTag(number = 3)
        public long sessionId;
    }

    public static class FragmentHeader
    {
        public static final int SIZE = 2;

        @SerializationTag(number = 1, bitOffset = 0, width = 11, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int length;

        @SerializationTag(number = 1, bitOffset = 14, width = 1)
        public boolean ack;

        @SerializationTag(number = 1, bitOffset = 15, width = 1)
        public boolean last;
    }

    public static class DataPayload
    {
        public static final int SIZE = 6;

        @SerializationTag(number = 1, width = 32)
        public int messageId;

        @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int fragmentOffset;

        @SerializationTag(number = 3)
        public byte[] data;
    }

    public static class AckPayload
    {
        public static final int SIZE = 4;

        @SerializationTag(number = 1, width = 32)
        public int messageId;

        @SerializationTag(number = 2)
        public List<AckRange> ranges;
    }

    public static class AckRange
    {
        public static final int SIZE = 4;

        @SerializationTag(number = 1, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int rangeStart;

        @SerializationTag(number = 2, width = 16, scaling = { @SerializationScaling(assumeUnsigned = true) })
        public int rangeEnd;
    }

    //--//

    private static class TransmissionWindowEstimator
    {
        private static final int   c_windowSizeMin        = 10;
        private static final float c_windowSizeGrowthRate = 0.2f;
        private static final float c_ackDelayMin          = 3;  // Don't wait less than X seconds for an ack, regardless of load.
        private static final float c_ackDelayMax          = 10; // Don't wait more than X seconds for an ack, regardless of load.
        private static final float c_ackDelay             = 25 * c_fromMilliToTimestamp;

        float timestampCycle;
        private int outstanding;

        private float m_roundTrip        = 1; // Start with a one-second roundtrip.
        private float m_windowSize       = 1;
        private float m_windowSizeGrowth = 1;
        private float m_ackDelay         = m_roundTrip;

        void startCycle()
        {
            timestampCycle = newTimestamp();
        }

        float canSend()
        {
            if (outstanding < (int) m_windowSize)
            {
                outstanding++;

                LoggerInstanceForBandwidth.debugVerbose("Outstanding INC: %d (window %f)", outstanding, m_windowSize);

                return timestampCycle + m_ackDelay;
            }

            return 0;
        }

        void handleAck(float sent,
                       float ack)
        {
            if (outstanding == (int) m_windowSize)
            {
                m_windowSize += m_windowSizeGrowth;

                // Slowly increase growth rate.
                m_windowSizeGrowth = Math.min(1.5f, m_windowSizeGrowth + c_windowSizeGrowthRate);
            }

            outstanding--;
            LoggerInstanceForBandwidth.debugVerbose("Outstanding DEC: %d (window %f)", outstanding, m_windowSize);

            //--//

            //
            // We add some delay to all acknowledgements, to batch them. Reverse that.
            //
            ack -= c_ackDelay;

            float roundTrip = ack - sent;

            if (roundTrip <= 0)
            {
                // Skip "negative" time roundtrips.
                return;
            }

            if (m_roundTrip != roundTrip)
            {
                LoggerInstanceForBandwidth.debug("New Roundtrip: %f", roundTrip);

                m_roundTrip = roundTrip;
            }

            // Good exchange, decrease timeout.
            m_ackDelay = Math.max(c_ackDelayMin, m_ackDelay - c_ackDelay);
        }

        void handleTimeout()
        {
            if (m_windowSize > c_windowSizeMin)
            {
                m_windowSize -= 1.0f;
                m_windowSizeGrowth = c_windowSizeGrowthRate; // Reduce growth rate.
            }

            // Bad exchange, increase timeout.
            m_ackDelay = Math.min(c_ackDelayMax, m_ackDelay + c_ackDelay);

            outstanding--;
            LoggerInstanceForBandwidth.debugVerbose("Outstanding TIM: %d (window %f)", outstanding, m_windowSize);
        }
    }

    //-//

    private static class BaseFrame
    {
        float   timeout;
        Integer messageId;

        protected BaseFrame(int messageId)
        {
            this.messageId = messageId;
            rescheduleTimeout(newTimestamp());
        }

        void rescheduleTimeout(float now)
        {
            timeout = now + 2 * 60; // Two minutes
        }

        boolean hasExpired(float now)
        {
            return timeout < now;
        }
    }

    static class TxFragment
    {
        byte[] data;

        float timestampSend;
        float timestampResend = Float.MAX_VALUE;
    }

    class TxFrame extends BaseFrame
    {
        private final TxFragment[] fragments;
        private final int          fragmentCount;
        private       int          fragmentRemaining;
        private       int          fragmentFirstNotAcknowledged;

        TxFrame(int messageId,
                byte[] data)
        {
            super(messageId);

            m_transmitQueue.put(this.messageId, this);

            //--//

            int length = data.length;
            fragmentCount     = (length + FRAGMENT_SIZE - 1) / FRAGMENT_SIZE;
            fragmentRemaining = fragmentCount;
            fragments         = new TxFragment[fragmentCount];

            for (int fragmentOffset = 0; fragmentOffset < fragmentCount; fragmentOffset++)
            {
                TxFragment fragment = new TxFragment();
                fragment.data = Arrays.copyOfRange(data, fragmentOffset * FRAGMENT_SIZE, Math.min(length, (fragmentOffset + 1) * FRAGMENT_SIZE));

                fragments[fragmentOffset] = fragment;
            }
        }

        void processAck(float receiveTimestamp,
                        AckPayload payload)
        {
            rescheduleTimeout(receiveTimestamp);

            if (payload.ranges != null)
            {
                for (AckRange range : payload.ranges)
                {
                    LoggerInstanceForMessage.debug("[%08X] Received Ack: [%d-%d]", messageId, range.rangeStart, range.rangeEnd);

                    int startOffset = Math.max(fragmentFirstNotAcknowledged, range.rangeStart);
                    int endOffset   = Math.min(fragmentCount - 1, range.rangeEnd);

                    for (int offset = startOffset; offset <= endOffset; offset++)
                    {
                        TxFragment fragment = fragments[offset];
                        if (fragment.data != null)
                        {
                            LoggerInstanceForMessage.debugVerbose("[%08X] Received Ack: Offset=%d", messageId, offset);

                            fragment.data = null;
                            fragmentRemaining--;

                            mark(fragment, receiveTimestamp);
                        }

                        if (offset == fragmentFirstNotAcknowledged)
                        {
                            fragmentFirstNotAcknowledged++;
                        }
                    }
                }
            }
        }

        float sendNextFragments(float nextActivity)
        {
            for (int fragmentOffset = fragmentFirstNotAcknowledged; fragmentOffset < fragmentCount; fragmentOffset++)
            {
                TxFragment fragment = fragments[fragmentOffset];

                byte[] pendingData = fragment.data;
                if (pendingData != null)
                {
                    if (fragment.timestampResend == Float.MAX_VALUE)
                    {
                        float resendTime = m_transmissionWindowEstimator.canSend();
                        if (resendTime > 0)
                        {
                            if (fragment.timestampSend == 0)
                            {
                                LoggerInstanceForMessage.debug("[%08X] Send Fragment %d", messageId, fragmentOffset);
                            }
                            else
                            {
                                LoggerInstanceForMessage.debug("[%08X] Resend Fragment %d", messageId, fragmentOffset);

                                m_statistics.packetTxBytesResent += pendingData.length;
                            }

                            fragment.timestampSend   = m_transmissionWindowEstimator.timestampCycle;
                            fragment.timestampResend = resendTime;

                            LoggerInstanceForMessage.debugVerbose("[%08X] Next Activity: %f ", messageId, fragment.timestampResend - fragment.timestampSend);

                            m_frameBuilder.pushFragment(messageId, fragmentOffset, pendingData, fragmentOffset + 1 == fragmentCount);
                        }
                    }
                    else
                    {
                        nextActivity = Math.min(nextActivity, fragment.timestampResend);
                    }
                }
                else if (fragmentOffset == fragmentFirstNotAcknowledged)
                {
                    fragmentFirstNotAcknowledged++;
                }
            }

            return nextActivity;
        }

        void processTimeouts()
        {
            for (int fragmentOffset = fragmentFirstNotAcknowledged; fragmentOffset < fragmentCount; fragmentOffset++)
            {
                TxFragment fragment = fragments[fragmentOffset];
                if (fragment.data != null && fragment.timestampResend < m_transmissionWindowEstimator.timestampCycle)
                {
                    mark(fragment, 0);
                }
            }
        }

        void expireOutstanding()
        {
            for (int fragmentOffset = fragmentFirstNotAcknowledged; fragmentOffset < fragmentCount; fragmentOffset++)
            {
                mark(fragments[fragmentOffset], 0);
            }
        }

        private void mark(TxFragment fragment,
                          float receiveTimestamp)
        {
            if (fragment.timestampResend != Float.MAX_VALUE)
            {
                fragment.timestampResend = Float.MAX_VALUE;

                if (receiveTimestamp > 0)
                {
                    m_transmissionWindowEstimator.handleAck(fragment.timestampSend, receiveTimestamp);
                }
                else
                {
                    m_transmissionWindowEstimator.handleTimeout();
                }
            }
        }
    }

    static class RxFragment
    {
        int     offset;
        byte[]  data;
        boolean acknowledged;
    }

    class RxFrame extends BaseFrame
    {
        int          fragmentTotal; // > 0 means we know how long the frame should be.
        int          fragmentCount;
        RxFragment[] fragments = new RxFragment[16];
        float        ackNeeded = Float.MAX_VALUE;
        int          ackCursor = 0;

        RxFrame(int messageId)
        {
            super(messageId);

            m_receiveQueue.put(this.messageId, this);
        }

        private void markAcknowledgeScanDone()
        {
            ackNeeded = Float.MAX_VALUE;
            ackCursor = 0;
        }

        float sendNextFragments(float nextActivity)
        {
            while (ackNeeded < m_transmissionWindowEstimator.timestampCycle)
            {
                int availableSlots = (m_frameBuilder.available() - AckPayload.SIZE) / AckRange.SIZE;
                if (availableSlots > 0)
                {
                    m_frameBuilder.ranges.clear();

                    if (fragments == null)
                    {
                        availableSlots--;

                        AckRange range = new AckRange();
                        range.rangeStart = 0;
                        range.rangeEnd   = fragmentTotal - 1;
                        m_frameBuilder.ranges.add(range);

                        LoggerInstanceForMessage.debug("[%08X] Ack Rx Frame (complete)", messageId);

                        markAcknowledgeScanDone();
                    }
                    else
                    {
                        AckRange range = null;

                        while (true)
                        {
                            if (ackCursor >= fragments.length)
                            {
                                LoggerInstanceForMessage.debug("[%08X] Ack Rx Frame (complete)", messageId);

                                markAcknowledgeScanDone();
                                break;
                            }

                            RxFragment fragment = fragments[ackCursor++];
                            if (fragment != null)
                            {
                                if (range == null)
                                {
                                    if (availableSlots == 0)
                                    {
                                        // No more room, try with the next frame.
                                        break;
                                    }

                                    availableSlots--;

                                    range            = new AckRange();
                                    range.rangeStart = fragment.offset;
                                    m_frameBuilder.ranges.add(range);
                                }

                                range.rangeEnd = fragment.offset;
                            }
                            else
                            {
                                range = null;
                            }
                        }
                    }

                    m_frameBuilder.pushFragment(messageId, m_frameBuilder.ranges);
                }

                if (availableSlots <= 0)
                {
                    m_frameBuilder.flush();
                }
            }

            return Math.min(nextActivity, ackNeeded);
        }

        void receivedFragment(float receiveTimestamp,
                              DataPayload payload,
                              boolean last)
        {
            rescheduleTimeout(receiveTimestamp);

            ackNeeded = Math.min(ackNeeded, receiveTimestamp + TransmissionWindowEstimator.c_ackDelay);

            if (fragments != null)
            {
                if (last)
                {
                    fragmentTotal = payload.fragmentOffset + 1;
                }

                int fragmentOffset = payload.fragmentOffset;

                if (fragmentOffset >= fragments.length)
                {
                    fragments = Arrays.copyOf(fragments, fragmentOffset + 16);
                }

                RxFragment newFragment = fragments[fragmentOffset];
                if (newFragment == null)
                {
                    newFragment               = new RxFragment();
                    newFragment.offset        = fragmentOffset;
                    fragments[fragmentOffset] = newFragment;
                }

                newFragment.data = payload.data;

                if (!newFragment.acknowledged)
                {
                    newFragment.acknowledged = true;

                    fragmentCount++;

                    LoggerInstanceForMessage.debug("[%08X] Received Fragment %d%s, %d bytes (%d fragments so far)",
                                                   messageId,
                                                   payload.fragmentOffset,
                                                   last ? " (last)" : "",
                                                   payload.data.length,
                                                   fragmentCount);
                }
                else
                {
                    LoggerInstanceForMessage.debug("[%08X] Received Duplicate Fragment %d, %d bytes", messageId, payload.fragmentOffset, payload.data.length);

                    m_statistics.packetRxBytesResent += payload.data.length;
                }

                if (last)
                {
                    fragmentTotal = payload.fragmentOffset + 1;
                }

                if (fragmentTotal > 0)
                {
                    attemptToReassemble();
                }
            }
            else
            {
                LoggerInstanceForMessage.debug("[%08X] Received Duplicate Fragment %d after done, %d bytes", messageId, payload.fragmentOffset, payload.data.length);

                m_statistics.packetRxBytesResent += payload.data.length;
            }
        }

        //--//

        private void attemptToReassemble()
        {
            int payloadLength = 0;

            for (int fragmentOffset = 0; fragmentOffset < fragmentTotal; fragmentOffset++)
            {
                RxFragment fragment = fragments[fragmentOffset];
                if (fragment == null || fragment.data == null)
                {
                    return;
                }

                payloadLength += fragment.data.length;
            }

            byte[] payload = new byte[payloadLength];
            int    offset  = 0;

            for (int fragmentOffset = 0; fragmentOffset < fragmentTotal; fragmentOffset++)
            {
                RxFragment fragment = fragments[fragmentOffset];
                byte[]     data     = fragment.data;
                int        length   = data.length;

                System.arraycopy(data, 0, payload, offset, length);
                offset += length;
            }

            fragments = null;

            LoggerInstanceForMessage.debug("[%08X] Reassembled from %d fragments, %d bytes", messageId, fragmentTotal, payload.length);

            Executors.getDefaultThreadPool()
                     .execute(() ->
                              {
                                  try
                                  {
                                      ByteArrayInputStream stream = new ByteArrayInputStream(payload, 0, payload.length);
                                      receiveMessageCompressed(getPhysicalConnection(), stream);
                                  }
                                  catch (Throwable t)
                                  {
                                      LoggerInstance.debug("(%08X) Dispatch failed, due to %s", (int) m_cfg.sessionId, t);
                                  }
                              });
        }
    }

    class FrameBuilder
    {
        final List<AckRange> ranges = Lists.newArrayList();

        private final PublicHeader   m_publicHeader          = new PublicHeader();
        private final SharedHeader   m_sharedHeader          = new SharedHeader();
        private final FragmentHeader m_fragmentHeader        = new FragmentHeader();
        private final DataPayload    m_valData               = new DataPayload();
        private final AckPayload     m_valAck                = new AckPayload();
        private final OutputBuffer   m_frame                 = new OutputBuffer();
        private final OutputBuffer   m_fragmentBuffer        = new OutputBuffer();
        private final OutputBuffer   m_fragmentPayloadBuffer = new OutputBuffer();
        private       boolean        m_isActive;

        void activate()
        {
            if (!m_isActive)
            {
                m_publicHeader.version     = PROTOCOL_V1;
                m_publicHeader.transportId = m_cfg.headerId;

                m_sharedHeader.keepAlive = m_keepAlivePending;
                m_sharedHeader.counter   = m_cfg.m_counter++;
                m_sharedHeader.sessionId = m_cfg.sessionId;

                m_isActive         = true;
                m_keepAlivePending = false;

                if (m_cfg.isClientSide && !m_keepAlive.isScheduled())
                {
                    if (hasReceivedAnyFrames())
                    {
                        m_keepAlive.schedule(1, TimeUnit.MINUTES);
                    }
                    else
                    {
                        m_keepAlive.schedule(5, TimeUnit.SECONDS);
                    }
                }
            }
        }

        int available()
        {
            return MAX_PACKET_SIZE - m_frame.size();
        }

        void pushFragment(int messageId,
                          List<AckRange> ranges)
        {
            m_fragmentHeader.ack  = true;
            m_fragmentHeader.last = false;

            m_valAck.messageId = messageId;
            m_valAck.ranges    = ranges;

            pushFragment(m_valAck);
        }

        void pushFragment(int messageId,
                          int fragmentOffset,
                          byte[] data,
                          boolean last)
        {
            m_fragmentHeader.ack  = false;
            m_fragmentHeader.last = last;

            m_valData.messageId      = messageId;
            m_valData.fragmentOffset = fragmentOffset;
            m_valData.data           = data;

            pushFragment(m_valData);
        }

        private void pushFragment(Object val)
        {
            m_fragmentPayloadBuffer.reset();
            SerializationHelper.write(m_fragmentPayloadBuffer, val);
            m_fragmentHeader.length = m_fragmentPayloadBuffer.size();

            m_fragmentBuffer.reset();
            SerializationHelper.write(m_fragmentBuffer, m_fragmentHeader);
            m_fragmentBuffer.emitNestedBlock(m_fragmentPayloadBuffer);

            activate();

            if (available() < m_fragmentBuffer.size())
            {
                //
                // Fragment too big for current frame, flush it, and start new frame.
                //
                flush();
                activate();
            }

            m_frame.emitNestedBlock(m_fragmentBuffer);
        }

        void flush()
        {
            if (m_isActive)
            {
                try
                {
                    try (OutputBuffer ob = toOutputBuffer(m_publicHeader))
                    {
                        m_cfg.encodeFrame(ob, m_sharedHeader, m_frame);

                        LoggerInstance.debug("(%08X) New Tx Packet: %d", (int) m_cfg.sessionId, ob.size());
                        sendFrame(ob);
                    }
                }
                catch (Throwable t)
                {
                    LoggerInstance.debug("(%08X) Failed to send frame, due to %s", (int) m_cfg.sessionId, t);
                }

                m_isActive = false;
                m_frame.reset();
            }
        }
    }

    //--//

    protected final SessionConfiguration m_cfg;
    private final   Object               m_sessionLock  = new Object();
    private final   FrameBuilder         m_frameBuilder = new FrameBuilder();
    private final   DelayedAction        m_resend       = new DelayedAction(this::processOutgoingFrames);

    private final Map<Integer, TxFrame> m_transmitQueue = Maps.newHashMap();
    private final Map<Integer, RxFrame> m_receiveQueue  = Maps.newHashMap();

    private final TransmissionWindowEstimator m_transmissionWindowEstimator = new TransmissionWindowEstimator();

    private       boolean       m_keepAlivePending;
    private       boolean       m_receiveAtLeastOneFrame;
    private final DelayedAction m_keepAlive = new DelayedAction(this::activateKeepAlive);

    private final LoggerPeriodic m_periodicDump = new LoggerPeriodic(LoggerInstanceForStatistics, Severity.Debug, 5, TimeUnit.MINUTES)
    {
        @Override
        protected void onActivation()
        {
            var stats = m_statistics.copy();
            stats.add(s_statisticsTotal);
            dumpStatistics(LoggerInstanceForStatistics, Severity.Debug, stats);
        }
    };

    private final LoggerPeriodic m_periodicDumpFast = new LoggerPeriodic(LoggerInstanceForStatistics, Severity.DebugVerbose, 10, TimeUnit.SECONDS)
    {
        @Override
        protected void onActivation()
        {
            var stats = m_statistics.copy();
            stats.add(s_statisticsTotal);
            dumpStatistics(LoggerInstanceForStatistics, Severity.DebugVerbose, stats);
        }
    };

    protected JsonDatagram(SessionConfiguration cfg)
    {
        LoggerInstance.debug("New Session: %08X", cfg.sessionId);
        m_statistics.sessions = 1;

        m_cfg = cfg;
    }

    //--//

    protected void dumpStatistics(ILogger logger,
                                  Severity severity,
                                  MessageBusStatistics stats)
    {
        logger.log(null,
                   severity,
                   null,
                   null,
                   "(%08X : %,d sessions) TX: %,d bytes (%,d resent) over %,d packets and %,d messages # RX: %,d bytes (%,d resent) over %,d packets and %,d messages (RoundTrip: %f, AckDelay: %f)",
                   (int) m_cfg.sessionId,
                   stats.sessions,
                   stats.packetTxBytes,
                   stats.packetTxBytesResent,
                   stats.packetTx,
                   stats.messageTx,
                   stats.packetRxBytes,
                   stats.packetRxBytesResent,
                   stats.packetRx,
                   stats.messageRx,
                   m_transmissionWindowEstimator.m_roundTrip,
                   m_transmissionWindowEstimator.m_ackDelay);
    }

    protected void recordRxPacket(InputBuffer ib)
    {
        m_statistics.packetRx++;
        m_statistics.packetRxBytes += ib.size();
    }

    protected void recordTxPacket(OutputBuffer ob)
    {
        m_statistics.packetTx++;
        m_statistics.packetTxBytes += ob.size();
    }

    @Override
    public void close()
    {
        onConnectionClosing();

        synchronized (m_sessionLock)
        {
            m_transmitQueue.clear();
            m_receiveQueue.clear();

            m_keepAlive.cancel();
        }

        onConnectionClosed();
    }

    @Override
    public void setTransmitCapabilities(EnumSet<JsonConnectionCapability> required)
    {
    }

    @Override
    public void setReceiveCapabilities(EnumSet<JsonConnectionCapability> required)
    {
    }

    @Override
    public final void sendMessage(T msg,
                                  Consumer<Integer> notifyPayloadSize) throws
                                                                       Exception
    {
        try
        {
            if (!m_cfg.isValid())
            {
                close();
                return;
            }

            int messageId;

            synchronized (m_sessionLock)
            {
                messageId = m_cfg.m_lastMessageId++;
            }

            if (LoggerInstanceForMessage.isEnabled(Severity.DebugVerbose))
            {
                LoggerInstanceForMessage.debugVerbose("[%08X] New Message: %s", messageId, ObjectMappers.prettyPrintAsJson(msg));
            }

            ByteArrayOutputStream compressed = prepareMessageCompressed(msg, notifyPayloadSize);

            synchronized (m_sessionLock)
            {
                m_statistics.messageTx++;

                TxFrame frame = new TxFrame(messageId, compressed.toByteArray());

                LoggerInstanceForMessage.debug("[%08X] New Tx Frame, %d fragments", messageId, frame.fragmentCount);
            }

            processOutgoingFrames();
        }
        catch (Exception e)
        {
            LoggerInstance.error("SendMessage received an unexpected exception: %s", e);

            close();

            throw e;
        }
    }

    //--//

    private static final float c_fromNanoToTimestamp  = 1 / 1E9f;
    private static final float c_fromTimestampToMilli = 1000.0f;
    private static final float c_fromMilliToTimestamp = 1 / c_fromTimestampToMilli;

    private static float newTimestamp()
    {
        return System.nanoTime() * c_fromNanoToTimestamp;
    }

    private long fromTimestampToDelay(float target)
    {
        return (long) ((target - newTimestamp()) * c_fromTimestampToMilli);
    }

    //--//

    protected PublicHeader decodePublicHeader(InputBuffer ib)
    {
        return m_cfg.decodePublicHeader(ib);
    }

    protected boolean matchPublicHeader(PublicHeader publicHeader)
    {
        return m_cfg.isValid() && m_cfg.matchPublicHeader(publicHeader);
    }

    protected void markAsStale()
    {
        m_cfg.markAsStale();
    }

    protected SharedHeader decodeSharedHeader(InputBuffer ib)
    {
        return m_cfg.decodeSharedHeader(ib);
    }

    protected void processIncomingFrame(InputBuffer ib,
                                        SharedHeader header)
    {
        LoggerInstance.debug("(%08X) New Rx Packet: %d", (int) m_cfg.sessionId, ib.size());

        ib = m_cfg.decodeFrame(ib, header);
        if (ib == null)
        {
            return;
        }

        if (LoggerInstance.isEnabled(Severity.DebugVerbose))
        {
            LoggerInstance.debugVerbose("ProcessIncomingFrame: %s", ObjectMappers.prettyPrintAsJson(header));
        }

        m_receiveAtLeastOneFrame = true;

        if (header.keepAlive)
        {
            receivedKeepAlive();
        }

        if (header.close)
        {
            close();
            return;
        }

        float receiveTimestamp = newTimestamp();

        try
        {
            synchronized (m_sessionLock)
            {
                while (!ib.isEOF())
                {
                    FragmentHeader fragmentHeader = new FragmentHeader();
                    SerializationHelper.read(ib, fragmentHeader);

                    InputBuffer ibSub = ib.readNestedBlock(fragmentHeader.length);

                    if (fragmentHeader.ack)
                    {
                        AckPayload ack = new AckPayload();
                        SerializationHelper.read(ibSub, ack);

                        TxFrame frame = m_transmitQueue.get(ack.messageId);
                        if (frame != null)
                        {
                            frame.processAck(receiveTimestamp, ack);
                        }
                    }
                    else
                    {
                        DataPayload data = new DataPayload();
                        SerializationHelper.read(ibSub, data);

                        RxFrame frame = m_receiveQueue.get(data.messageId);
                        if (frame == null)
                        {
                            frame = new RxFrame(data.messageId);

                            LoggerInstanceForMessage.debug("[%08X] New Rx Frame", frame.messageId);
                        }

                        frame.receivedFragment(receiveTimestamp, data, fragmentHeader.last);
                    }
                }
            }

            processOutgoingFrames();
        }
        catch (Throwable t)
        {
            LoggerInstance.debug("Failed to decode frame, due to %s", t);
        }
    }

    private void processOutgoingFrames()
    {
        if (!m_cfg.isValid())
        {
            close();
            return;
        }

        m_periodicDump.process();
        m_periodicDumpFast.process();

        synchronized (m_sessionLock)
        {
            if (m_keepAlivePending)
            {
                m_frameBuilder.activate();
            }

            m_transmissionWindowEstimator.startCycle();

            //--//

            float nextActivity = Float.MAX_VALUE;

            {
                Iterator<TxFrame> it = m_transmitQueue.values()
                                                      .iterator();
                while (it.hasNext())
                {
                    TxFrame frame = it.next();

                    if (frame.fragmentRemaining <= 0)
                    {
                        it.remove();
                        continue;
                    }

                    if (frame.hasExpired(m_transmissionWindowEstimator.timestampCycle))
                    {
                        LoggerInstanceForMessage.debug("[%08X] Expire Tx Frame", frame.messageId);

                        frame.expireOutstanding();
                        it.remove();
                        continue;
                    }

                    frame.processTimeouts();
                }
            }

            if (m_transmitQueue.isEmpty())
            {
                // Just in case we get out of sync...
                if (m_transmissionWindowEstimator.outstanding != 0)
                {
                    LoggerInstanceForBandwidth.debugVerbose("Outstanding RESET: %d", m_transmissionWindowEstimator.outstanding);
                    m_transmissionWindowEstimator.outstanding = 0;
                }
            }

            if (m_receiveAtLeastOneFrame) // Don't sent fragments until we receive at least one keep alive.
            {
                for (TxFrame frame : m_transmitQueue.values())
                {
                    nextActivity = frame.sendNextFragments(nextActivity);
                }
            }

            {
                Iterator<RxFrame> it = m_receiveQueue.values()
                                                     .iterator();
                while (it.hasNext())
                {
                    RxFrame frame = it.next();

                    if (frame.hasExpired(m_transmissionWindowEstimator.timestampCycle))
                    {
                        if (frame.fragments != null)
                        {
                            LoggerInstanceForMessage.debug("[%08X] Expire Rx Frame due to timeout", frame.messageId);
                        }
                        else
                        {
                            LoggerInstanceForMessage.debugVerbose("[%08X] Expire Rx Frame", frame.messageId);
                        }

                        it.remove();
                        continue;
                    }

                    nextActivity = frame.sendNextFragments(nextActivity);
                }
            }

            m_frameBuilder.flush();

            //--//

            if (nextActivity < Float.MAX_VALUE)
            {
                long delay = Math.max(50, fromTimestampToDelay(nextActivity));

                m_resend.schedule(delay, TimeUnit.MILLISECONDS);
            }
        }
    }

    //--//

    protected abstract InetSocketAddress getPhysicalConnection();

    protected void markActivity()
    {
        m_cfg.markActivity();
    }

    protected void activateKeepAlive()
    {
        m_keepAlivePending = true;
        processOutgoingFrames();
    }

    protected abstract void receivedKeepAlive();

    protected boolean hasReceivedAnyFrames()
    {
        return m_receiveAtLeastOneFrame;
    }

    protected abstract void sendFrame(OutputBuffer ob);

    //--//

    public static OutputBuffer toOutputBuffer(Object val)
    {
        OutputBuffer ob = new OutputBuffer();
        SerializationHelper.write(ob, val);
        return ob;
    }

    public static <T> T fromInputBuffer(InputBuffer ib,
                                        T val)
    {
        try
        {
            SerializationHelper.read(ib, val);

            return val;
        }
        catch (Throwable t)
        {
            LoggerInstance.debug("Failed to decode %s, due to %s", val.getClass(), t);

            return null;
        }
    }
}
