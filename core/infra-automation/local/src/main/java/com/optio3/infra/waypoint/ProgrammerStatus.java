/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

public enum ProgrammerStatus
{
    // @formatter:off
	Connecting           (1 <<  0),
    Connected            (1 <<  1),
    Reading              (1 <<  2),
    Writing              (1 <<  3),
    Resetting            (1 <<  4),
    Erasing              (1 <<  5),
    WritingPage          (1 <<  6),
    ErasingAndWritingPage(1 <<  7),
    Failure              (1 << 15);
	// @formatter:on

    private final int m_mask;

    ProgrammerStatus(int encoding)
    {
        m_mask = encoding;
    }

    public boolean isSet(int val)
    {
        return (val & m_mask) != 0;
    }
}
