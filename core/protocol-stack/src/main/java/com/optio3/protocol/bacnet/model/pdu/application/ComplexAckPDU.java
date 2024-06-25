/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.application;

import com.optio3.lang.Unsigned8;
import com.optio3.protocol.bacnet.ServiceContext;
import com.optio3.protocol.bacnet.model.enums.ConfirmedServiceChoice;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceResponse;
import com.optio3.serialization.ConditionalFieldSelector;
import com.optio3.serialization.Reflection;
import com.optio3.serialization.SerializationTag;
import com.optio3.stream.InputBuffer;

public final class ComplexAckPDU extends ApplicationPDU implements ConditionalFieldSelector
{
    @SerializationTag(number = 0, bitOffset = 3, width = 1)
    public boolean segmentedMessage;

    @SerializationTag(number = 0, bitOffset = 2, width = 1)
    public boolean moreFollows;

    @SerializationTag(number = 1)
    public Unsigned8 invokeId;

    @SerializationTag(number = 2)
    public Unsigned8 sequenceNumber; // Only present if segmentedMessage is true.

    @SerializationTag(number = 3)
    public Unsigned8 proposedWindowSize; // Only present if segmentedMessage is true.

    @SerializationTag(number = 4, width = 8)
    public ConfirmedServiceChoice serviceChoice;

    //--//

    public ComplexAckPDU(InputBuffer buffer)
    {
        super(buffer);
    }

    //--//

    @Override
    public boolean shouldEncode(String fieldName)
    {
        switch (fieldName)
        {
            case "sequenceNumber":
            case "proposedWindowSize":
                return segmentedMessage;
        }

        return true;
    }

    @Override
    public boolean shouldDecode(String fieldName)
    {
        switch (fieldName)
        {
            case "sequenceNumber":
            case "proposedWindowSize":
                return segmentedMessage;
        }

        return true;
    }

    //--//

    @Override
    protected ServiceCommon allocatePayload()
    {
        return Reflection.newInstance(serviceChoice.response());
    }

    @Override
    public void dispatch(ServiceContext sc)
    {
        if (segmentedMessage)
        {
            sc.processResponseChunk(this);
            return;
        }

        ConfirmedServiceResponse<?> response = (ConfirmedServiceResponse<?>) decodePayload();
        sc.processResponse(response, invokeId.unbox(), serviceChoice);
    }
}
