import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import {UUID} from "angular2-uuid";

import {InteractableSourcesChart} from "app/customer/visualization/time-series-utils";
import {AggregationHelper, AggregationResult, compareControlPointDisplayLabels, ControlPointAggregation, hasTwoControlPointDisplayLabels} from "app/services/domain/aggregation.helper";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {UnitsService} from "app/services/domain/units.service";
import {ControlPointsGroupExtended} from "app/services/domain/widget-data.service";
import {ExportsApi} from "app/services/proxy/api/ExportsApi";
import * as Models from "app/services/proxy/model/models";
import {AggregationTableCell, AggregationTableColumn, AggregationTableComponent, AggregationTableRow, AggregationTableStructure} from "app/shared/aggregation/aggregation-table.component";
import {AggregationTreeComponent, AggregationTreeSource} from "app/shared/charting/aggregation-visualization/aggregation-tree.component";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ExcelExporter} from "app/shared/utils/excel-exporter";

import {UtilsService} from "framework/services/utils.service";
import {Vector2} from "framework/ui/charting/charting-math";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, StepwiseColorMapper} from "framework/ui/charting/core/colors";
import {LegendOption, TreeChartComponent, TreeChartMode, TreeNumericAccessor, TreeStringAccessor} from "framework/ui/charting/tree-chart.component";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {mapInParallelNoNulls} from "framework/utils/concurrency";

