/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.security.Principal;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.SecurityContext;

class CookieSecurityContext implements SecurityContext
{
    private final CookiePrincipal m_principal;
    private final boolean         m_secure;

    CookieSecurityContext(@NotNull CookiePrincipal principal,
                          ContainerRequestContext context)
    {
        this.m_principal = principal;
        this.m_secure = context.getSecurityContext()
                               .isSecure();
    }

    @Override
    public Principal getUserPrincipal()
    {
        return m_principal;
    }

    @Override
    public boolean isUserInRole(String role)
    {
        return m_principal != null && m_principal.isInRole(role);
    }

    @Override
    public boolean isSecure()
    {
        return m_secure;
    }

    @Override
    public String getAuthenticationScheme()
    {
        return "JWT_COOKIE";
    }
}
