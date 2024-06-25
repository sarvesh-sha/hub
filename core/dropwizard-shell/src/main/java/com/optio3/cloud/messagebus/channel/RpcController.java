/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JavaType;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.messagebus.MessageBusChannelHandler;
import com.optio3.cloud.messagebus.payload.MbData_Message;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessageReply;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage_Call;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage_Call_Reply;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage_Callback;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage_Ping;
import com.optio3.cloud.messagebus.payload.rpc.RpcMessage_Ping_Reply;
import com.optio3.cloud.messagebus.transport.Endpoint;
import com.optio3.cloud.messagebus.transport.StableIdentity;
import com.optio3.cloud.remoting.CallDispatcher;
import com.optio3.cloud.remoting.CallMarshaller;
import com.optio3.cloud.remoting.LocalCall;
import com.optio3.cloud.remoting.RemoteArgument;
import com.optio3.cloud.remoting.RemoteCallDescriptor;
import com.optio3.cloud.remoting.RemoteResult;
import com.optio3.logging.Logger;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

class RpcController
{
    public static final Logger LoggerInstance = RpcChannel.LoggerInstance.createSubLogger(RpcController.class);

    //
    // Converts calls from an Interface Proxy into RPC messages.
    //
    class Dispatcher extends CallDispatcher
    {
        private final String   m_destinationHost;
        private final String   m_destinationInstance;
        private final int      m_timeout;
        private final TimeUnit m_timeoutUnit;

        Dispatcher(String destinationHost,
                   String destinationInstance,
                   int timeout,
                   TimeUnit timeoutUnit)
        {
            m_destinationHost     = destinationHost;
            m_destinationInstance = destinationInstance;
            m_timeout             = timeout;
            m_timeoutUnit         = timeoutUnit;
        }

        @Override
        @AsyncBackground(reason = "We don't want to process the reply on the WebSocket thread")
        public CompletableFuture<?> send(RemoteCallDescriptor rc,
                                         Object[] originalArguments) throws
                                                                     Throwable
        {
            try (CallState cs = new CallState(rc, originalArguments, m_timeout, m_timeoutUnit))
            {
                LoggerInstance.debugVerbose("TX(%s/%s) ==> %s", m_destinationHost, cs.callId, rc);

                RpcMessage_Call req = new RpcMessage_Call();
                req.callId      = cs.callId;
                req.instanceId  = m_destinationInstance;
                req.descriptor  = rc;
                req.timeout     = m_timeout;
                req.timeoutUnit = m_timeoutUnit;

                Endpoint       ep = m_handler.getEndpointForDestination(m_destinationHost);
                StableIdentity s  = ep != null ? ep.getIdentity() : null;
                if (s != null && s.logger != null)
                {
                    s.logger.info("Outbound Call (%s/%s)\n%s", m_destinationHost, cs.callId, prettyPrintAsJson(rc));
                }

                Stopwatch st = Stopwatch.createStarted();

                CompletableFuture<RpcMessage_Call_Reply> promise = m_handler.sendMessageWithReply(m_destinationHost, req, RpcMessage_Call_Reply.class, (size) ->
                {
                    m_statistics.record(rc, RpcChannel.Statistics.Event.OutgoingCall, size);
                }, m_timeout, m_timeoutUnit);

                RpcMessage_Call_Reply reply = await(promise);

                m_statistics.record(rc, RpcChannel.Statistics.Event.OutgoingCallTime, st.elapsed(TimeUnit.MILLISECONDS));

                if (s != null && s.logger != null)
                {
                    s.logger.info("Outbound Call Result (%s/%s)\n%s", m_destinationHost, cs.callId, prettyPrintAsJson(reply.result));
                }

                LoggerInstance.debugVerbose("TX(%s/%s) <== %s", m_destinationHost, cs.callId, reply.result);

                CallMarshaller marshaller = m_rpcContext.getCallMarshaller();
                return wrapAsync(marshaller.decodeResult(reply.result));
            }
        }
    }

    //
    // Keeps track of arguments and callbacks for an RPC call.
    //
    class CallState implements AutoCloseable
    {
        //
        // Handles callbacks on the receiver side, converting them into RPC messages.
        //
        class DispatcherForCallback extends CallDispatcher
        {
            private final String m_callbackId;

            DispatcherForCallback(String callbackId)
            {
                m_callbackId = callbackId;
            }

