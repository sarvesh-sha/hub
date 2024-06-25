/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.cellular;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.pelion.api.APNInformationApi;
import com.optio3.infra.pelion.api.AnalyticsApi;
import com.optio3.infra.pelion.api.ProvisioningApi;
import com.optio3.infra.pelion.api.StockOrdersApi;
import com.optio3.infra.pelion.api.SubscriberApi;
import com.optio3.infra.pelion.model.ActivationRequestBody;
import com.optio3.infra.pelion.model.ActivationResponse;
import com.optio3.infra.pelion.model.ApiResults;
import com.optio3.infra.pelion.model.ApnDetail;
import com.optio3.infra.pelion.model.ApnLog;
import com.optio3.infra.pelion.model.DataUsage;
import com.optio3.infra.pelion.model.DataUsageByIpAddress;
import com.optio3.infra.pelion.model.PelionDateTime;
import com.optio3.infra.pelion.model.StockOrders;
import com.optio3.infra.pelion.model.SubscriberResponse;
import com.optio3.infra.pelion.model.TariffObject;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class PelionHelper
{
    public static class Handler implements ICellularProviderHandler
    {
        private final PelionHelper m_helper;

        Handler(CredentialDirectory credentials)
        {
            m_helper = PelionHelper.buildIfPossible(credentials, null);
        }

        @Override
        public SimInfo lookupSim(String iccid)
        {
            SubscriberResponse sim = m_helper.getSim(iccid);
            if (sim != null)
            {
                SimInfo si = new SimInfo();
                si.id    = sim.physicalId;
                si.iccid = iccid;
                si.name  = null;
                si.plan  = sim.tariffName;

                if (sim.isActive)
                {
                    si.status = Status.ACTIVE;
                }
                else if (sim.isBarred)
                {
                    si.status = Status.SUSPENDED;
                }
                else
                {
                    si.status = Status.NEW;
                }

                return si;
            }

            LoggerInstance.info("Looking for SIM %s...", iccid);
            List<StockOrders> orders = m_helper.listStockOrders();
            if (orders != null)
            {
                LoggerInstance.info("Found %d orders", orders.size());

                for (StockOrders order : orders)
                {
                    LoggerInstance.info("Looking for SIM %s in order %s (%s)...", iccid, order.orderId, order.dateCreated);

                    Set<String> sims = m_helper.extractSimsFromOrder(order.orderId);
                    LoggerInstance.info("Found %d SIMs in order %s", sims.size(), order.orderId);
                    if (sims.contains(iccid))
                    {
                        LoggerInstance.info("Found SIM!", iccid);
                        SimInfo si = new SimInfo();
                        si.id     = iccid;
                        si.iccid  = iccid;
                        si.status = Status.NEW;

                        return si;
                    }
                }
            }

            return null;
        }

        @Override
        public void updateStatus(SimInfo si,
                                 String productId)
        {
            if (si.status == Status.READY)
            {
                Map<String, List<TariffObject>> tariff = m_helper.getTariff(si.iccid);
                if (tariff != null)
                {
                    LoggerInstance.info("Found tariff(s) '%s' for SIM %s", tariff.keySet(), si.iccid);

                    for (List<TariffObject> tariffObjects : tariff.values())
                    {
                        for (TariffObject tariffObject : tariffObjects)
                        {
                            LoggerInstance.info("Activating SIM %s on %s", si.iccid, tariffObject.productSetID);
                            m_helper.activate(si.iccid, null, tariffObject.productSetID);
                            LoggerInstance.info("Activated SIM %s on %s!!", si.iccid, tariffObject.productSetID);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public List<SimCharges> getCharges(String id,
                                           ZonedDateTime start,
                                           ZonedDateTime end)
        {
            List<SimCharges> processedResults = Lists.newArrayList();

            if (start == null)
            {
                start = TimeUtils.future(-1, TimeUnit.DAYS);
            }

            if (end == null)
            {
                end = TimeUtils.now();
            }

            final DataUsage results = m_helper.getUsage(id, start, end);
            if (results != null)
            {
                Set<ZonedDateTime> timestamps = Sets.newHashSet();

                timestamps.addAll(results.mobileOriginated.keySet());
                timestamps.addAll(results.mobileTerminated.keySet());

                for (ZonedDateTime timestamp : timestamps)
                {
                    SimCharges sc = new SimCharges();
                    sc.timestamp = timestamp;
                    sc.upload    = BoxingUtils.get(results.mobileOriginated.get(timestamp), 0);
                    sc.download  = BoxingUtils.get(results.mobileTerminated.get(timestamp), 0);
                    sc.total     = sc.upload + sc.download;
                    processedResults.add(sc);
                }

                processedResults.sort((a, b) -> TimeUtils.compare(a.timestamp, b.timestamp));
            }

            return processedResults;
        }

        @Override
        public SimConnectionStatus getConnectionStatus(String id)
        {
            SubscriberResponse sim = m_helper.getSim(id);
            if (sim != null && sim.networkState != null)
            {
                SimConnectionStatus res = new SimConnectionStatus();
                res.isOnline       = sim.networkState.isOnline;
                res.isTransferring = sim.networkState.isTransferring;
                return res;
            }

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
            List<SimDataExchange> processedResults = Lists.newArrayList();

            ZonedDateTime now = TimeUtils.now();

            List<Integer> daysLst = Lists.newArrayList();
            for (int daysAgo = 0; daysAgo < days; daysAgo++)
            {
                daysLst.add(daysAgo);
            }

            CollectionUtils.transformInParallel(daysLst, rateLimiter, (daysAgo) ->
            {
                DataUsageByIpAddress results = m_helper.getUsageByIpAddress(id, now.minus(daysAgo + 1, ChronoUnit.DAYS), now.minus(daysAgo, ChronoUnit.DAYS));
                if (results != null)
                {
                    for (Map.Entry<String, DataUsageByIpAddress.Details> pair : results.dataUsageByIp.entrySet())
                    {
                        SimDataExchange ex = new SimDataExchange();
                        ex.ip      = pair.getKey();
                        ex.daysAgo = daysAgo;
                        ex.bytes   = pair.getValue().bytes;

                        processedResults.add(ex);
                    }
                }

                return null;
            });

            return processedResults;
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(PelionHelper.class);

    public static final  String API_CREDENTIALS_SITE = "pelion-us.com";
    private static final String API_URL              = "https://api.connectivity-us.pelion.com";
    private static final String API_URL_ALTERNATE    = "https://pcm-us-api.iot-x.com";

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
    private final String m_apiSecret;

    public PelionHelper(String baseAddress,
                        String apiSecret)
    {
        m_baseAddress = baseAddress;
        m_apiSecret   = apiSecret;
    }

    public static PelionHelper buildIfPossible(CredentialDirectory credDir,
                                               String account)
    {
        if (credDir != null)
        {
            ApiInfo ai = credDir.findFirstApiCredentialOrNull(API_CREDENTIALS_SITE, account);
            if (ai != null)
            {
                return new PelionHelper(API_URL, ai.secretKey);
            }
        }

        return null;
    }

    public static boolean isValidICCID(String val)
    {
        return val != null && (val.length() == 19 || val.length() == 20);
    }

    //--//

    public List<SubscriberResponse> listSims()
    {
        SubscriberApi            proxy = createProxy(SubscriberApi.class);
        List<SubscriberResponse> lst   = Lists.newArrayList();

        for (int offset = 0; true; )
        {
            try
            {
                ApiResults<List<SubscriberResponse>> res = proxy.getSubscribers(50, offset, null, null);

                List<SubscriberResponse> lstPage = res.decode(s_objectMapper, new TypeReference<List<SubscriberResponse>>()
                {
                });

                if (lstPage.isEmpty())
                {
                    break;
                }

                lst.addAll(lstPage);

                offset += lstPage.size();
            }
            catch (Throwable t)
            {
                decodeFailure("getSubscribers", t);
                break;
            }
        }

        return lst;
    }

    public SubscriberResponse getSim(String iccid)
    {
        SubscriberApi proxy = createProxy(SubscriberApi.class);

        try
        {
            ApiResults<SubscriberResponse> res = proxy.getSubscriber(iccid);

            return res.decode(s_objectMapper, new TypeReference<SubscriberResponse>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getSim", t);
            return null;
        }
    }

    public DataUsage getUsage(String iccid,
                              ZonedDateTime startDate,
                              ZonedDateTime endDate)
    {
        AnalyticsApi proxy = createProxy(AnalyticsApi.class);

        try
        {
            ApiResults<DataUsage> res = proxy.getDataUsage(PelionDateTime.wrap(startDate), PelionDateTime.wrap(endDate), iccid);

            return res.decode(s_objectMapper, new TypeReference<DataUsage>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getUsage", t);
            return null;
        }
    }

    public DataUsageByIpAddress getUsageByIpAddress(String iccid,
                                                    ZonedDateTime startDate,
                                                    ZonedDateTime endDate)
    {
        AnalyticsApi proxy = createProxy(AnalyticsApi.class);

        try
        {
            ApiResults<DataUsageByIpAddress> res = proxy.getDataUsageByIpAddress(PelionDateTime.wrap(startDate), PelionDateTime.wrap(endDate), iccid);

            return res.decode(s_objectMapper, new TypeReference<DataUsageByIpAddress>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getUsageByIpAddress", t);
            return null;
        }
    }

    public List<StockOrders> listStockOrders()
    {
        StockOrdersApi proxy = createProxy(StockOrdersApi.class);

        try
        {
            ApiResults<List<StockOrders>> res = proxy.stockOrdersGet();

            return res.decode(s_objectMapper, new TypeReference<List<StockOrders>>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("listStockOrders", t);
            return null;
        }
    }

    public Set<String> extractSimsFromOrder(String orderId)
    {
        Set<String> sims = Sets.newHashSet();

        try
        {
            StockOrdersApi proxy = createProxy(StockOrdersApi.class);

            XSSFWorkbook workbook;

            try (InputStream stream = proxy.stockOrdersOrderIdSubscribersDownloadGet(orderId))
            {
                workbook = new XSSFWorkbook(stream);
            }

            for (int i = 0; i < workbook.getNumberOfSheets(); i++)
            {
                XSSFSheet sheet = workbook.getSheetAt(0);
                for (int j = sheet.getFirstRowNum(); j <= sheet.getLastRowNum(); j++)
                {
                    XSSFRow  row  = sheet.getRow(j);
                    XSSFCell cell = row.getCell(0);

                    sims.add(cell.getStringCellValue());
                }
            }
        }
        catch (Throwable t)
        {
            decodeFailure("extractSimsFromOrder", t);
        }

        return sims;
    }

    public Map<String, List<TariffObject>> getTariff(String sim)
    {
        ProvisioningApi proxy = createProxy(ProvisioningApi.class);

        try
        {
            ApiResults<Map<String, List<TariffObject>>> res = proxy.getAvailableTariffs(sim);

            return res.decode(s_objectMapper, new TypeReference<Map<String, List<TariffObject>>>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getTariff", t);
            return null;
        }
    }

    public List<ApnLog> getAPNLog(String sim)
    {
        APNInformationApi proxy = createProxy(APNInformationApi.class);

        try
        {
            ApiResults<List<ApnLog>> res = proxy.getAll(sim, null, null, null, null);

            return res.decode(s_objectMapper, new TypeReference<List<ApnLog>>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getAPNLog", t);
            return null;
        }
    }

    public List<ApnDetail> getAPNDetails(String sim)
    {
        APNInformationApi proxy = createProxy(APNInformationApi.class);

        try
        {
            ApiResults<List<ApnDetail>> res = proxy.apnDetails(sim);

            return res.decode(s_objectMapper, new TypeReference<List<ApnDetail>>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("getAPNLog", t);
            return null;
        }
    }

    public ActivationResponse activate(String sim,
                                       String nickname,
                                       Integer productSetId)
    {
        ProvisioningApi proxy = createProxy(ProvisioningApi.class);

        try
        {
            ActivationRequestBody body = new ActivationRequestBody();
            body.subscriberNickname = nickname;
            body.productSetId       = productSetId;

            ApiResults<ActivationResponse> res = proxy.activateSubscriber(sim, body);

            return res.decode(s_objectMapper, new TypeReference<ActivationResponse>()
            {
            });
        }
        catch (Throwable t)
        {
            decodeFailure("activate", t);
            return null;
        }
    }

    //--//

    private <P> P createProxy(Class<P> cls)
    {
        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setAddress(m_baseAddress);
        bean.setResourceClass(cls);
        bean.setProvider(s_jsonProvider);

        // Handle authentication.
        Map<String, String> map = Maps.newHashMap();
        map.put("Authorization", String.format("Bearer %s", m_apiSecret));
        bean.setHeaders(map);

        Client client = bean.createWithValues();

        HTTPConduit conduit = WebClient.getConfig(client)
                                       .getHttpConduit();
        conduit.getClient()
               .setReceiveTimeout(m_timeout);

        return cls.cast(client);
    }

    private void decodeFailure(String ctx,
                               Throwable t)
    {
        try
        {
            BadRequestException t2 = Reflection.as(t, BadRequestException.class);
            if (t2 != null)
            {
                Response response = t2.getResponse();

                InputStream inputStream = Reflection.as(response.getEntity(), InputStream.class);
                if (inputStream != null)
                {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    IOUtils.copyLarge(inputStream, outputStream);
                    String val = outputStream.toString();
                    LoggerInstance.error("%s: %s", ctx, val);
                    return;
                }
            }
        }
        catch (Throwable t3)
        {
            // Ignore failure during failure decoding...
        }

        LoggerInstance.debug("%s: %s", ctx, t);
    }
}
