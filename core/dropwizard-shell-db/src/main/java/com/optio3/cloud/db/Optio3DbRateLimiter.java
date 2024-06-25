/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.db;

public enum Optio3DbRateLimiter
{
    Normal(25),
    Background(10),
    System(5),
    HighPriority(0);

    private final double m_minimumAvailablePercent;

    Optio3DbRateLimiter(int minimumAvailablePercent)
    {
        m_minimumAvailablePercent = minimumAvailablePercent / 100.0;
    }

    int getRequiredPermits(int maxSize)
    {
        if (m_minimumAvailablePercent <= 0)
        {
            return 0;
        }

        return Math.max(1, (int) Math.ceil(maxSize * m_minimumAvailablePercent));
    }
}
