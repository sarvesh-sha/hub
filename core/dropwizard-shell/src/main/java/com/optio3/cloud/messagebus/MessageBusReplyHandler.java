/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.logging.Logger;
import com.optio3.util.concurrency.TypedCompletableFuture;

public abstract class MessageBusReplyHandler
{
    public static final Logger LoggerInstance = MessageBusBroker.LoggerInstance.createSubLogger(MessageBusReplyHandler.class);

    private final ContextualLogger contextualLogger = new ContextualLogger(LoggerInstance)
    {
        @Override
        public String getPrefix()
        {
            return MessageBusBroker.formatPrefix("REPLY", getContextId());
        }
    };

    //--//

    private final Map<String, TypedCompletableFuture<? extends MessageBusPayload>> m_pendingReplies = Maps.newHashMap();
    private       TimeoutException                                                 m_closed;

    //--//

    public abstract String getContextId();

    public void close()
    {
        if (m_closed == null)
        {
            m_closed = new TimeoutException("Closed");
        }

        for (TypedCompletableFuture<?> pending : removeAllReplies())
        {
            pending.completeExceptionally(m_closed);
        }
    }

    //--//

    public boolean handleReply(MessageBusPayload req)
    {
        TypedCompletableFuture<? extends MessageBusPayload> reply = removeReply(req.messageId);
        if (reply == null)
        {
            return false;
        }

        if (getContextId() != null)
        {
            contextualLogger.debugVerbose("[MSG: %s] handleReply: %s", req.messageId, req);
        }

        reply.tryCompleteWithCast(req);
        return true;
    }

    public <T extends MessageBusPayload> CompletableFuture<T> createNewReplyHandler(Class<T> replyClass,
                                                                                    int timeoutForReply,
                                                                                    TimeUnit timeoutUnit,
                                                                                    String messageId) throws
                                                                                                      Exception
    {
        try
        {
            if (getContextId() != null)
            {
                contextualLogger.debugVerbose("[MSG: %s] createNewReplyId: waiting for %s", messageId, replyClass.getName());
            }

            TypedCompletableFuture<T> replyFuture = newReply(messageId, replyClass);

            T reply = await(replyFuture, timeoutForReply, timeoutUnit);

            if (getContextId() != null)
            {
                contextualLogger.debugVerbose("[MSG: %s] createNewReplyId: got reply: %s", messageId, reply);
            }

            return wrapAsync(reply);
        }
        finally
        {
            removeReply(messageId);
        }
    }

    //--//

    private List<TypedCompletableFuture<?>> removeAllReplies()
    {
        synchronized (m_pendingReplies)
        {
            List<TypedCompletableFuture<?>> replies = Lists.newArrayList(m_pendingReplies.values());

            m_pendingReplies.clear();

            return replies;
        }
    }

    private TypedCompletableFuture<? extends MessageBusPayload> removeReply(String id)
    {
        synchronized (m_pendingReplies)
        {
            return m_pendingReplies.remove(id);
        }
    }

    private <T extends MessageBusPayload> TypedCompletableFuture<T> newReply(String messageId,
                                                                             Class<T> clz)
    {
        TypedCompletableFuture<T> replyFuture = new TypedCompletableFuture<T>(clz);

        if (m_closed != null)
        {
            replyFuture.completeExceptionally(m_closed);
        }
        else
        {
            synchronized (m_pendingReplies)
            {
                m_pendingReplies.put(messageId, replyFuture);
            }
        }

        return replyFuture;
    }
}
