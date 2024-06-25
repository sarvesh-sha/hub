/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.ZonedDateTime;

public class DischargeAirTemperatureSensor extends TemperatureSensor
{
    private double        m_low;
    private double        m_high;
    private double        m_higher;
    private ZonedDateTime m_endOfDay;

    public DischargeAirTemperatureSensor(String name,
                                         int instanceNumber,
                                         double low,
                                         double high,
                                         double higher)
    {
        super(name, instanceNumber);
        m_low    = low;
        m_high   = high;
        m_higher = higher;
    }

    public double getPresentTemperature(ZonedDateTime now)
    {
        ZonedDateTime time        = getPSTTime(now);
        double        randomNoise = (Math.random() - .5) * 2;

        if (isDuringBusinessHours(time))
        {
            if (m_endOfDay != null)
            {
                m_endOfDay = null;
            }
            return m_high + (m_higher - m_high) * getPercentOfBusinessDay(time) + randomNoise;
        }
        else
        {
            if (m_endOfDay == null)
            {
                m_endOfDay = time;
            }
            double percentage = getPercentage(m_endOfDay, time, 6 * 60 * 60);
            return (m_higher - m_low) * Math.pow(percentage - 1, 2) + m_low + randomNoise;
        }
    }
}
