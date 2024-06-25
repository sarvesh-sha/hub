/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("MbDataMessage") // No underscore in model name, due to Swagger issues.
public class MbData_Message extends MbData
{
    @Override
    public MbData makeCopy()
    {
        MbData_Message copy = new MbData_Message();

        copy.copyFrom(this, true, true);

        return copy;
    }

    public boolean wasBroadcast()
    {
        return isBroadcast(destination) || isForServices(destination);
    }
}
