/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.statistics;

import java.util.concurrent.atomic.AtomicLong;

public class Measure
{
    private final AtomicLong m_counts  = new AtomicLong();
    private final AtomicLong m_elapsed = new AtomicLong();

    public void addSample(long l)
    {
        m_counts.incrementAndGet();
        m_elapsed.addAndGet(l);
    }

    @Override
    public String toString()
    {
        return String.format("counts=%d elapsed=%,dnsec", m_counts.get(), m_elapsed.get());
    }
}
