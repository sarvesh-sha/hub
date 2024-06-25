/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class NotAuthenticatedException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public NotAuthenticatedException(String message)
    {
        super(message);
    }

    public NotAuthenticatedException(String message,
                                     String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public NotAuthenticatedException(String message,
                                     Throwable t)
    {
        super(message, t);
    }
}
