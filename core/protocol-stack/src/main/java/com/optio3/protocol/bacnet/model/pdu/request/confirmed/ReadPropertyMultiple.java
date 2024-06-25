/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessResult;
import com.optio3.protocol.model.bacnet.constructed.ReadAccessSpecification;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;
import com.optio3.protocol.model.bacnet.serialization.BACnetSerializationTag;
import com.optio3.serialization.SerializationTag;

public final class ReadPropertyMultiple extends ConfirmedServiceRequest
{
    public static final class Ack extends ConfirmedServiceResponse<ReadPropertyMultiple>
    {
        @SerializationTag(number = 0)
        @BACnetSerializationTag(untagged = true)
        public List<ReadAccessResult> list_of_read_access_results = Lists.newArrayList();

        //--//

        public ReadAccessResult add(BACnetObjectIdentifier objId)
        {
            ReadAccessResult res = new ReadAccessResult();
            res.object_identifier = objId;

            list_of_read_access_results.add(res);
            return res;
        }

        public ReadAccessResult.Values add(BACnetObjectIdentifier objId,
                                           BACnetPropertyIdentifierOrUnknown prop)
        {
            ReadAccessResult res = add(objId);
            return res.add(prop);
        }
    }

    //--//

    @SerializationTag(number = 0)
    @BACnetSerializationTag(untagged = true)
    public List<ReadAccessSpecification> list_of_read_access_specifications = Lists.newArrayList();

    //--//

    public void add(BACnetObjectIdentifier objId,
                    BACnetPropertyIdentifierOrUnknown prop)
    {
        list_of_read_access_specifications.add(ReadAccessSpecification.newRequest(objId, prop));
    }

    public void add(BACnetObjectIdentifier objId,
                    BACnetPropertyIdentifierOrUnknown... props)
    {
        list_of_read_access_specifications.add(ReadAccessSpecification.newRequest(objId, props));
    }

    public void add(BACnetObjectIdentifier objId,
                    BACnetPropertyIdentifier... props)
    {
        list_of_read_access_specifications.add(ReadAccessSpecification.newRequest(objId, props));
    }
}
