/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetObjectType;
import com.optio3.protocol.model.bacnet.enums.BACnetPropertyIdentifier;
import com.optio3.protocol.model.bacnet.objects.analog_input;

public class CO2Sensor extends ObjectGenerator<analog_input>
{
    private double m_low;
    private double m_high;

    public CO2Sensor(String name,
                     int instanceNumber,
                     double low,
                     double high)
    {
        super(name, instanceNumber, BACnetObjectType.analog_input, BACnetEngineeringUnits.parts_per_million, analog_input.class);
        m_low  = low;
        m_high = high;
    }

    @Override
    public void readSamples(ZonedDateTime now,
                            analog_input ai)
    {
        ai.setValue(BACnetPropertyIdentifier.event_state, getEventState());
        ai.setValue(BACnetPropertyIdentifier.status_flags, getStatusFlags());
        ai.setValue(BACnetPropertyIdentifier.out_of_service, getOutOfService());
        ai.setValue(BACnetPropertyIdentifier.present_value, (float) getCO2Value(now));
    }

    private double getCO2Value(ZonedDateTime now)
    {
        ZonedDateTime time                = getPSTTime(now);
        ZonedDateTime midnight            = time.truncatedTo(ChronoUnit.DAYS);
        double        secondsFromMidnight = midnight.until(time, ChronoUnit.SECONDS);
        double        percentage          = secondsFromMidnight / (24 * 60 * 60);
        double        amplitude           = m_high - m_low;
        double        randomNoise         = Math.random() * 10 > 5 ? Math.random() * amplitude / 10 - amplitude / 20 : 0;

        if (isWeekend(time) || !isDuringBusinessHours(time))
        {
            return m_low + randomNoise;
        }

        return m_low + amplitude * Math.sin(percentage * 2 * Math.PI - Math.PI / 2) + randomNoise;
    }
}
