/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.concurrency.Executors;
import com.optio3.lang.Unsigned32;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.SerializationHelper;
import com.optio3.serialization.SerializationTag;
import com.optio3.serialization.TypeDescriptorKind;
import com.optio3.stream.BitBufferEncoding;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.Resources;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.usb4java.ConfigDescriptor;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.EndpointDescriptor;
import org.usb4java.Interface;
import org.usb4java.InterfaceDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;
import org.usb4java.Transfer;

public class LibUsbHelper implements AutoCloseable
{
    public static final Logger LoggerInstance = new Logger(LibUsbHelper.class);

    public class DeviceListHelper implements AutoCloseable
    {
        private       DeviceList         m_list;
        private final List<DeviceHelper> m_devices = Lists.newArrayList();

        DeviceListHelper(DeviceList list)
        {
            m_list = list;

            for (Device device : list)
            {
                m_devices.add(new DeviceHelper(device));
            }
        }

        public void close()
        {
            if (m_list != null)
            {
                for (DeviceHelper device : m_devices)
                {
                    device.close();
                }
                m_devices.clear();

                // Ensure the allocated device list is freed
                LibUsb.freeDeviceList(m_list, true);
                m_list = null;
            }
        }

        public List<DeviceHelper> getDevices()
        {
            return m_devices;
        }

        public List<Dfu> getDfuDevices()
        {
            List<Dfu> dfuList = Lists.newArrayList();

            for (DeviceHelper device : getDevices())
            {
                Dfu dfu = device.asDfu();
                if (dfu != null)
                {
                    dfuList.add(dfu);
                }
            }

            return dfuList;
        }
    }

    public class DeviceHelper implements AutoCloseable
    {
        private Device       m_device;
        private DeviceHandle m_handle;
        private int          m_kernelAttached;
        private byte         m_claimedItfNumber = -1;
        private int          m_epInAddress      = -1;
        private int          m_epOutAddress     = -1;

        private DeviceDescriptor       m_deviceDescriptor;
        private List<ConfigDescriptor> m_configDescriptors;

        private       ByteBuffer m_sharedBuffer = ByteBuffer.allocateDirect(1024);
        private final IntBuffer  m_transferred  = ByteBuffer.allocateDirect(4)
                                                            .asIntBuffer();

        DeviceHelper(Device device)
        {
            m_device = device;
        }

        public void close()
        {
            if (m_handle != null)
            {
                try
                {
                    releaseInterface();
                }
                catch (Throwable t)
                {
                    // Ignore failures on close.
                }

                LibUsb.close(m_handle);
                m_handle = null;
            }

            m_device = null;
        }

        public DeviceDescriptor getDeviceDescriptor()
        {
            if (m_deviceDescriptor == null)
            {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                checkResultPositive(LibUsb.getDeviceDescriptor(m_device, descriptor), "Unable to read device descriptor.");
                m_deviceDescriptor = descriptor;
            }

            return m_deviceDescriptor;
        }

        public List<ConfigDescriptor> getConfigDescriptors()
        {
            if (m_configDescriptors == null)
            {
                List<ConfigDescriptor> configDescriptors = Lists.newArrayList();

                DeviceDescriptor deviceDescriptor = getDeviceDescriptor();

                for (byte cfgNum = 0; cfgNum < deviceDescriptor.bNumConfigurations(); cfgNum++)
                {
                    ConfigDescriptor configDescriptor = new ConfigDescriptor();
                    checkResultPositive(LibUsb.getConfigDescriptor(m_device, cfgNum, configDescriptor), "Unable to read device descriptor");
                    configDescriptors.add(configDescriptor);
                }

                m_configDescriptors = configDescriptors;
            }

            return m_configDescriptors;
        }

        public DeviceHandle open()
        {
            if (m_handle == null)
            {
                DeviceHandle deviceHandle = new DeviceHandle();
                checkResultPositive(LibUsb.open(m_device, deviceHandle), "Unable to open device");
                m_handle = deviceHandle;
            }

            return m_handle;
        }

