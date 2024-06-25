/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;

public enum BACnetVirtualLinkLayerResult
{
    // @formatter:off
    Success                               (0x0000),
    Write_Broadcast_Distribution_Table_NAK(0x0010), 
    Read_Broadcast_Distribution_Table_NAK (0x0020), 
    Register_Foreign_Device_NAK           (0x0030), 
    Read_Foreign_Device_Table_NAK         (0x0040), 
    Delete_Foreign_Device_Table_Entry_NAK (0x0050), 
    Distribute_Broadcast_To_Network_NAK   (0x0060);
    // @formatter:on

    private final short m_encoding;

    BACnetVirtualLinkLayerResult(int encoding)
    {
        m_encoding = (short) encoding;
    }

    @HandlerForDecoding
    public static BACnetVirtualLinkLayerResult parse(short value)
    {
        for (BACnetVirtualLinkLayerResult t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public short encoding()
    {
        return m_encoding;
    }
}
