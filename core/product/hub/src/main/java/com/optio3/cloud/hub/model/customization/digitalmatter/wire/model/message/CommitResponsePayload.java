/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.serialization.SerializationTag;

public class CommitResponsePayload extends BaseWireModel
{
    @SerializationTag(number = 0, width = 1, bitOffset = 0)
    public boolean success;
}
