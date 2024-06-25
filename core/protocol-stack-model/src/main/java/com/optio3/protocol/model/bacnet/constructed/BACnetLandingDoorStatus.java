/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetDoorStatus;
import com.optio3.serialization.SerializationTag;

public final class BACnetLandingDoorStatus extends Sequence
{
    @SerializationTag(number = 0)
    public List<typefor_landing_doors> landing_doors;

    public static class typefor_landing_doors extends Sequence
    {
        @SerializationTag(number = 0)
        public Unsigned8 floor_number;

        @SerializationTag(number = 1)
        public BACnetDoorStatus door_status;
    }
}
