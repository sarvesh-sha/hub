/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.orchestration.state;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.Sets;

class SamplingConfigPerObject<TDevice, TObject, TProperty> implements ISamplingConfig<TObject, SamplingConfigPerObject<TDevice, TObject, TProperty>>
{
    private final TObject        m_object;
    private       Set<TProperty> m_state = Collections.emptySet();

    //--//

    SamplingConfigPerObject(TObject object)
    {
        m_object = object;
    }

    //--//

    @Override
    public TObject getKey()
    {
        return m_object;
    }

    //--//

    void add(SamplingConfig.Statistics stats,
             Collection<TProperty> propIds,
             Function<Set<TProperty>, Set<TProperty>> memoizingCallback)
    {
        Set<TProperty> set = Sets.newHashSet(m_state);
        set.addAll(propIds);

        int numBefore = m_state.size();
        m_state = memoizingCallback != null ? memoizingCallback.apply(set) : set;
        int numAfter = m_state.size();
        stats.countProperties += (numAfter - numBefore);
    }

    void enumerate(int period,
                   TDevice deviceId,
                   SamplingEnumerator<TDevice, TObject, TProperty> callback)
    {
        for (TProperty propId : m_state)
        {
            callback.accept(period, deviceId, m_object, propId);
        }
    }

    //--//

    void prepareBatches(TDevice deviceId,
                        SamplingBatcher<TDevice, TObject, TProperty> callback)
    {
        for (TProperty propId : m_state)
        {
            callback.addSample(deviceId, m_object, propId);
        }
    }
}
