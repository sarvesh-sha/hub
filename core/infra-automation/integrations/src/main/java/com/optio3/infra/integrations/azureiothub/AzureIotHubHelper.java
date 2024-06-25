/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.integrations.azureiothub;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.microsoft.azure.sdk.iot.device.DeviceClient;
import com.microsoft.azure.sdk.iot.device.IotHubClientProtocol;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.IotHubStatusCode;
import com.microsoft.azure.sdk.iot.device.Message;
import com.microsoft.azure.sdk.iot.device.MessageType;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;
import com.optio3.serialization.ObjectMappers;

public abstract class AzureIotHubHelper implements AutoCloseable
{
    public static class Credentials
    {
        public String hostName;
        public String deviceId;
        public String sharedAccessKey;

        public void obfuscate()
        {
            char[] secret = sharedAccessKey.toCharArray();

            for (int i = 0; i < secret.length - 3; i++)
            {
                secret[i] = '*';
            }

            sharedAccessKey = new String(secret);
        }

        public String generateConnectionString()
        {
            return String.format("HostName=%s;DeviceId=%s;SharedAccessKey=%s", hostName, deviceId, sharedAccessKey);
        }
    }

    private final DeviceClient  m_client;
    private final AtomicInteger m_pendingMessages = new AtomicInteger();

    protected AzureIotHubHelper(Credentials cred) throws
                                                  URISyntaxException,
                                                  IOException
    {
        DeviceClient client = new DeviceClient(cred.generateConnectionString(), IotHubClientProtocol.AMQPS);

        // Set your token expiry time limit here
        long time = 2400;
        client.setOption("SetSASTokenExpiryTime", time);

        client.registerConnectionStatusChangeCallback((iotHubConnectionStatus, iotHubConnectionStatusChangeReason, throwable, o) ->
                                                      {
                                                          reportStatusChange(iotHubConnectionStatus, iotHubConnectionStatusChangeReason, throwable);
                                                      }, new Object());

        client.open();

        m_client = client;
    }

    @Override
    public void close() throws
                        Exception
    {
        try
        {
            synchronized (m_pendingMessages)
            {
                while (m_pendingMessages.get() > 0)
                {
                    m_pendingMessages.wait(30_000);
                }
            }
        }
        catch (Throwable t)
        {
            // If we can't drain the queue in 30 seconds, quit.
        }

        m_client.closeNow();
    }

    //--//

    protected abstract void reportStatusChange(IotHubConnectionStatus status,
                                               IotHubConnectionStatusChangeReason changeReason,
                                               Throwable t);

    protected abstract void reportMessageSent(String messageId,
                                              Object payload,
                                              IotHubStatusCode responseStatus);

    //--//

    public void sendMessage(String messageId,
                            Object payload,
                            int maxOutstandingMessages) throws
                                                        JsonProcessingException,
                                                        InterruptedException
    {
        Message msg = new Message(ObjectMappers.SkipNulls.writeValueAsString(payload));
        msg.setContentTypeFinal("application/json");
        msg.setMessageType(MessageType.DEVICE_TELEMETRY);
        msg.setMessageId(messageId);

        synchronized (m_pendingMessages)
        {
            while (m_pendingMessages.get() > maxOutstandingMessages)
            {
                m_pendingMessages.wait();
            }

            m_pendingMessages.incrementAndGet();
        }

        m_client.sendEventAsync(msg, (iotHubStatusCode, context) ->
        {
            synchronized (m_pendingMessages)
            {
                if (m_pendingMessages.decrementAndGet() < 0.8 * maxOutstandingMessages) // A bit of hysteresis.
                {
                    m_pendingMessages.notifyAll();
                }
            }

            reportMessageSent(messageId, payload, iotHubStatusCode);
        }, msg);
    }
}
