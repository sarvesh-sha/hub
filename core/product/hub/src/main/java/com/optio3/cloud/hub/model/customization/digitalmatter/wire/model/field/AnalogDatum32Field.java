/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.field;

import com.optio3.serialization.SerializationTag;

public class AnalogDatum32Field
{
    @SerializationTag(number = 0)
    public byte input;

    @SerializationTag(number = 1)
    public int value;
}
