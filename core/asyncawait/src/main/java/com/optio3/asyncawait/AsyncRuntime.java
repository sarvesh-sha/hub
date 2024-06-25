/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait;

import java.util.concurrent.CompletableFuture;

public class AsyncRuntime
{
    public static final CompletableFuture<Void>    NullResult = CompletableFuture.completedFuture(null);
    public static final CompletableFuture<Boolean> True       = CompletableFuture.completedFuture(true);
    public static final CompletableFuture<Boolean> False = CompletableFuture.completedFuture(false);

    @SuppressWarnings("unchecked")
    public static <T> CompletableFuture<T> asNull()
    {
        return (CompletableFuture<T>) NullResult;
    }
}
