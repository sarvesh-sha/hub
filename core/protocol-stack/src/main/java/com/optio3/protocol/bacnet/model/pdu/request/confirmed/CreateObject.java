/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;
import java.util.Optional;

import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.Choice;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectTypeOrUnknown;
import com.optio3.serialization.SerializationTag;

public final class CreateObject extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<CreateObject>
    {
        @SerializationTag(number = 0)
        public BACnetObjectIdentifier object_identifier;
    }

    //--//

    public static final class ObjectSpecifier extends Choice
    {
        @SerializationTag(number = 0)
        public BACnetObjectTypeOrUnknown object_type;

        @SerializationTag(number = 1)
        public BACnetObjectIdentifier object_identifier;
    }

    @SerializationTag(number = 0)
    public ObjectSpecifier object_specifier;

    @SerializationTag(number = 1)
    public Optional<List<BACnetPropertyValue>> list_of_initial_values;
}
