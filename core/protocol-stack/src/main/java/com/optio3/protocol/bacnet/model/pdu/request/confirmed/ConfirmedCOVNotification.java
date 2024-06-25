/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.BACnetObjectModel;
import com.optio3.protocol.model.bacnet.BACnetObjectModelMarshaller;
import com.optio3.protocol.model.bacnet.constructed.BACnetPropertyValue;
import com.optio3.serialization.SerializationTag;

public final class ConfirmedCOVNotification extends ConfirmedServiceRequest implements BACnetObjectModelMarshaller
{
    @SerializationTag(number = 0)
    public Unsigned32 subscriber_process_identifier;

    @SerializationTag(number = 1)
    public BACnetObjectIdentifier initiating_device_identifier;

    @SerializationTag(number = 2)
    public BACnetObjectIdentifier monitored_object_identifier;

    @SerializationTag(number = 3)
    public Unsigned32 time_remaining;

    @SerializationTag(number = 4)
    public List<BACnetPropertyValue> list_of_values = Lists.newArrayList();

    //--//

    @Override
    public <T extends BACnetObjectModel> T allocateObject(Class<T> clz)
    {
        Object val = monitored_object_identifier.allocateNewObject();

        return clz.cast(val);
    }

    @Override
    public void updateObjectNoLog(BACnetObjectModel target)
    {
        target.validate(monitored_object_identifier.object_type);

        for (BACnetPropertyValue prop : list_of_values)
        {
            target.setValue(prop);
        }
    }
}
