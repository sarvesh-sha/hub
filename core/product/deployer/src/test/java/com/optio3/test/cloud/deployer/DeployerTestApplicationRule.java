/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.util.Exceptions.getAndUnwrapException;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.deployer.DeployerCommand;
import com.optio3.cloud.deployer.DeployerConfiguration;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.test.dropwizard.TestApplicationRule;
import com.optio3.util.function.ConsumerWithException;

public class DeployerTestApplicationRule extends TestApplicationRule<DeployerApplication, DeployerConfiguration>
{
    public DeployerTestApplicationRule(ConsumerWithException<DeployerConfiguration> configurationCallback,
                                       ConsumerWithException<DeployerApplication> applicationCallback)
    {
        super(DeployerApplication.class, "deployer-test.yml", configurationCallback, applicationCallback, DeployerCommand::new);
    }

    public String getEndpointId() throws
                                  Exception
    {
        RpcClient clientDep = getAndUnwrapException(getApplication().getRpcClient(10, TimeUnit.SECONDS));
        return clientDep.getEndpointId();
    }
}
