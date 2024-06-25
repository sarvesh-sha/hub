/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request;

import com.optio3.protocol.bacnet.model.pdu.application.ServiceCommon;

public abstract class ConfirmedServiceResponse<T extends ConfirmedServiceRequest> extends ServiceCommon
{
    public static class NoDataReply extends ConfirmedServiceResponse<ConfirmedServiceRequest>
    {
    }
}
