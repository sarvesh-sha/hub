/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.ipn;

import com.optio3.collection.ExpandableArrayOfBytes;
import com.optio3.protocol.model.ipn.objects.bluesky.BaseBlueSkyObjectModel;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_DisplayUnitNeeds;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_DisplayUnitRequest;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_DuskAndDawnRequest;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_DuskAndDawnValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterRequest;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterSetpoints;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterSetpointsRequest;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_MasterValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_ProRemoteTransmit;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_ProRemoteValues;
import com.optio3.protocol.model.ipn.objects.bluesky.BlueSky_UnitValues;
import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public abstract class FrameDecoder
{
    public enum MessageCode
    {
        //@formatter:off
        MasterRequest          (0x01, BlueSky_MasterRequest.class         ), // Master request’s a power unit’s information
        MasterValues           (0x02, BlueSky_MasterValues.class          ), // Active values from master
        DisplayUnitRequest     (0x03, BlueSky_DisplayUnitRequest.class    ), // Master requests the display unit’s needs
        DisplayUnitResponse    (0x04, BlueSky_DisplayUnitNeeds.class      ), // Display unit’s broadcast of its needs and amp-hour information (“needs response”)
        MasterSetpoints        (0x05, BlueSky_MasterSetpoints.class       ), // Master’s broadcast of its setpoints
        MasterSetpointsRequest (0x06, BlueSky_MasterSetpointsRequest.class), // Master requests setpoints from the display unit
        MasterSetpointsResponse(0x07, BlueSky_MasterSetpoints.class       ), // Display unit transmits its setpoints
        UnitValues             (0x0B, BlueSky_UnitValues.class            ), // Status response from a slave, or broadcast from master.
        DuskAndDawnValues      (0x0C, BlueSky_DuskAndDawnValues.class     ), // Broadcast of dusk and dawn hours from master
        DuskAndDawnRequest     (0x0D, BlueSky_DuskAndDawnRequest.class    ), // Master requests dusk/dawn hours from the display unit
        DuskAndDawnResponse    (0x0E, BlueSky_DuskAndDawnValues.class     ), // Display unit’s broadcast of its dusk-to-dawn hours (“needs response”)
        ProRemoteValues        (0x0F, BlueSky_ProRemoteValues.class       ), // ProRemote Data
        ProRemoteDisable       (0x10, null                            ), // Disable ProRemote Transmit
        ProRemoteTransmit      (0x11, BlueSky_ProRemoteTransmit.class     ); // Enable ProRemote Transmit
        //@formatter:on

        private final byte                                    m_encoding;
        private final Class<? extends BaseBlueSkyObjectModel> m_clz;

        MessageCode(int encoding,
                    Class<? extends BaseBlueSkyObjectModel> clz)
        {
            m_encoding = (byte) encoding;
            m_clz      = clz;
        }

        @HandlerForDecoding
        public static MessageCode parse(byte value)
        {
            for (MessageCode t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        public static MessageCode parse(Class<? extends BaseBlueSkyObjectModel> value)
        {
            for (MessageCode t : values())
            {
                if (t.m_clz == value)
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

        public Class<? extends BaseBlueSkyObjectModel> factory()
        {
            return m_clz;
        }

        public BaseBlueSkyObjectModel create()
        {
            return m_clz != null ? Reflection.newInstance(m_clz) : null;
        }
    }

    private final byte[] m_frame    = new byte[512];
    private       int    m_framePos = 0;

    private byte        m_destinationAddress;
    private byte        m_sourceAddress;
    private byte        m_frameLength;
    private MessageCode m_messageCode;
    private byte        m_checksum;

    public void reset()
    {
        m_framePos = 0;
        m_checksum = 0;
    }

    public boolean push(byte c)
    {
        m_frame[m_framePos] = c;

        //
        // All communication packets have the same format as shown below:
        //
        //  Byte 0: 0xAA first sync byte
        //  Byte 1: 0xFF second sync byte
        //  Byte 2: address destination address
        //  Byte 3: n total bytes in packet from byte 0 to and including the checksum byte
        //  Byte 4: n not one’s complement of total bytes
        //  Byte 5: message code communication message code
        //  Byte 6: address source address
        //  Bytes 7-(n-1) data Message dependent data
        //  Last byte: checksum lower 8 bytes of sum of all bytes in packet excluding this byte
        //
        switch (m_framePos++)
        {
            case 0:
                if (c != (byte) 0xAA)
                {
                    reset();
                    return false;
                }
                break;

            case 1:
                if (c != (byte) 0xFF)
                {
                    reset();
                    return false;
                }
                break;

            case 2:
                m_destinationAddress = c;
                break;

            case 3:
                m_frameLength = c;
                if (m_frameLength < 7)
                {
                    reset();
                    return false;
                }
                break;

            case 4:
                if ((m_frameLength & c) != 0)
                {
                    reset();
                    return false;
                }
                break;

            case 5:
                m_messageCode = MessageCode.parse(c);
                break;

            case 6:
                m_sourceAddress = c;
                break;

            default:
                if (m_framePos == m_frameLength)
                {
                    if (m_checksum != c)
                    {
                        notifyBadChecksum(m_frame, m_framePos);

                        reset();
                        return false;
                    }
                    else
                    {
                        notifyGoodFrame(m_frame, 7, m_framePos - 8);

                        reset();
                        return true;
                    }
                }
                break;
        }

        m_checksum += c;
        return false;
    }

    protected abstract void notifyBadChecksum(byte[] buffer,
                                              int length);

    private void notifyGoodFrame(byte[] buffer,
                                 int offset,
                                 int length)
    {
        if (m_messageCode != null)
        {
            BaseBlueSkyObjectModel val = m_messageCode.create();
            if (val != null)
            {
                try (InputBuffer ib = InputBuffer.createFrom(buffer, offset, length))
                {
                    ib.littleEndian = true;

                    SerializationHelper.read(ib, val);
                }
            }

            BlueSky_UnitValues val_unit = Reflection.as(val, BlueSky_UnitValues.class);
            if (val_unit != null)
            {
                val_unit.unitId = m_sourceAddress;
            }

            if (val != null && !val.postDecodingValidation())
            {
                notifyBadMessage(m_messageCode, m_destinationAddress, m_sourceAddress, val);
            }
            else
            {
                notifyGoodMessage(m_messageCode, m_destinationAddress, m_sourceAddress, val);
            }
        }
    }

    protected abstract void notifyBadMessage(MessageCode code,
                                             int destinationAddress,
                                             int sourceAddress,
                                             BaseBlueSkyObjectModel val);

    protected abstract void notifyGoodMessage(MessageCode code,
                                              int destinationAddress,
                                              int sourceAddress,
                                              BaseBlueSkyObjectModel val);

    //--//

    public static byte[] encode(MessageCode messageCode,
                                int destinationAddress,
                                int sourceAddress,
                                BaseBlueSkyObjectModel val)
    {
        try (OutputBuffer obPayload = new OutputBuffer())
        {
            obPayload.littleEndian = true;
            SerializationHelper.write(obPayload, val);

            //
            // All communication packets have the same format as shown below:
            //
            //  Byte 0: 0xAA first sync byte
            //  Byte 1: 0xFF second sync byte
            //  Byte 2: address destination address
            //  Byte 3: n total bytes in packet from byte 0 to and including the checksum byte
            //  Byte 4: n not one’s complement of total bytes
            //  Byte 5: message code communication message code
            //  Byte 6: address source address
            //  Bytes 7-(n-1) data Message dependent data
            //  Last byte: checksum lower 8 bytes of sum of all bytes in packet excluding this byte
            //
            int len = obPayload.size() + 8;

            try (OutputBuffer ob = new OutputBuffer())
            {
                ob.emit1Byte(0xAA);
                ob.emit1Byte(0xFF);
                ob.emit1Byte(destinationAddress);
                ob.emit1Byte(len);
                ob.emit1Byte(~len);
                ob.emit1Byte(messageCode.encoding());
                ob.emit1Byte(sourceAddress);
                ob.emitNestedBlock(obPayload);
                ob.emit1Byte(0); // Placeholder for checksum

                byte[] result   = ob.toByteArray();
                int    checksum = 0;
                int    lastPos  = result.length - 1;

                for (int i = 0; i < lastPos; i++)
                {
                    checksum += result[i];
                }
                result[lastPos] = (byte) checksum;

                return result;
            }
        }
    }
}