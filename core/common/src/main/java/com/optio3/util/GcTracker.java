/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.ref.Cleaner;
import java.util.List;

import com.google.common.collect.Lists;

public class GcTracker
{
    @FunctionalInterface
    public interface Callback
    {
        void run(long freeMemory,
                 long totalMemory,
                 long maxMemory) throws
                                 Exception;
    }

    public static class Holder implements AutoCloseable
    {
        private final Callback m_callback;

        Holder(Callback callback)
        {
            m_callback = callback;
        }

        @Override
        public void close()
        {
            synchronized (s_cleaner)
            {
                s_callbacks.remove(this);

                schedule();
            }
        }

        void invoke(long freeMemory,
                    long totalMemory,
                    long maxMemory)
        {
            try
            {
                m_callback.run(freeMemory, totalMemory, maxMemory);
            }
            catch (Throwable t)
            {
                // Swallow failures.
            }
        }
    }

    static final Cleaner      s_cleaner   = Cleaner.create();
    static final List<Holder> s_callbacks = Lists.newArrayList();
    static       GcTracker    s_activeTracker;

    private Cleaner.Cleanable m_holder;

    public static Holder register(Callback callback)
    {
        var holder = new Holder(callback);
        synchronized (s_cleaner)
        {
            s_callbacks.add(holder);

            schedule();
        }

        return holder;
    }

    private static void schedule()
    {
        if (s_activeTracker == null)
        {
            if (!s_callbacks.isEmpty())
            {
                GcTracker gt = new GcTracker();
                gt.m_holder     = s_cleaner.register(new Object(), gt::dispatch);
                s_activeTracker = gt;
            }
        }
    }

    private void dispatch()
    {
        Runtime rt          = Runtime.getRuntime();
        long    freeMemory  = rt.freeMemory();
        long    totalMemory = rt.totalMemory();
        long    maxMemory   = rt.maxMemory();

        List<Holder> lst;

        synchronized (s_cleaner)
        {
            s_activeTracker = null; // Clear to force re-registration.
            schedule();

            lst = Lists.newArrayList(s_callbacks);
        }

        for (Holder holder : lst)
        {
            holder.invoke(freeMemory, totalMemory, maxMemory);
        }
    }
}
