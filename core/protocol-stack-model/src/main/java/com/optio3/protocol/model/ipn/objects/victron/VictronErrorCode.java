/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.ipn.objects.victron;

public enum VictronErrorCode
{
    // @formatter:off
    NoError                   (0),
    BatteryVoltageTooHigh     (2),
    ChargerTemperatureTooHigh (17),
    ChargerOverCurrent        (18),
    ChargerCurrentReversed    (19),
    BulkTimeLimitExceeded     (20),
    CurrentSensorIssue        (21),
    TerminalsOverheated       (26),
    InputVoltageTooHigh       (33),
    InputCurrentTooHigh       (34),
    InputShutdown             (38),
    FactoryCalibrationDataLost(116),
    InvalidFirmware           (117),
    UserSettingsInvalid       (119);
    // @formatter:on

    private final int m_encoding;

    VictronErrorCode(int encoding)
    {
        m_encoding = encoding;
    }

    public static VictronErrorCode parse(int value)
    {
        for (VictronErrorCode t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }
}
