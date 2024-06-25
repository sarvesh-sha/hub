/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.exception;

import com.optio3.cloud.model.ValidationResults;

public class InvalidStateException extends DetailedApplicationException
{
    private static final long serialVersionUID = 1L;

    public ValidationResults validationErrors;

    public InvalidStateException(String message)
    {
        super(message);
    }

    public InvalidStateException(String message,
                                 String exceptionTrace)
    {
        super(message, exceptionTrace);
    }

    public InvalidStateException(String message,
                                 Throwable t)
    {
        super(message, t);
    }

    @Override
    protected void postAllocation(ErrorDetails details)
    {
        super.postAllocation(details);

        this.validationErrors = details.validationErrors;
    }

    @Override
    public ErrorDetails getDetails()
    {
        return getDetails(validationErrors);
    }
}
