import {CDK_DRAG_CONFIG, CdkDragMove} from "@angular/cdk/drag-drop";
import {DragRef, Point} from "@angular/cdk/drag-drop/drag-ref";
import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartTimelineTick, TimeInterval} from "framework/ui/charting/chart-timeline.component";
import {BoxAnchor} from "framework/ui/charting/core/basics";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import moment from "framework/utils/moment";


@Component({
               selector       : "o3-time-scrubber[range]",
               templateUrl    : "./time-scrubber.component.html",
               styleUrls      : ["./time-scrubber.component.scss"],
               providers      : [
                   {
                       provide : CDK_DRAG_CONFIG,
                       useValue: {
                           dragStartThreshold             : 0,
                           pointerDirectionChangeThreshold: 5
                       }
                   }
               ],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimeScrubberComponent extends SharedSvc.BaseApplicationComponent
{
    private static readonly selectorHeightPx: number   = 22;
    private static readonly timeZoneMinWidthPx: number = 12;
    private static readonly deadZoneRangeColor: string = "white";

    @Input() targetElement: HTMLElement;

    private m_rangeExt: RangeSelectionExtended;
    private m_range: Models.RangeSelection;
    @Input() set range(range: Models.RangeSelection)
    {
        if (range && this.m_range !== range)
        {
            this.m_range = range;
            this.updateRangeExt();
            let chartRange = this.m_rangeExt.getChartRange();
            this.timeLeft  = chartRange.minInMillisec;
            this.timeRight = chartRange.maxInMillisec;
            this.updateEmitRange();
        }
    }

    // non-overlapping inclusively bound ranges to be rendered with a white background
    @Input() deadZoneRanges: { lowMs: number, highMs: number }[];

    show: boolean = false;

    @ViewChild("scrubberContainer", {static: true}) scrubberContainerElem: ElementRef;

    @ViewChild("canvasElement", {static: true}) set canvasRef(elemRef: ElementRef)
    {
        if (elemRef)
        {
            this.canvas = elemRef.nativeElement;
            this.rebuildCanvas();
        }
    }

    readonly constrainFn = (point: Point,
                            dragRef: DragRef): Point =>
    {
        return this.constrainDrag(point);
    };

    private canvas: HTMLCanvasElement;
    private renderInfo: RenderInfo;

    formattedTimeLeft: string;
    formattedTimeRight: string;
    private timeLeft: number;
    private timeRight: number;
    private startTimeLeft: number;
    private startTimeRight: number;

    private gripOffset: number;

    private draggingLeft: boolean;

    private canvasLeft: number;
    private limitLeft: number;
    private limitRight: number;

    freeDragPositionLeft = {
        x: 0,
        y: 0
    };

    freeDragPositionRight = {
        x: 0,
        y: 0
    };

    freeDragPositionCenter = {
        x: 0,
        y: 0
    };
    rangeWidthPx: number;

    private m_dragging: boolean = false;
    get dragging(): boolean
    {
        return this.m_dragging;
    }

    set dragging(dragging: boolean)
    {
        this.m_dragging = dragging;
        if (this.m_dragging)
        {
            this.dragStarted.emit();
        }
        else
        {
            this.dragEnded.emit();
        }
    }

    @Output() timeRangeChange = new EventEmitter<ChartTimeRange>();
    @Output() dragStarted     = new EventEmitter<void>();
    @Output() dragEnded       = new EventEmitter<void>();

    protected afterConfigurationChanges(): void
    {
        super.afterConfigurationChanges();

        this.rebuildCanvas();
    }

    protected afterLayoutChange(): void
    {
        super.afterLayoutChange();

        this.rebuildCanvas();
    }

    public notifyLayoutChange()
    {
        this.afterLayoutChange();
    }

    private updateRangeExt()
    {
        this.m_rangeExt = new RangeSelectionExtended(this.m_range);
    }

    public markDragEnded()
    {
        this.startTimeRight = this.startTimeLeft = this.draggingLeft = this.limitLeft = this.limitRight = undefined;
        this.dragging       = false;
    }

    private updateTime(timestampMs: number): number
    {
        if (isNaN(timestampMs)) return timestampMs;

        let low;
        let high;
        let freeDragPosition;
        let range = this.m_rangeExt.getChartRange();
        if (this.draggingLeft)
        {
            low              = range.minInMillisec;
            high             = this.timeRight;
            freeDragPosition = this.freeDragPositionLeft;
        }
        else
        {
            low              = this.timeLeft;
            high             = range.maxInMillisec;
            freeDragPosition = this.freeDragPositionRight;
        }

        timestampMs        = UtilsService.clamp(low, high, timestampMs);
        freeDragPosition.x = this.timeToXCoordinate(timestampMs);

        this.updateCenterStyles(true);

        if (this.draggingLeft)
        {
            if (this.timeLeft !== timestampMs)
            {
                this.timeLeft          = timestampMs;
                this.formattedTimeLeft = MomentHelper.friendlyFormatConciseUS(MomentHelper.parse(this.timeLeft, this.m_range?.zone));
            }
        }
        else
        {
            if (this.timeRight !== timestampMs)
            {
                this.timeRight          = timestampMs;
                this.formattedTimeRight = MomentHelper.friendlyFormatConciseUS(MomentHelper.parse(this.timeRight, this.m_range?.zone));
            }
        }

        this.updateEmitRange();

        return timestampMs;
    }

    private updateEmitRange()
    {
        this.timeRangeChange.emit(new ChartTimeRange(this.timeLeft, this.timeRight));
    }

    private updateCenterStyles(detectChanges: boolean = true)
    {
        this.freeDragPositionCenter = {
            x: this.freeDragPositionLeft.x,
            y: 0
        };
        this.rangeWidthPx           = this.freeDragPositionRight.x - this.freeDragPositionLeft.x;

        if (detectChanges) this.detectChanges();
    }

    private constrainDrag(point: Point): Point
    {
        point.x = UtilsService.clamp(this.limitLeft + this.gripOffset, this.limitRight + this.gripOffset, point.x);
        return point;
    }

    public setCenterDragStart()
    {
        this.startTimeLeft  = this.timeLeft;
        this.startTimeRight = this.timeRight;
    }

    public setGripperDragStart(fromLeft: boolean,
                               event: MouseEvent)
    {
        this.draggingLeft = fromLeft;
        if (fromLeft)
        {
            this.startTimeLeft = this.timeLeft;

            this.gripOffset = event.clientX - (this.canvasLeft + this.freeDragPositionLeft.x);
            this.limitLeft  = this.canvasLeft;
            this.limitRight = this.canvasLeft + this.timeToXCoordinate(this.timeRight) - TimeScrubberComponent.timeZoneMinWidthPx;
        }
        else
        {
            this.startTimeRight = this.timeRight;

            this.gripOffset = event.clientX - (this.canvasLeft + this.freeDragPositionRight.x);
            this.limitLeft  = this.canvasLeft + this.timeToXCoordinate(this.timeLeft) + TimeScrubberComponent.timeZoneMinWidthPx;
            this.limitRight = this.canvasLeft + this.canvas.clientWidth;
        }
    }

    public handleGripperDrag(drag: CdkDragMove)
    {
        let distanceMs   = this.distToMs(drag.distance.x);
        let originalTime = this.draggingLeft ? this.startTimeLeft : this.startTimeRight;
        this.updateTime(originalTime + distanceMs);
    }

    public handleCenterDrag(drag: CdkDragMove)
    {
        let distanceMs = this.distToMs(drag.distance.x);
        this.timeLeft  = this.startTimeLeft + distanceMs;
        this.timeRight = this.startTimeRight + distanceMs;

        this.repositionDraggers(true);

        this.updateEmitRange();
    }

    private repositionDraggers(detectChanges: boolean)
    {
        this.freeDragPositionLeft  = {
            x: this.renderInfo && this.timeLeft ? this.timeToXCoordinate(this.timeLeft) : 0,
            y: 0
        };
        this.freeDragPositionRight = {
            x: this.renderInfo && this.timeRight ? this.timeToXCoordinate(this.timeRight) : this.canvas.clientWidth,
            y: 0
        };
        this.updateCenterStyles(detectChanges);
    }

    private rebuildCanvas()
    {
        if (!this.m_rangeExt || !this.canvas || !this.scrubberContainerElem) return;

        let width = this.scrubberContainerElem.nativeElement.clientWidth;
        if (width <= 1) return;

        this.canvasLeft = this.canvas.getBoundingClientRect().left;
        ChartHelpers.scaleCanvas(this.canvas, width, TimeScrubberComponent.selectorHeightPx);
        this.renderInfo                  = new RenderInfo(width, this.m_range, this.m_rangeExt.getChartRange());
        this.renderInfo.backgroundRecipe = this.buildBackgroundRecipe();

        this.render(new ChartHelpers(this.canvas.getContext("2d")));

        this.repositionDraggers(true);
    }

    private render(helper: ChartHelpers)
    {
        helper.canvas.clearRect(0, 0, this.canvas.clientWidth, TimeScrubberComponent.selectorHeightPx);

        this.renderBackground(helper);
        this.renderText(helper);
    }

    private buildBackgroundRecipe(): BackgroundItem[]
    {
        this.updateRangeExt();
        let range = this.m_rangeExt.getChartRange();

        let validDeadZones = (this.deadZoneRanges ?? []).filter(
            (deadRange) => deadRange.lowMs >= range.minInMillisec && deadRange.highMs <= range.maxInMillisec);
        if (validDeadZones.length === 0) return [new BackgroundItem(this.timeToXCoordinate(range.maxInMillisec), false)];

        let backgroundRecipe = validDeadZones[0].lowMs === range.minInMillisec ?
            [new BackgroundItem(this.timeToXCoordinate(validDeadZones[0].highMs), true)] :
            [
                new BackgroundItem(this.timeToXCoordinate(validDeadZones[0].lowMs), false),
                new BackgroundItem(this.timeToXCoordinate(validDeadZones[0].highMs), true)
            ];
        for (let i = 1; i < validDeadZones.length; i++)
        {
            let deadZone = validDeadZones[i];
            backgroundRecipe.push(new BackgroundItem(this.timeToXCoordinate(deadZone.lowMs), false));
            backgroundRecipe.push(new BackgroundItem(this.timeToXCoordinate(deadZone.highMs), true));
        }
        if (validDeadZones[validDeadZones.length - 1].highMs !== range.maxInMillisec) backgroundRecipe.push(new BackgroundItem(this.timeToXCoordinate(range.maxInMillisec), false));

        return backgroundRecipe;
    }

    private renderBackground(helper: ChartHelpers)
    {
        helper.canvas.globalAlpha = 0.5;
        let prevX                 = 0;
        for (let backgroundItem of this.renderInfo.backgroundRecipe)
        {
            helper.canvas.fillStyle = backgroundItem.isDeadZone ? TimeScrubberComponent.deadZoneRangeColor : "#E0E0E0";
            helper.canvas.fillRect(prevX, 0, backgroundItem.endX - prevX, TimeScrubberComponent.selectorHeightPx);
            prevX = backgroundItem.endX;
        }
    }

    private renderText(helper: ChartHelpers)
    {
        helper.canvas.globalAlpha = 0.75;
        let font                  = new ChartFont(undefined, undefined, 11, "bold");
        for (let tick of this.renderInfo.timeTicks)
        {
            helper.drawTextInBox(font, TextPlacement.Center,
                                 TextOrientation.Horizontal,
                                 this.renderInfo.handler.convertToString(tick.moment),
                                 "#808080",
                                 this.timeToXCoordinate(tick.timestampInMillisec),
                                 TimeScrubberComponent.selectorHeightPx,
                                 BoxAnchor.Bottom,
                                 5);
        }
    }

    private timeToXCoordinate(time: moment.Moment | number): number
    {
        time      = typeof time == "number" ? time : time.valueOf();
        let range = this.m_rangeExt.getChartRange();
        return (time.valueOf() - range.minInMillisec) * this.renderInfo.pxPerMs;
    }

    private xCoordinateToMs(xCoord: number): number
    {
        if (!this.renderInfo) return 0;
        let range = this.m_rangeExt.getChartRange();
        return xCoord / this.renderInfo.pxPerMs + range.minInMillisec;
    }

    private distToMs(dist: number): number
    {
        if (!this.renderInfo) return 0;
        return dist / this.renderInfo.pxPerMs;
    }
}

class RenderInfo
{
    readonly handler: TimeInterval;
    readonly timeTicks: ChartTimelineTick[];
    readonly pxPerMs: number;

    backgroundRecipe: BackgroundItem[];

    constructor(public readonly widthPx: number,
                range: Models.RangeSelection,
                public readonly timeRange: ChartTimeRange)
    {
        let diff     = timeRange.diffAsMs;
        this.pxPerMs = widthPx / diff;

        let bestFit    = TimeInterval.calculateBestTickFit(widthPx, diff / 1000);
        this.handler   = bestFit.handler;
        this.timeTicks = bestFit.handler.computeTicks(timeRange, range.zone, bestFit.step);
    }
}

class BackgroundItem
{
    constructor(public endX: number,
                public isDeadZone: boolean)
    {}
}
