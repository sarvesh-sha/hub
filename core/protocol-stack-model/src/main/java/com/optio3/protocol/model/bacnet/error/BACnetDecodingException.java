/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

import com.optio3.util.Exceptions;

public class BACnetDecodingException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public BACnetDecodingException(String message)
    {
        super(message);
    }

    public BACnetDecodingException(String message,
                                   Throwable t)
    {
        super(message, t);
    }

    public static BACnetDecodingException newException(String fmt,
                                                       Object... args)
    {
        return Exceptions.newGenericException(BACnetDecodingException.class, fmt, args);
    }
}
