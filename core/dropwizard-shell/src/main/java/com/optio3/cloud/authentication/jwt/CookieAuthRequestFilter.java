/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.Priority;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;

import com.google.common.io.BaseEncoding;
import com.optio3.cloud.exception.NotAuthenticatedException;
import com.optio3.util.Exceptions;
import io.dropwizard.auth.AuthFilter;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.basic.BasicCredentials;
import org.apache.commons.lang3.StringUtils;

@Priority(Priorities.AUTHENTICATION)
public class CookieAuthRequestFilter extends AuthFilter<Object, CookiePrincipal>
{
    static final String CHALLENGE_FORMAT = "%s realm=\"%s\"";

    private final String m_cookieName;

    private CookieAuthRequestFilter(String cookieName)
    {
        m_cookieName = cookieName;
    }

    @Override
    public void filter(ContainerRequestContext context)
    {
        //
        // CORS requires that the browser make a call to the resource with OPTIONS method.
        // But the browser does that without cookies or anything like that, which would cause an authentication failures.
        //
        // So we disallow authentication on OPTIONS methods (see CookieAuthDynamicFeature) and let the OPTIONS requests pass through. 
        //
        if ("OPTIONS".equalsIgnoreCase(context.getMethod()))
        {
            return;
        }

        try
        {
            if (checkHeaders(context))
            {
                return;
            }
        }
        catch (AuthenticationException e)
        {
            throw new InternalServerErrorException(e);
        }

        throw Exceptions.newGenericException(NotAuthenticatedException.class, CHALLENGE_FORMAT, prefix, realm);
    }

    public void process(ContainerRequestContext requestContext,
                        Optional<String> usernameOpt,
                        Optional<String> passwordOpt) throws
                                                      AuthenticationException
    {
        if (usernameOpt.isPresent() && passwordOpt.isPresent())
        {
            String username = StringUtils.trim(usernameOpt.get());
            String password = StringUtils.trim(passwordOpt.get());

            Optional<CookiePrincipal> subject = authenticator.authenticate(new BasicCredentials(username, password));
            if (subject.isPresent())
            {
                subject.get()
                       .addInContext(requestContext);
            }
        }

        checkHeaders(requestContext);
    }

    private boolean checkHeaders(ContainerRequestContext context) throws
                                                                  AuthenticationException
    {
        Cookie cookie = context.getCookies()
                               .get(m_cookieName);
        if (cookie != null)
        {
            String accessToken = cookie.getValue();
            if (accessToken != null && accessToken.length() > 0)
            {
                final Optional<CookiePrincipal> subject = authenticator.authenticate(accessToken);
                if (subject.isPresent())
                {
                    subject.get()
                           .addInContext(context);
                    return true;
                }
            }
        }

        final BasicCredentials credentials = getCredentials(context.getHeaders()
                                                                   .getFirst(HttpHeaders.AUTHORIZATION));
        if (credentials != null)
        {
            final Optional<CookiePrincipal> subject = authenticator.authenticate(credentials);
            if (subject.isPresent())
            {
                subject.get()
                       .addInContext(context);
                return true;
            }
        }

        return false;
    }

    /**
     * Parses a Base64-encoded value of the `Authorization` header in the form
     * of `Basic dXNlcm5hbWU6cGFzc3dvcmQ=`.
     *
     * @param header the value of the `Authorization` header
     *
     * @return a username and a password as {@link BasicCredentials}
     */
    @Nullable
    private BasicCredentials getCredentials(String header)
    {
        if (header == null)
        {
            return null;
        }

        final int space = header.indexOf(' ');
        if (space <= 0)
        {
            return null;
        }

        final String method = header.substring(0, space);
        if (!prefix.equalsIgnoreCase(method))
        {
            return null;
        }

        final String decoded;
        try
        {
            decoded = new String(BaseEncoding.base64()
                                             .decode(header.substring(space + 1)), StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e)
        {
            logger.warn("Error decoding credentials", e);
            return null;
        }

        // Decoded credentials is 'username:password'
        final int i = decoded.indexOf(':');
        if (i <= 0)
        {
            return null;
        }

        final String username = decoded.substring(0, i);
        final String password = decoded.substring(i + 1);
        return new BasicCredentials(username, password);
    }

    //--//

    public static class Builder extends AuthFilterBuilder<Object, CookiePrincipal, CookieAuthRequestFilter>
    {
        private String m_cookieName;

        public Builder setCookieName(String cookieName)
        {
            m_cookieName = cookieName;

            return this;
        }

        @Override
        protected CookieAuthRequestFilter newInstance()
        {
            return new CookieAuthRequestFilter(Objects.requireNonNull(m_cookieName, "cookieName is not set"));
        }
    }
}
