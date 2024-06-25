import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {AppContext} from "app/app.service";
import {ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import {AggregationHelper, AggregationResult, compareControlPointDisplayLabels, ControlPointAggregation, hasTwoControlPointDisplayLabels} from "app/services/domain/aggregation.helper";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {AggregationTableCell, AggregationTableColumn, AggregationTableComponent, AggregationTableRow, AggregationTableStructure} from "app/shared/aggregation/aggregation-table.component";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {StepwiseColorMapper} from "framework/ui/charting/core/colors";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-node-binding-aggregation-table",
               templateUrl    : "./node-binding-aggregation-table.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class NodeBindingAggregationTable extends BaseApplicationComponent
{
    datatableConfigExt: NodeBindingAggregationTableExtended;

    @Input() graph: Models.AssetGraph;
    @Input() columns: Models.AggregationNodeBinding[]   = [];
    @Input() range: Models.FilterableTimeRange          = Models.FilterableTimeRange.newInstance({range: RangeSelectionExtended.newModel(Models.TimeRangeId.Last24Hours)});
    @Input() mode: Models.HierarchicalVisualizationType = Models.HierarchicalVisualizationType.TABLE;
    @Input() contextDisplay: Models.ControlPointDisplayType;
    @Input() initialSort: Models.SortCriteria;

    @Output() cellSelected = new EventEmitter<AggregationTableBindingCell>();

    @ViewChild(AggregationTableComponent) aggregationTable: AggregationTableComponent;

    currSort: Models.SortCriteria;
    twoLabels: boolean;

    get isBarMode(): boolean
    {
        return this.mode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
    }

    public async refreshContent()
    {
        if (this.graph && this.columns.length && this.range)
        {
            let configExt  = new NodeBindingAggregationTableExtended(this.columns, this.contextDisplay, this.initialSort);
            let rootAssets = new Set<string>();

            let aggregationsByCol = await mapInParallel(this.columns, async (column) =>
            {
                let binding      = Models.AssetGraphBinding.newInstance({nodeId: column.nodeId});
                let aggregations = await AggregationHelper.aggregateByBinding(this.app, this.graph, binding, this.range, column.aggregationType, column.units, column.unitsDisplay);

                let aggregationsByRootAsset: Lookup<ControlPointAggregation[]> = {};
                for (let aggregation of aggregations)
                {
                    let rootAsset = aggregation.tuple[0]?.sysId;
                    rootAssets.add(rootAsset);
                    if (rootAsset)
                    {
                        let rowAggregations = aggregationsByRootAsset[rootAsset];
                        if (!rowAggregations) rowAggregations = aggregationsByRootAsset[rootAsset] = [];

                        rowAggregations.push(aggregation.getAggByTimeRange(0));
                    }
                }

                return aggregationsByRootAsset;
            });

            let rootAssetToAggs: Lookup<ControlPointAggregation[][]> = {};
            for (let rootAsset of rootAssets) rootAssetToAggs[rootAsset] = [];

            for (let colAggs of aggregationsByCol)
            {
                for (let rootAsset of rootAssets)
                {
                    rootAssetToAggs[rootAsset].push(colAggs[rootAsset]);
                }
            }

            await configExt.addRows(this.app, this.graph, rootAssetToAggs);

            this.currSort           = Models.SortCriteria.newInstance(configExt.initialSort);
            this.twoLabels          = hasTwoControlPointDisplayLabels(this.contextDisplay);
            this.datatableConfigExt = configExt;

            this.detectChanges();
        }
    }

    handleSelection(event: AggregationTableCell)
    {
        this.cellSelected.emit(<AggregationTableBindingCell>event);
    }

    refreshSize(): boolean
    {
        return this.aggregationTable?.refreshSize();
    }
}

export class NodeBindingAggregationTableExtended
{
    private m_rowValues: AggregationTableBindingRow[] = [];
    private m_tableStructure: AggregationTableStructure;
    get tableStructure(): AggregationTableStructure
    {
        return this.m_tableStructure;
    }

    constructor(public readonly columns: Models.AggregationNodeBinding[],
                public readonly contextDisplay: Models.ControlPointDisplayType,
                public readonly initialSort: Models.SortCriteria)
    {
        if (!initialSort) this.initialSort = Models.SortCriteria.newInstance({ascending: true});
    }

    sortRows(sort?: Models.SortCriteria)
    {
        if (!sort) sort = this.initialSort;

        let sortFn: (rowA: AggregationTableBindingRow,
                     rowB: AggregationTableBindingRow) => number;
        let colIdx = this.columns.findIndex((col) => col.name === sort.column);
        if (colIdx === -1)
        {
            sortFn = (rowA,
                      rowB) => compareControlPointDisplayLabels(rowA, rowB, sort.ascending);
        }
        else
        {
            sortFn = (rowA,
                      rowB) => UtilsService.compareNumbers(rowA.cells[colIdx]?.value, rowB.cells[colIdx]?.value, sort.ascending, true);
        }

        this.m_rowValues.sort(sortFn);
        this.m_tableStructure.rows = UtilsService.arrayCopy(this.m_rowValues);
    }

    async addRows(app: AppContext,
                  graph: Models.AssetGraph,
                  rootAssetToAggs: Lookup<ControlPointAggregation[][]>)
    {
        this.m_rowValues = [];
        for (let rootAsset in rootAssetToAggs)
        {
            await this.addRow(app, rootAsset, rootAssetToAggs[rootAsset]);
        }

        let aggRanges: ChartValueRange[] = [];
        for (let colIdx = 0; colIdx < this.columns.length; colIdx++)
        {
            const aggRange = new ChartValueRange();
            for (let row of this.m_rowValues)
            {
                let value = row.cells[colIdx].value;
                if (!isNaN(value)) aggRange.expandForValue(value);
            }
            aggRanges.push(aggRange);
        }

        const graphExt                        = new AssetGraphExtended(app.domain, graph);
        let columns: AggregationTableColumn[] = [];
        for (let colIdx = 0; colIdx < this.columns.length; colIdx++)
        {
            let colorMapper: StepwiseColorMapper;

            const columnModel = this.columns[colIdx];
            const aggRange    = aggRanges[colIdx];
            if (columnModel.color && !isNaN(aggRange.diff))
            {
                const stops = new ColorConfigurationExtended(columnModel.color).computeStops(aggRange.min, aggRange.max);
                colorMapper = new StepwiseColorMapper(stops);
            }

            const activeBarRange = ToggleableNumericRangeExtended.isActive(columnModel.barRange);
            for (let row of this.m_rowValues)
            {
                const cell = row.cells[colIdx];
                if (cell)
                {
                    cell.aggRange = aggRange;
                    if (activeBarRange) cell.barRange = columnModel.barRange;
                    if (colorMapper && !isNaN(cell.value)) cell.color = colorMapper.getColor(cell.value);
                }
            }

            let nodeName  = graphExt.getNodeName(columnModel.nodeId);
            let aggString = AggregationHelper.aggTransformPlural(columnModel.aggregationType);
            columns.push({
                             identifier   : columnModel.name,
                             headerLabel  : columnModel.name,
                             headerTooltip: `${aggString} of values from ${nodeName} control points`
                         });
        }

        this.m_tableStructure = new AggregationTableStructure(columns);
        this.sortRows();
    }

    private async addRow(app: AppContext,
                         rootAssetId: string,
                         aggregations: ControlPointAggregation[][])
    {
        let cells = [];
        for (let colIdx = 0; colIdx < aggregations.length; colIdx++)
        {
            cells.push(new AggregationTableBindingCell(rootAssetId, this.columns[colIdx].nodeId, aggregations[colIdx], this.columns[colIdx].aggregationType));
        }

        let rootEquipExt = await app.domain.assets.getExtendedById(rootAssetId);
        let location: string;
        switch (this.contextDisplay)
        {
            case Models.ControlPointDisplayType.LocationOnly:
            case Models.ControlPointDisplayType.NameLocation:
            case Models.ControlPointDisplayType.LocationName:
                location = await AggregationHelper.resolveLocationDetails(rootEquipExt);
                break;

            case Models.ControlPointDisplayType.FullLocationOnly:
            case Models.ControlPointDisplayType.NameFullLocation:
            case Models.ControlPointDisplayType.FullLocationName:
                location = await AggregationHelper.resolveFullLocationDetails(rootEquipExt);
                break;
        }

        let labels = AggregationResult.getLabels(this.contextDisplay, rootEquipExt.model.name, location, "");
        this.m_rowValues.push(new AggregationTableBindingRow(this.m_rowValues.length, labels.primaryLabel, labels.secondaryLabel, cells));
    }
}

class AggregationTableBindingRow implements AggregationTableRow
{
    public readonly id: string;
    public readonly primaryLabel: string;
    public readonly secondaryLabel: string;

    public readonly valid: boolean                         = true;
    public readonly showChildren: boolean                  = true;
    public readonly showThisNode: boolean                  = true;
    public readonly depth: number                          = 0;
    public readonly children: AggregationTableBindingRow[] = [];

    get label(): string
    {
        return this.primaryLabel;
    }

    constructor(public readonly topLevelRowNumber: number,
                primaryLabel: string,
                secondaryLabel: string,
                public readonly cells: AggregationTableBindingCell[])
    {
        this.id             = "" + this.topLevelRowNumber;
        this.primaryLabel   = primaryLabel?.trim();
        this.secondaryLabel = secondaryLabel?.trim();

        for (let cell of cells) cell.row = this;
    }
}

export class AggregationTableBindingCell implements AggregationTableCell
{
    public readonly value: number;
    public readonly formattedValue: string        = "N/A";
    public readonly significantElement: DeviceElementExtended;
    public readonly tooltip: string;
    public readonly compositeAggregation: boolean = false;

    public row: AggregationTableBindingRow;

    public color: string;
    public aggRange: ChartValueRange;
    public barRange: Models.ToggleableNumericRange;
    public barWidthCss: string;

    get colIdx(): number
    {
        if (!this.row) return -1;
        return this.row.cells.indexOf(this) + 1;
    }

    get italicize(): boolean
    {
        return this.compositeAggregation;
    }

    constructor(public readonly rootAssetId: string,
                public readonly nodeId: string,
                aggregations: ControlPointAggregation[],
                aggregationType: Models.AggregationTypeId)
    {
        if (aggregations?.length)
        {
            let relevantAgg: ControlPointAggregation;
            let relevantAggs = aggregations?.filter((aggregation) => !isNaN(aggregation.value ?? NaN));

            if (relevantAggs.length > 1)
            {
                this.compositeAggregation = true;

                let aggregationString: string;
                let validComposite: boolean = false;
                switch (aggregationType)
                {
                    case Models.AggregationTypeId.MIN:
                        relevantAgg = relevantAggs.reduce((min,
                                                           aggregation) => aggregation.value < min.value ? aggregation : min);

                        aggregationString = "Min";
                        validComposite    = true;
                        break;

                    case Models.AggregationTypeId.MAX:
                        relevantAgg = relevantAggs.reduce((max,
                                                           aggregation) => aggregation.value > max.value ? aggregation : max);

                        aggregationString = "Max";
                        validComposite    = true;
                        break;

                    case Models.AggregationTypeId.MEAN:
                    {
                        let values          = relevantAggs.map((aggregation) => aggregation.value);
                        this.value          = UtilsService.sum(...values) / values.length;
                        this.formattedValue = AggregationHelper.numberWithUnitDisplay(this.value, relevantAggs[0].unitsDisplay);

                        aggregationString = "Mean";
                        validComposite    = true;
                        break;
                    }

                    case Models.AggregationTypeId.SUM:
                    {
                        let values          = relevantAggs.map((aggregation) => aggregation.value);
                        this.value          = UtilsService.sum(...values);
                        this.formattedValue = AggregationHelper.numberWithUnitDisplay(this.value, relevantAggs[0].unitsDisplay);

                        aggregationString = "Sum";
                        validComposite    = true;
                        break;
                    }

                    case Models.AggregationTypeId.DELTA:
                        aggregationString = "delta";
                        break;

                    case Models.AggregationTypeId.INCREASE:
                        aggregationString = "increase";
                        break;

                    case Models.AggregationTypeId.DECREASE:
                        aggregationString = "decrease";
                        break;

                    case Models.AggregationTypeId.FIRST:
                        aggregationString = "first";
                        break;

                    case Models.AggregationTypeId.LAST:
                        aggregationString = "last";
                        break;
                }

                if (validComposite)
                {
                    this.tooltip = `${aggregationString} of ${aggregations.length} control points`;
                }
                else
                {
                    this.tooltip = `Cannot get/show the ${aggregationString} of ${aggregations.length} control points`;
                }
            }
            else
            {
                relevantAgg = aggregations[0];
            }

            if (relevantAgg)
            {
                this.value              = relevantAgg.value;
                this.formattedValue     = relevantAgg.formattedValue;
                this.significantElement = relevantAgg.significantElement;
            }
        }
        else
        {
            this.tooltip = "No control point found";
        }
    }
}
