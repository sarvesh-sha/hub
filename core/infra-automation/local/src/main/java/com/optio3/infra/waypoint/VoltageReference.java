/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

public enum VoltageReference
{
    // @formatter:off
	Vdd  (0),
    V2_5 (1),
    V1_5 (2),
    V1_1 (3),
    V0_55(4);
	// @formatter:on

    private final byte m_id;

    VoltageReference(int id)
    {
        m_id = (byte) id;
    }

    public byte getId()
    {
        return m_id;
    }
}
