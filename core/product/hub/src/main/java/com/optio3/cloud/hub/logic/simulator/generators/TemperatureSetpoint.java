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
import com.optio3.protocol.model.bacnet.objects.analog_value;

public class TemperatureSetpoint extends ObjectGenerator<analog_value>
{
    private double m_low;
    private double m_high;

    public TemperatureSetpoint(String name,
                               int instanceNumber,
                               double low,
                               double high)
    {
        super(name, instanceNumber, BACnetObjectType.analog_value, BACnetEngineeringUnits.degrees_fahrenheit, analog_value.class);
        m_low  = low;
        m_high = high;
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            analog_value av)
    {
        av.setValue(BACnetPropertyIdentifier.present_value, (float) (isDuringBusinessHours(now) ? m_high : m_low));
        av.setValue(BACnetPropertyIdentifier.event_state, getEventState());
        av.setValue(BACnetPropertyIdentifier.status_flags, getStatusFlags());
        av.setValue(BACnetPropertyIdentifier.out_of_service, getOutOfService());
    }
}
