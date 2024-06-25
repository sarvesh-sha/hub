/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class NotImplementedException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public NotImplementedException(String message)
    {
        super(message);
    }

    public NotImplementedException(String message,
                                   String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public NotImplementedException(String message,
                                   Throwable t)
    {
        super(message, t);
    }
}
