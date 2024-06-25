/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.serialization.SerializationTag;

public final class DeleteObject extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public BACnetObjectIdentifier object_identifier;
}
