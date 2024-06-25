/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.stream.OutputBuffer;

public abstract class ServiceCommon
{
    public OutputBuffer encodePayload()
    {
        return ApplicationPDU.encodePayload(this);
    }
}
