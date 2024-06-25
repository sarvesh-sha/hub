/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.bacnet.model.pdu.request.confirmed;

import java.util.Optional;

import com.optio3.lang.Unsigned16;
import com.optio3.protocol.bacnet.model.pdu.request.ConfirmedServiceRequest;
import com.optio3.protocol.model.bacnet.enums.BACnetDeviceEnableDisable;
import com.optio3.serialization.SerializationTag;

public class DeviceCommunicationControl extends ConfirmedServiceRequest
{
    @SerializationTag(number = 0)
    public Optional<Unsigned16> time_duration;

    @SerializationTag(number = 1)
    public BACnetDeviceEnableDisable enable_disable;

    @SerializationTag(number = 2)
    public Optional<String> password;
}
