import {Component, EventEmitter, Input, Output, SimpleChanges} from "@angular/core";

import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {VirtualAssetGraphNodeExtended} from "app/shared/charting/interactive-tree/interactive-tree.component";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-query-column-picker",
               templateUrl: "./query-column-picker.component.html",
               styleUrls  : ["./query-column-picker.component.scss"]
           })
export class QueryColumnPickerComponent extends SharedSvc.BaseApplicationComponent
{
    @Input() graph: Models.AssetGraph;
    @Input() bindings: Models.HierarchicalVisualizationBinding[];
    @Input() virtualNodes: Models.VirtualAssetGraphNode[] = [];

    @Output() virtualNodesChanged = new EventEmitter<void>();

    optionTypes: ControlOption<Models.VirtualAssetGraphNodeType>[] = UtilsService.getEnumValues<Models.VirtualAssetGraphNodeType>(Models.VirtualAssetGraphNodeType)
                                                                                 .map((type) => new ControlOption(type, VirtualAssetGraphNodeExtended.fromTypeToLabel(type)));

    virtualGrouping: VirtualNodeGrouping;

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (this.graph && this.bindings?.length)
        {
            let leafNodeIds      = this.bindings.map((binding) => binding.leafNodeId);
            this.virtualGrouping = new VirtualNodeGrouping(this.app.domain, this.graph, leafNodeIds, this.virtualNodes);
        }
    }

    toggleVirtualNode(group: VirtualNodeGroup,
                      type: Models.VirtualAssetGraphNodeType)
    {
        if (group.currTypes.has(type))
        {
            let idx = this.virtualNodes.findIndex((virtualNode) => virtualNode.nodeId === group.nodeId && virtualNode.type === type);
            if (idx < 0) throw Error("Virtual asset graph node not found");

            this.virtualNodes.splice(idx, 1);
            group.currTypes.delete(type);
        }
        else
        {
            this.virtualNodes.push(VirtualAssetGraphNodeExtended.newModel(group.nodeId, type));
            group.currTypes.add(type);
        }

        this.virtualNodesChanged.emit();
    }
}

class VirtualNodeGrouping
{
    public groups: VirtualNodeGroup[] = [];

    constructor(domain: AppDomainContext,
                assetGraph: Models.AssetGraph,
                leafNodeIds: string[],
                virtualNodes: Models.VirtualAssetGraphNode[])
    {
        const assetGraphExt = new AssetGraphExtended(domain, assetGraph);
        let nodeId          = leafNodeIds[0];
        while (nodeId)
        {
            let overrideLabel: string;
            let nodes: Models.AssetGraphNode[] = [];
            if (this.groups.length === 0)
            {
                nodes = leafNodeIds.map((leafNodeId) => assetGraphExt.getNodeById(leafNodeId));
                if (nodes.length > 1) overrideLabel = "Leaf Node";
            }
            else
            {
                nodes = [assetGraphExt.getNodeById(nodeId)];
            }

            let nodeGroup = new VirtualNodeGroup(nodes, virtualNodes, overrideLabel);
            this.groups.unshift(nodeGroup);

            nodeId = assetGraphExt.getNodeParentId(nodeId);
        }
    }
}

class VirtualNodeGroup
{
    public readonly nodeId: string;
    public readonly nodeLabel: string;

    public readonly currTypes = new Set<Models.VirtualAssetGraphNodeType>();

    constructor(nodes: Models.AssetGraphNode[],
                virtualNodes: Models.VirtualAssetGraphNode[],
                overrideLabel?: string)
    {
        if (nodes.length === 1)
        {
            let node       = nodes[0];
            this.nodeId    = node.id;
            this.nodeLabel = node.name;
        }
        else
        {
            this.nodeId    = null;
            this.nodeLabel = overrideLabel || nodes[0].name;
        }

        for (let i = 0; i < virtualNodes.length; i++)
        {
            let virtualNode = virtualNodes[i];
            if (virtualNode.nodeId === this.nodeId)
            {
                this.currTypes.add(virtualNode.type);
            }
        }
    }

    typeSelected(type: Models.VirtualAssetGraphNodeType): boolean
    {
        return this.currTypes.has(type);
    }
}
