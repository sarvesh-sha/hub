/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.gateway;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.cli.ServerCommand;

public class HubTestApplicationRule extends TestApplicationWithDbRule<HubApplication, HubConfiguration>
{
    public HubTestApplicationRule(ConsumerWithException<HubConfiguration> configurationCallback,
                                  ConsumerWithException<HubApplication> applicationCallback)
    {
        super(HubApplication.class, "hub-test.yml", configurationCallback, applicationCallback, ServerCommand::new);

        dropDatabaseOnExit(false);
    }

    public String getEndpointId() throws
                                  Exception
    {
        RpcClient clientDep = getAndUnwrapException(getApplication().getRpcClient(10, TimeUnit.SECONDS));
        return clientDep.getEndpointId();
    }
}
