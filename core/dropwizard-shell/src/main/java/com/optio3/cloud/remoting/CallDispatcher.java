/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

import java.util.concurrent.CompletableFuture;

public abstract class CallDispatcher
{
    public abstract CompletableFuture<?> send(RemoteCallDescriptor rc,
                                              Object[] originalArguments) throws
                                                                          Throwable;
}
