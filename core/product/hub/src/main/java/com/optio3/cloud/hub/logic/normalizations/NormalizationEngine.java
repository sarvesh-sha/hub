/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.logic.normalizations;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionContext;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.model.normalization.NormalizationDefinitionDetails;
import com.optio3.cloud.hub.model.tags.TagsSummary;
import com.optio3.cloud.hub.persistence.asset.AssetRecord;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.serialization.Reflection;
import com.optio3.text.MultiCharacterSplit;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class NormalizationEngine
{
    private static final String[] s_separatorsRaw = new String[] { " ,;:+*()[]{}\"'=", "-", "/_.", "0123456789", };
    private static final String[] s_separators;

    static
    {
        StringBuilder sb = new StringBuilder();

        s_separators = new String[s_separatorsRaw.length];

        for (int i = 0; i < s_separatorsRaw.length; i++)
        {
            sb.append(s_separatorsRaw[i]);
            s_separators[i] = sb.toString();
        }
    }

    //--//

    public static class PreprocessedTargets<K>
    {
        private final List<K>                        m_keys       = Lists.newArrayList();
        private final Map<K, Set<NormalizationTerm>> m_dimensions = Maps.newHashMap();

        private void initialize(NormalizationEngine engine,
                                Collection<K> targets,
                                Function<K, String> mappingCallback)
        {
            for (K target : targets)
            {
                final String s = mappingCallback.apply(target);

                // Record the order, to generate deterministic results.
                m_keys.add(target);
                m_dimensions.put(target, s != null ? engine.extractDimensions(s) : Collections.emptySet());
            }
        }
    }

    public enum Settings
    {
        AddUnknownTerms,
    }

    private static class SynonymGroup
    {
        Set<String> synonyms;
        String      placeHolder;

        SynonymGroup(Set<String> synonyms)
        {
            this.synonyms = Collections.unmodifiableSet(synonyms);

            List<String> sortedTerms = Lists.newArrayList(synonyms);
            sortedTerms.sort(String::compareTo);
            this.placeHolder = String.join("|", sortedTerms);
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }

            SynonymGroup that = Reflection.as(o, SynonymGroup.class);
            if (that == null)
            {
                return false;
            }

            return Objects.equals(synonyms, that.synonyms);
        }

        @Override
        public int hashCode()
        {
            return Objects.hashCode(synonyms);
        }
    }

    private class Cache
    {
        private final Map<String, String> m_processedAbbreviations = Maps.newHashMap();

        private final Map<String, SynonymGroup> m_lookupSynonyms = Maps.newHashMap();

        private final Multimap<SynonymGroup, NormalizationTerm> m_processedTerms = HashMultimap.create();

        private final Map<String, NormalizationTerm> m_processedAcronyms = Maps.newHashMap();

        //--//

        private final Map<String, NormalizationMatch>     m_lookupNormalizationSteps = Maps.newHashMap();
        private final Map<String, String>                 m_lookupNormalization      = Maps.newHashMap();
        private final Map<String, String>                 m_lookupSeparatorRemoval   = Maps.newHashMap();
        private final Map<String, String>                 m_lookupAcronymCompression = Maps.newHashMap();
        private final Map<String, Set<NormalizationTerm>> m_lookupDimensions         = Maps.newHashMap();

        private final Set<String> m_allKnownTermsLowerCase = Sets.newHashSet();

        //--//

        Cache()
        {
            //
            // Build lookup map for abbreviations.
            //
            for (String term : m_abbreviations.keySet())
            {
                for (String abbr : m_abbreviations.get(term))
                {
                    m_processedAbbreviations.put(abbr.toLowerCase(), term);
                }
            }

            //
            // Also create Synonym Groups for known terms, using just one word per group.
            //
            for (String name : m_knownTerms.keySet())
            {
                NormalizationRules.KnownTerm knownTerm = m_knownTerms.get(name);
                processKnownTerm(name, knownTerm);
            }
        }

        private SynonymGroup processKnownTerm(String name,
                                              NormalizationRules.KnownTerm knownTerm)
        {
            List<String> lst = NormalizationEngine.splitAndLowercase(name);

            //
            // Finally, create a processed term for each known term, indexing by SynonymGroup.
            //
            NormalizationTerm pt = new NormalizationTerm();
            pt.name           = name;
            pt.acronym        = StringUtils.lowerCase(knownTerm.acronym);
            pt.positiveWeight = normalizedWeight(knownTerm.positiveWeight);
            pt.negativeWeight = normalizedWeight(knownTerm.negativeWeight);
            pt.nameWords      = lst;

            String firstPart = lst.get(0);

            if (lst.size() == 1)
            {
                for (String synonym : knownTerm.synonyms)
                {
                    makeSynonyms(firstPart, synonym);
                }
            }

            SynonymGroup group = findSynonymGroup(firstPart, true);
            m_processedTerms.put(group, pt);

            for (String nameWord : pt.nameWords)
            {
                m_allKnownTermsLowerCase.add(nameWord.toLowerCase());

                if (pt.acronym != null)
                {
                    m_allKnownTermsLowerCase.add(pt.acronym.toLowerCase());
                }
            }

            if (lst.size() > 1)
            {
                pt.dimensionId = String.join(" ", lst);
            }
            else
            {
                pt.dimensionId = group.placeHolder;
            }

            if (!StringUtils.isBlank(pt.acronym))
            {
                m_processedAcronyms.put(pt.acronym, pt);
            }

            return group;
        }

        private void makeSynonyms(String name1,
                                  String name2)
        {
            SynonymGroup group1 = findSynonymGroup(name1, true);
            SynonymGroup group2 = findSynonymGroup(name2, true);

            if (group1 != group2)
            {
                //
                // Create a new merged group for all the synonyms.
                //
                Set<String> synonyms = Sets.newHashSet();

                synonyms.addAll(group1.synonyms);
                synonyms.addAll(group2.synonyms);

                SynonymGroup mergedGroup = new SynonymGroup(synonyms);

                //
                // Redirect all the synonyms to the new group.
                //
                for (String synonym : synonyms)
                {
                    m_lookupSynonyms.put(synonym, mergedGroup);
                }

                //
                // Point from the new group to all the processed terms from the previous separate groups.
                //
                m_processedTerms.putAll(mergedGroup, m_processedTerms.get(group1));
                m_processedTerms.putAll(mergedGroup, m_processedTerms.get(group2));
            }
        }

        private SynonymGroup findSynonymGroup(String name,
                                              boolean createIfMissing)
        {
            SynonymGroup group = m_lookupSynonyms.get(name);
            if (group == null)
            {
                if (!createIfMissing)
                {
                    return null;
                }

                group = new SynonymGroup(Sets.newHashSet(name));
                m_lookupSynonyms.put(name, group);
            }

            return group;
        }

        private double normalizedWeight(Double weight)
        {
            if (weight == null)
            {
                return 1.0;
            }

            return weight;
        }

        //--//

        boolean isKnownTerm(String word)
        {
            if (StringUtils.isEmpty(word))
            {
                return false;
            }

            String wordLowerCase = word.toLowerCase();
            return m_allKnownTermsLowerCase.contains(wordLowerCase);
        }

        String compressAcronym(String input)
        {
            if (input == null)
            {
                return "";
            }

            String output = m_lookupAcronymCompression.get(input);
            if (output == null)
            {
                final MultiCharacterSplit               mcs            = new MultiCharacterSplit(input, s_separators[s_separators.length - 1], true);
                final List<MultiCharacterSplit.Segment> parts          = mcs.getParts();
                final List<MultiCharacterSplit.Segment> partsSep       = mcs.getSeparators();
                final List<String>                      partsLowerCase = Lists.newArrayList();
                final StringBuilder                     sb             = new StringBuilder();
                final int                               numParts       = parts.size();
                final int                               numSep         = partsSep.size();

                for (MultiCharacterSplit.Segment part : parts)
                {
                    partsLowerCase.add(part.value.toLowerCase());
                }

                for (int i = 0; i < numParts; i++)
                {
                    NormalizationTerm bestAcronymMatch = null;

                    for (NormalizationTerm acronymTerm : m_processedAcronyms.values())
                    {
                        if (matchTerm(acronymTerm, partsLowerCase, i))
                        {
                            if (bestAcronymMatch == null || bestAcronymMatch.nameWords.size() < acronymTerm.nameWords.size())
                            {
                                bestAcronymMatch = acronymTerm;
                            }
                        }
                    }

                    if (bestAcronymMatch != null)
                    {
                        sb.append(bestAcronymMatch.acronym.toUpperCase());
                        i += bestAcronymMatch.nameWords.size() - 1;
                    }
                    else
                    {
                        sb.append(parts.get(i).value);
                    }

                    if (i < numSep)
                    {
                        sb.append(partsSep.get(i).value);
                    }
                }

                output = sb.toString();
                m_lookupAcronymCompression.put(input, output);
            }

            return output;
        }

        String normalizeSimple(List<NormalizationMatchHistory> history,
                               String value)
        {
            if (value == null)
            {
                return "";
            }

            String result = m_lookupNormalization.get(value);
            if (result == null)
            {
                String text          = value;
                int    initialLength = text.length();

                while (true)
                {
                    String textAfter = text;

                    int length = text.length();
                    if (length > 1000 && length > 10 * initialLength)
                    {
                        // Safety valve to protect against runaway situations...
                        break;
                    }

                    for (String separator : s_separators)
                    {
                        textAfter = normalize(history, textAfter, separator);
                    }

                    if (textAfter.equals(text))
                    {
                        break;
                    }

                    text = textAfter;
                }

                m_lookupNormalization.put(value, text);

                result = text;
            }

            return result;
        }

        String removeExtraSeparators(List<NormalizationMatchHistory> history,
                                     String value)
        {
            if (value == null)
            {
                return "";
            }

            String result = m_lookupSeparatorRemoval.get(value);
            if (result == null)
            {
                MultiCharacterSplit mcs = new MultiCharacterSplit(value, " .", false);
                result = mcs.join(" ");

                m_lookupSeparatorRemoval.put(value, result);
            }

            if (history != null && !StringUtils.equals(value, result))
            {
                history.add(new NormalizationMatchHistory(null, null, result));
            }

            return result;
        }

        NormalizationState normalize(NormalizationState stateIn,
                                     boolean runScripts)
        {
            NormalizationState state = stateIn.copy();

            if (runScripts && hasAnyLogic())
            {
                executeScript(state, m_logic, traceExecution, maxSteps);
            }
            else
            {
                state.controlPointName = state.extractInputThroughLegacyHeuristics();
                state.controlPointName = normalizeSimple(state.history, state.controlPointName);
                state.controlPointName = removeExtraSeparators(state.history, state.controlPointName);
            }

            return state;
        }

        private void executeScript(NormalizationState state,
                                   NormalizationEngineExecutionContext script,
                                   boolean traceExecution,
                                   int maxSteps)
        {
            try
            {
                if (state.controlPointName == null)
                {
                    state.controlPointName = "";
                }

                if (state.controlPointNameRaw == null)
                {
                    state.controlPointNameRaw = state.controlPointName;
                }

                String textBefore = state.controlPointName;

                script.state            = state;
                script.pointClasses     = pointClasses;
                script.equipmentClasses = equipmentClasses;
                script.traceExecution   = traceExecution;
                script.reset(null);
                ZonedDateTime now = TimeUtils.now();
                script.evaluate(maxSteps, logScripts ? (stack, line) ->
                {
                    LogLine ll = new LogLine();
                    ll.lineNumber = script.logEntries.size();
                    ll.timestamp  = now;
                    ll.line       = line;
                    script.logEntries.add(ll);
                } : null);

                String textAfter = state.controlPointName;

                if (!StringUtils.equals(textBefore, textAfter))
                {
                    state.history.add(new NormalizationMatchHistory(null, textBefore, textAfter));
                }
            }
            catch (Exception e)
            {
                // Ignore processing exceptions.
            }
        }

        private String normalize(List<NormalizationMatchHistory> history,
                                 String text,
                                 String separators)
        {
            MultiCharacterSplit mcs = new MultiCharacterSplit(text, separators, false);

            for (MultiCharacterSplit.Segment segment : mcs.getParts())
            {
                NormalizationMatch match = normalizeSingle(segment.value);
                if (match != null)
                {
                    segment.value = match.output;
                    String textAfter = mcs.join();

                    if (history != null)
                    {
                        history.add(new NormalizationMatchHistory(match, text, textAfter));
                    }

                    text = textAfter;
                }

                segment.value = StringUtils.capitalize(segment.value);
            }

            return mcs.join();
        }

        private NormalizationMatch normalizeSingle(String part)
        {
            String             partLowercase = part.toLowerCase();
            NormalizationMatch match         = m_lookupNormalizationSteps.get(partLowercase);
            if (match == null)
            {
                match = normalizeSingleInner(part, partLowercase);
                m_lookupNormalizationSteps.put(partLowercase, match);
            }

            return match == NormalizationMatch.NoMatchSentinel ? null : match;
        }

        private NormalizationMatch normalizeSingleInner(String part,
                                                        String partLowercase)
        {
            String disambiguation = m_disambiguations.get(partLowercase);
            if (disambiguation != null)
            {
                return new NormalizationMatch(NormalizationMatchKind.Disambiguation, null, partLowercase, disambiguation);
            }

            NormalizationTerm acronym = m_processedAcronyms.get(partLowercase);
            if (acronym != null)
            {
                return new NormalizationMatch(NormalizationMatchKind.Acronym, null, partLowercase, acronym.name);
            }

            String term = m_processedAbbreviations.get(partLowercase);
            if (term != null)
            {
                return new NormalizationMatch(NormalizationMatchKind.Abbreviation, null, partLowercase, term);
            }

            SynonymGroup group = findSynonymGroup(partLowercase, false);
            if (group != null)
            {
                // If it's a synonym of a known term, stop processing.
                return NormalizationMatch.NoMatchSentinel;
            }

            String startWith = findLongest(m_startsWith, partLowercase::startsWith);
            if (startWith != null)
            {
                String result = m_startsWith.get(startWith);
                if (!result.equals(part) && !result.equals(partLowercase)) // Avoid recursing...
                {
                    return new NormalizationMatch(NormalizationMatchKind.StartsWith, startWith, partLowercase, result + " " + partLowercase.substring(startWith.length()));
                }
            }

            String endWith = findLongest(m_endsWith, partLowercase::endsWith);
            if (endWith != null)
            {
                String result = m_endsWith.get(endWith);
                if (!result.equals(part) && !result.equals(partLowercase)) // Avoid recursing...
                {
                    return new NormalizationMatch(NormalizationMatchKind.EndsWith, endWith, partLowercase, partLowercase.substring(0, partLowercase.length() - endWith.length()) + " " + result);
                }
            }

            String contain = findLongest(m_contains, partLowercase::contains);
            if (contain != null)
            {
                String result = m_contains.get(contain);
                if (!result.equals(part) && !result.equals(partLowercase)) // Avoid recursing...
                {
                    int pos = partLowercase.indexOf(contain);

                    return new NormalizationMatch(NormalizationMatchKind.Contains,
                                                  contain,
                                                  partLowercase,
                                                  partLowercase.substring(0, pos) + " " + result + " " + partLowercase.substring(pos + contain.length()));
                }
            }

            return NormalizationMatch.NoMatchSentinel;
        }

        private String findLongest(Map<String, String> map,
                                   Function<String, Boolean> filter)
        {
            String longestMatch = null;

            for (String key : map.keySet())
            {
                if (longestMatch != null && key.length() < longestMatch.length())
                {
                    continue;
                }

                if (filter.apply(key))
                {
                    longestMatch = key;
                }
            }

            return longestMatch;
        }

        //--//

        boolean isPartOfDimension(String wordLowercase)
        {
            return m_lookupSynonyms.containsKey(wordLowercase);
        }

        Set<NormalizationTerm> extractDimensions(String text)
        {
            if (StringUtils.isEmpty(text))
            {
                return Collections.emptySet();
            }

            Set<NormalizationTerm> res = m_lookupDimensions.get(text);
            if (res == null)
            {
                Map<String, NormalizationTerm> dimensions = Maps.newHashMap();

                List<String> parts = NormalizationEngine.splitAndLowercase(text);
                for (int i = 0; i < parts.size(); i++)
                {
                    String       part  = parts.get(i);
                    SynonymGroup group = findSynonymGroup(part, false);

                    if (group == null && m_callback != null)
                    {
                        NormalizationRules.KnownTerm term = m_callback.apply(part);
                        if (term != null)
                        {
                            m_knownTerms.put(part, term);
                            group = processKnownTerm(part, term);
                        }
                    }

                    if (group != null)
                    {
                        for (NormalizationTerm term : m_processedTerms.get(group))
                        {
                            if (matchTerm(term, parts, i))
                            {
                                NormalizationTerm oldTerm = dimensions.get(term.dimensionId);
                                if (oldTerm == null || oldTerm.positiveWeight < term.positiveWeight)
                                {
                                    dimensions.put(term.dimensionId, term);
                                }
                            }
                        }
                    }
                }

                res = Collections.unmodifiableSet(Sets.newHashSet(dimensions.values()));
                m_lookupDimensions.put(text, res);
            }

            return res;
        }

        private boolean matchTerm(NormalizationTerm term,
                                  List<String> parts,
                                  int offset)
        {
            for (String word1 : term.nameWords)
            {
                if (offset >= parts.size())
                {
                    return false;
                }

                String word2 = parts.get(offset++);

                SynonymGroup group1 = findSynonymGroup(word1, false);
                SynonymGroup group2 = findSynonymGroup(word2, false);

                if (group1 == group2 && group1 != null)
                {
                    continue;
                }

                if (group1 != null || group2 != null)
                {
                    return false;
                }

                if (!StringUtils.equalsIgnoreCase(word1, word2))
                {
                    return false;
                }
            }

            return true;
        }
    }

    //--//

    public boolean logScripts;
    public boolean traceExecution;
    public int     maxSteps = 5000000;

    private Function<String, NormalizationRules.KnownTerm> m_callback;

    private final NormalizationEngineExecutionContext m_logic;

    public final List<PointClass>     pointClasses;
    public final List<EquipmentClass> equipmentClasses;

    public final Map<String, PointClass> lookupPointClass = Maps.newHashMap();

    private final PreprocessedTargets<PointClass>     m_pointClassesPreprocessed;
    private final PreprocessedTargets<EquipmentClass> m_equipmentClassesPreprocessed;

    private final Map<String, NormalizationRules.KnownTerm> m_knownTerms      = Maps.newHashMap();
    private final Map<String, List<String>>                 m_abbreviations   = Maps.newHashMap();
    private final Map<String, String>                       m_startsWith      = Maps.newHashMap();
    private final Map<String, String>                       m_endsWith        = Maps.newHashMap();
    private final Map<String, String>                       m_contains        = Maps.newHashMap();
    private final Map<String, String>                       m_disambiguations = Maps.newHashMap();

    private Cache m_cache;

    //--//

    public NormalizationEngine(SessionProvider sessionProvider,
                               NormalizationRules input,
                               boolean enableProcessingLogic)
    {
        if (enableProcessingLogic && input.getLogic() != null)
        {
            m_logic = parse(sessionProvider, this, input.getLogic());
        }
        else
        {
            m_logic = null;
        }

        m_knownTerms.putAll(input.knownTerms);
        m_abbreviations.putAll(input.abbreviations);
        m_startsWith.putAll(input.startsWith);
        m_endsWith.putAll(input.endsWith);
        m_contains.putAll(input.contains);
        m_disambiguations.putAll(input.disambiguations);

        //--//

        pointClasses     = input.pointClasses;
        equipmentClasses = input.equipmentClasses;

        for (PointClass pc : pointClasses)
        {
            lookupPointClass.put(pc.idAsString(), pc);
        }

        m_pointClassesPreprocessed     = preprocessTargets(pointClasses, (pointClass) -> pointClass.pointClassDescription);
        m_equipmentClassesPreprocessed = preprocessTargets(equipmentClasses, (equipmentClass) -> equipmentClass.description);
    }

    //--//

    public boolean hasAnyLogic()
    {
        return m_logic != null;
    }

    public NormalizationEngineExecutionContext getLogic()
    {
        return m_logic;
    }

    @SuppressWarnings("unchecked")
    public void configure(Settings setting,
                          Object value)
    {
        switch (setting)
        {
            case AddUnknownTerms:
                m_callback = (Function<String, NormalizationRules.KnownTerm>) value;
                break;
        }

        m_cache = null;
    }

    //--//

    public void populateTagsSummary(TagsEngine.Snapshot tagsSnapshot,
                                    TagsSummary summary)
    {
        for (PointClass pc : m_pointClassesPreprocessed.m_dimensions.keySet())
        {
            extractTags(AssetRecord.WellKnownTags.pointClassId, pc.idAsString(), tagsSnapshot, summary.pointClassesFrequency);
        }

        for (EquipmentClass ec : m_equipmentClassesPreprocessed.m_dimensions.keySet())
        {
            extractTags(AssetRecord.WellKnownTags.equipmentClassId, ec.idAsString(), tagsSnapshot, summary.equipmentClassesFrequency);
        }
    }

    private static void extractTags(String tag,
                                    String value,
                                    TagsEngine.Snapshot tagsSnapshot,
                                    Map<String, Integer> frequencies)
    {
        int freq = tagsSnapshot.getFrequencyOfKeyValuePairs(tag, value);
        if (freq > 0)
        {
            frequencies.put(value, freq);
        }
    }

    //--//

    public boolean isPartOfDimension(String wordLowercase)
    {
        return ensureCache().isPartOfDimension(wordLowercase);
    }

    public Set<NormalizationTerm> extractDimensions(String text)
    {
        String textNormalized = normalizeSimple(text);

        return ensureCache().extractDimensions(textNormalized);
    }

    //--//

    public static NormalizationScore computeScore(Set<NormalizationTerm> target,
                                                  Set<NormalizationTerm> reference)
    {
        NormalizationScore score = new NormalizationScore();

        for (NormalizationTerm term : reference)
        {
            int words = term.nameWords.size();

            if (target.contains(term))
            {
                score.positiveScore += term.positiveWeight * words * words;
                score.matchingDimensions.add(term);
            }
            else
            {
                score.negativeScore -= term.negativeWeight * words * words;
            }
        }

        final int numberOfMatches = score.matchingDimensions.size();
        if (numberOfMatches > 0)
        {
            score.positiveScore *= numberOfMatches;
            score.negativeScore *= numberOfMatches;

            score.positiveScore /= target.size();
            score.negativeScore /= target.size();
        }

        return score;
    }

    public <K> PreprocessedTargets<K> preprocessTargets(Collection<K> targets,
                                                        Function<K, String> mappingCallback)
    {
        PreprocessedTargets<K> res = new PreprocessedTargets<>();
        res.initialize(this, targets, mappingCallback);
        return res;
    }

    public <K> List<NormalizationScore.Context<K>> score(String text,
                                                         PreprocessedTargets<K> targets,
                                                         Function<K, Boolean> filterCallback,
                                                         BiConsumer<K, NormalizationScore> adjustScoreCallback)
    {
        List<NormalizationScore.Context<K>> results = Lists.newArrayList();

        Set<NormalizationTerm> inputDimensions = extractDimensions(text);
        if (inputDimensions.size() > 0)
        {
            for (K target : targets.m_keys)
            {
                if (filterCallback != null && !filterCallback.apply(target))
                {
                    continue;
                }

                Set<NormalizationTerm> targetDimensions = targets.m_dimensions.get(target);
                if (!targetDimensions.isEmpty())
                {
                    NormalizationScore score = computeScore(inputDimensions, targetDimensions);

                    if (adjustScoreCallback != null)
                    {
                        adjustScoreCallback.accept(target, score);
                    }

                    if (score.matchingDimensions.size() > 0)
                    {
                        results.add(score.getContext(target));
                    }
                }
            }

            results.sort((a, b) ->
                         {
                             // Sort in descending order of score.
                             int diff = Double.compare(b.score.getTotalScore(), a.score.getTotalScore());
                             if (diff == 0)
                             {
                                 // If same score, sort in ascending order of dimensions
                                 diff = Integer.compare(a.score.matchingDimensions.size(), b.score.matchingDimensions.size());
                             }
                             return diff;
                         });
        }

        return results;
    }

    //--//

    public List<NormalizationScore.Context<PointClass>> scorePointClass(String text,
                                                                        Function<PointClass, Boolean> filter,
                                                                        BiConsumer<PointClass, NormalizationScore> adjustScoreCallback)
    {
        return score(text, m_pointClassesPreprocessed, filter, adjustScoreCallback);
    }

    public NormalizationScore.Context<PointClass> scoreTopPointClass(String text,
                                                                     Function<PointClass, Boolean> filter,
                                                                     BiConsumer<PointClass, NormalizationScore> adjustScoreCallback)
    {
        return CollectionUtils.firstElement(scorePointClass(text, filter, adjustScoreCallback));
    }

    //--//

    public List<NormalizationScore.Context<EquipmentClass>> scoreEquipmentClass(String text,
                                                                                Function<EquipmentClass, Boolean> filter,
                                                                                BiConsumer<EquipmentClass, NormalizationScore> adjustScoreCallback)
    {
        return score(text, m_equipmentClassesPreprocessed, filter, adjustScoreCallback);
    }

    public NormalizationScore.Context<EquipmentClass> scoreTopEquipmentClass(String text,
                                                                             Function<EquipmentClass, Boolean> filter,
                                                                             BiConsumer<EquipmentClass, NormalizationScore> adjustScoreCallback)
    {
        return CollectionUtils.firstElement(scoreEquipmentClass(text, filter, adjustScoreCallback));
    }

    //--//

    public static List<String> splitAndLowercase(String text)
    {
        return splitAndLowercase(text, s_separators[s_separators.length - 1]);
    }

    public static List<String> splitAndLowercase(String text,
                                                 String separator)
    {
        MultiCharacterSplit mcs = new MultiCharacterSplit(text, separator, false);
        return CollectionUtils.transformToList(mcs.getParts(), s -> s.value.toLowerCase());
    }

    public static List<String> split(String text)
    {
        MultiCharacterSplit mcs = new MultiCharacterSplit(text, s_separators[s_separators.length - 1], false);
        return CollectionUtils.transformToList(mcs.getParts(), s -> s.value);
    }

    //--//

    public String normalizeSimple(String value)
    {
        Cache cache = ensureCache();

        String output1 = cache.normalizeSimple(null, value);
        String output2 = cache.removeExtraSeparators(null, output1);
        return output2;
    }

    public NormalizationState normalizeWithHistory(NormalizationState state,
                                                   boolean runScripts)
    {
        return ensureCache().normalize(state, runScripts);
    }

    public String compressAcronym(String text)
    {
        return ensureCache().compressAcronym(text);
    }

    public boolean isKnownTerm(String word)
    {
        return ensureCache().isKnownTerm(word);
    }

    //--//

    public static NormalizationEngineExecutionContext parse(SessionProvider sessionProvider,
                                                            NormalizationEngine engine,
                                                            NormalizationDefinitionDetails details)
    {
        EngineExecutionProgram<NormalizationDefinitionDetails> program = new EngineExecutionProgram<>(null, details);

        NormalizationEngineExecutionContext ctx = new NormalizationEngineExecutionContext(sessionProvider, engine, program);
        return ctx.hasThread() ? ctx : null;
    }

    private Cache ensureCache()
    {
        if (m_cache == null)
        {
            m_cache = new Cache();
        }

        return m_cache;
    }
}
