/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.client.deployer.model.ShellToken;
import com.optio3.cloud.client.deployer.proxy.DeployerShellApi;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.logging.Severity;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class DeployerShellTest extends Optio3Test
{
    public static class MultiTestRule extends ExternalResource
    {
        public BuilderTestApplicationRule  builder;
        public DeployerTestApplicationRule deployer;

        public MultiTestRule()
        {
            builder = BuilderTestApplicationRule.newInstance((application) ->
                                                             {
                                                                 // Nothing to add.
                                                             });

            deployer = new DeployerTestApplicationRule((configuration) ->
                                                       {
                                                           configuration.connectionUrl = (builder.baseUri()
                                                                                                 .toString() + "api/v1/message-bus").replace("http:", "ws:");
                                                       }, (application) ->
                                                       {
                                                           // Nothing to add.
                                                       });
        }

        @Override
        protected void before() throws
                                Throwable
        {
            builder.invokeBefore();
            deployer.invokeBefore();
        }

        @Override
        protected void after()
        {
            deployer.invokeAfter();
            builder.invokeAfter();
        }
    }

    @ClassRule
    public static final MultiTestRule rule = new MultiTestRule();

    @Test
    @TestOrder(10)
    public void testLs() throws
                         Exception
    {
        MessageBusBroker.LoggerInstance.disable(Severity.Debug);
        MessageBusBroker.LoggerInstance.disable(Severity.DebugVerbose);

        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerShellApi proxy = client.createProxy(deployerId, null, DeployerShellApi.class, 100, TimeUnit.SECONDS);

        ShellToken token = getAndUnwrapException(proxy.start("ls -lR", 10, TimeUnit.MINUTES));

        System.out.println("########### Started Shell...");

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, false);
            System.out.printf("########### ExitCode: %d%n", exitCode);

            if (exitCode >= 0)
            {
                break;
            }
        }

        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) != null);
        getAndUnwrapException(proxy.stop(token));
        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) == null);
    }

    @Test
    @TestOrder(20)
    public void testBash() throws
                           Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerShellApi proxy = client.createProxy(deployerId, null, DeployerShellApi.class, 100, TimeUnit.SECONDS);

        ShellToken token = getAndUnwrapException(proxy.start("bash -i", 10, TimeUnit.MINUTES));

        System.out.println("########### Started Shell...");

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, false);
            assertEquals(-1, exitCode);

            Thread.sleep(100);
        }

        sendStdin(proxy, token, "ls -lR\n", 0);

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, false);
            assertEquals(-1, exitCode);

            Thread.sleep(100);
        }

        sendStdin(proxy, token, "exit\n", 0);

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, false);
            if (exitCode >= 0)
            {
                assertEquals(0, exitCode);
                break;
            }

            Thread.sleep(100);
        }

        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) != null);
        getAndUnwrapException(proxy.stop(token));
        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) == null);
    }

    @Test
    @TestOrder(25)
    public void testBashInteractive() throws
                                      Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerShellApi proxy = client.createProxy(deployerId, null, DeployerShellApi.class, 100, TimeUnit.SECONDS);

        ShellToken token = getAndUnwrapException(proxy.start("bash -i", 10, TimeUnit.MINUTES));

        System.out.println("########### Started Shell...");

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, true);
            assertEquals(-1, exitCode);

            Thread.sleep(100);
        }

        sendStdin(proxy, token, "lA" + ((char) 8) + "s -lR\n", 300);

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, true);
            assertEquals(-1, exitCode);

            Thread.sleep(100);
        }

        sendStdin(proxy, token, "exit\n", 300);

        for (int i = 0; i < 10; i++)
        {
            int exitCode = pollShell(proxy, token, true);
            if (exitCode >= 0)
            {
                assertEquals(0, exitCode);
                break;
            }

            Thread.sleep(100);
        }

        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) != null);
        getAndUnwrapException(proxy.stop(token));
        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) == null);
    }

    @Test
    @TestOrder(30)
    public void testBashTimeout() throws
                                  Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerShellApi proxy = client.createProxy(deployerId, null, DeployerShellApi.class, 100, TimeUnit.SECONDS);

        ShellToken token = getAndUnwrapException(proxy.start("bash -i", 100, TimeUnit.MILLISECONDS));

        System.out.println("########### Started Shell...");

        Thread.sleep(200);

        int exitCode = pollShell(proxy, token, false);
        assertEquals(-3, exitCode);

        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) != null);
        getAndUnwrapException(proxy.stop(token));
        assertTrue(rule.deployer.getApplication()
                                .getShellSession(token) == null);
    }

    private void sendStdin(DeployerShellApi proxy,
                           ShellToken token,
                           String text,
                           int delay) throws
                                      Exception
    {
        for (byte c : text.getBytes())
        {
            getAndUnwrapException(proxy.write(token, new byte[] { c }));
            pollShell(proxy, token, true);

            if (delay > 0)
            {
                Thread.sleep(delay);
            }
        }
    }

    private int pollShell(DeployerShellApi proxy,
                          ShellToken token,
                          boolean raw) throws
                                       Exception
    {
        return getAndUnwrapException(proxy.poll(token, 0, (stdout) ->
        {
            if (raw)
            {
                System.out.write(stdout);
            }
            else
            {
                for (String line : new String(stdout).split("\n"))
                    System.out.printf("########### StdOut: %s%n", line);
            }

            return AsyncRuntime.NullResult;
        }, (stderr) ->
                                                {
                                                    if (raw)
                                                    {
                                                        System.err.write(stderr);
                                                    }
                                                    else
                                                    {
                                                        for (String line : new String(stderr).split("\n"))
                                                            System.out.printf("########### StdErr: %s%n", line);
                                                    }

                                                    return AsyncRuntime.NullResult;
                                                }));
    }
}
