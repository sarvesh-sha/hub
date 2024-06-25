/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

public enum DbAction
{
    UPDATE_INDIRECT(0),
    UPDATE_DIRECT(1),
    INSERT(2),
    DELETE(3);

    private final int m_priority;

    DbAction(int priority)
    {
        this.m_priority = priority;
    }

    public int getPriority()
    {
        return m_priority;
    }
}
