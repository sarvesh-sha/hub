/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model.obdii.pgn.enums;

import com.optio3.serialization.HandlerForDecoding;
import com.optio3.serialization.HandlerForEncoding;
import com.optio3.serialization.TypedBitSet;

public enum PgnEngineStarterMode implements TypedBitSet.ValueGetter
{
    // @formatter:off
    StartNotRequested                                              (0),
    StarterActiveButNotEngaged                                     (1),
    StarterActiveAndEngaged                                        (2),
    StartFinished                                                  (3),
    StarterInhibitedDueToEngineAlreadyRunning                      (4),
    StarterInhibitedDueToEngineNotReadyForStart                    (5),
    StarterInhibitedDueToDrivelineEngagedOrOtherTransmissionInhibit(6),
    StarterInhibitedDueToActiveImmobilizer                         (7),
    StarterInhibitedDueToStarterOverTemp                           (8),
    StarterInhibitedDueToIntakeAirShutoffValveBeingActive          (9),
    StarterInhibitedDueToActiveEmissionsControlSystemCondition     (10),
    StarterInhibitedDueToIgnitionKeyCycleRequired                  (11),
    StarterInhibitedReasonUnknown                                  (12),
    Error13                                                        (13),
    Error14                                                        (14),
    NotAvailable                                                   (15);
    // @formatter:on

    private final byte m_encoding;

    PgnEngineStarterMode(int encoding)
    {
        m_encoding = (byte) encoding;
    }

    @HandlerForDecoding
    public static PgnEngineStarterMode parse(byte value)
    {
        for (PgnEngineStarterMode t : values())
        {
            if (t.m_encoding == value)
            {
                return t;
            }
        }

        return null;
    }

    @HandlerForEncoding
    public byte encoding()
    {
        return m_encoding;
    }

    @Override
    public int getEncodingValue()
    {
        return m_encoding;
    }
}
