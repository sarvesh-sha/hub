import * as Models from "app/services/proxy/model/models";
import {QueryNode} from "app/shared/charting/hierarchical-visualization/hierarchy-result-table";

export class TagsJoinQueryExtended
{
    constructor(public readonly model: Models.TagsJoinQuery)
    {
    }

    public static fromHierarchicalGraph(hierarchicalGraph: Models.AssetGraph,
                                        leafNodeId: string,
                                        retainNodeIds?: boolean): Models.TagsJoinQuery
    {
        if (!hierarchicalGraph || !leafNodeId) return null;

        const getId = retainNodeIds ?
            (node: Models.AssetGraphNode) => node.id :
            (node: Models.AssetGraphNode) => QueryNode.encodeLabel(node.name);

        let transforms = <Models.AssetGraphTransformRelationship[]>hierarchicalGraph.transforms;

        const query = Models.TagsJoinQuery.newInstance({
                                                           terms: [],
                                                           joins: []
                                                       });

        let node = hierarchicalGraph.nodes.find((node) => node.id === leafNodeId);
        while (node)
        {
            let rightTermId = getId(node);
            query.terms.unshift(Models.TagsJoinTerm.newInstance({
                                                                    id        : rightTermId,
                                                                    conditions: node.condition
                                                                }));

            const transform = transforms.find((transform) => transform.outputId === node.id);
            if (!transform) break;
            node = hierarchicalGraph.nodes.find((node) => node.id === transform.inputId);

            query.joins.unshift(Models.TagsJoin.newInstance({
                                                                leftSide : getId(node),
                                                                rightSide: rightTermId,
                                                                relation : transform.relationship
                                                            }));
        }

        return query;
    }
}
