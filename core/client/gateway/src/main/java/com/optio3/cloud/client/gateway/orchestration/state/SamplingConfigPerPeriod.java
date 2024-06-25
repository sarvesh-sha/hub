/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import com.optio3.util.TimeUtils;

class SamplingConfigPerPeriod<TDevice, TObject, TProperty> implements ISamplingConfig<Integer, SamplingConfigPerPeriod<TDevice, TObject, TProperty>>
{
    private final Integer                                                                                m_period;
    private final SamplingConfigContainer<TDevice, SamplingConfigPerDevice<TDevice, TObject, TProperty>> m_state = new SamplingConfigContainer<>();

    private long m_lastSample    = TimeUtils.minEpochSeconds();
    private long m_nextSample    = TimeUtils.maxEpochSeconds();
    private long m_currentSample = TimeUtils.maxEpochSeconds();

    //--//

    SamplingConfigPerPeriod(int period)
    {
        m_period = period;
    }

    //--//

    @Override
    public Integer getKey()
    {
        return m_period;
    }

    //--//

    void add(SamplingConfig.Statistics stats,
             TDevice deviceId,
             TObject objectId,
             Collection<TProperty> propIds,
             Function<Set<TProperty>, Set<TProperty>> memoizingCallback)
    {
        SamplingConfigPerDevice<TDevice, TObject, TProperty> cfg = m_state.find(deviceId);
        if (cfg == null)
        {
            cfg = new SamplingConfigPerDevice<>(deviceId);
            m_state.add(cfg);
            stats.countDevices++;
        }

        cfg.add(stats, objectId, propIds, memoizingCallback);
    }

    void enumerate(int period,
                   SamplingEnumerator<TDevice, TObject, TProperty> callback)
    {
        for (ISamplingConfig<TDevice, SamplingConfigPerDevice<TDevice, TObject, TProperty>> pair : m_state.asArray())
        {
            pair.asValue()
                .enumerate(period, callback);
        }
    }

    //--//

    long computeNextWakeup(SamplingRequest sr)
    {
        m_nextSample = Math.max(m_lastSample + sr.period, sr.nowEpochSeconds);

        //
        // Align sampling time to a multiple of the sampling period.
        // That way we always sample at the same time, every day.
        //
        m_nextSample = TimeUtils.adjustGranularity(m_nextSample, sr.period);

        //
        // Next sample in the past? Adjust to the next period.
        //
        if (m_nextSample < sr.nowEpochSeconds)
        {
            m_nextSample += sr.period;
        }

        return m_nextSample;
    }

    void prepareBatches(long nowEpochSeconds,
                        SamplingBatcher<TDevice, TObject, TProperty> callback)
    {
        if (m_nextSample <= nowEpochSeconds)
        {
            m_currentSample = m_nextSample;

            for (ISamplingConfig<TDevice, SamplingConfigPerDevice<TDevice, TObject, TProperty>> pair : m_state.asArray())
            {
                pair.asValue()
                    .prepareBatches(callback);
            }
        }
    }

    void doneWithCurrentSample()
    {
        m_lastSample    = m_currentSample;
        m_currentSample = TimeUtils.maxEpochSeconds();
    }
}
