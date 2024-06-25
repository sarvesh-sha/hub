/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.collection.TokenTracker;
import com.optio3.util.IdGenerator;

public class BatchSessionTracker extends TokenTracker<BatchToken, BatchSession>
{
    public BatchSessionTracker()
    {
        super(1, TimeUnit.DAYS);
    }

    @Override
    protected BatchToken newToken(BatchSession payload)
    {
        BatchToken token = new BatchToken();
        token.id = IdGenerator.newGuid();

        return token;
    }

    @Override
    protected String getTokenId(BatchToken token)
    {
        return token != null ? token.id : null;
    }

    @Override
    protected void releasePayload(BatchSession payload)
    {
        payload.stop();
    }
}