/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class AlreadyExistsException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public AlreadyExistsException(String message)
    {
        super(message);
    }

    public AlreadyExistsException(String message,
                                  String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public AlreadyExistsException(String message,
                                  Throwable t)
    {
        super(message, t);
    }
}
