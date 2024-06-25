/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.unconfirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.bacnet.model.pdu.request.UnconfirmedServiceRequest;
import com.optio3.serialization.SerializationTag;

public class WhoIs extends UnconfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Optional<Unsigned32> device_instance_range_low_limit;

    @SerializationTag(number = 1)
    public Optional<Unsigned32> device_instance_range_high_limit;

    //--//

    public void setRange(int lowLimit,
                         int highLimit)
    {
        device_instance_range_low_limit  = Optional.of(Unsigned32.box(lowLimit));
        device_instance_range_high_limit = Optional.of(Unsigned32.box(highLimit));
    }
}
