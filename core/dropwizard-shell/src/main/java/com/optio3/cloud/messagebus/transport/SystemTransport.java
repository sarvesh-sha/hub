/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.transport;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.JsonConnectionCapability;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.MessageBusPayload;
import com.optio3.cloud.messagebus.MessageBusPayloadCallback;
import com.optio3.cloud.messagebus.TransportSecurityPolicy;
import com.optio3.cloud.messagebus.WellKnownDestination;

public interface SystemTransport
{
    void close();

    boolean isOpen();

    boolean isService();

    void markAsActive();

    String getEndpointId();

    String getPurposeInfo();

    EnumSet<JsonConnectionCapability> exchangeCapabilities(EnumSet<JsonConnectionCapability> available,
                                                           EnumSet<JsonConnectionCapability> required);

    @NotNull CookiePrincipal getTransportPrincipal();

    default String getTransportPrincipalAsText()
    {
        final CookiePrincipal principal = getTransportPrincipal();
        return principal.getName();
    }

    TransportSecurityPolicy getPolicy();

    Endpoint getEndpointForDestination(String destination);

    CompletableFuture<Void> dispatch(MessageBusPayload msg,
                                     MessageBusPayloadCallback notifyPayloadSize) throws
                                                                                  Exception;

    static boolean isService(String endpointId)
    {
        return endpointId != null && endpointId.startsWith(WellKnownDestination.Service.getId());
    }
}
