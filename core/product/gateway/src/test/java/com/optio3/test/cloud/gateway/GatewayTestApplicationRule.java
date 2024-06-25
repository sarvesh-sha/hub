/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.gateway;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.gateway.GatewayCommand;
import com.optio3.cloud.gateway.GatewayConfiguration;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.test.dropwizard.TestApplicationRule;
import com.optio3.util.function.ConsumerWithException;

public class GatewayTestApplicationRule extends TestApplicationRule<GatewayApplication, GatewayConfiguration>
{
    public GatewayTestApplicationRule(ConsumerWithException<GatewayConfiguration> configurationCallback,
                                      ConsumerWithException<GatewayApplication> applicationCallback)
    {
        super(GatewayApplication.class, "gateway-test.yml", configurationCallback, applicationCallback, GatewayCommand::new);
    }

    public String getEndpointId() throws
                                  Exception
    {
        RpcClient clientDep = getAndUnwrapException(getApplication().getRpcClient(10, TimeUnit.SECONDS));
        return clientDep.getEndpointId();
    }
}
