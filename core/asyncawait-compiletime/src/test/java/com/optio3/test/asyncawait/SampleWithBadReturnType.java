/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncawait;

import static com.optio3.asyncawait.CompileTime.awaitNoUnwrapException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleWithBadReturnType
{
    @SafeVarargs
    public static Integer sumAsyncWithMonitor(Object lock,
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
            sum += val;
        }

        return sum;
    }
}
