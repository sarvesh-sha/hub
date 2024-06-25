/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetPortPermission extends Sequence
{
    @SerializationTag(number = 0)
    public Unsigned8 port_id;

    @SerializationTag(number = 1)
    public Boolean enabled;
}
