/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.infra.directory.ApiInfo;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.infra.godaddy.api.DefaultApi;
import com.optio3.infra.godaddy.model.DNSRecord;
import com.optio3.infra.godaddy.model.RecordType;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.URLConnectionHTTPConduit;

public class GoDaddyHelper implements AutoCloseable
{
    public static final  String API_CREDENTIALS_SITE = "godaddy.com";
    private static final String API_URL              = "https://api.godaddy.com";

    //
    // GoDaddy doesn't like when JSON objects contain extra null values.
    // To deal with that, we have to install a custom serializer provider that ignores nulls.
    //
    private final static JacksonJsonProvider       s_jsonProvider = new JacksonJsonProvider(ObjectMappers.SkipNulls);
    private static final LinkedList<GoDaddyHelper> s_cache        = new LinkedList<>();

    private final MonotonousTime m_expiration;
    private final String         m_targetDomain;
    private final String         m_baseAddress;
    private final String         m_apiKey;
    private final String         m_apiSecret;

    public GoDaddyHelper(CredentialDirectory credDir,
                         String domain)
    {
        ApiInfo ai = credDir.findFirstApiCredential(API_CREDENTIALS_SITE, domain);

        m_expiration   = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
        m_targetDomain = domain;
        m_baseAddress  = API_URL;
        m_apiKey       = ai.accessKey;
        m_apiSecret    = ai.secretKey;
    }

    @Override
    public void close()
    {
        if (!TimeUtils.isTimeoutExpired(m_expiration) && m_targetDomain != null)
        {
            synchronized (s_cache)
            {
                s_cache.add(this);
            }
        }
    }

    public static GoDaddyHelper buildCachedWithDirectoryLookup(CredentialDirectory credDir,
                                                               String accountDomain)
    {
        synchronized (s_cache)
        {
            for (Iterator<GoDaddyHelper> it = s_cache.iterator(); it.hasNext(); )
            {
                var helper = it.next();

                if (Objects.equals(helper.getAccountDomain(), accountDomain))
                {
                    it.remove();

                    if (!TimeUtils.isTimeoutExpired(helper.m_expiration))
                    {
                        return helper;
                    }
                }
            }
        }

        return new GoDaddyHelper(credDir, accountDomain);
    }

    public String getAccountDomain()
    {
        return m_targetDomain;
    }

    //--//

    public List<DNSRecord> listDomain(String domain)
    {
        return listDomain((proxy, offset, limit) -> proxy.recordGet(domain, offset, limit));
    }

    public List<DNSRecord> listDomain(String domain,
                                      RecordType type)
    {
        return listDomain((proxy, offset, limit) -> proxy.recordGetWithType(domain, type, offset, limit));
    }

    public List<DNSRecord> listDomain(String domain,
                                      RecordType type,
                                      String hostname)
    {
        return listDomain((proxy, offset, limit) -> proxy.recordGetWithTypeName(domain, type, hostname, offset, limit));
    }

