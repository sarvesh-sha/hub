/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetSegmentation;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public class IAm extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetObjectIdentifier i_am_device_identifier;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public Unsigned16 max_apdu_length_accepted;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(untagged = true)
    public BACnetSegmentation segmentation_supported;

    @SerializationTag(number = 3)
    @BACnetSerializationTag(untagged = true)
    public Unsigned16 vendor_id;
}
