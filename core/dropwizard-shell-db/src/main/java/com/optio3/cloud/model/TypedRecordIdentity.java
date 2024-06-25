/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.model;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.client.SwaggerTypeReplacement;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.serialization.Reflection;

@SwaggerTypeReplacement(targetElement = RecordIdentity.class)
public class TypedRecordIdentity<T extends RecordWithCommonFields> extends RecordIdentity
{
    public static <T extends RecordWithCommonFields> T getOrNull(RecordHelper<T> helper,
                                                                 TypedRecordIdentity<T> ri)
    {
        if (ri == null)
        {
            return null;
        }

        return helper.getOrNull(ri.sysId);
    }

    public static <T extends RecordWithCommonFields> RecordLocked<T> getWithLockOrNull(RecordHelper<T> helper,
                                                                                       TypedRecordIdentity<T> ri,
                                                                                       long timeout,
                                                                                       TimeUnit unit)
    {
        if (ri == null)
        {
            return null;
        }

        return helper.getWithLockOrNull(ri.sysId, timeout, unit);
    }

    @SuppressWarnings("unchecked")
    public Class<T> resolveEntityClass()
    {
        return (Class<T>) getEntityClass();
    }

    @SuppressWarnings("unchecked")
    public <T2 extends RecordWithCommonFields> TypedRecordIdentity<T2> as(Class<T2> clz)
    {
        Class<T> clzThis = resolveEntityClass();

        if (clzThis != null && Reflection.isSubclassOf(clz, clzThis))
        {
            return (TypedRecordIdentity<T2>) this;
        }

        return null;
    }
}
