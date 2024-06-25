/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.WriteAccessSpecification;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.serialization.SerializationTag;

public final class WritePropertyMultiple extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public List<WriteAccessSpecification> list_of_write_access_specifications = Lists.newArrayList();

    //--//

    public void add(BACnetObjectIdentifier objId,
                    BACnetPropertyIdentifier prop,
                    Object value)
    {
        list_of_write_access_specifications.add(WriteAccessSpecification.newRequest(objId, prop, value));
    }
}
