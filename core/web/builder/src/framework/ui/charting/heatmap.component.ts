import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, SimpleChanges, ViewChild} from "@angular/core";

import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartZoomState} from "framework/ui/charting/chart-timeline.component";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {EventReceiver} from "framework/ui/charting/charting-interaction";
import {ChartBox, ChartClipArea, ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities, ColorGradientDiscrete, ColorGradientExtended, ColorGradientStop, ColorMapper, IColorGradient} from "framework/ui/charting/core/colors";
import {ChartPoint, ChartPointSource, ChartPointType, ChartValueTransform} from "framework/ui/charting/core/data-sources";
import {ChartFont} from "framework/ui/charting/core/text";
import {ChartTimeRange, ChartTimeWindow} from "framework/ui/charting/core/time";
import {BaseComponent, fromEvent} from "framework/ui/components";
import {ContextMenuComponent, ContextMenuItemComponent} from "framework/ui/context-menu/context-menu.component";
import {Debouncer} from "framework/utils/debouncers";
import moment from "framework/utils/moment";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-heatmap",
               templateUrl    : "./heatmap.component.html",
               styleUrls      : ["./heatmap.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class HeatmapComponent extends BaseComponent implements EventReceiver
{
    private static readonly lastSampleWidth = 30;

    private stateChanged = true;

    private renderThrottler                         = new Debouncer(500, async () => this.render());
    private font                                    = new ChartFont(undefined, undefined, 16);
    private labelWidth: number                      = 150;
    private calculatedColorMapper: ColorGradientDiscrete;
    private processedSources: ProcessedDataSource[] = [];
    private timeRange                               = new ChartTimeRange();
    private valueRange                              = new ChartValueRange();
    private domain: ChartTimeWindow;
    private noValuePattern: HTMLImageElement;
    private noDataPattern: HTMLImageElement;

    private m_zoomSub: Subscription;
    private m_zoomState: ChartZoomState;
    @Input() set zoomState(zoomState: ChartZoomState)
    {
        this.m_zoomState  = zoomState;
        this.stateChanged = true;

        if (this.m_zoomSub)
        {
            this.m_zoomSub.unsubscribe();
            this.m_zoomSub = null;
        }

        if (this.m_zoomState)
        {
            this.m_zoomSub = this.subscribeToObservable(this.m_zoomState.stateChanged, (fromZoom: boolean) =>
            {
                this.stateChanged = true;
                this.throttledRender(fromZoom);
            });
        }
    }

    get zoomState(): ChartZoomState
    {
        return this.m_zoomState;
    }

    @Input() range: ChartTimeRange;
    @Input() dataRange: ChartValueRange;
    @Input() gradient: IColorGradient;
    @Input() colorMapper: ColorMapper;
    @Input() missingValueColor: string = "#757571";
    @Input() minimumRowHeight: number  = 25;
    @Input() hideLabels: boolean       = false;
    @Input() interactive: boolean      = true;
    @Input() shrinkToFit: boolean      = false;

    private m_sources: ChartPointSource<number>[] = [];
    @Input() set sources(sources: ChartPointSource<number>[])
    {
        this.clearSources();

        // Assign new sources
        this.m_sources = sources;

        // Start streaming samples
        for (let source of this.m_sources)
        {
            source.provider?.startStreamingSamples(1000, {
                transitionToActive  : () => this.incrementStreamingCount(),
                transitionToInactive: () => this.decrementStreamingCount(),
                newSamples          : (timestamp: moment.Moment) => this.handleNewSample(source.provider.rangeEnd ? null : timestamp.valueOf())
            });
        }

        this.stateChanged = true;
    }

    get sources(): ChartPointSource<number>[]
    {
        return this.m_sources;
    }

    private m_canvasElem: HTMLCanvasElement;
    get canvasElem(): HTMLCanvasElement
    {
        return this.m_canvasElem;
    }

    private m_stageElem: HTMLDivElement;
    get stageElem(): HTMLDivElement
    {
        return this.m_stageElem;
    }

    @ViewChild("stage", {static: true}) chartStage: ElementRef;
    @ViewChild("canvas", {static: true}) chartCanvas: ElementRef;
    @ViewChild("interactiveCanvas", {static: true}) interactiveCanvas: ElementRef;
    @ViewChild("tooltip", {static: true}) chartTooltip: ChartTooltipComponent;

    @Output() renderComplete      = new EventEmitter();
    @Output() stoppedFetchingData = new EventEmitter<void>();
    @Output() startedFetchingData = new EventEmitter<void>();

    private m_streamingSourceCount: number = 0;

    //--//

    @ViewChild("contextMenu", {static: true}) contextMenu: ContextMenuComponent;

    @ViewChild("contextMenuTriggerWrapper", {
        read  : ElementRef,
        static: true
    }) contextMenuTriggerWrapper: ElementRef;

    //--//

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.m_canvasElem = this.chartCanvas.nativeElement;
        this.m_stageElem  = this.chartStage.nativeElement;

        // subscribe to orientation change
        this.subscribeToObservable(fromEvent(window, "orientationchange"), () => this.throttledRender());

        // Register DOM events
        if (this.interactive) this.registerEvents();

        this.noValuePattern     = new Image();
        this.noValuePattern.src = "/assets/img/no-value-pattern.svg";
        this.noDataPattern      = new Image();
        this.noDataPattern.src  = "/assets/img/no-data-pattern.svg";
    }

    public ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        if (changes.range && this.zoomState)
        {
            this.zoomState.outerRange = this.range;
        }

        this.reportConfigurationChanges();
    }

    public ngOnDestroy()
    {
        super.ngOnDestroy();

        this.clearSources();
    }

    private clearSources()
    {
        for (let source of this.m_sources)
        {
            source.provider?.stopStreamingSamples();
        }
        this.m_sources = [];
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();
        this.throttledRender();
    }

    protected afterConfigurationChanges(): void
    {
        super.afterConfigurationChanges();
        this.throttledRender();
    }

    private incrementStreamingCount()
    {
        if (this.m_streamingSourceCount++ === 0)
        {
            this.startedFetchingData.emit();
        }
    }

    private decrementStreamingCount()
    {
        if (--this.m_streamingSourceCount <= 0)
        {
            this.stateChanged = true;
            this.stoppedFetchingData.emit();
        }
    }

    private handleNewSample(timestamp: number)
    {
        this.throttledRender();

        if (timestamp && this.m_zoomState)
        {
            this.m_zoomState.shiftToContain(timestamp);
        }
    }

    throttledRender(force?: boolean)
    {
        this.renderThrottler.invoke();
        if (force) this.renderThrottler.forceExecution();
    }

    // http://teropa.info/blog/2016/12/12/graphics-in-angular-2.html#canvas-graphics
    // consider: https://developer.mozilla.org/en-US/docs/Web/API/CanvasRenderingContext2D/addHitRegion (experimental)
    render()
    {
        let ctx: CanvasRenderingContext2D  = this.canvasElem.getContext("2d");
        let ctxI: CanvasRenderingContext2D = this.interactiveCanvas.nativeElement.getContext("2d");

        let width  = this.stageElem.clientWidth;
        let height = this.stageElem.clientHeight;

        if (this.sources?.length)
        {
            // determine row height (and potential new canvas size)
            let rowHeight = Math.floor(height / this.sources.length);
            if (!this.shrinkToFit && rowHeight < this.minimumRowHeight)
            {
                rowHeight = this.minimumRowHeight;
                height    = this.minimumRowHeight * this.sources.length;
            }

            // Get reference to chart helpers
            let helper = new ChartHelpers(ctx);

            // determine label width (will fit the longest label or max of 15% of canvas width)
            if (!this.hideLabels)
            {
                let labels: string[] = [];
                for (let source of this.sources)
                {
                    if (source.label) labels.push(`000) ${source.label} `);
                }
                this.labelWidth = helper.longestText(this.font, labels);
                if (this.labelWidth > (width * .15)) this.labelWidth = Math.floor(width * .15);
            }
            else
            {
                this.labelWidth = 0;
            }

            ChartHelpers.scaleCanvas(this.canvasElem, width, height);
            ctxI.canvas.width  = width;
            ctxI.canvas.height = height;

            if (this.stateChanged)
            {
                this.valueRange       = new ChartValueRange();
                this.timeRange        = new ChartTimeRange();
                this.processedSources = [];

                let evaluateMin = isNaN(this.dataRange?.min);
                let evaluateMax = isNaN(this.dataRange?.max);
                for (let source of this.sources)
                {
                    let processedSource = new ProcessedDataSource(source, this.dataRange);
                    this.processedSources.push(processedSource);

                    // determine the value range of heatmap as a whole
                    if (evaluateMin) this.valueRange.expandForValue(Math.floor(processedSource.valueRange.min));
                    if (evaluateMax) this.valueRange.expandForValue(Math.ceil(processedSource.valueRange.max));
                    if (!this.range) this.timeRange.expandRange(processedSource.timeRange.minInMillisec, processedSource.timeRange.maxInMillisec);
                }

                if (!evaluateMin) this.valueRange.expandForValue(Math.floor(this.dataRange.min));
                if (!evaluateMax) this.valueRange.expandForValue(Math.ceil(this.dataRange.max));

                this.stateChanged = false;
            }

            if (this.m_zoomState)
            {
                this.timeRange.minInMillisec = this.m_zoomState.displayedRange.minInMillisec;
                this.timeRange.maxInMillisec = this.m_zoomState.displayedRange.maxInMillisec;
            }
            else if (this.range)
            {
                this.timeRange.minInMillisec = this.range.minInMillisec;
                this.timeRange.maxInMillisec = this.range.maxInMillisec;
            }

            this.domain = new ChartTimeWindow(this.timeRange.minInMillisec, this.timeRange.maxInMillisec, this.valueRange.min, this.valueRange.max);

            if (!this.colorMapper) this.generateColorGradient();

            for (let i = 0; i < this.processedSources.length; i++)
            {
                this.renderRow(ctx, rowHeight, width, this.processedSources[i], i);
            }
        }

        this.renderComplete.emit();
    }

    private generateColorGradient()
    {
        let valueRange = this.dataRange || this.valueRange;
        let stops: ColorGradientStop[];
        if (this.gradient?.isValid() && !isNaN(valueRange.diff))
        {
            stops = new ColorGradientExtended(this.gradient).model.computeStops(valueRange.min, valueRange.max);
        }
        else
        {
            stops = [
                new ColorGradientStop(0, ChartColorUtilities.getDefaultColorById("green").hex),
                new ColorGradientStop(1, ChartColorUtilities.getDefaultColorById("red").hex)
            ];
        }

        this.calculatedColorMapper = new ColorGradientDiscrete(stops);
    }

    renderRow(ctx: CanvasRenderingContext2D,
              rowHeight: number,
              width: number,
              source: ProcessedDataSource,
              index: number)
    {
        let helper     = new ChartHelpers(ctx);
        let rectHeight = rowHeight - 1;
        let y          = rowHeight * index;

        if (!this.hideLabels)
        {
            let label = source.dataSource.label;
            if (!label) label = "[Not Named]";
            label = (index + 1) + ") " + label;

            // ellipsis label as needed
            label = helper.ellipsis(this.font, label, this.labelWidth);

            // draw the name of the device
            ctx.beginPath();
            ctx.font         = this.font.toFontName();
            ctx.fillStyle    = "#616161";
            ctx.textBaseline = "middle";
            ctx.fillText(label, 0, (y + (rectHeight / 2)));
        }

        // Before drawing data, cover with the no-data pattern
        helper.fillPattern(this.noDataPattern, this.labelWidth, y, width - this.labelWidth, rectHeight);
        if (source.dataSource.ranges?.length)
        {
            let sourceClip = new ChartBox(this.labelWidth, y, width - this.labelWidth, rectHeight);
            let transform  = new ValueTransform(this.domain, sourceClip);

            // Draw all data colors
            let mapper = this.colorMapper || this.calculatedColorMapper;
            for (let range of source.dataSource.ranges)
            {
                let rectWidth = 0;

                for (let pos = 0; pos < range.size; pos++)
                {
                    let type = range.toType(pos);

                    // get rect color
                    let color = this.missingValueColor;
                    if (type == ChartPointType.Value) color = mapper.getColor(range.toNumericValue(pos));

                    // get rect start point
                    let xCoord = Math.floor(transform.fromMillisecondToXCoordinate(range.toTimestamp(pos)));
                    if (xCoord >= transform.fromMillisecondToXCoordinate(this.domain.endMillisecond)) continue;

                    // get rect width
                    if (pos + 1 < range.size)
                    {
                        let xCoordNext = Math.floor(transform.fromMillisecondToXCoordinate(range.toTimestamp(pos + 1)));
                        rectWidth      = Math.ceil(xCoordNext - xCoord);
                    }
                    else
                    {
                        rectWidth = HeatmapComponent.lastSampleWidth; // For last sample, assume fixed size.
                    }

                    if (xCoord + rectWidth < 0) continue;

                    // draw the rect
                    if (type == ChartPointType.Value)
                    {
                        ctx.fillStyle = color;
                        ctx.fillRect(Math.round(xCoord), Math.round(transform.clip.y), Math.round(rectWidth), Math.round(rectHeight));
                    }
                    else
                    {
                        // Canvas pattern fills do not play well with high DPI canvas
                        // elements, so we use a alternate pattern fill implementation
                        helper.fillPattern(this.noValuePattern, xCoord, transform.clip.y, rectWidth, rectHeight);
                    }
                }
            }
        }
    }

    registerEvents()
    {
        let ctx: CanvasRenderingContext2D = this.interactiveCanvas.nativeElement.getContext("2d");

        this.subscribeToObservable(fromEvent(ctx.canvas, "mouseleave"), () => this.handleMouseleave(ctx));
        this.subscribeToObservable(fromEvent(ctx.canvas, "mousemove"), (e: MouseEvent) => this.handleMouseMove(ctx, e.offsetX, e.offsetY));
        this.subscribeToObservable(fromEvent(ctx.canvas, "contextmenu"), async (event) =>
        {
            event.preventDefault();
            event.stopPropagation();

            if (this.sources)
            {
                let rowHeight = Math.floor(ctx.canvas.height / this.sources.length);
                let rowIndex  = Math.floor(event.offsetY / rowHeight);
                let source    = this.processedSources[rowIndex];

                if (source?.dataSource?.ranges?.length > 0 && source.dataSource.ranges[0].size > 0)
                {
                    if (event.offsetX >= this.labelWidth)
                    {
                        let sampleY    = rowHeight * rowIndex;
                        let sourceClip = new ChartClipArea(this.labelWidth, sampleY, (ctx.canvas.width - this.labelWidth), rowHeight);
                        let transform  = new ValueTransform(this.domain, sourceClip);

                        let samplePair = transform.fromPixelToSample(source, event.offsetX, HeatmapComponent.lastSampleWidth);
                        if (samplePair)
                        {
                            ContextMenuComponent.positionMenu(this.contextMenuTriggerWrapper.nativeElement, this.chartStage.nativeElement, event);

                            let root = new ContextMenuItemComponent();
                            if (await source.dataSource.provider.prepareContextMenu(samplePair.point, root))
                            {
                                this.contextMenu.open(root.subMenuItems);
                            }
                        }
                    }
                }
            }
        });
        this.subscribeToObservable(fromEvent(ctx.canvas, "wheel"), async (e: WheelEvent) =>
        {
            let offsetX = e.offsetX;
            let offsetY = e.offsetY;

            let canvasWidth  = ctx.canvas.width;
            let canvasHeight = ctx.canvas.height;

            if (this.sources)
            {
                let rowHeight = Math.floor(canvasHeight / this.sources.length);
                let rowIndex  = Math.floor(offsetY / rowHeight);
                let source    = this.processedSources[rowIndex];

                if (source?.dataSource?.ranges?.length && source?.dataSource?.ranges[0].size > 0)
                {
                    if (offsetX >= this.labelWidth)
                    {
                        let sampleY    = rowHeight * rowIndex;
                        let sourceClip = new ChartClipArea(this.labelWidth, sampleY, (canvasWidth - this.labelWidth), rowHeight);
                        let transform  = new ChartValueTransform(this.domain, sourceClip);

                        if (this.zoomState?.handleScrollEventWithTransform(e, transform))
                        {
                            this.clearTooltip();
                        }
                    }
                }
            }
        });
    }

    private handleMouseMove(ctx: CanvasRenderingContext2D,
                            offsetX: number,
                            offsetY: number)
    {
        let canvasWidth  = ctx.canvas.width;
        let canvasHeight = ctx.canvas.height;
        ctx.clearRect(0, 0, canvasWidth, canvasHeight);

        if (this.sources)
        {
            let rowHeight = Math.floor(canvasHeight / this.sources.length);
            let rowIndex  = Math.floor(offsetY / rowHeight);
            let source    = this.processedSources[rowIndex];

            if (source?.dataSource?.ranges?.length && source?.dataSource?.ranges[0].size > 0)
            {
                if (offsetX >= this.labelWidth)
                {
                    let sampleY    = rowHeight * rowIndex;
                    let sourceClip = new ChartClipArea(this.labelWidth, sampleY, (canvasWidth - this.labelWidth), rowHeight);
                    let transform  = new ValueTransform(this.domain, sourceClip);

                    let samplePair = transform.fromPixelToSample(source, offsetX, HeatmapComponent.lastSampleWidth);
                    if (samplePair)
                    {
                        let tooltipHtml = source.dataSource.getTooltip(samplePair.point);

                        let x        = transform.fromMillisecondToXCoordinate(samplePair.point.timestampInMillisec);
                        let sampleX  = Math.floor(x);
                        let sampleX2 = Math.floor(x + samplePair.rectWidth);

                        // highlight the sample
                        sourceClip.applyClipping(ctx, () =>
                        {
                            const lineWidth   = 2;
                            let halfLineWidth = lineWidth / 2;

                            ctx.beginPath();
                            ctx.strokeStyle = "white";
                            ctx.lineWidth   = lineWidth;

                            // borders
                            ctx.moveTo(sampleX + halfLineWidth, sampleY);
                            ctx.lineTo(sampleX + halfLineWidth, sampleY + canvasHeight);
                            ctx.moveTo(sampleX2 - halfLineWidth, sampleY);
                            ctx.lineTo(sampleX2 - halfLineWidth, sampleY + canvasHeight);

                            // diagonals
                            const diagonalSpacing = 10;
                            const diagonalSlope   = 2 / 3;
                            let xDiff             = sampleX2 - sampleX;
                            let yDiff             = xDiff * diagonalSlope;
                            let dashY             = sampleY + diagonalSpacing / 2;
                            while (dashY - yDiff <= sampleY + canvasHeight)
                            {
                                ctx.moveTo(sampleX, dashY);
                                ctx.lineTo(sampleX2, dashY - yDiff);
                                dashY += diagonalSpacing;
                            }

                            ctx.closePath();
                            ctx.stroke();
                        });

                        this.chartTooltip.render(offsetX, offsetY, this.sanitizeHtml(tooltipHtml));
                        return;
                    }
                }
                else if (!this.hideLabels)
                {
                    this.chartTooltip.render(offsetX, rowHeight * rowIndex, source.dataSource.label);
                    return;
                }
            }
        }

        // Nothing matches, remove tooltip.
        this.chartTooltip.remove();
    }

    private clearTooltip()
    {
        if (this.chartTooltip)
        {
            this.chartTooltip.remove();
        }
    }

    private handleMouseleave(ctx: CanvasRenderingContext2D)
    {
        this.clearTooltip();

        ctx.clearRect(0, 0, ctx.canvas.width, ctx.canvas.height);
    }

    onMouseMove(x: number,
                y: number): boolean
    {
        this.handleMouseMove(this.interactiveCanvas.nativeElement.getContext("2d"), x, y);
        return true;
    }

    onMouseLeave()
    {
        this.handleMouseleave(this.interactiveCanvas.nativeElement.getContext("2d"));
    }
}

