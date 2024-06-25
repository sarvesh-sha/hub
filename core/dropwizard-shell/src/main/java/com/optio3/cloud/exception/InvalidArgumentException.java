/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class InvalidArgumentException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(String message)
    {
        super(message);
    }

    public InvalidArgumentException(String message,
                                    String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public InvalidArgumentException(String message,
                                    Throwable t)
    {
        super(message, t);
    }
}
