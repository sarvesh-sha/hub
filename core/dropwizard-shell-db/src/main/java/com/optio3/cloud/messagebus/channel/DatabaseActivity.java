/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.validation.constraints.NotNull;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3MessageBusChannel;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.messagebus.ChannelLifecycle;
import com.optio3.cloud.messagebus.ChannelSecurityPolicy;
import com.optio3.cloud.messagebus.MessageBusBroker;
import com.optio3.cloud.messagebus.MessageBusChannelProvider;
import com.optio3.cloud.messagebus.MessageBusChannelSubscriber;
import com.optio3.cloud.messagebus.WellKnownDestination;
import com.optio3.cloud.messagebus.payload.MbData;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.db.DbMessage;
import com.optio3.cloud.messagebus.payload.db.DbMessageReply;
import com.optio3.cloud.messagebus.payload.db.DbMessage_Config;
import com.optio3.cloud.messagebus.payload.db.DbMessage_Config_Reply;
import com.optio3.cloud.messagebus.payload.db.DbMessage_Event;
import com.optio3.cloud.messagebus.transport.SystemTransport;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.persistence.DbEvent;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.service.IServiceProvider;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

@Optio3MessageBusChannel(name = "SYS.DB-ACTIVITY")
public class DatabaseActivity extends MessageBusChannelProvider<DbMessage, DbMessageReply> implements ChannelSecurityPolicy,
                                                                                                      ChannelLifecycle
{
    public static final class LocalSubscriber extends MessageBusChannelSubscriber<DbMessage, DbMessageReply>
    {
        private final ConcurrentMap<String, ConsumerWithException<DbEvent>> m_callbacks = Maps.newConcurrentMap();

        private LocalSubscriber()
        {
            super(DatabaseActivity.class);
        }

        @Override
        protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                          DbMessage obj) throws
                                                                         Exception
        {
            DbMessage_Event msg = Reflection.as(obj, DbMessage_Event.class);
            if (msg != null)
            {
                if (msg.events != null)
                {
                    for (DbEvent dbEvent : msg.events)
                    {
                        ConsumerWithException<DbEvent> callback = m_callbacks.get(dbEvent.context.getTable());
                        if (callback != null)
                        {
                            callback.accept(dbEvent);
                        }
                    }
                }
            }

            return AsyncRuntime.asNull();
        }

        public static LocalSubscriber create(MessageBusBroker broker)
        {
            LocalSubscriber sub = new LocalSubscriber();
            broker.registerLocalChannelSubscriber(sub);
            return sub;
        }

        public boolean subscribeToTable(Class<?> clz,
                                        ConsumerWithException<DbEvent> callback)
        {
            Optio3TableInfo anno = clz.getAnnotation(Optio3TableInfo.class);
            if (anno == null)
            {
                throw Exceptions.newRuntimeException("Missing @Optio3TableInfo on type '%s'", clz);
            }

            String table = anno.externalId();

            if (m_callbacks.containsKey(table))
            {
                m_callbacks.put(table, callback);
                return true;
            }

            DbMessage_Config cfg = new DbMessage_Config();
            cfg.table = table;
            cfg.active = true;

            try
            {
                DbMessage_Config_Reply reply = sendMessageWithReply(WellKnownDestination.Service.getId(), cfg, DbMessage_Config_Reply.class, null, 10, TimeUnit.SECONDS).get();
                if (reply.success)
                {
                    m_callbacks.put(table, callback);
                    return true;
                }

                return false;
            }
            catch (Exception e)
            {
                return false;
            }
        }
    }

    //--//

    public static final Logger LoggerInstance = MessageBusChannelProvider.LoggerInstance.createSubLogger(DatabaseActivity.class);

    //--//

    private static final int MaxEventsPerMessage = 100;

    private final Multimap<String, String>   m_endpointToTables  = LinkedHashMultimap.create();
    private final Multimap<String, String>   m_tableToEndpoints  = LinkedHashMultimap.create();
    private final Map<String, ZonedDateTime> m_tableToLastUpdate = Maps.newHashMap();

    private Map<String, AtomicInteger> m_deliveryStats;

    //--//

    public DatabaseActivity(IServiceProvider serviceProvider,
                            String channelName)
    {
        super(channelName);
    }

    @Override
    protected CompletableFuture<Void> receivedMessage(MbData_Message data,
                                                      DbMessage obj) throws
                                                                     Exception
    {
        DbMessage_Event ev = Reflection.as(obj, DbMessage_Event.class);
        if (ev != null)
        {
            // We received a broadcast from other brokers, forward it to our local subscribers.
            deliverLocally(ev.events);
        }
        else
        {
            DbMessage_Config cfg = Reflection.as(obj, DbMessage_Config.class);
            if (cfg != null)
            {
                DbMessage_Config_Reply reply = new DbMessage_Config_Reply();

                if (cfg.table != null)
                {
                    reply.lastUpdate = manageSubscription(data.origin, cfg.table, cfg.active);
                    reply.success = true;
                }

                return replyToMessage(data, reply, null);
            }
        }

        return AsyncRuntime.NullResult;
    }

    //--//

    @Override
    public boolean canJoin(@NotNull CookiePrincipal principal)
    {
        return principal.isAuthenticated();
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
        // Machine accounts can always send.
        if (principal.isInRole(WellKnownRole.Machine))
        {
            return true;
        }

        // Normal users can only send to the local service.
        return MbData.isForLocalService(data.destination);
    }

    @Override
    public boolean canSendBroadcast(@NotNull CookiePrincipal principal,
                                    MbData data)
    {
        // Only allow Machine accounts to send broadcast messages.
        return principal.isInRole(WellKnownRole.Machine);
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
        String endpoint = transport.getEndpointId();

        removeSubscriptions(endpoint);
    }

    //--//

    private List<String> getSubscribers(String table,
                                        ZonedDateTime lastUpdate)
    {
        synchronized (m_endpointToTables)
        {
            // 
            // If timestamp is present, update our records if newer.
            //
            if (lastUpdate != null)
            {
                ZonedDateTime oldLastUpdate = m_tableToLastUpdate.get(table);
                if (oldLastUpdate == null || oldLastUpdate.isBefore(lastUpdate))
                {
                    m_tableToLastUpdate.put(table, lastUpdate);
                }
            }

            return Lists.newArrayList(m_tableToEndpoints.get(table));
        }
    }

    private List<String> getSubscriptions(String origin)
    {
        synchronized (m_endpointToTables)
        {
            return Lists.newArrayList(m_endpointToTables.get(origin));
        }
    }

    private ZonedDateTime manageSubscription(String origin,
                                             String table,
                                             boolean active)
    {
        synchronized (m_endpointToTables)
        {
            if (active)
            {
                m_endpointToTables.put(origin, table);
                m_tableToEndpoints.put(table, origin);
            }
            else
            {
                m_endpointToTables.remove(origin, table);
                m_tableToEndpoints.remove(table, origin);
            }

            return m_tableToLastUpdate.computeIfAbsent(table, k -> TimeUtils.now());
        }
    }

    private void removeSubscriptions(String origin)
    {
        synchronized (m_endpointToTables)
        {
            for (String table : getSubscriptions(origin))
            {
                m_tableToEndpoints.remove(table, origin);
            }

            m_endpointToTables.removeAll(origin);
        }
    }

    //--//

    public void fireEvents(Collection<DbEvent> events)
    {
        if (events.size() > MaxEventsPerMessage)
        {
            List<DbEvent> subList = Lists.newArrayListWithCapacity(MaxEventsPerMessage);

            for (DbEvent event : events)
            {
                subList.add(event);
                if (subList.size() >= MaxEventsPerMessage)
                {
                    fireEvents(subList);
                    subList.clear();
                }
            }

            if (!subList.isEmpty())
            {
                fireEvents(subList);
            }
        }
        else
        {
            // Send to other brokers.
            sendMessage(WellKnownDestination.Service_Broadcast.getId(), events);

            // Send to local subscribers.
            deliverLocally(events);
        }
    }

    private void deliverLocally(Collection<DbEvent> lst)
    {
        Multimap<String, DbEvent> distribution = HashMultimap.create();

        for (DbEvent obj : lst)
        {
            LoggerInstance.debugObnoxious("DBEVENT: %s:%s - %s - %s", obj.context.getTable(), obj.context.sysId, obj.action, obj.context.lastUpdate);

            boolean debugEnabled        = LoggerInstance.isEnabled(Severity.Debug);
            boolean debugVerboseEnabled = LoggerInstance.isEnabled(Severity.DebugVerbose);

            if (debugEnabled || debugVerboseEnabled)
            {
                if (m_deliveryStats == null)
                {
                    m_deliveryStats = Maps.newHashMap();
                }
            }
            else
            {
                m_deliveryStats = null;
            }

            Map<String, AtomicInteger> deliveryStats = m_deliveryStats;

            for (String endpoint : getSubscribers(obj.context.getTable(), obj.context.lastUpdate))
            {
                if (deliveryStats != null)
                {
                    logDelivery(deliveryStats, debugVerboseEnabled, obj.context, endpoint);
                }

                distribution.put(endpoint, obj);
            }

            for (String endpoint : getSubscribers("*", obj.context.lastUpdate))
            {
                if (deliveryStats != null)
                {
                    logDelivery(deliveryStats, debugVerboseEnabled, obj.context, endpoint);
                }

                distribution.put(endpoint, obj);
            }
        }

        for (String endpoint : distribution.keySet())
        {
            sendMessage(endpoint, distribution.get(endpoint));
        }
    }

    private void logDelivery(Map<String, AtomicInteger> deliveryStats,
                             boolean reportEveryDelivery,
                             RecordIdentity context,
                             String endpoint)
    {
        AtomicInteger count;

        synchronized (deliveryStats)
        {
            count = deliveryStats.computeIfAbsent(endpoint, (key) -> new AtomicInteger());
        }

        int val = count.incrementAndGet();
        if (!reportEveryDelivery && (val % 100) != 0)
        {
            return;
        }

        if (reportEveryDelivery)
        {
            LoggerInstance.debugVerbose("DBEVENT: deliver to %s (hit: %d): %s:%s", endpoint, val, context.getTable(), context.sysId);
        }
        else
        {
            LoggerInstance.debug("DBEVENT: deliver to %s (hit: %d): %s:%s", endpoint, val, context.getTable(), context.sysId);
        }
    }

    private void sendMessage(String destination,
                             Collection<DbEvent> events)
    {
        try
        {
            DbMessage_Event msg = new DbMessage_Event();
            msg.events.addAll(events);
            sendMessageWithNoReply(destination, msg, null);
        }
        catch (Exception e)
        {
            LoggerInstance.error("Failed to deliver DB event due to exception: %s", e);
        }
    }
}
