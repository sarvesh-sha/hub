/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbData;

public interface ChannelSecurityPolicy
{
    boolean canJoin(@NotNull CookiePrincipal principal);

    boolean canListMembers(@NotNull CookiePrincipal principal);

    boolean canSend(@NotNull CookiePrincipal principal,
                    MbData data);

    boolean canSendBroadcast(@NotNull CookiePrincipal principal,
                             MbData data);
}
