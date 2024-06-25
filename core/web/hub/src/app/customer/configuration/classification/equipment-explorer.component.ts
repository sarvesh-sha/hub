import {ChangeDetectionStrategy, Component, Input} from "@angular/core";

import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {IProviderForMapHost} from "app/shared/tables/provider-for-map";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {FilterableTreeComponent, ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";

@Component({
               selector       : "o3-equipment-explorer",
               styles         : [".stale { color: #ffd740; }"],
               templateUrl    : "./equipment-explorer.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class EquipmentExplorerComponent extends BaseApplicationComponent
{
    private m_relationships: Lookup<string[]>;
    private m_equipments: Lookup<Models.NormalizationEquipment>;
    private m_nodes: EquipmentNode[];
    private m_allNodes: EquipmentNode[];

    static nodeSortFn(a: EquipmentNode,
                      b: EquipmentNode): number
    {
        if (a.isStale && !b.isStale)
        {
            return -1;
        }
        else if (!a.isStale && b.isStale)
        {
            return 1;
        }
        else
        {
            return UtilsService.compareStrings(a.id, b.id, true);
        }
    }

    staleRelationships: [string, string][] = [];
    selectedReplacementId: string;
    toReplaceId: string;

    selections: string[] = [];
    selectedParentId: string;

    totalCount: number;

    @Input()
    public host: IProviderForMapHost;

    @Input()
    public set equipmentRelationships(relationships: Lookup<string[]>)
    {
        this.m_relationships = relationships;
        this.m_nodes         = null;
    }

    @Input()
    public set equipments(equipments: Lookup<Models.NormalizationEquipment>)
    {
        this.m_equipments = equipments;
        this.m_nodes      = null;
        this.totalCount   = Object.keys(equipments).length;
    }

    @Input() public rules: Models.NormalizationRules;

    public get nodes(): EquipmentNode[]
    {
        if (!this.m_nodes)
        {
            let allNodes: EquipmentNode[] = [];

            this.m_nodes    = this.computeTree(allNodes);
            this.m_allNodes = allNodes;
        }

        return this.m_nodes;
    }

    private computeTree(allNodes: EquipmentNode[]): EquipmentNode[]
    {
        let nodes: EquipmentNode[]       = [];
        let cache: Lookup<EquipmentNode> = {};

        if (!(this.m_equipments && this.m_relationships))
        {
            return [];
        }

        for (let equipKey in this.m_equipments)
        {
            let node = this.getOrCreateNode(cache, equipKey);
            for (let subEquipKey of this.m_relationships[equipKey])
            {
                let subNode = this.getOrCreateNode(cache, subEquipKey);
                node.addChild(subNode);

                // Remove from root nodes if we already added it
                let index = nodes.indexOf(subNode);
                if (index >= 0)
                {
                    nodes.splice(index, 1);
                }
            }

            // Only add if it looks like a root node
            if (!node.parent)
            {
                nodes.push(node);
            }
        }

        this.staleRelationships = [];
        this.toReplaceId        = null;

        for (let equipKey in this.rules.equipmentRelationships)
        {
            let parentStale = this.checkStale(equipKey);
            let parentNode  = this.getOrCreateNode(cache, equipKey);
            if (parentStale)
            {
                parentNode.label = equipKey;
            }

            for (let subEquipKey of this.rules.equipmentRelationships[equipKey])
            {
                let childStale = this.checkStale(subEquipKey);
                let childNode  = this.getOrCreateNode(cache, subEquipKey);
                parentNode.addChild(childNode);
                if (childStale)
                {
                    childNode.label = subEquipKey;
                }

                // Remove from root nodes if we already added it
                let index = nodes.indexOf(childNode);
                if (index >= 0)
                {
                    nodes.splice(index, 1);
                }

                if (parentStale || childStale)
                {
                    this.staleRelationships.push([
                                                     equipKey,
                                                     subEquipKey
                                                 ]);
                }
            }

            if (!parentNode.parent && parentStale)
            {
                nodes.push(parentNode);
            }
        }

        allNodes.push(...Object.keys(cache)
                               .map((k) => cache[k]));

        nodes.sort(EquipmentExplorerComponent.nodeSortFn);

        return nodes;
    }

    setParent(tree: FilterableTreeComponent<string>)
    {
        if (!this.isValidParentSelected())
        {
            return;
        }

        let nodes: EquipmentNode[] = this.selections.map((s) => this.m_allNodes.find((n) => n.id === s));
        let parent                 = this.m_allNodes.find((n) => n.id === this.selectedParentId);

        for (let node of nodes)
        {
            if (node.parent)
            {
                this.removeRelationship(node.parent.id, node.id);
            }
            else
            {
                this.m_nodes.splice(this.m_nodes.indexOf(node), 1);
            }

            this.addRelationship(parent.id, node.id);
            parent.addChild(node);
        }

        this.m_nodes = [...this.m_nodes];
        this.clearSelection();
        tree.scrollIntoView(parent.id);
        tree.expandNode(parent.id);
        this.host.setDirty();
    }

    clearParent(parentDialog: StandardFormOverlayComponent)
    {
        let nodes: EquipmentNode[] = this.selections.map((s) => this.m_allNodes.find((n) => n.id === s));

        for (let node of nodes)
        {
            if (node.parent)
            {
                this.removeRelationship(node.parent.id, node.id);
                node.parent.removeChild(node);
                this.m_nodes.push(node);
            }
        }

        this.m_nodes = [...this.m_nodes].sort(EquipmentExplorerComponent.nodeSortFn);
        this.clearSelection();
        parentDialog.closeOverlay();
    }

    setReplacement()
    {
        let replacement = this.selectedReplacementId;
        if (replacement)
        {
            this.processReplacement(this.toReplaceId, replacement);
        }

        this.m_nodes = null;
        this.clearSelection();
    }

    get selectedNodes(): EquipmentNode[]
    {
        return this.selections
                   .map((s) => this.m_allNodes.find((n) => n.id === s));
    }


    get staleEquipmentSelected(): boolean
    {
        return this.selectedNodes.some((n) => n && n.isStale);
    }

    openResolveDialog(dialog: StandardFormOverlayComponent)
    {
        let [node]       = this.selectedNodes;
        this.toReplaceId = node.id;
        dialog.toggleOverlay();
    }

    private processReplacement(from: string,
                               to: string)
    {
        let staleParentRelations = this.staleRelationships.filter(([parent]) => parent === from);
        let staleChildRelations  = this.staleRelationships.filter(([, child]) => child === from);

        for (let relation of staleParentRelations)
        {
            let [parent, child] = relation;
            this.removeRelationship(parent, child);
            this.addRelationship(to, child);
            relation[0] = to;
            this.addToTreeIfValid(to, child);
        }

        for (let relation of staleChildRelations)
        {
            let [parent, child] = relation;
            this.removeRelationship(parent, child);
            this.addRelationship(parent, to);
            relation[1] = to;
            this.addToTreeIfValid(parent, to);
        }
    }

    private addToTreeIfValid(parent: string,
                             child: string)
    {
        if (!this.checkStale(parent) && !this.checkStale(child))
        {
            let relations = this.m_relationships[parent];
            if (relations.indexOf(child) === -1)
            {
                relations.push(child);
            }
        }
    }

    isValidParentSelected(): boolean
    {
        let selectedNodes = this.selectedNodes;
        let parentNode    = this.selectedParentId ? this.m_allNodes.find((n) => n.id === this.selectedParentId) : null;
        if (selectedNodes.length === 0 || !parentNode || selectedNodes.indexOf(parentNode) >= 0)
        {
            return false;
        }

        let parentPath: EquipmentNode[] = [];
        while (parentNode.parent)
        {
            parentPath.push(parentNode.parent);
            parentNode = parentNode.parent;
        }

        return !parentPath.some((n) => selectedNodes.indexOf(n) >= 0);
    }

    clearSelection()
    {
        this.selections = [];
    }

    addRelationship(parent: string,
                    child: string): boolean
    {
        let relationships = this.ensureRelationships(parent);
        if (relationships.indexOf(child) === -1)
        {
            relationships.push(child);
            return true;
        }

        return false;
    }

    removeRelationship(parent: string,
                       child: string): boolean
    {
        let relationships = this.ensureRelationships(parent);
        let index         = relationships.findIndex((s) => s === child);
        if (index >= 0)
        {
            relationships.splice(index, 1);

            if (relationships.length === 0)
            {
                delete this.rules.equipmentRelationships[parent];
            }

            return true;
        }

        return false;
    }

    ensureRelationships(id: string): string[]
    {
        if (!this.rules.equipmentRelationships[id])
        {
            this.rules.equipmentRelationships[id] = [];
        }

        return this.rules.equipmentRelationships[id];
    }

    private checkStale(id: string): boolean
    {
        return !this.m_equipments.hasOwnProperty(id);
    }

    private getOrCreateNode(cache: Lookup<EquipmentNode>,
                            key: string): EquipmentNode
    {
        if (cache[key])
        {
            return cache[key];
        }

        let node      = new EquipmentNode();
        node.id       = key;
        node.children = [];
        cache[key]    = node;

        let equip = this.m_equipments[key];
        if (equip)
        {
            node.label = `${equip.name} ${equip.locations.map((l) => l.name)
                                               .join(" - ")}`;
        }
        else
        {
            node.isStale = true;
        }

        return node;
    }
}

class EquipmentNode implements ITreeNode<string>
{
    id: string;
    label: string;
    parent: EquipmentNode;
    children: EquipmentNode[];
    isStale: boolean;

    get hasChildren(): boolean
    {
        return !!this.children?.length;
    }

    addChild(child: EquipmentNode)
    {
        if (child.parent)
        {
            child.parent.removeChild(child);
        }

        child.parent = this;
        this.children.push(child);

        this.children.sort(EquipmentExplorerComponent.nodeSortFn);
    }

    removeChild(child: EquipmentNode)
    {
        let index = this.children.indexOf(child);
        if (index >= 0)
        {
            this.children.splice(index, 1);
        }

        child.parent = null;
    }
}
