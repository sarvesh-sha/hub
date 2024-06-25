import {ScrollDispatcher} from "@angular/cdk/scrolling";
import {AfterViewChecked, ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, QueryList, SimpleChanges, ViewChild, ViewChildren} from "@angular/core";

import {AppContext} from "app/app.service";
import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost, ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import {ContextPaneComponent} from "app/dashboard/context-pane/panes/context-pane.component";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {HierarchicalVisualizationConfigurationComponent} from "app/shared/charting/hierarchical-visualization/hierarchical-visualization-configuration.component";
import {InteractiveTreeComponent, InteractiveTreeRow, PivotTable, PivotTableView} from "app/shared/charting/interactive-tree/interactive-tree.component";
import {TimeSeriesChartComponent} from "app/shared/charting/time-series-chart/time-series-chart.component";
import {TimeSeriesChartingComponent} from "app/shared/charting/time-series-container/common";
import {ColorConfigurationExtended} from "app/shared/colors/color-configuration-extended";
import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {MinEdgePadding} from "framework/ui/charting/app-charting-utilities";
import {ChartTimelineComponent, ChartZoomState} from "framework/ui/charting/chart-timeline.component";
import {EventReceiver} from "framework/ui/charting/charting-interaction";
import {ChartValueRange} from "framework/ui/charting/core/basics";
import {ColorGradientDiscrete, ColorMapper, PaletteId, StepwiseColorMapper} from "framework/ui/charting/core/colors";
import {ChartPointSource} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {HeatmapComponent} from "framework/ui/charting/heatmap.component";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {ControlOption} from "framework/ui/control-option";
import {SelectComponent} from "framework/ui/forms/select.component";
import {OverlayComponent, OverlayConfig} from "framework/ui/overlays/overlay.component";
import {inParallel} from "framework/utils/concurrency";
import {AsyncDebouncer, Debouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-hierarchical-visualization",
               templateUrl    : "./hierarchical-visualization.component.html",
               styleUrls      : ["./hierarchical-visualization.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class HierarchicalVisualizationComponent extends SharedSvc.BaseApplicationComponent implements AfterViewChecked,
                                                                                                      TimeSeriesChartingComponent
{
    private m_chartRange: ChartTimeRange;
    get chartRange(): ChartTimeRange
    {
        return this.m_chartRange;
    }

    private m_detectChangesDebouncer = new Debouncer(50, async () => this.detectChanges());
    private m_timeChangedDebouncer   = new AsyncDebouncer(400, () => this.rebuild(this.rows));

    @Input() set range(range: Models.RangeSelection)
    {
        this.m_range = range;
        this.m_timeChangedDebouncer.invoke();
    }

    private m_rangeExt: RangeSelectionExtended;
    private m_range: Models.RangeSelection;
    get range(): Models.RangeSelection
    {
        return this.m_range;
    }

    private m_outerRangeChangeSub: Subscription;
    private m_zoomState: ChartZoomState;
    set zoomState(zoomState: ChartZoomState)
    {
        this.m_zoomState = zoomState;

        if (this.m_outerRangeChangeSub)
        {
            this.m_outerRangeChangeSub.unsubscribe();
            this.m_outerRangeChangeSub = null;
        }

        if (this.m_zoomState)
        {
            this.m_outerRangeChangeSub = this.subscribeToObservable(this.m_zoomState.stateChanged, (fromScrub: boolean) =>
            {
                if (!fromScrub && !this.m_chartRange.isSame(this.m_zoomState.outerRange))
                {
                    this.rebuild(this.rows);
                }
            });
        }
    }

    get zoomState(): ChartZoomState
    {
        return this.m_zoomState;
    }

    private m_bindings: Models.HierarchicalVisualizationBinding[] = [];
    @Input() set bindings(bindings: Models.HierarchicalVisualizationBinding[])
    {
        if (bindings)
        {
            this.m_bindings = bindings;

            this.rebuildBindingInfos(this.rows);
        }
    }

    @Input() table: PivotTable;
    @Input() interactionBehavior: Models.InteractionBehavior;

    private m_viewWindow: VerticalViewWindow;
    @Input() set viewWindow(viewWindow: VerticalViewWindow)
    {
        this.m_viewWindow = viewWindow;
        this.checkViewThrottler.invoke();
    }

    get viewWindow(): VerticalViewWindow
    {
        return this.m_viewWindow;
    }

    @Output() bindingsChange      = new EventEmitter<Models.HierarchicalVisualizationBinding[]>();
    @Output() minHeightChange     = new EventEmitter<number>();
    @Output() stoppedFetchingData = new EventEmitter<void>();
    @Output() startedFetchingData = new EventEmitter<void>();

    @ViewChild("colorDialog", {static: true}) colorDialog: OverlayComponent;
    @ViewChild("optionsDialog", {static: true}) optionsDialog: OverlayComponent;
    @ViewChild("paneOverlay") paneOverlay: OverlayComponent;
    @ViewChild(ChartTimelineComponent) timeline: ChartTimelineComponent;
    @ViewChild("rowsContainer") rowsContainer: ElementRef;
    @ViewChildren("rowElement") rowElements !: QueryList<ElementRef>;

    @ViewChild("test_configurer") test_configurer: HierarchicalVisualizationConfigurationComponent;
    @ViewChild("test_editBinding") test_editBinding: SelectComponent<string>;

    private m_trendlines: QueryList<TimeSeriesChartComponent>;
    @ViewChildren(TimeSeriesChartComponent) set trendlines(trendLines: QueryList<TimeSeriesChartComponent>)
    {
        this.m_trendlines = trendLines;
        this.registerRows(trendLines, (row: Row,
                                       trendLine: TimeSeriesChartComponent) => trendLine.configExt === row.lineConfigExt);
    }

    private m_heatmaps: QueryList<HeatmapComponent>;
    @ViewChildren(HeatmapComponent) set heatmaps(heatmaps: QueryList<HeatmapComponent>)
    {
        this.m_heatmaps = heatmaps;
        this.registerRows(heatmaps, (row: Row,
                                     heatmap: HeatmapComponent) => heatmap.sources === row.heatmapSources);
    }

    tableDisplay: PivotTableView;

    get numHeatmaps(): number
    {
        return this.m_heatmaps?.length;
    }

    get numTrendlines(): number
    {
        return this.m_trendlines?.length;
    }

    private m_rowsInView: number;
    numRowsLoading: number = 0;
    rows: Row[]            = [];
    private m_prevHover: EventReceiver;

    leafNodeOptions: ControlOption<string>[] = [];
    selectedLeafNodeId: string;

    bindingInfoLookup: Lookup<HierarchicalVisualizationBindingInfo> = {};

    get usingPalette(): boolean
    {
        let bindingInfo = this.bindingInfoLookup[this.selectedLeafNodeId];
        return bindingInfo.isTrendline || !!bindingInfo.enumRange;
    }

    private m_minChartHeight       = 0;
    private m_interactiveTreeWidth = 0;

    get rowsMaxWidthCss(): string
    {
        return `calc(100% - ${this.m_interactiveTreeWidth}px)`;
    }

    get timelinePadding(): number
    {
        let someAreTrendlines = false;
        for (let leafNodeId in this.bindingInfoLookup)
        {
            if (this.bindingInfoLookup[leafNodeId]?.isTrendline)
            {
                someAreTrendlines = true;
                break;
            }
        }

        return someAreTrendlines ? MinEdgePadding : 0;
    }

    get heatmapPadding(): string
    {
        let padding = this.timelinePadding;
        return `0px ${padding}px 0px ${padding}px`;
    }

    get minChartHeight(): number
    {
        return this.m_minChartHeight;
    }

    get isFixed(): boolean
    {
        let allAreFixed = true;
        for (let leafNodeId in this.bindingInfoLookup)
        {
            if (this.bindingInfoLookup[leafNodeId].options.sizing !== Models.HierarchicalVisualizationSizing.FIXED)
            {
                allAreFixed = false;
                break;
            }
        }
        return allAreFixed;
    }

    private m_rowsUpdated = false;

    private checkViewThrottler = new Debouncer(40, async () => this.checkView());
    private viewChanged        = false;

    private m_fetchedRows = new Map<string, Row>();

    paneConfig: Models.PaneConfiguration;
    paneModels: Models.Pane[];
    paneOverlayConfig: OverlayConfig;

    editColorConfig    = ColorPickerConfigurationComponent.colorOverlayConfig(true, 666);
    chartOptionsConfig = OverlayConfig.onTopDraggable();

    private readonly host = new TimeSeriesSourceHost(this);

    constructor(inj: Injector,
                private scrolling: ScrollDispatcher)
    {
        super(inj);

        this.subscribeToObservableUntil(this.scrolling.scrolled(0), () => this.invokeViewDebouncer(), () => !!this.m_viewWindow);
    }

    async ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (changes.table)
        {
            this.resetView();

            if (this.table?.tuples?.length)
            {
                this.selectedLeafNodeId = this.table.leafNodeIds[0];
                this.leafNodeOptions    = this.table.leafNodeIds.map((leafNodeId) => new ControlOption(leafNodeId, this.table.nodeName(leafNodeId)));

                this.tableDisplay = await PivotTableView.new(this.table, this.m_bindings);
            }
            else
            {
                this.tableDisplay = null;
            }

            this.markViewChange();
        }

        if (changes.interactionBehavior)
        {
            this.paneConfig = await this.app.domain.panes.getConfig(this.interactionBehavior?.paneConfigId);
        }

        this.invokeViewDebouncer();
    }

    protected afterLayoutChange()
    {
        super.afterLayoutChange();
        this.invokeViewDebouncer();
    }

    //--//

    async onRows(interactiveRows: InteractiveTreeRow[])
    {
        this.numRowsLoading = interactiveRows.length;

        let prevRows: Lookup<Row> = {};
        if (this.rows) UtilsService.extractLookup(this.rows, prevRows);

        let newRows = false;
        let rows    = [];
        for (let row of interactiveRows)
        {
            let prevRow = prevRows[row.pivotRow.deviceElemId];
            newRows ||= !prevRow;
            rows.push(new Row(row, prevRow));
        }

        if (newRows)
        {
            this.m_timeChangedDebouncer.cancelInvocation();
            await inParallel(rows, (row) => row.updateConfig(this.app));
            await this.rebuild(rows);
        }

        this.rows           = rows;
        this.numRowsLoading = 0;
        this.m_rowsUpdated  = true;

        this.onMouseLeave();
        this.invokeViewDebouncer();
        this.refreshSize();
    }

    private async rebuild(rows: Row[])
    {
        if (!rows?.length) return;

        this.m_rangeExt   = new RangeSelectionExtended(this.m_range);
        this.m_chartRange = this.m_rangeExt.getChartRange();

        await this.rebuildBindingInfos(rows);
        await this.updateRowTimeRanges(rows);
    }

    private async rebuildBindingInfos(rows: Row[])
    {
        let bindingInfo = this.m_bindings.map((binding) => new HierarchicalVisualizationBindingInfo(binding));
        UtilsService.extractLookup(bindingInfo, this.bindingInfoLookup);

        await this.updateBindingInfos(rows);
    }

    private async updateBindingInfos(rows: Row[])
    {
        if (!rows.length) return;

        let bindingInfosToUpdate = [];
        for (let leafNodeId in this.bindingInfoLookup) bindingInfosToUpdate.push(this.bindingInfoLookup[leafNodeId]);

        await inParallel(bindingInfosToUpdate, (bindingInfo) => this.updateBindingInfo(rows, bindingInfo));

        this.updateTrendlineColors(rows);
        this.markForCheck();
    }

    private async updateBindingInfo(rows: Row[],
                                    bindingInfo: HierarchicalVisualizationBindingInfo)
    {
        let rangeMin = this.m_chartRange.minAsMoment;
        let rangeMax = this.m_chartRange.maxAsMoment;

        let relevantRows     = bindingInfo.relevantRows(rows);
        let recordIds        = relevantRows.map((row) => Models.RecordIdentity.newInstance({sysId: row.id}));
        let deviceElemExts   = <DeviceElementExtended[]>await this.app.domain.assets.getExtendedBatch(recordIds);
        let deviceElemLookup = UtilsService.extractLookup(deviceElemExts);

        for (let row of relevantRows)
        {
            let deviceElemExt = deviceElemLookup[row.interactiveRow.pivotRow.deviceElemId];
            let schema        = await deviceElemExt?.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
            if (schema?.values?.length)
            {
                bindingInfo.enumRange = schema.values;
                break; // select the first enumerated range
            }
        }

        if (bindingInfo.usingSharedRange)
        {
            let sharedRange: ChartValueRange;
            if (bindingInfo.enumRange)
            {
                sharedRange = new ChartValueRange();
                sharedRange.expandToContain(new ChartValueRange(0, bindingInfo.enumRange.length - 1));
            }
            else
            {
                await inParallel(relevantRows, async (row) =>
                {
                    let spec   = Models.TimeSeriesPropertyRequest.newInstance({
                                                                                  sysId: row.id,
                                                                                  prop : DeviceElementExtended.PRESENT_VALUE
                                                                              });
                    let result = await this.app.domain.assets.getRange(spec, rangeMin, rangeMax);
                    if (result)
                    {
                        if (!sharedRange) sharedRange = new ChartValueRange();
                        sharedRange.expandToContain(new ChartValueRange(result.minValue, result.maxValue));
                    }
                });
            }
            bindingInfo.sharedRange = sharedRange;
        }
        bindingInfo.updateAxisRange(relevantRows);
        bindingInfo.rebuildColors();
    }

    private async registerRows<T extends HeatmapComponent | TimeSeriesChartComponent>(rowComponents: QueryList<T>,
                                                                                      conditionFn: (row: Row,
                                                                                                    rowComponent: T) => boolean)
    {
        if (rowComponents?.length)
        {
            let idx = -1;
            for (let rowComponent of rowComponents)
            {
                idx = this.findRowIdx(rowComponent, conditionFn, idx + 1);
                if (idx === -1) break;
                this.rows[idx].registerComponent(rowComponent);
            }

            if (!this.m_rowsInView) await this.checkViewThrottler.forceExecution();
            this.refreshSize();
        }
    }

    private findRowIdx<T>(rowComponent: T,
                          conditionFn: (row: Row,
                                        rowComponent: T) => boolean,
                          startIdx: number): number
    {
        for (let i = startIdx; i < this.rows.length; i++)
        {
            if (conditionFn(this.rows[i], rowComponent)) return i;
        }
        return -1;
    }

    rowId(index: number,
          row: Row): string
    {
        return row.id;
    }

    onHeight(height: number)
    {
        this.m_minChartHeight = height;
        this.minHeightChange.emit(this.m_minChartHeight);
        this.invokeViewDebouncer();
    }

    onWidth(width: number)
    {
        this.m_interactiveTreeWidth = width;
        this.invokeViewDebouncer();
    }

    ngAfterViewChecked()
    {
        if (this.viewChanged)
        {
            this.viewChanged = false;
            this.refreshSize();
        }
    }

    markViewChange()
    {
        this.viewChanged = true;
    }

    onMouseMove(x: number,
                y: number): boolean
    {
        x -= this.m_interactiveTreeWidth;
        y -= InteractiveTreeComponent.HEADER_HEIGHT;
        let haveRowComponents = this.m_heatmaps?.length || this.m_trendlines?.length;
        if (!haveRowComponents || x < 0 || y < 0)
        {
            this.onMouseLeave();
            return false;
        }

        let rowIdx = UtilsService.binarySearch(this.rows, y, (row) => row.rowTop);
        rowIdx     = UtilsService.clamp(0, this.rows.length - 1, rowIdx < 0 ? ~rowIdx : rowIdx);
        let row    = this.rows[rowIdx];
        if (row.rowTop > y) row = this.rows[rowIdx - 1];

        let isHeatmap = this.bindingInfoLookup[row.leafNodeId]?.isHeatmap;
        let currHover = isHeatmap ? row?.heatmap : row?.trendline;
        if (currHover !== this.m_prevHover) this.onMouseLeave();

        if (currHover)
        {
            if (isHeatmap) x -= this.timelinePadding;
            currHover.onMouseMove(x, y - row.rowTop);

            this.m_prevHover = currHover;
        }

        return !!currHover;
    }

    onMouseLeave()
    {
        if (this.m_prevHover)
        {
            this.m_prevHover.onMouseLeave();
            this.m_prevHover = null;
        }
    }

    refreshSize(): boolean
    {
        this.detectChanges();

        if (this.timeline) this.timeline.refreshSize();

        if (!this.m_rowsInView)
        {
            this.invokeViewDebouncer();
            return false;
        }

        let updated = !this.m_trendlines?.length;
        if (!updated)
        {
            for (let chart of this.m_trendlines)
            {
                if (chart.refreshSize()) updated = true;
            }
        }

        for (let heatmap of this.m_heatmaps || []) heatmap.throttledRender();

        return updated;
    }

    async handleRowClick(row: Row)
    {
        let type = this.interactionBehavior?.type;
        if (!type) return;

        let deviceElemExt = await this.app.domain.assets.getTypedExtendedById(DeviceElementExtended, row.id);
        switch (type)
        {
            case Models.InteractionBehaviorType.NavigateDeviceElem:
                await DeviceElementsDetailPageComponent.navigate(this.app, deviceElemExt);
                break;

            case Models.InteractionBehaviorType.Pane:
                if (this.paneConfig)
                {
                    let paneModels = await this.app.domain.panes.evaluate(this.paneConfig, [Models.AssetGraphContextAsset.newInstance({sysId: row.interactiveRow.pivotRow.rootId})]);
                    if (paneModels.length)
                    {
                        this.paneOverlayConfig = ContextPaneComponent.getOverlayConfig(this.app.ui.overlay);
                        this.paneModels        = paneModels;
                        this.detectChanges();
                        this.paneOverlay?.toggleOverlay();
                    }
                }
                break;
        }
    }

    toggleColorConfigurer()
    {
        this.colorDialog.toggleOverlay();
    }

    toggleConfigurer()
    {
        this.optionsDialog.toggleOverlay();
    }

    colorsChanged(changedBindingInfo: HierarchicalVisualizationBindingInfo)
    {
        changedBindingInfo.rebuildColors();

        this.updateTrendlineColors(this.rows, changedBindingInfo);

        this.bindingsChange.emit(this.m_bindings);
    }

    private updateTrendlineColors(rows: Row[],
                                  bindingInfo?: HierarchicalVisualizationBindingInfo)
    {
        let leafNodeToIdx: Lookup<number> = {};
        if (bindingInfo)
        {
            if (!bindingInfo.isTrendline) return;
            leafNodeToIdx[bindingInfo.id] = 0;
        }
        else
        {
            for (let leafNodeId in this.bindingInfoLookup)
            {
                if (this.bindingInfoLookup[leafNodeId].isTrendline) leafNodeToIdx[leafNodeId] = 0;
            }
        }

        for (let row of rows)
        {
            let idx = leafNodeToIdx[row.leafNodeId];
            if (!isNaN(idx))
            {
                let bindingInfo               = this.bindingInfoLookup[row.leafNodeId];
                row.lineColor                 = bindingInfo.colorExt.getPaletteColor(idx);
                leafNodeToIdx[row.leafNodeId] = idx + 1;
            }
        }
    }

    async optionsChanged(changedBindingInfo: HierarchicalVisualizationBindingInfo)
    {
        if (changedBindingInfo.ready)
        {
            changedBindingInfo.updateAxisRange(changedBindingInfo.relevantRows(this.rows));
            changedBindingInfo.rebuildColors();
            this.updateTrendlineColors(this.rows, changedBindingInfo);
            this.tableDisplay.updateFixedHeights(this.m_bindings);
        }
        else
        {
            await this.updateBindingInfo(this.rows, changedBindingInfo);
        }

        this.markForCheck();
        this.markViewChange();

        this.bindingsChange.emit(this.m_bindings);
    }

    rowFetched(index: number,
               row: Row)
    {
        row.fetched = true;
        this.m_fetchedRows.set(this.rowId(index, row), row);

        this.checkFetchDone();

        // confusing that this is necessary... probably has to do with time-series-chart/heatmap's stoppedFetchingData observable residing in skeleton-screen
        this.m_detectChangesDebouncer.invoke();
    }

    private updateFetchedLookup()
    {
        let fetchedRows = new Map<string, Row>();
        let numInView   = 0;
        for (let i = 0; i < this.rows.length; i++)
        {
            let row = this.rows[i];
            if (row.inView)
            {
                numInView++;

                let rowId = this.rowId(i, row);
                if (this.m_fetchedRows.has(rowId))
                {
                    fetchedRows.set(rowId, row);
                }
            }
        }
        this.m_fetchedRows = fetchedRows;
        this.m_rowsInView  = numInView;

        this.checkFetchDone();
    }

    private checkFetchDone()
    {
        if (this.m_fetchedRows.size === this.m_rowsInView)
        {
            this.stoppedFetchingData.emit();
        }
    }

    private invokeViewDebouncer()
    {
        this.markForCheck();
        this.checkViewThrottler.invoke();
    }

    private async updateRowTimeRanges(rows: Row[])
    {
        if (!rows?.length) return;

        await inParallel(rows, async (row: Row) => await row.updateSource(this.host, this.m_rangeExt));

        this.resetView();
        this.invokeViewDebouncer();
    }

    private checkView()
    {
        let rowElements = this.rowElements.toArray();

        let viewTop: number;
        let viewBottom: number;
        if (this.m_viewWindow)
        {
            viewTop    = this.m_viewWindow.viewTop - InteractiveTreeComponent.HEADER_HEIGHT;
            viewBottom = viewTop + this.m_viewWindow.viewHeight;
        }
        else
        {
            let rowsTop = this.rowsContainer.nativeElement.getBoundingClientRect().top;
            viewTop     = Math.max(0, -rowsTop);
            viewBottom  = window.innerHeight - rowsTop;
        }

        let changed = false;
        let rowTop  = 0;
        for (let i = 0; i < rowElements.length; i++)
        {
            let element = rowElements[i];
            let row     = this.rows[i];

            let nativeElement = element?.nativeElement;
            if (nativeElement)
            {
                rowTop = nativeElement.offsetTop;

                let rowBottom = rowTop + nativeElement.offsetHeight;
                let inView    = rowTop < viewBottom && rowBottom >= viewTop;

                if (inView && !row.inView)
                {
                    row.inView = true;
                    this.m_rowsInView++;
                    changed = true;
                }
            }

            row.rowTop = rowTop;
        }

        if (this.m_rowsUpdated)
        {
            this.m_rowsUpdated = false;
            this.updateFetchedLookup();
        }

        if (changed) this.detectChanges();
    }

    private resetView()
    {
        for (let row of this.rows) row.inView = false;
        this.m_rowsInView = 0;
    }

    public getCanvasPNG(): string
    {
        // todo: implement when we consolidate canvases into one
        return "";
    }

    public getCanvasTitle(): string
    {
        // todo: reconsider when we consolidated canvases into one
        return "hierarchical-visualization";
    }
}

class HierarchicalVisualizationBindingInfo
{
    get id(): string
    {
        return this.m_binding.leafNodeId;
    }

    get ready(): boolean
    {
        if (!this.colorExt) return false;
        if (this.usingSharedRange)
        {
            if (!this.sharedRange) return false;
            if (this.isHeatmap && !this.colorMapper) return false;
        }

        return true;
    }

    get isHeatmap(): boolean
    {
        return this.options.type === Models.HierarchicalVisualizationType.HEATMAP;
    }

    get isTrendline(): boolean
    {
        return this.options.type === Models.HierarchicalVisualizationType.LINE;
    }

    get usingSharedRange(): boolean
    {
        return this.isHeatmap || this.options.axisSizing === Models.HierarchicalVisualizationAxisSizing.SHARED;
    }

    get fixedRowHeight(): number
    {
        return this.options.sizing === Models.HierarchicalVisualizationSizing.FIXED ? this.options.size : null;
    }

    colorExt: ColorConfigurationExtended;
    axisRange: Models.NumericRange;
    sharedRange: ChartValueRange;
    enumRange: Models.TimeSeriesEnumeratedValue[];
    colorMapper: ColorMapper;

    get color(): Models.ColorConfiguration
    {
        return this.m_binding.color;
    }

    set color(color: Models.ColorConfiguration)
    {
        this.m_binding.color = color;
    }

    get options(): Models.HierarchicalVisualizationConfiguration
    {
        return this.m_binding.options;
    }

    set options(options: Models.HierarchicalVisualizationConfiguration)
    {
        this.m_binding.options = options;
    }

    constructor(private readonly m_binding: Models.HierarchicalVisualizationBinding)
    {
        this.colorExt = new ColorConfigurationExtended(this.color);
    }

    relevantRows(rows: Row[]): Row[]
    {
        return rows.filter((row) => row.leafNodeId === this.id);
    }

    rebuildColors()
    {
        this.colorExt = new ColorConfigurationExtended(Models.ColorConfiguration.deepClone(this.color));
        if (this.usingSharedRange && this.sharedRange)
        {
            if (this.enumRange)
            {
                this.colorMapper = StepwiseColorMapper.fromEnumsToColorMapper(this.enumRange, <PaletteId>this.colorExt.model.paletteName);
            }
            else
            {
                this.colorMapper = new ColorGradientDiscrete(this.colorExt.computeStops(this.sharedRange.min, this.sharedRange.max));
            }
        }
        else
        {
            this.colorMapper = null;
        }
    }

    updateAxisRange(rows: Row[])
    {
        if (this.isTrendline)
        {
            switch (this.options.axisSizing)
            {
                case Models.HierarchicalVisualizationAxisSizing.SHARED:
                    this.axisRange = Models.NumericRange.newInstance({
                                                                         min: this.sharedRange.min,
                                                                         max: this.sharedRange.max
                                                                     });
                    break;

                case Models.HierarchicalVisualizationAxisSizing.FIXED:
                    this.axisRange = Models.NumericRange.newInstance(this.options.axisRange);
                    break;

                default:
                    this.axisRange = null;
                    break;
            }

            for (let row of rows)
            {
                row.displayRange = this.axisRange;
                row.updateAxisRange();
            }
        }
    }
}

class Row
{
    public rowTop: number;
    public inView: boolean  = false;
    public fetched: boolean = false;

    public heatmap: HeatmapComponent;
    public trendline: TimeSeriesChartComponent;

    public heatmapSources: ChartPointSource<number>[] = [];
    private prevSourceExt: TimeSeriesSourceConfigurationExtended;
    public lineConfigExt: TimeSeriesChartConfigurationExtended;

    private m_lineColor: string;
    set lineColor(color: string)
    {
        this.m_lineColor = color;
        if (color && this.lineConfigExt)
        {
            this.lineConfigExt.dataSources[0].color = this.m_lineColor;
            if (this.trendline) this.trendline.onChange();
        }
    }

    public displayRange: Models.NumericRange;

    get styles(): Lookup<string>
    {
        return {
            "flex"  : this.interactiveRow.flexCss,
            "border": this.interactiveRow.displayContent ? null : "none"
        };
    }

    get isValid(): boolean
    {
        return !!this.lineConfigExt?.sourcesExt.length;
    }

    get id(): string
    {
        return this.interactiveRow.pivotRow.deviceElemId;
    }

    get leafNodeId(): string
    {
        return this.interactiveRow.pivotRow.leafNodeId;
    }

    constructor(public readonly interactiveRow: InteractiveTreeRow,
                prevRow?: Row)
    {
        if (prevRow && prevRow.id === this.id)
        {
            this.heatmapSources = prevRow.heatmapSources;
            this.prevSourceExt  = prevRow.prevSourceExt;
            this.lineConfigExt  = prevRow.lineConfigExt;
            this.m_lineColor    = prevRow.m_lineColor;
            this.displayRange   = prevRow.displayRange;
            this.fetched        = prevRow.fetched;
            this.inView         = prevRow.inView;
        }
    }

    async updateConfig(app: AppContext)
    {
        if (this.lineConfigExt) this.prevSourceExt = this.lineConfigExt.sourcesExt[0];

        let axis            = new Models.TimeSeriesAxisConfiguration();
        axis.displayFactors = null;
        axis.groupedFactors = [new Models.TimeSeriesAxisGroupConfiguration()];

        let panel              = new Models.TimeSeriesPanelConfiguration();
        panel.leftAxis         = axis;
        panel.rightAxis        = null;

        let source       = new Models.TimeSeriesSourceConfiguration();
        source.id        = this.id;
        source.dimension = DeviceElementExtended.PRESENT_VALUE;
        source.panel     = 0;
        source.axis      = 0;
        source.color     = this.m_lineColor;
        source.range     = null;

        let display                  = new Models.TimeSeriesDisplayConfiguration();
        display.automaticAggregation = false;
        display.hideDecimation       = true;
        display.fillArea             = false;

        let config         = new Models.TimeSeriesChartConfiguration();
        config.type        = Models.TimeSeriesChartType.STANDARD;
        config.panels      = [panel];
        config.dataSources = [source];
        config.display     = display;

        this.lineConfigExt = await TimeSeriesChartConfigurationExtended.newInstance(app, config);
        this.updateAxisRange();

        if (this.isValid)
        {
            this.lineConfigExt.sourcesExt[0].includeAlerts = false;
            for (let panelExt of this.lineConfigExt.panelsExt) panelExt.leftAxisExtended.hideLabel = true;
        }
    }

    updateAxisRange()
    {
        let toggleableRange = ToggleableNumericRangeExtended.toToggleable(this.displayRange);
        if (ToggleableNumericRangeExtended.isActive(toggleableRange))
        {
            let diff    = toggleableRange.max - toggleableRange.min;
            let padding = diff > 0 ? diff * 0.025 : 1; // 2.5% padding or just 1px in case of no diff
            toggleableRange.min -= padding;
            toggleableRange.max += padding;
        }
        else
        {
            toggleableRange = null;
        }

        if (this.lineConfigExt)
        {
            this.lineConfigExt.model.dataSources[0].range = toggleableRange;
            if (this.trendline) this.trendline.onChange();
        }
    }

    async updateSource(host: TimeSeriesSourceHost,
                       rangeExt: RangeSelectionExtended)
    {
        if (this.isValid)
        {
            let sourceExt = this.lineConfigExt.sourcesExt[0];
            if (this.prevSourceExt)
            {
                sourceExt.attemptAdoptCachedData(this.prevSourceExt);
                this.prevSourceExt = null;
            }

            await sourceExt.fetch(host, rangeExt);
            this.heatmapSources = [sourceExt.getChartData()];
        }
    }

    registerComponent(component: EventReceiver)
    {
        if (component instanceof HeatmapComponent)
        {
            this.heatmap = component;
        }
        else if (component instanceof TimeSeriesChartComponent)
        {
            this.trendline = component;
        }
    }
}
