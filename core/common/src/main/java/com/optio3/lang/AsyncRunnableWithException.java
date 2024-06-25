/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.lang;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncRunnableWithException
{
    public abstract CompletableFuture<Void> run() throws
                                                  Exception;
}
