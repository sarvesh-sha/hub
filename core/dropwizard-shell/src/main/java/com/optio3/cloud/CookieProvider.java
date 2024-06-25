/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import java.time.Instant;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.google.common.collect.Maps;
import org.apache.cxf.jaxrs.client.Client;

public class CookieProvider
{
    private static class PerAuthority
    {
        private final ConcurrentMap<String, NewCookie> m_cookies = Maps.newConcurrentMap();

        void setCookies(Client client)
        {
            Instant now = Instant.now();

            // First, reset all the cookies.
            client.reset();

            // Then add the non-expired ones.
            Iterator<NewCookie> it = m_cookies.values()
                                              .iterator();
            while (it.hasNext())
            {
                NewCookie cookie = it.next();

                Instant expire = getExpirationDate(cookie);
                if (now.isAfter(expire))
                {
                    it.remove();
                }
                else
                {
                    client.cookie(cookie);
                }
            }
        }

        void updateCookies(Client client)
        {
            Instant now = Instant.now();

            Response               response = client.getResponse();
            Map<String, NewCookie> cookies  = response.getCookies();
            for (NewCookie cookie : cookies.values())
            {
                Instant expire = getExpirationDate(cookie);
                if (now.isAfter(expire))
                {
                    m_cookies.remove(cookie.getName());
                }
                else
                {
                    m_cookies.put(cookie.getName(), cookie);
                }
            }
        }

        void addCookie(NewCookie cookie)
        {
            m_cookies.put(cookie.getName(), cookie);
        }
    }

    private final ConcurrentMap<String, CookieProvider.PerAuthority> m_authorities = Maps.newConcurrentMap();

    public void setCookies(Client client)
    {
        getAuthority(client).setCookies(client);
    }

    public void updateCookies(Client client)
    {
        getAuthority(client).updateCookies(client);
    }

    public void addCookie(Client client,
                          NewCookie cookie)
    {
        getAuthority(client).addCookie(cookie);
    }

    //--//

    private static Instant getExpirationDate(NewCookie cookie)
    {
        Date expire = cookie.getExpiry();
        if (expire != null)
        {
            return Instant.ofEpochMilli(expire.getTime());
        }

        int maxAge = cookie.getMaxAge();
        if (maxAge > 0)
        {
            return Instant.now()
                          .plusSeconds(maxAge);
        }

        return Instant.MAX;
    }

    //--//

    private CookieProvider.PerAuthority getAuthority(Client client)
    {
        String key = client.getBaseURI()
                           .getAuthority();

        return m_authorities.computeIfAbsent(key, (key2) -> new PerAuthority());
    }
}
