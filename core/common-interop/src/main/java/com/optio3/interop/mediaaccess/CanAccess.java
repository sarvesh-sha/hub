/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.interop.mediaaccess;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.interop.FileDescriptorAccess;
import com.optio3.interop.FileDescriptorAccessCleaner;
import com.optio3.logging.Logger;
import com.optio3.util.ProcessUtils;
import com.sun.jna.LastErrorException;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public final class CanAccess implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(CanAccess.class);

    private static final int PF_CAN   = 29;
    private static final int AF_CAN   = PF_CAN;
    private static final int SOCK_RAW = 3;
    private static final int CAN_RAW  = 1; /* RAW sockets */

    private static final int SIOCGIFMTU = 0x8921; // get MTU size

    private static final int EWOULDBLOCK = 11;

    private static final int SOL_CAN_BASE = 100;
    private static final int SOL_CAN_RAW  = (SOL_CAN_BASE + CAN_RAW);

    private static final int CAN_RAW_FILTER        = 1; // set 0 .. n can_filter(s)
    private static final int CAN_RAW_ERR_FILTER    = 2; // set filter for error frames
    private static final int CAN_RAW_LOOPBACK      = 3; // local loopback (default:on)
    private static final int CAN_RAW_RECV_OWN_MSGS = 4; // receive my own msgs (default:off)
    private static final int CAN_RAW_FD_FRAMES     = 5; // allow CAN FD frames (default:off)
    private static final int CAN_RAW_JOIN_FILTERS  = 6; // all filters must match to trigger

    //--//

    public static class singleWord32 extends Structure
    {
        public int value;

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("value");
        }
    }

    public static class sockaddr_can extends Structure
    {
        public short can_family;
        public int   can_ifindex;

        public int rx_id; // C type : canid_t
        public int tx_id; // C type : canid_t

        public sockaddr_can(int ifIndex)
        {
            can_family = AF_CAN;
            can_ifindex = ifIndex;
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("can_family", "can_ifindex", "rx_id", "tx_id");
        }
    }

    public static abstract class ifreq extends Structure
    {
        public byte[] ifrn_name = new byte[16];

        protected ifreq(String name)
        {
            byte[] buf = Native.toByteArray(name);

            System.arraycopy(buf, 0, ifrn_name, 0, buf.length);
        }
    }

    public static class ifreq_mtu extends ifreq
    {
        public int ifru_mtu;

        public ifreq_mtu(String name)
        {
            super(name);
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("ifrn_name", "ifru_mtu");
        }
    }

    public static class can_frame extends Structure
    {
        /* special address description flags for the CAN_ID */
        public static final int EFF_FLAG = (int) 0x80000000L; /* EFF/SFF is set in the MSB */
        public static final int RTR_FLAG = 0x40000000; /* remote transmission request */
        public static final int ERR_FLAG = 0x20000000; /* error message frame */

        /* valid bits in CAN ID for frame formats */
        public static final int SFF_MASK = 0x000007FF; /* standard frame format (SFF) */
        public static final int EFF_MASK = 0x1FFFFFFF; /* extended frame format (EFF) */
        public static final int ERR_MASK = 0x1FFFFFFF; /* omit EFF, RTR, ERR flags */

        //--//

        /*
         * Controller Area Network Identifier structure
         *
         * bit 0-28	: CAN identifier (11/29 bit)
         * bit 29	: error message frame flag (0 = data frame, 1 = error message)
         * bit 30	: remote transmission request flag (1 = rtr frame)
         * bit 31	: frame format flag (0 = standard 11 bit, 1 = extended 29 bit)
         */

        /**
         * 32 bit CAN_ID + EFF/RTR/ERR flags<br>
         * C type : canid_t
         */
        public int can_id;

        /**
         * data length code: 0 .. 8<br>
         * C type : __u8
         */
        public byte can_dlc;

        /**
         * Work around for aligning data to 8 bytes
         */
        public byte[] padding = new byte[3];

        /**
         * C type : __u8[8]
         */
        public byte[] data = new byte[8];

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("can_id", "can_dlc", "padding", "data");
        }
    }

    //--//

    public static abstract class BaseFrame
    {
        public byte[] data;

        public abstract int encodeId();

        abstract void convertTo(can_frame frame);

        abstract void convertFrom(can_frame frame);

        protected void receiveData(can_frame frame)
        {
            data = new byte[frame.can_dlc];

            System.arraycopy(frame.data, 0, data, 0, frame.can_dlc);
        }

        protected void sendData(can_frame frame)
        {
            frame.can_dlc = (byte) data.length;

            System.arraycopy(data, 0, frame.data, 0, data.length);
        }
    }

    public static class StandardFrame extends BaseFrame
    {
        public int sourceAddress;

        @Override
        void convertTo(can_frame frame)
        {
            frame.can_id = encodeId() & can_frame.SFF_MASK;

            sendData(frame);
        }

        @Override
        void convertFrom(can_frame frame)
        {
            sourceAddress = frame.can_id & can_frame.SFF_MASK;

            receiveData(frame);
        }

        @Override
        public int encodeId()
        {
            return sourceAddress;
        }
    }

    public static class ExtendedFrame extends BaseFrame
    {
        public int     priority;
        public boolean extendedDataPage;
        public boolean dataPage;
        public int     pduFormat;
        public int     destinationAddress;
        public int     sourceAddress;

        public int pgn;

        //--//

        public void configureForRequest(int pgn,
                                        int destinationAddress,
                                        int sourceAddress)
        {
            priority = 6;
            extendedDataPage = false;
            dataPage = (pgn >= 65536);

            pduFormat = (pgn >> 8) & 0xFF;
            if (pduFormat >= 240)
            {
                this.destinationAddress = pgn & 0xFF;
            }
            else
            {
                this.destinationAddress = destinationAddress;
            }

            this.sourceAddress = sourceAddress;
        }

        @Override
        void convertTo(can_frame frame)
        {
            frame.can_id = can_frame.EFF_FLAG | (encodeId() & can_frame.EFF_MASK);

            sendData(frame);
        }

        @Override
        void convertFrom(can_frame frame)
        {
            int id = frame.can_id & can_frame.EFF_MASK;

            //@formatter:off
            priority           =  (id >> 26) & 0x07;
            extendedDataPage   = ((id >> 25) & 0x01) != 0;
            dataPage           = ((id >> 24) & 0x01) != 0;
            pduFormat          =  (id >> 16) & 0xFF;
            destinationAddress =  (id >>  8) & 0xFF;
            sourceAddress      =  (id >>  0) & 0xFF;
            //@formatter:on

            pgn = dataPage ? 65536 : 0;
            pgn += pduFormat * 256;

            if (pduFormat >= 240)
            {
                pgn += destinationAddress;
            }

            receiveData(frame);
        }

        @Override
        public int encodeId()
        {
            int id = 0;

            //@formatter:off
            id |= (priority           & 0x07) << 26;
            id |= (extendedDataPage  ? 1 : 0) << 25;
            id |= (dataPage          ? 1 : 0) << 24;
            id |= (pduFormat          & 0xFF) << 16;
            id |= (destinationAddress & 0xFF) << 8;
            id |= (sourceAddress      & 0xFF) << 0;
            //@formatter:on

            return id;
        }
    }

    static class Cleaner extends FileDescriptorAccessCleaner
    {
        private final String  m_name;
        private       boolean m_interfaceUp;

        public Cleaner(Object holder,
                       String name)
        {
            super(holder);

            m_name = name;
        }

        @Override
        protected void closeUnderCleaner()
        {
            super.closeUnderCleaner();

            if (m_interfaceUp)
            {
                down();
            }
        }

        void down()
        {
            exec("/sbin/ip", "link", "set", m_name, "down");
            m_interfaceUp = false;
        }

        void up(int frequency)
        {
            exec("/sbin/ip", "link", "set", m_name, "up", "type", "can", "bitrate", frequency);
            m_interfaceUp = true;
        }

        private static void exec(Object... args)
        {
            String[] textArgs = new String[args.length];

            for (int i = 0; i < args.length; i++)
            {
                Object val = args[i];

                textArgs[i] = val.toString();
            }

            String cmdLine = String.join(" ", textArgs);
            LoggerInstance.debug("Executing: %s", cmdLine);

            if (Platform.isLinux())
            {
                try
                {
                    ProcessUtils.exec(null, 5, TimeUnit.SECONDS, textArgs);
                }
                catch (Throwable t)
                {
                    LoggerInstance.error("Failed execution of '%s', due to %s", cmdLine, t);
                }
            }
        }
    }

    //--//

    static
    {
        Native.register(CanAccess.class, NativeLibrary.getInstance("c"));
    }

    private static native int if_nametoindex(String name);

    private static native int socket(int domain,
                                     int type,
                                     int protocol);

    private static native int bind(int s,
                                   sockaddr_can sockAddr,
                                   int sockAddrLen);

    private static native int setsockopt(int fd,
                                         int level,
                                         int optname,
                                         Pointer optval,
                                         int optlen);

    //--//

    private final Cleaner   m_state;
    private final can_frame m_frameRawRead  = new can_frame();
    private final can_frame m_frameRawWrite = new can_frame();

    public CanAccess(String name,
                     int frequency)
    {
        m_state = new Cleaner(this, name);

        if (frequency <= 0)
        {
            frequency = 250000;
        }

        m_state.down();
        m_state.up(frequency);

        int res = socket(AF_CAN, SOCK_RAW, CAN_RAW);
        if (res <= 0)
        {
            int errno = Native.getLastError();
            throw new LastErrorException(errno);
        }

        m_state.setHandle(res, name);

        int ifIndex = if_nametoindex(name);
        if (ifIndex == 0)
        {
            int errno = Native.getLastError();
            throw new LastErrorException(errno);
        }

        sockaddr_can addr = new sockaddr_can(ifIndex);
        res = bind(m_state.getHandle(), addr, addr.size());
        FileDescriptorAccess.checkResult(res);

        //
        // Try turning on CAN FD.
        //
        ifreq_mtu mtu = new ifreq_mtu(name);
        res = m_state.ioctl(SIOCGIFMTU, mtu);
        FileDescriptorAccess.checkResult(res);

        if (mtu.ifru_mtu > 16)
        {
            singleWord32 canFd = new singleWord32();
            canFd.value = 1;
            res = setsockopt(m_state.getHandle(), SOL_CAN_RAW, CAN_RAW_FD_FRAMES, canFd.getPointer(), canFd.size());
            FileDescriptorAccess.checkResult(res);
        }
    }

    public void close()
    {
        m_state.clean();
    }

    public BaseFrame read(int timeout,
                          ChronoUnit unit)
    {
        if (unit != null)
        {
            int res = m_state.poll(timeout, unit);
            if (res <= 0)
            {
                return null;
            }
        }

        int res = m_state.readStruct(m_frameRawRead);
        if (res == m_frameRawRead.size())
        {
            return parseRawFrame(m_frameRawRead);
        }

        if (res < 0)
        {
            int errno = Native.getLastError();
            if (errno != EWOULDBLOCK)
            {
                throw new LastErrorException(errno);
            }
        }

        return null;
    }

    public static can_frame buildRawFrame(int id,
                                          boolean isExtended,
                                          int len,
                                          byte[] payload)
    {
        CanAccess.can_frame frame = new CanAccess.can_frame();

        frame.can_id = id;
        frame.can_dlc = (byte) len;
        frame.data = payload;

        if (isExtended)
        {
            frame.can_id |= CanAccess.can_frame.EFF_FLAG;
        }

        return frame;
    }

    public static BaseFrame parseRawFrame(can_frame frameRaw)
    {
        if ((frameRaw.can_id & can_frame.EFF_FLAG) != 0)
        {
            ExtendedFrame frame = new ExtendedFrame();
            frame.convertFrom(frameRaw);
            return frame;
        }
        else
        {
            StandardFrame frame = new StandardFrame();
            frame.convertFrom(frameRaw);
            return frame;
        }
    }

    public synchronized void write(BaseFrame frame)
    {
        frame.convertTo(m_frameRawWrite);
        m_state.writeStruct(m_frameRawWrite);
    }
}
