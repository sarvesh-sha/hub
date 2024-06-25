/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

public enum DeploymentAgentUpgradeAction
{
    // @formatter:off
    StartAgentsWithReleaseCandidate           (true , false, false, false, false, true , false),
    StartAgentsWithRelease                    (true , false, false, false, false, false, true ),
    StartOperationalAgentsWithReleaseCandidate(true , false, false, false, true , true , false),
    StartOperationalAgentsWithRelease         (true , false, false, false, true , false, true ),
    ActivateAgentsWithReleaseCandidate        (false, true , false, false, false, true , false),
    ActivateAgentsWithRelease                 (false, true , false, false, false, false, true ),
    TerminateNonActiveAgents                  (false, false, true , false, false, false, false),
    DeleteTerminatedAgents                    (false, false, false, true , false, false, false);
    // @formatter:on

    public final boolean start;
    public final boolean activate;
    public final boolean terminate;
    public final boolean delete;
    public final boolean onlyOperational;
    public final boolean usingReleaseCandidate;
    public final boolean usingRelease;

    DeploymentAgentUpgradeAction(boolean start,
                                 boolean activate,
                                 boolean terminate,
                                 boolean delete,
                                 boolean onlyOperational,
                                 boolean usingReleaseCandidate,
                                 boolean usingRelease)
    {
        this.start = start;
        this.activate = activate;
        this.terminate = terminate;
        this.delete = delete;
        this.onlyOperational = onlyOperational;
        this.usingReleaseCandidate = usingReleaseCandidate;
        this.usingRelease = usingRelease;
    }
}
