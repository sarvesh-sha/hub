/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.ZonedDateTime;

import com.optio3.protocol.model.bacnet.enums.BACnetBinaryPV;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.objects.binary_input;

public class BinaryStatus extends ObjectGenerator<binary_input>
{
    public BinaryStatus(String name,
                        int instanceNumber)
    {
        super(name, instanceNumber, BACnetObjectType.binary_input, BACnetEngineeringUnits.no_units, binary_input.class);
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            binary_input bi)
    {
        bi.setValue(BACnetPropertyIdentifier.event_state, getEventState());
        bi.setValue(BACnetPropertyIdentifier.out_of_service, getOutOfService());
        bi.setValue(BACnetPropertyIdentifier.status_flags, getStatusFlags());
        bi.setValue(BACnetPropertyIdentifier.present_value, isDuringBusinessHours(now) ? BACnetBinaryPV.active : BACnetBinaryPV.inactive);
    }
}