@Component({
               selector       : "o3-aggregation-groups",
               templateUrl    : "./aggregation-groups.component.html",
               styleUrls      : ["./aggregation-groups.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationGroupsComponent extends BaseApplicationComponent
{
    private static readonly FIRST_COLUMN_HEADER = "Group Name";

    get firstColumnHeader(): string
    {
        return AggregationGroupsComponent.FIRST_COLUMN_HEADER;
    }

    results: AggregationResult[];

    timeRanges: Models.FilterableTimeRange[];
    processedTimeRanges: AggregationGroupsTimeRange[] = [];

    topLevelRows: AggregationGroupRow[] = [];
    tableStructure: AggregationTableStructure;
    groups: Models.ControlPointsGroup[] = [];
    colorConfigLookup                   = new Map<Models.ControlPointsGroup, Models.ColorConfiguration>();
    twoLabels: boolean                  = false;

    tableSort: Models.SortCriteria;

    idFn: TreeStringAccessor<any>;
    weightFn: TreeNumericAccessor<any>;
    labelFn: TreeStringAccessor<any>;
    colorizerFn: TreeStringAccessor<any>;
    formatFn: TreeStringAccessor<any>;
    descriptionFn: TreeStringAccessor<any>;
    chartMode: TreeChartMode;
    legendItems: LegendOption[];
    maxDepth: number = Number.POSITIVE_INFINITY;

    rangeOptions: ControlOption<number>[] = [];

    private m_defaultColor: string;

    loading: boolean = true;

    @Input() config: AggregationGroupsConfig;
    @Input() enableTooltips: boolean;
    @Input() viewport: Vector2 = null;

    private m_rangeIdx: number = 0;
    @Input() set rangeIdx(range: number)
    {
        if (!isNaN(range))
        {
            this.m_rangeIdx = range;
            this.chart?.chart?.process();
        }
    }

    get rangeIdx(): number
    {
        return this.m_rangeIdx;
    }

    private m_mode: Models.HierarchicalVisualizationType;
    @Input() set mode(mode: Models.HierarchicalVisualizationType)
    {
        if (this.m_mode !== mode)
        {
            this.m_mode = mode;

            if (this.groups.length)
            {
                this.updateColors();
                if (this.isChart) this.setChartMode();
            }
        }
    }

    get mode(): Models.HierarchicalVisualizationType
    {
        return this.m_mode || this.config.visualizationMode || Models.HierarchicalVisualizationType.TABLE;
    }

    @Output() aggregationSelected = new EventEmitter<ControlPointAggregation>();
    @Output() selectedRangeChange = new EventEmitter<number>();
    @Output() interactableCharts  = new EventEmitter<InteractableSourcesChart[]>();

    @ViewChild(AggregationTreeComponent) chart: AggregationTreeComponent;
    @ViewChild(AggregationTableComponent) table: AggregationTableComponent;

    get isTable(): boolean
    {
        switch (this.mode)
        {
            case Models.HierarchicalVisualizationType.TABLE:
            case Models.HierarchicalVisualizationType.TABLE_WITH_BAR:
                return true;

            default:
                return false;
        }
    }

    get isChart(): boolean
    {
        switch (this.mode)
        {
            case Models.HierarchicalVisualizationType.BUBBLEMAP:
            case Models.HierarchicalVisualizationType.TREEMAP:
            case Models.HierarchicalVisualizationType.SUNBURST:
            case Models.HierarchicalVisualizationType.PIEBURST:
            case Models.HierarchicalVisualizationType.DONUT:
            case Models.HierarchicalVisualizationType.PIE:
                return true;

            default:
                return false;
        }
    }

    get isBar(): boolean
    {
        return this.mode === Models.HierarchicalVisualizationType.TABLE_WITH_BAR;
    }

    async bind()
    {
        this.groups     = this.config.groups.filter((group) => ControlPointsGroupExtended.isValid(group, true, false, true, true));
        this.timeRanges = this.app.domain.widgetData.getValidRanges(this.config.filterableRanges);

        for (let group of this.groups)
        {
            if (!group.colorConfig?.segments?.length) group.colorConfig = null;
        }

        await this.refreshContent();
    }

    async refreshContent()
    {
        const cpDisplayType = this.config.controlPointDisplayType;
        this.results        = await mapInParallelNoNulls(this.groups, (group) => AggregationHelper.aggregateControlPointsGroupSingle(this.app, group, this.timeRanges, cpDisplayType));
        this.topLevelRows   = this.results.map((aggResult,
                                                groupIdx) => new AggregationGroupRow(groupIdx + "", aggResult, this.getGroupColor(aggResult.group), groupIdx, cpDisplayType));

        this.updateColors();
        this.tableSetup();
        this.chartSetup();

        this.loading = false;
        this.detectChanges();
    }

    getGroupColor(aggregationGroup: Models.ControlPointsGroup): string
    {
        let colorConfig = this.colorConfigLookup.get(aggregationGroup);
        return this.mode !== Models.HierarchicalVisualizationType.TABLE && colorConfig?.segments[0].color || this.m_defaultColor;
    }

    private updateColors()
    {
        let otherColors: string[] = [];
        for (let group of this.groups)
        {
            let colorConfig = ControlPointsGroupExtended.ensureColorConfig(group, this.mode, otherColors);
            this.colorConfigLookup.set(group, colorConfig);

            let color = colorConfig.segments[0].color;
            if (color) otherColors.push(color);
        }

        switch (this.mode)
        {
            case Models.HierarchicalVisualizationType.TABLE:
                this.m_defaultColor = ChartColorUtilities.getColorById("Gray", "gray4").hex;
                break;

            default:
                this.m_defaultColor = ChartColorUtilities.getDefaultColorById("blue").hex;
                break;
        }
    }

    private tableSetup()
    {
        if (!this.timeRanges) return;

        this.twoLabels = hasTwoControlPointDisplayLabels(this.config.controlPointDisplayType);

        let flattenedRows = this.flattenResults(this.topLevelRows);
        let firstRow      = flattenedRows[0];
        if (!firstRow) return;

        let firstGroup             = this.groups[0];
        let sameUnits              = this.groups.every((group) => UnitsService.areIdentical(firstGroup.unitsFactors, group.unitsFactors));
        const compareBetweenGroups = sameUnits && !this.config.isolateGroupRanges;

        let aggValueRanges = this.topLevelRows.map((topLevelRow) => topLevelRow.calculateAggregationRanges());
        if (compareBetweenGroups)
        {
            for (let rangeIdx = 0; rangeIdx < this.timeRanges.length; rangeIdx++)
            {
                let valueRange = aggValueRanges[0][rangeIdx];
                for (let groupIdx = 1; groupIdx < aggValueRanges.length; groupIdx++)
                {
                    valueRange.expandToContain(aggValueRanges[groupIdx][rangeIdx]);
                    aggValueRanges[groupIdx][rangeIdx] = valueRange;
                }
            }
        }

        for (let groupIdx = 0; groupIdx < this.topLevelRows.length; groupIdx++)
        {
            let topLevelRow = this.topLevelRows[groupIdx];
            let aggRanges   = aggValueRanges[groupIdx];

            let colorConfig = topLevelRow.group?.colorConfig;
            let colorMappers: StepwiseColorMapper[];
            if (colorConfig?.segments?.length > 1)
            {
                let colorExt = new ColorConfigurationExtended(colorConfig);
                colorMappers = aggRanges.map((range) =>
                                             {
                                                 if (!isNaN(range.diff))
                                                 {
                                                     let stops = colorExt.computeStops(range.min, range.max);
                                                     return new StepwiseColorMapper(stops);
                                                 }
                                                 return null;
                                             });
            }

            this.updateValueRanges(topLevelRow, aggRanges, colorMappers);
        }

        if (!this.processedTimeRanges.length)
        {
            this.processedTimeRanges = this.timeRanges.map((validRange) => new AggregationGroupsTimeRange(validRange));
        }

        let cols: AggregationTableColumn[]   = this.processedTimeRanges.map((range) =>
                                                                            {
                                                                                return {
                                                                                    identifier   : range.id,
                                                                                    headerLabel  : range.label,
                                                                                    headerTooltip: (range.hasName ? "over" : "") + range.description
                                                                                };
                                                                            });
        this.tableStructure                  = new AggregationTableStructure(cols);
        this.tableStructure.hierarchicalRows = this.topLevelRows;
        this.tableStructure.rows             = flattenedRows;

        if (!this.tableSort)
        {
            let initiallyAscending = false;

            let firstChild = firstRow.children.length > 0 && flattenedRows[1];
            if (firstRow.aggType === Models.AggregationTypeId.MIN ||
                firstRow.aggType === Models.AggregationTypeId.NONE && firstChild?.aggType === Models.AggregationTypeId.MIN)
            {
                initiallyAscending = true;
            }

            this.tableSort = Models.SortCriteria.newInstance({
                                                                 column   : this.processedTimeRanges[this.processedTimeRanges.length - 1].id,
                                                                 ascending: initiallyAscending
                                                             });
        }
        this.sortRows();
    }

    private chartSetup()
    {
        this.idFn          = (result: AggregationResult) => this.idFnInternal(result);
        this.weightFn      = (result: AggregationResult) => this.weightFnInternal(result);
        this.colorizerFn   = this.cachedColorizer(this.results);
        this.labelFn       = (result: AggregationResult) => this.labelFnInternal(result);
        this.formatFn      = (result: AggregationResult) => this.formatFnInternal(result);
        this.descriptionFn = (result: AggregationResult) => this.descriptionFnInternal(result);
        this.rangeOptions  = this.processedTimeRanges.map((aggRange,
                                                           index) => new ControlOption(index, aggRange.description));

        this.setChartMode();
    }

    private setChartMode()
    {
        switch (this.mode)
        {
            case Models.HierarchicalVisualizationType.BUBBLEMAP:
                this.chartMode = TreeChartMode.BUBBLE;
                this.maxDepth  = Infinity;
                break;

            case Models.HierarchicalVisualizationType.TREEMAP:
                this.chartMode = TreeChartMode.BOX;
                this.maxDepth  = Infinity;
                break;

            case Models.HierarchicalVisualizationType.SUNBURST:
                this.chartMode = TreeChartMode.SUNBURST;
                this.maxDepth  = Infinity;
                break;

            case Models.HierarchicalVisualizationType.DONUT:
                this.chartMode = TreeChartMode.SUNBURST;
                this.maxDepth  = 0;
                break;

            case Models.HierarchicalVisualizationType.PIEBURST:
                this.chartMode = TreeChartMode.PIEBURST;
                this.maxDepth  = Infinity;
                break;

            case Models.HierarchicalVisualizationType.PIE:
                this.chartMode = TreeChartMode.PIEBURST;
                this.maxDepth  = 0;
                break;
        }

        this.colorizerFn = this.cachedColorizer(this.results);
        this.legendItems = [];
    }

    private flattenResults(results: AggregationGroupRow[]): AggregationGroupRow[]
    {
        let flattenedResults: AggregationGroupRow[] = [];
        for (let result of results)
        {
            if (result)
            {
                this.preOrderAdd(result, flattenedResults, true, 0);
            }
        }

        return flattenedResults;
    }

    private preOrderAdd(row: AggregationGroupRow,
                        targetArr: AggregationGroupRow[],
                        showThisNode: boolean,
                        currDepth: number,
                        valueRanges?: ChartValueRange[],
                        colorMappers?: StepwiseColorMapper[]): void
    {
        row.depth        = currDepth;
        row.showThisNode = showThisNode;

        targetArr.push(row);
        row.children.forEach((child) => this.preOrderAdd(child, targetArr, row.showChildren && showThisNode, currDepth + 1, valueRanges, colorMappers));
    }

    private updateValueRanges(row: AggregationGroupRow,
                              valueRanges: ChartValueRange[],
                              colorMappers?: StepwiseColorMapper[])
    {
        for (let i = 0; i < this.timeRanges.length; i++)
        {
            let cell      = row.cells[i];
            cell.aggRange = valueRanges[i];

            let colorMapper = colorMappers?.[i];
            if (colorMapper) cell.color = colorMapper.getColor(cell.value);
        }

        row.children.forEach((child) => this.updateValueRanges(child, valueRanges, colorMappers));
    }

    sortRows()
    {
        let rangeIdx             = this.processedTimeRanges.findIndex((aggRange) => aggRange.id === this.tableSort.column);
        this.tableStructure.rows = rangeIdx === -1 ?
            this.flattenResults(this.topLevelRows.map((resultGroup) => resultGroup.sortByName(this.tableSort.ascending))) :
            this.flattenResults(this.topLevelRows.map((resultGroup) => resultGroup.sortByRange(rangeIdx, this.tableSort.ascending)));
    }

    handleSelection(aggCell: AggregationTableCell)
    {
        let range = this.timeRanges[aggCell.colIdx - 1];
        if (range) this.aggregationSelected.emit(new ControlPointAggregation(aggCell.value, aggCell.significantElement, range.range));
    }

    private findPath(root: AggregationResult[],
                     target: AggregationResult): number[]
    {
        let index = root.indexOf(target);
        if (index >= 0)
        {
            return [index];
        }
        else
        {
            for (let i = 0; i < root.length; i++)
            {
                if (root[i].children?.length > 0)
                {
                    let result = this.findPath(root[i].children, target);
                    if (result) return [i].concat(result);
                }
            }

            return null;
        }
    }

    private idFnInternal(result: AggregationResult): string
    {
        if (result.asset?.model?.sysId) return result.asset.model.sysId;

        let path = this.findPath(this.results, result);
        return path ? path.join("-") : null;
    }

    private weightFnInternal(result: AggregationResult): number
    {
        let weight = result.aggregationsByTimeRange[this.rangeIdx].value;
        return weight > 0 ? weight : 0;
    }

    private labelFnInternal(result: AggregationResult): string
    {
        switch (this.config.controlPointDisplayType)
        {
            case Models.ControlPointDisplayType.NameOnly:
                return result.cpLabel;

            case Models.ControlPointDisplayType.LocationOnly:
            case Models.ControlPointDisplayType.FullLocationOnly:
                return result.locationDisplay || result.cpLabel;

            case Models.ControlPointDisplayType.LocationName:
            case Models.ControlPointDisplayType.FullLocationName:
                return [
                    result.locationDisplay,
                    result.cpLabel
                ].filter((s) => !!s)
                 .join("\n");

            case Models.ControlPointDisplayType.NameLocation:
            case Models.ControlPointDisplayType.NameFullLocation:
                return [
                    result.cpLabel,
                    result.locationDisplay
                ].filter((s) => !!s)
                 .join("\n");
        }

        return result.cpLabel;
    }

    private descriptionFnInternal(result: AggregationResult): string
    {
        return AggregationHelper.aggregationToLabel(result);
    }

    private formatFnInternal(result: AggregationResult): string
    {
        return AggregationHelper.numberWithUnitDisplay(result.getAggByTimeRange(this.rangeIdx).value, result.unitsDisplay);
    }

    private cachedColorizer(tree: AggregationResult[]): TreeStringAccessor<AggregationResult>
    {
        // Build a color cache
        let cacheByGroup     = new Map<Models.ControlPointsGroup, string[]>();
        let cacheByAsset     = new Map<string, string[]>();
        let limitedDepth     = this.maxDepth === 0;
        let singleGroup      = tree.length === 1;
        let individualColors = limitedDepth && singleGroup;
        let depthSkew        = individualColors ? -1 : 0;

        if (!individualColors)
        {
            for (let node of tree)
            {
                cacheByGroup.set(node.group, TreeChartComponent.depthBasedColorScale([node], this.getGroupColor(node.group), "#ffffff"));
            }
        }
        else
        {
            let palette = ChartColorUtilities.getPaletteFromColor(tree[0].group?.colorConfig?.segments?.[0]?.color) || ChartColorUtilities.defaultPalette;

            let children = tree[0].children;
            for (let i = 0; i < children.length; i++)
            {
                let id    = children[i]?.asset?.model?.sysId;
                let color = ChartColorUtilities.getColor(i, palette);
                cacheByAsset.set(id, TreeChartComponent.depthBasedColorScale([tree[0]], color, "#ffffff"));
            }
        }

        // Return a color accessor
        return (node: AggregationResult) =>
        {
            if (individualColors)
            {
                return node.asset ? cacheByAsset.get(node?.asset?.model?.sysId)[node.depth + depthSkew] : "#cccccc";
            }
            else
            {
                return cacheByGroup.get(node.group)[node.depth + depthSkew];
            }
        };
    }

    refreshSize(): boolean
    {
        if (this.chart)
        {
            this.chart.chart.resize();
            return true;
        }

        if (this.table)
        {
            return this.table.refreshSize();
        }

        return false;
    }

    onSourcesChanged(sources: AggregationTreeSource[])
    {
        // Get distinct charts
        let distinct = new Set<AggregationTreeComponent>(sources.map((s) => s.chart));

        // Emit set of charts
        this.interactableCharts.emit(Array.from(distinct));
    }
}

class AggregationGroupRow implements AggregationTableRow
{
    public readonly cells: AggregationGroupCell[];

    children: AggregationGroupRow[];

    get valid(): boolean
    {
        return this.children.length === 0 || this.aggResult.aggType !== Models.AggregationTypeId.NONE;
    }

    depth: number;
    primaryLabel: string;
    secondaryLabel: string;

    get label(): string
    {
        return this.primaryLabel;
    }

    get group(): Models.ControlPointsGroup
    {
        return this.aggResult.group;
    }

    get aggType(): Models.AggregationTypeId
    {
        return this.aggResult.aggType;
    }

    showThisNode: boolean = true;
    showChildren: boolean = true;

    constructor(public readonly id: string,
                public readonly aggResult: AggregationResult,
                private readonly m_color: string,
                public readonly topLevelRowNumber: number,
                displayType?: Models.ControlPointDisplayType)
    {
        if (displayType) this.aggResult.displayType = displayType;
        this.children = this.aggResult.children.map((aggResult,
                                                     childIdx) => new AggregationGroupRow(`${this.id}-${childIdx}`, aggResult, m_color, topLevelRowNumber, displayType));

        this.depth          = this.aggResult.depth;
        this.primaryLabel   = (this.aggResult.primaryLabel || this.aggResult.cpLabel)?.trim();
        this.secondaryLabel = this.aggResult.secondaryLabel?.trim();

        let tooltip  = AggregationHelper.aggregationToLabel(aggResult);
        let barRange = aggResult.group?.range;
        this.cells   = aggResult.aggregationsByTimeRange.map((cpAgg,
                                                              rangeIdx) => new AggregationGroupCell(cpAgg, tooltip, barRange, rangeIdx + 1, m_color));
    }

    calculateAggregationRanges(): ChartValueRange[]
    {
        return this.aggResult.calculateAggregationRanges();
    }

    sortByName(ascending: boolean): AggregationGroupRow
    {
        return this.sortInner((a,
                               b) => compareControlPointDisplayLabels(a, b, ascending));
    }

    sortByRange(timeRangeIdx: number,
                ascending: boolean): AggregationGroupRow
    {
        return this.sortInner((a,
                               b) => UtilsService.compareNumbers(a.cells[timeRangeIdx].value, b.cells[timeRangeIdx].value, ascending, true));
    }

    private sortInner(sortFn: (a: AggregationGroupRow,
                               b: AggregationGroupRow) => number): AggregationGroupRow
    {
        this.children = this.children.map((child) => child.sortInner(sortFn))
                            .sort(sortFn);
        return this;
    }
}

class AggregationGroupCell implements AggregationTableCell
{
    get value(): number
    {
        return this.m_cpAgg.value;
    }

    get formattedValue(): string
    {
        return this.m_cpAgg.formattedValue;
    }

    get significantElement(): DeviceElementExtended
    {
        return this.m_cpAgg.significantElement;
    }

    private m_color: string;
    get color(): string
    {
        return this.m_color || this.m_defaultColor;
    }

    set color(color: string)
    {
        this.m_color = color;
    }

    get hasOverrideColor(): boolean
    {
        return !!this.m_color;
    }

    aggRange: ChartValueRange;
    barWidthCss: string;

    public readonly italicize = false;

    constructor(private readonly m_cpAgg: ControlPointAggregation,
                public readonly tooltip: string,
                public readonly barRange: Models.ToggleableNumericRange,
                public readonly colIdx: number,
                private readonly m_defaultColor: string)
    {}
}

export interface AggregationGroupsConfig
{
    groups: Models.ControlPointsGroup[];

    filterableRanges: Models.FilterableTimeRange[];

    controlPointDisplayType: Models.ControlPointDisplayType;

    visualizationMode: Models.HierarchicalVisualizationType;

    visualizationLegend: boolean;

    visualizationRanges: boolean;

    isolateGroupRanges: boolean;

    initialSort?: Models.SortCriteria;
}

export class AggregationGroupsTimeRange
{
    readonly id: string = UUID.UUID();

    readonly hasName: boolean;
    readonly description: string;

    get label(): string
    {
        return this.hasName ? this.filterableRange.name : this.description;
    }

    constructor(public readonly filterableRange: Models.FilterableTimeRange)
    {
        this.hasName     = filterableRange.name && filterableRange.isFilterApplied;
        this.description = RangeSelectionExtended.getFilterableDisplayName(filterableRange);
    }
}

export class AggregationGroupDownloadGenerator implements DownloadGenerator
{
    private m_aggregationLabelFn: (agg: AggregationResult) => string;

    constructor(private m_exportsApi: ExportsApi,
                private m_fileName: string,
                private m_sheetName: string,
                private m_host: AggregationGroupsComponent)
    {}

    private getAggLabel(aggregation: AggregationResult): string
    {
        return this.m_aggregationLabelFn(aggregation);
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public getProgressMessage(): string
    {
        return "Generating Excel file...";
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        return true;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate(): boolean
    {
        return false;
    }

    public async getResults(): Promise<DownloadResults>
    {
        let exporter = new ExcelExporter(this.m_exportsApi, this.m_sheetName, "NO DATA");
        switch (this.m_host.config.controlPointDisplayType)
        {
            case Models.ControlPointDisplayType.EquipmentOnly:
                exporter.addColumnHeader("Equipment");
                this.m_aggregationLabelFn = (agg: AggregationResult) => agg.equipmentDisplay;
                break;

            case Models.ControlPointDisplayType.EquipmentName:
                exporter.addColumnHeader("Equipment (Label)");
                this.m_aggregationLabelFn = (agg: AggregationResult) => `${agg.equipmentDisplay} (${agg.cpLabel})`;
                break;

            case Models.ControlPointDisplayType.NameEquipment:
                exporter.addColumnHeader("Label (Equipment)");
                this.m_aggregationLabelFn = (agg: AggregationResult) => `${agg.cpLabel} (${agg.equipmentDisplay})`;
                break;

            case Models.ControlPointDisplayType.LocationOnly:
            case Models.ControlPointDisplayType.FullLocationOnly:
                exporter.addColumnHeader("Location");
                this.m_aggregationLabelFn = (agg: AggregationResult) => agg.locationDisplay;
                break;

            case Models.ControlPointDisplayType.LocationName:
            case Models.ControlPointDisplayType.FullLocationName:
                exporter.addColumnHeader("Location (Label)");
                this.m_aggregationLabelFn = (agg: AggregationResult) => `${agg.locationDisplay} (${agg.cpLabel})`;
                break;

            case Models.ControlPointDisplayType.NameLocation:
            case Models.ControlPointDisplayType.NameFullLocation:
                exporter.addColumnHeader("Label (Location)");
                this.m_aggregationLabelFn = (agg: AggregationResult) => `${agg.cpLabel} (${agg.locationDisplay})`;
                break;

            case Models.ControlPointDisplayType.NameOnly:
            default:
                exporter.addColumnHeader("Label");
                this.m_aggregationLabelFn = (agg: AggregationResult) => agg.cpLabel;
                break;
        }
        exporter.addColumnHeader("Depth");
        for (let range of this.m_host.processedTimeRanges)
        {
            exporter.addColumnHeader(range.label);
        }

        let rows = <AggregationGroupRow[]>this.m_host.tableStructure?.rows || [];
        for (let aggregation of rows)
        {
            let row = await exporter.addRow();

            let aggResult = aggregation.aggResult;
            row.push(this.getAggLabel(aggResult) || aggResult.cpLabel, aggResult.depth);

            for (let cell of aggregation.cells)
            {
                row.push(cell.value ?? NaN);
            }
        }

        return exporter.getResults(this.m_fileName);
    }
}
