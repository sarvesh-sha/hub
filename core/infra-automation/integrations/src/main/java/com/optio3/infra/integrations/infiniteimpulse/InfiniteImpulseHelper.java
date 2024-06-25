/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.infiniteimpulse;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotAuthorizedException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Maps;
import com.optio3.infra.integrations.infiniteimpulse.api.InfiniteImpulseApi;
import com.optio3.infra.integrations.infiniteimpulse.model.LoginRequest;
import com.optio3.infra.integrations.infiniteimpulse.model.LoginResponse;
import com.optio3.infra.integrations.infiniteimpulse.model.MonitorsRequest;
import com.optio3.infra.integrations.infiniteimpulse.model.MonitorsResponse;
import com.optio3.infra.integrations.infiniteimpulse.model.TrendHistory;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.BoxingUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class InfiniteImpulseHelper
{
    public static final Logger LoggerInstance = new Logger(InfiniteImpulseHelper.class);

    private static final String API_URL = "https://api-idap.infinite-uptime.com";

    //--//

    private static final ObjectMapper        s_objectMapper;
    private static final JacksonJsonProvider s_jsonProvider;

    static
    {
        ObjectMapper mapper = new ObjectMapper();

        ObjectMappers.configureCaseInsensitive(mapper);

        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        //
        // As the Twilio API evolves, new optional properties could be added to the models.
        // Just ignore what we don't understand.
        //
        ObjectMappers.configureToIgnoreMissingProperties(mapper);

        ObjectMappers.configureEnumAsStrings(mapper, true);

        mapper.registerModules(new JavaTimeModule());

        s_objectMapper = mapper;
        s_jsonProvider = new JacksonJsonProvider(mapper);
    }

    //--//

    private final MonotonousTime       m_expiration;
    private       long                 m_timeout = 60000;
    private final String               m_baseAddress;
    private final String               m_email;
    private final String               m_password;
    private       String               m_bearer;
    private final Map<Integer, String> m_plants  = Maps.newHashMap();

    public InfiniteImpulseHelper(String baseAddress,
                                 String email,
                                 String password)
    {
        m_baseAddress = BoxingUtils.get(baseAddress, API_URL);
        m_email       = email;
        m_password    = password;

        m_expiration = TimeUtils.computeTimeoutExpiration(12, TimeUnit.HOURS);
    }

    //--//

    public boolean isStale()
    {
        return TimeUtils.isTimeoutExpired(m_expiration);
    }

    public Map<Integer, String> getPlants()
    {
        ensureLoggedIn();

        return Collections.unmodifiableMap(m_plants);
    }

    public List<MonitorsResponse> getMonitorsByPlantId(int plantId,
                                                       boolean detailed)
    {
        ensureLoggedIn();

        InfiniteImpulseApi proxy = createProxy(InfiniteImpulseApi.class);

        var req = new MonitorsRequest();
        req.id    = plantId;
        req.qtype = detailed ? 1 : 0;

        List<MonitorsResponse> res = proxy.getMonitorsByPlantId(req);
        for (var v : res)
        {
            if (StringUtils.equalsIgnoreCase("null", v.deviceName))
            {
                v.deviceName = null;
            }

            if (StringUtils.equalsIgnoreCase("null", v.label))
            {
                v.label = null;
            }
        }

        return res;
    }

    public TrendHistory getTrend(ZonedDateTime from,
                                 ZonedDateTime to,
                                 boolean fast,
                                 int monitorId)
    {
        ensureLoggedIn();

        InfiniteImpulseApi proxy = createProxy(InfiniteImpulseApi.class);

        from = from.withZoneSameInstant(ZoneOffset.UTC);
        to   = to.withZoneSameInstant(ZoneOffset.UTC);

        if (fast)
        {
            return proxy.trendHistory(from, to, "seconds", 10, monitorId);
        }
        else
        {
            return proxy.trendHistory(from, to, "minute", 1, monitorId);
        }
    }

    private void ensureLoggedIn()
    {
        if (m_bearer == null)
        {
            InfiniteImpulseApi proxy = createProxy(InfiniteImpulseApi.class);

            LoginRequest req = new LoginRequest();
            req.email    = m_email;
            req.password = m_password;

            LoginResponse res = proxy.login(req);
            m_bearer = res.accessToken;

            m_plants.clear();

            if (res.scopeSelector != null)
            {
                for (LoginResponse.Organization subOrganization : res.scopeSelector.subOrganizations)
                {
                    for (LoginResponse.Plant plant : subOrganization.plants)
                    {
                        m_plants.put(plant.id, plant.name);
                    }
                }
            }

            if (m_bearer == null)
            {
                throw new NotAuthorizedException("Failed to log into Infinite Impulse");
            }
        }
    }

    //--//

    private <P> P createProxy(Class<P> cls)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        if (m_bearer != null)
        {
            // Handle authentication.
            Map<String, String> map = Maps.newHashMap();
            map.put("Authorization", String.format("Bearer %s", m_bearer));
            bean.setHeaders(map);
        }

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(m_timeout);

        return cls.cast(client);
    }

    public static ObjectMapper getObjectMapper()
    {
        return s_objectMapper;
    }
}
