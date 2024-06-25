/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessRuleLocation;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessRuleTimeRange;
import com.optio3.serialization.SerializationTag;

public final class BACnetAccessRule extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetAccessRuleTimeRange time_range_specifier;

    @SerializationTag(number = 1)
    public Optional<BACnetDeviceObjectPropertyReference> time_range;

    @SerializationTag(number = 2)
    public BACnetAccessRuleLocation location_specifier;

    @SerializationTag(number = 3)
    public Optional<BACnetDeviceObjectPropertyReference> location;

    @SerializationTag(number = 4)
    public boolean enable;
}
