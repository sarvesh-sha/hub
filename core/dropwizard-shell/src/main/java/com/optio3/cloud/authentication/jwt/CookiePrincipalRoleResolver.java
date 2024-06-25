/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.authentication.jwt;

import java.util.Set;

import javax.validation.constraints.NotNull;

public interface CookiePrincipalRoleResolver
{
    boolean stillValid(@NotNull CookiePrincipal principal);

    boolean authenticate(@NotNull CookiePrincipal principal,
                         String password);

    Set<String> getRoles(@NotNull CookiePrincipal principal);

    boolean hasRole(@NotNull CookiePrincipal principal,
                    String role);
}
