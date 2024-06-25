/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.serialization.SerializationTag;

public final class VtData extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<VtData>
    {
        @SerializationTag(number = 0)
        public boolean all_new_data_accepted;

        @SerializationTag(number = 1)
        public Optional<Unsigned32> accepted_octet_count; // present only if all_new_data_accepted = FALSE
    }

    @SerializationTag(number = 0)
    public Unsigned8 vt_session_identifier;

    @SerializationTag(number = 1)
    public byte[] vt_new_data;

    @SerializationTag(number = 2)
    public Unsigned32 vt_data_flag; // 0 or 1
}
