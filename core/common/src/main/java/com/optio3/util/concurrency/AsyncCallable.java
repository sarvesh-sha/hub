/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.concurrency;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncCallable<V>
{
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     *
     * @throws Exception if unable to compute a result
     */
    CompletableFuture<V> call() throws
                                Exception;
}
