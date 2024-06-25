/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

public enum ModemStatus
{
    // @formatter:off
	VoltageOn(1 << 0),
    Status   (1 << 1),
    Reset    (1 << 2),
    OnOff    (1 << 3);
	// @formatter:on

    private final byte m_mask;

    ModemStatus(int encoding)
    {
        m_mask = (byte) encoding;
    }

    public boolean isSet(byte val)
    {
        return (val & m_mask) != 0;
    }
}
