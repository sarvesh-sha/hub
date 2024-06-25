/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.Lists;
import com.optio3.concurrency.Executors;
import com.optio3.util.function.FunctionWithException;

public class CollectionUtils
{
    public static <T> Collection<T> asEmptyCollectionIfNull(Collection<T> coll)
    {
        return coll != null ? coll : Collections.emptyList();
    }

    public static <T> List<T> asEmptyCollectionIfNull(List<T> list)
    {
        return list != null ? list : Collections.emptyList();
    }

    public static <T> Set<T> asEmptyCollectionIfNull(Set<T> set)
    {
        return set != null ? set : Collections.emptySet();
    }

    public static <K, V> Map<K, V> asEmptyCollectionIfNull(Map<K, V> map)
    {
        return map != null ? map : Collections.emptyMap();
    }

    //--//

    public static int size(Collection<?> list)
    {
        return list != null ? list.size() : 0;
    }

    public static boolean isEmpty(Collection<?> list)
    {
        return list == null || list.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> list)
    {
        return list != null && !list.isEmpty();
    }

    public static <T> T firstElement(Collection<T> list)
    {
        return isNotEmpty(list) ? list.iterator()
                                      .next() : null;
    }

    public static <T> T firstElement(List<T> list)
    {
        return isNotEmpty(list) ? list.get(0) : null;
    }

    public static <T> T getNthElement(List<T> list,
                                      int index)
    {
        return list != null && (index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    public static <T> T lastElement(List<T> list)
    {
        return isNotEmpty(list) ? list.get(list.size() - 1) : null;
    }

    public static <I, O> List<O> transformToList(Collection<I> coll,
                                                 Function<I, O> transform)
    {
        return transformToListInner(coll, transform, false);
    }

    public static <I, O> List<O> transformToListNoNulls(Collection<I> coll,
                                                        Function<I, O> transform)
    {
        return transformToListInner(coll, transform, true);
    }

    private static <I, O> List<O> transformToListInner(Collection<I> coll,
                                                       Function<I, O> transform,
                                                       boolean skipNulls)
    {
        List<O> res = Lists.newArrayList();

        if (coll != null)
        {
            for (I input : coll)
            {
                O output = input != null ? transform.apply(input) : null;

                if (output != null || !skipNulls)
                {
                    res.add(output);
                }
            }
        }

        return res;
    }

    public static <I, O> List<O> transformInParallel(Collection<I> coll,
                                                     Semaphore gate,
                                                     FunctionWithException<I, O> transform)
    {
        List<CompletableFuture<O>> res = Lists.newArrayList();

        for (I input : coll)
        {
            transformInParallelWorker(res, gate, transform, input);
        }

        return transformToList(res, (val) -> safeExecution(CompletableFuture::get, val));
    }

    private static <I, O> void transformInParallelWorker(List<CompletableFuture<O>> res,
                                                         Semaphore gate,
                                                         FunctionWithException<I, O> transform,
                                                         I input)
    {
        gate.acquireUninterruptibly();

        CompletableFuture<O> future = new CompletableFuture<>();
        res.add(future);

        Executors.getDefaultThreadPool()
                 .execute(() ->
                          {
                              future.complete(safeExecution(transform, input));
                              gate.release();
                          });
    }

    private static <I, O> O safeExecution(FunctionWithException<I, O> transform,
                                          I input)
    {
        try
        {
            return transform.apply(input);
        }
        catch (Throwable t)
        {
            return null;
        }
    }
    //--//

    public static <T> List<T> filter(Collection<T> coll,
                                     Predicate<T> filter)
    {
        List<T> res = Lists.newArrayList();

        if (coll != null)
        {
            for (T input : coll)
            {
                if (filter.test(input))
                {
                    res.add(input);
                }
            }
        }

        return res;
    }

    public static <T> T findFirst(Collection<T> coll,
                                  Predicate<T> filter)
    {
        if (coll != null)
        {
            for (T input : coll)
            {
                if (filter.test(input))
                {
                    return input;
                }
            }
        }

        return null;
    }

    public static <T> T findLast(Collection<T> coll,
                                 Predicate<T> filter)
    {
        T lastMatch = null;

        if (coll != null)
        {
            for (T input : coll)
            {
                if (filter.test(input))
                {
                    lastMatch = input;
                }
            }
        }

        return lastMatch;
    }

    public static <T> int indexOf(List<T> coll,
                                  Predicate<T> filter)
    {
        if (coll != null)
        {
            for (int i = 0; i < coll.size(); i++)
            {
                T val = coll.get(i);
                if (filter.test(val))
                {
                    return i;
                }
            }
        }

        return -1;
    }

    public static <T> boolean addIfMissing(Collection<T> coll,
                                           T item)
    {
        if (!coll.contains(item))
        {
            return coll.add(item);
        }

        return false;
    }

    public static <T> boolean addIfMissingAndNotNull(Collection<T> coll,
                                                     T item)
    {
        if (item != null && !coll.contains(item))
        {
            return coll.add(item);
        }

        return false;
    }

    public static <T> Object toArray(Class<T> clz,
                                     Collection<T> coll)
    {
        int size = coll.size();

        Object array = Array.newInstance(clz, size);
        int    pos   = 0;

        for (T t : coll)
        {
            Array.set(array, pos++, t);
        }

        return array;
    }

    public static <T> List<T> fromArray(Class<T> clz,
                                        Object array)
    {
        List<T> res = Lists.newArrayList();

        if (array != null && array.getClass()
                                  .isArray())
        {
            int size = Array.getLength(array);
            for (int i = 0; i < size; i++)
            {
                res.add(clz.cast(Array.get(array, i)));
            }
        }

        return res;
    }

    public static <T> boolean equals(Collection<T> a,
                                     Collection<T> b,
                                     BiFunction<T, T, Boolean> comparator)
    {
        if (a == null)
        {
            a = Collections.emptyList();
        }

        if (b == null)
        {
            b = Collections.emptyList();
        }

        Iterator<T> itA = a.iterator();
        Iterator<T> itB = b.iterator();

        while (true)
        {
            boolean hasNextA = itA.hasNext();
            boolean hasNextB = itB.hasNext();

            if (hasNextA != hasNextB)
            {
                return false;
            }

            if (!hasNextA)
            {
                return true;
            }

            if (!comparator.apply(itA.next(), itB.next()))
            {
                return false;
            }
        }
    }
}
