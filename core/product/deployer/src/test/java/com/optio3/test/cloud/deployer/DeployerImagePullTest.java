/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.client.deployer.model.ImagePullToken;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class DeployerImagePullTest extends Optio3Test
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
        DeployerDockerApi proxy = client.createProxy(deployerId, null, DeployerDockerApi.class, 100, TimeUnit.SECONDS);

        List<ImageStatus> images = getAndUnwrapException(proxy.listImages(true));
        for (ImageStatus image : images)
            System.out.printf("########### Image: %s%n", ObjectMappers.SkipNulls.writeValueAsString(image));
    }

    @Ignore("Manually enable to test, since it  modifies the local image repository.")
    @Test
    @TestOrder(20)
    public void testPull() throws
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

        ImagePullToken token = getAndUnwrapException(proxy.startPullImage("alpine:3.4", null, null));

        System.out.println("########### Started PullImage...");

        int exitCode = -1;

        for (int i = 0; i < 20; i++)
        {
            final int pass = i;

            exitCode = getAndUnwrapException(proxy.checkPullImageProgress(token, (line) ->
            {
                System.out.printf("########### Output as pass %d: %s%n", pass, line);

                return AsyncRuntime.NullResult;
            }));

            Thread.sleep(500);

            if (exitCode != 0)
            {
                break;
            }
        }

        assertEquals(1, exitCode);

        assertTrue(rule.deployer.getApplication()
                                .getImagePullSession(token) != null);
        getAndUnwrapException(proxy.closePullImage(token));
        assertTrue(rule.deployer.getApplication()
                                .getImagePullSession(token) == null);

        try (DockerHelper helper = new DockerHelper(null))
        {
            helper.removeImage(new DockerImageIdentifier("alpine:3.4"), false);
        }
    }

    @Test
    @TestOrder(30)
    public void testNonExistingImage() throws
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

        ImagePullToken token = getAndUnwrapException(proxy.startPullImage("foo:bar", null, null));

        System.out.println("########### Started PullImage...");

        int exitCode = -1;

        for (int i = 0; i < 20; i++)
        {
            final int pass = i;

            exitCode = getAndUnwrapException(proxy.checkPullImageProgress(token, (line) ->
            {
                System.out.printf("########### Output as pass %d: %s%n", pass, line);

                return AsyncRuntime.NullResult;
            }));

            Thread.sleep(500);

            if (exitCode != 0)
            {
                break;
            }
        }

        assertEquals(-1, exitCode);

        assertTrue(rule.deployer.getApplication()
                                .getImagePullSession(token) != null);
        getAndUnwrapException(proxy.closePullImage(token));
        assertTrue(rule.deployer.getApplication()
                                .getImagePullSession(token) == null);
    }
}
