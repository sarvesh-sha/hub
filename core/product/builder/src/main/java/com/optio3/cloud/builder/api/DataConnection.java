/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.ZonedDateTime;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.amazonaws.arn.Arn;
import com.amazonaws.arn.ArnResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.LoggerResource;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.Exceptions;
import io.swagger.annotations.Api;
import org.apache.commons.io.IOUtils;

@Api(tags = { "DataConnection" }) // For Swagger
@Optio3RestEndpoint(name = "DataConnection") // For Optio3 Shell
@Path("/v1/data-connection")
public class DataConnection
{
    public static final Logger LoggerInstance = new Logger(DataConnection.class);

    private static final ObjectMapper s_objectMapper;

    static
    {
        ObjectMapper mapper = ObjectMappers.SkipNulls.copy();
        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        s_objectMapper = mapper;
    }

    //--//

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("endpoint/AWS/SNS")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Optio3RequestLogLevel(Severity.Debug)
    @Optio3NoAuthenticationNeeded
    public String receiveRaw(@HeaderParam("x-amz-sns-message-type") String messageType,
                             @HeaderParam("x-amz-sns-topic-arn") String topic,
                             String payload) throws
                                             IOException
    {
        LoggerInstance.debugVerbose("SNS: Type=%s Topic=%s Payload=%s", messageType, topic, payload);

        if (messageType == null)
        {
            throw new IllegalArgumentException("Missing Message Type");
        }

        if (topic == null)
        {
            throw new IllegalArgumentException("Missing Topic");
        }

        try
        {
            Arn         arn    = Arn.fromString(topic);
            ArnResource arnRes = arn.getResource();
            switch (BoxingUtils.get(arnRes.getResourceType(), arnRes.getResource()))
            {
                case "SES_notification":
                    return handleSES(arn, messageType, ObjectMappers.SkipNulls.readTree(payload));
            }

            throw Exceptions.newIllegalArgumentException("Unknown Topic: %s", topic);
        }
        catch (Throwable t)
        {
            LoggerInstance.error("Failed to process SNS request, due to %s", t);

            throw t;
        }
    }

    private String handleSES(Arn arn,
                             String messageType,
                             JsonNode payload) throws
                                               IOException
    {
        switch (messageType)
        {
            case "SubscriptionConfirmation":
            {
                String subscribeURL = getText(payload, "SubscribeURL");

                LoggerInstance.info("SubscriptionConfirmation: %s", subscribeURL);

                URL               url           = new URL(subscribeURL);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

                urlConnection.setConnectTimeout(2 * 1000);
                urlConnection.setReadTimeout(2 * 1000);

                try
                {
                    urlConnection.getResponseCode();
                }
                finally
                {
                    IOUtils.close(urlConnection);
                }

                return "SUCCESS";
            }

            case "Notification":
                JsonNode payloadSub = ObjectMappers.SkipNulls.readTree(getText(payload, "Message"));
                switch (getText(payloadSub, "notificationType"))
                {
                    case "Bounce":
                        LoggerInstance.error("Email bounced!!");
                        try (LoggerResource resource = LoggerFactory.indent(">>>"))
                        {
                            Bounce bounce = getObject(payloadSub, "bounce", Bounce.class);
                            LoggerInstance.error("%s", ObjectMappers.prettyPrintAsJson(bounce));
                        }
                        return "SUCCESS";

                    case "Complaint":
                        LoggerInstance.error("Email complaint!!");
                        try (LoggerResource resource = LoggerFactory.indent(">>>"))
                        {
                            Complaint complaint = getObject(payloadSub, "complaint", Complaint.class);
                            LoggerInstance.error("%s", ObjectMappers.prettyPrintAsJson(complaint));
                        }
                        return "SUCCESS";

                    case "Delivery":
                        LoggerInstance.debug("Email delivery!!");
                        try (LoggerResource resource = LoggerFactory.indent(">>>"))
                        {
                            Mail mail = getObject(payloadSub, "mail", Mail.class);
                            LoggerInstance.debug("Mail: %s", ObjectMappers.prettyPrintAsJson(mail));

                            Delivery delivery = getObject(payloadSub, "delivery", Delivery.class);
                            LoggerInstance.debug("Delivery: %s", ObjectMappers.prettyPrintAsJson(delivery));
                        }
                        return "SUCCESS";
                }

                throw Exceptions.newIllegalArgumentException("Unknown notificationType: %s", ObjectMappers.prettyPrintAsJson(payloadSub));
        }

        throw Exceptions.newIllegalArgumentException("Unknown Message Type: %s", messageType);
    }

    private static String getText(JsonNode payload,
                                  String field)
    {
        JsonNode node = payload.get(field);
        return node != null ? node.asText() : "";
    }

    private static <T> T getObject(JsonNode payload,
                                   String field,
                                   Class<T> clz) throws
                                                 JsonProcessingException
    {
        JsonNode node = payload.get(field);
        return node != null ? s_objectMapper.treeToValue(node, clz) : null;
    }

    //--//

    @GET
    @Path("endpoint/pelion/authcode")
    @Optio3NoAuthenticationNeeded
    public String pelionAuthCode(@QueryParam("code") String code,
                                 @QueryParam("state") String state)
    {
        LoggerInstance.warn("pelionAuthCode: Token: %s", code);
        LoggerInstance.warn("pelionAuthCode: State: %s", state);

        return "<done>";
    }

    //--//

    static class Mail
    {
        public ZonedDateTime timestamp;
        public String        messageId;
        public String        source;
        public List<String>  destination;
    }

    static class Recipient
    {
        public String emailAddress;
        public String action;
        public String status;
        public String diagnosticCode;
    }

    static class Delivery
    {
        public ZonedDateTime timestamp;
        public int           processingTimeMillis;
        public List<String>  recipients;
        public String        smtpResponse;
        public String        reportingMTA;
        public String        remoteMtaIp;
    }

    static class Bounce
    {
        static class Recipient
        {
            public String emailAddress;
            public String action;
            public String status;
            public String diagnosticCode;
        }

        public ZonedDateTime timestamp;
        public String        feedbackId;
        public String        remoteMtaIp;
        public String        reportingMTA;

        public String          bounceType;
        public String          bounceSubType;
        public List<Recipient> bouncedRecipients;
    }

    static class Complaint
    {
        public ZonedDateTime timestamp;
        public String        feedbackId;

        public String          complaintSubType;
        public String          complaintFeedbackType;
        public List<Recipient> complainedRecipients;
    }
}
