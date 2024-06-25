/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.remoting;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.infra.docker.model.Image;
import com.optio3.util.function.AsyncFunctionWithException;

@Optio3RemotableProxy
public interface RemoteDockerApi
{
    CompletableFuture<String> createVolume(String volumeName,
                                           Map<String, String> labels,
                                           String driver,
                                           Map<String, String> driverOpts) throws
                                                                           Exception;

    CompletableFuture<Void> deleteVolume(String volumeName,
                                         boolean force) throws
                                                        Exception;

    CompletableFuture<String> getVolumeMountPoint(String volumeName) throws
                                                                     Exception;

    //--//

    CompletableFuture<String> createContainer(String containerName,
                                              ContainerConfiguration config) throws
                                                                             Exception;

    CompletableFuture<Void> startContainer(String containerId) throws
                                                               Exception;

    CompletableFuture<ZonedDateTime> fetchOutput(String containerId,
                                                 ZonedDateTime lastOutput,
                                                 AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                           Exception;

    CompletableFuture<Integer> stopContainer(String containerId) throws
                                                                 Exception;

    CompletableFuture<Integer> getExitCode(String containerId) throws
                                                               Exception;

    //--//

    CompletableFuture<Void> deleteImage(DockerImageIdentifier imageParsed,
                                        boolean force) throws
                                                       Exception;

    CompletableFuture<Image> pullImage(DockerImageIdentifier imageParsed,
                                       UserInfo credentials,
                                       AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                 Exception;

    CompletableFuture<String> buildImage(Path sourceDirectory,
                                         String dockerFile,
                                         Map<String, String> buildargs,
                                         String registryAddress,
                                         UserInfo registryUser,
                                         Map<String, String> labels,
                                         ZonedDateTime overrideTime,
                                         AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                   Exception;

    CompletableFuture<Image> inspectImage(DockerImageIdentifier image) throws
                                                                       Exception;

    CompletableFuture<DockerImageArchitecture> inspectImageArchitecture(DockerImageIdentifier image) throws
                                                                                                     Exception;

    CompletableFuture<Boolean> tagImage(DockerImageIdentifier sourceParsed,
                                        DockerImageIdentifier targetParsed) throws
                                                                            Exception;

    CompletableFuture<String> pushImage(DockerImageIdentifier imageParsed,
                                        UserInfo credentials,
                                        AsyncFunctionWithException<DockerHelper.LogEntry, Void> progressCallback) throws
                                                                                                                  Exception;
}
