/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.modbus;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.base.Stopwatch;
import com.optio3.asyncawait.AsyncQueue;
import com.optio3.concurrency.AsyncSemaphore;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.protocol.modbus.pdu.ApplicationPDU;
import com.optio3.protocol.model.modbus.error.ModbusFailedException;
import com.optio3.serialization.Reflection;
import com.optio3.stream.OutputBuffer;
import com.optio3.util.Exceptions;

public class ServiceRequestHandle<T extends ApplicationPDU, U extends ApplicationPDU.Response> implements IApplicationPduListener
{
    public static final Logger LoggerInstance = ModbusManager.LoggerInstance.createSubLogger(ServiceRequestHandle.class);

    private static final Duration MaxTimeout         = Duration.of(4, ChronoUnit.SECONDS);
    private static final long     MaxTimeoutMillisec = MaxTimeout.toMillis();

    private final ModbusManager        m_manager;
    private final Integer              m_targetIdentifier;
    private final T                    m_request;
    private final Class<U>             m_responseClass;
    private       CompletableFuture<U> m_response;
    private       Duration             m_requestTiming;

    private final int      m_retries;
    private final long     m_timeout;
    private final TimeUnit m_timeoutUnit;
    private       int      m_failedAttempts;

    private final OutputBuffer m_payload;

    private final AsyncQueue<Callable<U>> m_events = new AsyncQueue<>();

    ServiceRequestHandle(ModbusManager manager,
                         Integer targetIdentifier,
                         T request,
                         int retries,
                         long timeout,
                         TimeUnit unit,
                         Class<U> responseClass)
    {
        m_manager = manager;
        m_targetIdentifier = targetIdentifier;
        m_request = request;
        m_responseClass = responseClass;

        m_retries = retries;
        m_timeout = timeout;
        m_timeoutUnit = unit;

        m_payload = m_request.encode();
    }

    public void cancel()
    {
        if (m_response != null)
        {
            m_response.cancel(false);
        }
    }

    //--//

    public CompletableFuture<U> result() throws
                                         Exception
    {
        if (m_response == null)
        {
            m_response = startInner();
        }

        return m_response;
    }

    public Duration getExecutionTime()
    {
        return m_requestTiming;
    }

    private CompletableFuture<U> startInner() throws
                                              Exception
    {
        Stopwatch swAcquire = Stopwatch.createStarted();
        try (AsyncSemaphore.Holder holder = await(m_manager.acquireAccessToTransport(m_targetIdentifier)))
        {
            LoggerInstance.debugVerbose("[Device %s] Acquired access in %,dmsec",
                                        m_targetIdentifier,
                                        swAcquire.elapsed()
                                                 .toMillis());

            Stopwatch sw = Stopwatch.createStarted();

            try (TransactionIdListener invokeListener = m_manager.registerForTransactionId(m_targetIdentifier, this))
            {
                final int transactionId = invokeListener.getTransactionId();

                LoggerInstance.debugVerbose("[Device %s] %s/%02x ...",
                                            m_targetIdentifier,
                                            m_request.getClass()
                                                     .getSimpleName(),
                                            transactionId);

                while (true)
                {
                    await(m_manager.sendDirect(m_targetIdentifier, transactionId, m_payload));

                    // Increase the timeout based on the number of parallel requests.
                    final int outstandingPermits = holder.getOutstandingPermits();
                    long      timeoutMillisec    = m_timeoutUnit.toMillis(m_timeout * outstandingPermits);

                    // Limit timeout baseline to N seconds.
                    timeoutMillisec = Math.min(timeoutMillisec, MaxTimeoutMillisec);

                    // Increase timeout with each attempt.
                    timeoutMillisec = (long) (timeoutMillisec * (1.0 + m_failedAttempts * 0.5));

                    try
                    {
                        Callable<U> eventCallback = await(m_events.pull(), timeoutMillisec, TimeUnit.MILLISECONDS);

                        U msg = eventCallback.call();
                        if (msg != null)
                        {
                            sw.stop();

                            m_requestTiming = sw.elapsed();

                            dumpRequestInfo(transactionId, timeoutMillisec, sw, false, true);

                            return wrapAsync(msg);
                        }
                    }
                    catch (TimeoutException e)
                    {
                        m_failedAttempts++;

                        if (m_failedAttempts >= m_retries)
                        {
                            dumpRequestInfo(transactionId, timeoutMillisec, sw, false, false);

                            throw Exceptions.newTimeoutException("Timeout while waiting for device '%s' reply to message '%s'", m_targetIdentifier, m_request.getClass());
                        }
                        else
                        {
                            dumpRequestInfo(transactionId, timeoutMillisec, sw, true, false);
                        }
                    }
                }
            }
        }
    }

    private void dumpRequestInfo(int transactionId,
                                 long timeoutMillisec,
                                 Stopwatch sw,
                                 boolean gotTimeout,
                                 boolean gotResult)
    {
        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            final long elapsedMillisec = sw.elapsed(TimeUnit.MILLISECONDS);

            final String requestType = m_request.getClass()
                                                .getSimpleName();

            LoggerInstance.debug("[Device %s] %s/%02x | %s | timeout:%,d requestTime:%,d retries:%d",
                                 m_targetIdentifier,
                                 requestType,
                                 transactionId,
                                 gotResult ? "SUCCESS" : gotTimeout ? "TIMEOUT" : "FAILURE",
                                 timeoutMillisec,
                                 elapsedMillisec,
                                 m_failedAttempts);
        }
    }

    //--//

    @Override
    public void processResponse(int deviceIdentifier,
                                ApplicationPDU.Response res)
    {
        ApplicationPDU.Error apdu_err = Reflection.as(res, ApplicationPDU.Error.class);
        if (apdu_err != null)
        {
            m_events.push(() ->
                          {
                              throw new ModbusFailedException(apdu_err.exceptionCode);
                          });
        }
        else
        {
            m_events.push(() ->
                          {
                              return m_responseClass.cast(res);
                          });
        }
    }
}
