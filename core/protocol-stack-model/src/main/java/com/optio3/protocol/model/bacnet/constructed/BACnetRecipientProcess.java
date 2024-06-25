/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.lang.Unsigned32;
import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.constructed.choice.BACnetRecipient;
import com.optio3.serialization.SerializationTag;

public final class BACnetRecipientProcess extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetRecipient recipient;

    @SerializationTag(number = 1)
    public Unsigned32 process_identifier;
}
