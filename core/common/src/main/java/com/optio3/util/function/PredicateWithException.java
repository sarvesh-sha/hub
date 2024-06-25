/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

@FunctionalInterface
public interface PredicateWithException<T>
{

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    boolean test(T t) throws
                      Exception;
}
