/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.util.List;
import java.util.WeakHashMap;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.bacnet.enums.BACnetEngineeringUnits;
import com.optio3.util.CollectionUtils;

public class UnitsClassifier
{
    private final NormalizationEngine                                                     m_engine;
    private final WeakHashMap<String, List<NormalizationScore.Context<EngineeringUnits>>> m_cache = new WeakHashMap<>();

    public UnitsClassifier(SessionProvider sessionProvider,
                           NormalizationRules rules)
    {
        m_engine = new NormalizationEngine(sessionProvider, rules, true);

        m_engine.configure(NormalizationEngine.Settings.AddUnknownTerms, (Function<String, NormalizationRules.KnownTerm>) text ->
        {
            NormalizationRules.KnownTerm term = new NormalizationRules.KnownTerm();
            term.positiveWeight = 1.0;
            term.negativeWeight = 0.3; // Don't penalize too much for missing terms.

            return term;
        });

        for (BACnetEngineeringUnits unit : BACnetEngineeringUnits.values())
        {
            m_engine.extractDimensions(unit.name());
        }
        m_engine.configure(NormalizationEngine.Settings.AddUnknownTerms, null);
    }

    public List<NormalizationScore.Context<EngineeringUnits>> score(String text)
    {
        return m_cache.computeIfAbsent(text, (key) ->
        {

            List<EngineeringUnits>                                    values  = Lists.newArrayList(EngineeringUnits.values());
            NormalizationEngine.PreprocessedTargets<EngineeringUnits> targets = m_engine.preprocessTargets(values, (unit) -> unit.name());

            return m_engine.score(key, targets, null, null);
        });
    }

    public NormalizationScore.Context<EngineeringUnits> scoreTop(String text)
    {
        return CollectionUtils.firstElement(score(text));
    }
}
