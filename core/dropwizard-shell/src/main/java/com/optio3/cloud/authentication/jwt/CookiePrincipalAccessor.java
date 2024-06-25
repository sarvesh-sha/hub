/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import javax.validation.constraints.NotNull;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;

public class CookiePrincipalAccessor
{
    @Context
    private ContainerRequestContext m_requestContext;

    public static @NotNull CookiePrincipal get(CookiePrincipalAccessor accessor)
    {
        if (accessor == null)
        {
            return CookiePrincipal.createEmpty();
        }

        //
        // We have to delay extracting the principal from the context,
        // because Injection happens before the authentication filter has a chance to process the request.
        //
        return CookiePrincipal.getFromContext(accessor.m_requestContext);
    }
}
