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

public class HeatingCoolingSignal extends ObjectGenerator<analog_value>
{
    private int m_defaultPercent;
    private int m_businessHoursPercent;

    public HeatingCoolingSignal(String name,
                                int instanceNumber,
                                int defaultPercent,
                                int businessHoursPercent)
    {
        super(name, instanceNumber, BACnetObjectType.analog_value, BACnetEngineeringUnits.percent, analog_value.class);
        m_defaultPercent       = defaultPercent;
        m_businessHoursPercent = businessHoursPercent;
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            analog_value av)
    {
        av.setValue(BACnetPropertyIdentifier.present_value, getPresentSignal(now));
        av.setValue(BACnetPropertyIdentifier.event_state, getEventState());
        av.setValue(BACnetPropertyIdentifier.status_flags, getStatusFlags());
        av.setValue(BACnetPropertyIdentifier.out_of_service, getOutOfService());
    }

    private int getPresentSignal(ZonedDateTime now)
    {
        if (isDuringBusinessHours(now))
        {
            return m_businessHoursPercent;
        }
        else
        {
            return m_defaultPercent;
        }
    }
}
