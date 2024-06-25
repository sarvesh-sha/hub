/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncawait;

import static com.optio3.asyncawait.CompileTime.awaitNoUnwrapException;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SampleWithMonitorAroundBlockingCall
{
    @SafeVarargs
    public static CompletableFuture<Integer> sumAsyncWithMonitor(Object lock,
                                                                 int timeout,
                                                                 CompletableFuture<Integer>... args) throws
                                                                                                     InterruptedException,
                                                                                                     ExecutionException,
                                                                                                     TimeoutException,
                                                                                                     IOException
    {
        int sum = 0;

        for (CompletableFuture<Integer> arg : args)
        {
            synchronized (lock)
            {
                try (FileOutputStream stream = new FileOutputStream("test"))
                {
                    int val = awaitNoUnwrapException(arg, timeout, TimeUnit.MILLISECONDS);
                    sum += val;
                }
            }
        }

        return wrapAsync(sum);
    }
}
