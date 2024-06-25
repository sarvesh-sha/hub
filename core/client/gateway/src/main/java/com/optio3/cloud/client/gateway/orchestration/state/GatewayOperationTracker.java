/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.client.gateway.model.GatewayOperationStatus;
import com.optio3.cloud.client.gateway.model.GatewayOperationToken;
import com.optio3.collection.TokenTracker;
import com.optio3.logging.ILogger;
import com.optio3.logging.RedirectingLogger;
import com.optio3.util.function.FunctionWithException;

public class GatewayOperationTracker extends TokenTracker<GatewayOperationToken, GatewayOperationTracker.State>
{
    public class State
    {
        private final String                  m_id;
        private       CompletableFuture<Void> m_operationCancelled;

        private GatewayOperationStatus m_status;
        private Exception              m_failure;
        private Object                 m_value;

        State()
        {
            m_id = String.format("Task%d", m_seq.incrementAndGet());

            m_operationCancelled = new CompletableFuture<>();
            m_status             = GatewayOperationStatus.Initialized;
        }

        public Object getValue()
        {
            return m_value;
        }

        public void setValue(Object value)
        {
            m_value = value;
        }

        public ILogger getContextualLogger(ILogger logger)
        {
            return new RedirectingLogger(logger)
            {
                @Override
                public String getPrefix()
                {
                    return String.format("[%s]", m_id);
                }
            };
        }

        public boolean wasCancelled()
        {
            return m_operationCancelled.isCancelled();
        }

        GatewayOperationStatus getStatus() throws
                                           Exception
        {
            if (m_failure != null)
            {
                throw m_failure;
            }

            return m_status;
        }

        void cancel()
        {
            m_operationCancelled.cancel(false);
        }

        @AsyncBackground
        CompletableFuture<Void> run(FunctionWithException<State, CompletableFuture<Boolean>> worker)
        {
            try
            {
                m_status = GatewayOperationStatus.Executing;

                boolean success = await(worker.apply(this));

                m_status = success ? GatewayOperationStatus.Completed : GatewayOperationStatus.Failed;
            }
            catch (Exception e)
            {
                m_failure = e;
                m_status  = GatewayOperationStatus.Failed;
            }

            return wrapAsync(null);
        }
    }

    private final AtomicInteger m_seq = new AtomicInteger();

    public GatewayOperationTracker()
    {
        super(1, TimeUnit.DAYS);
    }

    @Override
    protected GatewayOperationToken newToken(State payload)
    {
        GatewayOperationToken token = new GatewayOperationToken();
        token.id = payload.m_id;

        return token;
    }

    @Override
    protected String getTokenId(GatewayOperationToken token)
    {
        return token != null ? token.id : null;
    }

    @Override
    protected void releasePayload(State payload)
    {
        payload.m_operationCancelled.cancel(false);
    }

    public GatewayOperationStatus checkOperationStatus(GatewayOperationToken token) throws
                                                                                    Exception
    {
        State state = get(token);
        return state != null ? state.getStatus() : null;
    }

    public CompletableFuture<GatewayOperationToken> trackOperation(FunctionWithException<GatewayOperationTracker.State, CompletableFuture<Boolean>> worker) throws
                                                                                                                                                            Exception
    {
        State state = new State();

        GatewayOperationToken token = register(state);

        state.run(worker);

        return wrapAsync(token);
    }
}