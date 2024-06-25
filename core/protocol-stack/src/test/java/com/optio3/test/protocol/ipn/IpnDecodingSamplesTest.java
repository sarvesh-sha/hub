/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.protocol.ipn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.optio3.lang.Unsigned8;
import com.optio3.logging.Severity;
import com.optio3.protocol.ipn.FrameDecoder;
import com.optio3.protocol.model.ipn.enums.IpnAuxiliaryOutput;
import com.optio3.protocol.model.ipn.enums.IpnChargeState;
import com.optio3.protocol.model.ipn.objects.bluesky.BaseBlueSkyObjectModel;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_DuskAndDawnValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterSetpoints;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_ProRemoteTransmit;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_ProRemoteValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_UnitValues;
import com.optio3.protocol.model.obdii.pgn.sys.SysAddressClaimed;
import com.optio3.protocol.model.obdii.pgn.sys.SysRequest;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;
import com.optio3.test.common.AutoRetryOnFailure;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.BufferUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class IpnDecodingSamplesTest extends Optio3Test
{
    class FrameDecoderImpl extends FrameDecoder
    {
        boolean                badChecksum;
        MessageCode            code;
        int                    destinationAddress;
        int                    sourceAddress;
        BaseBlueSkyObjectModel payload;

        @Override
        protected void notifyBadChecksum(byte[] buffer,
                                         int length)
        {
            badChecksum = true;
        }

        @Override
        protected void notifyBadMessage(MessageCode code,
                                        int destinationAddress,
                                        int sourceAddress,
                                        BaseBlueSkyObjectModel val)
        {
            badChecksum = true;
        }

        @Override
        protected void notifyGoodMessage(MessageCode code,
                                         int destinationAddress,
                                         int sourceAddress,
                                         BaseBlueSkyObjectModel val)
        {
            this.code               = code;
            this.destinationAddress = destinationAddress;
            this.sourceAddress      = sourceAddress;
            this.payload            = val;
        }

        void checkRoundTrip(byte[] buf)
        {
            byte[] newBuf = encode(code, destinationAddress, sourceAddress, payload);
            assertEquals(buf.length, newBuf.length);
            comparePayloads(buf, newBuf, newBuf.length);
        }
    }

    public static boolean verboseSweep = false;

    private boolean m_verbose;

    @Before
    public void setVerboseLogging()
    {
        if (failedOnFirstRun())
        {
            m_verbose = true;

            InputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
            OutputBuffer.LoggerInstance.enablePerThread(Severity.Debug);
        }
        else
        {
            m_verbose = false;
        }
    }

    @After
    public void resetVerboseLogging()
    {
        InputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
        OutputBuffer.LoggerInstance.inheritPerThread(Severity.Debug);
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testMasterValues()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 20 20 DF 02 00 00 00 00 00 7C 00 00 00 00 00 52 00 02 E6 11 00 00 00 FF FF 00 00 01 C0 50" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_MasterValues val = assertCast(BlueSky_MasterValues.class, decoded.payload);
        assertEquals(12.4, val.batteryVoltage, 0.01);
        assertEquals(22, val.batteryTemperature, 0.01);
        assertEquals(IpnChargeState.Acceptance, val.chargeState);
        assertEquals(Unsigned8.box(255), val.daysSinceLastFullCharge);
        assertEquals(Unsigned8.box(255), val.daysSinceLastEqualize);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testMasterSetpoints()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 20 23 DC 05 00 9B 00 73 00 7E 00 90 00 84 00 98 00 F4 01 0F 00 14 00 1E 00 14 00 64 00 0A 00 01 BE" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_MasterSetpoints val = assertCast(BlueSky_MasterSetpoints.class, decoded.payload);
        assertEquals(15.5, val.maximumBatteryVoltage, 0.01);
        assertEquals(11.5, val.auxiliaryOutputOffVoltage, 0.01);
        assertEquals(12.6, val.auxiliaryOutputOnVoltage, 0.01);
        assertEquals(14.4, val.acceptanceCharge, 0.01);
        assertEquals(13.2, val.floatCharge, 0.01);
        assertEquals(15.2, val.equalizeCharge, 0.01);
        assertEquals(-5.0, val.temperatureCompensationSlope, 0.01);
        assertEquals(1.5, val.floatCurrent, 0.01);
        assertEquals(2.0, val.acceptanceChargeTime, 0.01);
        assertEquals(30, val.daysBetweenEqualize);
        assertEquals(20, val.hoursForEqualizeCycle);
        assertEquals(100, val.auxiliaryOutputOffAh);
        assertEquals(10, val.auxiliaryOutputOnAh);
        assertEquals(IpnAuxiliaryOutput.AHs, val.auxiliaryOutputUsesAHs);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testUnitValues()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 20 13 EC 0B 00 00 00 00 00 00 00 00 00 00 11 00 E4" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_UnitValues val = assertCast(BlueSky_UnitValues.class, decoded.payload);
        assertEquals(0, val.inputVoltage, 0.01);
        assertEquals(0, val.inputCurrent, 0.01);
        assertEquals(17, val.heatSinkTemperature, 0.01);
        assertFalse(val.charging);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testDuskAndDawn()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 20 0A F5 0C 00 01 02 D7" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_DuskAndDawnValues val = assertCast(BlueSky_DuskAndDawnValues.class, decoded.payload);
        assertEquals(0.1, val.postDusk, 0.01);
        assertEquals(0.2, val.preDawn, 0.01);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testProRemoteValues()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 20 18 E7 0F 10 00 00 00 7A 00 7A 00 36 00 00 DC 00 5E 00 00 0C 57" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_ProRemoteValues val = assertCast(BlueSky_ProRemoteValues.class, decoded.payload);
        assertEquals(0, val.lifeTimeBatteryDischargeAH, 0.01);
        assertEquals(12.2, val.maxBatteryVolt, 0.01);
        assertEquals(12.2, val.minBatteryVolt, 0.01);
        assertEquals(5.4, val.netBatteryCurrent, 0.01);
        assertEquals(220 * 3600, val.batterySetpointAH, 0.01);
        assertEquals(94, val.chargeEfficiencySetpoint, 0.01);
        assertEquals(12, val.selfDischargeSetpoint);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testProRemoteTransmit()
    {
        byte[]           buf     = parseHex(new String[] { "AA FF 10 11 EE 11 10 00 7A 00 7A 00 DC 00 5E 0C 13" });
        FrameDecoderImpl decoded = decoder(buf);
        assertFalse(decoded.badChecksum);

        BlueSky_ProRemoteTransmit val = assertCast(BlueSky_ProRemoteTransmit.class, decoded.payload);
        assertEquals(0, val.enaFlags);
        assertEquals(12.2, val.maxBatteryVolt, 0.01);
        assertEquals(12.2, val.minBatteryVolt, 0.01);
        assertEquals(220 * 3600, val.minBatterySetpointAH, 0.01);
        assertEquals(94.0, val.chargeEfficiencySetpoint, 0.01);
        assertEquals(0.12, val.selfDischargeSetpoint, 0.01);

        decoded.checkRoundTrip(buf);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testPgn_SysAddressClaimed()
    {
        byte[]      buf = parseHex(new String[] { "FF FF FF FF 01 FF FE 80" });
        InputBuffer ib  = InputBuffer.createFrom(buf);
        ib.littleEndian = true;

        SysAddressClaimed val = new SysAddressClaimed();
        SerializationHelper.read(ib, val);

        assertTrue(val.arbitrary_address_capable);
        assertEquals(2097151, val.identity_number);
        assertEquals(2047, val.manufacturer_code);
        assertEquals(255, val.function);
        assertEquals(0, val.function_instance);
        assertEquals(127, val.vehicle_system);
        assertEquals(0, val.vehicle_system_instance);
        assertEquals(0, val.industry_group);

        OutputBuffer ob = new OutputBuffer();
        ob.littleEndian = true;
        SerializationHelper.write(ob, val);
        byte[] newBuf = ob.toByteArray();
        comparePayloads(buf, newBuf, buf.length);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testPgn_SysRequest()
    {
        byte[]      buf = parseHex(new String[] { "EC FE 00" });
        InputBuffer ib  = InputBuffer.createFrom(buf);
        ib.littleEndian = true;

        SysRequest val = new SysRequest();
        SerializationHelper.read(ib, val);

        assertEquals(0xFEEC, val.pgn);

        OutputBuffer ob = new OutputBuffer();
        ob.littleEndian = true;
        SerializationHelper.write(ob, val);
        byte[] newBuf = ob.toByteArray();
        comparePayloads(buf, newBuf, buf.length);
    }

    //--//

    private boolean comparePayloads(byte[] targetBuf,
                                    byte[] sourceBuf,
                                    int length)
    {
        for (int i = 0; i < length; i++)
        {
            if (i >= sourceBuf.length)
            {
                dumpContext("Src", sourceBuf);
                dumpContext("Dst", targetBuf);
                String txt = String.format("Values at offset %d differ: Expected end of stream Got=%d", i, targetBuf[i]);
                failOrPrint(txt);
                return false;
            }

            if (targetBuf[i] != sourceBuf[i])
            {
                dumpContext("Src", sourceBuf);
                dumpContext("Dst", targetBuf);
                String txt = String.format("Values at offset %d differ: Expected=%02x Got=%02x", i, sourceBuf[i], targetBuf[i]);
                failOrPrint(txt);
                return false;
            }
        }
        return true;
    }

    private void failOrPrint(String txt)
    {
        fail(txt);
        //        System.out.println(txt);
    }

    private void dumpContext(String prefix,
                             byte[] buf)
    {
        if (!m_verbose)
        {
            return;
        }

        BufferUtils.convertToHex(buf, 0, buf.length, 32, false, System.out::println);
    }

    //--//

    private FrameDecoderImpl decoder(byte[] buf)
    {
        FrameDecoderImpl decoder = new FrameDecoderImpl();

        for (byte aBuf : buf)
        {
            decoder.push(aBuf);
        }

        return decoder;
    }

    private byte[] parseHex(String[] lines)
    {
        OutputBuffer ob = new OutputBuffer();

        for (String line : lines)
        {
            parseHex(ob, line);
        }

        return ob.toByteArray();
    }

    private void parseHex(OutputBuffer ob,
                          String line)
    {
        String[] parts = line.trim()
                             .split(" ");

        for (String part : parts)
        {
            int digitHi = Character.digit(part.charAt(0), 16);
            int digitLo = Character.digit(part.charAt(1), 16);

            if (digitHi < 0 || digitLo < 0)
            {
                break;
            }

            ob.emit1Byte((digitHi << 4) | digitLo);
        }
    }

    private byte[] parse(String[] lines)
    {
        OutputBuffer ob = new OutputBuffer();

        for (String line : lines)
        {
            String line2 = line.trim();
            if (line2.startsWith("X'"))
            {
                if (m_verbose)
                {
                    System.out.println(String.format("%04d : %s", ob.size(), line));
                }

                for (int pos = 2; pos + 2 <= line2.length(); )
                {
                    int digitHi = Character.digit(line2.charAt(pos++), 16);
                    int digitLo = Character.digit(line2.charAt(pos++), 16);

                    if (digitHi < 0 || digitLo < 0)
                    {
                        break;
                    }

                    ob.emit1Byte((digitHi << 4) | digitLo);
                }
            }
        }

        return ob.toByteArray();
    }

    private void dumpByteArray(byte[] buf)
    {
        if (m_verbose)
        {
            System.out.println();
            System.out.println("Dump of buffer:");
            BufferUtils.convertToHex(buf, 0, buf.length, 32, true, System.out::println);
            System.out.println();
        }
    }
}
