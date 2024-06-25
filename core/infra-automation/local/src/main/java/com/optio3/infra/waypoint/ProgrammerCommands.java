/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

import com.optio3.interop.mediaaccess.I2cAccess;
import com.optio3.util.Exceptions;
import com.optio3.util.function.ConsumerWithException;
import com.optio3.util.function.FunctionWithException;

public enum ProgrammerCommands
{
    // @formatter:off
	VersionHardware         (0x01, false, false, true ),
	VersionFirmware         (0x02, false, false, true ),
    //
    LedIntensityHigh        (0x11, false, true , false), // LED, value
    LedIntensityLow         (0x12, false, true , false), // LED, value
    LedHoldHigh             (0x13, false, true , false), // LED, hundredths of second
    LedHoldLow              (0x14, false, true , false), // LED, hundredths of second
    LedRampUp               (0x15, false, true , false), // LED, increase every hundredths of a second
    LedRampDown             (0x16, false, true , false), // LED, decrease every hundredths of a second
    LedCycleCount           (0x17, false, true , false), // LED, number of times to repeat the cycle
    //
    SwitchPositionForProduct(0xD1, false, false, true ),
    SwitchPositionForStation(0xD2, false, false, true ),
    //
    ConnectionStatus        (0xE1, false, false, true ), // Payload => ProgrammerStatus
	StartConnection         (0xE2, true , false, false), // Payload = ignored, just a trigger
	ResetCpu                (0xE3, true , false, false), // Payload = ignored, just a trigger
	EraseChip               (0xE4, true , false, false), // Payload = ignored, just a trigger
	WritePage               (0xE5, true , false, false), // Payload = ignored, just a trigger
	EraseAndWritePage       (0xE6, true , false, false), // Payload = ignored, just a trigger
    //
	AddressCursor           (0xF1, false, true , true ), // Payload = Address
	MemoryRead              (0xF2, true , false, false), // Payload = # bytes to read from memory
	MemoryWrite             (0xF3, true , false, false), // Payload = # bytes to write to memory
	MemoryBuffer            (0xF4, true , false, true ); // Payload = byte to/from memory
	// @formatter:on

    private final byte    m_encoding;
    private final boolean m_byteWrite;
    private final boolean m_wordWrite;
    private final boolean m_wordRead;

    ProgrammerCommands(int encoding,
                       boolean byteWrite,
                       boolean wordWrite,
                       boolean wordRead)
    {
        m_encoding = (byte) encoding;
        m_byteWrite = byteWrite;
        m_wordWrite = wordWrite;
        m_wordRead = wordRead;
    }

    //--//

    public boolean writeByte(int port,
                             int i2cAddress,
                             int value)
    {
        return execSafelyNoReturn(port, (i2cHandler) -> writeByte(i2cHandler, i2cAddress, value));
    }

    public void writeByte(I2cAccess i2cHandler,
                          int i2cAddress,
                          int value)
    {
        if (!m_byteWrite)
        {
            throw Exceptions.newRuntimeException("Byte Write not supported on %s", this);
        }

        i2cHandler.writeCommandByte(i2cAddress, m_encoding, (byte) value);
    }

    //--//

    public boolean writeWord(int port,
                             int i2cAddress,
                             int value)
    {
        return execSafelyNoReturn(port, (i2cHandler) -> writeWord(i2cHandler, i2cAddress, value));
    }

    public void writeWord(I2cAccess i2cHandler,
                          int i2cAddress,
                          int value)
    {
        if (!m_wordWrite)
        {
            throw Exceptions.newRuntimeException("Word Write not supported on %s", this);
        }

        i2cHandler.writeCommandWord(i2cAddress, m_encoding, (short) value);
    }

    //--//

    public boolean writeWord(int port,
                             int i2cAddress,
                             int lo,
                             int hi)
    {
        return execSafelyNoReturn(port, (i2cHandler) -> writeWord(i2cHandler, i2cAddress, lo, hi));
    }

    public void writeWord(I2cAccess i2cHandler,
                          int i2cAddress,
                          int lo,
                          int hi)
    {
        writeWord(i2cHandler, i2cAddress, (((hi & 0xFF) << 8) | (lo & 0xFF)));
    }

    //--//

    public Short readWord(int port,
                          int i2cAddress)
    {
        return execSafely(port, (i2cHandler) -> readWord(i2cHandler, i2cAddress));
    }

    public short readWord(I2cAccess i2cHandler,
                          int i2cAddress)
    {
        if (!m_wordRead)
        {
            throw Exceptions.newRuntimeException("Word Read not supported on %s", this);
        }

        return i2cHandler.readCommandWord(i2cAddress, m_encoding);
    }

    //--//

    private static boolean execSafelyNoReturn(int port,
                                              ConsumerWithException<I2cAccess> callback)
    {
        try (I2cAccess i2cHandler = new I2cAccess(port))
        {
            callback.accept(i2cHandler);
            return true;
        }
        catch (Throwable t)
        {
            return false;
        }
    }

    private static <T> T execSafely(int port,
                                    FunctionWithException<I2cAccess, T> callback)
    {
        try (I2cAccess i2cHandler = new I2cAccess(port))
        {
            return callback.apply(i2cHandler);
        }
        catch (Throwable t)
        {
            return null;
        }
    }
}
