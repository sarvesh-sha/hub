/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

import com.optio3.protocol.model.bacnet.enums.BACnetRejectReason;

public class BACnetRejectedException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public final BACnetRejectReason reason;

    public BACnetRejectedException(BACnetRejectReason reason)
    {
        super("Rejected with " + reason);

        this.reason = reason;
    }
}