        public void claimInterface(InterfaceDescriptor itf)
        {
            releaseInterface();

            DeviceHandle handle    = open();
            byte         itfNumber = itf.bInterfaceNumber();

            // Check if kernel driver is attached to the interface
            m_kernelAttached = LibUsb.kernelDriverActive(handle, itfNumber);
            checkResultPositive(m_kernelAttached, "Unable to check kernel driver active");

            // Detach kernel driver from interface 0 and 1. This can fail if
            // kernel is not attached to the device or operating system
            // doesn't support this operation. These cases are ignored here.
            int result = LibUsb.detachKernelDriver(handle, itfNumber);
            switch (result)
            {
                case LibUsb.SUCCESS:
                case LibUsb.ERROR_NOT_SUPPORTED:
                case LibUsb.ERROR_NOT_FOUND:
                    break;

                default:
                    throw new LibUsbException("Unable to detach kernel driver", result);
            }

            // Claim interface
            checkResult(LibUsb.claimInterface(handle, itfNumber), "Unable to claim interface");

            m_claimedItfNumber = itfNumber;

            for (EndpointDescriptor ep : itf.endpoint())
            {
                byte epAddress = ep.bEndpointAddress();
                if ((epAddress & LibUsb.ENDPOINT_IN) != 0)
                {
                    if (m_epInAddress < 0)
                    {
                        m_epInAddress = epAddress & 0xFF;
                    }
                }
                else
                {
                    if (m_epOutAddress < 0)
                    {
                        m_epOutAddress = epAddress & 0xFF;
                    }
                }
            }
        }

        public void releaseInterface()
        {
            if (m_claimedItfNumber >= 0)
            {
                checkResult(LibUsb.releaseInterface(m_handle, m_claimedItfNumber), "Unable to release interface");

                if (m_kernelAttached == 1)
                {
                    m_kernelAttached = 0;

                    checkResult(LibUsb.attachKernelDriver(m_handle, m_claimedItfNumber), "Unable to re-attach kernel driver");
                }

                m_claimedItfNumber = -1;
                m_epInAddress      = -1;
                m_epOutAddress     = -1;
            }
        }

        public void switchToAltSettings(int altNum)
        {
            checkResult(LibUsb.setInterfaceAltSetting(open(), m_claimedItfNumber, altNum), "Unable to select alternate settings");
        }

        public String getString(byte idx)
        {
            return LibUsb.getStringDescriptor(open(), idx);
        }

        public void sendControl(final byte bRequest,
                                final short wValue,
                                final long timeout) throws
                                                    Exception
        {
            ByteBuffer buffer = prepareBufferForReceive(8);
            LibUsb.fillControlSetup(buffer, (byte) (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE), bRequest, wValue, (short) 0, (short) 0);

            Transfer transfer = LibUsb.allocTransfer();
            try
            {
                synchronized (buffer)
                {
                    LibUsb.fillControlTransfer(transfer, open(), buffer, (t) ->
                    {
                        synchronized (buffer)
                        {
                            buffer.notifyAll();
                        }
                    }, null, timeout);

                    checkResult(LibUsb.submitTransfer(transfer), "Failed to send empty control request");

                    buffer.wait(2000);
                }
            }
            finally
            {
                LibUsb.freeTransfer(transfer);
            }
        }

        public void sendControl(final byte bRequest,
                                final short wValue,
                                final byte[] data,
                                final int offset,
                                final int length,
                                final long timeout) throws
                                                    Exception
        {
            ByteBuffer buffer = prepareBufferForSend(data, offset, length);

            sendControl(bRequest, wValue, buffer, timeout);
        }

