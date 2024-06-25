/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.service;

import javax.annotation.Nonnull;

import com.optio3.util.Exceptions;

public interface IServiceProvider
{
    <S> S getService(Class<S> serviceClass);

    @Nonnull
    default <S> S getServiceNonNull(Class<S> serviceClass)
    {
        S s = getService(serviceClass);
        if (s == null)
        {
            throw Exceptions.newRuntimeException("Unknown service %s", serviceClass);
        }

        return s;
    }
}
