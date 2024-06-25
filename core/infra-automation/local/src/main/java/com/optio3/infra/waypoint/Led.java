/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.waypoint;

public enum Led
{
    // @formatter:off
	Error (0, null),
    Serial(1, null),
    Can1  (2, null),
    Can2  (3, null),
    Red   (-1, "red"),
    Green (-1, "green"),
    Blue  (-1, "blue");
	// @formatter:on

    private final byte   m_id;
    private final String m_name;

    Led(int id,
        String name)
    {
        m_id = (byte) id;
        m_name = name;
    }

    public byte getId()
    {
        return m_id;
    }

    public String getName()
    {
        return m_name;
    }
}
