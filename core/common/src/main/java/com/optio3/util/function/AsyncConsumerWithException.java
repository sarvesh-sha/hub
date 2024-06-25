/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncConsumerWithException<T>
{

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    CompletableFuture<Void> accept(T t) throws
                                        Exception;
}
