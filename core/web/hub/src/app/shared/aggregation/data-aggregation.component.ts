import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {InteractableSourcesChart} from "app/customer/visualization/time-series-utils";
import {ControlPointAggregation} from "app/services/domain/aggregation.helper";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {AggregationGroupsComponent, AggregationGroupsConfig} from "app/shared/aggregation/aggregation-groups.component";
import {AggregationTableBindingCell, NodeBindingAggregationTable} from "app/shared/aggregation/node-binding-aggregation-table.component";
import {AggregationNodeBindingExtended} from "app/shared/assets/aggregation-node-binding/aggregation-node-binding-configurer.component";
import {ControlOptionItem} from "app/shared/tables/control-option-list.component";

import {UtilsService} from "framework/services/utils.service";
import {Vector2} from "framework/ui/charting/charting-math";
import {ITreeNode} from "framework/ui/dropdowns/filterable-tree.component";

@Component({
               selector       : "o3-data-aggregation",
               templateUrl    : "./data-aggregation.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class DataAggregationComponent extends BaseApplicationComponent
{
    @Input() config: DataAggregationConfig;

    private m_overrideMode: Models.HierarchicalVisualizationType;
    @Input() set overrideMode(mode: Models.HierarchicalVisualizationType)
    {
        this.m_overrideMode = mode;
    }

    get mode(): Models.HierarchicalVisualizationType
    {
        return this.m_overrideMode || this.config.visualizationMode;
    }

    @Input() rangeIdx: number;
    @Input() enableTooltips: boolean;
    @Input() viewport: Vector2;

    @ViewChild(AggregationGroupsComponent) aggregationGroups: AggregationGroupsComponent;
    @ViewChild(NodeBindingAggregationTable) nodeBindingAggTable: NodeBindingAggregationTable;

    @Output() interactableCharts = new EventEmitter<InteractableSourcesChart[]>();
    @Output() itemSelected       = new EventEmitter<DataAggregationItemSelection>();

    needsContentRefresh: boolean = false;

    async bind()
    {
        if (this.config.groups.length)
        {
            if (!this.aggregationGroups) this.detectChanges();
            if (this.aggregationGroups) await this.aggregationGroups.bind();
        }
        else if (this.config.columns.length)
        {
            if (!this.nodeBindingAggTable) this.detectChanges();
            if (this.nodeBindingAggTable) await this.nodeBindingAggTable.refreshContent();
        }
    }

    public async refreshSize(): Promise<boolean>
    {
        if (this.config.groups.length)
        {
            let loaded = !!this.aggregationGroups?.refreshSize();
            if (!this.aggregationGroups || this.aggregationGroups.loading) this.needsContentRefresh = false;
            return loaded;
        }
        else if (this.config.columns.length)
        {
            if (!this.nodeBindingAggTable?.datatableConfigExt) this.needsContentRefresh = false;
            return !!this.nodeBindingAggTable?.refreshSize();
        }

        return false;
    }

    public async refreshContent(): Promise<void>
    {
        if (this.aggregationGroups)
        {
            await this.aggregationGroups.refreshContent();
        }
        else if (this.nodeBindingAggTable)
        {
            await this.nodeBindingAggTable.refreshContent();
        }
    }

    aggregationSelected(aggregation: ControlPointAggregation)
    {
        let item = new DataAggregationItemSelection(aggregation.significantElement, aggregation.range);
        this.itemSelected.emit(item);
    }

    cellSelected(cell: AggregationTableBindingCell)
    {
        let item = new DataAggregationItemSelection(cell.significantElement, this.config.filterableRanges[0].range, cell.rootAssetId, cell.nodeId);
        this.itemSelected.emit(item);
    }
}

export class DataAggregationItemSelection
{
    constructor(public readonly significantElement: DeviceElementExtended,
                public readonly range: Models.RangeSelection,
                public readonly rootAssetId?: string,
                public readonly nodeId?: string)
    {}
}

export interface DataAggregationConfig extends AggregationGroupsConfig
{
    columns: Models.AggregationNodeBinding[];

    graph: Models.AssetGraph;

    initialSort?: Models.SortCriteria;
}

export enum DataAggregationType
{
    Groups   = "Groups",
    Bindings = "Bindings"
}

export class DataAggregationExtended
{
    public readonly options: DataAggregationOption[] = [
        new DataAggregationOption(DataAggregationType.Groups, "Use Control Point Groups", "Allows for multiple time ranges"),
        new DataAggregationOption(DataAggregationType.Bindings, "Use Asset Graph Bindings", "Allows for multiple aggregations per context")
    ];

    private m_groups: Models.ControlPointsGroup[]      = [];
    private m_columns: Models.AggregationNodeBinding[] = [];
    private m_graph: Models.AssetGraph                 = AssetGraphExtended.emptyModel();

    private m_type: DataAggregationType;
    get type(): DataAggregationType
    {
        return this.m_type;
    }

    set type(type: DataAggregationType)
    {
        if (this.m_type !== type)
        {
            switch (type)
            {
                case DataAggregationType.Groups:
                    this.m_columns     = this.model.columns || [];
                    this.m_graph       = this.model.graph || AssetGraphExtended.emptyModel();
                    this.model.columns = [];
                    this.model.graph   = null;

                    this.model.groups = this.m_groups;
                    break;

                case DataAggregationType.Bindings:
                    this.m_groups     = this.model.groups;
                    this.model.groups = [];

                    this.model.graph   = this.m_graph;
                    this.model.columns = this.m_columns;
                    this.updateRanges();
                    if (this.manageInitialSort)
                    {
                        const invalidInitialSort = !this.model.initialSort ||
                                                   this.model.columns.every((col) => col.name !== this.model.initialSort.column);
                        if (invalidInitialSort) this.model.initialSort = Models.SortCriteria.newInstance({ascending: true});
                    }
                    break;
            }

            this.m_type = type;
        }
    }

    constructor(public readonly model: DataAggregationConfig,
                public readonly manageInitialSort: boolean)
    {
        if (this.model.columns?.length && this.model.graph)
        {
            this.m_graph   = this.model.graph;
            this.m_columns = this.model.columns;
            this.m_type    = DataAggregationType.Bindings;

            this.updateRanges();
            this.updateGraph(this.model.graph);
        }
        else if (this.model.groups.length)
        {
            this.m_groups = this.model.groups;
            this.m_type   = DataAggregationType.Groups;
        }
    }

    private updateRanges()
    {
        if (this.m_type === DataAggregationType.Bindings && this.model.filterableRanges?.length > 1)
        {
            this.model.filterableRanges = [this.model.filterableRanges[0]];
        }
    }

    public updateGraph(graph: Models.AssetGraph)
    {
        if (graph)
        {
            this.model.graph = graph;

            let nodeLookup = UtilsService.extractLookup(graph.nodes);
            if (this.model.columns.some((col) => !nodeLookup[col.nodeId]))
            {
                this.model.columns = [];
            }
        }
    }

    public isValid(): boolean
    {
        if (!this.m_type) return false;

        switch (this.m_type)
        {
            case DataAggregationType.Groups:
                return this.model.groups.every((cpGroup) => ControlPointsGroupExtended.isValid(cpGroup, true, false, true, true));

            case DataAggregationType.Bindings:
                if (!this.model.columns.length) return false;
                if (this.model.columns.some((col) => !AggregationNodeBindingExtended.isValid(col))) return false;

                let colNames = this.model.columns.map((col) => col.name);
                return UtilsService.valuesAreUnique(colNames);
        }
    }
}

class DataAggregationOption implements ControlOptionItem<DataAggregationType>,
                                       ITreeNode<DataAggregationType>
{
    public readonly hasChildren: boolean = false;
    public readonly children: DataAggregationOption[];

    constructor(public readonly id: DataAggregationType,
                public readonly label: string,
                public readonly description: string)
    {}
}
