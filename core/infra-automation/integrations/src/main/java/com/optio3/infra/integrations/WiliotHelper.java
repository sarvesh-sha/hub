/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class WiliotHelper
{
    public static final Logger LoggerInstance = new Logger(WiliotHelper.class);

    @Path("/")
    public interface MainApi
    {
        @POST
        @Path("/v1/auth/token/api")
        @Produces({ "application/json" })
        GetToken.Response getToken();

        @GET
        @Path("/v1/owner/{ownerId}/tag")
        @Produces({ "application/json" })
        ListTags.Response listTags(@PathParam("ownerId") String ownerId,
                                   @QueryParam("limit") Integer limit,
                                   @QueryParam("next") String next);

        @GET
        @Path("/v1/owner/{ownerId}/gateway")
        @Produces({ "application/json" })
        ListGateways.Response listGateways(@PathParam("ownerId") String ownerId,
                                           @QueryParam("cursor") String cursor);

        @POST
        @Path("/v1/owner/{ownerId}/gateway/{gatewayId}/mobile")
        @Consumes({ "application/json" })
        @Produces({ "application/json" })
        RegisterGateway.Response registerGateway(@PathParam("ownerId") String ownerId,
                                                 @PathParam("gatewayId") String gatewayId,
                                                 RegisterGateway.Request request);
    }

    public static class GetToken
    {
        public static class Response
        {
            public String access_token;
            public int    expires_in;
            public String scope;
            public String token_type;
        }
    }

    public static class ListTags
    {
        public static class Response
        {
            public static class Entry
            {
                public String id;
            }

            public Entry[] data;
            public String  next;
        }
    }

    public static class ListGateways
    {
        public static class Response
        {
            public static class Entry
            {
                public String        gatewayId;
                public String        gatewayName;
                public String        gatewayType;
                public String        status;
                //                    "reportedConf" : { },
                //                    "desiredConf" : { },
                //                    "gatewayInfo" : { },
                public ZonedDateTime activateAt;
                //                        "connections" : [ ],
                public boolean       online;
                public ZonedDateTime onlineUpdatedAt;
                public boolean       tagMetadataCouplingSupported;
                public boolean       downlinkSupported;
                public boolean       downlinkEnabled;
                public boolean       bridgeOtaUpgradeSupported;
                public boolean       busy;
            }

            public static class Metadata
            {
                public int     total;
                public String  cursor;
                public boolean hasNext;
            }

            public Entry[]  data;
            public Metadata meta;
        }
    }

    public static class RegisterGateway
    {
        public static class Request
        {
            public String gatewayType;
            public String gatewayName;
        }

        public static class Response
        {
            public static class Entry
            {
                public String access_token;
                public int    expires_in;
                public String refresh_token;
                public String token_type;
                public String userId;
                public String ownerId;
            }

            public Entry data;
        }
    }
    //--//

    private static final JacksonJsonProvider s_jsonProvider;

    static
    {
        ObjectMapper mapper = new ObjectMapper();

        ObjectMappers.configureCaseInsensitive(mapper);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        //
        // As the Wiliot API evolves, new optional properties could be added to the models.
        // Just ignore what we don't understand.
        //
        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        ObjectMappers.configureEnumAsStrings(mapper, true);

        mapper.registerModules(new JavaTimeModule());

        s_jsonProvider = new JacksonJsonProvider(mapper);
    }

    //--//

    class TokenHolder
    {
        private final String         m_accessKey;
        private       String         m_token;
        private       MonotonousTime m_expiration;

        TokenHolder(String accessKey)
        {
            m_accessKey = accessKey;
        }

        <P> P createProxy(Class<P> cls) throws
                                        Exception
        {
            ensureLoggedIn();

            return WiliotHelper.this.createProxy(cls, m_token);
        }

        private void ensureLoggedIn()
        {
            if (m_token != null)
            {
                if (!TimeUtils.isTimeoutExpired(m_expiration))
                {
                    return;
                }

                m_token = null;
            }

            MainApi proxy = WiliotHelper.this.createProxy(MainApi.class, m_accessKey);
            var     res   = proxy.getToken();

            m_token      = String.format("Bearer %s", res.access_token);
            m_expiration = TimeUtils.computeTimeoutExpiration(res.expires_in - 60, TimeUnit.SECONDS);
        }
    }

    private final String      m_baseUrl;
    private final String      m_ownerId;
    private final String      m_gatewayId;
    private final TokenHolder m_tokenForAsset;
    private final TokenHolder m_tokenForEdge;

    private final String                         m_mqttBroker;
    private       RegisterGateway.Response.Entry m_gatewayRegistration;
    private       MonotonousTime                 m_gatewayRegistrationExpiration;
    private       IMqttClient                    m_gatewateMqttClient;

    public WiliotHelper(String baseUrl,
                        String mqttBroker,
                        String ownerId,
                        String accessKeyForAsset,
                        String accessKeyForEdge,
                        String gatewayId)
    {
        if (baseUrl == null)
        {
            baseUrl = "https://api.us-east-2.prod.wiliot.cloud";
        }

        if (mqttBroker == null)
        {
            mqttBroker = "ssl://mqtt.us-east-2.prod.wiliot.cloud:1883";
        }

        m_baseUrl       = baseUrl;
        m_ownerId       = ownerId;
        m_tokenForAsset = new TokenHolder(accessKeyForAsset);
        m_tokenForEdge  = new TokenHolder(accessKeyForEdge);
        m_gatewayId     = gatewayId;
        m_mqttBroker    = mqttBroker;
    }

    public ListTags.Response listTags(Integer limit,
                                      String next) throws
                                                   Exception
    {
        return m_tokenForAsset.createProxy(MainApi.class)
                              .listTags(m_ownerId, limit, next);
    }

    public ListGateways.Response listGateways() throws
                                                Exception
    {
        return m_tokenForEdge.createProxy(MainApi.class)
                             .listGateways(m_ownerId, null);
    }

    public void registerGateway() throws
                                  Exception
    {
        if (m_gatewayRegistration == null || TimeUtils.isTimeoutExpired(m_gatewayRegistrationExpiration))
        {
            var req = new RegisterGateway.Request();
            req.gatewayType = "mobile";
            req.gatewayName = m_gatewayId;

            m_gatewayRegistration = m_tokenForEdge.createProxy(MainApi.class)
                                                  .registerGateway(m_ownerId, m_gatewayId, req).data;

            m_gatewayRegistrationExpiration = TimeUtils.computeTimeoutExpiration(m_gatewayRegistration.expires_in - 60, TimeUnit.SECONDS);

            closeMqttClient();
        }
    }

    //--//

    private <P> P createProxy(Class<P> cls,
                              String auth)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseUrl);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        // Handle authentication.
        Map<String, String> map = Maps.newHashMap();
        map.put("Authorization", auth);
        bean.setHeaders(map);

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(10_000);

        return cls.cast(client);
    }

    //--//

    public static class PixelDataRequest
    {
        public static class Location
        {
            public double lat;
            public double lng;
        }

        public static class Packet
        {
            public long   timestamp;
            public int    sequenceId;
            public int    rssi;
            public String payload;
        }

        public       String       gatewayId;
        public       String       gatewayType;
        public       String       gatewayName;
        public       Location     location;
        public       long         timestamp;
        public final List<Packet> packets = Lists.newArrayList();
    }

    public long sendPixelTag(String bridge,
                             String payload,
                             double lat,
                             double lon)
    {
        var now = TimeUtils.nowMilliUtc();

        var req = new PixelDataRequest();
        req.gatewayId   = m_gatewayId;
        req.gatewayType = "mobile";
        req.gatewayName = m_gatewayId;
        req.timestamp   = now;

        if (Double.isFinite(lat) && Double.isFinite(lon))
        {
            var location = new PixelDataRequest.Location();
            location.lat = lat;
            location.lng = lon;
            req.location = location;
        }

        var packetBridge = new PixelDataRequest.Packet();
        packetBridge.timestamp  = now;
        packetBridge.sequenceId = 0;
        packetBridge.payload    = bridge;
        req.packets.add(packetBridge);

        if (!StringUtils.isEmpty(payload))
        {
            var packet = new PixelDataRequest.Packet();
            packet.timestamp  = now;
            packet.sequenceId = 1;
            packet.payload    = payload;
            req.packets.add(packet);
        }

        try
        {
            var message = ObjectMappers.SkipNulls.writeValueAsString(req);
            //noinspection resource
            ensureMqttClient().publish(String.format("data/%s/%s", m_ownerId, m_gatewayId), message.getBytes(), 0, false);
            return now;
        }
        catch (Exception e)
        {
            LoggerInstance.debug("Failed to publish sample: %s", e);
            return 0;
        }
    }

    private IMqttClient ensureMqttClient() throws
                                           Exception
    {
        registerGateway();

        if (m_gatewateMqttClient == null)
        {
            MemoryPersistence  persistence = new MemoryPersistence();
            MqttClient         client      = new MqttClient(m_mqttBroker, m_gatewayId, persistence);
            MqttConnectOptions connOpts    = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setUserName(m_ownerId);
            connOpts.setKeepAliveInterval(120);
            connOpts.setPassword(m_gatewayRegistration.access_token.toCharArray());

            // Connect to the MQTT Broker
            client.connect(connOpts);
            m_gatewateMqttClient = client;
        }

        return m_gatewateMqttClient;
    }

    private void closeMqttClient()
    {
        if (m_gatewateMqttClient != null)
        {
            try
            {
                m_gatewateMqttClient.close();
            }
            catch (MqttException ignored)
            {
            }

            m_gatewateMqttClient = null;
        }
    }
}