        public void sendControl(final byte bRequest,
                                final short wValue,
                                final ByteBuffer buffer,
                                final long timeout) throws
                                                    Exception
        {
            transferControl((byte) (LibUsb.ENDPOINT_OUT | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
                            bRequest,
                            wValue,
                            m_claimedItfNumber,
                            buffer,
                            buffer.limit(),
                            buffer.limit(),
                            timeout);
        }

        public byte[] receiveControl(final byte bRequest,
                                     final short wValue,
                                     final int expectedMin,
                                     final int expectedMax,
                                     final long timeout) throws
                                                         Exception
        {
            ByteBuffer buffer = prepareBufferForReceive(expectedMax);

            int transferred = transferControl((byte) (LibUsb.ENDPOINT_IN | LibUsb.REQUEST_TYPE_CLASS | LibUsb.RECIPIENT_INTERFACE),
                                              bRequest,
                                              wValue,
                                              m_claimedItfNumber,
                                              buffer,
                                              expectedMin,
                                              expectedMax,
                                              timeout);

            byte[] data = new byte[transferred];
            buffer.rewind();
            buffer.get(data);

            return data;
        }

        public int transferControl(final byte bmRequestType,
                                   final byte bRequest,
                                   final short wValue,
                                   final short wIndex,
                                   final ByteBuffer data,
                                   final int expectedMin,
                                   final int expectedMax,
                                   final long timeout) throws
                                                       Exception
        {
            int transferred = transferControl(bmRequestType, bRequest, wValue, wIndex, data, timeout);
            if (transferred < expectedMin || transferred > expectedMax)
            {
                throw Exceptions.newRuntimeException("Not all data was exchanged with device: %d (%d / %d)", transferred, expectedMin, expectedMax);
            }

            return transferred;
        }

        public int transferControl(final byte bmRequestType,
                                   final byte bRequest,
                                   final short wValue,
                                   final short wIndex,
                                   final ByteBuffer data,
                                   final long timeout) throws
                                                       Exception
        {
            int transferred = LibUsb.controlTransfer(open(), bmRequestType, bRequest, wValue, wIndex, data, timeout);
            if (transferred < 0)
            {
                if (transferred == LibUsb.ERROR_TIMEOUT)
                {
                    throw new TimeoutException();
                }

                throw new LibUsbException("Control transfer failed", transferred);
            }

            return transferred;
        }

        //--//

        public int writeBulk(final ByteBuffer data,
                             final long timeout)
        {
            if (m_epOutAddress < 0)
            {
                throw new RuntimeException("No OUT endpoint");
            }

            m_transferred.clear();

            checkResult(LibUsb.bulkTransfer(open(), (byte) m_epOutAddress, data, m_transferred, timeout), "Unable to write bulk EP");

            return m_transferred.get();
        }

        //--//

        public Dfu asDfu()
        {
            try
            {
                List<ConfigDescriptor> configDescriptors = getConfigDescriptors();
                for (ConfigDescriptor cfgDescriptor : configDescriptors)
                {
                    for (Interface anInterface : cfgDescriptor.iface())
                    {
                        for (InterfaceDescriptor interfaceDescriptor : anInterface.altsetting())
                        {
                            if (interfaceDescriptor.bInterfaceClass() == (byte) 0xFE && interfaceDescriptor.bInterfaceSubClass() == 1)
                            {
                                return new Dfu(this, anInterface);
                            }
                        }
                    }
                }
            }
            catch (Throwable t)
            {
                // Ignore failures.
                LoggerInstance.debug("asDfu failed due to %s", t);
            }

            return null;
        }

        private ByteBuffer prepareBufferForSend(byte[] data,
                                                int offset,
                                                int length)
        {
            if (m_sharedBuffer.capacity() < length)
            {
                m_sharedBuffer = ByteBuffer.allocateDirect(length);
            }

            m_sharedBuffer.clear();
            m_sharedBuffer.put(data, offset, length);
            m_sharedBuffer.flip();
            return m_sharedBuffer.slice();
        }

        private ByteBuffer prepareBufferForReceive(int length)
        {
            if (m_sharedBuffer.capacity() < length)
            {
                m_sharedBuffer = ByteBuffer.allocateDirect(length);
            }

            m_sharedBuffer.clear();
            m_sharedBuffer.limit(length);
            return m_sharedBuffer.slice();
        }
    }

    //--//

    public static class FirmwareArchive
    {
        private final static int c_headerSize = 4096;

        public static class Part
        {
            public Dfu.FlashLayout descriptor;
            public long            offset;
            public long            length;
        }

        public final List<Part> partitions = Lists.newArrayList();

        //--//

        @JsonIgnore
        public long getTotalSize()
        {
            long size = 0;

            for (Part partition : partitions)
            {
                size += partition.length;
            }

            return size;
        }

        public static FirmwareArchive open(String file) throws
                                                        IOException
        {
            try (FileInputStream stream = new FileInputStream(file))
            {
                InputBuffer ib = InputBuffer.takeOwnership(ExpandableArrayOfBytes.create(stream, 4));
                if (ib.read1ByteSigned() != 'O' || ib.read1ByteSigned() != '3' || ib.read1ByteSigned() != 'v')
                {
                    throw new RuntimeException("Invalid signature");
                }

                int ver = ib.read1ByteUnsigned();
                if (ver == '1')
                {
                    ib = InputBuffer.takeOwnership(ExpandableArrayOfBytes.create(stream, 4));
                    int len = (int) ib.read4BytesUnsigned();

                    return ObjectMappers.deserializeFromGzip(IOUtils.readFully(stream, len), FirmwareArchive.class);
                }

                throw new RuntimeException("Invalid version");
            }
        }

        public static FirmwareArchive convertFrom(List<Dfu.FlashLayout> partitions)
        {
            FirmwareArchive archive = new FirmwareArchive();

            for (Dfu.FlashLayout partition : partitions)
            {
                Part part = new Part();
                part.descriptor = partition;

                if (!partition.binary.exists())
                {
                    throw Exceptions.newRuntimeException("Missing file '%s'", partition.binary);
                }

                part.length = partition.binary.length();
                archive.partitions.add(part);
            }

            return archive;
        }

