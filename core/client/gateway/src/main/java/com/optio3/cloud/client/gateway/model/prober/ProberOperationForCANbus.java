/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model.prober;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForCANbusToRawRead.class), @JsonSubTypes.Type(value = ProberOperationForCANbusToDecodedRead.class) })
public abstract class ProberOperationForCANbus extends ProberOperation
{
    @JsonSubTypes({ @JsonSubTypes.Type(value = ProberOperationForCANbusToRawRead.Results.class), @JsonSubTypes.Type(value = ProberOperationForCANbusToDecodedRead.Results.class) })
    public static abstract class BaseResults extends ProberOperation.BaseResults
    {
    }

    //--//

    public String  port;
    public int     frequency;
    public boolean noTermination;
    public boolean invert;
}
