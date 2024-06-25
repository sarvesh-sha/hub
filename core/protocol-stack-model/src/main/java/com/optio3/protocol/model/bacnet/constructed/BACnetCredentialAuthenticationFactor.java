/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetAccessAuthenticationFactorDisable;
import com.optio3.serialization.SerializationTag;

public final class BACnetCredentialAuthenticationFactor extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetAccessAuthenticationFactorDisable disable;

    @SerializationTag(number = 1)
    public BACnetAuthenticationFactor authentication_factor;
}
