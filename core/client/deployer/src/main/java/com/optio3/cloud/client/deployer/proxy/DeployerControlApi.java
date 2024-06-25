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

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.client.deployer.model.DeploymentAgentApiVersion;
import com.optio3.cloud.client.deployer.model.DeploymentAgentFeature;
import com.optio3.cloud.client.deployer.model.FileStatus;
import com.optio3.logging.LoggerConfiguration;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableProxy
public interface DeployerControlApi
{
    /**
     * Sets the system time.
     *
     * @param time
     *
     * @return The new system time
     *
     * @throws Exception
     */
    CompletableFuture<ZonedDateTime> setSystemTime(ZonedDateTime time) throws
                                                                       Exception;

    CompletableFuture<List<String>> dumpThreads(boolean includeMemInfo) throws
                                                                        Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.ShutdownOnLowVoltage)
    CompletableFuture<String> setShutdownConfiguration(DeployerShutdownConfiguration cfg) throws
                                                                                          Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.FlushAndRestart)
    CompletableFuture<Void> flushHeartbeat() throws
                                             Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.FlushAndRestart)
    CompletableFuture<Void> restart() throws
                                      Exception;

    //--//

    CompletableFuture<List<LoggerConfiguration>> getLoggers() throws
                                                              Exception;

    CompletableFuture<LoggerConfiguration> configLogger(LoggerConfiguration cfg) throws
                                                                                 Exception;

    CompletableFuture<Map<String, String>> getBootParameters() throws
                                                               Exception;

    CompletableFuture<String> setBootParameter(String key,
                                               String value) throws
                                                             Exception;

    //--//

    CompletableFuture<List<FileStatus>> listFiles(String path) throws
                                                               Exception;

    CompletableFuture<Boolean> deleteFile(String path) throws
                                                       Exception;

    CompletableFuture<Boolean> deleteDirectory(String path) throws
                                                            Exception;

    CompletableFuture<Long> getFileSize(String path) throws
                                                     Exception;

    CompletableFuture<Void> writeFile(String path,
                                      FunctionWithException<Integer, CompletableFuture<byte[]>> getChunk) throws
                                                                                                          Exception;

    CompletableFuture<Integer> readFile(String path,
                                        FunctionWithException<byte[], CompletableFuture<Boolean>> putChunk) throws
                                                                                                            Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.CopyFileChunk)
    CompletableFuture<Void> writeFileChunk(String path,
                                           long offset,
                                           byte[] chunk) throws
                                                         Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.CopyFileChunk)
    CompletableFuture<byte[]> readFileChunk(String path,
                                            long offset,
                                            int length) throws
                                                        Exception;

    @DeploymentAgentApiVersion(DeploymentAgentFeature.CopyFileChunk)
    CompletableFuture<Void> closeFile(String path) throws
                                                   Exception;

    /**
     * Copies a file from the deployer's file area to the host.
     *
     * @param user       The user to log in
     * @param privateKey The private key for the user
     * @param publicKey  The public key for the user
     * @param passphrase The passphrase for the private key
     * @param path       The file in the deployer's file area
     * @param hostPath   The destination file in the host
     *
     * @return Exit code of copy command
     *
     * @throws Exception
     */
    CompletableFuture<Integer> copyFileToHost(String user,
                                              byte[] privateKey,
                                              byte[] publicKey,
                                              byte[] passphrase,
                                              String path,
                                              String hostPath) throws
                                                               Exception;

    /**
     * Copies a file from the host to the deployer's file area.
     *
     * @param user       The user to log in
     * @param privateKey The private key for the user
     * @param publicKey  The public key for the user
     * @param passphrase The passphrase for the private key
     * @param path       The file in the deployer's file area
     * @param hostPath   The source file in the host
     *
     * @return Exit code of copy command
     *
     * @throws Exception
     */
    CompletableFuture<Integer> copyFileFromHost(String user,
                                                byte[] privateKey,
                                                byte[] publicKey,
                                                byte[] passphrase,
                                                String path,
                                                String hostPath) throws
                                                                 Exception;
}
