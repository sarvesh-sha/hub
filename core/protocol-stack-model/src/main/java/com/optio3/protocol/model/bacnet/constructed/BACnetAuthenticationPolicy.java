/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.List;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetAuthenticationPolicy extends Sequence
{
    @SerializationTag(number = 0)
    public List<typefor_policy> policy;

    public static class typefor_policy extends Sequence
    {
        @SerializationTag(number = 0)
        public BACnetDeviceObjectReference credential_data_input;

        @SerializationTag(number = 1)
        public Unsigned32 index;
    }

    @SerializationTag(number = 1)
    public boolean order_enforced;

    @SerializationTag(number = 2)
    public Unsigned32 timeout;
}
