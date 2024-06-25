/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.interop.mediaaccess;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.concurrency.Executors;
import com.optio3.interop.FileDescriptorAccess;
import com.optio3.interop.FileDescriptorAccessCleaner;
import com.optio3.util.Exceptions;
import com.optio3.util.ProcessUtils;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public final class SerialAccess implements AutoCloseable
{
    // typedef unsigned long	tcflag_t;
    // typedef unsigned char	cc_t;
    // typedef unsigned long	speed_t;
    //
    // struct termios
    // {
    //    tcflag_t	c_iflag;	/* input flags */
    //    tcflag_t	c_oflag;	/* output flags */
    //    tcflag_t	c_cflag;	/* control flags */
    //    tcflag_t	c_lflag;	/* local flags */
    //    cc_t		c_cc[NCCS];	/* control chars */
    //    speed_t	c_ispeed;	/* input speed */
    //    speed_t	c_ospeed;	/* output speed */
    // };

    public static class termios extends Structure
    {

        /* c_iflag bits */
        public static final int IGNBRK  = 0000001;
        public static final int BRKINT  = 0000002;
        public static final int IGNPAR  = 0000004;
        public static final int PARMRK  = 0000010;
        public static final int INPCK   = 0000020;
        public static final int ISTRIP  = 0000040;
        public static final int INLCR   = 0000100;
        public static final int IGNCR   = 0000200;
        public static final int ICRNL   = 0000400;
        public static final int IUCLC   = 0001000;
        public static final int IXON    = 0002000;
        public static final int IXANY   = 0004000;
        public static final int IXOFF   = 0010000;
        public static final int IMAXBEL = 0020000;
        public static final int IUTF8   = 0040000;

        //--//

        /* c_oflag bits */
        public static final int OPOST  = 0000001;
        public static final int OLCUC  = 0000002;
        public static final int ONLCR  = 0000004;
        public static final int OCRNL  = 0000010;
        public static final int ONOCR  = 0000020;
        public static final int ONLRET = 0000040;
        public static final int OFILL  = 0000100;
        public static final int OFDEL  = 0000200;

        public static final int NLDLY = 0000400;
        public static final int NL0   = 0000000;
        public static final int NL1   = 0000400;

        public static final int CRDLY = 0003000;
        public static final int CR0   = 0000000;
        public static final int CR1   = 0001000;
        public static final int CR2   = 0002000;
        public static final int CR3   = 0003000;

        public static final int TABDLY = 0014000;
        public static final int TAB0   = 0000000;
        public static final int TAB1   = 0004000;
        public static final int TAB2   = 0010000;
        public static final int TAB3   = 0014000;
        public static final int XTABS  = 0014000;

        public static final int BSDLY = 0020000;
        public static final int BS0   = 0000000;
        public static final int BS1   = 0020000;

        public static final int VTDLY = 0040000;
        public static final int VT0   = 0000000;
        public static final int VT1   = 0040000;

        public static final int FFDLY = 0100000;
        public static final int FF0   = 0000000;
        public static final int FF1   = 0100000;

        //--//

        /* c_cflag bit meaning */
        private static final int CBAUD   = 0010017;
        private static final int CIBAUD  = 002003600000;  /* input baud rate */
        private static final int IBSHIFT = 16;            /* Shift from CBAUD to CIBAUD */
        private static final int B0      = 0000000;       /* hang up */
        private static final int B50     = 0000001;
        private static final int B75     = 0000002;
        private static final int B110    = 0000003;
        private static final int B134    = 0000004;
        private static final int B150    = 0000005;
        private static final int B200    = 0000006;
        private static final int B300    = 0000007;
        private static final int B600    = 0000010;
        private static final int B1200   = 0000011;
        private static final int B1800   = 0000012;
        private static final int B2400   = 0000013;
        private static final int B4800   = 0000014;
        private static final int B9600   = 0000015;
        private static final int B19200  = 0000016;
        private static final int B38400  = 0000017;
        private static final int CBAUDEX = 0010000;
        private static final int BOTHER  = 0010000;
        private static final int B57600  = 0010001;
        private static final int B115200 = 0010002;
        private static final int B230400 = 0010003;
        private static final int B460800 = 0010004;
        private static final int B500000 = 0010005;
        private static final int B576000 = 0010006;

        private static final int CSIZE = 0000060;
        private static final int CS5   = 0000000;
        private static final int CS6   = 0000020;
        private static final int CS7   = 0000040;
        private static final int CS8   = 0000060;

        private static final int CSTOPB = 0000100;
        private static final int CREAD  = 0000200;
        private static final int PARENB = 0000400;
        private static final int PARODD = 0001000;
        private static final int HUPCL  = 0002000;
        private static final int CLOCAL = 0004000;

        private static final int CMSPAR  = 010000000000;  /* mark or space (stick) parity */
        private static final int CRTSCTS = 020000000000;  /* flow control */

        //--//

        /* c_lflag bits */
        private static final int ISIG    = 0000001;
        private static final int ICANON  = 0000002;
        private static final int XCASE   = 0000004;
        private static final int ECHO    = 0000010;
        private static final int ECHOE   = 0000020;
        private static final int ECHOK   = 0000040;
        private static final int ECHONL  = 0000100;
        private static final int NOFLSH  = 0000200;
        private static final int TOSTOP  = 0000400;
        private static final int ECHOCTL = 0001000;
        private static final int ECHOPRT = 0002000;
        private static final int ECHOKE  = 0004000;
        private static final int FLUSHO  = 0010000;
        private static final int PENDIN  = 0040000;
        private static final int IEXTEN  = 0100000;
        private static final int EXTPROC = 0200000;

        //--//

        public static final int NCCS = 1 + 17 + 2;

        public int    c_iflag;    // input flags
        public int    c_oflag;    // output flags
        public int    c_cflag;    // control flags
        public int    c_lflag;    // local flags
        public byte[] c_cc      = new byte[NCCS];
        public int    c_ispeed;   // input speed
        public int    c_ospeed;   // output speed
        public byte[] c_padding = new byte[256];

        public termios()
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("c_iflag", "c_oflag", "c_cflag", "c_lflag", "c_cc", "c_ispeed", "c_ospeed", "c_padding");
        }

        //--//

        public static class ByReference extends termios implements Structure.ByReference
        {
        }
    }

    public static class serial_struct extends Structure
    {
        public static final int ASYNCB_SPD_HI  = 4; /* Use 57600 instead of 38400 bps */
        public static final int ASYNCB_SPD_VHI = 5; /* Use 115200 instead of 38400 bps */
        public static final int ASYNCB_SPD_SHI = 12; /* Use 230400 instead of 38400 bps */

        public static final int ASYNC_SPD_HI  = (1 << ASYNCB_SPD_HI);
        public static final int ASYNC_SPD_VHI = (1 << ASYNCB_SPD_VHI);
        public static final int ASYNC_SPD_SHI = (1 << ASYNCB_SPD_SHI);

        public static final int ASYNC_SPD_CUST = (ASYNC_SPD_HI | ASYNC_SPD_VHI);
        public static final int ASYNC_SPD_MASK = (ASYNC_SPD_HI | ASYNC_SPD_VHI | ASYNC_SPD_SHI);

        //--//

        public int     type;
        public int     line;
        public int     port; // unsigned
        public int     irq;
        public int     flags;
        public int     xmit_fifo_size;
        public int     custom_divisor;
        public int     baud_base;
        public short   close_delay;
        public byte    io_type;
        public byte    reserved_char;
        public int     hub6;
        public short   closing_wait; /* time to wait before closing */
        public short   closing_wait2; /* no longer used... */
        public Pointer iomem_base;
        public short   iomem_reg_shift;
        public int     port_high;
        public long    iomap_base;    /* cookie passed into ioremap */

        public serial_struct()
        {
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("type",
                                      "line",
                                      "port",
                                      "irq",
                                      "flags",
                                      "xmit_fifo_size",
                                      "custom_divisor",
                                      "baud_base",
                                      "close_delay",
                                      "io_type",
                                      "reserved_char",
                                      "hub6",
                                      "closing_wait",
                                      "closing_wait2",
                                      "iomem_base",
                                      "iomem_reg_shift",
                                      "port_high",
                                      "iomap_base");
        }

        //--//

        public static class ByReference extends serial_struct implements Structure.ByReference
        {
        }
    }

    //--//

    private static final int TIOCGSERIAL = 0x541E;
    private static final int TIOCSSERIAL = 0x541F;

    private static final int TCIFLUSH  = 1;
    private static final int TCOFLUSH  = 2;
    private static final int TCIOFLUSH = 3;
    private static final int TCOOFF    = 1;
    private static final int TCOON     = 2;
    private static final int TCIOFF    = 3;
    private static final int TCION     = 4;

    //
    // Commands passed to tcsetattr() for setting the termios structure.
    //
    private static final int TCSANOW   = 0;    //  make change immediate
    private static final int TCSADRAIN = 1;    //  drain output, then change
    private static final int TCSAFLUSH = 2;    //  drain output, flush input
    private static final int TCSASOFT  = 0x10; //  flag - don't alter h.w. state

    private static final int O_RDWR     = 0x0002; // open for reading and writing
    private static final int O_NONBLOCK = 0x0004; // no delay
    private static final int O_NOCTTY   = 0x20000; // don't assign controlling terminal

    //--//

    static
    {
        Native.register(SerialAccess.class, NativeLibrary.getInstance("c"));
    }

    private static native int tcflush(int fd,
                                      int flags);

    private static native int tcgetattr(int fd,
                                        termios cfg);

    private static native int tcsetattr(int fd,
                                        int when,
                                        termios cfg);

    //--//

    private final FileDescriptorAccessCleaner m_state = new FileDescriptorAccessCleaner(this);

    //--//

    public SerialAccess(String path,
                        boolean exclusiveLock)
    {
        int res = FileDescriptorAccess.open(path, O_RDWR | O_NONBLOCK | O_NOCTTY);
        if (res <= 0)
        {
            throw Exceptions.newRuntimeException("Failed to open serial port '%s': %d", path, Native.getLastError());
        }

        m_state.setHandle(res, path);

        if (exclusiveLock)
        {
            // /* Operations for the `flock' call.  */
            // #define	LOCK_SH	1	/* Shared lock.  */
            // #define	LOCK_EX	2 	/* Exclusive lock.  */
            // #define	LOCK_UN	8	/* Unlock.  */

            FileDescriptorAccess.checkResult(FileDescriptorAccess.flock(res, 2));
        }
    }

    public SerialAccess(String path,
                        int baudrate,
                        int bits,
                        char parity,
                        int stopBits)
    {
        this(path, false);

        if (baudrate <= 0)
        {
            baudrate = 38400;
        }

        termios cfg = new termios();
        int     res = tcgetattr(m_state.getHandle(), cfg);
        if (res < 0)
        {
            throw Exceptions.newRuntimeException("tcgetattr failed: %d", Native.getLastError());
        }

        cfg.c_cflag &= ~termios.CRTSCTS; // Disable hardware flow control
        cfg.c_cflag |= termios.CREAD; // Enable receiver
        cfg.c_cflag |= termios.CLOCAL; // Local connection, no modem control

        boolean customBaudrate = false;

        if (!Platform.isMac())
        {
            cfg.c_cflag &= ~termios.CBAUD;

            switch (baudrate)
            {
                case 9600:
                    cfg.c_cflag |= termios.B9600;
                    break;

                case 19200:
                    cfg.c_cflag |= termios.B19200;
                    break;

                case 38400:
                    cfg.c_cflag |= termios.B38400;
                    break;

                default:
                    cfg.c_cflag |= termios.B38400;
                    customBaudrate = true;
                    break;
            }
        }

        cfg.c_cflag &= ~termios.CSIZE;
        switch (bits)
        {
            case 5:
                cfg.c_cflag |= termios.CS5;
                break;

            case 6:
                cfg.c_cflag |= termios.CS6;
                break;

            case 7:
                cfg.c_cflag |= termios.CS7;
                break;

            case 8:
            default:
                cfg.c_cflag |= termios.CS8;
                break;
        }

        switch (parity)
        {
            case 'N':
            case 'n':
                cfg.c_cflag &= ~(termios.PARENB | termios.PARODD);
                break;

            case 'E':
            case 'e':
                cfg.c_cflag |= termios.PARENB;
                break;

            case 'O':
            case 'o':
                cfg.c_cflag |= termios.PARENB | termios.PARODD;
                break;
        }

        if (stopBits == 2)
        {
            cfg.c_cflag |= termios.CSTOPB;
        }
        else
        {
            cfg.c_cflag &= ~termios.CSTOPB;
        }

        //
        // IGNPAR  : ignore bytes with parity errors
        //
        cfg.c_iflag = termios.IGNPAR;

        //
        // Raw output.
        //
        cfg.c_oflag = 0;

        //
        // Raw input.
        //
        cfg.c_lflag = 0;

        res = tcsetattr(m_state.getHandle(), TCSANOW, cfg);
        if (res < 0)
        {
            throw Exceptions.newRuntimeException("tcsetattr failed: %d", Native.getLastError());
        }

        if (Platform.isMac())
        {
            try
            {
                ProcessUtils.exec(null, 10, TimeUnit.SECONDS, "/bin/stty", "-f", path, Integer.toString(baudrate));
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        else if (customBaudrate)
        {
            serial_struct ser = new serial_struct();

            res = m_state.ioctl(TIOCGSERIAL, ser);
            if (res < 0)
            {
                throw Exceptions.newRuntimeException("TIOCGSERIAL failed: %d", Native.getLastError());
            }

            //--//

            ser.custom_divisor = (ser.baud_base + (baudrate / 2)) / baudrate;
            ser.flags &= ~serial_struct.ASYNC_SPD_MASK;
            ser.flags |= serial_struct.ASYNC_SPD_CUST;

            res = m_state.ioctl(TIOCSSERIAL, ser);
            if (res < 0)
            {
                throw Exceptions.newRuntimeException("TIOCSSERIAL failed: %d", Native.getLastError());
            }
        }
    }

    //--//

    public static SerialAccess openMultipleTimes(int passes,
                                                 String path,
                                                 int baudrate,
                                                 int bits,
                                                 char parity,
                                                 int stopBits)
    {
        for (int pass = 0; ; pass++)
        {
            SerialAccess sa = new SerialAccess(path, baudrate, bits, parity, stopBits);
            if (pass >= passes)
            {
                return sa;
            }

            Executors.safeSleep(5);
            sa.close();
        }
    }

    //--//

    public void close()
    {
        m_state.clean();
    }

    public int read(byte[] buffer,
                    int timeout)
    {
        int res = m_state.poll(timeout, ChronoUnit.MILLIS);
        if (res <= 0)
        {
            return res;
        }

        return m_state.readBuffer(buffer, buffer.length);
    }

    public void write(byte[] buffer,
                      int len)
    {
        m_state.writeBuffer(buffer, len);
    }

    public void write(String text)
    {
        ByteBuffer encoded = StandardCharsets.UTF_8.encode(text);
        int        len     = encoded.limit();
        byte[]     buf     = new byte[len];
        encoded.get(buf);

        write(buf, len);
    }
}
