/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.logging;

public enum Severity
{
    Error(0),
    Warn(1),
    Info(2),
    Debug(3),
    DebugVerbose(4),
    DebugObnoxious(5);

    private final int m_order;

    Severity(int order)
    {
        m_order = order;
    }

    public int order()
    {
        return m_order;
    }
}
