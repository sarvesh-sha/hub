/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

public class NotFoundException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public NotFoundException(String message)
    {
        super(message);
    }

    public NotFoundException(String message,
                             String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public NotFoundException(String message,
                             Throwable t)
    {
        super(message, t);
    }
}
