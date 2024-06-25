/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.infra.GitHubHelper;
import com.optio3.infra.github.model.event.CommonEvent;
import com.optio3.infra.github.model.event.EventType;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import io.dropwizard.jackson.Jackson;
import io.swagger.annotations.Api;

@Api(tags = { "GitHubWebHooks" }) // For Swagger
@Optio3RestEndpoint(name = "GitHubWebHooks") // For Optio3 Shell
@Path("/v1/github/webhooks")
public class GitHubWebHooks
{
    public static final Logger LoggerInstance = new Logger(GitHubWebHooks.class);

    private final static ObjectMapper s_mapper;

    static
    {
        ObjectMapper mapper = Jackson.newObjectMapper();

        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        s_mapper = mapper;
    }

    @Inject
    private BuilderConfiguration m_config;

    @POST
    public String postToHook(@Context ContainerRequestContext requestContext,
                             @HeaderParam("X-GitHub-Event") String eventType,
                             @HeaderParam("X-GitHub-Delivery") String guid,
                             @HeaderParam("X-Hub-Signature") String signature,
                             byte[] body) throws
                                          Exception
    {
        LoggerInstance.debug("eventType: %s%n", eventType);
        LoggerInstance.debug("guid: %s%n", guid);
        LoggerInstance.debug("signature: %s%n", signature);

        if (m_config.gitHubSignatureKey != null)
        {
            if (signature == null)
            {
                throw new NotAuthorizedException("No signature");
            }

            if (!GitHubHelper.validateSignature(m_config.gitHubSignatureKey, body, signature))
            {
                LoggerInstance.error("Got POST with invalid signature: GUID=%s eventType=%s, signature=%s", guid, eventType, signature);
                throw new NotAuthorizedException("Invalid signature");
            }
        }

        try
        {
            EventType et = EventType.parse(eventType);
            if (et == null)
            {
                JsonNode root = s_mapper.readTree(body);

                LoggerInstance.warn("Unknown event: %s", eventType);
                LoggerInstance.warn("%s",
                                    s_mapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(root));
            }
            else
            {
                CommonEvent ce = et.decode(s_mapper, body);
                LoggerInstance.debug("%s",
                                     s_mapper.writerWithDefaultPrettyPrinter()
                                             .writeValueAsString(ce));
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to process GitHub event %s : %s", eventType, t);

            throw t;
        }

        return "OK";
    }
}
