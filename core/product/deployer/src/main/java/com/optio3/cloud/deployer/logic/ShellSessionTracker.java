/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.collection.TokenTracker;
import com.optio3.util.IdGenerator;

public class ShellSessionTracker extends TokenTracker<ShellToken, ShellSession>
{
    public ShellSessionTracker()
    {
        super(10, TimeUnit.MINUTES);
    }

    @Override
    protected ShellToken newToken(ShellSession payload)
    {
        ShellToken token = new ShellToken();
        token.id = IdGenerator.newGuid();

        return token;
    }

    @Override
    protected String getTokenId(ShellToken token)
    {
        return token != null ? token.id : null;
    }

    @Override
    protected void releasePayload(ShellSession payload)
    {
        payload.stop();
    }
}