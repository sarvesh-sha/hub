/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model.batch;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;

@JsonTypeName("DockerBatchForContainerLaunch")
public class DockerBatchForContainerLaunch extends DockerBatch
{
    @JsonTypeName("DockerBatchForContainerLaunch_Result") // No underscore in model name, due to Swagger issues.
    public static class Result extends BaseResult
    {
        public String dockerId;
    }

    //--//

    public static class FileSystemInit
    {
        public String  containerPath;
        public byte[]  input;
        public boolean decompress;
    }

    public String                 name;
    public ContainerConfiguration config;
    public List<FileSystemInit>   configurationFiles = Lists.newArrayList();

    @Override
    public String toString()
    {
        return "DockerBatchForContainerLaunch{" + "name='" + name + '\'' + ", image='" + config.image + '\'' + '}';
    }
}
