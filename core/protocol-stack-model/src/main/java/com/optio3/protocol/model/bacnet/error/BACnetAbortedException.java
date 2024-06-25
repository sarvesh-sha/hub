/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

import com.optio3.protocol.model.bacnet.enums.BACnetAbortReason;

public class BACnetAbortedException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public final BACnetAbortReason reason;

    public BACnetAbortedException(String reason)
    {
        super(reason);

        this.reason = null;
    }

    public BACnetAbortedException(BACnetAbortReason reason)
    {
        super("Aborted with " + reason);

        this.reason = reason;
    }
}
