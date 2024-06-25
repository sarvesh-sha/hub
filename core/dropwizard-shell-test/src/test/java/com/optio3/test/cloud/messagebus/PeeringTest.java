/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.messagebus;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.messagebus.ChannelLifecycle;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.MessageBusChannelSubscriber;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Severity;
import com.optio3.service.IServiceProvider;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

public class PeeringTest extends Optio3Test
{
    public static class MultiTestRule extends ExternalResource
    {
        public TestMessageBusRule application1;
        public TestMessageBusRule application2;
        public TestMessageBusRule application3;
        public TestMessageBusRule application4;

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

            application4 = new TestMessageBusRule("messagebus-test4.yml", (configuration) ->
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
            application4.invokeBefore();
        }

        @Override
        protected void after()
        {
            application4.invokeAfter();
            application3.invokeAfter();
            application2.invokeAfter();
            application1.invokeAfter();
        }
    }

    @ClassRule
    public static final MultiTestRule rule = new MultiTestRule();

    public static class MessageBusTest
    {
        public String key;
        public String value;
    }

    @Optio3MessageBusChannel(name = "TEST")
    static class TestChannel extends MessageBusChannelProvider<MessageBusTest, MessageBusTest> implements ChannelLifecycle
    {
        private final Set<String> m_seen = Sets.newHashSet();

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
            System.out.printf("SERVER: %s = %s (%s)\n", obj.key, obj.value, data.messageId);

            synchronized (m_seen)
            {
                assertTrue("Duplicate message!", m_seen.add(data.messageId));
            }

            obj.key += " got";

