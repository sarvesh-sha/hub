/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.enums.BACnetDeviceState;
import com.optio3.serialization.SerializationTag;

public class ReinitializeDevice extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public BACnetDeviceState reinitialized_state_of_device;

    @SerializationTag(number = 1)
    public Optional<String> password;
}
