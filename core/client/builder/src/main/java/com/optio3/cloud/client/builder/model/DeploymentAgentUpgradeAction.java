/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 *
 * Optio3 Builder APIs
 * APIs and Definitions for the Optio3 Builder product.
 *
 * OpenAPI spec version: 1.0.0
 *
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */

package com.optio3.cloud.client.builder.model;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum DeploymentAgentUpgradeAction
{
    StartAgentsWithReleaseCandidate(String.valueOf("StartAgentsWithReleaseCandidate")),
    StartAgentsWithRelease(String.valueOf("StartAgentsWithRelease")),
    StartOperationalAgentsWithReleaseCandidate(String.valueOf("StartOperationalAgentsWithReleaseCandidate")),
    StartOperationalAgentsWithRelease(String.valueOf("StartOperationalAgentsWithRelease")),
    ActivateAgentsWithReleaseCandidate(String.valueOf("ActivateAgentsWithReleaseCandidate")),
    ActivateAgentsWithRelease(String.valueOf("ActivateAgentsWithRelease")),
    TerminateNonActiveAgents(String.valueOf("TerminateNonActiveAgents")),
    DeleteTerminatedAgents(String.valueOf("DeleteTerminatedAgents"));

    private String value;

    DeploymentAgentUpgradeAction(String v)
    {
        value = v;
    }

    public String value()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return String.valueOf(value);
    }

    @JsonCreator
    public static DeploymentAgentUpgradeAction fromValue(String v)
    {
        for (DeploymentAgentUpgradeAction b : DeploymentAgentUpgradeAction.values())
        {
            if (String.valueOf(b.value).equals(v))
                return b;
        }

        throw new IllegalArgumentException(String.format("'%s' is not one of the values accepted for %s class: %s", v, DeploymentAgentUpgradeAction.class.getSimpleName(), Arrays.toString(DeploymentAgentUpgradeAction.values())));
    }
}
