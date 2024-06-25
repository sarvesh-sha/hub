/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.proxy;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.DeploymentAgentApiVersion;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.model.PullProgress;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.model.VolumeStatus;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.util.function.AsyncFunctionWithException;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableProxy
public interface DeployerDockerApi
{
    /**
     * Lists images on the host.
     *
     * @param all If set, it will list all images, not just the tagged ones
     *
     * @return
     *
     * @throws Exception
     */
    CompletableFuture<List<ImageStatus>> listImages(boolean all) throws
                                                                 Exception;

    /**
     * Starts pulling an image from a Docker Registry.
     *
     * @param image            The identity of the image
     * @param registryAccount  The account to use to connect to the registry
     * @param registryPassword The password to use to connect to the registry
     *
     * @return Image Pull identifier
     *
     * @throws Exception
     */
    CompletableFuture<ImagePullToken> startPullImage(String image,
                                                     String registryAccount,
                                                     String registryPassword) throws
                                                                              Exception;

    /**
     * For an ongoing Image Pull, this checks the download progress.
     *
     * @param token  Image Pull identifier
     * @param output Callback to receive the output from Docker
     *
     * @return 1 if the pull was completed, 0 if the pull is ongoing, -1 if the token does not match any session
     *
     * @throws Exception
     */
    CompletableFuture<Integer> checkPullImageProgress(ImagePullToken token,
                                                      FunctionWithException<String, CompletableFuture<Void>> output) throws
                                                                                                                     Exception;

    /**
     * For an ongoing Image Pull, this checks the download progress.
     *
     * @param token  Image Pull identifier
     * @param offset Offline in the progress log
     * @param output Callback to receive the output from Docker
     *
     * @return 1 if the pull was completed, 0 if the pull is ongoing, -1 if the token does not match any session
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.ImagePullProgressEx)
    CompletableFuture<Integer> checkPullImageProgressEx(ImagePullToken token,
                                                        int offset,
                                                        FunctionWithException<ShellOutput, CompletableFuture<Void>> output) throws
                                                                                                                            Exception;

    /**
     * For an ongoing Image Pull, this checks the download progress.
     *
     * @param token  Image Pull identifier
     * @param offset Start of log reporting
     * @param count  Number of lines of log to report
     *
     * @return Details of the pull process
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.ImagePullProgressEx2)
    CompletableFuture<PullProgress> checkPullImageProgressEx2(ImagePullToken token,
                                                              int offset,
                                                              int count) throws
                                                                         Exception;

    /**
     * Closes an Image Pull session
     *
     * @param token Image Pull identifier
     *
     * @return
     *
     * @throws Exception
     */
    CompletableFuture<Void> closePullImage(ImagePullToken token) throws
                                                                 Exception;

    /**
     * Tries to delete an image
     *
     * @param image Image to delete
     * @param force If true, the deletion will be forced
     *
     * @return true if image was deleted
     *
     * @throws Exception
     */
    CompletableFuture<Boolean> removeImage(String image,
                                           boolean force) throws
                                                          Exception;

    /**
     * Tags an image.
     *
     * @param sourceImage Source image
     * @param targetImage Target image to tag
     *
     * @return true if image was tagged
     *
     * @throws Exception
     */
    CompletableFuture<Boolean> tagImage(String sourceImage,
                                        String targetImage) throws
                                                            Exception;

    //--//

    CompletableFuture<List<VolumeStatus>> listVolumes(Map<String, String> filterByLabels) throws
                                                                                          Exception;

    CompletableFuture<VolumeStatus> inspectVolume(String volumeName) throws
                                                                     Exception;

    CompletableFuture<String> createVolume(String volumeName,
                                           Map<String, String> labels,
                                           String driver,
                                           Map<String, String> driverOpts) throws
                                                                           Exception;

    CompletableFuture<Void> deleteVolume(String volumeName,
                                         boolean force) throws
                                                        Exception;

    CompletableFuture<String> backupVolume(String volumeName,
                                           String relativePath,
                                           int timeout,
                                           TimeUnit unit) throws
                                                          Exception;

    CompletableFuture<Void> restoreVolume(String volumeName,
                                          String relativePath,
                                          String inputFile,
                                          int timeout,
                                          TimeUnit unit) throws
                                                         Exception;

    //--//

    CompletableFuture<List<ContainerStatus>> listContainers(Map<String, String> filterByLabels) throws
                                                                                                Exception;

    CompletableFuture<ContainerStatus> inspectContainer(String containerId) throws
                                                                            Exception;

    CompletableFuture<String> createContainer(String name,
                                              ContainerConfiguration config) throws
                                                                             Exception;

    CompletableFuture<Void> startContainer(String containerId) throws
                                                               Exception;

    CompletableFuture<Void> fetchOutput(String containerId,
                                        int limit,
                                        int batchSize,
                                        boolean removeEscapeSequences,
                                        ZonedDateTime lastOutput,
                                        AsyncFunctionWithException<List<LogEntry>, Void> progressCallback) throws
                                                                                                           Exception;

    CompletableFuture<Boolean> signalContainer(String containerId,
                                               String signal) throws
                                                              Exception;

    CompletableFuture<Integer> stopContainer(String containerId) throws
                                                                 Exception;

    CompletableFuture<Integer> getContainerExitCode(String containerId) throws
                                                                        Exception;

    CompletableFuture<ContainerStatus> removeContainer(String containerId) throws
                                                                           Exception;

    //--//

    CompletableFuture<String> readContainerFileSystemToTar(String containerId,
                                                           String containerPath,
                                                           boolean compress) throws
                                                                             Exception;

    CompletableFuture<Boolean> writeContainerFileSystemFromTar(String containerId,
                                                               String containerPath,
                                                               String inputFile,
                                                               boolean decompress) throws
                                                                                   Exception;

    //--//

    /**
     * Creates a batch of docker operations.
     *
     * @param request The set of operations to perform, in sequential order.
     *
     * @return Batch identifier
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.DockerBatch)
    CompletableFuture<BatchToken> prepareBatch(DockerBatch.Request request) throws
                                                                            Exception;

    /**
     * Starts a batch of docker operations.
     *
     * @param token Batch identifier
     *
     * @return progess report
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.DockerBatch)
    CompletableFuture<DockerBatch.Report> startBatch(BatchToken token) throws
                                                                       Exception;

    /**
     * For an ongoing Batch, this checks the progress.
     *
     * @param token  Batch identifier
     * @param output Callback to receive the output from Docker
     *
     * @return progess report
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.DockerBatch)
    CompletableFuture<DockerBatch.Report> checkBatchProgress(BatchToken token,
                                                             int offset,
                                                             FunctionWithException<ShellOutput, CompletableFuture<Void>> output) throws
                                                                                                                                 Exception;

    /**
     * Closes a batch session
     *
     * @param token Batch identifier
     *
     * @return
     *
     * @throws Exception
     */
    @DeploymentAgentApiVersion(DeploymentAgentFeature.DockerBatch)
    CompletableFuture<Void> closeBatch(BatchToken token) throws
                                                         Exception;
}
