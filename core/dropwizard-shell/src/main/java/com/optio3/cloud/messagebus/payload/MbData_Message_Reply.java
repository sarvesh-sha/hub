/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;

@JsonTypeName("MbDataMessageReply") // No underscore in model name, due to Swagger issues.
public class MbData_Message_Reply extends MbData
{
    @Override
    public MbData makeCopy()
    {
        MbData_Message_Reply copy = new MbData_Message_Reply();

        copy.copyFrom(this, true, true);

        return copy;
    }

    public static MbData_Message_Reply prepareForReply(MbData_Message data,
                                                       Object payload) throws
                                                                       JsonProcessingException
    {
        MbData_Message_Reply copy = new MbData_Message_Reply();

        //
        // Don't copy the previous path.
        //
        copy.copyFrom(data, false, false);

        //
        // Swap identities.
        //
        copy.origin = data.destination;
        copy.destination = data.origin;

        copy.convertPayload(payload);

        return copy;
    }
}
