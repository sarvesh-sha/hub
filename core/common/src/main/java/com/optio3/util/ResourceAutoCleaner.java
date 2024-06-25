/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

public class ResourceAutoCleaner<T extends AutoCloseable> extends ResourceCleaner
{
    public final T resource;

    public ResourceAutoCleaner(Object holder,
                               T res)
    {
        super(holder);

        resource = res;
    }

    @Override
    protected void closeUnderCleaner() throws
                                        Exception
    {
        resource.close();
    }
}