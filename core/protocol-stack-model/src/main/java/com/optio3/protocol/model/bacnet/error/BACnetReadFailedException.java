/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.bacnet.error;

import com.optio3.protocol.model.bacnet.BACnetObjectIdentifier;
import com.optio3.protocol.model.bacnet.enums.BACnetErrorClass;
import com.optio3.protocol.model.bacnet.enums.BACnetErrorCode;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifierOrUnknown;

public class BACnetReadFailedException extends BACnetException
{
    private static final long serialVersionUID = 1L;

    public final BACnetObjectIdentifier            objId;
    public final BACnetPropertyIdentifierOrUnknown propId;
    public final BACnetErrorClass                  errorClass;
    public final BACnetErrorCode                   errorCode;

    public BACnetReadFailedException(BACnetObjectIdentifier objId,
                                     BACnetPropertyIdentifierOrUnknown propId,
                                     BACnetErrorClass errorClass,
                                     BACnetErrorCode errorCode)
    {
        super(String.format("Reading property '%s' on object '%s' failed with error %s/%s", propId, objId, errorClass, errorCode));

        this.objId = objId;
        this.propId = propId;
        this.errorClass = errorClass;
        this.errorCode = errorCode;
    }
}