class ProcessedDataSource
{
    readonly timeRange: ChartTimeRange   = new ChartTimeRange();
    readonly valueRange: ChartValueRange = new ChartValueRange();

    constructor(public readonly dataSource: ChartPointSource<any>,
                sharedRange: ChartValueRange)
    {
        // Get the x range of the actual data
        this.timeRange = dataSource.extractMinMaxTimestamps();

        if (!isNaN(sharedRange?.diff))
        {
            this.valueRange = new ChartValueRange(sharedRange.min, sharedRange.max);
        }
        else
        {
            for (let range of dataSource.ranges)
            {
                this.timeRange.expandRange(range.minDate, range.maxDate);

                for (let index = 0; index < range.size; index++)
                {
                    // Get the y range of the actual data
                    this.valueRange.expandForValue(range.toNumericValue(index));
                }
            }
        }
    }
}

class ValueTransform
{
    private xRangeInMillisec: number;
    private xRatio: number;

    constructor(public domain: ChartTimeWindow,
                public clip: ChartBox)
    {
        this.xRangeInMillisec = domain.timeRangeInMilliseconds;
        this.xRatio           = this.xRangeInMillisec != 0 ? clip.width / this.xRangeInMillisec : 0;
    }

    fromMillisecondToXCoordinate(x: number): number
    {
        return this.clip.x + (x - this.domain.startMillisecond) * this.xRatio;
    }

