/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.optio3.asyncawait.bootstrap.AsyncClassFileTransformer;
import com.optio3.asyncawait.converter.KnownMethod;
import com.optio3.asyncawait.converter.KnownMethodId;

public class CompileTime
{
    static
    {
        AsyncClassFileTransformer.ensureLoaded();
    }

    @KnownMethod(KnownMethodId.CompileTime_bootstrap)
    public static void bootstrap()
    {
        // Do nothing method that bootstraps the agent.
    }

    //--//

    @KnownMethod(KnownMethodId.CompileTime_wasComputationCancelled)
    public static boolean wasComputationCancelled()
    {
        throw new RuntimeException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    //--//

    @KnownMethod(KnownMethodId.CompileTime_awaitNoUnwrap)
    public static <T> T awaitNoUnwrapException(CompletableFuture<T> t) throws
                                                                       InterruptedException,
                                                                       ExecutionException
    {
        throw new CancellationException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    @KnownMethod(KnownMethodId.CompileTime_awaitNoUnwrapTimeout)
    public static <T> T awaitNoUnwrapException(CompletableFuture<T> t,
                                               long timeout,
                                               TimeUnit unit) throws
                                                              InterruptedException,
                                                              ExecutionException,
                                                              TimeoutException
    {
        throw new CancellationException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    @KnownMethod(KnownMethodId.CompileTime_await)
    public static <T> T await(CompletableFuture<T> t) throws
                                                      Exception
    {
        throw new CancellationException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    @KnownMethod(KnownMethodId.CompileTime_awaitTimeout)
    public static <T> T await(CompletableFuture<T> t,
                              long timeout,
                              TimeUnit unit) throws
                                             Exception
    {
        throw new CancellationException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    @KnownMethod(KnownMethodId.CompileTime_sleep)
    public static CompletableFuture<Void> sleep(long timeout,
                                                TimeUnit unit) throws
                                                               Exception
    {
        throw new CancellationException("This method should never be called at runtime. Async-await code transformation must have failed.");
    }

    //--//

    @KnownMethod(KnownMethodId.CompileTime_wrapAsync)
    public static <T> CompletableFuture<T> wrapAsync(T t)
    {
        return CompletableFuture.completedFuture(t);
    }
}
