/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

public enum DeploymentAgentFeature
{
    ShutdownOnLowVoltage,
    ImagePullProgressEx,
    ImagePullProgressEx2,
    FlushAndRestart,
    DockerBatch,
    DockerBatchForContainerLaunch,
    DockerBatchForContainerTerminate,
    DockerBatchForVolumeCreate,
    DockerBatchForVolumeDelete,
    CopyFileChunk
}
