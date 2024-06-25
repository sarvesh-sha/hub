/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.function;

@FunctionalInterface
public interface CallableWithoutException<V>
{
    /**
     * Computes a result, without throwing an exception.
     *
     * @return computed result
     */
    V call();
}