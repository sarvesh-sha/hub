/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.lang.ref.Cleaner;
import java.util.function.Supplier;

import com.google.common.base.Suppliers;

public abstract class ResourceCleaner
{
    private final static Supplier<Cleaner> s_globalCleaner = Suppliers.memoize(Cleaner::create);

    private final Cleaner.Cleanable m_cleaner;

    public ResourceCleaner(Object holder)
    {
        m_cleaner = s_globalCleaner.get()
                                   .register(holder, () ->
                                   {
//                                       System.err.printf(">> CLEANER: %s\n", getClass().getSimpleName());
//                                       Exception e2 = new Exception();
//                                       e2.printStackTrace();
                                       try
                                       {
                                           closeUnderCleaner();
                                       }
                                       catch (Exception e)
                                       {
                                           // Ignore failures
                                       }
                                   });
    }

    public void clean()
    {
        m_cleaner.clean();
    }

    protected abstract void closeUnderCleaner() throws
                                                Exception;
}