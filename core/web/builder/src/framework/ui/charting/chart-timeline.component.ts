import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {fromTouchEvent, MouseTouchEvent, UtilsService} from "framework/services/utils.service";
import {ChartAnimation, ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ProcessedDataSource} from "framework/ui/charting/chart.component";
import {BoxAnchor, ChartClipArea, ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartValueTransform} from "framework/ui/charting/core/data-sources";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {ChartTimeRange, ChartTimeWindow} from "framework/ui/charting/core/time";
import {BaseComponent} from "framework/ui/components";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import moment from "framework/utils/moment";

import {Subject} from "rxjs";

@Component({
               selector       : "o3-chart-timeline[timeRange]",
               templateUrl    : "./chart-timeline.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ChartTimelineComponent extends BaseComponent
{
    public static readonly MIN_SCRUBBER_WIDTH          = 3;
    public static readonly MIN_STANDING_SCRUBBER_WIDTH = 30;

    private static readonly MIN_WIDTH  = 120;
    private static readonly MIN_HEIGHT = 15;

    private m_zoomState: ChartZoomState;
    get zoomState(): ChartZoomState
    {
        if (!this.m_zoomState && this.m_timeRange)
        {
            this.m_zoomState = new ChartZoomState(this, this.m_timeRange, this.zoomedRange);
            this.zoomStateGenerated.emit(this.m_zoomState);
            this.subscribeToObservable(this.m_zoomState.stateChanged, () =>
            {
                this.m_stateChanged = true;
                this.redrawCanvas();
            });
        }

        return this.m_zoomState;
    }

    private m_timeRange: ChartTimeRange;
    @Input() set timeRange(timeRange: ChartTimeRange)
    {
        if (ChartTimeRange.isValid(timeRange))
        {
            timeRange = timeRange.clone();
            if (timeRange.diffAsMs < this.minimumRangeMs)
            {
                timeRange.maxInMillisec = timeRange.minInMillisec + this.minimumRangeMs;
            }

            this.m_timeRange = timeRange;
            if (this.m_zoomState)
            {
                this.m_zoomState.updateTimeRange(this.m_timeRange);
            }
        }
    }

    @Input() zoomedRange: ChartTimeRange;

    get timeRange(): ChartTimeRange
    {
        return this.zoomState.outerRange;
    }

    get zoomRange(): ChartTimeRange
    {
        return this.zoomState.zoomRange;
    }

    get displayedRange(): ChartTimeRange
    {
        return this.zoomState.displayedRange;
    }

    @Input() set zoomSource(zoomSource: ProcessedDataSource)
    {
        this.m_zoomSource = zoomSource;
        this.reportConfigurationChanges();
    }

    get zoomSource(): ProcessedDataSource
    {
        return this.m_zoomSource;
    }

    @Input() scrubContainerSelector = "body";
    @Input() zone: string;
    @Input() minimumRangeMs: number = 10;
    @Input() offsetLeft             = 0;
    @Input() offsetRight            = 0;

    @Output() scrubberDragStarted = new EventEmitter<string>();
    @Output() scrubberDragEnded   = new EventEmitter<void>();
    @Output() zoomStateGenerated  = new EventEmitter<ChartZoomState>();

    @ViewChild("timelineContainer", {static: true}) containerRef: ElementRef;
    @ViewChild("timeline", {static: true}) canvasRef: ElementRef;

    debouncingWidth: number;
    debouncingHeight: number;

    get widthPx(): number
    {
        return this.m_containerElement?.clientWidth;
    }

    get heightPx(): number
    {
        return this.m_containerElement?.clientHeight;
    }

    private m_containerElement: HTMLDivElement;
    private m_timelineElement: HTMLCanvasElement;

    zoomNeedsRebalance     = true;
    private m_stateChanged = true;

    private m_zoomSource: ProcessedDataSource;
    private m_domainForZoomArea: ChartTimeWindow;

    clipForZoomAreaOuter: ChartClipArea;
    clipForZoomAreaInner: ChartClipArea;
    transformForZoomArea: ChartValueTransform;

    private m_zoomTickInterval: TimeInterval;
    private m_zoomTicks: ChartTimelineTick[];

    private m_zoomHandler: ZoomHandler;

    private m_zoomCenterDecimal: number;
    private m_zoomAnimationTimer: number;
    private m_zoomAnimationTrigger: () => void;
    private m_zoomAnimation: ChartAnimation;

    constructor(inj: Injector,
                private m_elementRef: ElementRef)
    {
        super(inj);
    }

    public ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.m_containerElement = this.containerRef.nativeElement;
        this.m_timelineElement  = this.canvasRef.nativeElement;

        this.m_timelineElement.addEventListener("mousemove", (e: MouseEvent) => this.zoomState?.handleMoveEvent(e));

        this.subscribeToMouseDrag(this.m_timelineElement, (e: MouseEvent,
                                                           mouseDown: boolean,
                                                           mouseUp: boolean) => this.handleDrag(e, mouseDown, mouseUp));
        this.subscribeToTouchDrag(this.m_timelineElement, (e: TouchEvent,
                                                           touchStart: boolean,
                                                           touchEnd: boolean) => this.handleDrag(fromTouchEvent(e, this.m_timelineElement), touchStart, touchEnd));

        this.refreshSize();
    }

    private stateIsReadyForCanvas(): boolean
    {
        return this.m_containerElement?.clientWidth >= ChartTimelineComponent.MIN_WIDTH && this.m_containerElement.clientHeight >= ChartTimelineComponent.MIN_HEIGHT &&
               this.zoomState && ChartTimeRange.isValid(this.timeRange) && ChartTimeRange.isValid(this.displayedRange);
    }

    protected afterConfigurationChanges(): void
    {
        super.afterConfigurationChanges();

        ChartHelpers.scaleCanvas(this.m_timelineElement, this.debouncingWidth, this.debouncingHeight);

        if (this.stateIsReadyForCanvas())
        {
            this.m_stateChanged = true;
            if (!this.zoomState.zoomingViaScrubber)
            {
                this.zoomNeedsRebalance  = true;
                this.m_zoomCenterDecimal = this.getZoomCenter();
            }

            this.redrawCanvas();
        }
    }

    public setCursor(cursor: string)
    {
        this.m_elementRef.nativeElement.style.cursor = cursor || "";
    }

    public refreshSize(): boolean
    {
        let width  = this.m_containerElement.offsetWidth;
        let height = this.m_containerElement.offsetHeight;
        if (this.debouncingWidth != width || this.debouncingHeight != height)
        {
            this.debouncingWidth  = width;
            this.debouncingHeight = height;

            this.reportConfigurationChanges();
        }

        return this.stateIsReadyForCanvas() && !!this.debouncingWidth && !!this.debouncingHeight;
    }

    private redrawCanvas()
    {
        if (this.zoomNeedsRebalance)
        {
            this.clearZoomTimer();
            this.endZoomAnimation();

            this.updateOuterZoomRange();
            this.zoomNeedsRebalance = false;
        }

        this.zoomState.alignRanges(true);

        if (this.m_stateChanged)
        {
            this.clipForZoomAreaOuter = new ChartClipArea(0, 0, this.widthPx, this.heightPx);
            this.clipForZoomAreaInner = new ChartClipArea(this.offsetLeft, 0, this.widthPx - (this.offsetLeft + this.offsetRight), this.heightPx);
            this.transformForZoomArea = new ChartValueTransform(this.m_domainForZoomArea, this.clipForZoomAreaInner);

            this.m_stateChanged = false;
        }

        let canvas = this.canvasRef.nativeElement.getContext("2d");
        canvas.clearRect(0, 0, this.widthPx, this.heightPx);
        this.renderZoom(new ChartHelpers(canvas));
    }

    public triggerZoomAnimation()
    {
        if (this.clearZoomTimer())
        {
            this.m_zoomAnimationTrigger();
        }
    }

    private clearZoomTimer(): boolean
    {
        if (this.m_zoomAnimationTimer)
        {
            clearTimeout(this.m_zoomAnimationTimer);
            this.m_zoomAnimationTimer = null;
            return true;
        }

        return false;
    }

    private endZoomAnimation()
    {
        if (this.m_zoomAnimation)
        {
            this.m_zoomAnimation.endAnimation();
            this.m_zoomAnimation = null;
        }
    }

    private updateOuterZoomRange(outerZoomRange?: ChartTimeRange)
    {
        if (!outerZoomRange)
        {
            outerZoomRange                   = ChartTimelineComponent.computeOuterZoomRange(this.widthPx, this.timeRange.diffAsMs, this.displayedRange, this.zoomState.zoomCenterDecimal ?? 0.5);
            this.zoomState.zoomCenterDecimal = null;
        }

        this.zoomState.zoomRange = outerZoomRange;

        this.prepareDomainForZoom();
    }

    private prepareDomainForZoom()
    {
        const expandFactor = 0.1;

        let zoomRange  = this.zoomRange;
        let valueRange = new ChartValueRange();
        if (this.m_zoomSource)
        {
            this.m_zoomSource.updateValueRangeForPointsInTimeRange(valueRange, zoomRange.minAsMoment, zoomRange.maxAsMoment, this.widthPx, this.heightPx);
        }

        let yRange   = valueRange.diff;
        let minValue = valueRange.min - yRange * (expandFactor / 2);
        let maxValue = valueRange.max + yRange * (expandFactor / 2);

        this.m_domainForZoomArea = new ChartTimeWindow(zoomRange.minInMillisec, zoomRange.maxInMillisec, minValue, maxValue);

        let bestFit             = TimeInterval.calculateBestTickFit(this.widthPx, zoomRange.diffAsMs / 1000);
        this.m_zoomTickInterval = bestFit.handler;
        this.m_zoomTicks        = bestFit.handler.computeTicks(zoomRange, this.zone, bestFit.step);
    }

    private renderZoom(helper: ChartHelpers)
    {
        if (this.transformForZoomArea)
        {
            let font           = new ChartFont();
            let canvas         = helper.canvas;
            let displayedRange = this.displayedRange;

            let zoomMin = this.transformForZoomArea.fromMillisecondToXCoordinate(displayedRange.minInMillisec);
            let zoomMax = this.transformForZoomArea.fromMillisecondToXCoordinate(displayedRange.maxInMillisec);
            this.clipForZoomAreaInner.applyClipping(canvas, () =>
            {
                // Show the zoomed area.
                let width             = this.clipForZoomAreaInner.width;
                let topBorder: number = this.clipForZoomAreaInner.y;
                let height: number    = this.clipForZoomAreaInner.height;
                let bottomBorder      = topBorder + height;

                if (this.m_zoomSource)
                {
                    // Show the mini view of the main data.
                    this.m_zoomSource.renderArea(canvas, this.transformForZoomArea, "#D8D8D8", 0.85);
                    this.m_zoomSource.renderLine(canvas, this.transformForZoomArea, 1, "#D8D8D8", "#D8D8D8", "#D8D8D8", 1);
                }

                // Show the ticks on the mini view.
                canvas.globalAlpha = 0.75;
                for (let tick of this.m_zoomTicks)
                {
                    let x         = this.transformForZoomArea.fromTimestampToXCoordinate(tick.moment);
                    let labelFont = font.clone();

                    labelFont.size  = 11;
                    labelFont.style = "bold";

                    helper.drawTextInBox(labelFont, TextPlacement.Center, TextOrientation.Horizontal,
                                         this.m_zoomTickInterval.convertToString(tick.moment), "#808080", x,
                                         bottomBorder, BoxAnchor.Bottom, 5);
                }

                // Highlight the selected area
                canvas.globalAlpha = 0.5;
                canvas.fillStyle   = "#E0E0E0";
                canvas.fillRect(zoomMin, this.clipForZoomAreaInner.y, zoomMax - zoomMin, height);

                canvas.globalAlpha = 1;
                canvas.lineWidth   = 1;
                canvas.strokeStyle = "#DDDDDD";

                canvas.beginPath();
                canvas.moveTo(0, this.clipForZoomAreaInner.y);
                canvas.lineTo(zoomMin, this.clipForZoomAreaInner.y);
                canvas.lineTo(zoomMin, this.clipForZoomAreaInner.y + height);
                canvas.stroke();

                canvas.beginPath();
                canvas.moveTo(width, this.clipForZoomAreaInner.y);
                canvas.lineTo(zoomMax, this.clipForZoomAreaInner.y);
                canvas.lineTo(zoomMax, this.clipForZoomAreaInner.y + height);
                canvas.stroke();

                canvas.globalAlpha = 1;
                canvas.lineWidth   = 3;
                canvas.strokeStyle = "#258cbb";

                canvas.beginPath();
                canvas.moveTo(zoomMin, this.clipForZoomAreaInner.y + height);
                canvas.lineTo(zoomMax, this.clipForZoomAreaInner.y + height);
                canvas.stroke();
            });

            this.clipForZoomAreaOuter.applyClipping(canvas, () =>
            {
                let topBorder: number = this.clipForZoomAreaInner.y;
                let height: number    = this.clipForZoomAreaInner.height;

                // because the check for hits gives priority to the min knob, we render the max knob first.
                ZoomHandler.render(helper, zoomMax, topBorder, height);
                ZoomHandler.render(helper, zoomMin, topBorder, height);
            });
        }
    }

    public handleDrag(e: MouseTouchEvent,
                      mouseDown: boolean,
                      mouseUp: boolean)
    {
        if (!this.zoomState) return;
        if (!e && !mouseUp) return;

        if (this.m_zoomHandler)
        {
            this.handleZoom(e, mouseDown, mouseUp);
        }
        else if (mouseDown)
        {
            let zoomHit = ZoomHandler.hitCheck(this.transformForZoomArea, this.clipForZoomAreaOuter, this.displayedRange, e.offsetX, e.offsetY, this.heightPx);
            if (zoomHit)
            {
                this.handleZoom(e, mouseDown, mouseUp, zoomHit);
            }
        }
    }

    private handleZoom(e: MouseTouchEvent,
                       mouseDown: boolean,
                       mouseUp: boolean,
                       zoomHit?: ZoomHit)
    {
        if (mouseDown)
        {
            this.m_zoomHandler = null;
            if (zoomHit)
            {
                let zoomRange          = this.zoomRange;
                let displayedTimeRange = this.displayedRange;

                this.endZoomAnimation();

                let cursor: string;
                if (zoomHit.hitRegion == 0)
                {
                    let currentMs = this.transformForZoomArea.fromXCoordinateToMillisecond(e.offsetX);
                    if (currentMs)
                    {
                        let initialTimeRange = displayedTimeRange.clone();
                        let initialDiff      = initialTimeRange.diffAsMs;

                        this.m_zoomHandler = new ZoomHandler(0, (time) =>
                        {
                            let timeMillisec = time.valueOf();
                            let diff         = timeMillisec - currentMs;

                            let newMin = initialTimeRange.minInMillisec + diff;
                            let newMax = initialTimeRange.maxInMillisec + diff;
                            if (newMin < zoomRange.minInMillisec)
                            {
                                newMin = zoomRange.minInMillisec;
                                newMax = newMin + initialDiff;
                            }
                            else if (newMax > zoomRange.maxInMillisec)
                            {
                                newMax = zoomRange.maxInMillisec;
                                newMin = newMax - initialDiff;
                            }

                            displayedTimeRange.minInMillisec = newMin;
                            displayedTimeRange.maxInMillisec = newMax;
                            this.zoomState.stateChanged.next(true);
                        });
                        cursor             = "move";
                    }
                }
                else
                {
                    let scrubHandler: (time: moment.Moment) => void;
                    if (zoomHit.hitRegion < 0)
                    {
                        scrubHandler = (time) =>
                        {
                            let clampHigh     = displayedTimeRange.maxInMillisec - Math.max(this.minimumRangeMs, ChartTimelineComponent.MIN_SCRUBBER_WIDTH * zoomRange.diffAsMs / this.widthPx);
                            let minInMillisec = UtilsService.clamp(zoomRange.minInMillisec, clampHigh, time.valueOf());
                            if (minInMillisec !== displayedTimeRange.minInMillisec)
                            {
                                displayedTimeRange.minInMillisec = minInMillisec;
                                this.zoomState.stateChanged.next(true);
                            }
                        };
                    }
                    else
                    {
                        scrubHandler = (time) =>
                        {
                            let clampLow      = displayedTimeRange.minInMillisec + Math.max(this.minimumRangeMs, ChartTimelineComponent.MIN_SCRUBBER_WIDTH * zoomRange.diffAsMs / this.widthPx);
                            let maxInMillisec = UtilsService.clamp(clampLow, zoomRange.maxInMillisec, time.valueOf());
                            if (maxInMillisec !== displayedTimeRange.maxInMillisec)
                            {
                                displayedTimeRange.maxInMillisec = maxInMillisec;
                                this.zoomState.stateChanged.next(true);
                            }
                        };
                    }

                    this.m_zoomHandler = new ZoomHandler(zoomHit.skew, scrubHandler);
                    cursor             = "grabbing";
                }

                this.zoomState.zoomingViaScrubber = true;
                this.setCursor(cursor);
                this.scrubberDragStarted.emit(cursor);
            }
        }
        else if (mouseUp)
        {
            this.m_zoomHandler = null;

            let outerZoomDiff          = this.zoomRange.diffAsMs;
            let projectedScrubberWidth = this.widthPx * this.displayedRange.diffAsMs / outerZoomDiff;
            if (projectedScrubberWidth < ChartTimelineComponent.MIN_STANDING_SCRUBBER_WIDTH || this.zoomRange !== this.timeRange)
            {
                this.queueZoomAnimation(this.getZoomCenter());
            }

            this.zoomState.zoomingViaScrubber = false;
            if (e) this.zoomState.handleMoveEvent(e);
            this.scrubberDragEnded.emit();
            this.zoomState.stateChanged.next(true);
        }
        else if (this.m_zoomHandler)
        {
            if (e.target === this.m_timelineElement || this.scrubContainerSelector && this.m_timelineElement.closest(this.scrubContainerSelector))
            {
                let offsetX      = e.clientX - this.m_timelineElement.getBoundingClientRect().x;
                let cursorMoment = MomentHelper.parse(this.transformForZoomArea.fromXCoordinateToMillisecond(offsetX + this.m_zoomHandler.skewX));
                cursorMoment     = MomentHelper.toZone(cursorMoment, this.zone);
                this.m_zoomHandler.callback(cursorMoment);
            }
        }
    }

    private getZoomCenter(): number
    {
        let displayedCenter = (this.displayedRange.minInMillisec + this.displayedRange.maxInMillisec) / 2;
        return (displayedCenter - this.zoomRange.minInMillisec) / this.zoomRange.diffAsMs;
    }

    public queueZoomAnimation(zoomCenterDecimal: number)
    {
        let startOuterZoom = this.zoomRange;
        let endOuterZoom   = ChartTimelineComponent.computeOuterZoomRange(this.widthPx, this.timeRange.diffAsMs, this.displayedRange, zoomCenterDecimal);
        if (!startOuterZoom.isSame(endOuterZoom || this.timeRange))
        {
            this.clearZoomTimer();

            this.m_zoomAnimationTrigger = () =>
            {
                let effectiveOuterZoom   = endOuterZoom || this.timeRange;
                let minDiff              = effectiveOuterZoom.minInMillisec - startOuterZoom.minInMillisec;
                let maxDiff              = effectiveOuterZoom.maxInMillisec - startOuterZoom.maxInMillisec;
                this.m_zoomAnimation     = new ChartAnimation((progressDecimal) =>
                                                              {
                                                                  let outerRange;
                                                                  if (progressDecimal < 1)
                                                                  {
                                                                      outerRange = new ChartTimeRange(startOuterZoom.minInMillisec + minDiff * progressDecimal,
                                                                                                      startOuterZoom.maxInMillisec + maxDiff * progressDecimal);
                                                                  }
                                                                  else
                                                                  {
                                                                      outerRange = endOuterZoom;
                                                                  }

                                                                  this.updateOuterZoomRange(outerRange);
                                                                  this.m_stateChanged = true;
                                                                  this.redrawCanvas();
                                                              });
                this.m_zoomCenterDecimal = zoomCenterDecimal;

                this.m_zoomAnimationTrigger = null;
                this.m_zoomAnimationTimer   = null;
            };
            this.m_zoomAnimationTimer   = setTimeout(this.m_zoomAnimationTrigger, 500);
        }
    }

    //--//

    public static computeOuterZoomRange(chartWidth: number,
                                        overallTimeDiff: number,
                                        displayedRange: ChartTimeRange,
                                        zoomCenterDecimal: number): ChartTimeRange
    {
        let projectedScrubberWidth = chartWidth * displayedRange.diffAsMs / overallTimeDiff;
        if (projectedScrubberWidth < ChartTimelineComponent.MIN_STANDING_SCRUBBER_WIDTH && zoomCenterDecimal)
        {
            let outerZoomDiff = overallTimeDiff * projectedScrubberWidth / ChartTimelineComponent.MIN_STANDING_SCRUBBER_WIDTH;
            let leftOffset    = outerZoomDiff * zoomCenterDecimal;
            let rightOffset   = outerZoomDiff - leftOffset;
            let centerViewMs  = (displayedRange.minInMillisec + displayedRange.maxInMillisec) / 2;
            return new ChartTimeRange(centerViewMs - leftOffset, centerViewMs + rightOffset);
        }

        return null;
    }
}

export class ChartZoomState
{
    private m_outerRange: ChartTimeRange;
    private m_zoomRange: ChartTimeRange;
    private m_displayedRange: ChartTimeRange;

    set outerRange(range: ChartTimeRange)
    {
        this.m_outerRange     = range;
        this.m_zoomRange      = null;
        this.m_displayedRange = range?.clone();
    }

    set zoomRange(range: ChartTimeRange)
    {
        this.m_zoomRange = range;
    }

    get outerRange(): ChartTimeRange
    {
        return this.m_outerRange;
    }

    get zoomRange(): ChartTimeRange
    {
        return this.m_zoomRange || this.m_outerRange;
    }

    get displayedRange(): ChartTimeRange
    {
        return this.m_displayedRange;
    }

    get zoomable(): boolean
    {
        return !!this.m_timeline;
    }

    zoomingViaScrubber = false;
    zoomCenterDecimal: number;

    stateChanged = new Subject<boolean>();

    constructor(private m_timeline: ChartTimelineComponent,
                outerRange: ChartTimeRange,
                displayedRange?: ChartTimeRange)
    {
        this.outerRange = outerRange;
        if (this.m_timeline)
        {
            if (displayedRange)
            {
                this.m_displayedRange.minInMillisec = displayedRange.minInMillisec;
                this.m_displayedRange.maxInMillisec = displayedRange.maxInMillisec;
            }
        }
    }

    public updateTimeRange(range: ChartTimeRange)
    {
        this.outerRange = range;
        this.stateChanged.next(false);
    }

    public mousemoveOnChart()
    {
        if (this.m_timeline)
        {
            this.m_timeline.triggerZoomAnimation();
        }
    }

    public handleMoveEvent(e: MouseTouchEvent)
    {
        if (!this.zoomingViaScrubber)
        {
            let hit = ZoomHandler.hitCheck(this.m_timeline.transformForZoomArea, this.m_timeline.clipForZoomAreaOuter, this.m_displayedRange, e.offsetX, e.offsetY, this.m_timeline.heightPx);
            if (hit)
            {
                if (hit.hitRegion === 0)
                {
                    this.m_timeline.setCursor("move");
                }
                else
                {
                    this.m_timeline.setCursor("grab");
                }
            }
            else
            {
                this.m_timeline.setCursor(null);
            }
        }
    }

    public handleScrollEvent(e: WheelEvent): boolean
    {
        return this.handleScrollEventWithTransform(e, this.m_timeline?.zoomSource?.group.transform);
    }

    public handleScrollEventWithTransform(e: WheelEvent,
                                          transform: ChartValueTransform): boolean
    {
        if (!transform) return false;

        // Deal with the differences between browsers.
        let movement: number;
        if (e.detail)
        {
            movement = -120 * e.detail;
        }
        else
        {
            movement = -e.deltaY || 0;
        }

        if (movement != 0)
        {
            let focus = transform.fromXCoordinateToMillisecond(e.offsetX);
            if (focus)
            {
                const scale  = 20;
                let ratio    = 1 + (scale / 100);
                let minRange = this.m_timeline.minimumRangeMs;

                let min = this.displayedRange.minInMillisec;
                let max = this.displayedRange.maxInMillisec;

                this.zoomCenterDecimal = (focus - min) / (max - min);

                if (movement > 0)
                {
                    if (max - min <= minRange) return false;

                    ratio = 1 / ratio;
                }

                let leftOffset  = (min - focus) * ratio;
                let rightOffset = (max - focus) * ratio;
                let viewRange   = rightOffset - leftOffset;

                if (viewRange < minRange)
                {
                    let msToRedistribute = minRange - viewRange;
                    let rightProportion  = rightOffset / viewRange;
                    let leftProportion   = 1 - rightProportion;

                    leftOffset -= leftProportion * msToRedistribute;
                    rightOffset += rightProportion * msToRedistribute;
                }

                this.displayedRange.minInMillisec  = leftOffset + focus;
                this.displayedRange.maxInMillisec  = rightOffset + focus;
                this.m_timeline.zoomNeedsRebalance = true;
                this.stateChanged.next(true);

                e.preventDefault();
                e.stopPropagation();

                return true;
            }
        }

        return false;
    }

    public shiftToContain(timestampMs: number)
    {
        let maxMs = this.m_outerRange.maxInMillisec;
        if (maxMs < timestampMs)
        {
            this.m_outerRange.shift(timestampMs - maxMs);

            this.alignRanges();
        }
    }

    public alignRanges(preventEmit?: boolean)
    {
        let changed = false;
        if (this.zoomRange !== this.m_outerRange)
        {
            let start = this.m_zoomRange.clone();
            if (!this.m_outerRange.shiftToBeContained(this.zoomRange))
            {
                this.m_zoomRange = this.m_outerRange.clone();
            }
            changed = !start.isSame(this.m_zoomRange);
        }

        let start = this.m_displayedRange.clone();
        if (!this.zoomRange.shiftToBeContained(this.displayedRange))
        {
            this.m_displayedRange = this.zoomRange.clone();
        }

        if (changed || !start.isSame(this.displayedRange))
        {
            if (this.m_timeline) this.m_timeline.zoomNeedsRebalance = true;

            if (!preventEmit) this.stateChanged.next(false);
        }
    }
}

class ZoomHandler
{
    public static readonly handleWidthPx        = 13;
    private static readonly handleHeightDecimal = 0.6;

    constructor(public skewX: number,
                public callback: (time: moment.Moment) => void)
    {
    }

    public static hitCheck(transform: ChartValueTransform,
                           outerClip: ChartClipArea,
                           timeRange: ChartTimeRange,
                           x: number,
                           y: number,
                           height: number): ZoomHit
    {
        if (!transform || !outerClip) return undefined;

        if (outerClip.hitCheck(x, y))
        {
            let zoomMin = transform.fromMillisecondToXCoordinate(timeRange.minInMillisec);
            if (ZoomHandler.isPointInHandle(x, y - transform.clip.y, zoomMin, height))
            {
                return {
                    hitRegion: -1,
                    skew     : zoomMin - x
                };
            }

            let zoomMax = transform.fromMillisecondToXCoordinate(timeRange.maxInMillisec);
            if (ZoomHandler.isPointInHandle(x, y - transform.clip.y, zoomMax, height))
            {
                return {
                    hitRegion: 1,
                    skew     : zoomMax - x
                };
            }
        }

        if (transform.clip.hitCheck(x, y))
        {
            return {
                hitRegion: 0,
                skew     : 0
            };
        }

        return undefined;
    }

    private static isPointInHandle(x: number,
                                   zoomOffsetY: number,
                                   handleX: number,
                                   height: number): boolean
    {
        let handleHeight = height * ZoomHandler.handleHeightDecimal;
        let halfWidth    = ZoomHandler.handleWidthPx / 2;
        return ChartHelpers.roundedRectHitCheck(x, zoomOffsetY, handleX - halfWidth, (height - handleHeight) / 2,
                                                ZoomHandler.handleWidthPx, handleHeight, halfWidth);
    }

    public static render(helper: ChartHelpers,
                         handleCenterX: number,
                         zoomWindowY: number,
                         zoomWindowHeight: number)
    {
        let canvas       = helper.canvas;
        let halfWidth    = ZoomHandler.handleWidthPx / 2;
        let handleHeight = zoomWindowHeight * ZoomHandler.handleHeightDecimal;
        let yCenter      = zoomWindowY + zoomWindowHeight / 2;
        let handleTop    = yCenter - handleHeight / 2;

        canvas.globalAlpha = 1;
        canvas.lineWidth   = 1;

        canvas.fillStyle = "white";
        helper.roundedRect(handleCenterX - halfWidth, handleTop, ZoomHandler.handleWidthPx, handleHeight, halfWidth);
        canvas.fill();

        canvas.strokeStyle = "#959595";
        helper.roundedRect(handleCenterX - halfWidth, handleTop, ZoomHandler.handleWidthPx, handleHeight, halfWidth);
        canvas.stroke();

        canvas.beginPath();
        canvas.moveTo(handleCenterX - 2, yCenter - handleHeight / 4);
        canvas.lineTo(handleCenterX - 2, yCenter + handleHeight / 4);
        canvas.moveTo(handleCenterX + 2, yCenter - handleHeight / 4);
        canvas.lineTo(handleCenterX + 2, yCenter + handleHeight / 4);
        canvas.closePath();
        canvas.stroke();
    }
}

interface ZoomHit
{
    hitRegion: number;
    skew: number;
}

//--//

export abstract class TimeInterval
{
    abstract readonly boldDayTicks: boolean;

    steps: number[];

    constructor(private readonly unitForAlignment: moment.unitOfTime.Base,
                private readonly unit: moment.unitOfTime.Base,
                private readonly stepSizeInSeconds: number,
                ...steps: number[])
    {
        this.steps = steps;
    }

    public static calculateBestTickFit(width: number,
                                       diffInSeconds: number,
                                       targetTickDistance: number = 60): TimeIntervalStep
    {
        let numberOfMajorTicksX = Math.trunc(width / targetTickDistance);
        let majorTickX          = diffInSeconds / numberOfMajorTicksX;
        if (majorTickX <= 0.5) // will use millisecond timestamps which are significantly longer: avoid overlap
        {
            numberOfMajorTicksX = Math.trunc(numberOfMajorTicksX / 1.5);
            majorTickX          = diffInSeconds / numberOfMajorTicksX;
        }

        for (let handler of KnownIntervals)
        {
            let step = handler.isGoodFit(majorTickX);
            if (step !== null)
            {
                return {
                    handler: handler,
                    step   : step
                };
            }
        }

        let handlerDefault = new YearInterval();
        return {
            handler: handlerDefault,
            step   : handlerDefault.stepSizeInSeconds
        };
    }

    public isGoodFit(stepSize: number): number
    {
        for (let step of this.steps)
        {
            let length = step * this.stepSizeInSeconds;
            if (stepSize < length)
            {
                return step;
            }
        }

        return null;
    }

    public computeTicks(outerRange: ChartTimeRange,
                        zone: string,
                        step: number,
                        innerRange?: ChartTimeRange): ChartTimelineTick[]
    {
        let curr = innerRange ? innerRange.minAsMoment : outerRange.minAsMoment;

        // If we have a timezone offset, we have to add it before computing the start of the day, then remove it.
        curr = MomentHelper.toZone(curr, zone)
                           .startOf(this.unitForAlignment);

        let ticks          = [];
        let dateInMillisec = curr.valueOf();
        let endInMillisec  = innerRange ? Math.min(innerRange.maxInMillisec, outerRange.maxInMillisec) : outerRange.maxInMillisec;
        while (dateInMillisec <= endInMillisec)
        {
            let bold = this.boldDayTicks && curr.clone()
                                                .startOf("day")
                                                .valueOf() === dateInMillisec;

            ticks.push(new ChartTimelineTick(MomentHelper.parse(dateInMillisec, zone), dateInMillisec, bold));
            curr.add(step, this.unit);

            // Correct for DST
            if (this.unit === "hour")
            {
                let off = curr.hour() % step;
                if (off === 1)
                {
                    curr.subtract(1, "hour");
                }
                else if (off === step - 1)
                {
                    curr.add(1, "hour");
                }
            }
            dateInMillisec = curr.valueOf();
        }

        return ticks;
    }

    abstract convertToString(value: moment.Moment): string;
}

export class ChartTimelineTick
{
    constructor(public readonly moment: moment.Moment,
                public readonly timestampInMillisec: number,
                public bold: boolean)
    {
    }
}

export interface TimeIntervalStep
{
    handler: TimeInterval;
    step: number;
}

class MillisecondInterval extends TimeInterval
{
    boldDayTicks = true;

    constructor()
    {
        super("second", "millisecond", 0.001, 1, 5, 10, 50, 250, 500);
    }

    public convertToString(value: moment.Moment): string
    {
        return value.format("h:mm:ss.SSS a");
    }
}

class SecondInterval extends TimeInterval
{
    boldDayTicks = true;

    constructor()
    {
        super("minute", "second", 1, 1, 2, 5, 10, 30);
    }

    public convertToString(value: moment.Moment): string
    {
        return value.format("h:mm:ss a");
    }
}

class MinuteInterval extends TimeInterval
{
    boldDayTicks = true;

    constructor()
    {
        super("hour", "minute", 60, 1, 2, 5, 10, 30);
    }

    public convertToString(value: moment.Moment): string
    {
        let value2 = MomentHelper.startOf(value, "day");

        if (value2.isSame(value))
        {
            return value.format("MMM D");
        }

        return value.format("h:mm a");
    }
}

class HourInterval extends TimeInterval
{
    boldDayTicks = true;

    constructor()
    {
        super("day", "hour", 60 * 60, 1, 2, 3, 6, 12);
    }

    public convertToString(value: moment.Moment): string
    {
        let value2 = MomentHelper.startOf(value, "day");

        if (value2.isSame(value))
        {
            return value.format("MMM D");
        }

        return value.format("h:mm a");
    }
}

class DayInterval extends TimeInterval
{
    boldDayTicks = false;

    constructor()
    {
        super("week", "day", 24 * 60 * 60, 1, 2, 5);
    }

    public convertToString(value: moment.Moment): string
    {
        let value2 = MomentHelper.startOf(value, "day");

        if (value2.isSame(value))
        {
            return value.format("MMM D");
        }

        return value.format("h:mm a");
    }
}

class WeekInterval extends TimeInterval
{
    boldDayTicks = false;

    constructor()
    {
        super("month", "week", 7 * 24 * 60 * 60, 1, 2, 3, 4);
    }

    public convertToString(value: moment.Moment): string
    {
        return value.format("MMM D");
    }
}

class MonthInterval extends TimeInterval
{
    boldDayTicks = false;

    constructor()
    {
        super("year", "month", 365 * 24 * 60 * 60 / 12, 1, 2, 3);
    }

    public convertToString(value: moment.Moment): string
    {
        return value.format("MMM");
    }
}

class YearInterval extends TimeInterval
{
    boldDayTicks = false;

    constructor()
    {
        super("year", "year", 365 * 24 * 60 * 60, 1, 2, 3);
    }

    public convertToString(value: moment.Moment): string
    {
        return value.format("YYYY");
    }
}

const KnownIntervals = [
    new MillisecondInterval(),
    new SecondInterval(),
    new MinuteInterval(),
    new HourInterval(),
    new DayInterval(),
    new WeekInterval(),
    new MonthInterval(),
    new YearInterval()
];
