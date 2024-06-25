/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model.batch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = DockerBatchForContainerLaunch.class),
                @JsonSubTypes.Type(value = DockerBatchForContainerTerminate.class),
                @JsonSubTypes.Type(value = DockerBatchForVolumeCreate.class),
                @JsonSubTypes.Type(value = DockerBatchForVolumeDelete.class) })
public abstract class DockerBatch
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type_result")
    @JsonSubTypes({ @JsonSubTypes.Type(value = DockerBatchForContainerLaunch.Result.class),
                    @JsonSubTypes.Type(value = DockerBatchForContainerTerminate.Result.class),
                    @JsonSubTypes.Type(value = DockerBatchForVolumeCreate.Result.class),
                    @JsonSubTypes.Type(value = DockerBatchForVolumeDelete.Result.class) })
    public static abstract class BaseResult
    {
        public String  failure;
        public boolean cancelled;
    }

    public static class Report
    {
        public List<BaseResult> results;
    }

    public static class Request
    {
        public List<DockerBatch> items;
    }
}