import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";
import {AggregationTrendGroup, AggregationTrendGroupAggregation} from "framework/ui/charting/aggregation-trend-group";
import {CanvasRenderer, ChartHelpers, MutedColor} from "framework/ui/charting/app-charting-utilities";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {ChartComponent} from "framework/ui/charting/chart.component";
import {Vector2} from "framework/ui/charting/charting-math";
import {BoxAnchor, ChartBox, ChartClipArea, ChartPointStyle, ChartValueRange, smoothDecimal} from "framework/ui/charting/core/basics";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartPointSource, ChartPointToPixel, ChartPointType, ChartValueTransformer} from "framework/ui/charting/core/data-sources";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {ChartTimeWindow} from "framework/ui/charting/core/time";
import {BaseComponent} from "framework/ui/components";

@Component({
               selector       : "o3-aggregation-trend-chart",
               templateUrl    : "./aggregation-trend-chart.component.html",
               styleUrls      : ["./aggregation-trend-chart.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AggregationTrendChartComponent extends BaseComponent
{
    private m_mode: VisualizationMode = VisualizationMode.Line;
    @Input() set mode(mode: VisualizationMode)
    {
        if (!mode) return;

        this.m_mode = mode;
        this.reportConfigurationChanges();
    }

    private m_aggregations: AggregationTrendGroup[] = [];
    @Input() set aggregations(aggregations: AggregationTrendGroup[])
    {
        if (aggregations)
        {
            let length: number;
            if (!aggregations.every((group) =>
                                    {
                                        let currLen = group.aggregations.length;
                                        if (length !== undefined)
                                        {
                                            return currLen === length;
                                        }
                                        else
                                        {
                                            length = currLen;
                                            return true;
                                        }
                                    }))
            {
                return;
            }
        }

        this.m_aggregations = aggregations;
        this.reportConfigurationChanges();
    }

    private m_showY: boolean = true;
    @Input() set showY(show: boolean)
    {
        if (!!show === this.m_showY) return;

        this.m_showY = !!show;
        this.reportConfigurationChanges();
    }

    private m_showLegend: boolean = true;
    @Input() set showLegend(show: boolean)
    {
        if (!!show === this.m_showLegend) return;

        this.m_showLegend = !!show;
        this.reportConfigurationChanges();
    }

    private interactionState                                 = new AggregationTrendInteractionState();
    private computedPanels: ProcessedAggregationTrendPanel[] = [];

    get test_ticks(): number[][]
    {
        return this.computedPanels.map((panel) => UtilsService.arrayCopy(panel.yTickValues));
    }

    get valid(): boolean
    {
        return this.computedPanels.length && this.computedPanels.some((panel) => panel.valid);
    }

    @Input() label: string;

    private debouncingHeight: number;
    private debouncingWidth: number;

    private containerElement: HTMLElement;
    private chartElement: HTMLCanvasElement;

    chartHelpers: ChartHelpers;

    @ViewChild("chart", {static: true}) chartRef: ElementRef;
    @ViewChild("tooltip", {static: true}) tooltip: ChartTooltipComponent;

    @Output() renderCompleted = new EventEmitter<void>();

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.chartElement     = this.chartRef.nativeElement;
        this.containerElement = this.chartElement.parentElement;
        this.chartHelpers     = new ChartHelpers(this.chartElement.getContext("2d"));

        this.chartElement.addEventListener("mousemove", (e: MouseEvent) => this.handleEvent("mousemove", e));
        this.chartElement.addEventListener("mouseleave", (e: MouseEvent) => this.handleEvent("mouseleave", e));

        this.refreshSize();
    }

    afterConfigurationChanges()
    {
        super.afterConfigurationChanges();

        if (!this.containerElement || !this.chartElement) return;

        this.computedPanels = [new ProcessedAggregationTrendPanel(this, this.m_aggregations, this.m_mode, this.m_showY, this.m_showLegend)];

        ChartHelpers.scaleCanvas(this.chartElement, this.debouncingWidth, this.debouncingHeight);

        for (let panel of this.computedPanels) panel.generateRange();

        let width  = this.chartElement.clientWidth;
        let height = this.chartElement.clientHeight;
        if (!width || width <= 0 || !height || height <= 0) return;

        let titleSpace = this.label ? ChartComponent.CHART_TITLE_HEIGHT : 0;
        // assume one title for n panels and each panel provided same height

        let availableHeightPerPanel = (height - titleSpace) / this.computedPanels.length;

        let maxPrecision = 0;
        for (let group of this.m_aggregations)
        {
            if (group.valuePrecision)
            {
                maxPrecision = Math.max(maxPrecision, group.valuePrecision - 1);
            }
        }

        this.interactionState = new AggregationTrendInteractionState();
        this.tooltip.remove();

        let offsetY = titleSpace;
        for (let i = 0; i < this.computedPanels.length; i++)
        {
            let panel              = this.computedPanels[i];
            panel.interactionState = this.interactionState;

            offsetY += panel.prepareLayout(0, offsetY, width, availableHeightPerPanel);
        }

        this.redrawCanvas();

        this.markForCheck();
    }

    private handleEvent<K extends keyof WindowEventMap>(type: K,
                                                        e: WindowEventMap[K])
    {
        if (!this.valid) return;

        let redraw = false;
        switch (type)
        {
            case "mousemove":
                let mouseEvent = <MouseEvent>e;
                redraw         = this.handleMouseMove(mouseEvent.offsetX, mouseEvent.offsetY);
                break;

            case "mouseleave":
                redraw                                 = this.interactionState.hasState;
                this.interactionState.mouseOverPrimary = this.interactionState.mouseOverSecondary = undefined;
                this.tooltip.remove();
                break;
        }

        if (redraw) this.redrawCanvas();
    }

    private handleMouseMove(x: number,
                            y: number): boolean
    {
        let redraw = false;
        for (let panel of this.computedPanels)
        {
            redraw = panel.handleMouseMove(x, y) || redraw;
        }

        return redraw;
    }

    renderTooltip(aggregation: ProcessedAggregationTrendPoint,
                  mouseX: number,
                  mouseY: number)
    {
        let tooltip = ChartPointSource.generateTooltipEntry("Aggregation", aggregation.aggregation.group.aggType) +
                      ChartPointSource.generateTooltipEntry("Group", aggregation.aggregation.group.name) +
                      ChartPointSource.generateTooltipEntry("Value", aggregation.aggregation.formattedValue, false);

        switch (this.m_mode)
        {
            case VisualizationMode.Bar:
                this.tooltip.render(mouseX, mouseY, tooltip);
                break;

            case VisualizationMode.Line:
                this.tooltip.render(aggregation.x, aggregation.y, tooltip);
                break;
        }
    }

    private redrawCanvas()
    {
        let helpers = this.chartHelpers;
        let width   = this.chartElement.clientWidth;
        let height  = this.chartElement.clientHeight;

        helpers.canvas.clearRect(0, 0, width, height);

        if (this.label)
        {
            let titlePadding = 8;
            let font         = new ChartFont(this.computedPanels[0].labelFont.color, undefined, ChartComponent.CHART_TITLE_HEIGHT - (titlePadding * 2));

            helpers.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal,
                                  this.label, font.color, width / 2, 0, BoxAnchor.Top, titlePadding);
        }

        for (let panel of this.computedPanels)
        {
            panel.render();
        }

        this.renderCompleted.emit();
    }

    refreshSize(): boolean
    {
        if (!this.containerElement) return false;

        let width  = this.containerElement.offsetWidth;
        let height = this.containerElement.offsetHeight;

        if (width <= 0 || height <= 0) return false;

        if (this.debouncingWidth != width || this.debouncingHeight != height)
        {
            this.debouncingWidth  = width;
            this.debouncingHeight = height;

            this.reportConfigurationChanges();
        }

        if (this.interactionState.hasState)
        {
            this.reportConfigurationChanges();
        }

        return true;
    }
}

//--//

class AggregationTrendInteractionState
{
    get hasState(): boolean
    {
        if (this.mouseOverPrimary) return true;
        if (this.mouseOverSecondary) return true;

        return false;
    }

    public mouseOverPrimary: ProcessedAggregationTrendPoint;
    public mouseOverSecondary: ProcessedAggregationTrendPoint;
}

const minAxisSectionWidth: number  = 20;
const axisWidth: number            = 1;
const baseSpacingForYTicks: number = 50;
const labelPadding: number         = 2;
const legendPadding: number        = 3;

enum VisualizationMode
{
    Line = "Line",
    Bar  = "Bar"
}

class ProcessedAggregationTrendPoint
{
    public static readonly pointRadius: number = 4;

    barX: number;
    barY: number;
    barHeight: number;

    get color(): string
    {
        return this.group.color;
    }

    constructor(public readonly x: number,
                public readonly y: number,
                public readonly type: ChartPointType,
                public readonly aggregation: AggregationTrendGroupAggregation,
                public readonly group: ProcessedAggregationTrendGroup)
    {
    }
}

class ProcessedAggregationTrendGroup
{
    public color: string;

    constructor(public group: AggregationTrendGroup)
    {
        for (let aggregation of group.aggregations)
        {
            aggregation.roundValue(group.valuePrecision);
        }
    }
}

class ProcessedAggregationTrendPanel
{
    processedAggregationGroups: ProcessedAggregationTrendGroup[];
    processedAggregationGroupPoints: ProcessedAggregationTrendPoint[][];

    numRanges: number;

    get valid(): boolean
    {
        return !!this.numRanges;
    }

    clipForMainArea: ChartClipArea;
    clipForLegendArea: ChartClipArea;
    clipForDataArea: ChartClipArea;
    xTicksAngle: number;

    interactionState = new AggregationTrendInteractionState();

    dataAreaTopBuffer: number = 0;
    dataAreaBotBuffer: number = 0;

    private static readonly BAR_WIDTH_FACTOR           = 0.75;
    private static readonly LINEAR_BAR_WIDTH_THRESHOLD = 30;

    barWidth: number;
    useRangeSeparators: boolean;

    readonly labelFont                    = new ChartFont("#696969", undefined, 11);
    readonly axisColor: string            = "#cdcdcd";
    readonly highlightBorderColor: string = this.labelFont.color;

    chartWindow: ChartTimeWindow;
    yTickValues: number[] = [];
    transform             = new AggregationChartValueTransform(ChartTimeWindow.EmptyPlaceholder, ChartClipArea.EmptyPlaceholder, 1);

    range: ChartValueRange;

    private m_hideLegendOverride: boolean;

    get showLegend(): boolean
    {
        return !this.m_hideLegendOverride && this.m_showLegend;
    }

    get chartHelpers(): ChartHelpers
    {
        return this.owner.chartHelpers;
    }

    constructor(private readonly owner: AggregationTrendChartComponent,
                aggregationGroups: AggregationTrendGroup[],
                public visualizationMode: VisualizationMode,
                public showYAxis: boolean,
                private m_showLegend: boolean)
    {
        let colors                      = aggregationGroups.map((group) => group.color)
                                                           .filter((color) => color);
        this.processedAggregationGroups = aggregationGroups.map((group) =>
                                                                {
                                                                    let processedGroup   = new ProcessedAggregationTrendGroup(group);
                                                                    group.color ||= ChartColorUtilities.nextBestColor(colors);
                                                                    processedGroup.color = group.color;
                                                                    return processedGroup;
                                                                });

        this.numRanges = this.processedAggregationGroups.length > 0 && this.processedAggregationGroups[0].group.aggregations.length || 0;
    }

    prepareLayout(offsetX: number,
                  offsetY: number,
                  width: number,
                  height: number): number
    {
        if (this.numRanges === 0) return offsetY;

        if (this.showYAxis)
        {
            let halfLabelHeight = this.labelFont.size / 2;
            offsetY += halfLabelHeight;
            height -= halfLabelHeight;
        }

        this.clipForMainArea = new ChartClipArea(offsetX, offsetY, width, height);

        this.generateTicks();

        this.generateChartInfo();

        return offsetY + height;
    }

    private generateTicks()
    {
        let tickInfo = ChartHelpers.getMajorTickInfo(this.clipForMainArea.height, baseSpacingForYTicks, this.range, false);

        // check to see if can override max/min at zero
        if (this.range.min >= 0 && tickInfo.min < 0) tickInfo.min = 0;
        if (this.range.max <= 0 && tickInfo.max > 0) tickInfo.max = 0;

        this.yTickValues = tickInfo.generateArray();

        // if range.max was modified, then last tick might be below range.max: adjust
        let lastTick = this.yTickValues[this.yTickValues.length - 1];
        if (lastTick < this.range.max)
        {
            this.yTickValues.push(UtilsService.getRoundedValue(lastTick + tickInfo.tickDist, tickInfo.tickPrecision));
        }
    }

    measureLabel(label: string): number
    {
        return this.chartHelpers.measureText(this.labelFont, label) + 2 * labelPadding;
    }

    private generateClipForLegend()
    {
        if (!this.showLegend)
        {
            this.clipForLegendArea = new ChartClipArea(this.clipForMainArea.x, this.clipForMainArea.y + this.clipForMainArea.height, this.clipForMainArea.width, 0);
        }
        else
        {
            let numLegendRows = Math.ceil(this.processedAggregationGroups.length / 2);
            let legendHeight  = numLegendRows * this.labelFont.size * 2 + (numLegendRows + 1) * legendPadding;

            this.clipForLegendArea = new ChartClipArea(this.clipForMainArea.x,
                                                       this.clipForMainArea.y + this.clipForMainArea.height - legendHeight,
                                                       this.clipForMainArea.width,
                                                       legendHeight);
        }
    }

    private generateClipForDataArea(longestYTickLabel: number): ChartClipArea
    {
        const yAxisWidth = this.showYAxis ? Math.max(minAxisSectionWidth, longestYTickLabel) : 0;
        const xTicks     = this.processedAggregationGroups[0].group.aggregations.map((aggregation) => aggregation.rangeLabel);

        let prevLength: number;
        let max    = 0;
        let maxSum = 0;
        for (let i = 0; i < xTicks.length; i++)
        {
            let currLength = this.measureLabel(xTicks[i]);
            max            = Math.max(max, currLength);
            if (prevLength !== undefined)
            {
                maxSum = Math.max(prevLength + currLength, maxSum);
            }

            prevLength = currLength;
        }

        const width     = this.clipForMainArea.width - yAxisWidth;
        const tickWidth = width / xTicks.length || width;
        let xAxisHeight: number;
        if (maxSum / 2 < tickWidth)
        {
            xAxisHeight      = minAxisSectionWidth;
            this.xTicksAngle = 0;
        }
        else
        {
            const textHeight = this.labelFont.size + labelPadding * 2;
            this.xTicksAngle = Math.acos((tickWidth - labelPadding * 2) / max);

            xAxisHeight = Math.sqrt(Math.pow(max, 2) - Math.pow(tickWidth - labelPadding * 2, 2)) + textHeight * Math.cos(this.xTicksAngle);
        }

        return new ChartClipArea(this.clipForMainArea.x + yAxisWidth,
                                 this.clipForMainArea.y + this.dataAreaTopBuffer,
                                 width,
                                 this.clipForMainArea.height - xAxisHeight - this.dataAreaTopBuffer - this.dataAreaBotBuffer - this.clipForLegendArea.height);
    }

    private generateChartInfo()
    {
        this.m_hideLegendOverride = null;

        let longestYTickLabel = 0;
        if (this.showYAxis)
        {
            for (let tick of this.yTickValues) longestYTickLabel = Math.max(this.measureLabel("" + tick), longestYTickLabel);
        }
        else
        {
            for (let processedGroup of this.processedAggregationGroups)
            {
                for (let aggregation of processedGroup.group.aggregations)
                {
                    aggregation.valueLength = this.measureLabel("" + aggregation.value);
                }
            }
        }

        let idx = 0;
        while (idx < 2)
        {
            this.generateClipForLegend();

            this.clipForDataArea = this.generateClipForDataArea(longestYTickLabel);

            if (this.clipForDataArea.height <= 1)
            {
                // not enough space for chart under provided config: override panel configs to change how it's rendered and make space
                if (!this.showYAxis)
                {
                    this.showYAxis = true;
                    continue;
                }
                else if (this.showLegend)
                {
                    this.m_hideLegendOverride = true;
                    continue;
                }
                break;
            }

            this.chartWindow = new ChartTimeWindow(0, this.numRanges - 1, this.yTickValues[0], this.yTickValues[this.yTickValues.length - 1]);
            this.transform   = new AggregationChartValueTransform(this.chartWindow, this.clipForDataArea, this.numRanges);

            if (this.showYAxis) break;

            let pxStillRequired = 0;
            if (idx === 0)
            {
                const spaceForBarAndLabel = this.dataAreaTopBuffer - this.clipForDataArea.top;
                for (let processedGroup of this.processedAggregationGroups)
                {
                    for (let aggregation of processedGroup.group.aggregations)
                    {
                        if (aggregation.rawValue > 0 || this.visualizationMode === VisualizationMode.Line)
                        {
                            let y = this.transform.fromValueToYCoordinate(aggregation.rawValue);

                            // using ceil to limit marginal impact cycling
                            pxStillRequired = Math.max(pxStillRequired, Math.ceil(aggregation.valueLength - (spaceForBarAndLabel + y)));
                        }
                    }
                }
            }
            else if (this.visualizationMode === VisualizationMode.Bar)
            {
                const spaceForBarAndLabel = this.clipForDataArea.bottom + this.dataAreaBotBuffer;
                for (let processedGroup of this.processedAggregationGroups)
                {
                    for (let aggregation of processedGroup.group.aggregations)
                    {
                        if (aggregation.rawValue < 0)
                        {
                            let y = this.transform.fromValueToYCoordinate(aggregation.rawValue);

                            // using ceil to limit marginal impact cycling
                            pxStillRequired = Math.max(pxStillRequired, Math.ceil(aggregation.valueLength - (spaceForBarAndLabel - y)));
                        }
                    }
                }
            }

            if (pxStillRequired > 0)
            {
                if (idx === 0)
                {
                    this.dataAreaTopBuffer += pxStillRequired;
                }
                else
                {
                    this.dataAreaBotBuffer += pxStillRequired;
                }
            }
            else
            {
                idx++;
            }
        }

        this.processedAggregationGroupPoints = this.processedAggregationGroups.map((processedGroup) => this.extractPoints(this.transform, processedGroup));
    }

    generateRange()
    {
        this.range = new ChartValueRange();
        for (let processedGroup of this.processedAggregationGroups)
        {
            for (let agg of processedGroup.group.aggregations)
            {
                this.range.expandForValue(agg.rawValue);
            }
        }
    }

    extractPoints(transform: AggregationChartValueTransform,
                  aggregationGroup: ProcessedAggregationTrendGroup): ProcessedAggregationTrendPoint[]
    {
        return transform.computeIfMissing(aggregationGroup, "visibleValues", (aggGroup: ProcessedAggregationTrendGroup) =>
        {
            let results = [];

            let aggregations = aggGroup.group.aggregations;
            for (let i = 0; i < aggregations.length; i++)
            {
                let value = aggregations[i].rawValue;
                let type  = !isNaN(value) ? ChartPointType.Value : ChartPointType.NoValue;

                let pixel = new ProcessedAggregationTrendPoint(transform.fromMillisecondToXCoordinate(i),
                                                               transform.fromValueToYCoordinate(value),
                                                               type,
                                                               aggregations[i],
                                                               aggregationGroup);

                results.push(pixel);
            }

            return results;
        });
    }

    handleMouseMove(mouseX: number,
                    mouseY: number): boolean
    {
        let point: ProcessedAggregationTrendPoint;
        let pointSecondary: ProcessedAggregationTrendPoint;
        if (this.clipForDataArea?.hitCheck(mouseX, mouseY))
        {
            let pxPerRange            = this.clipForDataArea.width / this.numRanges;
            let rangeIdxApproximation = (mouseX - this.clipForDataArea.left) / pxPerRange;
            switch (this.visualizationMode)
            {
                case VisualizationMode.Bar:
                    let rangeIdx = Math.floor(rangeIdxApproximation);
                    for (let processedPoints of this.processedAggregationGroupPoints)
                    {
                        let curr    = processedPoints[rangeIdx];
                        let currBar = new ChartBox(curr.barX, curr.barY, this.barWidth, curr.barHeight);
                        if (currBar.hitCheck(mouseX, mouseY))
                        {
                            point = curr;
                            break;
                        }
                    }
                    break;

                case VisualizationMode.Line:
                    const maxPointDist = 30;
                    let shortestDist   = maxPointDist;

                    rangeIdxApproximation -= 1 / 2; // points for line mode are drawn at the horizontal center of their respective range
                    let low  = Math.max(Math.ceil(rangeIdxApproximation - maxPointDist / pxPerRange), 0);
                    let high = Math.min(Math.ceil(rangeIdxApproximation + maxPointDist / pxPerRange), this.numRanges);
                    for (let rangeIdx = low; rangeIdx < high; rangeIdx++)
                    {
                        for (let processedPoints of this.processedAggregationGroupPoints)
                        {
                            let curr = processedPoints[rangeIdx];
                            let dist = ChartHelpers.pointDistance(mouseX, mouseY, curr.x, curr.y);
                            if (dist < shortestDist)
                            {
                                point        = curr;
                                shortestDist = dist;
                            }
                        }
                    }

                    if (!point)
                    {
                        const maxLineDist = 18;
                        shortestDist      = maxLineDist;

                        low  = Math.max(Math.ceil(rangeIdxApproximation - maxLineDist / pxPerRange) - 1, 0);
                        high = Math.min(Math.ceil(rangeIdxApproximation + maxLineDist / pxPerRange) + 1, this.numRanges);
                        for (let rangeIdx = low; rangeIdx + 1 < high; rangeIdx++)
                        {
                            for (let processedPoints of this.processedAggregationGroupPoints)
                            {
                                let prevPoint   = processedPoints[rangeIdx];
                                let nextPoint   = processedPoints[rangeIdx + 1];
                                let pointOnLine = ChartHelpers.pointOnLineClosestToTarget(prevPoint.x, prevPoint.y, nextPoint.x, nextPoint.y, mouseX, mouseY, true);
                                let dist        = ChartHelpers.pointDistance(mouseX, mouseY, pointOnLine.x, pointOnLine.y);
                                if (dist < shortestDist)
                                {
                                    point          = prevPoint;
                                    pointSecondary = nextPoint;
                                    shortestDist   = dist;
                                }
                            }
                        }
                    }
                    break;
            }
        }

        let redraw = false;
        if (this.interactionState.mouseOverPrimary != point || this.interactionState.mouseOverSecondary != pointSecondary)
        {
            this.interactionState.mouseOverPrimary   = point;
            this.interactionState.mouseOverSecondary = pointSecondary;
            redraw                                   = true;
        }

        if (this.interactionState.mouseOverPrimary && !this.interactionState.mouseOverSecondary)
        {
            this.owner.renderTooltip(this.interactionState.mouseOverPrimary, mouseX, mouseY);
        }
        else
        {
            this.owner.tooltip.remove();
        }

        return redraw;
    }

    render()
    {
        if (!this.valid) return;

        this.renderAxes();

        this.renderAxisLabels();

        this.renderData();

        this.renderRangeSeparators();

        if (this.showLegend) this.renderLegend();
    }

    private renderAxes()
    {
        let canvas = this.chartHelpers.canvas;
        canvas.save();
        canvas.globalAlpha = 1.0;
        canvas.strokeStyle = this.axisColor;
        canvas.lineWidth   = axisWidth;

        let halfLineWidth = canvas.lineWidth / 2;
        canvas.beginPath();
        canvas.moveTo(this.clipForDataArea.left, this.clipForDataArea.bottom + this.dataAreaBotBuffer - 1 + halfLineWidth);
        if (this.showYAxis)
        {
            canvas.moveTo(this.clipForDataArea.left, this.clipForDataArea.top - this.dataAreaTopBuffer);
            canvas.lineTo(this.clipForDataArea.left, this.clipForDataArea.bottom + this.dataAreaBotBuffer - 1 + halfLineWidth);
        }
        canvas.lineTo(this.clipForDataArea.right - 1 + halfLineWidth, this.clipForDataArea.bottom + this.dataAreaBotBuffer - 1 + halfLineWidth);
        canvas.stroke();
        canvas.restore();

        if (this.visualizationMode === VisualizationMode.Line)
        {
            let verticalGridValues = [];
            for (let i = 0; i <= this.numRanges; i += 0.5) verticalGridValues.push(i);

            let horizontalGridValues = this.yTickValues;
            if (!this.showYAxis)
            {
                // yTickValues match up when there is a y axis... when there's no y-axis, there is space allocated to floating labels
                // this means that the yTickValues don't necessarily match up to the chart's vertical space: create cosmetic horizontalGridValues

                let bottom           = this.clipForDataArea.bottom + this.dataAreaBotBuffer;
                let prev             = this.transform.fromYCoordinateToValue(bottom);
                let diff             = this.transform.fromYCoordinateToValue(bottom - baseSpacingForYTicks) - prev;
                horizontalGridValues = [prev];
                while (this.transform.fromValueToYCoordinate(prev + diff) > this.clipForMainArea.top)
                {
                    prev += diff;
                    horizontalGridValues.push(prev);
                }
            }

            CanvasRenderer.renderGrid(canvas,
                                      this.transform,
                                      this.clipForDataArea.x,
                                      this.clipForDataArea.right,
                                      this.clipForDataArea.y - (!this.showYAxis && this.dataAreaTopBuffer || 0),
                                      this.clipForDataArea.bottom + (!this.showYAxis && this.dataAreaBotBuffer || 0),
                                      verticalGridValues,
                                      horizontalGridValues);
        }
    }

    private renderRangeSeparators()
    {
        let aggregationPoints = this.processedAggregationGroupPoints[0];
        if (this.visualizationMode !== VisualizationMode.Bar || aggregationPoints.length <= 1 || !this.useRangeSeparators) return;

        let canvas         = this.chartHelpers.canvas;
        canvas.strokeStyle = this.axisColor;
        canvas.lineWidth   = axisWidth;

        let pixelDiff = aggregationPoints[1].x - aggregationPoints[0].x;
        let prevX     = aggregationPoints[0].x - pixelDiff / 2;
        for (let i = 0; i < aggregationPoints.length - 1; i++)
        {
            let currX = prevX + pixelDiff;

            canvas.beginPath();
            canvas.moveTo(currX, this.clipForDataArea.bottom);
            canvas.lineTo(currX, this.clipForDataArea.top);
            canvas.stroke();

            prevX = currX;
        }
    }

    private renderAxisLabels()
    {
        let processedPoints = this.processedAggregationGroupPoints[0];

        let helpers                = this.chartHelpers;
        helpers.canvas.globalAlpha = 1.0;

        // y axis labels
        if (this.showYAxis)
        {
            for (let tick of this.yTickValues)
            {
                helpers.drawTextInBox(this.labelFont,
                                      TextPlacement.Right,
                                      TextOrientation.Horizontal,
                                      "" + tick,
                                      this.labelFont.color,
                                      this.clipForDataArea.left,
                                      this.transform.fromValueToYCoordinate(tick),
                                      BoxAnchor.Right,
                                      labelPadding);
            }
        }

        // x axis labels
        for (let pixel of processedPoints)
        {
            let box = helpers.placeTextInBoxWithAngle(this.labelFont, TextPlacement.Center, this.xTicksAngle,
                                                      pixel.aggregation.rangeLabel, pixel.x, this.clipForDataArea.bottom + this.dataAreaBotBuffer + 1,
                                                      BoxAnchor.Top, 0);

            let preRotationTranslation = new Vector2(0, 0);
            if (this.xTicksAngle > Math.PI * 5 / 12) preRotationTranslation.x += this.labelFont.size * Math.sin(this.xTicksAngle) / 3;

            helpers.drawAngledBoxText(box, this.labelFont, pixel.aggregation.rangeLabel, this.labelFont.color, preRotationTranslation, null, null);
        }
    }

    private renderData()
    {
        switch (this.visualizationMode)
        {
            case VisualizationMode.Line:
                this.renderLines();
                break;

            case VisualizationMode.Bar:
                this.renderBars();
                break;
        }
    }

    private renderLines()
    {
        let helpers = this.chartHelpers;
        let canvas  = helpers.canvas;
        this.clipForMainArea.applyClipping(canvas, () =>
        {
            canvas.globalAlpha = 1;
            canvas.lineWidth   = 2;

            for (let processedPoints of this.processedAggregationGroupPoints)
            {
                let numValues = processedPoints.length;
                if (numValues === 0) return;

                let firstPoint = processedPoints[0];
                let color      = firstPoint.color;
                if (this.interactionState.mouseOverPrimary && this.interactionState.mouseOverPrimary.group !== firstPoint.group) color = MutedColor;

                if (numValues === 1)
                {
                    helpers.drawPoint(ChartPointStyle.circle, color, ProcessedAggregationTrendPoint.pointRadius, firstPoint.x, firstPoint.y);
                }
                else
                {
                    canvas.beginPath();
                    for (let i = 0; i < numValues; i++)
                    {
                        let aggregationPoint = processedPoints[i];
                        if (aggregationPoint.type !== ChartPointType.Value) continue;

                        if (i == 0)
                        {
                            CanvasRenderer.safeMoveTo(canvas, aggregationPoint.x, aggregationPoint.y);
                        }
                        else
                        {
                            CanvasRenderer.safeLineTo(canvas, aggregationPoint.x, aggregationPoint.y);
                        }
                    }

                    canvas.strokeStyle = color;
                    canvas.stroke();
                }
            }

            if (this.interactionState.mouseOverPrimary)
            {
                let points = [this.interactionState.mouseOverPrimary];
                let radius = ProcessedAggregationTrendPoint.pointRadius;
                if (this.interactionState.mouseOverSecondary)
                {
                    points.push(this.interactionState.mouseOverSecondary);
                    radius *= Math.sqrt(1 / 2);
                }

                for (let point of points)
                {
                    if (point) helpers.drawPoint(ChartPointStyle.circle, point.color, radius, point.x, point.y);
                }
            }

            if (!this.showYAxis)
            {
                let pointsWithLabels: ProcessedAggregationTrendPoint[] = [];
                if (this.processedAggregationGroupPoints.length === 1)
                {
                    pointsWithLabels = this.processedAggregationGroupPoints[0];
                }
                else if (this.interactionState.mouseOverPrimary)
                {
                    let mouseOverIdx = this.processedAggregationGroupPoints.findIndex((groupPoints) => groupPoints[0].group === this.interactionState.mouseOverPrimary.group);
                    pointsWithLabels = this.processedAggregationGroupPoints[mouseOverIdx];
                }

                for (let aggregationPoint of pointsWithLabels)
                {
                    this.renderFloatingLabel(aggregationPoint, 0, -labelPadding, null);
                }
            }
        });
    }

    private computeBarWidth(pxAvailablePerBar: number)
    {
        if (pxAvailablePerBar > ProcessedAggregationTrendPanel.LINEAR_BAR_WIDTH_THRESHOLD)
        {
            // mapping fxn is a ln equation computed using (20, 15) and (30, 22.5); intersects with y = BAR_WIDTH_FACTOR * x
            return 18.5 * Math.log(0.1125 * pxAvailablePerBar);
        }
        else
        {
            return ProcessedAggregationTrendPanel.BAR_WIDTH_FACTOR * pxAvailablePerBar;
        }
    }

    private renderBars()
    {
        const numGroups = this.processedAggregationGroupPoints.length;

        const perRangePxAvailable = this.clipForDataArea.width / this.numRanges;
        this.barWidth             = this.computeBarWidth(perRangePxAvailable / numGroups);

        const basePaddingWidth        = (1 - ProcessedAggregationTrendPanel.BAR_WIDTH_FACTOR) * perRangePxAvailable;
        const totalPaddingWidth       = perRangePxAvailable - this.barWidth * numGroups;
        const extraEdgeOfRangePadding = (totalPaddingWidth - basePaddingWidth) / 2;
        const betweenBarPadding       = basePaddingWidth / (numGroups + 1);
        this.useRangeSeparators       = betweenBarPadding > extraEdgeOfRangePadding;

        const halfBarWidth = this.barWidth / 2;
        const borderRadius = Math.max(this.barWidth / 5, 2);

        const minGradientFactor   = 0.125;
        const maxGradientFactor   = 0.35;
        const maxGradientWidth    = 55;
        const easeInOutFactor     = 1.8;
        const gradientSpanDecimal = UtilsService.clamp(0, 1, this.barWidth / maxGradientWidth);
        const gradientFactor      = minGradientFactor + smoothDecimal(gradientSpanDecimal, easeInOutFactor) * (maxGradientFactor - minGradientFactor);

        const helpers      = this.chartHelpers;
        const canvas       = helpers.canvas;
        canvas.globalAlpha = 1;

        let addX = -perRangePxAvailable / 2 + extraEdgeOfRangePadding;
        for (let aggregationPoints of this.processedAggregationGroupPoints)
        {
            const chroma     = ChartColorUtilities.safeChroma(aggregationPoints[0].color);
            const colorLight = chroma.brighten(gradientFactor)
                                     .hex();
            const colorDark  = chroma.darken(gradientFactor)
                                     .hex();

            addX += betweenBarPadding;
            for (let aggPoint of aggregationPoints)
            {
                if (aggPoint.type !== ChartPointType.Value) continue;

                let y           = aggPoint.y;
                let height      = this.transform.getYZeroOffset() - aggPoint.y;
                const belowZero = height < 0;
                if (belowZero)
                {
                    y      = this.transform.getYZeroOffset();
                    height = -height;
                }

                aggPoint.barX      = aggPoint.x + addX;
                aggPoint.barHeight = height - 1;
                let topBorderRadius: number;
                let botBorderRadius: number;
                if (belowZero)
                {
                    aggPoint.barY   = y + 1;
                    topBorderRadius = 0;
                    botBorderRadius = borderRadius;
                }
                else
                {
                    aggPoint.barY   = y;
                    topBorderRadius = borderRadius;
                    botBorderRadius = 0;
                }

                if (this.interactionState.mouseOverPrimary === aggPoint)
                {
                    canvas.strokeStyle = this.highlightBorderColor;
                    helpers.roundedRectExplicit(aggPoint.barX - 0.5, aggPoint.barY - 0.5, this.barWidth + 1, aggPoint.barHeight + 1,
                                                topBorderRadius, topBorderRadius, botBorderRadius, botBorderRadius);
                    canvas.stroke();
                }

                helpers.roundedRectExplicit(aggPoint.barX, aggPoint.barY, this.barWidth, aggPoint.barHeight,
                                            topBorderRadius, topBorderRadius, botBorderRadius, botBorderRadius);

                const gradient = canvas.createLinearGradient(aggPoint.barX, y, aggPoint.barX + this.barWidth, y);
                gradient.addColorStop(0, colorLight);
                gradient.addColorStop(1, colorDark);
                canvas.fillStyle = gradient;
                canvas.fill();

                if (!this.showYAxis)
                {
                    y = belowZero ? y + height : undefined;
                    this.renderFloatingLabel(aggPoint, addX + halfBarWidth, 0, y);
                }
            }
            addX += this.barWidth;
        }
    }

    private renderFloatingLabel(aggregationPoint: ProcessedAggregationTrendPoint,
                                offsetX: number,
                                offsetY: number,
                                y: number)
    {
        let orientation = TextOrientation.VerticalAscending;
        let boxAnchor   = BoxAnchor.Bottom;
        if (y == undefined)
        {
            y = aggregationPoint.y;
        }
        else
        {
            orientation = TextOrientation.VerticalDescending;
            boxAnchor   = BoxAnchor.Top;
        }

        this.chartHelpers.drawTextInBox(this.labelFont,
                                        TextPlacement.Left,
                                        orientation,
                                        "" + aggregationPoint.aggregation.value,
                                        this.labelFont.color,
                                        aggregationPoint.x + offsetX,
                                        y + offsetY,
                                        boxAnchor,
                                        labelPadding);
    }

    private renderLegend()
    {
        let helpers = this.chartHelpers;
        let canvas  = helpers.canvas;

        this.clipForLegendArea.applyClipping(canvas, () =>
        {
            canvas.lineWidth = 2;

            let halfLegendWidth    = this.clipForLegendArea.width / 2;
            let availableWidth     = halfLegendWidth - 2 * legendPadding;
            let availableTextWidth = availableWidth - (this.labelFont.size * 2 + legendPadding);

            let yOffset = 0;
            for (let i = 0; i < this.processedAggregationGroups.length; i++)
            {
                let processedGroup = this.processedAggregationGroups[i];
                canvas.fillStyle   = canvas.strokeStyle = processedGroup.color;

                let x = i % 2 === 0 ? legendPadding : halfLegendWidth + legendPadding;
                let y = legendPadding + yOffset;

                ProcessedAggregationTrendPanel.renderLegendCircle(helpers, this.clipForLegendArea.x + x, this.clipForLegendArea.y + y, this.labelFont.size);

                if (i % 2 === 1) yOffset += this.labelFont.size * 2 + legendPadding;

                const experimentallyDevelopedYAdjustment = -1;

                if (helpers.measureText(this.labelFont, processedGroup.group.name) < availableTextWidth)
                {
                    helpers.drawTextInBox(this.labelFont,
                                          TextPlacement.Left,
                                          TextOrientation.Horizontal,
                                          processedGroup.group.name,
                                          this.labelFont.color,
                                          this.clipForLegendArea.x + x + 2 * this.labelFont.size + legendPadding,
                                          this.clipForLegendArea.y + this.labelFont.size + y + experimentallyDevelopedYAdjustment,
                                          BoxAnchor.Left,
                                          legendPadding);
                }
                else
                {
                    let splitLabels = UtilsService.splitTextIntoTwoLines(processedGroup.group.name);
                    for (let i = 0; i < splitLabels.length; i++)
                    {
                        let labelPart = splitLabels[i];

                        helpers.drawTextInBox(this.labelFont,
                                              TextPlacement.Left,
                                              TextOrientation.Horizontal,
                                              labelPart,
                                              this.labelFont.color,
                                              this.clipForLegendArea.x + x + 2 * this.labelFont.size + legendPadding,
                                              this.clipForLegendArea.y + this.labelFont.size * (i + 0.5) + y + experimentallyDevelopedYAdjustment,
                                              BoxAnchor.Left,
                                              legendPadding);
                    }
                }
            }
        });
    }

    private static renderLegendCircle(helpers: ChartHelpers,
                                      x: number,
                                      y: number,
                                      legendFontSize: number)
    {
        const canvas = helpers.canvas;
        canvas.beginPath();
        canvas.arc(x + legendFontSize, y + legendFontSize, 0.8 * legendFontSize, 0, 2 * Math.PI);
        canvas.closePath();
        canvas.fill();
    }
}

export class AggregationChartValueTransform extends ChartValueTransformer<ProcessedAggregationTrendGroup>
{
    private m_zeroTransferFunction: ChartPointToPixel;

    constructor(domain: ChartTimeWindow,
                clip: ChartClipArea,
                numAggregations: number)
    {
        let aggBufferSpace = clip.width / numAggregations / 2;
        clip               = new ChartClipArea(clip.x + aggBufferSpace, clip.y, clip.width - 2 * aggBufferSpace, clip.height);
        super(domain, clip);

        this.m_zeroTransferFunction = ChartPointToPixel.generateMapping(domain, clip, 0, 0);
    }

    getYZeroOffset(): number
    {
        return this.m_zeroTransferFunction.yZeroOffset;
    }

    fromMillisecondToXCoordinate(msec: number): number
    {
        return this.m_zeroTransferFunction.applyX(msec);
    }

    fromValueToYCoordinate(y: number): number
    {
        return this.m_zeroTransferFunction.applyY(y);
    }

    fromXCoordinateToMillisecond(x: number): number
    {
        return this.m_zeroTransferFunction.applyReverseX(x);
    }

    fromYCoordinateToValue(y: number): number
    {
        return this.m_zeroTransferFunction.applyReverseY(y);
    }

    protected generateKey(range: ProcessedAggregationTrendGroup,
                          prefix: string): string
    {
        return `${prefix}/${range.group.id}`;
    }
}
