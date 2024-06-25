/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("MbControlExchangeCapabilities") // No underscore in model name, due to Swagger issues.
public class MbControl_ExchangeCapabilities extends MbControl
{
    public Set<String> available;
    public Set<String> required;
}
