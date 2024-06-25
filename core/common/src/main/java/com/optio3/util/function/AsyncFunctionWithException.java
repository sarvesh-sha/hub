/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncFunctionWithException<T, R>
{

    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     *
     * @return the function result
     */
    CompletableFuture<R> apply(T t) throws
                                    Exception;
}
