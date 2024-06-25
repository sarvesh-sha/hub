import {Directive, Input} from "@angular/core";

import {AssetGraphExtended, SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";

import {Lookup} from "framework/services/utils.service";
import {ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";
import {SelectComponent} from "framework/ui/forms/select.component";

@Directive({
               selector: "[o3AssetGraphNodeSelector],[o3AssetGraphNodeSelectorMulti],[o3AssetGraphNodeSelectorMultiGroups]"
           })
export class AssetGraphNodeSelectorDirective
{
    private m_graph: AssetGraphExtended;
    private m_graphs: Map<string, SharedAssetGraphExtended>;
    private m_graphHosts: GraphConfigurationHost[];

    private m_onlyLeaves: boolean   = false;
    private m_onlySiblings: boolean = false;

    constructor(private selectComponent: SelectComponent<string>)
    {
    }

    @Input("o3AssetGraphNodeSelector")
    public set graph(graph: AssetGraphExtended)
    {
        this.m_graph = graph;
        this.updateOptions();
    }

    @Input("o3AssetGraphNodeSelectorMulti")
    public set graphs(graphs: Map<string, SharedAssetGraphExtended>)
    {
        this.m_graphs = graphs;
        this.updateOptions();
    }

    @Input("o3AssetGraphNodeSelectorMultiGroups")
    public set graphHosts(groups: GraphConfigurationHost[])
    {
        this.m_graphHosts = groups;
        this.updateOptions();
    }

    @Input("o3AssetGraphNodeSelectorOnlyLeaves")
    public set onlyLeaves(onlyLeaves: boolean)
    {
        if (this.m_onlyLeaves !== onlyLeaves)
        {
            this.m_onlyLeaves = onlyLeaves;
            this.updateOptions();
        }
    }

    @Input("o3AssetGraphNodeSelectorOnlySiblings")
    public set onlySiblings(onlySiblings: boolean)
    {
        if (this.m_onlySiblings !== onlySiblings)
        {
            this.m_onlySiblings = onlySiblings;
            this.updateOptions();
        }
    }

    private async updateOptions()
    {
        if (this.m_graph)
        {
            this.updateOptionsSingle();
        }
        else if (this.m_graphs)
        {
            this.updateOptionsMulti();
        }
        else if (this.m_graphHosts)
        {
            await this.updateOptionsMultiGroup();
        }
    }

    private updateOptionsSingle()
    {
        this.selectComponent.options = this.m_graph?.getTreeNodes(this.m_onlyLeaves) || [];
    }

    private updateOptionsMulti()
    {
        this.selectComponent.options = this.getOptionsMulti();
    }

    private async updateOptionsMultiGroup()
    {
        let topLevelOptions: ITreeNode<string>[] = [];
        for (let graphHost of this.m_graphHosts)
        {
            let graphs = await graphHost.resolveGraphs();
            let label  = graphHost.hostContext + " Bindings";
            topLevelOptions.push({
                                     id              : label,
                                     label           : label,
                                     children        : this.getOptionsMulti(graphs),
                                     hasChildren     : true,
                                     disableSelection: true
                                 });
        }
        this.selectComponent.options = topLevelOptions;
    }

    private getOptionsMulti(graphs?: Map<string, SharedAssetGraphExtended>): ITreeNode<string>[]
    {
        if (!graphs) graphs = this.m_graphs;

        let options: ITreeNode<string>[] = [];
        for (let [id, graph] of graphs.entries())
        {
            options.push(...graph.getTreeNodes(this.m_onlyLeaves, id, graph.name));
        }

        if (this.m_onlySiblings) options = this.getOnlySiblingsOptions(options);

        return options;
    }

    private getOnlySiblingsOptions(options: ITreeNode<string>[])
    {
        return new OnlySiblingsManager(options, () => this.selectComponent.editValues).options;
    }

    ngOnInit()
    {
        this.selectComponent.placeholder   = this.selectComponent.placeholder || "Select node";
        this.selectComponent.autoExpandAll = true;
    }
}

class OnlySiblingsManager
{
    private m_parentLookup: Lookup<string>;

    public readonly options: ITreeNode<string>[];

    constructor(options: ITreeNode<string>[],
                private readonly m_selectedFn: () => string[])
    {
        this.options = options.map((option) => new OnlySiblingsOption(option, (optionId: string) => this.isDisabled(optionId)));

        this.m_parentLookup = {};
        this.buildParentLookup(options, null);
    }

    private isDisabled(id: string): boolean
    {
        let selected = this.m_selectedFn();
        if (!selected.length) return false;
        return this.m_parentLookup[id] !== this.m_parentLookup[selected[0]];
    }

    private buildParentLookup(options: ITreeNode<string>[],
                              parent: string)
    {
        for (let option of options)
        {
            let id                  = option.id;
            this.m_parentLookup[id] = parent;
            this.buildParentLookup(option.children, id);
        }
    }
}

class OnlySiblingsOption implements ITreeNode<string>
{
    get id(): string
    {
        return this.m_option.id;
    }

    get label(): string
    {
        return this.m_option.label;
    }

    public readonly children: OnlySiblingsOption[];

    get hasChildren(): boolean
    {
        return this.m_option.hasChildren;
    }

    get disableSelection(): boolean
    {
        if (this.m_option.disableSelection) return true;
        return this.isDisabledFn(this.id);
    }

    constructor(private m_option: ITreeNode<string>,
                private isDisabledFn: (optionId: string) => boolean)
    {
        this.children = this.m_option.children?.map((child) => new OnlySiblingsOption(child, this.isDisabledFn)) || [];
    }
}
