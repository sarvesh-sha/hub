/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.proxy;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableProxy
public interface DeployerShellApi
{
    CompletableFuture<List<ShellToken>> listSessions() throws
                                                       Exception;

    /**
     * Starts a new shell.
     *
     * @param commandLine The command line to execute in the shell
     * @param timeout     Maximum execution time, before the shell is killed
     * @param unit        Units for the timeout
     *
     * @return Shell identifier
     *
     * @throws Exception
     */
    CompletableFuture<ShellToken> start(String commandLine,
                                        int timeout,
                                        TimeUnit unit) throws
                                                       Exception;

    /**
     * Starts a new shell using ssh.
     *
     * @param server     The server to connect to
     * @param user       The user to log in
     * @param privateKey The private key for the user
     * @param publicKey  The public key for the user
     * @param passphrase The passphrase for the private key
     * @param timeout    Maximum execution time, before the shell is killed
     * @param unit       Units for the timeout
     *
     * @return Shell identifier
     *
     * @throws Exception
     */
    CompletableFuture<ShellToken> startWithSsh(String server,
                                               String user,
                                               byte[] privateKey,
                                               byte[] publicKey,
                                               byte[] passphrase,
                                               int timeout,
                                               TimeUnit unit) throws
                                                              Exception;

    /**
     * Writes to stdin of the shell.
     *
     * @param token      Shell identifier
     * @param writeStdIn Payload to write
     *
     * @return Exit code from the shell, -1 if still running, -2 if token invalid, or -3 if shell was killed.
     *
     * @throws Exception
     */
    CompletableFuture<Integer> write(ShellToken token,
                                     byte[] writeStdIn) throws
                                                        Exception;

    /**
     * Reads stdout and/or stderr of the shell.
     *
     * @param token           Shell identifier
     * @param maxOutputLength Maximum number of bytes to report through the callback (4096 if zero)
     * @param readStdOut      Callback invoked with bytes from stdout
     * @param readStdErr      Callback invoked with bytes from stderr
     *
     * @return Exit code from the shell, -1 if still running, -2 if token invalid, or -3 if shell was killed.
     *
     * @throws Exception
     */
    CompletableFuture<Integer> poll(ShellToken token,
                                    int maxOutputLength,
                                    FunctionWithException<byte[], CompletableFuture<Void>> readStdOut,
                                    FunctionWithException<byte[], CompletableFuture<Void>> readStdErr) throws
                                                                                                       Exception;

    /**
     * Stops a shell.
     *
     * @param token Shell identifier
     *
     * @return Exit code from the shell, -2 if token invalid, or -3 if shell was killed.
     *
     * @throws Exception
     */
    CompletableFuture<Integer> stop(ShellToken token) throws
                                                      Exception;
}
