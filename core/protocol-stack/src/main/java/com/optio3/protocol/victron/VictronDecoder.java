/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.victron;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.optio3.logging.Logger;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Decoder for GPS adapters using NMEA format, see http://www.gpsinformation.org/dale/nmea.htm
 */
public abstract class VictronDecoder
{
    public static final Logger LoggerInstance = new Logger(VictronDecoder.class);

    private enum DecoderState
    {
        Idle,
        Success,
        Failure,
    }

    private DecoderState   m_currentState = DecoderState.Idle;
    private MonotonousTime m_deadlineForTransportReset;

    public boolean process(String line) throws
                                        IOException
    {
        String[] parts = StringUtils.splitPreserveAllTokens(line, "\t");

        if (parts.length == 2)
        {
            String key   = parts[0];
            String value = parts[1];

            if (StringUtils.isNotBlank(key))
            {
                switch (key)
                {
                    case "PID":
                    case "VER#":
                        transitionToSuccess();
                        break;
                }

                if (m_currentState == DecoderState.Success)
                {
                    return reportValue(key, value);
                }
            }
        }

        return false;
    }

    protected abstract boolean reportValue(String key,
                                           String value);

    //--//

    private int parseInt(String[] parts,
                         int index,
                         int defaultValue)
    {
        String val = parts[index];
        if (StringUtils.isEmpty(val))
        {
            return defaultValue;
        }

        try
        {
            return Integer.valueOf(val);
        }
        catch (NumberFormatException t)
        {
            return defaultValue;
        }
    }

    private double parseDouble(String[] parts,
                               int index,
                               double defaultValue)
    {
        String val = parts[index];
        if (StringUtils.isEmpty(val))
        {
            return defaultValue;
        }

        try
        {
            return Double.valueOf(val);
        }
        catch (NumberFormatException t)
        {
            return defaultValue;
        }
    }

    //--//

    public void transitionToSuccess()
    {
        m_deadlineForTransportReset = null;

        switch (m_currentState)
        {
            case Failure:
                LoggerInstance.info("Victron data flow resumed!");
                // Fallthrough

            case Idle:
                m_currentState = DecoderState.Success;
                break;
        }
    }

    public void transitionToFailure()
    {
        switch (m_currentState)
        {
            case Idle:
            case Success:
                if (m_deadlineForTransportReset == null)
                {
                    m_deadlineForTransportReset = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);
                }

                if (TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
                {
                    LoggerInstance.error("Victron not receiving any data...");

                    m_currentState = DecoderState.Failure;
                }
                break;
        }
    }

    public boolean shouldCloseTransport()
    {
        if (m_deadlineForTransportReset != null && TimeUtils.isTimeoutExpired(m_deadlineForTransportReset))
        {
            m_deadlineForTransportReset = TimeUtils.computeTimeoutExpiration(2, TimeUnit.MINUTES);

            return true;
        }

        return false;
    }
}