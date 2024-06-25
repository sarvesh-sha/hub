/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.serialization.SerializationTag;

public final class BACnetAssignedAccessRights extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetDeviceObjectReference assigned_access_rights;

    @SerializationTag(number = 0)
    public boolean enable;
}
