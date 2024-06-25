/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.remoting.impl;

import javax.inject.Inject;

import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.gateway.GatewayConfiguration;
import com.optio3.cloud.gateway.logic.ProberOperationTracker;

public abstract class CommonGatewayApiImpl
{
    @Inject
    private GatewayApplication m_app;

    protected GatewayApplication getApplication()
    {
        return m_app;
    }

    protected GatewayConfiguration getConfiguration()
    {
        return m_app.getServiceNonNull(GatewayConfiguration.class);
    }

    protected GatewayOperationTracker getTracker()
    {
        return getApplication().getOperationTracker();
    }

    protected ProberOperationTracker getTrackerForProber()
    {
        return getApplication().getOperationTrackerForProber();
    }
}
