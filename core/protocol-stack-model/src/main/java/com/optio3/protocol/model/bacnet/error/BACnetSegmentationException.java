/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

public class BACnetSegmentationException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public BACnetSegmentationException(String message)
    {
        super(message);
    }

    public BACnetSegmentationException(String message,
                                       Throwable t)
    {
        super(message, t);
    }
}
