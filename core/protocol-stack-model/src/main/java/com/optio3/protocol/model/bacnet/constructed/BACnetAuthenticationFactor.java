/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetAuthenticationFactorType;
import com.optio3.serialization.SerializationTag;

public final class BACnetAuthenticationFactor extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetAuthenticationFactorType format_type;

    @SerializationTag(number = 1)
    public Unsigned32 format_class;

    @SerializationTag(number = 2)
    public byte[] value; // for encoding of values into this octet string see Annex P.
}
