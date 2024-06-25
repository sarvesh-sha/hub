/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.statistics;

import com.google.common.base.Ticker;

public class TimeMeasurer implements AutoCloseable
{
    final Measure m_target;
    final long    m_start;

    public TimeMeasurer(Measure target)
    {
        m_target = target;
        m_start = getSample();
    }

    @Override
    public void close()
    {
        long stop = getSample();

        m_target.addSample(stop - m_start);
    }

    private static long getSample()
    {
        return Ticker.systemTicker()
                     .read();
    }
}
