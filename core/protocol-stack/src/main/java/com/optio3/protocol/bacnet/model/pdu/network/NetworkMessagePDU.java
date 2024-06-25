/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.network;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.MessageType;
import com.optio3.serialization.SerializationHelper;
import com.optio3.stream.InputBuffer;
import com.optio3.stream.OutputBuffer;

public abstract class NetworkMessagePDU
{
    protected NetworkMessagePDU()
    {
    }

    protected void decode(InputBuffer buffer)
    {
        if (buffer != null)
        {
            SerializationHelper.read(buffer, this);
        }
    }

    //--//

    public static NetworkMessagePDU decode(MessageType t,
                                           InputBuffer buffer)
    {
        NetworkMessagePDU res = t.create(buffer);

        return res;
    }

    public void encodeHeader(OutputBuffer buffer)
    {
        SerializationHelper.write(buffer, this);
    }

    //--//

    public abstract void dispatch(ServiceContext sc);
}
