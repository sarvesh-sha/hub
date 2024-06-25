/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.collection.TokenTracker;
import com.optio3.util.IdGenerator;

public class ImagePullSessionTracker extends TokenTracker<ImagePullToken, ImagePullSession>
{
    public ImagePullSessionTracker()
    {
        super(1, TimeUnit.DAYS);
    }

    @Override
    protected ImagePullToken newToken(ImagePullSession payload)
    {
        ImagePullToken token = new ImagePullToken();
        token.id = IdGenerator.newGuid();

        return token;
    }

    @Override
    protected String getTokenId(ImagePullToken token)
    {
        return token != null ? token.id : null;
    }

    @Override
    protected void releasePayload(ImagePullSession payload)
    {
        payload.stop();
    }
}