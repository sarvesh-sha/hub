/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.async;

import java.time.ZonedDateTime;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseAsyncModel;
import com.optio3.lang.Unsigned16;
import com.optio3.serialization.SerializationTag;

public class DeleteSystemParameterAsync extends BaseAsyncModel
{
    @SerializationTag(number = 0)
    public ZonedDateTime version;

    @SerializationTag(number = 4)
    public Unsigned16 id;
}
