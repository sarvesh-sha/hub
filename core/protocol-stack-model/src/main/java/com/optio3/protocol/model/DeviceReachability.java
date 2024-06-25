/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.protocol.model;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class DeviceReachability
{
    @FunctionalInterface
    public interface ReachabilityCallback
    {
        boolean shouldCommitReachabilityChange(boolean isReachable,
                                               ZonedDateTime lastReachable) throws
                                                                            Exception;
    }

    public static class State
    {
        private final int            m_unreachableDelay;
        private       boolean        m_wasReachable;
        private       ZonedDateTime  m_lastReachable;
        private       Boolean        m_lastReachableReport;
        private       MonotonousTime m_initialGracePeriod;

        public State(int unreachableDelayInSeconds)
        {
            m_unreachableDelay   = unreachableDelayInSeconds;
            m_initialGracePeriod = TimeUtils.computeTimeoutExpiration(unreachableDelayInSeconds, TimeUnit.SECONDS);
        }

        public void markAsReachable()
        {
            m_wasReachable       = true;
            m_lastReachable      = TimeUtils.now();
            m_initialGracePeriod = null;
        }

        public void markAsUnreachable()
        {
            m_wasReachable = false;
        }

        public boolean wasReachable()
        {
            return m_wasReachable || TimeUtils.wasUpdatedRecently(m_lastReachable, m_unreachableDelay, TimeUnit.SECONDS);
        }

        public ZonedDateTime getLastReachable()
        {
            return m_lastReachable;
        }

        public void reportReachabilityChange(ReachabilityCallback callback) throws
                                                                            Exception
        {
            boolean wasReachable = wasReachable();
            if (!Objects.equals(m_lastReachableReport, wasReachable))
            {
                if (!TimeUtils.isTimeoutExpired(m_initialGracePeriod))
                {
                    // Wait a bit on startup for the device to report.
                    return;
                }

                m_initialGracePeriod = null;

                if (callback.shouldCommitReachabilityChange(wasReachable, m_lastReachable))
                {
                    m_lastReachableReport = wasReachable;
                }
            }
        }
    }

    public boolean reachable;

    public ZonedDateTime lastReachable;
}
