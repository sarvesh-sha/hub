/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.authentication.jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import com.google.common.collect.Sets;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalRoleResolver;
import io.jsonwebtoken.lang.Strings;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class CookieAuthenticationTest
{
    @ClassRule
    public static final TestApplicationRule applicationRule = new TestApplicationRule(new CookiePrincipalRoleResolver()
    {
        @Override
        public boolean stillValid(@NotNull CookiePrincipal principal)
        {
            return true;
        }

        @Override
        public boolean hasRole(@NotNull CookiePrincipal principal,
                               String role)
        {
            switch (principal.getName())
            {
                case "adminUser":
                    return "admin".equals(role);

                case "normalUser":
                    return false;
            }

            return false;
        }

        @Override
        public Set<String> getRoles(@NotNull CookiePrincipal principal)
        {
            switch (principal.getName())
            {
                case "adminUser":
                    return Sets.newHashSet("admin");
            }

            return Collections.emptySet();
        }

        @Override
        public boolean authenticate(@NotNull CookiePrincipal principal,
                                    String password)
        {
            switch (principal.getName())
            {
                case "adminUser":
                    return "adminPwd".equals(password);

                case "normalUser":
                    return "normalPwd".equals(password);
            }

            return false;
        }
    });

    private WebTarget getTargetNoCred()
    {
        return getTargetInner(null, null);
    }

    private WebTarget getTargetAdmin()
    {
        return getTargetInner("adminUser", "adminPwd");
    }

    private WebTarget getTargetNormal()
    {
        return getTargetInner("normalUser", "normalPwd");
    }

    private WebTarget getTargetInner(String userName,
                                     String password)
    {
        Client client = ClientBuilder.newClient();

        if (userName != null)
        {
            HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(userName, password);
            client.register(feature);
        }

        return client.target(applicationRule.baseUri())
                     .path("security");
    }

    @Test
    public void testUnauthorized()
    {
        //calls to APIs with the @Auth annotation without prior authentication should result in HTTP 401
        Response response = getTargetNoCred().path("login")
                                             .request(MediaType.APPLICATION_JSON)
                                             .get();
        Assert.assertEquals(412, response.getStatus());
    }

    @Test
    public void testCookieSetting()
    {
        // First request with Admin credentials.
        Response response = getTargetAdmin().path("login")
                                            .request(MediaType.APPLICATION_JSON)
                                            .get();
        Assert.assertEquals(200, response.getStatus());

        //check that a session cookie has been set
        NewCookie cookie1 = response.getCookies()
                                    .get("sessionToken");
        Assert.assertNotNull(cookie1);
        Assert.assertTrue(Strings.hasText(cookie1.getValue()));
        Assert.assertTrue(cookie1.isHttpOnly());

        //a GET with this cookie should return the Principal and *not* refresh the cookie
        response = getTargetNoCred().path("login")
                                    .request(MediaType.APPLICATION_JSON)
                                    .cookie(cookie1)
                                    .get();
        Assert.assertEquals(200, response.getStatus());
        NewCookie cookie2 = response.getCookies()
                                    .get("sessionToken");
        Assert.assertNull(cookie2);
    }

    @Test
    public void testPublicEndpoint()
    {
        //public endpoints (i.e. not with @Auth, @RolesAllowed etc.) should not modify the cookie
        Response response = getTargetAdmin().request(MediaType.APPLICATION_JSON)
                                            .get();
        NewCookie cookie = response.getCookies()
                                   .get("sessionToken");

        //request made to public methods should not refresh the cookie
        response = getTargetNoCred().path("public")
                                    .request(MediaType.APPLICATION_JSON)
                                    .cookie(cookie)
                                    .get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertNull(response.getCookies()
                                  .get("sessionToken"));
    }

    @Test
    public void testRememberMe()
    {
        //a volatile principal should set a volatile cookie
        Response response = getTargetAdmin().request(MediaType.APPLICATION_JSON)
                                            .get();
        NewCookie cookie = response.getCookies()
                                   .get("sessionToken");
        Assert.assertNotNull(cookie);
        Assert.assertTrue(cookie.getMaxAge() > 86400);
    }

    @Test
    public void testRoles()
    {
        WebTarget restrictedTarget = getTargetNoCred().path("restricted");
        //try to access the resource without cookie (-> 401 UNAUTHORIZED)
        Response response = restrictedTarget.request()
                                            .get();
        Assert.assertEquals(412, response.getStatus());

        //set a principal without the admin role (-> 403 FORBIDDEN)
        response = getTargetNormal().request(MediaType.APPLICATION_JSON)
                                    .get();
        NewCookie cookie = response.getCookies()
                                   .get("sessionToken");
        Assert.assertNotNull(cookie);
        response = restrictedTarget.request()
                                   .cookie(cookie)
                                   .get();
        Assert.assertEquals(403, response.getStatus());

        //set a principal with the admin role (-> 200 OK)
        response = getTargetAdmin().request(MediaType.APPLICATION_JSON)
                                   .get();
        cookie   = response.getCookies()
                           .get("sessionToken");
        Assert.assertNotNull(cookie);
        response = restrictedTarget.request()
                                   .cookie(cookie)
                                   .get();
        Assert.assertEquals(200, response.getStatus());
    }

    @Test
    public void testDeleteCookie()
    {
        Response response = getTargetAdmin().request(MediaType.APPLICATION_JSON)
                                            .get();
        NewCookie cookie = response.getCookies()
                                   .get("sessionToken");
        Assert.assertNotNull(cookie);

        // Calling the logout endpoint should produce a cookie with empty content and a past expiration date
        response = getTargetNoCred().path("logout")
                                    .request()
                                    .cookie(cookie)
                                    .get();
        Assert.assertEquals(204, response.getStatus());
        cookie = response.getCookies()
                         .get("sessionToken");
        Assert.assertNotNull(cookie);
        Assert.assertEquals("void", cookie.getValue());
        Assert.assertEquals(Date.from(Instant.EPOCH), cookie.getExpiry());
    }
}
