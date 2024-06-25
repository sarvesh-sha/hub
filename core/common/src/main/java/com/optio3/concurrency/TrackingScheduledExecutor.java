/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.lang.management.ThreadInfo;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Sets;
import com.optio3.logging.Logger;
import com.optio3.logging.Severity;
import com.optio3.serialization.Reflection;
import com.optio3.util.StackTraceAnalyzer;

class TrackingScheduledExecutor implements ScheduledExecutorService
{
    public static final Logger LoggerInstance = new Logger(TrackingScheduledExecutor.class);

    private class RunnableTracker implements Runnable
    {
        private final Runnable m_target;

        private RunnableTracker(Runnable target)
        {
            m_target = target;
        }

        @Override
        public void run()
        {
            synchronized (m_lock)
            {
                m_executingRunnables.add(this);
            }

            try
            {
                m_target.run();
            }
            finally
            {
                synchronized (m_lock)
                {
                    m_executingRunnables.remove(this);
                }
            }
        }

        @Override
        public String toString()
        {
            return m_target.toString();
        }
    }

    private class CallableTracker<T> implements Callable<T>
    {
        private final Callable<T> m_target;

        private CallableTracker(Callable<T> target)
        {
            m_target = target;
        }

        @Override
        public T call() throws
                        Exception
        {
            synchronized (m_lock)
            {
                m_executingCallables.add(this);
            }

            try
            {
                return m_target.call();
            }
            finally
            {
                synchronized (m_lock)
                {
                    m_executingCallables.remove(this);
                }
            }
        }

        @Override
        public String toString()
        {
            return m_target.toString();
        }
    }

    private static final int      c_minThreshold = 30;
    private static final Duration c_decayTime    = Duration.of(10, ChronoUnit.MINUTES);

    private final ScheduledThreadPoolExecutor m_delegate;
    private final Object                      m_lock;
    private final Set<RunnableTracker>        m_executingRunnables;
    private final Set<CallableTracker<?>>     m_executingCallables;
    private       int                         m_maximumSize;
    private       Instant                     m_nextReport;

    TrackingScheduledExecutor(ScheduledThreadPoolExecutor delegate)
    {
        m_delegate = delegate;
        m_lock = new Object();
        m_executingRunnables = Sets.newHashSet();
        m_executingCallables = Sets.newHashSet();
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command,
                                       long delay,
                                       TimeUnit unit)
    {
        return monitorSize(m_delegate.schedule(new RunnableTracker(command), delay, unit));
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable,
                                           long delay,
                                           TimeUnit unit)
    {
        return monitorSize(m_delegate.schedule(new CallableTracker<>(callable), delay, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit)
    {
        return monitorSize(m_delegate.scheduleAtFixedRate(new RunnableTracker(command), initialDelay, period, unit));
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
                                                     long initialDelay,
                                                     long delay,
                                                     TimeUnit unit)
    {
        return monitorSize(m_delegate.scheduleWithFixedDelay(new RunnableTracker(command), initialDelay, delay, unit));
    }

    @Override
    public void shutdown()
    {
        m_delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow()
    {
        return m_delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown()
    {
        return m_delegate.isShutdown();
    }

    @Override
    public boolean isTerminated()
    {
        return m_delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout,
                                    TimeUnit unit) throws
                                                   InterruptedException
    {
        return m_delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
        return monitorSize(m_delegate.submit(new CallableTracker<>(task)));
    }

    @Override
    public <T> Future<T> submit(Runnable task,
                                T result)
    {
        return monitorSize(m_delegate.submit(new RunnableTracker(task), result));
    }

    @Override
    public Future<?> submit(Runnable task)
    {
        return monitorSize(m_delegate.submit(new RunnableTracker(task)));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws
                                                                                  InterruptedException
    {
        return m_delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout,
                                         TimeUnit unit) throws
                                                        InterruptedException
    {
        return m_delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws
                                                                    InterruptedException,
                                                                    ExecutionException
    {
        return m_delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks,
                           long timeout,
                           TimeUnit unit) throws
                                          InterruptedException,
                                          ExecutionException,
                                          TimeoutException
    {
        return m_delegate.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command)
    {
        m_delegate.execute(new RunnableTracker(command));
    }

    //--//

    private <T> T monitorSize(T value)
    {
        if (LoggerInstance.isEnabled(Severity.Debug))
        {
            int                           threshold = LoggerInstance.isEnabled(Severity.DebugVerbose) ? 5 : c_minThreshold;
            final BlockingQueue<Runnable> queue     = m_delegate.getQueue();
            int                           size      = queue.size();

            if (size >= threshold)
            {
                final Instant now = Instant.now();
                if (m_nextReport != null && m_nextReport.isBefore(now))
                {
                    m_maximumSize /= 2;
                    m_nextReport = now.plus(c_decayTime);
                }

                if (size > m_maximumSize)
                {
                    synchronized (m_lock)
                    {
                        m_maximumSize = size;
                        m_nextReport = now.plus(c_decayTime);

                        LoggerInstance.debug("New pending highmark: %d", size);

                        for (Runnable task : queue)
                        {
                            RunnableScheduledFuture<?> futureTask = Reflection.as(task, RunnableScheduledFuture.class);
                            if (futureTask != null)
                            {
                                Optional<Object> target = Reflection.dereference(futureTask, "callable", "task");
                                if (!target.isPresent())
                                {
                                    target = Reflection.dereference(futureTask, "callable");
                                }

                                LoggerInstance.debug("    Task due in %ssec : %s", futureTask.getDelay(TimeUnit.MILLISECONDS) / 1000.0, target.orElse("<unknown callback>"));
                            }
                        }

                        for (RunnableTracker command : m_executingRunnables)
                        {
                            LoggerInstance.debug("    Executing Runnable: %s", command);
                        }

                        for (CallableTracker<?> callable : m_executingCallables)
                        {
                            LoggerInstance.debug("    Executing Callable: %s", callable);
                        }

                        Map<StackTraceAnalyzer, List<ThreadInfo>> uniqueStackTraces = StackTraceAnalyzer.allThreadInfos();
                        List<String>                              lines             = StackTraceAnalyzer.printThreadInfos(false, uniqueStackTraces);
                        for (String line : lines)
                        {
                            LoggerInstance.debug(line);
                        }
                    }
                }
            }
        }

        return value;
    }
}
