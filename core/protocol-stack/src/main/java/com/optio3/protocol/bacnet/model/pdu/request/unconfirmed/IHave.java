/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public class IHave extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public BACnetObjectIdentifier device_identifier;

    @SerializationTag(number = 1)
    @BACnetSerializationTag(untagged = true)
    public BACnetObjectIdentifier object_identifier;

    @SerializationTag(number = 2)
    @BACnetSerializationTag(untagged = true)
    public String object_name;
}
