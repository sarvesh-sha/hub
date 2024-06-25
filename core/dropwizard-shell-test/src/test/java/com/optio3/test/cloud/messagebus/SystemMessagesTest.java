/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.messagebus;

import static com.optio3.util.Exceptions.getAndUnwrapException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.messagebus.ChannelLifecycle;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.MessageBusChannelSubscriber;
import com.optio3.cloud.messagebus.MessageBusClientWebSocket;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.channel.Ping;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Severity;
import com.optio3.service.IServiceProvider;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.common.TestOrder;
import org.junit.ClassRule;
import org.junit.Test;

public class SystemMessagesTest extends Optio3Test
{
    @ClassRule
    public static final TestMessageBusRule applicationRule = new TestMessageBusRule("messagebus-test1.yml", (configuration) ->
    {
    });

    public static class MessageBusTest
    {
        public String key;
        public String value;
    }

    @Optio3MessageBusChannel(name = "TEST")
    static class TestChannel extends MessageBusChannelProvider<MessageBusTest, MessageBusTest> implements ChannelLifecycle
    {
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

            obj.key += " got";

            return replyToMessage(data, obj, null);
        }
    }

    @Test
    @TestOrder(1)
    public void testConnect() throws
                              Exception
    {
        applicationRule.getApplication()
                       .addChannel(TestChannel.class);
        applicationRule.getApplication()
                       .addChannel(Ping.class);

        MessageBusBroker.LoggerInstance.enable(Severity.Debug);
        MessageBusBroker.LoggerInstance.enable(Severity.DebugVerbose);

        try (MessageBusClientWebSocket.NoUDP socket = new MessageBusClientWebSocket.NoUDP(null, getWsConnectString(applicationRule), TestMessageBusRule.NORMAL_USER, TestMessageBusRule.NORMAL_PWD))
        {
            MessageBusChannelSubscriber<String, String> subscriberPing = new MessageBusChannelSubscriber<String, String>("SYS.PING")
            {
                @Override
                protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                                  String obj)
                {
                    // TODO Auto-generated method stub
                    throw new RuntimeException("Not implemented");
                }
            };

            MessageBusChannelSubscriber<MessageBusTest, MessageBusTest> subscriber = new MessageBusChannelSubscriber<MessageBusTest, MessageBusTest>("TEST")
            {
                @Override
                protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                                  MessageBusTest obj)
                {
                    // TODO Auto-generated method stub
                    throw new RuntimeException("Not implemented");
                }
            };

            socket.startConnection();

            String       ourId = getAndUnwrapException(socket.getEndpointId());
            List<String> res   = getAndUnwrapException(socket.listChannels());
            assertTrue(res.contains("TEST"));
            assertTrue(res.contains("<<PEERING>>"));

            try
            {
                getAndUnwrapException(socket.listMembers("TEST"));
                fail("Unexpected success");
            }
            catch (RuntimeException e)
            {
                // Not subscribed, it should fail.
            }

            getAndUnwrapException(socket.join(subscriber));

            List<String> resAfter = getAndUnwrapException(socket.listMembers("TEST"));
            assertTrue(resAfter.contains(ourId));
            assertEquals(2, resAfter.size());
            resAfter.removeIf((s) -> s.startsWith("<<SERVICE>>"));
            assertEquals(1, resAfter.size());

            MessageBusTest msg1 = new MessageBusTest();
            msg1.key   = "key1";
            msg1.value = "val1";
            MessageBusTest reply1 = getAndUnwrapException(subscriber.sendMessageWithReply(WellKnownDestination.Service.getId(), msg1, MessageBusTest.class, null, 10, TimeUnit.SECONDS));
            assertEquals("key1 got", reply1.key);
            assertEquals("val1", reply1.value);

            //--//

            getAndUnwrapException(socket.join(subscriberPing));
            String reply = getAndUnwrapException(subscriberPing.sendMessageWithReply(WellKnownDestination.Service.getId(), "Test", String.class, null, 10, TimeUnit.SECONDS));
            assertEquals("Got: Test", reply);
        }
    }

    private String getWsConnectString(TestMessageBusRule rule)
    {
        URI    baseUri = rule.baseUri();
        String destUri = String.format("ws://%s:%d/api/v1/message-bus", baseUri.getHost(), baseUri.getPort());

        return destUri;
    }
}
