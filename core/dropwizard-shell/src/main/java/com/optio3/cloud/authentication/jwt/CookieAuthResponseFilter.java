/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

import com.google.common.primitives.Ints;

class CookieAuthResponseFilter implements ContainerResponseFilter
{
    private final String   m_cookieName;
    private final boolean  m_secure;
    private final boolean  m_httpOnly;
    private final Key      m_signingKey;
    private final Duration m_volatileValidity;
    private final Duration m_persistentValidity;
    private final Duration m_refreshThreshold;

    public CookieAuthResponseFilter(String cookieName,
                                    boolean secure,
                                    boolean httpOnly,
                                    Key signingKey,
                                    Duration volatileValidity,
                                    Duration persistentValidity)
    {
        m_cookieName = cookieName;
        m_secure = secure;
        m_httpOnly = httpOnly;
        m_signingKey = signingKey;
        m_volatileValidity = volatileValidity;
        m_persistentValidity = persistentValidity;

        // Refresh if there are less than 30 minutes of validity left in the token.
        m_refreshThreshold = Duration.ofSeconds(30 * 60);
    }

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response)
    {
        CookiePrincipal principal = CookiePrincipal.getFromContext(request, true);

        final MultivaluedMap<String, Object> responseHeaders = response.getHeaders();

        boolean requestHasCookie = request.getCookies()
                                          .containsKey(m_cookieName);

        if (principal.wasLoggedOut())
        {
            //
            // The principal has been remove during the response, delete the cookie on the client.
            //
            if (requestHasCookie)
            {
                responseHeaders.add("Set-Cookie", generateCookie(principal));
            }
        }
        else if (principal.isLoggedIn())
        {
            if (!requestHasCookie || principal.shouldRefreshToken(m_refreshThreshold))
            {
                principal.setExpirationDate(principal.isPersistent() ? m_persistentValidity : m_volatileValidity);

                responseHeaders.add("Set-Cookie", generateCookie(principal));
            }
        }
    }

    NewCookie generateCookie(@NotNull CookiePrincipal principal)
    {
        boolean isLoggedIn = principal.isLoggedIn();

        String token  = isLoggedIn ? principal.toJwt(m_signingKey) : "void";
        int    maxAge = -1;
        Date   expiry = null;

        if (!isLoggedIn)
        {
            expiry = new Date(0);
        }
        else if (principal.isPersistent())
        {
            Date expiration = principal.getExpirationDate();
            if (expiration != null)
            {
                Duration left = Duration.between(Instant.now(), expiration.toInstant());
                maxAge = Ints.checkedCast(Math.max(-1, left.getSeconds()));
            }
        }

        return new NewCookie(m_cookieName, token, "/", null, 1, null, maxAge, expiry, m_secure, m_httpOnly);
    }
}
