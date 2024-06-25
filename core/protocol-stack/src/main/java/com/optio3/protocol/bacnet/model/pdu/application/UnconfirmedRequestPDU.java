/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.UnconfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

//
// | 7 | 6 | 5 | 4 | 3 | 2 | 1 | 0 |
// |---|---|---|---|---|---|---|---|
// | PDU Type      | 0 | 0 | 0 | 0 |
// |---|---|---|---|---|---|---|---|
// | Service Choice                |
// |---|---|---|---|---|---|---|---|
// | Service Request               |
// |---|---|---|---|---|---|---|---|
//
public final class UnconfirmedRequestPDU extends ApplicationPDU
{
    @SerializationTag(number = 1, width = 8)
    public UnconfirmedServiceChoice serviceChoice;

    //--//

    @Override
    protected ServiceCommon allocatePayload()
    {
        return Reflection.newInstance(serviceChoice.request());
    }

    //--//

    public UnconfirmedRequestPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        UnconfirmedServiceRequest res = (UnconfirmedServiceRequest) decodePayload();

        sc.processRequest(res);
    }
}
