/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

public abstract class BACnetException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    protected BACnetException(String message)
    {
        super(message);
    }

    protected BACnetException(String message,
                              Throwable t)
    {
        super(message, t);
    }
}
