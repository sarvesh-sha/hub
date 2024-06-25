/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.constructed;

import com.optio3.protocol.model.bacnet.Sequence;
import com.optio3.protocol.model.bacnet.enums.BACnetErrorClass;
import com.optio3.protocol.model.bacnet.enums.BACnetErrorCode;
import com.optio3.serialization.SerializationTag;

public final class BACnetError extends Sequence
{
    @SerializationTag(number = 0)
    public BACnetErrorClass error_class;

    @SerializationTag(number = 1)
    public BACnetErrorCode error_code;
}