            @Override
            @AsyncBackground(reason = "We don't want to process the reply on the WebSocket thread")
            public CompletableFuture<?> send(RemoteCallDescriptor rc,
                                             Object[] originalArguments) throws
                                                                         Throwable
            {
                RpcMessage_Callback req = new RpcMessage_Callback();
                req.callId     = callId;
                req.callbackId = m_callbackId;
                req.descriptor = rc;

                Stopwatch st = Stopwatch.createStarted();

                CompletableFuture<RpcMessage_Call_Reply> promise = m_handler.sendMessageWithReply(m_source, req, RpcMessage_Call_Reply.class, (size) ->
                {
                    m_statistics.record(m_rc, RpcChannel.Statistics.Event.OutgoingCallback, size);
                }, m_timeout, m_timeoutUnit);

                RpcMessage_Call_Reply reply = await(promise);

                m_statistics.record(m_rc, RpcChannel.Statistics.Event.OutgoingCallbackTime, st.elapsed(TimeUnit.MILLISECONDS));

                CallMarshaller marshaller = m_rpcContext.getCallMarshaller();
                return wrapAsync(marshaller.decodeResult(reply.result));
            }
        }

        final RemoteCallDescriptor m_rc;
        final String               m_source;
        final String               callId;
        final int                  m_timeout;
        final TimeUnit             m_timeoutUnit;
        final Map<String, Object>  m_callbacks;

        //
        // Constructor for the sender side, which keeps track of callback arguments.
        //
        CallState(RemoteCallDescriptor rc,
                  Object[] originalArguments,
                  int timeout,
                  TimeUnit timeoutUnit)
        {
            m_rc          = rc;
            m_source      = null;
            callId        = IdGenerator.newGuid();
            m_timeout     = timeout;
            m_timeoutUnit = timeoutUnit;

            Map<String, Object> callbacks = Collections.emptyMap();

            for (int i = 0; i < originalArguments.length; i++)
            {
                String callbackId = rc.parameters.get(i).callbackId;
                if (callbackId != null)
                {
                    if (callbacks.isEmpty())
                    {
                        callbacks = Maps.newHashMap();
                    }

                    callbacks.put(callbackId, originalArguments[i]);
                }
            }

            m_callbacks = callbacks;

            register(callId, this);
        }

        //
        // Constructor for the receiver side, which doesn't need to keep track of callback arguments, but needs to make callbacks. 
        //
        CallState(String source,
                  RpcMessage_Call info)
        {
            m_rc          = info.descriptor;
            m_source      = source;
            callId        = info.callId;
            m_timeout     = info.timeout;
            m_timeoutUnit = info.timeoutUnit;
            m_callbacks   = Collections.emptyMap();

            register(callId, this);
        }

        @Override
        public void close()
        {
            unregister(callId);
        }

        //--//

        Object createCallback(Type paramType,
                              String callbackId)
        {
            DispatcherForCallback dispatcher     = new DispatcherForCallback(callbackId);
            CallMarshaller        callMarshaller = m_rpcContext.getCallMarshaller();

            return callMarshaller.createRemotableProxy(paramType, true, dispatcher);
        }
    }

    //--//

    private final RpcContext                                            m_rpcContext;
    private final MessageBusChannelHandler<RpcMessage, RpcMessageReply> m_handler;

    private final Map<String, CallState> m_callStates = Maps.newHashMap();
    private final RpcChannel.Statistics  m_statistics = new RpcChannel.Statistics();

    RpcController(RpcContext rpcContext,
                  MessageBusChannelHandler<RpcMessage, RpcMessageReply> handler)
    {
        m_rpcContext = rpcContext;
        m_handler    = handler;
    }

    List<String> reportStatistics()
    {
        return m_statistics.report();
    }

    //--//

    CompletableFuture<Boolean> waitForDestination(String destination,
                                                  int timeout,
                                                  TimeUnit unit)
    {
        Endpoint ep                  = m_handler.getEndpointForDestination(destination);
        long     lastMessageMilliUTC = ep != null ? ep.getTimestampOfLastIncomingMessage() : 0;

        if (TimeUtils.wasUpdatedRecently(lastMessageMilliUTC, 15, TimeUnit.SECONDS))
        {
            return wrapAsync(true);
        }

        try
        {
            RpcMessage_Ping req = new RpcMessage_Ping();

            await(m_handler.sendMessageWithReply(destination, req, RpcMessage_Ping_Reply.class, null, timeout, unit));
            // If we get here, it means we were able to reach the destination.

            return wrapAsync(true);
        }
        catch (Throwable t)
        {
            return wrapAsync(false);
        }
    }

    <P> P createProxy(String destinationHost,
                      String destinationInstance,
                      Class<P> itf,
                      int timeout,
                      TimeUnit timeoutUnit)
    {
        Dispatcher     dispatcher     = new Dispatcher(destinationHost, destinationInstance, timeout, timeoutUnit);
        CallMarshaller callMarshaller = m_rpcContext.getCallMarshaller();

        return callMarshaller.createRemotableProxy(itf, false, dispatcher);
    }

