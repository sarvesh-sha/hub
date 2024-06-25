/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetAuthenticationFactorType;
import com.optio3.serialization.SerializationTag;

public final class BACnetAuthenticationFactorFormat extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetAuthenticationFactorType format_type;

    @SerializationTag(number = 1)
    public Optional<Unsigned16> vendor_id;

    @SerializationTag(number = 2)
    public Optional<Unsigned16> vendor_format;
}
