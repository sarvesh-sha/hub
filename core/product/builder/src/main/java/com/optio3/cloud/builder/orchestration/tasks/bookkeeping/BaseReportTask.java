/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.orchestration.AbstractBuilderActivityHandler;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class BaseReportTask extends AbstractBuilderActivityHandler
{
    public static class ReportFlusher
    {
        private final int            n_intervalInMillisec;
        private       MonotonousTime m_batchTimeout;

        public ReportFlusher(int intervalInMillisec)
        {
            n_intervalInMillisec = intervalInMillisec;

            updateTimeout();
        }

        public void updateTimeout()
        {
            m_batchTimeout = TimeUtils.computeTimeoutExpiration(n_intervalInMillisec, TimeUnit.MILLISECONDS);
        }

        public boolean shouldReport()
        {
            if (TimeUtils.isTimeoutExpired(m_batchTimeout))
            {
                updateTimeout();
                return true;
            }
            return false;
        }
    }
}
