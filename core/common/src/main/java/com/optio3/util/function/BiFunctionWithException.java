/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

@FunctionalInterface
public interface BiFunctionWithException<T, U, R>
{
    /**
     * Applies this function to the given arguments.
     *
     * @param t the first function argument
     * @param u the second function argument
     *
     * @return the function result
     */
    R apply(T t,
            U u) throws
                 Exception;
}
