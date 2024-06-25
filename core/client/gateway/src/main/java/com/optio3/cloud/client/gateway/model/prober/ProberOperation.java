/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForBACnet.class), @JsonSubTypes.Type(value = ProberOperationForCANbus.class), @JsonSubTypes.Type(value = ProberOperationForIpn.class) })
public abstract class ProberOperation
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type_result")
    @JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForBACnet.BaseResults.class),
                    @JsonSubTypes.Type(value = ProberOperationForCANbus.BaseResults.class),
                    @JsonSubTypes.Type(value = ProberOperationForIpn.BaseResults.class) })
    public static abstract class BaseResults
    {
    }
}
