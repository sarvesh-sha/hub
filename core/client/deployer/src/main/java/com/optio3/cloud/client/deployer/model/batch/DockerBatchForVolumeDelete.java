/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model.batch;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DockerBatchForVolumeDelete")
public class DockerBatchForVolumeDelete extends DockerBatch
{
    @JsonTypeName("DockerBatchForVolumeDelete_Result") // No underscore in model name, due to Swagger issues.
    public static class Result extends BaseResult
    {
    }

    //--//

    public String  volumeName;
    public boolean force;

    @Override
    public String toString()
    {
        return "DockerBatchForVolumeDelete{" + "volumeName='" + volumeName + '\'' + '}';
    }
}