    public DNSRecord queryDomain(String domain,
                                 RecordType type,
                                 String key)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        // Return the first match
        return CollectionUtils.firstElement(proxy.recordGetWithTypeName(domain, type, key, 0, 1));
    }

    //--//

    public void refreshRecord(String domain,
                              String hostname,
                              String oldIp,
                              String newIp,
                              int timeToLive)
    {
        refreshDns(domain, RecordType.A, hostname, oldIp, newIp, timeToLive);
    }

    public void removeRecord(String domain,
                             String hostname)
    {
        removeRecord(domain, RecordType.A, hostname);
    }

    //--//

    public void updateOrCreateTxtRecord(String domain,
                                        String key,
                                        String value,
                                        int timeToLive)
    {
        updateOrCreateRecord(domain, RecordType.TXT, key, (rec) ->
        {
            rec.data = value;
            rec.ttl  = fixupTTL(timeToLive);
        });
    }

    public void removeTxtRecord(String domain,
                                String key)
    {
        removeRecord(domain, RecordType.TXT, key);
    }

    //--//

    public void updateOrCreateCNameRecord(String domain,
                                          String key,
                                          String value,
                                          int timeToLive)
    {
        updateOrCreateRecord(domain, RecordType.CNAME, key, (rec) ->
        {
            rec.data = value;
            rec.ttl  = fixupTTL(timeToLive);
        });
    }

    public void removeCNameRecord(String domain,
                                  String key)
    {
        removeRecord(domain, RecordType.CNAME, key);
    }

    //--//

    private void refreshDns(String domain,
                            RecordType type,
                            String key,
                            String oldIp,
                            String newId,
                            int timeToLive)
    {
        List<DNSRecord> lst   = listDomain(domain, type, key);
        boolean         isNew = lst.isEmpty();

        String valBefore = ObjectMappers.toJsonNoThrow(null, lst);

        if (oldIp != null)
        {
            lst.removeIf((r) -> StringUtils.equals(r.data, oldIp));
        }

        if (newId != null)
        {
            DNSRecord rec = CollectionUtils.findFirst(lst, (r) -> StringUtils.equals(r.data, newId));
            if (rec != null)
            {
                rec.ttl = fixupTTL(timeToLive);
            }
            else
            {
                rec      = new DNSRecord();
                rec.type = type;
                rec.name = key;
                rec.data = newId;
                rec.ttl  = fixupTTL(timeToLive);
                lst.add(rec);
            }
        }

        String  valAfter     = ObjectMappers.toJsonNoThrow(null, lst);
        boolean shouldRemove = lst.isEmpty();

        // Only update if different.
        if (!StringUtils.equals(valBefore, valAfter))
        {
            if (shouldRemove)
            {
                removeRecord(domain, type, key);
            }
            else
            {
                DefaultApi proxy = createProxy(DefaultApi.class);

                if (isNew)
                {
                    proxy.recordAdd(domain, lst);
                }
                else
                {
                    proxy.recordReplaceTypeName(domain, type, key, lst);
                }
            }
        }
    }

    private void updateOrCreateRecord(String domain,
                                      RecordType type,
                                      String key,
                                      Consumer<DNSRecord> callback)
    {
        DNSRecord rec = queryDomain(domain, type, key);
        if (rec != null)
        {
            //
            // Found an existing entry.
            // Just update it.
            //
            String valBefore = ObjectMappers.toJsonNoThrow(null, rec);
            callback.accept(rec);
            String valAfter = ObjectMappers.toJsonNoThrow(null, rec);

            // Only update if different.
            if (!StringUtils.equals(valBefore, valAfter))
            {
                DefaultApi proxy = createProxy(DefaultApi.class);
                proxy.recordReplaceTypeName(domain, type, rec.name, Lists.newArrayList(rec));
            }
        }
        else
        {
            DNSRecord newRec = new DNSRecord();
            newRec.name = key;
            newRec.type = type;
            callback.accept(newRec);

            DefaultApi proxy = createProxy(DefaultApi.class);
            proxy.recordAdd(domain, Lists.newArrayList(newRec));
        }
    }

    private void removeRecord(String domain,
                              RecordType type,
                              String key)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);
        proxy.recordDeleteTypeName(domain, type, key);
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
        map.put("Authorization", String.format("sso-key %s:%s", m_apiKey, m_apiSecret));
        bean.setHeaders(map);

        Client client = bean.createWithValues();

        //
        // GoDaddy uses a PATCH request method.
        // The Java system classes don't support it.
        // We have to turn on this Apache workaround to override the system classes.
        //
        WebClient.getConfig(client)
                 .getRequestContext()
                 .put(URLConnectionHTTPConduit.HTTPURL_CONNECTION_METHOD_REFLECTION, true);

        return cls.cast(client);
    }

    @FunctionalInterface
    public interface FetchDomain
    {
        List<DNSRecord> accept(DefaultApi proxy,
                               int offset,
                               int limit);
    }

    private List<DNSRecord> listDomain(FetchDomain callback)
    {
        DefaultApi proxy = createProxy(DefaultApi.class);

        Set<DNSRecord> records = Sets.newHashSet();
        final int      limit   = 50;

        //
        // GoDaddy API is sort of broken.
        // It returns the same results for different offsets.
        // We have to put all the results in a set to filter the duplicates out.
        //
        for (int pageOffset = 0; ; pageOffset++)
        {
            List<DNSRecord> partial = callback.accept(proxy, pageOffset, limit);
            if (partial.isEmpty())
            {
                break;
            }

            records.addAll(partial);
        }

        ArrayList<DNSRecord> res = Lists.newArrayList(records);
        res.sort((a, b) -> StringUtils.compareIgnoreCase(a.name, b.name));
        return res;
    }

    private int fixupTTL(int timeToLive)
    {
        return Math.max(timeToLive, 30 * 60); // Minimum is half an hour.
    }
}
