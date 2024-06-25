/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.provisioner;

import com.optio3.cloud.provisioner.ProvisionerApplication;
import com.optio3.cloud.provisioner.ProvisionerConfiguration;
import com.optio3.test.dropwizard.TestApplicationRule;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.cli.ServerCommand;

public class ProvisionerTestApplicationRule extends TestApplicationRule<ProvisionerApplication, ProvisionerConfiguration>
{
    public ProvisionerTestApplicationRule(ConsumerWithException<ProvisionerConfiguration> configurationCallback,
                                          ConsumerWithException<ProvisionerApplication> applicationCallback)
    {
        super(ProvisionerApplication.class, "provisioner-test.yml", configurationCallback, applicationCallback, ServerCommand::new);
    }
}
