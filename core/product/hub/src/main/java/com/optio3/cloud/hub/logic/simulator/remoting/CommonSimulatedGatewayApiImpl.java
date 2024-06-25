/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.remoting;

import com.optio3.cloud.client.gateway.orchestration.state.GatewayOperationTracker;
import com.optio3.cloud.hub.logic.simulator.SimulatedGateway;

public abstract class CommonSimulatedGatewayApiImpl
{
    @Optio3SimulatedGateway
    private SimulatedGateway m_app;

    protected SimulatedGateway getApplication()
    {
        return m_app;
    }

    protected GatewayOperationTracker getTracker()
    {
        return getApplication().getOperationTracker();
    }
}