            return replyToMessage(data, obj, null);
        }
    }

    static class TestClient extends MessageBusChannelSubscriber<MessageBusTest, MessageBusTest>
    {
        private final Set<String> m_seen = Sets.newHashSet();
        private final String      m_id;

        public TestClient(String name,
                          String id)
        {
            super(name);

            m_id = id;
        }

        @Override
        protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                          MessageBusTest obj) throws
                                                                              Exception
        {
            System.out.printf("CLIENT: %s = %s (%s)\n", obj.key, obj.value, data.messageId);

            synchronized (m_seen)
            {
                if (m_seen.add(data.messageId))
                {
                    obj.key += " got client on " + m_id;
                }
                else
                {
                    obj.key += " got duplicate client on " + m_id;
                }
            }

            return replyToMessage(data, obj, null);
        }
    }

    @Test
    @TestOrder(0)
    public void init()
    {
        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        rule.application1.getApplication()
                         .addChannel(TestChannel.class);
        rule.application2.getApplication()
                         .addChannel(TestChannel.class);
        rule.application3.getApplication()
                         .addChannel(TestChannel.class);
        rule.application4.getApplication()
                         .addChannel(TestChannel.class);
    }

    @Test
    @TestOrder(10)
    public void notAuthorizedForPeering() throws
                                          Exception
    {
        MessageBusBroker broker1 = rule.application1.getApplication()
                                                    .getServiceNonNull(MessageBusBroker.class);

        assertFailure(UpgradeException.class, () ->
        {
            getAndUnwrapException(broker1.connectToPeer(null, getWsConnectString(rule.application2), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_USER));
        });

        try
        {
            getAndUnwrapException(broker1.connectToPeer(null, getWsConnectString(rule.application2), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD));
            fail("Unexpected success");
        }
        catch (RuntimeException e)
        {
            assertEquals("JoinChannel command failed", e.getMessage());
            // Good password, but not MACHINE role, it should fail.
        }
    }

    @Test
    @TestOrder(20)
    public void testConnect() throws
                              Exception
    {
        try (MessageBusClientWebSocket.NoUDP socket1 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application1), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD);
             MessageBusClientWebSocket.NoUDP socket2 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application2), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD);
             MessageBusClientWebSocket.NoUDP socket3 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application3), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD);
             MessageBusClientWebSocket.NoUDP socket4 = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(rule.application4), TestMessageBusRule.ADMIN_USER, TestMessageBusRule.ADMIN_PWD))
        {
            TestClient client1 = new TestClient("TEST", "1");
            socket1.startConnection();
            String client1Id = getAndUnwrapException(socket1.getEndpointId());

            TestClient client2 = new TestClient("TEST", "2");
            socket2.startConnection();
            String client2Id = getAndUnwrapException(socket2.getEndpointId());

            TestClient client3 = new TestClient("TEST", "3");
            socket3.startConnection();
            String client3Id = getAndUnwrapException(socket3.getEndpointId());

            TestClient client4 = new TestClient("TEST", "4");
            socket4.startConnection();
            String client4Id = getAndUnwrapException(socket4.getEndpointId());

            getAndUnwrapException(socket1.join(client1));
            getAndUnwrapException(socket2.join(client2));
            getAndUnwrapException(socket3.join(client3));
            getAndUnwrapException(socket4.join(client4));

            MessageBusBroker broker1 = rule.application1.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);
            MessageBusBroker broker2 = rule.application2.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);
            MessageBusBroker broker3 = rule.application3.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);
            MessageBusBroker broker4 = rule.application4.getApplication()
                                                        .getServiceNonNull(MessageBusBroker.class);

            //--//

            System.out.println("####### Getting list of members before peering");
            List<String> res1 = getAndUnwrapException(socket1.listMembers("TEST"));
            assertTrue(res1.contains(client1Id));
            assertFalse(res1.contains(client2Id));
            assertFalse(res1.contains(client3Id));
            assertFalse(res1.contains(client4Id));

            getAndUnwrapException(broker1.connectToPeer(null, getWsConnectString(rule.application2), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));

            System.out.println("####### Getting list of members after peering 1<->2");
            List<String> res2 = getAndUnwrapException(socket1.listMembers("TEST"));
            assertTrue(res2.contains(client1Id));
            assertTrue(res2.contains(client2Id));
            assertFalse(res2.contains(client3Id));
            assertFalse(res2.contains(client4Id));

            getAndUnwrapException(broker1.connectToPeer(null, getWsConnectString(rule.application3), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));

            System.out.println("####### Getting list of members after peering 1<->3");
            List<String> res3 = getAndUnwrapException(socket1.listMembers("TEST"));
            assertTrue(res3.contains(client1Id));
            assertTrue(res3.contains(client2Id));
            assertTrue(res3.contains(client3Id));
            assertFalse(res3.contains(client4Id));

            getAndUnwrapException(broker2.connectToPeer(null, getWsConnectString(rule.application4), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));
            getAndUnwrapException(broker3.connectToPeer(null, getWsConnectString(rule.application4), TestMessageBusRule.MACHINE_USER, TestMessageBusRule.MACHINE_PWD));

            System.out.println("####### Getting list of members after peering 2<->4 and 3<->4");
            List<String> res4 = getAndUnwrapException(socket1.listMembers("TEST"));
            assertTrue(res4.contains(client1Id));
            assertTrue(res4.contains(client2Id));
            assertTrue(res4.contains(client3Id));
            assertTrue(res4.contains(client4Id));

            System.out.println("####### Send message through peering 1 -> 2");
            MessageBusTest msg1 = new MessageBusTest();
            msg1.key   = "key1";
            msg1.value = "val1";
            MessageBusTest reply1 = getAndUnwrapException(client1.sendMessageWithReply(client2Id, msg1, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
            assertEquals("key1 got client on 2", reply1.key);
            assertEquals("val1", reply1.value);

            System.out.println("####### Send message through peering 2 -> 3");
            MessageBusTest msg2 = new MessageBusTest();
            msg2.key   = "key2";
            msg2.value = "val2";
            MessageBusTest reply2 = getAndUnwrapException(client2.sendMessageWithReply(client3Id, msg2, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
            assertEquals("key2 got client on 3", reply2.key);
            assertEquals("val2", reply2.value);

            System.out.println("####### Send message through peering 1 -> 2 -> 4 and 1 -> 3 -> 4");
            MessageBusTest msg3 = new MessageBusTest();
            msg3.key   = "key3";
            msg3.value = "val3";
            MessageBusTest reply3 = getAndUnwrapException(client1.sendMessageWithReply(client4Id, msg3, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
            assertEquals("key3 got client on 4", reply3.key);
            assertEquals("val3", reply3.value);

            //--//

            System.out.println("####### Closing client1");
            getAndUnwrapException(socket1.leave(client1));
            client1.close();

            System.out.println("####### Closing client2");
            getAndUnwrapException(socket2.leave(client2));
            client2.close();

            System.out.println("####### Closing client3");
            getAndUnwrapException(socket3.leave(client3));
            client3.close();

            System.out.println("####### Closing client4");
            getAndUnwrapException(socket3.leave(client4));
            client4.close();
            Thread.sleep(100);
        }
    }

    private String getWsConnectString(TestMessageBusRule rule)
    {
        URI    baseUri = rule.baseUri();
        String destUri = String.format("ws://%s:%d/api/v1/message-bus", baseUri.getHost(), baseUri.getPort());

        return destUri;
    }
}
