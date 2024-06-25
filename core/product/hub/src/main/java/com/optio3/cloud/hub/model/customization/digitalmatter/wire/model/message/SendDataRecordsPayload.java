/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.customization.digitalmatter.wire.model.message;

import com.optio3.cloud.hub.model.customization.digitalmatter.wire.BaseWireModel;
import com.optio3.serialization.SerializationTag;

public class SendDataRecordsPayload extends BaseWireModel
{
    @SerializationTag(number = 0)
    public SendDataRecordPayload[] records;
}
