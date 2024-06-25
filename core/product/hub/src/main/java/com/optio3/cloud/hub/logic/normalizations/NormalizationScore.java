/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.Set;

import com.google.common.collect.Sets;

public class NormalizationScore
{
    public static class Context<K>
    {
        public final NormalizationScore score;
        public final K                  context;

        Context(NormalizationScore score,
                K context)
        {
            this.score   = score;
            this.context = context;
        }
    }

    public double positiveScore;
    public double negativeScore;

    public Set<NormalizationTerm> matchingDimensions = Sets.newHashSet();

    public double getTotalScore()
    {
        return positiveScore + negativeScore;
    }

    public <K> Context<K> getContext(K context)
    {
        return new Context<>(this, context);
    }
}
