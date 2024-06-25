/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import java.util.List;
import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.constructed.BACnetGroupChannelValue;
import com.optio3.serialization.SerializationTag;

public class WriteGroup extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Unsigned32 group_number;

    @SerializationTag(number = 1)
    public Unsigned8 write_priority;

    @SerializationTag(number = 2)
    public List<BACnetGroupChannelValue> change_list;

    @SerializationTag(number = 3)
    public Optional<Boolean> inhibit_delay;
}
