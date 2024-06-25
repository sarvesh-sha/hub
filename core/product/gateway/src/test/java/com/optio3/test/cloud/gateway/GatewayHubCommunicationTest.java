/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.gateway;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.optio3.cloud.annotation.Optio3RemotableEndpoint;
import com.optio3.cloud.client.Optio3RemotableProxy;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.RpcClient;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class GatewayHubCommunicationTest extends Optio3Test
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
        @Override
        public CompletableFuture<Integer> addNumbers(int a,
                                                     int b)
        {
            return wrapAsync(a + b);
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
        public HubTestApplicationRule     hub;
        public GatewayTestApplicationRule gateway;

        public MultiTestRule()
        {
            hub = new HubTestApplicationRule((configuration) ->
                                             {
                                                 configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
                                             }, (application) ->
                                             {
                                                 application.registerRemotableEndpoint(TestApiImpl.class);
                                             });

            gateway = new GatewayTestApplicationRule((configuration) ->
                                                     {
                                                         configuration.connectionUrl = (hub.baseUri()
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
            hub.invokeBefore();
            gateway.invokeBefore();
        }

        @Override
        protected void after()
        {
            gateway.invokeAfter();
            hub.invokeAfter();
        }
    }

    @ClassRule
    public static final MultiTestRule rule = new MultiTestRule();

    @Test
    @TestOrder(10)
    public void testGatewayToHub() throws
                                   Exception
    {
//        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
//        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        GatewayApplication gateway = rule.gateway.getApplication();
        RpcClient          client  = getAndUnwrapException(gateway.getRpcClient(10, TimeUnit.SECONDS));

        TestApi proxy = client.createProxy(WellKnownDestination.Service.getId(), null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(30, sum);
    }

    @Test
    @TestOrder(20)
    public void testHubToGateway() throws
                                   Exception
    {
        HubApplication hub    = rule.hub.getApplication();
        RpcClient      client = getAndUnwrapException(hub.getRpcClient(10, TimeUnit.SECONDS));

        String gatewayId = rule.gateway.getEndpointId();

        TestApi proxy = client.createProxy(gatewayId, null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbers(10, 20));
        assertEquals(10 + 20, sum);
    }

    @Test
    @TestOrder(30)
    public void testGatewayToHubWithCallback() throws
                                               Exception
    {
        GatewayApplication gateway = rule.gateway.getApplication();
        RpcClient          client  = getAndUnwrapException(gateway.getRpcClient(10, TimeUnit.SECONDS));

        TestApi proxy = client.createProxy(WellKnownDestination.Service.getId(), null, TestApi.class, 100, TimeUnit.SECONDS);
        int     sum   = getAndUnwrapException(proxy.addNumbersWithCallback(10, 20, (c) -> CompletableFuture.completedFuture(c * 3)));
        assertEquals(10 + 20 * 3, sum);
    }
}
