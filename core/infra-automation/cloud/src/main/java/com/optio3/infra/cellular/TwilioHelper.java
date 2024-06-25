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
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.twilio.api.DefaultApi;
import com.optio3.infra.twilio.model.ApiResults;
import com.optio3.infra.twilio.model.Command;
import com.optio3.infra.twilio.model.DataSession;
import com.optio3.infra.twilio.model.RatePlan;
import com.optio3.infra.twilio.model.Sim;
import com.optio3.infra.twilio.model.UsageRecord;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;

public class TwilioHelper
{
    public static class Handler implements ICellularProviderHandler
    {
        private final TwilioHelper m_helper;

        Handler(CredentialDirectory credentials)
        {
            m_helper = TwilioHelper.buildIfPossible(credentials, null);
        }

        @Override
        public SimInfo lookupSim(String iccid)
        {
            Sim sim = CollectionUtils.firstElement(m_helper.listSims(null, iccid, null));
            if (sim != null)
            {
                SimInfo si = new SimInfo();
                si.id     = sim.sid;
                si.iccid  = sim.iccid;
                si.name   = sim.unique_name;
                si.status = statusFromTwilio(sim.status);
                si.plan   = sim.rate_plan_sid;

                return si;
            }

            return null;
        }

        @Override
        public void updateStatus(SimInfo si,
                                 String productId)
        {
            if (StringUtils.equals(productId, "Oyster2"))
            {
                si.plan = "DigitalMatter";
            }

            if (si.plan == null)
            {
                si.plan = "5CentsPerMB";
            }

            m_helper.updateSim(si.id, si.name, statusToTwilio(si.status), si.plan, false);
        }

        @Override
        public List<SimCharges> getCharges(String id,
                                           ZonedDateTime start,
                                           ZonedDateTime end)
        {
            List<SimCharges> processedResults = Lists.newArrayList();

            final List<UsageRecord> results = m_helper.getUsage(id, start, end, UsageRecord.Granularity.HOURLY);
            for (UsageRecord result : results)
            {
                SimCharges processedResult = new SimCharges();
                processedResult.timestamp = result.period.start;
                processedResult.upload    = result.data.upload;
                processedResult.download  = result.data.download;
                processedResult.total     = result.data.total;
                processedResult.billed    = result.data.billed;
                processedResults.add(processedResult);
            }

            return processedResults;
        }

        @Override
        public SimConnectionStatus getConnectionStatus(String id)
        {
            SimConnectionStatus res = new SimConnectionStatus();

            final List<DataSession> results = m_helper.getSessions(id, TimeUtils.past(24, TimeUnit.HOURS), null);
            for (DataSession result : results)
            {
                if (result.end == null)
                {
                    res.isOnline = true;

                    if (result.packets_downloaded + result.packets_uploaded > 10)
                    {
                        res.isTransferring = true;
                    }

                    break;
                }
            }

            return res;
        }

        @Override
        public List<SimDataSession> getDataSessions(String id,
                                                    ZonedDateTime start,
                                                    ZonedDateTime end)
        {
            List<SimDataSession> processedResults = Lists.newArrayList();

            final List<DataSession> results = m_helper.getSessions(id, start, end);
            for (DataSession result : results)
            {
                SimDataSession processedResult = new SimDataSession();
                processedResult.start       = result.start;
                processedResult.end         = result.end;
                processedResult.lastUpdated = result.last_updated;

                processedResult.packetsDownloaded = result.packets_downloaded;
                processedResult.packetsUploaded   = result.packets_uploaded;

                processedResult.cellId          = result.cell_id;
                processedResult.operator        = result.operator_name;
                processedResult.operatorCountry = result.operator_country;
                processedResult.radioLink       = result.radio_link;

                if (result.cell_location_estimate != null)
                {
                    processedResult.estimatedLongitude = result.cell_location_estimate.lon;
                    processedResult.estimatedLatitude  = result.cell_location_estimate.lat;
                }
                else
                {
                    processedResult.estimatedLongitude = Double.NaN;
                    processedResult.estimatedLatitude  = Double.NaN;
                }

                processedResults.add(processedResult);
            }

            return processedResults;
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

                case SUSPENDED:
                    return Status.SUSPENDED;

                case DEACTIVATED:
                    return Status.DEACTIVATED;

                case CANCELED:
                    return Status.CANCELED;

                case SCHEDULED:
                    return Status.SCHEDULED;

                case UPDATING:
                    return Status.UPDATING;

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

                case SUSPENDED:
                    return Sim.Status.SUSPENDED;

                case DEACTIVATED:
                    return Sim.Status.DEACTIVATED;

                case CANCELED:
                    return Sim.Status.CANCELED;

                case SCHEDULED:
                    return Sim.Status.SCHEDULED;

                case UPDATING:
                    return Sim.Status.UPDATING;

                default:
                    return null;
            }
        }
    }

    //--//

    public static final  String API_CREDENTIALS_SITE = "twilio.com";
    private static final String API_URL              = "https://wireless.twilio.com";

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

    public TwilioHelper(String baseAddress,
                        String apiKey,
                        String apiSecret)
    {
        m_baseAddress = baseAddress;
        m_apiKey      = apiKey;
        m_apiSecret   = apiSecret;
    }

    public static TwilioHelper buildIfPossible(CredentialDirectory credDir,
                                               String account)
    {
        if (credDir != null)
        {
            ApiInfo ai = credDir.findFirstApiCredentialOrNull(API_CREDENTIALS_SITE, account);
            if (ai != null)
            {
                return new TwilioHelper(API_URL, ai.accessKey, ai.secretKey);
            }
        }

        return null;
    }

    //--//

    public List<Sim> listSims(Sim.Status status,
                              String iccid,
                              String ratePlan)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);
        List<Sim>  lst   = Lists.newArrayList();

        String pageToken = null;

        for (int page = 0; true; page++)
        {
            try
            {
                ApiResults res = proxy.listSims(status, iccid, ratePlan, 50, page, pageToken);
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
                         Sim.Status status,
                         String ratePlan,
                         boolean reset)
    {
        try
        {
            DefaultApi proxy = createProxy(DefaultApi.class);

            return proxy.updateSim(simSid, uniqueName, status, ratePlan, reset ? "resetting" : null);
        }
        catch (Throwable t)
        {
            // Swallow failures.
            return null;
        }
    }

    public List<RatePlan> listRatePlans()
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        ApiResults res = proxy.listRatePlans();
        return res.rate_plans;
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

    public List<DataSession> getSessions(String simSid,
                                         ZonedDateTime start,
                                         ZonedDateTime end)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        String start2 = start != null ? start.withZoneSameInstant(ZoneOffset.UTC)
                                             .toString() : null;

        String end2 = end != null ? end.withZoneSameInstant(ZoneOffset.UTC)
                                       .toString() : null;

        ApiResults res = proxy.getDataSessions(simSid, start2, end2);
        return res.data_sessions;
    }

    public Command sendTextCommand(String simSid,
                                   String command)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        return proxy.command(simSid, command, Command.CommandMode.TEXT);
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
