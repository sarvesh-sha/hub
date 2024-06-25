/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.gateway.logic;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.cloud.client.gateway.model.LogEntry;
import com.optio3.cloud.client.gateway.model.prober.ProberOperation;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationStatus;
import com.optio3.cloud.client.gateway.model.prober.ProberOperationToken;
import com.optio3.cloud.gateway.GatewayApplication;
import com.optio3.collection.TokenTracker;
import com.optio3.logging.ILogger;
import com.optio3.logging.ILoggerAppender;
import com.optio3.logging.LoggerFactory;
import com.optio3.logging.RedirectingLogger;
import com.optio3.logging.Severity;
import com.optio3.util.function.FunctionWithException;

public class ProberOperationTracker extends TokenTracker<ProberOperationToken, ProberOperationTracker.State>
{
    public class State
    {
        private final String                  m_id;
        private final ProberOperation         m_input;
        private final CompletableFuture<Void> m_operationCancelled;

        private ProberOperationStatus       m_status;
        private ProberOperation.BaseResults m_output;

        private final LinkedList<LogEntry> m_logEntries = new LinkedList<>();

        State(ProberOperation input)
        {
            m_id = String.format("Probe%d", m_seq.incrementAndGet());

            m_operationCancelled = new CompletableFuture<>();
            m_status             = ProberOperationStatus.Initialized;

            m_input = input;
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

        ProberOperationStatus getStatus()
        {
            return m_status;
        }

        public ProberOperation.BaseResults getOutput()
        {
            return m_output;
        }

        void cancel()
        {
            m_operationCancelled.cancel(false);
        }

        @AsyncBackground
        CompletableFuture<Void> run(FunctionWithException<ProberOperation, CompletableFuture<ProberOperation.BaseResults>> worker)
        {
            ILoggerAppender appender = new ILoggerAppender()
            {
                @Override
                public boolean append(ILogger context,
                                      ZonedDateTime timestamp,
                                      Severity level,
                                      String thread,
                                      String selector,
                                      String msg)
                {
                    if (context.canForwardToRemote())
                    {
                        LogEntry en = new LogEntry();
                        en.timestamp = timestamp;
                        en.level     = level;
                        en.thread    = thread;
                        en.selector  = selector;
                        en.line      = msg;

                        synchronized (m_logEntries)
                        {
                            m_logEntries.add(en);
                        }

                        return true; // Done, stop propagating entry.
                    }

                    return false; // Allow other appenders to see entry.
                }
            };

            try
            {
                LoggerFactory.registerAppender(appender);

                m_status = ProberOperationStatus.Executing;

                m_output = await(worker.apply(m_input));

                m_status = m_output != null ? ProberOperationStatus.Completed : ProberOperationStatus.Failed;
            }
            catch (Exception e)
            {
                m_status = ProberOperationStatus.Failed;

                GatewayApplication.LoggerInstance.error("Execution of '%s' failed due to %s", m_input.getClass(), e);
            }
            finally
            {
                LoggerFactory.unregisterAppender(appender);
            }

            return wrapAsync(null);
        }

        private CompletableFuture<ProberOperationStatus> flushOutput(FunctionWithException<List<LogEntry>, CompletableFuture<Void>> callback) throws
                                                                                                                                              Exception
        {
            List<LogEntry> candidates = Lists.newArrayList();

            while (true)
            {
                candidates.clear();

                int queued = 0;

                synchronized (m_logEntries)
                {
                    int threshold = 50;

                    for (LogEntry entry : m_logEntries)
                    {
                        candidates.add(entry);
                        queued++;

                        if (--threshold == 0)
                        {
                            break;
                        }
                    }
                }

                if (candidates.size() == 0)
                {
                    break;
                }

                await(callback.apply(candidates));

                synchronized (m_logEntries)
                {
                    for (int i = 0; i < queued && !m_logEntries.isEmpty(); i++)
                    {
                        m_logEntries.removeFirst();
                    }
                }
            }

            return wrapAsync(getStatus());
        }
    }

    private final AtomicInteger m_seq = new AtomicInteger();

    public ProberOperationTracker()
    {
        super(1, TimeUnit.DAYS);
    }

    @Override
    protected ProberOperationToken newToken(State payload)
    {
        ProberOperationToken token = new ProberOperationToken();
        token.id = payload.m_id;

        return token;
    }

    @Override
    protected String getTokenId(ProberOperationToken token)
    {
        return token != null ? token.id : null;
    }

    @Override
    protected void releasePayload(State payload)
    {
        payload.m_operationCancelled.cancel(false);
    }

    public CompletableFuture<ProberOperationStatus> checkOperationStatus(ProberOperationToken token,
                                                                         FunctionWithException<List<LogEntry>, CompletableFuture<Void>> output) throws
                                                                                                                                                Exception
    {
        State state = get(token);
        if (state == null)
        {
            return wrapAsync(null);
        }

        return state.flushOutput(output);
    }

    public CompletableFuture<ProberOperationToken> trackOperation(ProberOperation input,
                                                                  FunctionWithException<ProberOperation, CompletableFuture<ProberOperation.BaseResults>> worker)
    {
        State state = new State(input);

        ProberOperationToken token = register(state);

        state.run(worker);

        return wrapAsync(token);
    }
}