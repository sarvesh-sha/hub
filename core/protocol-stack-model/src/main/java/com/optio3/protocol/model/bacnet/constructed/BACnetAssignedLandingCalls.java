/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetLiftCarDirection;
import com.optio3.serialization.SerializationTag;

public final class BACnetAssignedLandingCalls extends Sequence
{
    @SerializationTag(number = 0)
    public List<typefor_landing_calls> landing_calls;

    public static class typefor_landing_calls extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned8 floor_number;

        @SerializationTag(number = 1)
        public BACnetLiftCarDirection direction;
    }
}
