/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.concurrency;

import java.util.List;
import java.util.concurrent.Semaphore;

import com.google.common.collect.Lists;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class SyncWaitMultiple
{
    private final Object          m_lock = new Object();
    private final Semaphore       m_limiter;
    private       int             m_pending;
    private       List<Throwable> m_failures;

    public SyncWaitMultiple(Semaphore gate)
    {
        m_limiter = gate;
    }

    public void queue(Runnable worker)
    {
        m_limiter.acquireUninterruptibly();

        synchronized (m_lock)
        {
            m_pending++;
        }

        Executors.getDefaultThreadPool()
                 .execute(() ->
                          {
                              Throwable failure;

                              try
                              {
                                  worker.run();

                                  failure = null;
                              }
                              catch (Throwable t)
                              {
                                  failure = t;
                              }

                              m_limiter.release();

                              synchronized (m_lock)
                              {
                                  if (failure != null)
                                  {
                                      if (m_failures == null)
                                      {
                                          m_failures = Lists.newArrayList();
                                      }

                                      m_failures.add(failure);
                                  }

                                  m_pending--;
                                  m_lock.notifyAll();
                              }
                          });
    }

    public boolean drain(MonotonousTime timeoutExpiration)
    {
        synchronized (m_lock)
        {
            while (m_pending > 0)
            {
                if (!TimeUtils.waitOnLock(m_lock, timeoutExpiration))
                {
                    return false;
                }
            }

            if (m_failures != null)
            {
                Throwable firstException = m_failures.get(0);

                if (m_failures.size() == 1)
                {
                    throw new RuntimeException(firstException);
                }
                else
                {
                    throw new RuntimeException(String.format("Failed with %d exceptions", m_failures.size()), firstException);
                }
            }

            return true;
        }
    }
}