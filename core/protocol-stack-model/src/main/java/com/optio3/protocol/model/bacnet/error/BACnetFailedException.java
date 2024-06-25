/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

public class BACnetFailedException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public BACnetFailedException(String message)
    {
        super(message);
    }

    public BACnetFailedException(String message,
                                 Throwable t)
    {
        super(message, t);
    }
}
