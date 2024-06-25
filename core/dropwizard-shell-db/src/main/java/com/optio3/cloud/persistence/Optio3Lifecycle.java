/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

/**
 * Our own lifecycle callback for just-in-time manipulation of records.
 * <p>
 * Records can implements this interface to tweak their state just before or after they are persisted to the database.
 */
public interface Optio3Lifecycle
{
    void onSave(InterceptorState interceptorState);

    void onLoad(InterceptorState interceptorState);

    void onFlushDirty(InterceptorState interceptorState);

    void onPreDelete(SessionHolder sessionHolder);

    void onDelete(InterceptorState interceptorState);

    void onEviction();
}