        public void emit(String output) throws
                                        IOException
        {
            long offset = c_headerSize;
            for (Part partition : partitions)
            {
                partition.offset = offset;
                offset += pad(partition.length);
            }

            byte[] manifest = ObjectMappers.serializeToGzip(this);

            try (OutputBuffer ob = new OutputBuffer())
            {
                ob.emit1Byte('O');
                ob.emit1Byte('3');
                ob.emit1Byte('v');
                ob.emit1Byte('1');
                ob.emit4Bytes(manifest.length);
                ob.emit(manifest);

                byte[] header = ob.toByteArray(pad(ob.getPosition()));

                if (header.length != c_headerSize)
                {
                    throw Exceptions.newIllegalArgumentException("Header too long, %d entries!", partitions.size());
                }

                try (FileOutputStream outputStream = new FileOutputStream(output))
                {
                    outputStream.write(header);

                    long pos = c_headerSize;
                    for (Part partition : partitions)
                    {
                        if (pos != partition.offset)
                        {
                            throw Exceptions.newIllegalArgumentException("Partition %s is not aligned!", partition.descriptor.phaseId);
                        }

                        pos += FileUtils.copyFile(partition.descriptor.binary, outputStream);

                        long posPadded = pad(pos);
                        if (posPadded > pos)
                        {
                            outputStream.write(new byte[(int) (posPadded - pos)]);
                        }

                        pos = posPadded;
                    }
                }
            }
        }

        static long pad(long pos)
        {
            return ((pos + 4095) / 4096) * 4096;
        }

        static int pad(int pos)
        {
            return (int) pad((long) pos);
        }
    }

    //--//

    public enum DfuCommand
    {
        // @formatter:off
        DETACH      (   0),
        DNLOAD      (   1),
        UPLOAD      (   2),
        GETSTATUS   (   3),
        CLRSTATUS   (   4),
        GETSTATE    (   5),
        ABORT       (   6),
        OPTIO3_OTP  (0x41),
        OPTIO3_RESET(0x42),
        OPTIO3_BOOT (0x43);
        // @formatter:on

        private final byte m_encoding;

        DfuCommand(int encoding)
        {
            m_encoding = (byte) encoding;
        }

        public byte encoding()
        {
            return m_encoding;
        }
    }

    public enum DfuState
    {
        // @formatter:off
        appIDLE             ( 0),
        appDETACH           ( 1),
        dfuIDLE             ( 2),
        dfuDNLOAD_SYNC      ( 3),
        dfuDNBUSY           ( 4),
        dfuDNLOAD_IDLE      ( 5),
        dfuMANIFEST_SYNC    ( 6),
        dfuMANIFEST         ( 7),
        dfuMANIFEST_WAIT_RST( 8),
        dfuUPLOAD_IDLE      ( 9),
        dfuERROR            (10);
        // @formatter:on

        private final byte m_encoding;

        DfuState(int encoding)
        {
            m_encoding = (byte) encoding;
        }