    void registerInstance(String instanceId,
                          Object target)
    {
        m_rpcContext.registerInstance(instanceId, target);
    }

    void unregisterInstance(String instanceId)
    {
        m_rpcContext.unregisterInstance(instanceId);
    }

    CompletableFuture<Void> processRpcRequest(MbData_Message data,
                                              RpcMessage obj) throws
                                                              Exception
    {
        RpcMessage_Ping ping = Reflection.as(obj, RpcMessage_Ping.class);
        if (ping != null)
        {
            return invokeAndReply(data, ping);
        }

        RpcMessage_Call call = Reflection.as(obj, RpcMessage_Call.class);
        if (call != null)
        {
            return invokeAndReply(data, call);
        }

        RpcMessage_Callback callback = Reflection.as(obj, RpcMessage_Callback.class);
        if (callback != null)
        {
            CallState cs = getCallState(callback.callId);
            if (cs != null)
            {
                return invokeCallbackAndReply(data, cs, callback.callbackId, callback.descriptor);
            }
        }

        return AsyncRuntime.NullResult;
    }

    //--//

    private void register(String callId,
                          CallState cs)
    {
        synchronized (m_callStates)
        {
            m_callStates.put(callId, cs);
        }
    }

    private void unregister(String callId)
    {
        synchronized (m_callStates)
        {
            m_callStates.remove(callId);
        }
    }

    private CallState getCallState(String id)
    {
        synchronized (m_callStates)
        {
            return m_callStates.get(id);
        }
    }

    //--//

    private CompletableFuture<Void> invokeAndReply(MbData_Message data,
                                                   RpcMessage_Ping ping) throws
                                                                         Exception
    {
        RpcMessage_Ping_Reply reply = new RpcMessage_Ping_Reply();

        return m_handler.replyToMessage(data, reply, null);
    }

    @AsyncBackground(reason = "We don't want to process the reply on the WebSocket thread")
    private CompletableFuture<Void> invokeAndReply(MbData_Message data,
                                                   RpcMessage_Call call) throws
                                                                         Exception
    {
        m_statistics.record(call.descriptor, RpcChannel.Statistics.Event.IncomingCall, data.messageSize);
        Stopwatch st = Stopwatch.createStarted();

        CompletableFuture<RpcMessage_Call_Reply> res = invoke(data.physicalConnection, data.endpoint, data.origin, call);

        RpcMessage_Call_Reply reply = await(res);

        m_statistics.record(call.descriptor, RpcChannel.Statistics.Event.IncomingCallTime, st.elapsed(TimeUnit.MILLISECONDS));

        return m_handler.replyToMessage(data, reply, (size) ->
        {
            m_statistics.record(call.descriptor, RpcChannel.Statistics.Event.IncomingCallResult, size);
        });
    }

