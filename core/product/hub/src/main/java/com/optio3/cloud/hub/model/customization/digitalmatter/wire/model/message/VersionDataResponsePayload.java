/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.lang.Unsigned16;
import com.optio3.lang.Unsigned32;
import com.optio3.serialization.SerializationTag;

public class VersionDataResponsePayload extends BaseWireModel
{
    @SerializationTag(number = 5)
    public Unsigned32 deviceSerial;

    @SerializationTag(number = 9)
    public Unsigned32 canAddress = Unsigned32.box(0xFFFFFFFF);

    @SerializationTag(number = 13)
    public Unsigned16 reserved1 = Unsigned16.box(0);

    @SerializationTag(number = 15)
    public Unsigned16 reserved2 = Unsigned16.box(0);
}
