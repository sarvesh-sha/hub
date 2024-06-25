/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.waypoint;

import com.optio3.cloud.waypoint.WaypointApplication;
import com.optio3.cloud.waypoint.WaypointConfiguration;
import com.optio3.test.dropwizard.TestApplicationRule;
import com.optio3.util.function.ConsumerWithException;
import io.dropwizard.cli.ServerCommand;

public class WaypointTestApplicationRule extends TestApplicationRule<WaypointApplication, WaypointConfiguration>
{
    public WaypointTestApplicationRule(ConsumerWithException<WaypointConfiguration> configurationCallback,
                                       ConsumerWithException<WaypointApplication> applicationCallback)
    {
        super(WaypointApplication.class, "waypoint-test.yml", configurationCallback, applicationCallback, ServerCommand::new);
    }
}
