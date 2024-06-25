/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.deployer;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.logging.Severity;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class DeployerBuilderCommunicationTest extends Optio3Test
{
    @Optio3RemotableProxy
    public interface TestApi
    {
        CompletableFuture<Integer> addNumbers(int a,
                                              int b);

        CompletableFuture<Integer> addNumbersWithCallback(int a,
                                                          int b,
                                                          Function<Integer, CompletableFuture<Integer>> callback) throws
                                                                                                                  Exception;
    }

    @Optio3RemotableEndpoint(itf = TestApi.class)
    public final static class TestApiImpl implements TestApi
    {
        int state;

        @Override
        public CompletableFuture<Integer> addNumbers(int a,
                                                     int b)
        {
            return wrapAsync(a + b + state);
        }

        @Override
        public CompletableFuture<Integer> addNumbersWithCallback(int a,
                                                                 int b,
                                                                 Function<Integer, CompletableFuture<Integer>> callback) throws
                                                                                                                         Exception
        {
            int b2 = await(callback.apply(b));

            return wrapAsync(a + b2);
        }
    }

    public static class MultiTestRule extends ExternalResource
    {
        public BuilderTestApplicationRule  builder;
        public DeployerTestApplicationRule deployer;

        public MultiTestRule()
        {
            builder = BuilderTestApplicationRule.newInstance((application) ->
                                                             {
                                                                 application.registerRemotableEndpoint(TestApiImpl.class);
                                                             });

            deployer = new DeployerTestApplicationRule((configuration) ->
                                                       {
                                                           configuration.connectionUrl = (builder.baseUri()
                                                                                                 .toString() + "api/v1/message-bus").replace("http:", "ws:");
                                                       }, (application) ->
                                                       {
                                                           application.registerRemotableEndpoint(TestApiImpl.class);
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
    public void testDeployerToBuilder() throws
                                        Exception
    {
        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        DeployerApplication deployer = rule.deployer.getApplication();
        RpcClient           client   = getAndUnwrapException(deployer.getRpcClient(10, TimeUnit.SECONDS));

        TestApi proxy = client.createProxy(WellKnownDestination.Service.getId(), null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(30, sum);
    }

    @Test
    @TestOrder(20)
    public void testBuilderToDeployer() throws
                                        Exception
    {
        BuilderApplication builder = rule.builder.getApplication();
        RpcClient          client  = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        String deployerId = rule.deployer.getEndpointId();

        TestApi proxy = client.createProxy(deployerId, null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(10 + 20, sum);
    }

    @Test
    @TestOrder(30)
    public void testDeployerToBuilderWithCallback() throws
                                                    Exception
    {
        DeployerApplication deployer = rule.deployer.getApplication();
        RpcClient           client   = getAndUnwrapException(deployer.getRpcClient(10, TimeUnit.SECONDS));

        TestApi proxy = client.createProxy(WellKnownDestination.Service.getId(), null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbersWithCallback(10, 20, (c) -> CompletableFuture.completedFuture(c * 3)));
        assertEquals(10 + 20 * 3, sum);
    }

    @Test
    @TestOrder(40)
    public void testDeployerToBuilderWithInstance() throws
                                                    Exception
    {
        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        BuilderApplication builder       = rule.builder.getApplication();
        RpcClient          builderClient = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        DeployerApplication deployer       = rule.deployer.getApplication();
        RpcClient           deployerClient = getAndUnwrapException(deployer.getRpcClient(10, TimeUnit.SECONDS));

        TestApiImpl obj = new TestApiImpl();
        obj.state = 123;

        builderClient.registerInstance("test", obj);

        TestApi proxy = deployerClient.createProxy(WellKnownDestination.Service.getId(), "test", TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(10 + 20 + 123, sum);
    }

    @Test
    @TestOrder(50)
    public void testBuilderToDeployerWithInstance() throws
                                                    Exception
    {
        BuilderApplication builder       = rule.builder.getApplication();
        RpcClient          builderClient = getAndUnwrapException(builder.getRpcClient(10, TimeUnit.SECONDS));

        DeployerApplication deployer       = rule.deployer.getApplication();
        RpcClient           deployerClient = getAndUnwrapException(deployer.getRpcClient(10, TimeUnit.SECONDS));

        TestApiImpl obj = new TestApiImpl();
        obj.state = 123;

        deployerClient.registerInstance("test", obj);

        String deployerId = rule.deployer.getEndpointId();

        TestApi proxy = builderClient.createProxy(deployerId, "test", TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(10 + 20 + 123, sum);
    }
}
