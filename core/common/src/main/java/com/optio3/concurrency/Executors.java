/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;
import com.optio3.lang.RunnableWithException;

public class Executors
{
    private final static Supplier<ScheduledExecutorService> s_globalScheduledExecutor = Suppliers.memoize(() ->
                                                                                                          {
                                                                                                              int                       cpus          = getNumberOfProcessors();
                                                                                                              final CustomThreadFactory threadFactory = new CustomThreadFactory("SchedTask");
                                                                                                              ScheduledThreadPoolExecutor val = new ScheduledThreadPoolExecutor(cpus * 4,
                                                                                                                                                                                threadFactory);
                                                                                                              val.setRemoveOnCancelPolicy(true); // Avoid holding on to cancelled tasks.
                                                                                                              return new TrackingScheduledExecutor(val);
                                                                                                          });

    public static ScheduledExecutorService getDefaultScheduledExecutor()
    {
        return s_globalScheduledExecutor.get();
    }

    public static ScheduledFuture<?> scheduleOnDefaultPool(Runnable command,
                                                           long delay,
                                                           TimeUnit unit)
    {
        return getDefaultScheduledExecutor().schedule(command, delay, unit);
    }

    public static <V> ScheduledFuture<V> scheduleOnDefaultPool(Callable<V> command,
                                                               long delay,
                                                               TimeUnit unit)
    {
        return getDefaultScheduledExecutor().schedule(command, delay, unit);
    }

    //--//

    private final static Supplier<ThreadPoolExecutor> s_globalThreadPoolExecutor = Suppliers.memoize(() ->
                                                                                                     {
                                                                                                         int cpus = getNumberOfProcessors();
                                                                                                         return createPrivatePool("GlobalPool", cpus * 16, 60L, TimeUnit.SECONDS);
                                                                                                     });

    public static ThreadPoolExecutor getDefaultThreadPool()
    {
        return s_globalThreadPoolExecutor.get();
    }

    public static void closeWithTimeout(RunnableWithException target,
                                        long timeoutMillisec,
                                        Consumer<Throwable> failureCallback)
    {
        //
        // To avoid deadlocks, we close in a separate thread and monitor it from a timer...
        //

        ScheduledExecutorService defaultTimed      = getDefaultScheduledExecutor();
        LongRunningThreadPool    defaultThreadPool = getDefaultLongRunningThreadPool();

        ScheduledFuture<?> timer = defaultTimed.schedule(() ->
                                                         {
                                                             failureCallback.accept(new TimeoutException());
                                                         }, timeoutMillisec, TimeUnit.MILLISECONDS);

        defaultThreadPool.queue(() ->
                                {
                                    try
                                    {
                                        target.run();
                                    }
                                    catch (Throwable t)
                                    {
                                        failureCallback.accept(t);
                                    }
                                    finally
                                    {
                                        timer.cancel(false);
                                    }
                                });
    }

    //--//

    public static ThreadPoolExecutor createPrivatePool(String name,
                                                       int maximumPoolSize,
                                                       long keepAliveTime,
                                                       TimeUnit unit)
    {
        final CustomThreadFactory threadFactory = new CustomThreadFactory(name);
        ThreadPoolExecutor        pool          = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), threadFactory);
        pool.allowCoreThreadTimeOut(true);
        return pool;
    }

    //--//

    private static class CustomThreadFactory implements ThreadFactory
    {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        private final String        m_namePrefix;
        private final ThreadGroup   m_group;
        private final Stack<String> m_freeNames = new Stack<>();

        private int m_lastThreadNumber = 1;

        CustomThreadFactory(String name)
        {
            SecurityManager s = System.getSecurityManager();
            m_group = (s != null) ? s.getThreadGroup() : Thread.currentThread()
                                                               .getThreadGroup();
            m_namePrefix = name + "-" + poolNumber.getAndIncrement() + "-T";
        }

        @Override
        public Thread newThread(Runnable r)
        {
            Thread t = new Thread(m_group, () -> wrapWorker(r), m_namePrefix, 0);

            // Make sure the threads are marked as daemon, so they don't prevent a clean shutdown.
            t.setDaemon(true);

            if (t.getPriority() != Thread.NORM_PRIORITY)
            {
                t.setPriority(Thread.NORM_PRIORITY);
            }

            return t;
        }

        private void wrapWorker(Runnable r)
        {
            String name = acquireName();

            try
            {
                Thread.currentThread()
                      .setName(name);

                r.run();
            }
            finally
            {
                releaseName(name);
            }
        }

        private synchronized String acquireName()
        {
            if (m_freeNames.isEmpty())
            {
                return m_namePrefix + m_lastThreadNumber++;
            }

            return m_freeNames.pop();
        }

        private synchronized void releaseName(String name)
        {
            m_freeNames.push(name);
        }
    }

    //--//

    private final static Supplier<LongRunningThreadPool> s_globalLongRunningThreadPool = Suppliers.memoize(() ->
                                                                                                           {
                                                                                                               int cpus = getNumberOfProcessors();
                                                                                                               ThreadPoolExecutor executor = createPrivatePool("LongRunning",
                                                                                                                                                               cpus * 4,
                                                                                                                                                               60L,
                                                                                                                                                               TimeUnit.SECONDS);
                                                                                                               return new LongRunningThreadPool(executor);
                                                                                                           });

    public static LongRunningThreadPool getDefaultLongRunningThreadPool()
    {
        return s_globalLongRunningThreadPool.get();
    }

    //--//

    private static class DelayedCompletableFuture extends CompletableFuture<Boolean>
    {
        private ScheduledFuture<?> m_timer;

        void start(ScheduledExecutorService executor,
                   int timeout,
                   TimeUnit unit)
        {
            m_timer = executor.schedule(() -> this.complete(true), timeout, unit);
        }

        @Override
        public boolean complete(Boolean value)
        {
            m_timer.cancel(false);

            return super.complete(value);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            m_timer.cancel(false);

            return super.cancel(mayInterruptIfRunning);
        }
    }

    public static CompletableFuture<Boolean> asyncDelay(int timeout,
                                                        TimeUnit unit)
    {
        DelayedCompletableFuture res = new DelayedCompletableFuture();

        res.start(getDefaultScheduledExecutor(), timeout, unit);

        return res;
    }

    public static void safeSleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e1)
        {
            // Swallow wake up.
        }
    }

    public static Semaphore allocateSemaphore(double ratioToAvailableProcessors)
    {
        int permits = Math.max(1, (int) (getNumberOfProcessors() * ratioToAvailableProcessors));

        return new Semaphore(permits);
    }

    public static void callWithAutoRetry(int maxRetries,
                                         int milliSecSleepBetweenAttempts,
                                         Runnable callback)
    {
        while (true)
        {
            try
            {
                callback.run();
                return;
            }
            catch (Throwable t)
            {
                if (maxRetries-- > 0)
                {
                    safeSleep(milliSecSleepBetweenAttempts);
                    continue;
                }

                throw new RuntimeException(t);
            }
        }
    }

    public static <T> T callWithAutoRetry(int maxRetries,
                                          int milliSecSleepBetweenAttempts,
                                          Callable<T> callback)
    {
        while (true)
        {
            try
            {
                return callback.call();
            }
            catch (Throwable t)
            {
                if (maxRetries-- > 0)
                {
                    safeSleep(milliSecSleepBetweenAttempts);
                    continue;
                }

                throw new RuntimeException(t);
            }
        }
    }

    //--//

    public static int getNumberOfProcessors()
    {
        return Runtime.getRuntime()
                      .availableProcessors();
    }
}
