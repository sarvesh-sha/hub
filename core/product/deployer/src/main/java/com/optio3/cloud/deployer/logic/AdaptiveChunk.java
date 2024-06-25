/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

public class AdaptiveChunk
{
    private final Stopwatch m_st        = Stopwatch.createUnstarted();
    private       int       m_chunkSize = 32 * 1024;

    public int size()
    {
        return m_chunkSize;
    }

    public void start()
    {
        m_st.reset();
        m_st.start();
    }

    public void stop()
    {
        long chunkTime = m_st.elapsed(TimeUnit.MILLISECONDS);

        if (chunkTime > 2000)
        {
            m_chunkSize = Math.max(4096, (int) (m_chunkSize * 0.8));
        }
        else if (chunkTime < 1000)
        {
            //
            // The MessageBus maximum frame is around 1MB.
            // Content is Base64-encoded and then compressed, let's assume it doubles in size.
            // Limit maximum encoded chunk to half the frame limit.
            //
            m_chunkSize = Math.min(256 * 1024, (int) (m_chunkSize * 1.2));
        }
    }
}