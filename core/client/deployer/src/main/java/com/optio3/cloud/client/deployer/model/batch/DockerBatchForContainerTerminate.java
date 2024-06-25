/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model.batch;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DockerBatchForContainerTerminate")
public class DockerBatchForContainerTerminate extends DockerBatch
{
    @JsonTypeName("DockerBatchForContainerTerminate_Result") // No underscore in model name, due to Swagger issues.
    public static class Result extends BaseResult
    {
    }

    //--//

    public String dockerId;

    @Override
    public String toString()
    {
        return "DockerBatchForContainerTerminate{" + "dockerId='" + dockerId + '\'' + '}';
    }
}
