/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.ZonedDateTime;

import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.objects.analog_input;

public abstract class TemperatureSensor extends ObjectGenerator<analog_input>
{
    public TemperatureSensor(String name,
                             int instanceNumber)
    {
        super(name, instanceNumber, BACnetObjectType.analog_input, BACnetEngineeringUnits.degrees_fahrenheit, analog_input.class);
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            analog_input ai)
    {
        ai.setValue(BACnetPropertyIdentifier.present_value, (float) getPresentTemperature(now));
        ai.setValue(BACnetPropertyIdentifier.event_state, getEventState());
        ai.setValue(BACnetPropertyIdentifier.status_flags, getStatusFlags());
        ai.setValue(BACnetPropertyIdentifier.out_of_service, getOutOfService());
    }

    public abstract double getPresentTemperature(ZonedDateTime now);
}
