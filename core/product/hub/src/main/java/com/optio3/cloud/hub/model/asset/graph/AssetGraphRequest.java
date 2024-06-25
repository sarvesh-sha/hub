/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.asset.graph;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.tags.TagsCondition;
import com.optio3.cloud.hub.model.tags.TagsConditionBinaryLogic;
import com.optio3.cloud.hub.model.tags.TagsConditionOperator;
import com.optio3.cloud.hub.model.tags.TagsJoin;
import com.optio3.cloud.hub.model.tags.TagsJoinQuery;
import com.optio3.cloud.hub.model.tags.TagsJoinTerm;
import com.optio3.cloud.model.BasePaginatedRequest;
import com.optio3.serialization.Reflection;

public class AssetGraphRequest extends BasePaginatedRequest
{
    public AssetGraph graph;

    public AssetGraphContext context;

    public AssetGraphResponse evaluate(TagsEngine.Snapshot tagsSnapshot)
    {
        AssetGraphResponse response = new AssetGraphResponse();
        response.offset = startOffset;

        // Ask for an extra result, to detect if we are not done.
        int maxResultsPlusOne = this.maxResults > 0 ? this.maxResults + 1 : Integer.MAX_VALUE;

        AssetGraph.Analyzed graphAnalyzed = graph.analyze();

        TagsJoinQuery query = buildQuery(graphAnalyzed, context != null ? context.getRootCondition() : null, startOffset, maxResultsPlusOne);

        List<String[]> tuples = Lists.newArrayList();

        tagsSnapshot.evaluateJoin(query, (tuple) ->
        {
            int count = response.results.size();
            if (count + 1 >= maxResultsPlusOne)
            {
                response.nextOffset = response.offset + count;
            }
            else
            {
                tuples.add(tuple.asSysIds());
            }

            return null;
        });

        response.resolve(graphAnalyzed, tuples);

        response.version = tagsSnapshot.getVersion();

        return response;
    }

    private static TagsJoinQuery buildQuery(AssetGraph.Analyzed graphAnalyzed,
                                            TagsCondition rootCondition,
                                            int offset,
                                            int maxResults)
    {
        TagsJoinQuery query = new TagsJoinQuery();
        query.startOffset = offset;
        query.maxResults  = maxResults;

        Set<String> nonRoots = Sets.newHashSet();

        for (AssetGraphTransform transform : graphAnalyzed.graph.transforms)
        {
            AssetGraphTransformRelationship transformRelationship = Reflection.as(transform, AssetGraphTransformRelationship.class);
            if (transformRelationship != null)
            {
                TagsJoin join = new TagsJoin();
                join.leftSide  = transform.inputId;
                join.rightSide = transform.outputId;
                join.relation  = transformRelationship.relationship;
                query.joins.add(join);
                nonRoots.add(transform.outputId);
            }
        }

        for (AssetGraphNode node : graphAnalyzed.graph.nodes)
        {
            AssetGraphNode.Analyzed nodeAnalyzed = graphAnalyzed.lookupNode(node.id);

            TagsJoinTerm term = new TagsJoinTerm();
            term.id         = node.id;
            term.conditions = node.condition;
            term.optional   = nodeAnalyzed.isOptional();

            if (rootCondition != null && !nonRoots.contains(node.id))
            {
                if (term.conditions == null)
                {
                    term.conditions = rootCondition;
                }
                else
                {
                    TagsConditionBinaryLogic condition = new TagsConditionBinaryLogic();
                    condition.a  = rootCondition;
                    condition.b  = term.conditions;
                    condition.op = TagsConditionOperator.And;

                    term.conditions = condition;
                }
            }

            query.terms.add(term);
        }

        return query;
    }
}
