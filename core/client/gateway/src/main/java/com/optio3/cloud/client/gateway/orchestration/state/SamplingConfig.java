/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public class SamplingConfig<TDevice, TObject, TProperty>
{
    public static class Statistics
    {
        private static final int REPORT_REFRESH = 30;

        public int countDevices;
        public int countObjects;
        public int countProperties;

        private MonotonousTime m_nextReport;

        Statistics()
        {
            setTimeout();
        }

        public boolean shouldReport()
        {
            if (TimeUtils.isTimeoutExpired(m_nextReport))
            {
                setTimeout();
                return true;
            }

            return false;
        }

        public void setTimeout()
        {
            m_nextReport = TimeUtils.computeTimeoutExpiration(REPORT_REFRESH, TimeUnit.SECONDS);
        }
    }

    private final SamplingConfigContainer<Integer, SamplingConfigPerPeriod<TDevice, TObject, TProperty>> m_state = new SamplingConfigContainer<>();

    public final Statistics stats = new Statistics();

    public Set<Integer> getPeriods()
    {
        return m_state.asMap()
                      .keySet();
    }

    public void add(Integer period,
                    TDevice deviceId,
                    TObject objectId,
                    Collection<TProperty> propIds,
                    Function<Set<TProperty>, Set<TProperty>> memoizingCallback)
    {
        SamplingConfigPerPeriod<TDevice, TObject, TProperty> cfg = m_state.find(period);
        if (cfg == null)
        {
            cfg = new SamplingConfigPerPeriod<>(period);
            m_state.add(cfg);
        }

        cfg.add(stats, deviceId, objectId, propIds, memoizingCallback);
    }

    public synchronized void enumerate(int period,
                                       SamplingEnumerator<TDevice, TObject, TProperty> callback)
    {
        for (ISamplingConfig<Integer, SamplingConfigPerPeriod<TDevice, TObject, TProperty>> pair : m_state.asArray())
        {
            Integer periodKey = pair.getKey();
            if (period >= 0 && periodKey != period)
            {
                continue;
            }

            pair.asValue()
                .enumerate(periodKey, callback);
        }
    }

    public synchronized long computeNextWakeup(SamplingRequest sr)
    {
        SamplingConfigPerPeriod<TDevice, TObject, TProperty> cfg = m_state.find(sr.period);
        return cfg != null ? cfg.computeNextWakeup(sr) : TimeUtils.maxEpochSeconds();
    }

    public synchronized void prepareBatches(long nowEpochSeconds,
                                            int period,
                                            SamplingBatcher<TDevice, TObject, TProperty> callback)
    {
        SamplingConfigPerPeriod<TDevice, TObject, TProperty> cfg = m_state.find(period);
        if (cfg != null)
        {
            cfg.prepareBatches(nowEpochSeconds, callback);
        }
    }

    public synchronized void doneWithCurrentSample(int period)
    {
        SamplingConfigPerPeriod<TDevice, TObject, TProperty> cfg = m_state.find(period);
        if (cfg != null)
        {
            cfg.doneWithCurrentSample();
        }
    }
}
