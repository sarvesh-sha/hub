/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.NotNull;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.ChannelLifecycle;
import com.optio3.cloud.messagebus.ChannelSecurityPolicy;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessageReply;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.cloud.remoting.RemoteCallDescriptor;
import com.optio3.logging.Logger;
import com.optio3.logging.LoggerPeriodic;
import com.optio3.logging.Severity;
import com.optio3.service.IServiceProvider;
import org.glassfish.jersey.internal.inject.InjectionManager;

@Optio3MessageBusChannel(name = "SYS.RPC")
public class RpcChannel extends MessageBusChannelProvider<RpcMessage, RpcMessageReply> implements ChannelSecurityPolicy,
                                                                                                  ChannelLifecycle
{
    public static final Logger LoggerInstance = MessageBusChannelProvider.LoggerInstance.createSubLogger(RpcChannel.class);

    //--//

    public static class Statistics
    {
        public static final Logger LoggerInstance = RpcChannel.LoggerInstance.createSubLogger(Statistics.class);

        public enum Event
        {
            IncomingCall("%,d bytes"),
            IncomingCallResult("%,d bytes"),
            IncomingCallTime("%,d  msec"),
            OutgoingCall("%,d bytes"),
            OutgoingCallTime("%,d  msec"),
            IncomingCallback("%,d bytes"),
            IncomingCallbackResult("%,d bytes"),
            IncomingCallbackTime("%,d  msec"),
            OutgoingCallback("%,d bytes"),
            OutgoingCallbackTime("%,d  msec");

            private final String fmt;

            Event(String fmt)
            {
                this.fmt = fmt;
            }
        }

        public static class Metric
        {
            private int  count;
            private long value;
            private long minValue;
            private long maxValue;

            public synchronized void record(long value)
            {
                if (count == 0)
                {
                    minValue = value;
                    maxValue = value;
                }
                else
                {
                    minValue = Math.min(minValue, value);
                    maxValue = Math.max(maxValue, value);
                }

                count++;
                this.value += value;
            }

            @Override
            public String toString()
            {
                return String.format("%d count(s), avg=%d sum=%d (MIN:%d / MAX:%d)", count, count > 0 ? value / count : 0, value, minValue, maxValue);
            }
        }

        public static class Entry
        {
            final RemoteCallDescriptor callDescriptor;
            final Metric[]             metrics;

            Entry(RemoteCallDescriptor callDescriptor)
            {
                this.callDescriptor = callDescriptor.cloneForSignatureMatching();

                int num = Event.values().length;
                this.metrics = new Metric[num];
                for (int i = 0; i < num; i++)
                {
                    this.metrics[i] = new Metric();
                }
            }

            void record(Event event,
                        long value)
            {
                Metric m = this.metrics[event.ordinal()];

                m.record(value);

                if (LoggerInstance.isEnabled(Severity.DebugVerbose))
                {
                    String msg = String.format(event.fmt, value);
                    LoggerInstance.debugVerbose("%-22s: %10s for %s", event.name(), msg, this.callDescriptor.toString(true, false));
                }
            }

            void dump(List<String> lines)
            {
                for (Event ev : Event.values())
                {
                    Metric m = this.metrics[ev.ordinal()];

                    if (m.count > 0)
                    {
                        lines.add(String.format("%-22s: %d calls, %s (MIN: %d, MAX: %d)", ev.name(), m.count, String.format(ev.fmt, m.value), m.minValue, m.maxValue));
                    }
                }
            }
        }

        //--//

        private final TreeMap<RemoteCallDescriptor, Entry> m_lookup = new TreeMap<>();

        private final LoggerPeriodic m_periodicDump = new LoggerPeriodic(LoggerInstance, Severity.Debug, 10, TimeUnit.MINUTES)
        {
            @Override
            protected void onActivation()
            {
                List<String> entries = report();

                LoggerInstance.info("");
                LoggerInstance.info("Statistics for RPC:");
                for (String entry : entries)
                {
                    LoggerInstance.info("    %s", entry);
                }
            }
        };

        void record(RemoteCallDescriptor rc,
                    Event event,
                    long value)
        {
            Entry en = lookup(rc);
            en.record(event, value);

            m_periodicDump.process();
        }

        public List<String> report()
        {
            List<String> res = Lists.newArrayList();
            List<Entry>  entries;

            synchronized (m_lookup)
            {
                entries = Lists.newArrayList(m_lookup.values());
            }

            TreeMap<String, TreeMap<String, List<Entry>>> orderedEntries = new TreeMap<>();
            for (Entry entry : entries)
            {
                TreeMap<String, List<Entry>> perClass  = orderedEntries.computeIfAbsent(entry.callDescriptor.classId, (key) -> new TreeMap<>());
                List<Entry>                  perMethod = perClass.computeIfAbsent(entry.callDescriptor.methodName, (key) -> Lists.newArrayList());
                perMethod.add(entry);
            }

            for (String classId : orderedEntries.keySet())
            {
                TreeMap<String, List<Entry>> perClass  = orderedEntries.get(classId);
                boolean                      emitClass = false;

                for (String methodName : perClass.keySet())
                {
                    List<Entry> perMethod = perClass.get(methodName);
                    for (Entry entry : perMethod)
                    {
                        List<String> lines = Lists.newArrayList();
                        entry.dump(lines);

                        if (!lines.isEmpty())
                        {
                            if (!emitClass)
                            {
                                res.add(String.format("Class %s:", classId));
                                emitClass = true;
                            }

                            res.add(String.format("    Method %s:", entry.callDescriptor.toString(false, false)));

                            for (String line : lines)
                            {
                                res.add(String.format("        %s", line));
                            }

                            res.add("");
                        }
                    }
                }
            }

            return res;
        }

        private Entry lookup(RemoteCallDescriptor rc)
        {
            synchronized (m_lookup)
            {
                Entry entry = m_lookup.get(rc);
                if (entry == null)
                {
                    entry = new Entry(rc);
                    m_lookup.put(entry.callDescriptor, entry);
                }
                return entry;
            }
        }
    }

    //--//

    private final RpcContext    m_context;
    private final RpcController m_controller;

    //--//

    public RpcChannel(IServiceProvider serviceProvider,
                      String channelName)
    {
        super(channelName);

        m_context = new RpcContext()
        {
            @Override
            public CallMarshaller getCallMarshaller()
            {
                return serviceProvider.getServiceNonNull(CallMarshaller.class);
            }

            @Override
            public InjectionManager getInjectionManager()
            {
                return serviceProvider.getServiceNonNull(InjectionManager.class);
            }
        };

        m_controller = new RpcController(m_context, this);
    }

    public RpcContext getContext()
    {
        return m_context;
    }

    @Override
    protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                      RpcMessage obj) throws
                                                                      Exception
    {
        return m_controller.processRpcRequest(data, obj);
    }

    public List<String> reportStatistics()
    {
        return m_controller.reportStatistics();
    }

    //--//

    @Override
    public boolean canJoin(@NotNull CookiePrincipal principal)
    {
        return principal.isInRole(WellKnownRole.Machine);
    }

    @Override
    public boolean canListMembers(@NotNull CookiePrincipal principal)
    {
        return false;
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
        // This is a point-to-point channel, no broadcasts.
        return false;
    }

    //--//

    @Override
    public void onJoin(SystemTransport transport)
    {
        // Nothing to do.
    }

    @Override
    public void onLeave(SystemTransport transport)
    {
        // Nothing to do.
    }
}