    fromPixelToSample(source: ProcessedDataSource,
                      x: number,
                      lastSampleWidth: number): { point: ChartPoint<any>, rectWidth: number }
    {
        if (this.xRatio == 0) return null;

        x -= this.clip.x;

        if (x < 0 || x > this.clip.width) return null;

        let numRanges = source.dataSource.ranges?.length || 0;

        let timestamp = this.domain.startMillisecond + (x / this.xRatio);

        for (let rangeIdx = 0; rangeIdx < numRanges; rangeIdx++)
        {
            let range = source.dataSource.ranges[rangeIdx];

            if (range.minDate > timestamp) continue;

            if (timestamp <= range.maxDate)
            {
                let pos = range.findPointIndex(timestamp, false);
                if (pos !== null)
                {
                    let point = range.toPoint(pos);

                    if (pos + 1 < range.size)
                    {
                        let pointPlus1 = range.toPoint(pos + 1);

                        return {
                            point    : point,
                            rectWidth: this.xRatio * (pointPlus1.timestampInMillisec - point.timestampInMillisec)
                        };
                    }

                    return {
                        point    : point,
                        rectWidth: lastSampleWidth
                    };
                }
            }
            else if (rangeIdx == numRanges - 1)
            {
                let point     = range.toPoint(range.size - 1);
                let rectWidth = this.xRatio * (timestamp - point.timestampInMillisec);

                if (rectWidth <= lastSampleWidth)
                {
                    return {
                        point    : point,
                        rectWidth: lastSampleWidth
                    };
                }
            }
        }

        return null;
    }
}
