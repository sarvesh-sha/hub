/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.transport.DataTransport;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;

public final class MessageBusChannel
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusChannel.class);

    private final ChannelSecurityPolicy s_openPolicy = new ChannelSecurityPolicy()
    {
        @Override
        public boolean canJoin(@NotNull CookiePrincipal principal)
        {
            return true;
        }

        @Override
        public boolean canListMembers(@NotNull CookiePrincipal principal)
        {
            return true;
        }

        @Override
        public boolean canSend(@NotNull CookiePrincipal principal,
                               MbData data)
        {
            return true;
        }

        @Override
        public boolean canSendBroadcast(@NotNull CookiePrincipal principal,
                                        MbData data)
        {
            return true;
        }
    };

    //--//

    private final String                m_channelName;
    private final DataTransport<?>      m_provider;
    private final ChannelSecurityPolicy m_policy;

    private final Map<String, SystemTransport> m_members = Maps.newHashMap();

    MessageBusChannel(String channelName,
                      DataTransport<?> provider,
                      ChannelSecurityPolicy policy)
    {
        m_channelName = channelName;
        m_provider = provider;
        m_policy = policy != null ? policy : s_openPolicy;
    }

    //--//

    public String getChannelName()
    {
        return m_channelName;
    }

    public DataTransport<?> getProvider()
    {
        return m_provider;
    }

    public ChannelSecurityPolicy getPolicy()
    {
        return m_policy;
    }

    //--//

    public List<String> getMemberIds()
    {
        synchronized (m_members)
        {
            return Lists.newArrayList(m_members.keySet());
        }
    }

    public List<SystemTransport> getTransports()
    {
        synchronized (m_members)
        {
            return Lists.newArrayList(m_members.values());
        }
    }

    //--//

    void join(SystemTransport transport)
    {
        synchronized (m_members)
        {
            m_members.put(transport.getEndpointId(), transport);

            LoggerInstance.debug("####### JOIN: %s / %s - %d subscriber(s) in %s channel", transport.getEndpointId(), transport.getTransportPrincipalAsText(), m_members.size(), getChannelName());
            listMembers();
        }

        for (SystemTransport st : getTransports())
        {
            if (st == transport)
            {
                // Don't notify the joining transport
                continue;
            }

            ChannelLifecycle cl = Reflection.as(st, ChannelLifecycle.class);
            if (cl != null)
            {
                try
                {
                    cl.onJoin(transport);
                }
                catch (Throwable ex)
                {
                    LoggerInstance.error("Channel join for %s failed: %s", transport.getEndpointId());
                }
            }
        }
    }

    void leave(SystemTransport transport)
    {
        synchronized (m_members)
        {
            m_members.remove(transport.getEndpointId());

            LoggerInstance.debug("####### LEAVE: %s / %s - %d subscriber(s) in %s channel", transport.getEndpointId(), transport.getTransportPrincipalAsText(), m_members.size(), getChannelName());
            listMembers();
        }

        for (SystemTransport st : getTransports())
        {
            ChannelLifecycle cl = Reflection.as(st, ChannelLifecycle.class);
            if (cl != null)
            {
                try
                {
                    cl.onLeave(transport);
                }
                catch (Throwable ex)
                {
                    LoggerInstance.error("Channel leave for %s failed: %s", transport.getEndpointId(), ex);
                }
            }
        }
    }

    private void listMembers()
    {
        for (SystemTransport transport : m_members.values())
        {
            LoggerInstance.debugVerbose("#######      %-60s | %-40s | %s", transport.getEndpointId(), transport.getPurposeInfo(), transport.getTransportPrincipalAsText());
        }
    }
}
