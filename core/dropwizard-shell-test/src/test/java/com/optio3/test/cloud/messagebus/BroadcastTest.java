/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.messagebus;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.messagebus.ChannelLifecycle;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.MessageBusChannelSubscriber;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Severity;
import com.optio3.service.IServiceProvider;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class BroadcastTest extends Optio3Test
{
    public static class MultiTestRule extends ExternalResource
    {
        public TestMessageBusRule application1;
        public TestMessageBusRule application2;
        public TestMessageBusRule application3;

        public MultiTestRule()
        {
            application1 = new TestMessageBusRule("messagebus-test1.yml", (configuration) ->
            {
            });

            application2 = new TestMessageBusRule("messagebus-test2.yml", (configuration) ->
            {
            });

            application3 = new TestMessageBusRule("messagebus-test3.yml", (configuration) ->
            {
            });
        }

        @Override
        protected void before() throws
                                Throwable
        {
            application1.invokeBefore();
            application2.invokeBefore();
            application3.invokeBefore();
        }

        @Override
        protected void after()
        {
            application3.invokeAfter();
            application2.invokeAfter();
            application1.invokeAfter();
        }
    }

    @ClassRule
    public static final MultiTestRule rule = new MultiTestRule();

    private static TestChannel server1;
    private static TestChannel server2;
    private static TestChannel server3;
    private static TestClient  localClient;

    public static class MessageBusTest
    {
        public String key;
        public String value;
    }

    @Optio3MessageBusChannel(name = "TEST")
    static class TestChannel extends MessageBusChannelProvider<MessageBusTest, MessageBusTest> implements ChannelLifecycle
    {
        int count;
        int countBroadcast;

        public TestChannel(IServiceProvider serviceProvider,
                           String channelName)
        {
            super(channelName);
        }

        @Override
        public void onJoin(SystemTransport transport)
        {
            System.out.printf("New Endpoint: %s%n", transport.getEndpointId());
        }

        @Override
        public void onLeave(SystemTransport transport)
        {
        }

        @Override
        protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                          MessageBusTest obj) throws
                                                                              Exception
        {
            System.out.printf("SERVER: %s = %s%n", obj.key, obj.value);

            count++;

            if (data.wasBroadcast())
            {
                countBroadcast++;
                return AsyncRuntime.NullResult;
            }

            obj.key += " got";

            return replyToMessage(data, obj, null);
        }
    }

    static class TestClient extends MessageBusChannelSubscriber<MessageBusTest, MessageBusTest>
    {
        int count;
        int countBroadcast;

        public TestClient(String name)
        {
            super(name);
        }

        @Override
        protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                          MessageBusTest obj) throws
                                                                              Exception
        {
            System.out.printf("CLIENT: %s = %s%n", obj.key, obj.value);

            count++;

            if (data.wasBroadcast())
            {
                countBroadcast++;
                return AsyncRuntime.NullResult;
            }

            obj.key += " got client";

            return replyToMessage(data, obj, null);
        }
    }

    @Test
    @TestOrder(0)
    public void init()
    {
        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        server1 = rule.application1.getApplication()
                                   .addChannel(TestChannel.class);
        server2 = rule.application2.getApplication()
                                   .addChannel(TestChannel.class);
        server3 = rule.application3.getApplication()
                                   .addChannel(TestChannel.class);

        localClient = new TestClient("TEST");
        rule.application2.getApplication()
                         .getServiceNonNull(MessageBusBroker.class)
                         .registerLocalChannelSubscriber(localClient);
    }

    @Test
    @TestOrder(20)
    public void testConnect() throws
                              Exception
    {
        try (MessageBusClientWebSocket.NoUDP socket1 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application1), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD);
             MessageBusClientWebSocket.NoUDP socket2 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application2), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD);
             MessageBusClientWebSocket.NoUDP socket3 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application3), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD))
        {
            TestClient client1 = new TestClient("TEST");
            socket1.startConnection();
            String client1Id = getAndUnwrapException(socket1.getEndpointId());

            TestClient client2 = new TestClient("TEST");
            socket2.startConnection();
            String client2Id = getAndUnwrapException(socket2.getEndpointId());

            TestClient client3 = new TestClient("TEST");
            socket3.startConnection();
            String client3Id = getAndUnwrapException(socket3.getEndpointId());

            getAndUnwrapException(socket1.join(client1));
            getAndUnwrapException(socket2.join(client2));
            getAndUnwrapException(socket3.join(client3));

            System.out.printf("Server1: %s%n", server1.getEndpointId());
            System.out.printf("Server2: %s%n", server2.getEndpointId());
            System.out.printf("Server3: %s%n", server3.getEndpointId());
            System.out.printf("Client1: %s%n", client1Id);
            System.out.printf("Client2: %s%n", client2Id);
            System.out.printf("Client3: %s%n", client3Id);

            //--//

            MessageBusBroker broker1 = rule.application1.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);
            getAndUnwrapException(broker1.connectToPeer(null, getWsConnectString(rule.application2), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));

            MessageBusBroker broker3 = rule.application3.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);
            getAndUnwrapException(broker3.connectToPeer(null, getWsConnectString(rule.application2), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));

            //--//

            System.out.println("####### Send message through peering: 1 -> 2");
            {
                MessageBusTest msg = new MessageBusTest();
                msg.key   = "key1";
                msg.value = "val1";
                MessageBusTest reply = getAndUnwrapException(client1.sendMessageWithReply(client2Id, msg, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
                assertEquals("key1 got client", reply.key);
                assertEquals("val1", reply.value);
            }

            assertEquals(0, server1.count);
            assertEquals(0, server2.count);
            assertEquals(0, server3.count);
            assertEquals(0, client1.count);
            assertEquals(1, client2.count);
            assertEquals(0, client3.count);
            assertEquals(0, localClient.count);

            System.out.println("####### Send message through peering: 1 -> 3");
            {
                MessageBusTest msg = new MessageBusTest();
                msg.key   = "key2";
                msg.value = "val2";
                MessageBusTest reply = getAndUnwrapException(client1.sendMessageWithReply(client3Id, msg, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
                assertEquals("key2 got client", reply.key);
                assertEquals("val2", reply.value);
            }

            assertEquals(0, server1.count);
            assertEquals(0, server2.count);
            assertEquals(0, server3.count);
            assertEquals(0, client1.count);
            assertEquals(1, client2.count);
            assertEquals(1, client3.count);
            assertEquals(0, localClient.count);

            System.out.println("####### Send message through peering: localClient on 2 -> 3");
            {
                MessageBusTest msg = new MessageBusTest();
                msg.key   = "key2";
                msg.value = "val2";
                MessageBusTest reply = getAndUnwrapException(localClient.sendMessageWithReply(client3Id, msg, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
                assertEquals("key2 got client", reply.key);
                assertEquals("val2", reply.value);
            }

            assertEquals(0, server1.count);
            assertEquals(0, server2.count);
            assertEquals(0, server3.count);
            assertEquals(0, client1.count);
            assertEquals(1, client2.count);
            assertEquals(2, client3.count);
            assertEquals(0, localClient.count);

            System.out.println("####### Send broadcast message to services");
            {
                MessageBusTest msg = new MessageBusTest();
                msg.key   = "key3";
                msg.value = "val3";
                getAndUnwrapException(client1.sendMessageWithNoReply(WellKnownDestination.Service_Broadcast.getId(), msg, null));
            }

            Thread.sleep(100);

            assertEquals(1, server1.count);
            assertEquals(1, server2.count);
            assertEquals(1, server3.count);
            assertEquals(0, client1.count);
            assertEquals(1, client2.count);
            assertEquals(2, client3.count);
            assertEquals(0, localClient.count);

            System.out.println("####### Send broadcast message to everyone");
            {
                MessageBusTest msg = new MessageBusTest();
                msg.key   = "key4";
                msg.value = "val4";
                getAndUnwrapException(client1.sendMessageWithNoReply(WellKnownDestination.Broadcast.getId(), msg, null));
            }

            Thread.sleep(100);

            assertEquals(2, server1.count);
            assertEquals(2, server2.count);
            assertEquals(2, server3.count);
            assertEquals(0, client1.count);
            assertEquals(2, client2.count);
            assertEquals(3, client3.count);
            assertEquals(1, localClient.count);
        }
    }

    private String getWsConnectString(TestMessageBusRule rule)
    {
        URI    baseUri = rule.baseUri();
        String destUri = String.format("ws://%s:%d/api/v1/message-bus", baseUri.getHost(), baseUri.getPort());

        return destUri;
    }
}
