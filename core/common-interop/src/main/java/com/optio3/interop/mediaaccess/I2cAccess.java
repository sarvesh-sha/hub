/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */

package com.optio3.interop.mediaaccess;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.interop.FileDescriptorAccess;
import com.optio3.interop.FileDescriptorAccessCleaner;
import com.optio3.util.Exceptions;
import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.Union;

public final class I2cAccess implements AutoCloseable
{
    public static class i2c_smbus_ioctl_data extends Structure
    {
        public byte           read_write;
        public byte           command;
        public int            size;
        public i2c_smbus_data data;

        public i2c_smbus_ioctl_data()
        {
            data = new i2c_smbus_data();
        }

        @Override
        protected List<String> getFieldOrder()
        {
            return Lists.newArrayList("read_write", "command", "size", "data");
        }
    }

    public static class i2c_smbus_data extends Union implements Structure.ByReference
    {
        public byte   data8;
        public short  data16;
        public byte[] block = new byte[32 + 2]; // block[0] is used for length and one more for user-space compatibility

        public i2c_smbus_data()
        {
        }
    }

    //--//

    private static final int I2C_RETRIES     = 0x0701; // number of times a device address should be polled when not acknowledging
    private static final int I2C_TIMEOUT     = 0x0702; // set timeout in units of 10 ms
    private static final int I2C_SLAVE       = 0x0703; // Use this slave address
    private static final int I2C_SLAVE_FORCE = 0x0706; // Use this slave address, even if it is already in use by a driver!
    private static final int I2C_SMBUS       = 0x0720; // SMBus transfer

    // i2c_smbus_xfer read or write markers
    private static final int I2C_SMBUS_READ  = 1;
    private static final int I2C_SMBUS_WRITE = 0;

    // SMBus transaction types (size parameter in the i2c_smbus_ioctl_data structure)
    private static final int I2C_SMBUS_QUICK            = 0;
    private static final int I2C_SMBUS_BYTE             = 1;
    private static final int I2C_SMBUS_BYTE_DATA        = 2;
    private static final int I2C_SMBUS_WORD_DATA        = 3;
    private static final int I2C_SMBUS_PROC_CALL        = 4;
    private static final int I2C_SMBUS_BLOCK_DATA       = 5;
    private static final int I2C_SMBUS_I2C_BLOCK_BROKEN = 6;
    private static final int I2C_SMBUS_BLOCK_PROC_CALL  = 7; // SMBus 2.0
    private static final int I2C_SMBUS_I2C_BLOCK_DATA   = 8;

    private static final int O_RDWR = 0x0002; // open for reading and writing

    private final i2c_smbus_ioctl_data        m_sharedCtl    = new i2c_smbus_ioctl_data();
    private final FileDescriptorAccessCleaner m_state        = new FileDescriptorAccessCleaner(this);
    private       int                         m_slaveAddress = -1;

    public I2cAccess(int bus)
    {
        this(String.format("/dev/i2c-%d", bus));
    }

    public I2cAccess(String path)
    {
        int res = FileDescriptorAccess.open(path, O_RDWR);
        if (res <= 0)
        {
            throw Exceptions.newRuntimeException("Failed to open I2C port '%s': %d", path, Native.getLastError());
        }

        m_state.setHandle(res, path);
    }

    public void close()
    {
        m_state.clean();
    }

    public synchronized byte readByte(int address)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_READ;
        m_sharedCtl.size = I2C_SMBUS_BYTE;

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);

        return m_sharedCtl.data.data8;
    }

    public synchronized void writeByte(int address,
                                       byte value)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_WRITE;
        m_sharedCtl.command = value;
        m_sharedCtl.size = I2C_SMBUS_BYTE;

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);
    }

    public synchronized byte readCommandByte(int address,
                                             byte command)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_READ;
        m_sharedCtl.command = command;
        m_sharedCtl.size = I2C_SMBUS_BYTE_DATA;

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);

        return m_sharedCtl.data.data8;
    }

    public synchronized void writeCommandByte(int address,
                                              byte command,
                                              byte value)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_WRITE;
        m_sharedCtl.command = command;
        m_sharedCtl.size = I2C_SMBUS_BYTE_DATA;

        m_sharedCtl.data.data8 = value;
        m_sharedCtl.data.setType("data8");

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);
    }

    public synchronized short readCommandWord(int address,
                                              byte command)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_READ;
        m_sharedCtl.command = command;
        m_sharedCtl.size = I2C_SMBUS_WORD_DATA;

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);

        return m_sharedCtl.data.data16;
    }

    public synchronized void writeCommandWord(int address,
                                              byte command,
                                              short value)
    {
        setSlave(address);

        m_sharedCtl.read_write = I2C_SMBUS_WRITE;
        m_sharedCtl.command = command;
        m_sharedCtl.size = I2C_SMBUS_WORD_DATA;

        m_sharedCtl.data.data16 = value;
        m_sharedCtl.data.setType("data16");

        int res = m_state.ioctl(I2C_SMBUS, m_sharedCtl);
        FileDescriptorAccess.checkResult(res);
    }

    //--//

    private void setSlave(int address)
    {
        if (m_slaveAddress != address)
        {
            int res = m_state.ioctl(I2C_SLAVE, address);
            FileDescriptorAccess.checkResult(res);

            m_slaveAddress = address;
        }
    }
}
