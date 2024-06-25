/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.simulator.generators;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;

public class SpaceTemperatureSensor extends TemperatureSensor
{
    private double m_low;
    private double m_high;
    private double m_weekendLow;
    private double m_temperatureFluctuation;
    private double m_temperatureFluctuationMax;
    private double m_temperatureFluctuationMin;
    private Random m_random;

    public SpaceTemperatureSensor(String name,
                                  int instanceNumber,
                                  double low,
                                  double high,
                                  double weekendLow)
    {
        super(name, instanceNumber);
        m_low                       = low;
        m_high                      = high;
        m_weekendLow                = weekendLow;
        m_temperatureFluctuation    = (high - low) / 2;
        m_temperatureFluctuationMin = m_temperatureFluctuation * .75;
        m_temperatureFluctuationMax = m_temperatureFluctuation * 1.25;
        m_random                    = new Random();
    }

    public double getPresentTemperature(ZonedDateTime now)
    {
        ZonedDateTime time                = getPSTTime(now);
        ZonedDateTime midnight            = time.truncatedTo(ChronoUnit.DAYS);
        double        secondsFromMidnight = midnight.until(time, ChronoUnit.SECONDS);
        double        percentage          = secondsFromMidnight / (24 * 60 * 60);
        int           rand                = m_random.nextInt(10);

        // Add some random noise for temperature fluctuations
        if (rand == 0)
        {
            m_temperatureFluctuation += .1;
            m_temperatureFluctuation = Math.min(m_temperatureFluctuation, m_temperatureFluctuationMax);
        }
        else if (rand == 1)
        {
            m_temperatureFluctuation += -.1;
            m_temperatureFluctuation = Math.max(m_temperatureFluctuation, m_temperatureFluctuationMin);
        }

        double randomNoise = .1 * Math.random() - .05;

        if (isWeekend(time))
        {
            percentage = getPercentOfWeekend(time);
            return ((m_high + m_low) / 2 - m_temperatureFluctuation - m_weekendLow) * Math.pow(percentage - 1, 2) + m_weekendLow + randomNoise;
        }

        return (m_high + m_low) / 2 + m_temperatureFluctuation * Math.sin(percentage * 2 * Math.PI - Math.PI / 2) + randomNoise;
    }
}
