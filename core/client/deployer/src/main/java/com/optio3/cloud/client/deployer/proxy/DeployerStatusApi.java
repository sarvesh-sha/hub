/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.deployer.model.DeploymentAgentStatus;
import com.optio3.cloud.client.deployer.model.DockerCompressedLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerLayerChunk;
import com.optio3.cloud.client.deployer.model.DockerLayerChunks;
import com.optio3.cloud.client.deployer.model.DockerLayerChunksWithMetadata;
import com.optio3.cloud.client.deployer.model.DockerLayerDescription;
import com.optio3.cloud.client.deployer.model.DockerPackageDescription;
import com.optio3.util.Encryption;

@Optio3RemotableProxy
public interface DeployerStatusApi
{
    CompletableFuture<Void> checkin(DeploymentAgentStatus status) throws
                                                                  Exception;

    CompletableFuture<String> getDockerImageSha(String img) throws
                                                            Exception;

    CompletableFuture<DockerPackageDescription> describeDockerTarball(String img) throws
                                                                                  Exception;

    CompletableFuture<DockerLayerDescription> describeDockerLayerTarball(String img,
                                                                         String layerId) throws
                                                                                         Exception;

    CompletableFuture<DockerCompressedLayerDescription> describeDockerCompressedLayerTarball(String img,
                                                                                             String layerId) throws
                                                                                                             Exception;

    // TODO: UPGRADE PATCH: Legacy API.
    CompletableFuture<List<DockerLayerChunk>> describeDockerLayerChunks(String img,
                                                                        String layerId) throws
                                                                                        Exception;

    CompletableFuture<DockerLayerChunks> describeDockerLayerCompactChunks(String img,
                                                                          String layerId) throws
                                                                                          Exception;

    CompletableFuture<DockerLayerChunksWithMetadata> describeDockerLayerChunksWithMetadata(String img,
                                                                                           String layerId) throws
                                                                                                           Exception;

    CompletableFuture<DockerLayerChunks> describeDockerLayerSubchunks(String img,
                                                                      String layerId,
                                                                      Encryption.Sha1Hash hash) throws
                                                                                                Exception;

    // TODO: UPGRADE PATCH: Legacy API.
    CompletableFuture<byte[]> fetchLayerTarball(String img,
                                                String layerId,
                                                long offset,
                                                int size) throws
                                                          Exception;

    CompletableFuture<byte[]> fetchCompressedLayerTarball(String img,
                                                          String layerId,
                                                          long offset,
                                                          int size) throws
                                                                    Exception;

    // TODO: UPGRADE PATCH: Legacy API.
    CompletableFuture<byte[]> fetchLayerChunk(String img,
                                              String hash,
                                              long offset,
                                              int size) throws
                                                        Exception;

    CompletableFuture<byte[]> fetchLayerChunk(String img,
                                              Encryption.Sha1Hash hash,
                                              long offset,
                                              int size) throws
                                                        Exception;

    CompletableFuture<List<byte[]>> fetchLayerChunks(String img,
                                                     List<Encryption.Sha1Hash> hashes) throws
                                                                                       Exception;
}
