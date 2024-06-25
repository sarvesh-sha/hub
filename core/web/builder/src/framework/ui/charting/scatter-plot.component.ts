import {Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";

import {UtilsService} from "framework/services/utils.service";
import {CanvasRenderer, CanvasZoneSelectionType, CanvasZoneSelector, ChartHelpers, ChartTickInfo, TooltipPoint, TooltipPointForSample} from "framework/ui/charting/app-charting-utilities";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {Vector2} from "framework/ui/charting/charting-math";
import {BoxAnchor, ChartClipArea, ChartPointStyle, ChartValueRange} from "framework/ui/charting/core/basics";
import {ColorGradientStop} from "framework/ui/charting/core/colors";
import {ChartPixelWithContext, ChartPointRange, ChartPointSubscriber, ChartPointType, DataSourceTuple, isVisible, ProcessedChartValueRange, ProcessedZoomState, ScatterPlotPoint, ScatterPlotPropertyTuple, ScatterPlotSubSource, ScatterPlotTransformer, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {BaseComponent} from "framework/ui/components";
import {RelativeLocation} from "framework/ui/utils/relative-location-styles";

import {Subject} from "rxjs";
import {debounceTime} from "rxjs/operators";

@Component({
               selector   : "o3-scatter-plot",
               templateUrl: "./scatter-plot.component.html",
               styleUrls  : ["./scatter-plot.component.scss"]
           })
export class ScatterPlotComponent extends BaseComponent
{
    private rendered: boolean = false;

    private debouncingWidth: number  = 1;
    private debouncingHeight: number = 1;

    private computedPanels: ProcessedPanel[] = [new ProcessedPanel(this, 0, [], null)];

    get test_panels(): ProcessedPanel[]
    {
        return this.computedPanels;
    }

    private redrawDebouncer = new Subject<void>();

    private m_panels: ScatterPlotInputPanel[] = [];
    @Input() set panels(panels: ScatterPlotInputPanel[])
    {
        this.m_panels = panels || [];
        this.reportConfigurationChanges();
    }

    @Input() allowZoom: boolean = true;

    @ViewChild("chart", {static: true}) canvasRef: ElementRef;
    @ViewChild("tooltip", {static: true}) public tooltip: ChartTooltipComponent;

    @Output() startedFetchingData = new EventEmitter<void>();
    @Output() stoppedFetchingData = new EventEmitter<void>();
    @Output() editGradient        = new EventEmitter<number>();
    @Output() sourceStateUpdated  = new EventEmitter<void>();

    numSampling: number = 0;

    private containerElement: HTMLElement;
    private chartElement: HTMLCanvasElement;

    constructor(inj: Injector)
    {
        super(inj);

        this.subscribeToObservable(this.redrawDebouncer.pipe(debounceTime(10)), () => this.redrawCanvas());
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.chartElement     = this.canvasRef.nativeElement;
        this.containerElement = this.chartElement.parentElement;

        this.chartElement.addEventListener("mousemove", (e: MouseEvent) => this.handleEvent("mousemove", e));
        this.chartElement.addEventListener("mouseleave", (e: MouseEvent) => this.handleEvent("mouseleave", e));
        this.chartElement.addEventListener("dblclick", (e: MouseEvent) => this.handleEvent("dblclick", e));
        this.chartElement.addEventListener("wheel", (e: WheelEvent) => this.handleEvent("wheel", e));
        this.chartElement.addEventListener("click", (e: MouseEvent) => this.handleEvent("click", e));

        this.subscribeToMouseDrag(this.chartElement,
                                  (e,
                                   mouseDown,
                                   mouseUp) => this.handleDrag(e, mouseDown, mouseUp));

        this.refreshSize();
    }

    public onChange(): void
    {
        this.reportConfigurationChanges();
    }

    protected afterConfigurationChanges(): void
    {
        super.afterConfigurationChanges();

        this.rendered = false;

        if (!this.containerElement || !this.chartElement) return;

        if (this.debouncingWidth <= 1 || this.debouncingHeight <= 1)
        {
            this.refreshSize();
            return;
        }

        ChartHelpers.scaleCanvas(this.chartElement, this.debouncingWidth, this.debouncingHeight);
        this.computePanels();
        this.preparePanelLayout(0, 0, this.chartElement.clientWidth, this.chartElement.clientHeight);

        this.redrawCanvas();
    }

    public toggleTarget(panelIdx: number,
                        tuple: DataSourceTuple,
                        fromMouseover: boolean,
                        redraw: boolean = true)
    {
        if (!tuple || tuple.state === VisualizationDataSourceState.Deleted) return;

        this.computedPanels[panelIdx].toggleTarget(tuple, fromMouseover);
        if (redraw) this.redrawCanvas();
    }

    public toggleEnabled(panelIdx: number,
                         tuple: DataSourceTuple)
    {
        switch (tuple?.state)
        {
            case VisualizationDataSourceState.Target:
                this.toggleTarget(panelIdx, tuple, false, false);
            // fall through
            case VisualizationDataSourceState.Active:
            case VisualizationDataSourceState.Muted:
                tuple.state = VisualizationDataSourceState.Disabled;
                break;

            case VisualizationDataSourceState.Disabled:
                let inTargetView = this.computedPanels[panelIdx].sourceTuples.some((curr) =>
                                                                                   {
                                                                                       let state = curr.sourceTuple.state;
                                                                                       return state === VisualizationDataSourceState.Muted || state === VisualizationDataSourceState.Target;
                                                                                   });
                tuple.state      = inTargetView ? VisualizationDataSourceState.Muted : VisualizationDataSourceState.Active;
                break;
        }

        this.sourceStateUpdated.emit();
        this.redrawCanvas();
    }

    public multiToggleEnabled(panelIdx: number,
                              tuple: DataSourceTuple)
    {
        if (tuple)
        {
            this.computedPanels[panelIdx].multiToggleEnabled(tuple);
            this.sourceStateUpdated.emit();
            this.redrawCanvas();
        }
    }

    public triggerColorPicker(panelIdx: number)
    {
        this.editGradient.emit(panelIdx);
    }

    private incrementActive()
    {
        if (this.numSampling++ == 0)
        {
            this.startedFetchingData.emit();
        }
    }

    private decrementActive()
    {
        if (--this.numSampling == 0)
        {
            this.stoppedFetchingData.emit();
            this.queueRedraw();
        }
    }

    private flushCache()
    {
        if (this.computedPanels)
        {
            for (let panel of this.computedPanels)
            {
                panel.flushCache();
            }

            this.queueRedraw();
        }
    }

    private computePanels()
    {
        if (this.computedPanels)
        {
            for (let panel of this.computedPanels)
            {
                for (let tuple of panel.sourceTuples)
                {
                    tuple.stopStreamingSamples();
                }
            }
        }

        this.computedPanels = this.m_panels.map(
            (panel,
             panelNum) =>
            {
                let processedTuples = panel?.sourceTuples.map(
                    (tuple) => new ProcessedScatterPlotSource(tuple,
                                                              {
                                                                  transitionToActive  : () => this.incrementActive(),
                                                                  transitionToInactive: () => this.decrementActive(),
                                                                  newSamples          : () => this.flushCache()
                                                              })) || [];
                return new ProcessedPanel(this, panelNum, processedTuples, panel);
            });
    }

    // todo: optimization: introduce redraw vs rerender - not always necessary to recalculate everything as is done whenever a redraw is called
    private redrawCanvas()
    {
        let width  = this.chartElement.clientWidth;
        let height = this.chartElement.clientHeight;

        if (!width || !height)
        {
            this.refreshSize();
            return;
        }

        this.rendered = false;

        this.updateRanges();

        let helpers = new ChartHelpers(this.chartElement.getContext("2d"));
        this.render(helpers, width, height);
    }

    private handleDrag(e: MouseEvent,
                       mouseDown: boolean,
                       mouseUp: boolean)
    {
        if (!this.rendered || e.target !== this.chartElement || !this.allowZoom) return;

        let redraw = false;

        for (let panel of this.computedPanels)
        {
            if (panel.handleDrag(e, mouseDown, mouseUp)) redraw = true;
        }

        if (redraw) this.redrawCanvas();
    }

    private handleEvent<K extends keyof WindowEventMap>(type: K,
                                                        ev: WindowEventMap[K])
    {
        if (!this.rendered) return;

        let redraw = false;

        if (this.computedPanels)
        {
            for (let panel of this.computedPanels)
            {
                if (panel.handleEvent(type, ev)) redraw = true;
            }
        }

        if (redraw) this.redrawCanvas();
    }

    public queueRedraw()
    {
        this.redrawDebouncer.next();
    }

    public getCanvasPNG(): string
    {
        return this.chartElement && ChartHelpers.getCanvasPNG(this.chartElement);
    }

    public refreshSize(): boolean
    {
        if (!this.containerElement) return false;

        let width  = this.containerElement.offsetWidth;
        let height = this.containerElement.offsetHeight;
        if (this.debouncingWidth != width || this.debouncingHeight != height)
        {
            this.debouncingWidth  = width;
            this.debouncingHeight = height;

            this.reportConfigurationChanges();
        }

        return this.computedPanels && !!this.debouncingWidth && !!this.debouncingHeight;
    }

    private render(helpers: ChartHelpers,
                   width: number,
                   height: number)
    {
        helpers.canvas.clearRect(0, 0, width, height);

        for (let panel of this.computedPanels)
        {
            if (panel.readyToRender)
            {
                panel.render(helpers, (html) => this.sanitizeHtml(html));
                panel.rendered = true;
            }
            else
            {
                panel.renderErrorMessage(helpers);
            }
        }

        this.rendered = true;
    }

    private updateRanges()
    {
        for (let panel of this.computedPanels)
        {
            for (let sourceTuple of panel.sourceTuples) sourceTuple.updateValueRanges();

            panel.updateRanges();
        }
    }

    private preparePanelLayout(top: number,
                               left: number,
                               width: number,
                               height: number)
    {
        let panelHeight = height / this.computedPanels.length;
        for (let i = 0; i < this.computedPanels.length; i++)
        {
            this.computedPanels[i].prepareLayout(top + panelHeight * i, left, width, panelHeight);
        }
    }
}

const notTargetColor: string    = "#d8d8d8";
const baseTickSpacingPx: number = 40;
const axisSize: number          = 30;
const axisLabelPadding: number  = 8.5;
const gradientWidth: number     = 200;

export class ScatterPlotAxisLabel
{
    private m_deviceElemName: string;
    set deviceElemName(name: string)
    {
        if (name)
        {
            if (this.m_deviceElemName == null)
            {
                this.m_deviceElemName = name;
            }
            else if (this.m_deviceElemName != name)
            {
                this.m_deviceElemName = "";
            }
        }
    }

    private m_deviceElemLocation: string;
    set deviceElemLocation(name: string)
    {
        if (name)
        {
            if (this.m_deviceElemLocation == null)
            {
                this.m_deviceElemLocation = name;
            }
            else if (this.m_deviceElemLocation != name)
            {
                this.m_deviceElemLocation = "";
            }
        }
    }

    unitsDisplay: string;

    labelGenerator: (label?: string) => string;

    clipArea = ChartClipArea.EmptyPlaceholder;
    tooltip: string;

    private get interestingLabel(): string
    {
        return this.labelOverride || this.m_deviceElemName;
    }

    constructor(private readonly labelOverride: string)
    {
    }

    public getLabel(measureText: (text: string) => number,
                    numPx: number)
    {
        let generatedLabel;

        if (this.interestingLabel)
        {
            if (this.m_deviceElemLocation)
            {
                generatedLabel = this.labelGenerator(this.interestingLabel + " - " + this.m_deviceElemLocation);
                if (measureText(generatedLabel) <= numPx)
                {
                    this.tooltip = "";
                    return generatedLabel;
                }
            }

            generatedLabel = this.labelGenerator(this.interestingLabel);
            if (measureText(generatedLabel) <= numPx)
            {
                this.tooltip = "";
                return generatedLabel;
            }

            this.tooltip = this.m_deviceElemLocation ? this.labelGenerator(this.interestingLabel + " - " + this.m_deviceElemLocation) : generatedLabel;
        }

        return this.unitsDisplay || "";
    }
}

export class ScatterPlotAxis
{
    private m_valueRange: ProcessedChartValueRange;

    get valueRange(): ProcessedChartValueRange
    {
        return this.m_valueRange;
    }

    set valueRange(valueRange: ProcessedChartValueRange)
    {
        if (this.override)
        {
            if (!isNaN(this.override.min)) valueRange.range.min = this.override.min;
            if (!isNaN(this.override.max)) valueRange.range.max = this.override.max;
        }
        this.m_valueRange = valueRange;
    }

    tickValues: number[];
    zoomViewTickValues: number[];

    constructor(public label: ScatterPlotAxisLabel,
                public override?: ChartValueRange)
    {
        if (this.override)
        {
            this.m_valueRange       = new ProcessedChartValueRange();
            this.m_valueRange.range = this.override;
        }
    }

    public update(subSource: ScatterPlotSubSource)
    {
        this.label.labelGenerator     = subSource.labelGenerator;
        this.label.deviceElemName     = subSource.name;
        this.label.deviceElemLocation = subSource.location;
        this.label.unitsDisplay       = subSource.unitsDisplay;
    }
}

export class ScatterPlotInputPanel
{
    private m_stops: ColorGradientStop[];

    set gradientStops(stops: ColorGradientStop[])
    {
        this.m_stops = stops;
    }

    get gradientStops(): ColorGradientStop[]
    {
        return this.m_stops?.map((stop) => stop.clone()) || [];
    }

    constructor(public sourceTuples: DataSourceTuple[],
                public xAxis: ScatterPlotAxis,
                public yAxis: ScatterPlotAxis,
                public zAxis?: ScatterPlotAxis)
    {
    }
}

class ProcessedPanel
{
    public static readonly minHighlightDistance: number = 24;

    private static readonly factorOfDataAreaForPoints = 0.03;

    rendered: boolean      = false;
    readyToRender: boolean = false;

    private clipForMainArea         = ChartClipArea.EmptyPlaceholder;
    private clipForDataArea         = ChartClipArea.EmptyPlaceholder;
    private areaUtilizedForGradient = ChartClipArea.EmptyPlaceholder;

    private xAxis: ScatterPlotAxis;
    private yAxis: ScatterPlotAxis;
    private zAxis: ScatterPlotAxis;

    private readonly zoomState    = new PanelZoomState();
    private readonly zoomSelector = new CanvasZoneSelector();

    private transformer = new ScatterPlotTransformer(ChartClipArea.EmptyPlaceholder, ProcessedZoomState.EmptyPlaceholder);

    private pointRadius: number;

    private mouseoverSource: ProcessedScatterPlotSource;

    private axisWithTooltip: ScatterPlotAxisLabel;
    private tooltipPoint: ScatterPlotPointTooltip = null;

    targetSource: DataSourceTuple;

    constructor(private owner: ScatterPlotComponent,
                public readonly idx: number,
                public sourceTuples: ProcessedScatterPlotSource[],
                public readonly rawPanel: ScatterPlotInputPanel)
    {
        if (this.rawPanel)
        {
            this.xAxis = rawPanel.xAxis;
            this.yAxis = rawPanel.yAxis;
            this.zAxis = rawPanel.zAxis;
            let withZ  = !!this.zAxis;

            for (let tuple of sourceTuples)
            {
                this.xAxis.update(tuple.sourceTuple.subSources.valueX);
                this.yAxis.update(tuple.sourceTuple.subSources.valueY);
                if (withZ) this.zAxis.update(tuple.sourceTuple.subSources.valueZ);
            }
        }
    }

    private getErrorMessage()
    {
        if (!this.rawPanel) return "No valid tuples available";
        if (this.owner.numSampling > 0) return "Loading...";

        return "No points available";
    }

    public prepareLayout(top: number,
                         left: number,
                         width: number,
                         height: number)
    {
        this.clipForMainArea = new ChartClipArea(left, top, width, height);
        this.clipForDataArea = new ChartClipArea(left + axisSize, top, width - axisSize, height - axisSize);

        if (this.rawPanel)
        {
            this.xAxis.label.clipArea = new ChartClipArea(left, top + height - axisSize, width - axisSize, axisSize);
            this.yAxis.label.clipArea = new ChartClipArea(left, top, axisSize, height - axisSize);

            this.zoomSelector.prepareLayout(this.clipForDataArea, CanvasZoneSelectionType.Area);
        }
    }

    public updateRanges()
    {
        if (!this.firstValidTuple()) return;

        let updated = this.updateValueRanges();
        if (updated)
        {
            this.updateTransformer();

            this.updateTickValues();

            this.readyToRender = true;
        }
    }

    public toggleTarget(source: DataSourceTuple,
                        fromMouseover: boolean)
    {
        if (fromMouseover)
        {
            if (source.state === VisualizationDataSourceState.Target) source = null;
            this.setMouseoverSource(this.getProcessedSource(source));
        }
        else
        {
            this.clearMouseoverSource();
            if (!source || this.targetSource === source)
            {
                this.targetSource = null;
                this.setSourcesState(VisualizationDataSourceState.Active);
            }
            else
            {
                this.targetSource = source;

                this.setSourcesState(VisualizationDataSourceState.Muted);
                this.targetSource.state = VisualizationDataSourceState.Target;
            }
        }

        this.owner.sourceStateUpdated.emit();
    }

    public multiToggleEnabled(tuple: DataSourceTuple)
    {
        let otherVisibleTuples = this.sourceTuples.some((curr) => curr.sourceTuple !== tuple && isVisible(curr.sourceTuple.state));
        if (otherVisibleTuples)
        {
            this.setSourcesState(VisualizationDataSourceState.Disabled);
            tuple.state = VisualizationDataSourceState.Active;
        }
        else
        {
            this.setSourcesState(VisualizationDataSourceState.Active, true);
        }
    }

    private setSourcesState(state: VisualizationDataSourceState,
                            override: boolean = false)
    {
        for (let tuple of this.sourceTuples)
        {
            if (override || tuple.sourceTuple.state !== VisualizationDataSourceState.Disabled) tuple.sourceTuple.state = state;
        }
    }

    private getProcessedSource(tuple: DataSourceTuple): ProcessedScatterPlotSource
    {
        return this.sourceTuples?.find((processedSource) => processedSource.sourceTuple === tuple);
    }

    private firstValidTuple(): ProcessedScatterPlotSource
    {
        for (let tuple of this.sourceTuples)
        {
            if (tuple.valueRanges.valueX) return tuple;
        }

        return null;
    }

    private updateValueRanges(): boolean
    {
        let firstTuple  = this.firstValidTuple();
        let valueRangeX = firstTuple.valueRanges.valueX.deepCopy();
        let valueRangeY = firstTuple.valueRanges.valueY.deepCopy();
        let valueRangeZ = firstTuple.valueRanges.valueZ?.deepCopy();

        let xUpdated: boolean;
        let yUpdated: boolean;
        let zUpdated: boolean;
        for (let sourceTuple of this.sourceTuples || [])
        {
            if (sourceTuple.valueRanges.valueX)
            {
                if (!valueRangeX.enumRange) xUpdated = valueRangeX.expandRange(sourceTuple.valueRanges.valueX.range) || !!xUpdated;
                if (!valueRangeY.enumRange) yUpdated = valueRangeY.expandRange(sourceTuple.valueRanges.valueY.range) || !!yUpdated;
                if (valueRangeZ && !valueRangeZ.enumRange) zUpdated = valueRangeZ.expandRange(sourceTuple.valueRanges.valueZ.range) || !!zUpdated;
            }
        }

        if (!(xUpdated || valueRangeX.enumRange) && !(yUpdated || valueRangeY.enumRange))
        {
            if (!valueRangeZ || !(zUpdated || valueRangeZ.enumRange)) return false;
        }

        this.xAxis.valueRange = valueRangeX;
        this.yAxis.valueRange = valueRangeY;
        if (this.zAxis) this.zAxis.valueRange = valueRangeZ;

        let xValueRange = this.xAxis.valueRange;
        let yValueRange = this.yAxis.valueRange;

        let tickInfo = ChartHelpers.getMajorTickInfo(this.clipForDataArea.width, 2 * baseTickSpacingPx, xValueRange.range, !!xValueRange.enumRange);
        ProcessedPanel.addTicks(this.xAxis.tickValues = [], tickInfo);

        tickInfo = ChartHelpers.getMajorTickInfo(this.clipForDataArea.height, baseTickSpacingPx, yValueRange.range, !!yValueRange.enumRange);
        ProcessedPanel.addTicks(this.yAxis.tickValues = [], tickInfo);

        return true;
    }

    private updateTransformer()
    {
        let xTicks        = this.xAxis.tickValues;
        let yTicks        = this.yAxis.tickValues;
        let processedZoom = this.zoomState.getProcessedZoomState(xTicks[0],
                                                                 xTicks[xTicks.length - 1],
                                                                 yTicks[0],
                                                                 yTicks[yTicks.length - 1]);

        this.transformer = new ScatterPlotTransformer(this.clipForDataArea, processedZoom, this.zAxis && this.zAxis.valueRange);
    }

    private updateTickValues()
    {
        if (this.zoomState.isZoomed())
        {
            let zoomRangeSource = this.transformer.processedZoom;

            let xZoomedChartValueRange = new ChartValueRange();
            xZoomedChartValueRange.expandForValue(zoomRangeSource.leftValue);
            xZoomedChartValueRange.expandForValue(zoomRangeSource.rightValue);
            let withEnum = !!this.xAxis.valueRange.enumRange;

            let tickInfo = ChartHelpers.getMajorTickInfo(this.clipForDataArea.width, (withEnum ? 4 : 2) * baseTickSpacingPx, xZoomedChartValueRange, withEnum);
            ProcessedPanel.addTicks(this.xAxis.zoomViewTickValues = [], tickInfo);

            let yZoomedChartValueRange = new ChartValueRange();
            yZoomedChartValueRange.expandForValue(zoomRangeSource.topValue);
            yZoomedChartValueRange.expandForValue(zoomRangeSource.bottomValue);
            withEnum = !!this.yAxis.valueRange.enumRange;

            tickInfo = ChartHelpers.getMajorTickInfo(this.clipForDataArea.height, baseTickSpacingPx, yZoomedChartValueRange, withEnum);
            ProcessedPanel.addTicks(this.yAxis.zoomViewTickValues = [], tickInfo);
        }
        else
        {
            this.xAxis.zoomViewTickValues = this.yAxis.zoomViewTickValues = null;
        }
    }

    private static addTicks(tickArr: number[],
                            tickInfo: ChartTickInfo)
    {
        tickArr.push(...tickInfo.generateArray());
    }

    public handleDrag(e: MouseEvent,
                      mouseDown: boolean,
                      mouseUp: boolean): boolean
    {
        if (!this.rendered) return false;

        if (!this.zoomState.atMaxZoom)
        {
            let selectionExecuted = this.zoomSelector.handleSelection(e, mouseDown, mouseUp);
            if (selectionExecuted)
            {
                let selection = this.zoomSelector.selection;
                this.zoomState.zoomToRectangle((selection.xCoord1 - this.clipForDataArea.x) / this.clipForDataArea.width,
                                               (selection.xCoord2 - this.clipForDataArea.x) / this.clipForDataArea.width,
                                               1 - (selection.yCoord1 - this.clipForDataArea.y) / this.clipForDataArea.height,
                                               1 - (selection.yCoord2 - this.clipForDataArea.y) / this.clipForDataArea.height);
                this.zoomSelector.clearSelection();
            }

            return !!this.zoomSelector.selection || selectionExecuted;
        }

        return false;
    }

    public handleEvent<K extends keyof WindowEventMap>(type: K,
                                                       e: WindowEventMap[K]): boolean
    {
        if (!this.readyToRender) return false;

        let redraw = false;

        switch (type)
        {
            case "mouseleave":
                if (this.zoomSelector.clearSelection()) redraw = true;

                if (this.mouseoverSource != null)
                {
                    redraw = true;
                    this.clearMouseoverSource();
                }
                break;

            case "mousemove":
                if (this.zoomSelector.selection) break;

                let moveEvent = <MouseEvent>e;
                let mouseX    = moveEvent.offsetX;
                let mouseY    = moveEvent.offsetY;

                let targetAxis: ScatterPlotAxisLabel = null;
                let xLabel                           = this.xAxis.label;
                let yLabel                           = this.yAxis.label;
                if (xLabel.clipArea.hitCheck(mouseX, mouseY))
                {
                    targetAxis = xLabel;
                }
                else if (yLabel.clipArea.hitCheck(mouseX, mouseY))
                {
                    targetAxis = yLabel;
                }
                else
                {
                    if (this.zAxis && this.zAxis.label.clipArea.hitCheck(mouseX, mouseY)) targetAxis = this.zAxis.label;
                }

                if (!targetAxis)
                {
                    if (this.axisWithTooltip)
                    {
                        this.owner.tooltip.remove();
                        this.axisWithTooltip = null;
                    }
                }
                else if (this.axisWithTooltip !== targetAxis)
                {
                    if (targetAxis && targetAxis.tooltip)
                    {
                        this.clearMouseoverSource();

                        this.owner.tooltip.render(mouseX, mouseY, undefined, targetAxis.tooltip);
                        this.axisWithTooltip = targetAxis;
                    }
                    else
                    {
                        this.axisWithTooltip = null;
                    }
                }

                if (!this.axisWithTooltip)
                {
                    let closestPoint = this.findClosestPoint(mouseX, mouseY, ProcessedPanel.minHighlightDistance);
                    if (!this.mouseoverSource && closestPoint || this.mouseoverSource && this.mouseoverSource.mouseoverPoint !== closestPoint)
                    {
                        if (closestPoint)
                        {
                            let owningSource            = this.getProcessedSource(<DataSourceTuple>closestPoint.owningSource);
                            owningSource.mouseoverPoint = closestPoint;
                            this.setMouseoverSource(owningSource);

                            this.setTooltip(new ScatterPlotPointTooltip(this.transformer, closestPoint, closestPoint.toPixel(this.transformer)));
                        }
                        else
                        {
                            this.clearMouseoverSource();
                        }

                        redraw = true;
                    }
                }

                break;

            case "click":
                let clickEvent = <MouseEvent>e;
                if (this.areaUtilizedForGradient.hitCheck(clickEvent.offsetX, clickEvent.offsetY)) this.owner.triggerColorPicker(this.idx);
                break;

            case "wheel":
                if (!this.owner.allowZoom) break;

                let wheelEvent = <WheelEvent>e;
                let x          = wheelEvent.offsetX;
                let y          = wheelEvent.offsetY;

                if (this.clipForDataArea.hitCheck(x, y))
                {
                    // if a point is selected, zoom in to it
                    let closestPoint = this.findClosestPoint(x, y, ProcessedPanel.minHighlightDistance);
                    let scrollCenter: Vector2;
                    if (closestPoint)
                    {
                        let postMappingX = closestPoint.getNumericValue((<DataSourceTuple>closestPoint.owningSource).dataSources.valueX, closestPoint.value.valueX);
                        let postMappingY = closestPoint.getNumericValue((<DataSourceTuple>closestPoint.owningSource).dataSources.valueY, closestPoint.value.valueY);
                        scrollCenter     = new Vector2(postMappingX, postMappingY);
                    }
                    else
                    {
                        scrollCenter = this.transformer.getInvertedProjection(x, y);
                    }

                    if (scrollCenter)
                    {
                        const scale = 10;
                        const ratio = 1 + scale / 100;

                        let detail     = wheelEvent.detail || 0;
                        let wheelDelta = wheelEvent.deltaY || 0;
                        let movement: number;

                        // Deal with the differences between browsers.
                        if (detail != 0)
                        {
                            movement = -120 * detail;
                        }
                        else
                        {
                            movement = -wheelDelta || 0;
                        }

                        if (movement > 0 && this.zoomState.atMaxZoom || movement < 0 && !this.zoomState.isZoomed()) break;

                        let multiplier = movement > 0 ? (1 / ratio) : ratio;

                        let zoomView = this.transformer.processedZoom;

                        let valuePointXFactor = (scrollCenter.x - zoomView.leftValue) / (zoomView.rightValue - zoomView.leftValue);
                        let valuePointYFactor = (scrollCenter.y - zoomView.topValue) / (zoomView.bottomValue - zoomView.topValue);

                        this.zoomState.zoomAroundPoint(valuePointXFactor, valuePointYFactor, multiplier);
                        redraw = true;
                    }

                    e.preventDefault();
                    e.stopPropagation();
                }
                break;

            case "dblclick":
                let doubleClickEvent = <MouseEvent>e;
                if (this.transformer.clipForDataArea.hitCheck(doubleClickEvent.offsetX, doubleClickEvent.offsetY))
                {
                    redraw = true;
                    this.setTooltip(null);
                    this.zoomState.reset();
                }
                break;
        }

        return redraw;
    }

    private setMouseoverSource(source: ProcessedScatterPlotSource)
    {
        if (this.mouseoverSource != source)
        {
            if (!this.targetSource)
            {
                if (source)
                {
                    this.setSourcesState(VisualizationDataSourceState.Muted);
                    source.sourceTuple.state = VisualizationDataSourceState.Target;
                }
                else
                {
                    let hasTargetSource = this.sourceTuples.some((processedTuple) =>
                                                                 {
                                                                     let state = processedTuple.sourceTuple.state;
                                                                     return state == VisualizationDataSourceState.Target || state == VisualizationDataSourceState.Muted;
                                                                 });
                    if (hasTargetSource)
                    {
                        this.mouseoverSource.mouseoverPoint = null;
                        this.setSourcesState(VisualizationDataSourceState.Active);
                    }
                }

                this.owner.sourceStateUpdated.emit();
            }

            this.mouseoverSource = source;
        }
    }

    public clearMouseoverSource(clearTooltip: boolean = true)
    {
        if (this.mouseoverSource) this.setMouseoverSource(null);
        if (clearTooltip) this.setTooltip(null);
    }

    public flushCache()
    {
        this.transformer.flushCache();
    }

    public render(helpers: ChartHelpers,
                  sanitizer: (html: string) => SafeHtml)
    {
        this.renderPoints(helpers);

        this.renderGrid(helpers.canvas);

        this.renderAxes(helpers);

        this.renderZoomSelection(helpers);

        this.renderTooltip(helpers, sanitizer);
    }

    public renderErrorMessage(helpers: ChartHelpers)
    {
        this.renderWatermarkMessage(helpers, this.getErrorMessage());
    }

    private renderWatermarkMessage(helpers: ChartHelpers,
                                   message: string)
    {
        this.clipForMainArea.applyClipping(helpers.canvas, () =>
        {
            let font                   = new ChartFont();
            helpers.canvas.globalAlpha = 0.5;
            helpers.drawTextInBox(font,
                                  TextPlacement.Center,
                                  TextOrientation.Horizontal,
                                  message || "Error",
                                  font.style,
                                  this.clipForDataArea.x + this.clipForDataArea.width / 2,
                                  this.clipForDataArea.y + 3 * font.size,
                                  BoxAnchor.Center,
                                  axisLabelPadding);

        });
    }

    private renderPoints(helpers: ChartHelpers)
    {
        this.transformer.buildColorGenerator(this.rawPanel.gradientStops);

        this.clipForDataArea.applyClipping(helpers.canvas, () =>
        {
            let numPixelsRendered = 0;

            for (let sourceTuple of this.sourceTuples)
            {
                for (let range of sourceTuple.sourceTuple.ranges)
                {
                    let pixels = sourceTuple.getVisiblePixels(this.transformer, range);
                    numPixelsRendered += pixels.length;
                }
            }

            let dataArea = this.clipForDataArea.width * this.clipForDataArea.height;

            let calculatedRadius = Math.sqrt(dataArea * ProcessedPanel.factorOfDataAreaForPoints / (Math.PI * numPixelsRendered));
            this.pointRadius     = UtilsService.clamp(3, 10, calculatedRadius);

            let targetSource = this.sourceTuples.find((tuple) => tuple.sourceTuple.state === VisualizationDataSourceState.Target) || this.mouseoverSource;
            for (let sourceTuple of this.sourceTuples)
            {
                if (sourceTuple !== targetSource) sourceTuple.render(helpers, this.transformer, this.pointRadius);
            }
            if (targetSource) targetSource.render(helpers, this.transformer, this.pointRadius);
        });
    }

    private renderGrid(canvas: CanvasRenderingContext2D)
    {
        CanvasRenderer.renderGridOutline(canvas, this.clipForDataArea);

        this.clipForDataArea.applyClipping(canvas, () => CanvasRenderer.renderGrid(canvas,
                                                                                   this.transformer,
                                                                                   this.clipForDataArea.x,
                                                                                   this.clipForDataArea.right,
                                                                                   this.clipForDataArea.y,
                                                                                   this.clipForDataArea.bottom,
                                                                                   this.xAxis.zoomViewTickValues || this.xAxis.tickValues,
                                                                                   this.yAxis.zoomViewTickValues || this.yAxis.tickValues));
    }

    private renderAxes(helpers: ChartHelpers)
    {
        let font        = new ChartFont("#000000", undefined, this.clipForMainArea.bottom - this.clipForDataArea.bottom - axisLabelPadding * 2);
        let measureText = (text: string) => helpers.measureText(font, text);

        let xTicks = this.xAxis.zoomViewTickValues || this.xAxis.tickValues;
        CanvasRenderer.renderTicks(helpers, this.transformer, xTicks, this.xAxis.valueRange.enumRange, undefined, this.clipForMainArea, this.clipForDataArea, RelativeLocation.Bottom, false);

        let xLabel = this.xAxis.label;
        xLabel.clipArea.applyClipping(helpers.canvas, () =>
        {
            CanvasRenderer.renderAxisLabel(helpers,
                                           xLabel.getLabel(measureText, this.clipForDataArea.right - this.clipForDataArea.x),
                                           null,
                                           this.clipForMainArea,
                                           this.clipForDataArea,
                                           axisLabelPadding,
                                           RelativeLocation.Bottom);
        });


        font.size  = this.clipForDataArea.x - this.clipForMainArea.x - axisLabelPadding * 2;
        let yTicks = this.yAxis.zoomViewTickValues || this.yAxis.tickValues;
        CanvasRenderer.renderTicks(helpers, this.transformer, yTicks, this.yAxis.valueRange.enumRange, undefined, this.clipForMainArea, this.clipForDataArea, RelativeLocation.Left, false);
        let yLabel = this.yAxis.label;
        yLabel.clipArea.applyClipping(helpers.canvas, () =>
        {
            CanvasRenderer.renderAxisLabel(helpers,
                                           yLabel.getLabel(measureText, this.clipForDataArea.bottom - this.clipForDataArea.y),
                                           null,
                                           this.clipForMainArea,
                                           this.clipForDataArea,
                                           axisLabelPadding,
                                           RelativeLocation.Left);
        });

        this.clipForDataArea.applyClipping(helpers.canvas, () => this.renderGradient(helpers));
    }

    private renderZoomSelection(helpers: ChartHelpers)
    {
        let selection = this.zoomSelector.selection;
        if (selection) CanvasZoneSelector.renderSelection(helpers.canvas, this.clipForDataArea, selection);
    }

    private renderGradient(helpers: ChartHelpers)
    {
        if (!this.zAxis)
        {
            this.areaUtilizedForGradient = ChartClipArea.EmptyPlaceholder;
            return;
        }

        if (!this.transformer.colorGenerator) return;

        const distFromRight = 40;
        let font            = new ChartFont("#000000", undefined, 11);

        let zRange    = this.zAxis.valueRange.range;
        let magnitude = Math.log10(zRange.diff || 1);
        let numDigits = 0;
        if (magnitude < 1) numDigits = magnitude <= 0.5 ? Math.abs(Math.round(magnitude)) + 2 : 1;

        let min             = this.transformer.colorGenerator.min;
        let max             = this.transformer.colorGenerator.max;
        let minText: string = "" + UtilsService.getRoundedValue(min, numDigits);
        let maxText: string = "" + UtilsService.getRoundedValue(max, numDigits);
        if (max < min)
        {
            if (max < this.zAxis.valueRange.range.min)
            {
                minText = "Min";
            }
            else
            {
                maxText = "Max";
            }
        }

        let spaceForMin = helpers.measureText(font, minText) / 2;
        let spaceForMax = helpers.measureText(font, maxText) / 2;
        let totalWidth  = Math.min(this.clipForDataArea.width - 100, spaceForMin + spaceForMax + gradientWidth);

        let x = this.clipForDataArea.right - distFromRight - totalWidth;

        let zLabel      = this.zAxis.label;
        zLabel.clipArea = new ChartClipArea(x, this.clipForDataArea.y + axisLabelPadding / 2, totalWidth, font.size);
        zLabel.clipArea.applyClipping(helpers.canvas, () =>
        {
            helpers.drawTextInBox(font,
                                  TextPlacement.Center,
                                  TextOrientation.Horizontal,
                                  zLabel.getLabel((text: string) => helpers.measureText(font, text), totalWidth),
                                  font.style,
                                  x + totalWidth / 2,
                                  this.clipForDataArea.y + axisLabelPadding / 2 + font.size / 2,
                                  BoxAnchor.Center,
                                  axisLabelPadding);
        });

        helpers.drawTextInBox(font,
                              TextPlacement.Right,
                              TextOrientation.Horizontal,
                              minText,
                              font.style,
                              x + spaceForMin,
                              this.clipForDataArea.y + axisLabelPadding * 2 + font.size - 1,
                              BoxAnchor.Right,
                              axisLabelPadding);

        helpers.drawTextInBox(font,
                              TextPlacement.Left,
                              TextOrientation.Horizontal,
                              maxText,
                              font.style,
                              x + totalWidth - spaceForMax,
                              this.clipForDataArea.y + axisLabelPadding * 2 + font.size - 1,
                              BoxAnchor.Left,
                              axisLabelPadding);

        let canvas = helpers.canvas;
        canvas.save();
        canvas.fillStyle = this.transformer.getCanvasGradient(canvas,
                                                              x + spaceForMin,
                                                              this.clipForDataArea.y + axisLabelPadding * 2 + font.size,
                                                              x + totalWidth - spaceForMax,
                                                              this.clipForDataArea.y + axisLabelPadding * 2 + font.size);
        canvas.fillRect(x + spaceForMin, this.clipForDataArea.y + axisLabelPadding + font.size, totalWidth - spaceForMin - spaceForMax, axisLabelPadding * 2);
        canvas.restore();

        this.areaUtilizedForGradient = new ChartClipArea(x, this.clipForDataArea.y, totalWidth, font.size + axisLabelPadding * 3);
    }

    private renderTooltip(helper: ChartHelpers,
                          sanitizer: (html: string) => SafeHtml)
    {
        if (this.tooltipPoint)
        {
            if (this.zoomSelector.selection)
            {
                this.setTooltip(null);
            }
            else
            {
                this.clipForMainArea.applyClipping(helper.canvas, () =>
                {
                    this.tooltipPoint.render(helper, this.owner.tooltip, sanitizer, undefined, this.pointRadius);
                });
            }
        }
    }

    private setTooltip(newTooltipPoint: ScatterPlotPointTooltip): boolean
    {
        if (this.tooltipPoint == newTooltipPoint) return false;

        if (!newTooltipPoint)
        {
            // Remove any existing tooltip
            this.owner.tooltip.remove();
        }

        this.tooltipPoint = newTooltipPoint;
        return true;
    }

    private findClosestPoint(x: number,
                             y: number,
                             maxDistance: number): ScatterPlotPoint
    {
        let lowestDist: number = maxDistance;
        let closestTuple: ScatterPlotPoint;

        for (let sourceSet of this.sourceTuples)
        {
            let state = sourceSet.sourceTuple.state;
            if (!isVisible(state) || this.targetSource && state === VisualizationDataSourceState.Muted) continue;

            for (let tuple of sourceSet.sourceTuple.tuples)
            {
                let pixel = this.transformer.hitCheck(sourceSet.sourceTuple, tuple);
                if (pixel)
                {
                    let dist = ChartHelpers.pointDistance(pixel.x, pixel.y, x, y);
                    if (dist < lowestDist)
                    {
                        lowestDist   = dist;
                        closestTuple = new ScatterPlotPoint(sourceSet.sourceTuple, tuple.timestamp, tuple, 0, ChartPointType.Value, false);
                    }
                }
            }
        }

        return closestTuple;
    }
}

class PanelZoomState
{
    private static readonly minScrollZoom = 0.001;

    private top: number    = 0;
    private bottom: number = 1;

    private left: number  = 0;
    private right: number = 1;

    public get atMaxZoom(): boolean
    {
        return this.right - this.left < PanelZoomState.minScrollZoom || this.bottom - this.top < PanelZoomState.minScrollZoom;
    }

    public isZoomed(): boolean
    {
        let baseState = PanelZoomState.baseState();

        return this.top !== baseState.top || this.bottom !== baseState.bottom ||
               this.left !== baseState.left || this.right !== baseState.right;
    }

    public zoomToRectangle(left: number,
                           right: number,
                           top: number,
                           bottom: number)
    {
        if (left === right || top === bottom) return;

        let swap;

        if (top > bottom)
        {
            swap   = top;
            top    = bottom;
            bottom = swap;
        }

        if (left > right)
        {
            swap  = left;
            left  = right;
            right = swap;
        }

        let oldWidth = this.right - this.left;
        this.right   = this.left + right * oldWidth;
        this.left += left * oldWidth;

        let oldHeight = this.bottom - this.top;
        this.bottom   = this.top + bottom * oldHeight;
        this.top += top * oldHeight;
    }

    public zoomAroundPoint(x: number,
                           y: number,
                           scaleFactor: number): void
    {
        x = this.left + (this.right - this.left) * x;
        y = this.top + (this.bottom - this.top) * y;

        let leftOffset  = (this.left - x) * scaleFactor;
        let rightOffset = (this.right - x) * scaleFactor;
        if (x + leftOffset < 0)
        {
            rightOffset += 0 - (x + leftOffset);
            leftOffset = -x;
        }
        else if (x + rightOffset > 1)
        {
            leftOffset -= x + rightOffset - 1;
            rightOffset = this.right - x;
        }

        this.left  = Math.min(1, Math.max(0, x + leftOffset));
        this.right = Math.min(1, Math.max(0, x + rightOffset));

        let topOffset    = (this.top - y) * scaleFactor;
        let bottomOffset = (this.bottom - y) * scaleFactor;
        if (y + topOffset < 0)
        {
            bottomOffset += 0 - (y + topOffset);
            topOffset -= y;
        }
        else if (y + bottomOffset > 1)
        {
            topOffset -= y + bottomOffset - 1;
            bottomOffset = this.bottom - y;
        }

        this.top    = Math.min(1, Math.max(0, y + topOffset));
        this.bottom = Math.min(1, Math.max(0, y + bottomOffset));
    }

    public reset(): void
    {
        let baseState = PanelZoomState.baseState();
        this.top      = baseState.top;
        this.bottom   = baseState.bottom;
        this.left     = baseState.left;
        this.right    = baseState.right;
    }

    public getProcessedZoomState(minXValue: number,
                                 maxXValue: number,
                                 minYValue: number,
                                 maxYValue: number): ProcessedZoomState
    {
        let xRange = maxXValue - minXValue;
        let yRange = maxYValue - minYValue;

        return new ProcessedZoomState(minYValue + this.top * yRange,
                                      minYValue + this.bottom * yRange,
                                      minXValue + this.left * xRange,
                                      minXValue + this.right * xRange);
    }

    private static baseState(): PanelZoomState
    {
        return new PanelZoomState();
    }
}

class ProcessedScatterPlotSource
{
    valueRanges: ScatterPlotPropertyTuple<ProcessedChartValueRange>;

    mouseoverPoint: ScatterPlotPoint;

    get ready(): boolean
    {
        return !!this.sourceTuple.firstPoint() && isVisible(this.sourceTuple.state);
    }

    constructor(public readonly sourceTuple: DataSourceTuple,
                subscriber: ChartPointSubscriber)
    {
        this.startStreamingSamples(subscriber);
    }

    public startStreamingSamples(subscriber: ChartPointSubscriber)
    {
        if (this.sourceTuple.provider)
        {
            this.sourceTuple.provider.startStreamingSamples(1000, subscriber);
        }
    }

    public stopStreamingSamples()
    {
        if (this.sourceTuple.provider)
        {
            this.sourceTuple.provider.stopStreamingSamples();
        }
    }

    public render(helpers: ChartHelpers,
                  transformer: ScatterPlotTransformer,
                  radius: number)
    {
        if (this.ready)
        {
            let overrideColor = this.sourceTuple.state === VisualizationDataSourceState.Muted ? notTargetColor : undefined;

            for (let range of this.sourceTuple.ranges)
            {
                let pixels = this.getVisiblePixels(transformer, range);
                for (let pixel of pixels)
                {
                    let point = <ScatterPlotPoint>pixel.ctx;
                    let color = overrideColor || this.sourceTuple.color || transformer.fromPointToColor(point);
                    helpers.drawPoint(ChartPointStyle.circle, color, radius, pixel.x, pixel.y);
                }
            }
        }
    }

    public updateValueRanges()
    {
        let sourceZ      = this.sourceTuple.dataSources.valueZ;
        this.valueRanges = new ScatterPlotPropertyTuple(0, new ProcessedChartValueRange(), new ProcessedChartValueRange(), sourceZ && new ProcessedChartValueRange());

        let firstPoint = this.sourceTuple.firstPoint();
        if (!firstPoint) return;

        this.updateValueRangesHelper(this.valueRanges.valueX, (tuple) => tuple.valueX, this.sourceTuple.dataSources.valueX.getEnumeratedRange());
        this.updateValueRangesHelper(this.valueRanges.valueY, (tuple) => tuple.valueY, this.sourceTuple.dataSources.valueY.getEnumeratedRange());
        if (sourceZ) this.updateValueRangesHelper(this.valueRanges.valueZ, (tuple) => tuple.valueZ, this.sourceTuple.dataSources.valueZ.getEnumeratedRange());
    }

    private updateValueRangesHelper(processedValueRange: ProcessedChartValueRange,
                                    getTupleValue: (tuple: ScatterPlotPropertyTuple<number>) => number,
                                    enumRange: string[])
    {
        if (enumRange)
        {
            processedValueRange.enumRange = enumRange;
        }
        else
        {
            let range = processedValueRange.range;
            for (let tuple of this.sourceTuple.tuples)
            {
                range.expandForValue(getTupleValue(tuple));
            }
        }
    }

    public getVisiblePixels(transform: ScatterPlotTransformer,
                            range: ChartPointRange<ScatterPlotPropertyTuple<number>>): ChartPixelWithContext<ScatterPlotPropertyTuple<number>>[]
    {
        return transform.computeIfMissing(range, `visible-${transform.processedZoom}`, (range) =>
        {
            let results = [];

            for (let index = 0; index < range.size; index++)
            {
                let pos   = range.toNumericValue(index);
                let tuple = this.sourceTuple.tuples[pos];

                let pixel = transform.hitCheck(this.sourceTuple, tuple);
                if (pixel) results.push(pixel);
            }

            return results;
        });
    }
}

export class ScatterPlotPointTooltip extends TooltipPointForSample
{
    get clickable(): boolean
    {
        return false;
    }

    public render(helper: ChartHelpers,
                  tooltip: ChartTooltipComponent,
                  sanitizer: (html: string) => SafeHtml,
                  color?: string,
                  radius?: number)
    {
        if (!color)
        {
            let transformer = <ScatterPlotTransformer>this.transform;
            color           = transformer.fromPointToColor(<ScatterPlotPoint>this.point);
        }

        super.render(helper, tooltip, sanitizer, color, radius);
    }

    public isEquivalent(other: TooltipPoint): boolean
    {
        return other instanceof ScatterPlotPointTooltip && this.point.owningSource === other.point.owningSource;
    }

    public handleClick()
    {
        super.handleClick();
    }
}
