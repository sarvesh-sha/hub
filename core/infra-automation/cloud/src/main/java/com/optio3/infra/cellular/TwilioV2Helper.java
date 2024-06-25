/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cellular;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.twilioV2.api.DefaultApi;
import com.optio3.infra.twilioV2.model.ApiResults;
import com.optio3.infra.twilioV2.model.Sim;
import com.optio3.infra.twilioV2.model.UsageRecord;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class TwilioV2Helper
{
    public static class Handler implements ICellularProviderHandler
    {
        private final TwilioV2Helper m_helper;

        Handler(CredentialDirectory credentials)
        {
            m_helper = TwilioV2Helper.buildIfPossible(credentials, null);
        }

        @Override
        public SimInfo lookupSim(String iccid)
        {
            Sim sim = CollectionUtils.firstElement(m_helper.listSims(null, iccid));
            if (sim != null)
            {
                SimInfo si = new SimInfo();
                si.id     = sim.sid;
                si.iccid  = sim.iccid;
                si.name   = sim.unique_name;
                si.status = statusFromTwilio(sim.status);

                return si;
            }

            return null;
        }

        @Override
        public void updateStatus(SimInfo si,
                                 String productId)
        {
            m_helper.updateSim(si.id, si.name, statusToTwilio(si.status));
        }

        @Override
        public List<SimCharges> getCharges(String id,
                                           ZonedDateTime start,
                                           ZonedDateTime end)
        {
            List<SimCharges> processedResults = Lists.newArrayList();

            final List<UsageRecord> results = m_helper.getUsage(id, start, end, UsageRecord.Granularity.HOUR);
            for (UsageRecord result : results)
            {
                SimCharges processedResult = new SimCharges();
                processedResult.timestamp = result.period.start_time;
                processedResult.upload    = result.data_upload;
                processedResult.download  = result.data_download;
                processedResult.total     = result.data_total;
                processedResults.add(processedResult);
            }

            return processedResults;
        }

        @Override
        public SimConnectionStatus getConnectionStatus(String id)
        {
            return null;
        }

        @Override
        public List<SimDataSession> getDataSessions(String id,
                                                    ZonedDateTime start,
                                                    ZonedDateTime end)
        {
            return Collections.emptyList();
        }

        @Override
        public List<SimDataExchange> getDataExchanges(Semaphore rateLimiter,
                                                      String id,
                                                      int days)
        {
            return Collections.emptyList();
        }

        //--//

        static Status statusFromTwilio(Sim.Status val)
        {
            if (val == null)
            {
                return null;
            }

            switch (val)
            {
                case NEW:
                    return Status.NEW;

                case READY:
                    return Status.READY;

                case ACTIVE:
                    return Status.ACTIVE;

                case INACTIVE:
                    return Status.INACTIVE;

                case SCHEDULED:
                    return Status.SCHEDULED;

                default:
                    return null;
            }
        }

        static Sim.Status statusToTwilio(Status val)
        {
            if (val == null)
            {
                return null;
            }

            switch (val)
            {
                case NEW:
                    return Sim.Status.NEW;

                case READY:
                    return Sim.Status.READY;

                case ACTIVE:
                    return Sim.Status.ACTIVE;

                case INACTIVE:
                    return Sim.Status.INACTIVE;

                case SCHEDULED:
                    return Sim.Status.SCHEDULED;

                default:
                    return null;
            }
        }
    }

    //--//

    public static final  String API_CREDENTIALS_SITE = "twilio.com";
    private static final String API_URL              = "https://supersim.twilio.com";

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

    private       long   m_timeout = 60000;
    private final String m_baseAddress;
    private final String m_apiKey;
    private final String m_apiSecret;

    public TwilioV2Helper(String baseAddress,
                          String apiKey,
                          String apiSecret)
    {
        m_baseAddress = baseAddress;
        m_apiKey      = apiKey;
        m_apiSecret   = apiSecret;
    }

    public static TwilioV2Helper buildIfPossible(CredentialDirectory credDir,
                                                 String account)
    {
        if (credDir != null)
        {
            ApiInfo ai = credDir.findFirstApiCredentialOrNull(API_CREDENTIALS_SITE, account);
            if (ai != null)
            {
                return new TwilioV2Helper(API_URL, ai.accessKey, ai.secretKey);
            }
        }

        return null;
    }

    //--//

    public List<Sim> listSims(Sim.Status status,
                              String iccid)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);
        List<Sim>  lst   = Lists.newArrayList();

        String pageToken = null;

        for (int page = 0; true; page++)
        {
            try
            {
                ApiResults res = proxy.listSims(status, iccid, 50, page, pageToken);
                if (res.sims.isEmpty())
                {
                    break;
                }

                lst.addAll(res.sims);

                pageToken = res.extractNextPage("PageToken");
                if (pageToken == null)
                {
                    break;
                }
            }
            catch (Throwable t)
            {
                break;
            }
        }

        return lst;
    }

    public Sim updateSim(String simSid,
                         String uniqueName,
                         Sim.Status status)
    {
        try
        {
            DefaultApi proxy = createProxy(DefaultApi.class);

            return proxy.updateSim(simSid, uniqueName, status);
        }
        catch (Throwable t)
        {
            // Swallow failures.
            return null;
        }
    }

    public List<UsageRecord> getUsage(String simSid,
                                      ZonedDateTime start,
                                      ZonedDateTime end,
                                      UsageRecord.Granularity granulariy)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        String start2 = start != null ? start.withZoneSameInstant(ZoneOffset.UTC)
                                             .toString() : null;

        String end2 = end != null ? end.withZoneSameInstant(ZoneOffset.UTC)
                                       .toString() : null;

        ApiResults res = proxy.getUsageRecords(simSid, start2, end2, granulariy);
        return res.usage_records;
    }

    //--//

    private <P> P createProxy(Class<P> cls)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        // Handle authentication.
        bean.setUsername(m_apiKey);
        bean.setPassword(m_apiSecret);

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(m_timeout);

        return cls.cast(client);
    }
}
