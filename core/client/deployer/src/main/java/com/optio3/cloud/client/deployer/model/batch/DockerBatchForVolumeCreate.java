/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model.batch;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("DockerBatchForVolumeCreate")
public class DockerBatchForVolumeCreate extends DockerBatch
{
    @JsonTypeName("DockerBatchForVolumeCreate_Result") // No underscore in model name, due to Swagger issues.
    public static class Result extends BaseResult
    {
        public String id;
    }

    //--//

    public String              volumeName;
    public Map<String, String> labels;
    public String              driver;
    public Map<String, String> driverOpts;

    //--//

    @Override
    public String toString()
    {
        return "DockerBatchForVolumeCreate{" + "volumeName='" + volumeName + '\'' + '}';
    }
}
