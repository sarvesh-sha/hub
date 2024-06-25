/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncBiConsumerWithException<T, U>
{
    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     *
     * @return A completable future that will be resolved once the callback completes.
     *
     * @throws Exception
     */
    CompletableFuture<Void> accept(T t,
                                   U u) throws
                                        Exception;
}
