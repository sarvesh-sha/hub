/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.LogEntry;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.logging.Severity;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExternalResource;

@Ignore("Manually enable to test, since it requires access privileges to Docker.")
public class DeployerDockerTest extends Optio3Test
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
    public void testList() throws
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
        DeployerDockerApi proxy = client.createProxy(deployerId, null, DeployerDockerApi.class, 100, TimeUnit.SECONDS);

        List<ContainerStatus> containers = getAndUnwrapException(proxy.listContainers(null));
        for (ContainerStatus status : containers)
        {
            System.out.printf("Container: %s%n", status.id);
        }
    }

    @Test
    @TestOrder(20)
    public void testGetLog() throws
                             Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        System.out.println("########### Waiting for Deployer...");
        String deployerId = rule.deployer.getEndpointId();

        boolean found = getAndUnwrapException(client.waitForDestination(deployerId, 2, TimeUnit.SECONDS));
        assertTrue(found);

        System.out.println("########### Found Deployer...");
        DeployerDockerApi proxy = client.createProxy(deployerId, null, DeployerDockerApi.class, 100, TimeUnit.SECONDS);

        List<ContainerStatus> containers = getAndUnwrapException(proxy.listContainers(null));
        for (ContainerStatus status : containers)
        {
            List<LogEntry> logs = Lists.newArrayList();

            getAndUnwrapException(proxy.fetchOutput(status.id, 0, 50, true, null, (lst) ->
            {
                logs.addAll(lst);

                return AsyncRuntime.NullResult;
            }));

            System.out.printf("Container: %s %d logs%n", status.id, logs.size());

            for (LogEntry en : logs)
            {
                System.out.printf("   FD:%d %s %s%n", en.fd, en.timestamp, en.line);
            }
        }
    }
}