    @AsyncBackground(reason = "We don't want to process the incoming message on the WebSocket thread")
    private CompletableFuture<RpcMessage_Call_Reply> invoke(InetSocketAddress physicalConnection,
                                                            Endpoint endpoint,
                                                            String origin,
                                                            RpcMessage_Call call)
    {
        LoggerInstance.debugVerbose("RX(%s/%s) ==> %s", origin, call.callId, call.descriptor);

        CallMarshaller marshaller = m_rpcContext.getCallMarshaller();

        RpcMessage_Call_Reply reply = new RpcMessage_Call_Reply();

        try
        {
            try (CallState cs = new CallState(origin, call))
            {
                RpcOrigin rpcOrigin = new RpcOrigin()
                {
                    @Override
                    public String getRpcId()
                    {
                        return origin;
                    }

                    @Override
                    public StableIdentity getIdentity(String id)
                    {
                        if (endpoint == null)
                        {
                            return null;
                        }

                        StableIdentity s = endpoint.ensureIdentity(id);

                        if (physicalConnection != null)
                        {
                            s.lastKnownConnection = physicalConnection;
                        }

                        return s;
                    }

                    @Override
                    public void setContext(String sysId,
                                           String instanceId)
                    {
                        if (endpoint != null)
                        {
                            endpoint.setContext(sysId, instanceId);
                        }
                    }

                    @Override
                    public String getContextRecordId()
                    {
                        if (endpoint == null)
                        {
                            return null;
                        }

                        return endpoint.getContextRecordId();
                    }

                    @Override
                    public String getContextInstanceId()
                    {
                        if (endpoint == null)
                        {
                            return null;
                        }

                        return endpoint.getContextInstanceId();
                    }
                };

                LocalCall lc = marshaller.decode(call.descriptor, (Type paramType, RemoteArgument paramVal) ->
                {
                    return cs.createCallback(paramType, paramVal.callbackId);
                });

                RemoteOriginProvider.set(rpcOrigin);

                Object target;

                if (call.instanceId != null)
                {
                    target = m_rpcContext.resolveInstance(call.instanceId);
                }
                else
                {
                    target = lc.instantiateTarget(m_rpcContext.getInjectionManager());
                }

                RemoteOriginProvider.set(null);

                StableIdentity s = endpoint != null ? endpoint.getIdentity() : null;
                if (s != null && s.logger != null)
                {
                    s.logger.info("Inbound Call (%s/%s)\n%s", origin, call.callId, prettyPrintAsJson(call.descriptor));
                }

                reply.result = await(makeLocalCallAndEncodeResult(marshaller, lc, target));

                if (s != null && s.logger != null)
                {
                    s.logger.info("Inbound Call Result (%s/%s)\n%s", origin, call.callId, prettyPrintAsJson(reply.result));
                }
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.debug("Encountered error for (%s/%s) while invoking %s: %s", origin, call.callId, call.descriptor, t);

            reply.result = marshaller.encodeResult(null, null, t);
        }

        LoggerInstance.debugVerbose("RX(%s/%s) <== %s", origin, call.callId, reply.result);

        return wrapAsync(reply);
    }

    //--//

    @AsyncBackground(reason = "We don't want to continue processing after the callback on the WebSocket thread")
    private CompletableFuture<Void> invokeCallbackAndReply(MbData_Message data,
                                                           CallState cs,
                                                           String callbackId,
                                                           RemoteCallDescriptor rc) throws
                                                                                    Exception
    {
        CallMarshaller marshaller = m_rpcContext.getCallMarshaller();

        RpcMessage_Call_Reply reply = new RpcMessage_Call_Reply();

        try
        {
            Object target = cs.m_callbacks.get(callbackId);

            CallMarshaller subMarshaller = new CallMarshaller()
            {
                @Override
                public JavaType decodeProxyId(String id)
                {
                    JavaType javaType = marshaller.decodeTypeId(id);
                    if (!javaType.getRawClass()
                                 .isInstance(target))
                    {
                        throw Exceptions.newIllegalArgumentException("Callback type '%s' doesn't match object type '%s'", javaType, target.getClass());
                    }

                    return javaType;
                }
            };

            LocalCall lc = subMarshaller.decode(rc, null);

            m_statistics.record(rc, RpcChannel.Statistics.Event.IncomingCallback, data.messageSize);

            StableIdentity s = data.endpoint != null ? data.endpoint.getIdentity() : null;
            if (s != null && s.logger != null)
            {
                s.logger.info("Callback (%s/%s)\n%s", cs.callId, callbackId, prettyPrintAsJson(rc));
            }

            Stopwatch st = Stopwatch.createStarted();

            reply.result = await(makeLocalCallAndEncodeResult(subMarshaller, lc, target));

            m_statistics.record(rc, RpcChannel.Statistics.Event.IncomingCallbackTime, st.elapsed(TimeUnit.MILLISECONDS));

            if (s != null && s.logger != null)
            {
                s.logger.info("Callback Result (%s/%s)\n%s", cs.callId, callbackId, prettyPrintAsJson(reply.result));
            }
        }
        catch (Throwable t)
        {
            LoggerInstance.debug("Encountered error invoking callback on %s: %s", rc, t);

            reply.result = marshaller.encodeResult(null, null, t);
        }

        return m_handler.replyToMessage(data, reply, (size) ->
        {
            m_statistics.record(rc, RpcChannel.Statistics.Event.IncomingCallbackResult, size);
        });
    }

    //--//

    private static CompletableFuture<RemoteResult> makeLocalCallAndEncodeResult(CallMarshaller marshaller,
                                                                                LocalCall lc,
                                                                                Object target) throws
                                                                                               Throwable,
                                                                                               Exception
    {
        Object callResult = lc.invoke(target);

        CompletableFuture<?> promise = Reflection.as(callResult, CompletableFuture.class);
        if (promise != null)
        {
            callResult = await(promise);
        }

        Type         returnType = Reflection.getTypeArgument(lc.returnType, 0);
        RemoteResult result     = marshaller.encodeResult(returnType, callResult, null);

        return wrapAsync(result);
    }

    //--//

    private String prettyPrintAsJson(Object val)
    {
        String res = ObjectMappers.prettyPrintAsJson(val);

        StringBuilder sb = new StringBuilder();
        for (String s : StringUtils.split(res, '\n'))
        {
            if (s.length() > 200)
            {
                sb.append(s, 0, 200);
                sb.append("...");
            }
            else
            {
                sb.append(s);
            }

            sb.append("\n");
        }

        return sb.toString();
    }
}
