/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncawait;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.awaitNoUnwrapException;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wasComputationCancelled;
import static com.optio3.asyncawait.CompileTime.wrapAsync;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;

public class AsyncSamples
{
    public static class ValueHolder
    {
        public Object tag;
        public Object value;

        public ValueHolder(Object tag,
                           Object value)
        {
            this.tag = tag;
            this.value = value;
        }

        public Object extractValue()
        {
            if (value instanceof AsyncSamples.ValueHolder)
            {
                return ((AsyncSamples.ValueHolder) value).extractValue();
            }

            return value;
        }
    }

    @SafeVarargs
    public static CompletableFuture<Integer> sumAsync(CompletableFuture<Integer> resultPreview,
                                                      int bias,
                                                      int valueLimit,
                                                      CompletableFuture<Integer>... args) throws
                                                                                          InterruptedException,
                                                                                          ExecutionException
    {
        int sum = bias;
        for (int i = 0; i < args.length; i++)
        {
            CompletableFuture<Integer> arg = args[i];
            System.out.println(String.format("Before %s", i));
            int val;
            try
            {
                val = awaitNoUnwrapException(arg);
            }
            catch (Exception e)
            {
                if (wasComputationCancelled())
                {
                    System.out.println(String.format("Ignore exception %s", e));
                    continue;
                }

                throw e;
            }

            System.out.println(String.format("After %s", i));
            if (val > valueLimit)
            {
                throw new RuntimeException();
            }

            // To test access to private methods and fields.
            sum = doSum(sum, val) + s_val;
        }

        if (resultPreview != null)
        {
            resultPreview.complete(sum);
        }

        return wrapAsync(sum);
    }

    // To test access to private methods.
    private static int s_val;

    // To test access to private methods.
    private static int doSum(int a,
                             int b)
    {
        return a + b;
    }

    @SafeVarargs
    public static CompletableFuture<Integer> sumAsyncWithTimeout(int timeout,
                                                                 CompletableFuture<Integer>... args) throws
                                                                                                     InterruptedException,
                                                                                                     ExecutionException,
                                                                                                     TimeoutException
    {
        int sum = 0;

        for (CompletableFuture<Integer> arg : args)
            sum += awaitNoUnwrapException(arg, timeout, TimeUnit.MILLISECONDS);

        return wrapAsync(sum);
    }

    @SafeVarargs
    public static CompletableFuture<Integer> sumAsyncWithMonitor(Object lock,
                                                                 int timeout,
                                                                 CompletableFuture<Integer>... args) throws
                                                                                                     InterruptedException,
                                                                                                     ExecutionException,
                                                                                                     TimeoutException
    {
        int sum = 0;

        for (CompletableFuture<Integer> arg : args)
        {
            int val = awaitNoUnwrapException(arg, timeout, TimeUnit.MILLISECONDS);

            synchronized (lock)
            {
                sum += val;
            }
        }

        return wrapAsync(sum);
    }

    public static CompletableFuture<Void> nopForegroundProcessing() throws
                                                                    Exception
    {
        System.out.println("Before synchronous sleep");
        await(sleep(100, TimeUnit.MILLISECONDS));
        System.out.println("After synchronous sleep");

        return wrapAsync(null);
    }

    // Method with no await, to test transformations triggered by annotations.
    @AsyncBackground
    public static CompletableFuture<Void> nopBackgroundProcessing() throws
                                                                    InterruptedException
    {
        System.out.println("Before synchronous sleep");
        Thread.sleep(100);
        System.out.println("After synchronous sleep");

        return wrapAsync(null);
    }

    @SafeVarargs
    public static CompletableFuture<Integer> sumAsyncWithBackgroundProcessing(long delay,
                                                                              TimeUnit delayUnit,
                                                                              CompletableFuture<Integer>... args) throws
                                                                                                                  Exception
    {
        int sum = 0;

        new Exception().printStackTrace();
        await(sleep(1, TimeUnit.MILLISECONDS));
        new Exception().printStackTrace();

        for (CompletableFuture<Integer> arg : args)
        {
            int val = awaitNoUnwrapException(arg);
            sum += val;
        }

        return wrapAsync(sum);
    }

    @AsyncBackground
    public static CompletableFuture<Void> backgroundProcessingThatShouldNotExecute(@AsyncDelay long delay,
                                                                                   @AsyncDelay TimeUnit delayUnit,
                                                                                   CompletableFuture<Void> done)
    {
        done.complete(null);

        return wrapAsync(null);
    }

    @SafeVarargs
    public static CompletableFuture<Integer> asyncAdd2(CompletableFuture<Integer>... args)
    {
        int a = 0;
        for (CompletableFuture<Integer> v : args)
        {
            System.out.println(String.format("Before: %d", a));
            try
            {
                a += awaitNoUnwrapException(v);
            }
            catch (Exception e)
            {
                System.out.println(String.format("Got: %s", e));
            }
            System.out.println(String.format("After: %d", a));
        }
        return CompletableFuture.completedFuture(a);
    }

    @SafeVarargs
    public static CompletableFuture<Long> asyncAddLong(CompletableFuture<Long>... args) throws
                                                                                        Exception
    {
        long a = 0;
        for (CompletableFuture<Long> v : args)
        {
            System.out.println(String.format("Before: %d", a));
            a += await(v);
            System.out.println(String.format("After: %d", a));
        }
        return CompletableFuture.completedFuture(a);
    }

    @SafeVarargs
    public static CompletableFuture<Integer> awaitInConstructorCall(CompletableFuture<Integer>... args) throws
                                                                                                        InterruptedException,
                                                                                                        ExecutionException
    {
        int sum = 0;
        for (CompletableFuture<Integer> arg : args)
        {
            AsyncSamples.ValueHolder v = new ValueHolder(10, new ValueHolder(20, awaitNoUnwrapException(arg)));
            sum += (int) v.extractValue();

            // Just to test two awaits in the same method.
            AsyncSamples.ValueHolder v_b = new ValueHolder(10, new ValueHolder(null, new ValueHolder(30, awaitNoUnwrapException(arg))));
            assertEquals((int) v.extractValue(), (int) v_b.extractValue());
        }

        return wrapAsync(sum);
    }
}
