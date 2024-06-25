/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class NotAuthorizedException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public NotAuthorizedException(String message)
    {
        super(message);
    }

    public NotAuthorizedException(String message,
                                  String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public NotAuthorizedException(String message,
                                  Throwable t)
    {
        super(message, t);
    }
}
