/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.linklayer;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.model.bacnet.error.BACnetDecodingException;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

//
// Type (0x81)  1 octet
// Function     1 octet
// Length       2 octets
// Data         N - 4 octets
//
public abstract class BaseVirtualLinkLayer
{
    public static final byte c_BACnet_ID = (byte) 0x81; // BVLL for BACnet/IP

    public enum Function
    {
        // @formatter:off
        BVLC_Result                          (0x00, Result.class),
        Write_Broadcast_Distribution_Table   (0x01, null),
        Read_Broadcast_Distribution_Table    (0x02, ReadBroadcastDistributionTable.class),
        Read_Broadcast_Distribution_Table_Ack(0x03, ReadBroadcastDistributionTable.Ack.class),
        Forwarded_NPDU                       (0x04, Forwarded.class),
        Register_Foreign_Device              (0x05, RegisterForeignDevice.class),
        Read_Foreign_Device_Table            (0x06, ReadForeignDeviceTable.class),
        Read_Foreign_Device_Table_Ack        (0x07, ReadForeignDeviceTable.Ack.class),
        Delete_Foreign_Device_Table_Entry    (0x08, DeleteForeignDeviceTableEntry.class),
        Distribute_Broadcast_To_Network      (0x09, DistributeBroadcastToNetwork.class),
        Original_Unicast_NPDU                (0x0A, OriginalUnicast.class),
        Original_Broadcast_NPDU              (0x0B, OriginalBroadcast.class),
        Secure_BVLL                          (0x0C, null);
        // @formatter:on

        private final byte                                  m_encoding;
        private final Class<? extends BaseVirtualLinkLayer> m_clz;

        Function(int encoding,
                 Class<? extends BaseVirtualLinkLayer> clz)
        {
            m_encoding = (byte) encoding;
            m_clz      = clz;
        }

        public static Function parse(int value)
        {
            for (Function t : values())
            {
                if (t.m_encoding == value)
                {
                    return t;
                }
            }

            return null;
        }

        public static Function parse(Class<? extends BaseVirtualLinkLayer> value)
        {
            for (Function t : values())
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

        public Class<? extends BaseVirtualLinkLayer> factory()
        {
            return m_clz;
        }

        public BaseVirtualLinkLayer create()
        {
            return Reflection.newInstance(m_clz);
        }
    }

    //--//

    public static BaseVirtualLinkLayer decode(InputBuffer ib)
    {
        byte b = (byte) ib.read1ByteUnsigned();
        if (b != c_BACnet_ID)
        {
            throw BACnetDecodingException.newException("Encountered unknown VirtualLinkType signature: 0x%02x", b & 0xFF);
        }

        b = (byte) ib.read1ByteUnsigned();
        Function t = Function.parse(b);
        if (t == null)
        {
            throw BACnetDecodingException.newException("Encountered unknown VirtualLinkType type: %d", b);
        }

        BaseVirtualLinkLayer bvll = t.create();

        int length = ib.read2BytesUnsigned();

        InputBuffer payload = ib.readNestedBlock(length - 4);

        SerializationHelper.read(payload, bvll);

        bvll.decodePayload(payload);

        return bvll;
    }

    public OutputBuffer encode(OutputBuffer payload)
    {
        final int headerSize = 4;

        OutputBuffer ob = new OutputBuffer();
        ob.emit1Byte(c_BACnet_ID);
        ob.emit1Byte(Function.parse(getClass())
                             .encoding());

        try (OutputBuffer obPayload = new OutputBuffer())
        {
            SerializationHelper.write(obPayload, this);

            if (payload != null)
            {
                obPayload.emitNestedBlock(payload);
            }

            ob.emit2Bytes(obPayload.size() + headerSize);
            ob.emitNestedBlock(obPayload);
        }

        return ob;
    }

    protected void decodePayload(InputBuffer payload)
    {
    }

    public abstract void dispatch(ServiceContext sc);
}
