/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import com.optio3.lang.RunnableWithException;
import com.optio3.logging.Logger;

/**
 * A pool of threads for long-running execution.
 * <p>
 * Threads Threads are recycled, waiting
 */
public class LongRunningThreadPool
{
    public static final Logger LoggerInstance = new Logger(LongRunningThreadPool.class);

    private static class WorkerTask<T> extends CompletableFuture<T>
    {
        private final RunnableWithException m_runnable;
        private final Callable<T>           m_callable;

        private Future<?> m_task;

        WorkerTask(RunnableWithException task)
        {
            m_runnable = task;
            m_callable = null;
        }

        WorkerTask(Callable<T> task)
        {
            m_runnable = null;
            m_callable = task;
        }

        void start(ThreadPoolExecutor executor)
        {
            m_task = executor.submit(() ->
                                     {
                                         try
                                         {
                                             if (m_runnable != null)
                                             {
                                                 m_runnable.run();
                                                 this.complete(null);
                                             }
                                             else
                                             {
                                                 this.complete(m_callable.call());
                                             }
                                         }
                                         catch (Throwable ex)
                                         {
                                             LoggerInstance.error("Long-running task failed with exception: %s", ex);

                                             completeExceptionally(ex);
                                         }
                                         finally
                                         {
                                             m_task = null;
                                         }
                                     });
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning)
        {
            Future<?> task = m_task;
            return task != null && task.cancel(mayInterruptIfRunning);
        }
    }

    //--//

    private final ThreadPoolExecutor m_executor;

    //--//

    LongRunningThreadPool(ThreadPoolExecutor executor)
    {
        m_executor = executor;
    }

    public CompletableFuture<Void> queue(RunnableWithException task)
    {
        LongRunningThreadPool.WorkerTask<Void> workerTask = new WorkerTask<Void>(task);

        workerTask.start(m_executor);

        return workerTask;
    }

    public <T> CompletableFuture<T> queueFunction(Callable<T> task)
    {
        LongRunningThreadPool.WorkerTask<T> workerTask = new WorkerTask<T>(task);

        workerTask.start(m_executor);

        return workerTask;
    }
}
