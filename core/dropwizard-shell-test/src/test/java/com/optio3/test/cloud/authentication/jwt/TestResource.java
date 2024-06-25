/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.authentication.jwt;

import java.util.Set;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3NoAuthenticationNeeded;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;

@Path("security")
public class TestResource // Used by CookieAuthenticationTest
{
    @GET
    @Path("login")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> login(@Context ContainerRequestContext requestContext)
    {
        CookiePrincipal principal = CookiePrincipal.getFromContext(requestContext);

        return principal.getRoles();
    }

    @GET
    @Path("logout")
    public void logout(@Context ContainerRequestContext requestContext)
    {
        CookiePrincipal principal = CookiePrincipal.getFromContext(requestContext);
        principal.markAsLoggedOut();
        ;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public @NotNull CookiePrincipal getPrincipal(@Context ContainerRequestContext requestContext)
    {
        CookiePrincipal principal = CookiePrincipal.getFromContext(requestContext);

        return principal;
    }

    @GET
    @Path("restricted")
    @RolesAllowed("admin")
    public String getRestrictedResource()
    {
        return "SuperSecretStuff";
    }

    @GET
    @Path("public")
    @Optio3NoAuthenticationNeeded
    public String getPublicResource()
    {
        return "PublicStuff";
    }
}
