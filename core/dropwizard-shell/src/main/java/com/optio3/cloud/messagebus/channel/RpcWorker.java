/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus.channel;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.cloud.AbstractApplication;
import com.optio3.cloud.AbstractConfiguration;
import com.optio3.cloud.JsonDatagram;
import com.optio3.cloud.messagebus.MessageBusClient;
import com.optio3.cloud.messagebus.MessageBusStatistics;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class RpcWorker
{
    public abstract static class BaseHeartbeat<T extends AbstractApplication<C>, C extends AbstractConfiguration>
    {
        protected final T         m_app;
        private final   Logger    m_loggerInstance;
        private final   RpcWorker m_rpcWorker;
        private final   Runnable  m_callbackBefore;
        private final   Runnable  m_callbackAfter;

        private MonotonousTime          m_nextCheckin;
        private CompletableFuture<Void> m_checkinFuture;
        private int                     m_checkinFailures;

        private int                     m_rescheduleDelay = 1;
        private MonotonousTime          m_nextConnectionAttempt;
        private CompletableFuture<Void> m_connectionFuture;
        private int                     m_connections;
        private int                     m_connectionAttempts;
        private MonotonousTime          m_delayConnectionReport;
        private MonotonousTime          m_nextExceptionReport;
        private String                  m_lastClientId;

        protected BaseHeartbeat(T app,
                                Logger loggerInstance,
                                RpcWorker rpcWorker,
                                Runnable callbackBefore,
                                Runnable callbackAfter)
        {
            m_app                 = app;
            m_loggerInstance      = loggerInstance;
            m_rpcWorker           = rpcWorker;
            m_callbackBefore      = callbackBefore;
            m_callbackAfter       = callbackAfter;
            m_nextExceptionReport = TimeUtils.computeTimeoutExpiration(10, TimeUnit.MINUTES);
        }

        public final CompletableFuture<Void> sendCheckin(boolean force) throws
                                                                        Exception
        {
            if (m_callbackBefore != null)
            {
                m_callbackBefore.run();
            }

            Duration nextChecking = await(sendCheckinInner(force));
            if (nextChecking == null)
            {
                nextChecking = Duration.ofMinutes(15);
            }

            long delay = nextChecking.toSeconds();

            // Randomize by 40%, to make sure all clients don't call at the same time.
            long nextCheckin = (long) (delay * (1 + 0.4 * Math.random()));
            m_nextCheckin = TimeUtils.computeTimeoutExpiration(nextCheckin, TimeUnit.SECONDS);

            if (m_callbackAfter != null)
            {
                m_callbackAfter.run();
            }

            return wrapAsync(null);
        }

        protected abstract CompletableFuture<Duration> sendCheckinInner(boolean force) throws
                                                                                       Exception;

        //--//

        @AsyncBackground
        private CompletableFuture<Void> runLogic(@AsyncDelay long delay,
                                                 @AsyncDelay TimeUnit delayUnit) throws
                                                                                 Exception
        {
            if (m_rpcWorker.m_shutdown)
            {
                return wrapAsync(null);
            }

            //
            // Immediately reschedule us.
            //
            runLogic(m_rescheduleDelay, TimeUnit.SECONDS);

            synchronized (m_rpcWorker.m_lock)
            {
                if (m_rpcWorker.m_socket != null && TimeUtils.isTimeoutExpired(m_nextCheckin))
                {
                    if (m_checkinFuture != null && m_checkinFuture.isDone())
                    {
                        m_checkinFuture = null;
                    }

                    if (m_checkinFuture == null)
                    {
                        m_checkinFuture = performCheckin(); // Don't wait...
                    }
                }

                if (m_rpcWorker.m_socket == null && TimeUtils.isTimeoutExpired(m_nextConnectionAttempt))
                {
                    if (m_connectionFuture != null && m_connectionFuture.isDone())
                    {
                        m_connectionFuture = null;
                    }

                    if (m_connectionFuture == null)
                    {
                        m_connectionFuture = attemptConnection(); // Don't wait...
                    }
                }
            }

            return wrapAsync(null);
        }

        @AsyncBackground(reason = "The caller is holding the RPC lock, we need to continue on a different thread")
        private CompletableFuture<Void> performCheckin() throws
                                                         Exception
        {
            try
            {
                await(sendCheckin(false));

                m_checkinFailures = 0;
                m_rescheduleDelay = 10;
            }
            catch (Throwable t)
            {
                if (!m_rpcWorker.m_shutdown) // An exception is expected in shutdown scenario.
                {
                    if (!(t instanceof TimeoutException))
                    {
                        m_loggerInstance.error("Heartbeat loop got an exception: %s", t);
                    }

                    final int maxFailures = 3;
                    if (++m_checkinFailures >= maxFailures)
                    {
                        m_loggerInstance.debug("Heartbeat got %d failures in a row, closing socket...", maxFailures);
                        m_rpcWorker.closeSocket();
                        m_checkinFailures = 0;
                    }
                    else
                    {
                        m_nextCheckin = TimeUtils.computeTimeoutExpiration(30, TimeUnit.SECONDS);
                    }
                }
            }

            return wrapAsync(null);
        }

        @AsyncBackground(reason = "The caller is holding the RPC lock, we need to continue on a different thread")
        private CompletableFuture<Void> attemptConnection() throws
                                                            Exception
        {
            String connectionDesc = m_rpcWorker.getConnectionUrl();

            try
            {
                MessageBusClient socket = m_rpcWorker.allocateSocket();
                m_rpcWorker.m_socket = socket;

                if (socket.shouldUpgrade())
                {
                    if (socket.prepareUpgrade(m_rpcWorker))
                    {
                        m_rpcWorker.closeSocket();

                        return attemptConnection();
                    }
                }

                connectionDesc = socket.describeConnection();
                LoggerInstance.debug("Trying to connect to '%s'", connectionDesc);

                socket.startConnection();
                m_lastClientId = await(socket.getEndpointId(), 2, TimeUnit.MINUTES);

                if (TimeUtils.isTimeoutExpired(m_delayConnectionReport))
                {
                    LoggerInstance.info("Connected to '%s' as '%s' (%d attempts, %d total connections)", connectionDesc, m_lastClientId, m_connectionAttempts + 1, m_connections + 1);
                }

                m_delayConnectionReport = null;

                if (socket.shouldUpgrade())
                {
                    if (socket.prepareUpgrade(m_rpcWorker))
                    {
                        m_rpcWorker.closeSocket();

                        return attemptConnection();
                    }
                }

                //
                // Create the RPC service using a remote channel subscriber, and allow incoming requests.
                //
                RpcClient client = new RpcClient(m_rpcWorker.m_rpcContext);

                await(socket.join(client));

                m_rpcWorker.setRpcContext(client, this);

                m_rpcWorker.onSuccess();

                m_connections++;
                m_nextCheckin = null;

                await(socket.onDisconnected());

                m_connectionAttempts    = 0;
                m_nextConnectionAttempt = null;
                m_nextExceptionReport   = TimeUtils.computeTimeoutExpiration(3, TimeUnit.HOURS);
            }
            catch (Throwable t)
            {
                // Catch any issue.
                m_connectionAttempts++;

                int wait = Math.min(m_connectionAttempts, 15);
                if (m_rpcWorker.isCellularConnection())
                {
                    if (wait < 6)
                    {
                        m_nextConnectionAttempt = TimeUtils.computeTimeoutExpiration(10L * wait, TimeUnit.SECONDS);
                    }
                    else
                    {
                        m_nextConnectionAttempt = TimeUtils.computeTimeoutExpiration(wait - 5L, TimeUnit.MINUTES);
                    }
                }
                else
                {
                    m_nextConnectionAttempt = TimeUtils.computeTimeoutExpiration(wait, TimeUnit.SECONDS);
                }

                if (TimeUtils.isTimeoutExpired(m_nextExceptionReport))
                {
                    LoggerInstance.info("Failed to connect to '%s' after %d attempts, due to %s", connectionDesc, m_connectionAttempts, t);

                    m_nextExceptionReport = TimeUtils.computeTimeoutExpiration(3, TimeUnit.HOURS);
                }
            }

            m_rescheduleDelay = 1;
            m_rpcWorker.closeSocket();

            if (!m_rpcWorker.m_shutdown)
            {
                m_rpcWorker.onFailure();
            }

            if (m_lastClientId != null)
            {
                if (m_delayConnectionReport == null)
                {
                    // If we just got disconnected, don't report connection immediately.
                    m_delayConnectionReport = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);
                }

                if (TimeUtils.isTimeoutExpired(m_delayConnectionReport))
                {
                    LoggerInstance.info("Disconnected from '%s' as '%s'", m_rpcWorker.getConnectionUrl(), m_lastClientId);

                    m_delayConnectionReport = null; // Immediately report connection results.
                    m_lastClientId          = null;
                }
            }

            return wrapAsync(null);
        }
    }

    //--//

    public static final Logger LoggerInstance = new Logger(RpcWorker.class, true);

    private final Object     m_lock = new Object();
    private final RpcContext m_rpcContext;

    private           Thread           m_keepServerAlive;
    private transient boolean          m_shutdown;
    private           BaseHeartbeat    m_hb;
    private           MessageBusClient m_socket;

    //--//

    protected RpcWorker(RpcContext rpcContext)
    {
        m_rpcContext = rpcContext;
    }

    public void startLoop()
    {
        synchronized (m_lock)
        {
            if (m_hb != null)
            {
                throw new RuntimeException("Already started");
            }

            m_hb = createHeartbeat();

            //
            // Silly thing: we are fully asynchronous, so the JVM doesn't see any active thread and wants to shut down.
            // We are forced to create a idle thread just to keep running...
            //
            m_keepServerAlive = new Thread(this::keepServerAlive);
            m_keepServerAlive.setName("RPC KeepServerAlive");
            m_keepServerAlive.start();
        }

        try
        {
            // Kickstart logic.
            m_hb.runLogic(2, TimeUnit.SECONDS);
        }
        catch (Throwable t)
        {
            // This should never happen, runLogic is async and we don't wait on it.
            LoggerInstance.error("runLogic failed with %s", t);
        }
    }

    public void stopLoop()
    {
        synchronized (m_lock)
        {
            m_hb       = null;
            m_shutdown = true;

            closeSocket();

            m_lock.notifyAll();
        }

        try
        {
            m_keepServerAlive.join(10 * 1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

    public int getNumberOfConnections()
    {
        return m_hb != null ? m_hb.m_connections : 0;
    }

    public MessageBusStatistics sampleMessageBusStatistics()
    {
        return m_socket != null ? m_socket.sampleStatistics() : null;
    }

    private void keepServerAlive()
    {
        while (!m_shutdown)
        {
            synchronized (m_lock)
            {
                try
                {
                    m_lock.wait();
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    private void closeSocket()
    {
        setRpcContext(null, null);

        MessageBusClient socket;

        synchronized (m_lock)
        {
            socket   = m_socket;
            m_socket = null;
        }

        if (socket != null)
        {
            Executors.closeWithTimeout(socket::closeConnection, 2 * 60 * 1000, (t) ->
            {
                LoggerInstance.error("Failed to close socket, due to %s", t);
            });
        }
    }

    protected abstract void setRpcContext(RpcClient rpcClient,
                                          BaseHeartbeat hb);

    protected abstract boolean isCellularConnection();

    protected abstract MessageBusClient allocateSocket();

    protected abstract String getConnectionUrl();

    protected abstract BaseHeartbeat<?, ?> createHeartbeat();

    protected abstract void onSuccess();

    protected abstract void onFailure();

    public abstract boolean prepareUpgrade(JsonDatagram.SessionConfiguration session);
}
