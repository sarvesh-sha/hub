/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.enums.BACnetVTClass;
import com.optio3.serialization.SerializationTag;

public final class VtOpen extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<VtOpen>
    {
        @SerializationTag(number = 0)
        public Unsigned8 remote_vt_session_identifier;
    }

    @SerializationTag(number = 0)
    public BACnetVTClass vt_class;

    @SerializationTag(number = 1)
    public Unsigned8 local_vt_session_identifier;
}
