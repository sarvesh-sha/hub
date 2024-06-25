/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.serialization.SerializationTag;

public class UnknownAsync extends BaseAsyncModel
{
    @SerializationTag(number = 0)
    public byte[] payload;
}
