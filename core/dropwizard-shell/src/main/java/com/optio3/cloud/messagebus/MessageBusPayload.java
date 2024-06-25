/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.net.InetSocketAddress;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.JsonNode;
import com.optio3.cloud.JsonWebSocket;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbControl;
import com.optio3.cloud.messagebus.payload.MbControl_Reply;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.util.IdGenerator;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonSubTypes({ @Type(value = MbControl.class), @Type(value = MbControl_Reply.class), @Type(value = MbData.class) })
public abstract class MessageBusPayload
{
    @JsonIgnore
    public InetSocketAddress physicalConnection;

    @JsonIgnore
    public long messageSize;

    @JsonIgnore
    public Endpoint endpoint;

    @JsonIgnore
    public CookiePrincipal principal;

    public String messageId;

    //--//

    public void assignNewId()
    {
        messageId = IdGenerator.newGuid();
    }

    public static JsonNode toTree(Object payload)
    {
        return (payload instanceof JsonNode) ? (JsonNode) payload : JsonWebSocket.serializeValueAsTree(payload);
    }
}
