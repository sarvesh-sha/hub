/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.provision.imaging;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.interop.FileDescriptorAccess;
import com.optio3.interop.FileDescriptorAccessCleaner;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.TypedBitSet;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.BufferUtils;
import com.optio3.util.Exceptions;
import com.sun.jna.Native;

public class BrotherQlPrinterDriver extends AbstractPrinterDriver
{
    public static final Logger LoggerInstance = new Logger(BrotherQlPrinterDriver.class);

    public enum Errors
    {
        noMedia,
        endOfMedia,
        tapeCutterJam,
        mainUnitInUse,
        fanNotWorking,
        transmissionError,
        coverOpen,
        cannotFeed,
        systemError,
    }

    public enum MediaType implements TypedBitSet.ValueGetter
    {
        NoMedia(0x00),
        ContinuousLengthTape(0x0A),
        DieCutLabels(0x0C);

        private final int m_encoding;

        MediaType(int val)
        {
            m_encoding = val;
        }

        public static MediaType parse(byte value)
        {
            for (MediaType t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @Override
        public int getEncodingValue()
        {
            return m_encoding;
        }
    }

    public enum StatusType implements TypedBitSet.ValueGetter
    {
        ReplyToStatusRequest(0x00),
        PrintingCompleted(0x01),
        ErrorOccurred(0x02),
        Notification(0x05),
        PhaseChange(0x06);

        private final int m_encoding;

        StatusType(int val)
        {
            m_encoding = val;
        }

        public static StatusType parse(byte value)
        {
            for (StatusType t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @Override
        public int getEncodingValue()
        {
            return m_encoding;
        }
    }

    public enum PhaseType implements TypedBitSet.ValueGetter
    {
        WaitingToReceive(0x00),
        PrintingState(0x01);

        private final int m_encoding;

        PhaseType(int val)
        {
            m_encoding = val;
        }

        public static PhaseType parse(byte value)
        {
            for (PhaseType t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @Override
        public int getEncodingValue()
        {
            return m_encoding;
        }
    }

    public enum Notification implements TypedBitSet.ValueGetter
    {
        NotAvailable(0x00),
        CoolingStart(0x01),
        CoolingFinish(0x02);

        private final int m_encoding;

        Notification(int val)
        {
            m_encoding = val;
        }

        public static Notification parse(byte value)
        {
            for (Notification t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        @Override
        public int getEncodingValue()
        {
            return m_encoding;
        }
    }

    public static class Status
    {
        public List<Errors> errors = Lists.newArrayList();

        public int          mediaWidth;
        public MediaType    mediaType;
        public int          mediaLength;
        public StatusType   statusType;
        public PhaseType    phaseType;
        public Notification notificationNumber;

        public void pushErrorIfSet(Errors dst,
                                   byte src,
                                   int bit)
        {
            if ((src & (1 << bit)) != 0)
            {
                errors.add(dst);
            }
        }
    }

    private static final int O_RDWR     = 0x0002; // open for reading and writing
    private static final int O_NONBLOCK = 0x0004; // no delay

    private final boolean m_twoColors;
    private final boolean m_highDpi;
    private final int     m_scaleFactor;

    private final FileDescriptorAccessCleaner m_state        = new FileDescriptorAccessCleaner(this);
    private       byte[]                      m_statusBuffer = new byte[32];
    private       int                         m_statusOffset = 0;
    private       Status                      m_lastStatus;

    public BrotherQlPrinterDriver(String file,
                                  int horizontalResolution,
                                  int height,
                                  boolean twoColors,
                                  boolean highDpi)
    {
        super(horizontalResolution, ((twoColors || highDpi) ? 2 : 1) * height);

        int res = FileDescriptorAccess.open(file, O_RDWR | O_NONBLOCK);
        if (res <= 0)
        {
            throw Exceptions.newRuntimeException("Failed to open printer port '%s': %d", file, Native.getLastError());
        }

        m_state.setHandle(res, file);

        m_twoColors   = twoColors;
        m_highDpi     = highDpi;
        m_scaleFactor = (twoColors || highDpi) ? 2 : 1;
    }

    public void close()
    {
        m_state.clean();
    }

    public int getHeight()
    {
        return super.getHeight() / m_scaleFactor;
    }

    public Graphics2D getGraphics()
    {
        Graphics2D graphics2D = super.getGraphics();

        graphics2D.setTransform(AffineTransform.getScaleInstance(1, m_scaleFactor));

        return graphics2D;
    }

    public void print()
    {
        final Raster raster       = getRaster();
        final int    width        = raster.getWidth();
        final int    heightScaled = raster.getHeight();
        final int    height       = heightScaled / m_scaleFactor;

        invalidate();
        initialize();
        requestStatus();

        setPrintInformation(MediaType.ContinuousLengthTape, 62, 0, heightScaled);

        setExtraInfo(35);

        byte[] line   = new byte[width / 8];
        int[]  colors = new int[3];

        for (int y = heightScaled - 1; y >= 0; y--)
        {
            if (m_twoColors && y < height)
            {
                send(0x77, 0, width / 8);
            }
            else
            {
                send(0x67, 0, width / 8);
            }

            for (int x = 0; x < width; x++)
            {
                int offset = x / 8;
                int shift  = x % 8;

                raster.getPixel(x, y, colors);

                int     r       = colors[0];
                int     g       = colors[1];
                int     b       = colors[2];
                boolean isBlack = r < 128 && g < 128 && b < 128;
                boolean isRed   = r > 128 && g < 128 && b < 128;

                byte    val = line[offset];
                boolean set = false;

                if (isBlack)
                {
                    set = true;
                }
                else if (isRed)
                {
                    if (m_twoColors)
                    {
                        set = (x % 2) == 0;
                    }
                    else
                    {
                        set = true;
                    }
                }

                if (set)
                {
                    val |= 0x80 >> shift;
                }
                else
                {
                    val &= ~(0x80 >> shift);
                }

                line[offset] = val;
            }

            send(line);
        }

        // Print page
        send(0x1A);
    }

    private void invalidate()
    {
        send(new byte[200]);
    }

    private void requestStatus()
    {
        send(0x1B, 0x69, 0x53);
    }

    private void initialize()
    {
        // Initialize
        //   ESC + @
        //   1B H + 40 H
        send(0x1B, 0x40);

        // Command mode switch
        //   ESC + i + a + {n}
        //   1B H + 69 H + 61 H + {n}
        send(0x1B, 0x69, 0x61, 0x01); // Switch to raster mode.
    }

    private void setPrintInformation(MediaType paperType,
                                     int paperWidth,
                                     int paperLength,
                                     int rasterLines)
    {
        // Print information command
        //   ESC + i + z + {n1} + {n2} + {n3} + {n4} + {n5} + {n6} + {n7} + {n8} + {n9}+ {n10}
        //   1B H + 69 H + 7AH + {n1} + {n2} + {n3} + {n4} + {n5} + {n6} + {n7} + {n8} + {n9}+ {n10}
        //      {n1}: Valid flag; specifies which values are valid
        //         #define PI_KIND 0x02 // Paper type
        //         #define PI_WIDTH 0x04 // Paper width
        //         #define PI_LENGTH 0x08 // Paper length
        //         #define PI_QUALITY 0x40 // Give priority to print quality
        //         #define PI_RECOVER 0x80 // Always ON
        //      {n2}: Paper type
        //         Continuous length tape 0A Hex
        //         Die-cut labels 0B Hex
        //      {n3}: Paper width; units: mm
        //      {n4}: Paper length; units: mm
        //      {n5-n8}: Raster number = n8*256*256*256 + n7*256*256 + n6*256 + n5
        //   If the media is not correctly loaded into the printer when the valid flag for PI_KIND, PI_WIDTH and PI_LENGTH are set to “ON”, an error status is returned (Bit 0 of “error information 2” is set to “ON”.)
        //      {n9}: Starting page: 0; Other pages: 1
        //      {n10}: Fixed to “0”

        int flags = 0x80 | 0x40 | 0x08 | 0x04 | 0x02;
        send(0x1B, 0x69, 0x7A, flags, paperType, paperWidth, paperLength, Unsigned32.box(rasterLines), 0, 0);
    }

    private void setExtraInfo(int amountInDots)
    {
        // Set each mode
        //    ESC + i + M + {n}
        //    1B H + 69 H + 4D H + {n}
        //     Bit 6 Auto cut 1: Auto cut 0:No auto cut
        send(0x1B, 0x69, 0x4D, 0x40);

        // Specify the page number in ”cut every * labels” (QL-560/570/580N/700/1050/1060N)
        //    ESC + i + A + {n1}
        //    1B H + 69 H + 41 H + {n1}
        send(0x1B, 0x69, 0x41, 0x01);

        // Set expanded mode
        // ESC i K
        //        flags = 0x00
        //        flags |= self.cut_at_end << 3
        //        flags |= self.dpi_600 << 6
        //        flags |= self.two_color_printing << 0
        byte flags = 0x08;
        if (m_twoColors)
        {
            flags = 0x01;
        }
        else if (m_highDpi)
        {
            flags |= 0x40;
        }
        send(0x1B, 0x69, 0x4B, flags);

        // Set margin amount (feed amount)
        // ESC i d
        send(0x1B, 0x69, 0x64, Unsigned16.box(amountInDots));
    }

    private Status receiveStatus(int timeout)
    {
        byte[] buf = new byte[1];

        while (true)
        {
            if (receive(buf, timeout) != 1)
            {
                return null;
            }

            byte c = buf[0];

            switch (m_statusOffset)
            {
                case 0:
                    if (c != (byte) 0x80)
                    {
                        continue;
                    }
                    break;

                case 1:
                    if (c != (byte) 0x20)
                    {
                        m_statusOffset = 0;
                        continue;
                    }
                    break;
            }

            m_statusBuffer[m_statusOffset++] = c;
            if (m_statusOffset == 32)
            {
                m_statusOffset = 0;

                Status status = new Status();

                //    Bit 0   No media when printing
                //    Bit 1   End of media (die-cut size only)
                //    Bit 2   Tape cutter jam
                //    Bit 4   Main unit in use (QL-560/650TD/1050)
                //    Bit 7 Fan doesn’t work
                byte error1 = m_statusBuffer[8];
                status.pushErrorIfSet(Errors.noMedia, error1, 0);
                status.pushErrorIfSet(Errors.endOfMedia, error1, 1);
                status.pushErrorIfSet(Errors.tapeCutterJam, error1, 2);
                status.pushErrorIfSet(Errors.mainUnitInUse, error1, 4);
                status.pushErrorIfSet(Errors.fanNotWorking, error1, 7);

                //  Bit 2   Transmission error
                //  Bit 4   Cover opened while printing (Except QL-500)
                //  Bit 6   Cannot feed (used even when the media is empty)
                //  Bit 7   System error
                byte error2 = m_statusBuffer[9];
                status.pushErrorIfSet(Errors.transmissionError, error2, 2);
                status.pushErrorIfSet(Errors.coverOpen, error2, 4);
                status.pushErrorIfSet(Errors.cannotFeed, error2, 6);
                status.pushErrorIfSet(Errors.systemError, error2, 7);

                status.mediaWidth         = m_statusBuffer[10];
                status.mediaType          = MediaType.parse(m_statusBuffer[11]);
                status.mediaLength        = m_statusBuffer[17];
                status.statusType         = StatusType.parse(m_statusBuffer[18]);
                status.phaseType          = PhaseType.parse(m_statusBuffer[19]);
                status.notificationNumber = Notification.parse(m_statusBuffer[20]);

                if (LoggerInstance.isEnabled(Severity.Debug))
                {
                    LoggerInstance.debug("Received Status: %s", ObjectMappers.prettyPrintAsJson(status));
                }

                m_lastStatus = status;
                return status;
            }
        }
    }

    private void send(Object... values)
    {
        try (OutputBuffer output = new OutputBuffer())
        {
            output.littleEndian = true;

            for (Object v : values)
            {
                if (v instanceof Number)
                {
                    output.emit1Byte(((Number) v).byteValue());
                }
                else if (v instanceof Unsigned32)
                {
                    output.emit4Bytes(((Unsigned32) v).unbox());
                }
                else if (v instanceof Unsigned16)
                {
                    output.emit2Bytes(((Unsigned16) v).unbox());
                }
                else if (v instanceof TypedBitSet.ValueGetter)
                {
                    output.emit1Byte(((TypedBitSet.ValueGetter) v).getEncodingValue());
                }
            }

            send(output.toByteArray());
            receiveStatus(100);
        }
    }

    private void send(byte[] buffer)
    {
        if (LoggerInstance.isEnabled(Severity.DebugVerbose))
        {
            BufferUtils.convertToHex(buffer, 0, buffer.length, 16, true, (line) -> LoggerInstance.debugVerbose("Send: %s", line));
        }

        m_state.writeBuffer(buffer, buffer.length);
    }

    public int receive(byte[] buffer,
                       int timeout)
    {
        int res = m_state.poll(timeout, ChronoUnit.MILLIS);
        if (res <= 0)
        {
            return res;
        }

        return m_state.readBuffer(buffer, buffer.length);
    }
}
