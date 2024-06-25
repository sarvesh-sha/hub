/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.remoting.impl;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.cloud.client.deployer.proxy.DeployerShellApi;
import com.optio3.cloud.deployer.logic.ShellSession;
import com.optio3.util.function.FunctionWithException;

@Optio3RemotableEndpoint(itf = DeployerShellApi.class)
public class DeployerShellApiImpl extends CommonDeployerApiImpl implements DeployerShellApi
{
    @Override
    public CompletableFuture<List<ShellToken>> listSessions()
    {
        return wrapAsync(getApplication().listShellSessions());
    }

    @Override
    public CompletableFuture<ShellToken> start(String commandLine,
                                               int timeout,
                                               TimeUnit unit) throws
                                                              Exception
    {
        ShellSession session = new ShellSession(commandLine, timeout, unit);
        ShellToken   token   = getApplication().registerShellSession(session);

        return wrapAsync(token);
    }

    @Override
    public CompletableFuture<ShellToken> startWithSsh(String server,
                                                      String user,
                                                      byte[] privateKey,
                                                      byte[] publicKey,
                                                      byte[] passphrase,
                                                      int timeout,
                                                      TimeUnit unit) throws
                                                                     Exception
    {
        ShellSession session = new ShellSession(server, user, privateKey, publicKey, passphrase, timeout, unit);
        ShellToken   token   = getApplication().registerShellSession(session);

        return wrapAsync(token);
    }

    @Override
    public CompletableFuture<Integer> write(ShellToken token,
                                            byte[] writeStdIn) throws
                                                               Exception
    {
        ShellSession session = getApplication().getShellSession(token);
        if (session == null)
        {
            return wrapAsync(-2);
        }

        session.writeToStdin(writeStdIn);

        return wrapAsync(session.getExitCode());
    }

    @Override
    public CompletableFuture<Integer> poll(ShellToken token,
                                           int maxOutputLength,
                                           FunctionWithException<byte[], CompletableFuture<Void>> readStdOut,
                                           FunctionWithException<byte[], CompletableFuture<Void>> readStdErr) throws
                                                                                                              Exception
    {
        ShellSession session = getApplication().getShellSession(token);
        if (session == null)
        {
            return wrapAsync(-2);
        }

        if (maxOutputLength == 0)
        {
            maxOutputLength = 4096;
        }

        if (readStdOut != null)
        {
            int got = 0;

            while (true)
            {
                byte[] b = session.readFromStdout(1024);
                if (b.length == 0)
                {
                    break;
                }

                await(readStdOut.apply(b));

                got += b.length;
                if (got > maxOutputLength)
                {
                    break;
                }
            }
        }

        if (readStdErr != null)
        {
            int got = 0;

            while (true)
            {
                byte[] b = session.readFromStderr(1024);
                if (b.length == 0)
                {
                    break;
                }

                await(readStdErr.apply(b));

                got += b.length;
                if (got > maxOutputLength)
                {
                    break;
                }
            }
        }

        return wrapAsync(session.getExitCode());
    }

    @Override
    public CompletableFuture<Integer> stop(ShellToken token)
    {
        ShellSession session = getApplication().getShellSession(token);
        if (session == null)
        {
            return wrapAsync(-2);
        }

        session.stop();

        getApplication().unregisterShellSession(token);

        return wrapAsync(session.getExitCode());
    }
}
