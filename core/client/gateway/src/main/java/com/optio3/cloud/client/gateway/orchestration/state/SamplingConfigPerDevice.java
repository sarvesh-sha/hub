/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

class SamplingConfigPerDevice<TDevice, TObject, TProperty> implements ISamplingConfig<TDevice, SamplingConfigPerDevice<TDevice, TObject, TProperty>>
{
    private final TDevice                                                                                m_device;
    private final SamplingConfigContainer<TObject, SamplingConfigPerObject<TDevice, TObject, TProperty>> m_state = new SamplingConfigContainer<>();

    //--//

    SamplingConfigPerDevice(TDevice device)
    {
        m_device = device;
    }

    //--//

    @Override
    public TDevice getKey()
    {
        return m_device;
    }

    //--//

    void add(SamplingConfig.Statistics stats,
             TObject objectId,
             Collection<TProperty> propIds,
             Function<Set<TProperty>, Set<TProperty>> memoizingCallback)
    {
        SamplingConfigPerObject<TDevice, TObject, TProperty> cfg = m_state.find(objectId);
        if (cfg == null)
        {
            cfg = new SamplingConfigPerObject<>(objectId);
            m_state.add(cfg);
            stats.countObjects++;
        }

        cfg.add(stats, propIds, memoizingCallback);
    }

    void enumerate(int period,
                   SamplingEnumerator<TDevice, TObject, TProperty> callback)
    {
        for (ISamplingConfig<TObject, SamplingConfigPerObject<TDevice, TObject, TProperty>> pair : m_state.asArray())
        {
            pair.asValue()
                .enumerate(period, m_device, callback);
        }
    }

    //--//

    void prepareBatches(SamplingBatcher<TDevice, TObject, TProperty> callback)
    {
        for (ISamplingConfig<TObject, SamplingConfigPerObject<TDevice, TObject, TProperty>> pair : m_state.asArray())
        {
            pair.asValue()
                .prepareBatches(m_device, callback);
        }
    }
}
