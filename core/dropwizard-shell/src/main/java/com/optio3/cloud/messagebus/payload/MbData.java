/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.payload;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.messagebus.MessageBusPayload;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.util.Exceptions;

@JsonSubTypes({ @JsonSubTypes.Type(value = MbData_Message.class), @JsonSubTypes.Type(value = MbData_Message_Reply.class) })
public abstract class MbData extends MessageBusPayload
{
    public String origin;
    public String destination;

    public String channel;

    public JsonNode payload;

    public List<String> brokersPath;

    //--//

    public abstract MbData makeCopy();

    protected void copyFrom(MbData other,
                            boolean includePath,
                            boolean includePayload)
    {
        if (includePath)
        {
            if (other.brokersPath != null)
            {
                brokersPath = Lists.newArrayList(other.brokersPath);
            }
        }

        if (includePayload)
        {
            payload = other.payload;
        }

        origin = other.origin;
        destination = other.destination;

        messageId = other.messageId;

        channel = other.channel;
    }

    public void convertPayload(Object payload) throws
                                               JsonProcessingException
    {
        if (this.payload != null)
        {
            throw Exceptions.newRuntimeException("Payload on MessageBusPayload already set: %s <-> %s", JsonWebSocket.serializeValue(this), JsonWebSocket.serializeValue(payload));
        }

        this.payload = toTree(payload);
    }

    //--//

    public static boolean isForLocalService(String id)
    {
        return WellKnownDestination.Service.getId()
                                           .equals(id);
    }

    public static boolean isForServices(String id)
    {
        return WellKnownDestination.Service_Broadcast.getId()
                                                     .equals(id);
    }

    public static boolean isBroadcast(String id)
    {
        return WellKnownDestination.Broadcast.getId()
                                             .equals(id);
    }

    //--//

    public boolean alreadyVisited(String brokerId)
    {
        return brokersPath != null && brokersPath.contains(brokerId);
    }
}
