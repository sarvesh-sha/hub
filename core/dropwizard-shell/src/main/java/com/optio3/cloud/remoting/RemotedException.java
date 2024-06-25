/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.remoting;

public final class RemotedException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public RemotedException(String message,
                            Throwable t,
                            StackTraceElement[] stackTrace)
    {
        super(message, t);

        setStackTrace(stackTrace);
    }
}
