/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("MbControlListSubscriptions") // No underscore in model name, due to Swagger issues.
public class MbControl_ListSubscriptions extends MbControl
{
}
