/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util.concurrency;

import java.util.concurrent.CompletableFuture;

import com.optio3.serialization.Reflection;

public class TypedCompletableFuture<T> extends CompletableFuture<T>
{
    private final Class<T> m_clz;

    public TypedCompletableFuture(Class<T> clz)
    {
        m_clz = clz;
    }

    public void tryCompleteWithCast(Object req)
    {
        T req2 = Reflection.as(req, m_clz);
        if (req2 != null)
        {
            complete(req2);
        }
        else
        {
            String message = String.format("Can't cast result %s to promise of type %s",
                                           req.getClass()
                                              .getName(),
                                           m_clz.getName());
            completeExceptionally(new ClassCastException(message));
        }
    }
}
