/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

public class BACnetNotSupportedException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public BACnetNotSupportedException(String message)
    {
        super(message);
    }

    public BACnetNotSupportedException(String message,
                                       Throwable t)
    {
        super(message, t);
    }
}
