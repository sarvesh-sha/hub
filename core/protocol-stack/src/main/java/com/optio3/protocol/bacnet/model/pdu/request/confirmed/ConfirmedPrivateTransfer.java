/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.serialization.SerializationTag;

public class ConfirmedPrivateTransfer extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<ConfirmedPrivateTransfer>
    {
        @SerializationTag(number = 0)
        public Unsigned16 vendor_id;

        @SerializationTag(number = 1)
        public Unsigned32 service_number;

        @SerializationTag(number = 2)
        public Optional<Object> result_block;
    }

    //--//

    @SerializationTag(number = 0)
    public Unsigned16 vendor_id;

    @SerializationTag(number = 1)
    public Unsigned32 service_number;

    @SerializationTag(number = 2)
    public Optional<Object> service_parameters;
}
