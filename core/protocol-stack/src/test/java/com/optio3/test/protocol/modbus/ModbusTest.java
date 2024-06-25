/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.modbus;

import static org.junit.Assert.assertEquals;

import com.optio3.protocol.common.CRC;
import com.optio3.test.common.Optio3Test;
import org.junit.Test;

public class ModbusTest extends Optio3Test
{
    @Test
    public void crcTest()
    {
        CRC crc = new CRC(0xA001);

        byte[] buf = { 0x01, 0x04, 0x02, (byte) 0xFF, (byte) 0xFF };

        int res = crc.computeLittleEndian(0xFFFF, buf, 0, buf.length);
        assertEquals(0x80B8, res);
    }
}
