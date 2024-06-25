package com.optio3.protocol.montage;

import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;
import com.optio3.logging.Logger;
import com.optio3.protocol.common.CRC;
import com.optio3.protocol.model.ipn.objects.IpnLocation;
import com.optio3.protocol.model.ipn.objects.montage.BaseBluetoothGatewayObjectModel;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_PixelTagRaw;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_SmartLock;
import com.optio3.protocol.model.ipn.objects.montage.BluetoothGateway_TemperatureHumiditySensor;
import com.optio3.protocol.model.ipn.objects.montage.SmartLockState;
import com.optio3.util.BufferUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class BluetoothGatewayDecoder
{
    public static final  Logger LoggerInstance          = new Logger(BluetoothGatewayDecoder.class);
    private static final String CRC_MARKER              = "_CRC:";
    private static final int    EXPECTED_LENGTH         = 31;
    private static final byte   STATUS_PACKET_MARKER    = (byte) 0xEE;
    private static final byte   IGNORE_PACKET_MARKER    = (byte) 0xED;
    private static final byte   SIDE_INFO_PACKET_MARKER = (byte) 0xEC;

    private enum DecoderState
    {
        Idle,
        Success,
        Failure,
    }

    private DecoderState   m_currentState = DecoderState.Idle;
    private MonotonousTime m_deadlineForTransportReset;
    private MonotonousTime m_deadlineForTransportReport;
    private boolean        m_transportIssueReported;

    private final CRC m_crcEngine = new CRC(0xA001);

    private String m_lastBridge;
    private String m_lastBridgeSuffix;
    private String m_lastTag;
    private String m_lastTagSuffix;

    public void process(String line)
    {
        LoggerInstance.debugObnoxious("Raw line: %s", line);

        try
        {
            int crc_pos = line.indexOf(CRC_MARKER);
            if (crc_pos < 0)
            {
                return;
            }

            var data = line.substring(0, crc_pos);
            var buf  = data.getBytes(StandardCharsets.US_ASCII);

            var crc_val      = (short) m_crcEngine.computeLittleEndian(0xFFFF, buf, 0, buf.length);
            var crc_expected = BufferUtils.convertFromHex16(line, crc_pos + CRC_MARKER.length());
            if (crc_expected != crc_val)
            {
                return;
            }

            var                 parts  = StringUtils.split(data, '_');
            Map<String, String> fields = Maps.newHashMap();
            for (int i = 1; i < parts.length; i++)
            {
                var fieldParts = StringUtils.split(parts[i], ':');
                if (fieldParts.length != 2)
                {
                    return;
                }

                var value = fieldParts[1];

                // HACK
                value = StringUtils.replace(value, "TRH#", "TRH_");

                fields.put(fieldParts[0], value);
            }

            transitionToSuccess();

            switch (parts[0])
            {
                case "AT-HB":
                    receivedHeartbeat(fields);
                    break;

                case "AT-TRH":
                {
                    var obj = new BluetoothGateway_TemperatureHumiditySensor();
                    obj.unitId         = fields.get("ID");
                    obj.temperature    = BaseBluetoothGatewayObjectModel.extractHex16AsFloat(fields, "T", true) / 100.0f - 100.0f;
                    obj.humidity       = BaseBluetoothGatewayObjectModel.extractHex16AsFloat(fields, "H", true) / 100.0f;
                    obj.batteryVoltage = BaseBluetoothGatewayObjectModel.extractHex16AsFloat(fields, "B", true) / 1000.0f;
                    receivedMessage(obj);
                    break;
                }

                case "AT-PXL":
                {
                    var raw     = fields.get("DATA");
                    var decoded = BufferUtils.convertFromHex(raw);
                    if (decoded.length == EXPECTED_LENGTH && decoded[1] == 0x16 && decoded[2] == (byte) 0xC6 && decoded[3] == (byte) 0xFC)
                    {
                        var suffix = raw.substring(2 * (decoded.length - 4));

                        switch (decoded[6])
                        {
                            case STATUS_PACKET_MARKER:
                            {
                                var rssi     = BufferUtils.convertFromHex(fields.get("RSSI"), 0);
                                var location = getLastLocation();

                                var rawPayload = new BluetoothGateway_PixelTagRaw.RawPayload();
                                rawPayload.tag      = raw;
                                rawPayload.rssi     = rssi;
                                rawPayload.location = location;

                                var obj = new BluetoothGateway_PixelTagRaw();
                                obj.setRaw(rawPayload);
                                receivedMessage(obj);
                                break;
                            }

                            case IGNORE_PACKET_MARKER:
                                break;

                            case SIDE_INFO_PACKET_MARKER:
                                // Handle bridge side-info packet
                                m_lastBridge = raw;
                                m_lastBridgeSuffix = suffix;
                                break;

                            default:
                                m_lastTag = raw;
                                m_lastTagSuffix = suffix;
                                break;
                        }

                        if (m_lastTagSuffix != null && StringUtils.equals(m_lastTagSuffix, m_lastBridgeSuffix))
                        {
                            var rssi     = BufferUtils.convertFromHex(fields.get("RSSI"), 0);
                            var location = getLastLocation();

                            var rawPayload = new BluetoothGateway_PixelTagRaw.RawPayload();
                            rawPayload.lastBridge = m_lastBridge;
                            rawPayload.tag        = m_lastTag;
                            rawPayload.rssi       = rssi;
                            rawPayload.location   = location;

                            var obj = new BluetoothGateway_PixelTagRaw();
                            obj.setRaw(rawPayload);
                            receivedMessage(obj);

                            m_lastTagSuffix = null;
                        }
                    }
                    break;
                }

                case "AT-SLB":
                {
                    var raw = BufferUtils.convertFromHex(fields.get("D"));

                    BitSet bs = new BitSet();

                    for (int i = 0; i < 24; i++)
                    {
                        var bit = raw[4 + (i / 8)] & (1 << (i % 8));
                        if (bit != 0)
                        {
                            bs.set(i);
                        }
                    }

                    var obj = new BluetoothGateway_SmartLock();
                    obj.unitId         = fields.get("ADD");
                    obj.lockingCounter = raw[7] & 0xFF;
                    obj.status         = new SmartLockState(bs);

                    receivedMessage(obj);
                    break;
                }
            }
        }
        catch (Exception ignored)
        {
        }
    }

    protected abstract void receivedHeartbeat(Map<String, String> fields);

    protected abstract void receivedMessage(BaseBluetoothGatewayObjectModel obj);

    protected abstract IpnLocation getLastLocation();

    //--//

    public void transitionToSuccess()
    {
        m_deadlineForTransportReset  = null;
        m_deadlineForTransportReport = null;

        switch (m_currentState)
        {
            case Failure:
                if (m_transportIssueReported)
                {
                    LoggerInstance.info("Bluetooth Gateway data flow resumed!");

                    m_transportIssueReported = false;
                }
                // Fallthrough

            case Idle:
                m_currentState = DecoderState.Success;
                break;
        }
    }

    public void transitionToFailure()
    {
        switch (m_currentState)
        {
            case Idle:
            case Success:
                if (m_deadlineForTransportReset == null)
                {
                    setDeadlineForTransportReset();
                }

                if (TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
                {
                    m_currentState = DecoderState.Failure;
                }
                break;

            case Failure:
                if (m_deadlineForTransportReport == null)
                {
                    m_deadlineForTransportReport = TimeUtils.computeTimeoutExpiration(20, TimeUnit.MINUTES);
                }

                if (!m_transportIssueReported && TimeUtils.isTimeoutExpired(m_deadlineForTransportReport))
                {
                    LoggerInstance.error("Bluetooth Gateway not receiving any data...");

                    m_transportIssueReported = true;
                }
                break;
        }
    }

    public boolean shouldCloseTransport()
    {
        if (m_deadlineForTransportReset != null && TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
        {
            setDeadlineForTransportReset();

            return true;
        }

        return false;
    }

    private void setDeadlineForTransportReset()
    {
        m_deadlineForTransportReset = TimeUtils.computeTimeoutExpiration(10, TimeUnit.SECONDS);
    }
}