        public static DfuState parse(int value)
        {
            for (DfuState t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        public byte encoding()
        {
            return m_encoding;
        }
    }

    public enum DfuStatus
    {
        // @formatter:off
        OK             (0x00),
        errTARGET      (0x01),
        errFILE        (0x02),
        errWRITE       (0x03),
        errERASE       (0x04),
        errCHECK_ERASED(0x05),
        errPROG        (0x06),
        errVERIFY      (0x07),
        errADDRESS     (0x08),
        errNOTDONE     (0x09),
        errFIRMWARE    (0x0a),
        errVENDOR      (0x0b),
        errUSBR        (0x0c),
        errPOR         (0x0d),
        errUNKNOWN     (0x0e),
        errSTALLEDPKT  (0x0f);
        // @formatter:on

        private final byte m_encoding;

        DfuStatus(int encoding)
        {
            m_encoding = (byte) encoding;
        }

        public static DfuStatus parse(int value)
        {
            for (DfuStatus t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        public byte encoding()
        {
            return m_encoding;
        }
    }

    public static class Dfu
    {
        public static class Status
        {
            public DfuStatus bStatus;
            public int       bwPollTimeout;
            public DfuState  bState;
            public int       iString;
        }

        public static class Phase
        {
            public static final int FLASHLAYOUT = 0x00;
            public static final int CMD         = 0xF1;
            public static final int OTP         = 0xF2;
            public static final int PMIC        = 0xF4;
            public static final int END         = 0xFE;
            public static final int RESET       = 0xFF;

            public int        phaseId;
            public Unsigned32 address;
            public Unsigned32 offset;
            public boolean    detachRequested;
        }

        public static class FlashLayout
        {
            public String opt;
            public int    phaseId;
            public String name;
            public String type;
            public String ip;
            public String offset;
            public File   binary;
        }

        public static class OTP
        {
            //
            // struct otp_exchange {
            //     uint32_t version;
            //     uint32_t configuration;
            //     uint32_t reserved;
            //     uint32_t status;
            //     uint32_t general_lock;
            //     uint32_t debug_conf;
            //     uint32_t reserved1[2];
            //     uint32_t otp_disturb[3];
            //     uint32_t reserved2[3];
            //     uint32_t error_status[3];
            //     uint32_t reserved3[3];
            //     uint32_t permanent_lock[3];
            //     uint32_t reserved4[3];
            //     uint32_t programming_lock[3];
            //     uint32_t reserved5[3];
            //     uint32_t shadow_write_lock[3];
            //     uint32_t reserved6[3];
            //     uint32_t shadow_read_lock[3];
            //     uint32_t reserved7[3];
            //     uint32_t otp_value[STM32MP1_OTP_MAX_ID + 1];
            //     uint32_t reserved8[112];
            //     uint32_t bsec_hw_conf;
            //     uint32_t ip_version;
            //     uint32_t ip_id;
            //     uint32_t ip_magic_id;
            // };
            //

            @SerializationTag(number = 0)
            public Unsigned32 version;

            @SerializationTag(number = 1)
            public Unsigned32 configuration;

            @SerializationTag(number = 2)
            public Unsigned32 reserved;

            @SerializationTag(number = 3)
            public Unsigned32 status;

            @SerializationTag(number = 4)
            public Unsigned32 general_lock;

            @SerializationTag(number = 5)
            public Unsigned32 debug_conf;

            @SerializationTag(number = 6, fixedArraySize = 2)
            public Unsigned32[] reserved1;

            @SerializationTag(number = 7, fixedArraySize = 3)
            public Unsigned32[] otp_disturb;

            @SerializationTag(number = 8, fixedArraySize = 3)
            public Unsigned32[] reserved2;

            @SerializationTag(number = 9, fixedArraySize = 3)
            public Unsigned32[] error_status;

            @SerializationTag(number = 10, fixedArraySize = 3)
            public Unsigned32[] reserved3;

            @SerializationTag(number = 11, fixedArraySize = 3)
            public Unsigned32[] permanent_lock;

            @SerializationTag(number = 12, fixedArraySize = 3)
            public Unsigned32[] reserved4;

            @SerializationTag(number = 13, fixedArraySize = 3)
            public Unsigned32[] programming_lock;

            @SerializationTag(number = 14, fixedArraySize = 3)
            public Unsigned32[] reserved5;

            @SerializationTag(number = 15, fixedArraySize = 3)
            public Unsigned32[] shadow_write_lock;

            @SerializationTag(number = 16, fixedArraySize = 3)
            public Unsigned32[] reserved6;

            @SerializationTag(number = 17, fixedArraySize = 3)
            public Unsigned32[] shadow_read_lock;

            @SerializationTag(number = 18, fixedArraySize = 3)
            public Unsigned32[] reserved7;

            @SerializationTag(number = 19, fixedArraySize = 0x5F + 1)
            public Unsigned32[] otp_value;

            @SerializationTag(number = 20, fixedArraySize = 112)
            public Unsigned32[] reserved8;

            @SerializationTag(number = 21)
            public Unsigned32 bsec_hw_conf;

            @SerializationTag(number = 22)
            public Unsigned32 ip_version;

            @SerializationTag(number = 23)
            public Unsigned32 ip_id;

            @SerializationTag(number = 24)
            public Unsigned32 ip_magic_id;
        }

        @FunctionalInterface
        public interface UploadProgress
        {
            void report(int chunks,
                        int offset,
                        boolean last);
        }

        //--//

        public final DeviceHelper device;
        public final String       product;
        public final String       manufacturer;
        public final String       serialNumber;

        private final Map<Integer, Byte> m_phaseToAltSettings = Maps.newHashMap();

        Dfu(DeviceHelper device,
            Interface itf)
        {
            this.device = device;

            boolean claimed = false;

            DeviceDescriptor descriptor = device.getDeviceDescriptor();
            serialNumber = device.getString(descriptor.iSerialNumber());
            product      = device.getString(descriptor.iProduct());
            manufacturer = device.getString(descriptor.iManufacturer());

            for (InterfaceDescriptor interfaceDescriptor : itf.altsetting())
            {
                String   id    = device.getString(interfaceDescriptor.iInterface());
                String[] parts = StringUtils.split(id, '/');
                if (parts.length == 3)
                {
                    int phaseId = parsePhaseId(parts[1]);
                    if (phaseId >= 0)
                    {
                        m_phaseToAltSettings.put(phaseId, interfaceDescriptor.bAlternateSetting());

                        if (!claimed)
                        {
                            claimed = true;

                            device.claimInterface(interfaceDescriptor);
                        }
                    }
                }
            }
        }

        private static int parsePhaseId(String val)
        {
            if (val.startsWith("0x"))
            {
                return Integer.parseInt(val.substring(2), 16);
            }

            return -1;
        }

        public static List<FlashLayout> parseFlashLayout(String root,
                                                         String file) throws
                                                                      IOException
        {
            List<FlashLayout> res = Lists.newArrayList();

            for (String line : Resources.loadLines(Path.of(root, file)
                                                       .toString(), false))
            {
                if (line.startsWith("#"))
                {
                    continue;
                }

                // #Opt Id  Name    Type    IP  Offset  Binary

                String[] parts = StringUtils.split(line, '\t');
                if (parts.length == 7)
                {
                    int phaseId = parsePhaseId(parts[1]);
                    if (phaseId >= 0)
                    {
                        FlashLayout fl = new FlashLayout();
                        fl.opt     = parts[0];
                        fl.phaseId = phaseId;
                        fl.name    = parts[2];
                        fl.type    = parts[3];
                        fl.ip      = parts[4];
                        fl.offset  = parts[5];
                        fl.binary  = Path.of(root, parts[6])
                                         .toFile();

                        if (!fl.binary.exists())
                        {
                            throw Exceptions.newRuntimeException("Invalid flash layout (non-existing file): %s", line);
                        }

                        res.add(fl);
                        continue;
                    }
                }

                throw Exceptions.newRuntimeException("Invalid flash layout: %s", line);
            }

            return res;
        }

        public static byte[] prepareFlashLayout(List<FlashLayout> partitions)
        {
            StringBuilder sb = new StringBuilder();
            for (FlashLayout partition : partitions)
            {
                sb.append(String.format("%s\t0x%02x\t%s\t%s\t%s\t%s\n", partition.opt, partition.phaseId, partition.name, partition.type, partition.ip, partition.offset));
            }

            byte[] payload = sb.toString()
                               .getBytes();

            try (OutputBuffer ob = new OutputBuffer())
            {
                ob.littleEndian = true;

//              struct raw_header_s
//              {
//                  u32 magic_number;
//                  u32 image_signature[64 / 4];
//                  u32 image_checksum;
//                  u32 header_version;
//                  u32 image_length;
//                  u32 image_entry_point;
//                  u32 reserved1;
//                  u32 load_address;
//                  u32 reserved2;
//                  u32 version_number;
//                  u32 option_flags;
//                  u32 ecdsa_algorithm;
//                  u32 ecdsa_public_key[64 / 4];
//                  u32 padding[83 / 4];
//                  u32 binary_type;
//              };

                // u32 magic_number;
                ob.emit1Byte('S');
                ob.emit1Byte('T');
                ob.emit1Byte('M');
                ob.emit1Byte(0x32);

                // u32 image_signature[64 / 4];
                ob.emit(new byte[64]);

                // u32 image_checksum;
                int checksum = 0;
                for (byte b : payload)
                {
                    checksum += b;
                }
                ob.emit4Bytes(checksum);

                // u32 header_version;
                ob.emit4Bytes(0x00010000);

                // u32 image_length;
                ob.emit4Bytes(payload.length);

                // u32 image_entry_point;
                ob.emit4Bytes(0);

                // u32 reserved1;
                ob.emit4Bytes(0);

                // u32 load_address;
                ob.emit4Bytes(0);

                // u32 reserved2;
                ob.emit4Bytes(0);

                // u32 version_number;
                ob.emit4Bytes(0);

                // u32 option_flags;
                ob.emit4Bytes(1);

                // u32 ecdsa_algorithm;
                ob.emit4Bytes(0);

                // u32 ecdsa_public_key[64 / 4];
                ob.emit(new byte[64]);

                // u32 padding[83 / 4];
                ob.emit(new byte[80]);

                // u32 binary_type;
                ob.emit4Bytes(0);

                ob.emit(payload);

                return ob.toByteArray();
            }
        }

        public void ensureIdle()
        {
            Status status = getStatus();
            if (status.bState != DfuState.dfuIDLE)
            {
                sendABORT();
            }
        }

        public void sendABORT()
        {
            try
            {
                selectPhase(Phase.CMD);

                device.receiveControl(DfuCommand.ABORT.encoding(), (short) 0, 0, 1, 2000);
            }
            catch (Exception e)
            {
                // Expected, since we are restarting device.
                LoggerInstance.debug("sendABORT failed due to %s", e);
            }
        }

        public void sendDETACH()
        {
            try
            {
                selectPhase(Phase.CMD);

                device.receiveControl(DfuCommand.DETACH.encoding(), (short) 0, 0, 1, 2000);
            }
            catch (Exception e)
            {
                // Expected, since we are restarting device.
            }
        }

        public void sendRESET()
        {
            ensureIdle();

            try
            {
                selectPhase(Phase.CMD);

                device.receiveControl(DfuCommand.OPTIO3_RESET.encoding(), (short) 0, 0, 0, 2000);
            }
            catch (Exception e)
            {
                // Expected, since we are restarting device.
            }
        }

        public void sendBOOT()
        {
            ensureIdle();

            try
            {
                selectPhase(Phase.CMD);

                device.receiveControl(DfuCommand.OPTIO3_BOOT.encoding(), (short) 0, 0, 0, 2000);
            }
            catch (Exception e)
            {
                // Expected, since we are restarting device.
            }
        }

        public void configureMMC() throws
                                   Exception
        {
            ensureIdle();

            selectPhase(Phase.CMD);

            device.receiveControl(DfuCommand.OPTIO3_OTP.encoding(), (short) 0, 1, 1, 2000);

            OTP  otp                   = getOTP();
            long cfg3                  = otp.otp_value[3].unboxUnsigned();
            long primary_boot_source   = BitBufferEncoding.extract(cfg3, 27, 3);
            long secondary_boot_source = BitBufferEncoding.extract(cfg3, 24, 3);

            if (primary_boot_source == 0)
            {
                primary_boot_source = 4; // SD
            }

            if (secondary_boot_source == 0)
            {
                secondary_boot_source = 3; // eMMC
            }

            long cfg3After = BitBufferEncoding.insert(cfg3, primary_boot_source, 27, 3);
            cfg3After = BitBufferEncoding.insert(cfg3After, secondary_boot_source, 24, 3);

            if (cfg3 != cfg3After)
            {
                otp.otp_value[3] = Unsigned32.box(cfg3After);

                setOTP(otp);
            }
        }

        public Status getStatus()
        {
            try
            {
                byte[] data = device.receiveControl(DfuCommand.GETSTATUS.encoding(), (short) 0, 6, 6, 2000);

                InputBuffer ib = InputBuffer.createFrom(data);
                ib.littleEndian = true;

                Status res = new Status();
                res.bStatus       = DfuStatus.parse(ib.read1ByteUnsigned());
                res.bwPollTimeout = (int) ib.readGenericInteger(3, TypeDescriptorKind.integerUnsigned);
                res.bState        = DfuState.parse(ib.read1ByteUnsigned());
                res.iString       = ib.read1ByteUnsigned();

                return res;
            }
            catch (Exception ex)
            {
                // Ignore failures.
                return null;
            }
        }

        public int getState() throws
                              Exception
        {
            byte[] data = device.receiveControl(DfuCommand.GETSTATE.encoding(), (short) 0, 1, 1, 2000);

            return data[0] & 0xFF;
        }

        public Phase getPhase()
        {
            try
            {
                selectPhase(Phase.CMD);

                byte[] data = device.receiveControl(DfuCommand.UPLOAD.encoding(), (short) 0, 9, 10, 2000);

                InputBuffer ib = InputBuffer.createFrom(data);
                ib.littleEndian = false;

                Phase res = new Phase();
                res.phaseId = ib.read1ByteUnsigned();
                res.address = Unsigned32.box(ib.read4BytesUnsigned());
                res.offset  = Unsigned32.box(ib.read4BytesUnsigned());

                if (!ib.isEOF())
                {
                    res.detachRequested = ib.read1ByteUnsigned() != 0;
                }

                return res;
            }
            catch (Exception ex)
            {
                // Ignore failures.
                return null;
            }
        }

        public OTP getOTP() throws
                            Exception
        {
            selectPhase(Phase.OTP);

            byte[]      data = device.receiveControl(DfuCommand.UPLOAD.encoding(), (short) 0, 1024, 1024, 2000);
            InputBuffer ib   = InputBuffer.createFrom(data);
            ib.littleEndian = true;

            OTP res = new OTP();
            SerializationHelper.read(ib, res);

            return res;
        }

        public void setOTP(OTP otp) throws
                                    Exception
        {
            selectPhase(Phase.OTP);

            try (OutputBuffer ob = new OutputBuffer())
            {
                ob.littleEndian = true;
                SerializationHelper.write(ob, otp);

                byte[] data = ob.toByteArray();

                int transaction = 0;

                device.sendControl(DfuCommand.DNLOAD.encoding(), (short) transaction++, data, 0, data.length, 500);

                waitForIdle();

                device.sendControl(DfuCommand.DNLOAD.encoding(), (short) transaction, 2000);

                waitForIdle();
            }
        }

        public void sendToDevice(int phaseId,
                                 ByteBuffer buffer,
                                 UploadProgress callback) throws
                                                          Exception
        {
            selectPhase(phaseId);

            int       transaction   = 0;
            int       offset        = 0;
            int       length        = buffer.limit();
            boolean   useBulk       = false;
            final int bulkThreshold = 1024 * 1024;

            while (offset < length)
            {
                if (callback != null)
                {
                    callback.report(transaction, offset, false);
                }

                if (useBulk)
                {
                    // Switch to Bulk transfers.
                    int       transferLength  = Math.min(length - offset, bulkThreshold);
                    final int bulkGranularity = 128 * 1024;

                    //
                    // Due to the way the device configures bulk transfers, we have to send whole buffers, or the transfers will not complete, waiting for the rest of the data.
                    //
                    transferLength = (transferLength / bulkGranularity) * bulkGranularity;

                    if (transferLength > 0)
                    {
                        buffer.limit(offset + transferLength);
                        buffer.position(offset);
                        ByteBuffer bufferSub = buffer.slice();

                        device.writeBulk(bufferSub, 5000);

                        offset += bufferSub.remaining();
                        continue;
                    }
                }

                buffer.limit(offset + Math.min(length - offset, 1024));
                buffer.position(offset);
                ByteBuffer bufferSub = buffer.slice();

                device.sendControl(DfuCommand.DNLOAD.encoding(), (short) transaction++, bufferSub, 5000);

                offset += bufferSub.remaining();

                waitForIdle();

                // After the first regular transfer, which initializes the state on the device, we could switch to BULK if payload is big enough.
                useBulk = length > bulkThreshold;
            }

            device.sendControl(DfuCommand.DNLOAD.encoding(), (short) transaction, 5000);

            waitForIdle();

            if (callback != null)
            {
                callback.report(transaction, offset, true);
            }

            Executors.safeSleep(100);
        }

        private void waitForIdle() throws
                                   Exception
        {
            MonotonousTime timeout = TimeUtils.computeTimeoutExpiration(2, TimeUnit.SECONDS);
            while (!TimeUtils.isTimeoutExpired(timeout))
            {
                Status status = getStatus();
                if (status != null)
                {
                    if (status.bStatus != DfuStatus.OK)
                    {
                        throw Exceptions.newRuntimeException("Failed due to %s", status.bStatus);
                    }

                    switch (status.bState)
                    {
                        case dfuDNLOAD_IDLE:
                        case dfuIDLE:
                            return;
                    }

                    if (status.bwPollTimeout > 0)
                    {
                        Executors.safeSleep(Math.min(1000, status.bwPollTimeout));
                    }
                }
            }

            throw new TimeoutException();
        }

        private void selectPhase(int phaseId)
        {
            Byte altSettings = m_phaseToAltSettings.get(phaseId);
            if (altSettings == null)
            {
                throw Exceptions.newIllegalArgumentException("Unknown Phase: %02x", phaseId);
            }

            device.switchToAltSettings(altSettings);
        }
    }

    //--//

    private Context m_usbContext;

    private          Thread  m_eventThread;
    private volatile boolean m_abortEventThread;

    public LibUsbHelper()
    {
        Context ctx = new Context();
        checkResult(LibUsb.init(ctx), "Unable to initialize libusb");
        m_usbContext = ctx;

//        LibUsb.setOption(m_usbContext, LibUsb.OPTION_LOG_LEVEL, LibUsb.LOG_LEVEL_DEBUG);

        m_eventThread = new Thread(() ->
                                   {
                                       while (!m_abortEventThread)
                                       {
                                           // Let libusb handle pending events. This blocks until events
                                           // have been handled, a hotplug callback has been deregistered
                                           // or the specified time of 0.5 seconds (Specified in
                                           // Microseconds) has passed.
                                           checkResult(LibUsb.handleEventsTimeout(m_usbContext, 500000), "Unable to handle events");
                                       }
                                   });

        m_eventThread.start();
    }

    @Override
    public void close() throws
                        Exception
    {
        if (m_eventThread != null)
        {
            m_abortEventThread = true;
            m_eventThread.join();
            m_eventThread = null;
        }

        if (m_usbContext != null)
        {
            LibUsb.exit(m_usbContext);
            m_usbContext = null;
        }
    }

    public DeviceListHelper getDeviceList()
    {
        // Read the USB device list
        DeviceList list = new DeviceList();
        checkResultPositive(LibUsb.getDeviceList(m_usbContext, list), "Unable to get device list");
        return new DeviceListHelper(list);
    }

    //--//

    private static void checkResult(int result,
                                    String reason)
    {
        if (result != LibUsb.SUCCESS)
        {
            throw new LibUsbException(reason, result);
        }
    }

    private static void checkResultPositive(int result,
                                            String reason)
    {
        if (result < 0)
        {
            throw new LibUsbException(reason, result);
        }
    }
}
