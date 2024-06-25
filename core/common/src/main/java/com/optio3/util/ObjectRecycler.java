/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.ref.SoftReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ObjectRecycler<T>
{
    public final class Holder implements AutoCloseable
    {
        private final ObjectRecycler<T> m_ar;
        private       T                 m_obj;

        private Holder(ObjectRecycler<T> ar)
        {
            m_ar  = ar;
            m_obj = ar.acquireRaw();
        }

        @Override
        public void close()
        {
            if (m_obj != null)
            {
                m_ar.releaseRaw(m_obj);
                m_obj = null;
            }
        }

        public T get()
        {
            if (m_obj == null)
            {
                throw new RuntimeException("Object recycled");
            }

            return m_obj;
        }
    }

    private static class Cache
    {
        private final Object[] m_objects;
        private       int      m_available;

        private Cache(int max)
        {
            m_objects = new Object[max];
        }

        Object acquire()
        {
            if (m_available == 0)
            {
                return null;
            }

            int pos = --m_available;

            Object res = m_objects[pos];
            m_objects[pos] = null;

            return res;
        }

        boolean release(Object obj)
        {
            if (m_available == m_objects.length)
            {
                return false;
            }

            m_objects[m_available++] = obj;
            return true;
        }
    }

    @FunctionalInterface
    public interface Allocator<V>
    {
        V allocate();
    }

    @FunctionalInterface
    public interface Cleaner<V>
    {
        void cleanup(V value) throws
                              Exception;
    }

    //--//

    public static final boolean isARM;

    static
    {
        isARM = System.getProperty("os.arch")
                      .startsWith("arm");
    }

    private final Class<T>                          m_clz;
    private final Allocator<T>                      m_allocator;
    private final Cleaner<T>                        m_cleaner;
    private final int                               m_maxGlobal;
    private final int                               m_maxPerThread;
    private final ThreadLocal<SoftReference<Cache>> m_perThread = new ThreadLocal<>();
    private       SoftReference<Cache>              m_global;

    private final AtomicLong    m_hitPerThread = new AtomicLong();
    private final AtomicLong    m_hitGlobal    = new AtomicLong();
    private final AtomicLong    m_miss         = new AtomicLong();
    private final AtomicLong    m_rejected     = new AtomicLong();
    private final AtomicLong    m_collected    = new AtomicLong();
    private final AtomicInteger m_highwater    = new AtomicInteger();
    private final AtomicLong    m_acquireTime  = new AtomicLong();
    private final AtomicLong    m_releaseTime  = new AtomicLong();
    ;

    //--//

    private ObjectRecycler(int maxGlobal,
                           int maxPerThread,
                           Class<T> clz,
                           Allocator<T> allocator,
                           Cleaner<T> cleaner)
    {
        m_maxGlobal    = maxGlobal;
        m_maxPerThread = maxPerThread;
        m_clz          = clz;
        m_allocator    = allocator;
        m_cleaner      = cleaner;
    }

    public static <T> ObjectRecycler<T> build(int maxGlobal,
                                              int maxPerThread,
                                              Class<T> clz,
                                              Allocator<T> allocator,
                                              Cleaner<T> cleaner)
    {
        return new ObjectRecycler<T>(maxGlobal, maxPerThread, clz, allocator, cleaner);
    }

    //--//

    public long getHitCount()
    {
        return m_hitGlobal.get() + m_hitPerThread.get();
    }

    public long getMissCount()
    {
        return m_miss.get();
    }

    public long getRejected()
    {
        return m_rejected.get();
    }

    public int getHighwater()
    {
        return m_highwater.get();
    }

    //--//

    public Holder acquire()
    {
        return new Holder(this);
    }

    public T acquireRaw()
    {
        // Flip for low-level debugging.
        if (false)
        {
            if ((m_hitGlobal.get() + m_hitPerThread.get() + m_miss.get()) % 1_000 == 0)
            {
                System.out.printf("%s  Hit: %,d/%,d  Miss: %,d  Highwater: %,d Rejected: %,d  Collected: %,d  Acquire: %,d  Release: %,d\n",
                                  m_clz.getTypeName(),
                                  m_hitPerThread.get(),
                                  m_hitGlobal.get(),
                                  m_miss.get(),
                                  m_highwater.get(),
                                  m_rejected.get(),
                                  m_collected.get(),
                                  m_acquireTime.get(),
                                  m_releaseTime.get());
            }
        }

        Cache  cache;
        Object res;

        if (m_maxPerThread > 0)
        {
            cache = accessPerThread();
            res   = cache.acquire();
            if (res != null)
            {
                m_hitPerThread.incrementAndGet();

                return m_clz.cast(res);
            }
        }

        if (m_maxGlobal > 0)
        {
            cache = accessGlobal();
            long start = System.nanoTime();
            synchronized (cache)
            {
                res = cache.acquire();

                m_acquireTime.addAndGet(System.nanoTime() - start);
            }

            if (res != null)
            {
                m_hitGlobal.incrementAndGet();

                return m_clz.cast(res);
            }
        }

        m_miss.incrementAndGet();

        return m_allocator.allocate();
    }

    public void releaseRaw(T obj)
    {
        if (m_cleaner != null)
        {
            try
            {
                m_cleaner.cleanup(obj);
            }
            catch (Throwable t)
            {
                // Something went wrong during the cleanup, bail.
                return;
            }
        }

        if (m_maxPerThread > 0)
        {
            Cache cache = accessPerThread();
            if (cache.release(obj))
            {
                return;
            }
        }

        if (m_maxGlobal > 0)
        {
            Cache cache = accessGlobal();
            long  start = System.nanoTime();
            synchronized (cache)
            {
                if (!cache.release(obj))
                {
                    m_rejected.incrementAndGet();
                }

                m_highwater.set(Math.max(m_highwater.get(), cache.m_available));
            }

            m_releaseTime.addAndGet(System.nanoTime() - start);
        }
    }

    private Cache accessPerThread()
    {
        SoftReference<Cache> cacheRef = m_perThread.get();
        Cache                cache;

        if (cacheRef != null)
        {
            cache = cacheRef.get();
            if (cache != null)
            {
                return cache;
            }
        }

        cache    = new Cache(m_maxPerThread);
        cacheRef = new SoftReference<>(cache);
        m_perThread.set(cacheRef);
        return cache;
    }

    private Cache accessGlobal()
    {
        SoftReference<Cache> cacheRef = m_global;
        Cache                cache;

        if (cacheRef != null)
        {
            cache = cacheRef.get();
            if (cache != null)
            {
                return cache;
            }
        }

        cache    = new Cache(m_maxGlobal);
        cacheRef = new SoftReference<>(cache);
        m_global = cacheRef;
        return cache;
    }
}
