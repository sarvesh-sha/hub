import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, HostListener, Input, Output, ViewChild} from "@angular/core";
import {SafeHtml} from "@angular/platform-browser";

import {UtilsService} from "framework/services/utils.service";
import {AxisWidth, CanvasRenderer, CanvasZoneSelection, CanvasZoneSelectionType, CanvasZoneSelector, ChartGroup, ChartHelpers, ChartPanel, MinEdgePadding, MutedColor, TooltipPoint, TooltipPointForMarker, TooltipPointForSample} from "framework/ui/charting/app-charting-utilities";
import {ChartTimelineTick, ChartZoomState, TimeInterval} from "framework/ui/charting/chart-timeline.component";
import {ChartTooltipComponent} from "framework/ui/charting/chart-tooltip.component";
import {Vector2} from "framework/ui/charting/charting-math";
import {BoxAnchor, ChartBox, ChartClipArea, ChartLineType, ChartMarker, ChartPixel, ChartPointStyle, ChartValueRange} from "framework/ui/charting/core/basics";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {ChartPoint, ChartPointRange, ChartPointSource, ChartPointsRenderView, ChartPointToPixel, ChartPointType, ChartPointWithTransform, ChartValueConverter, ChartValueTransform, isVisible, PlaceHolderSource, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartFont, TextOrientation, TextPlacement} from "framework/ui/charting/core/text";
import {ChartTimeRange, ChartTimeWindow} from "framework/ui/charting/core/time";
import {BaseComponent} from "framework/ui/components";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {RelativeLocation} from "framework/ui/utils/relative-location-styles";
import moment from "framework/utils/moment";

import {Subject, Subscription} from "rxjs";
import {throttleTime} from "rxjs/operators";

@Component({
               selector       : "o3-chart",
               templateUrl    : "./chart.component.html",
               styleUrls      : ["./chart.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ChartComponent extends BaseComponent
{
    public static readonly CHART_TITLE_HEIGHT = 37;

    private m_panels: ChartPanel[];
    private m_shouldImportData = false;

    get panels(): ChartPanel[]
    {
        return this.m_panels;
    }

    @Input() set title(value: string)
    {
        this.chartTitle = value;
        this.hasTitle   = !!value;
    }

    private chartTitle: string   = null;
    private hasTitle: boolean    = false;
    private titlePadding: number = 10;

    //--//

    @Input() set panels(value: ChartPanel[])
    {
        if (this.m_panels === value) return;

        for (let panel of this.computedPanels)
        {
            panel.stopStreamingSamples();
        }

        this.m_panels           = value;
        this.m_shouldImportData = true;
    }

    //--//

    private m_stateChangedSub: Subscription;
    private m_zoomState: ChartZoomState;

    @Input() set zoomState(zoomState: ChartZoomState)
    {
        this.m_zoomState = zoomState;
        if (this.m_stateChangedSub)
        {
            this.m_stateChangedSub.unsubscribe();
            this.m_stateChangedSub = null;
        }

        if (this.zoomable)
        {
            this.m_stateChangedSub = this.subscribeToObservable(this.m_zoomState.stateChanged, () =>
            {
                this.stateChanged = true;
                this.redrawCanvas();
            });
        }

        this.reportConfigurationChanges();
    }

    get zoomState(): ChartZoomState
    {
        return this.m_zoomState;
    }

    get zoomingViaScrubber(): boolean
    {
        return this.m_zoomState?.zoomingViaScrubber;
    }

    get timeRange(): ChartTimeRange
    {
        return this.m_zoomState?.outerRange;
    }

    get displayedTimeRange(): ChartTimeRange
    {
        return this.m_zoomState?.displayedRange;
    }

    get zoomable(): boolean
    {
        return this.m_zoomState?.zoomable;
    }

    //--//

    movableAnnotationTooltips: boolean;
    newlyDefinedAnnotation: CanvasZoneSelection;

    private m_newAnnotationType: CanvasZoneSelectionType;
    @Input() set newAnnotationType(type: CanvasZoneSelectionType)
    {
        if (this.m_newAnnotationType != type)
        {
            this.m_newAnnotationType = type;

            let cursor = null;
            switch (type)
            {
                case CanvasZoneSelectionType.Point:
                    cursor = "pointer";
                    break;

                case CanvasZoneSelectionType.XRange:
                case CanvasZoneSelectionType.XRangeInverted:
                    cursor = "ew-resize";
                    break;

                case CanvasZoneSelectionType.YRange:
                case CanvasZoneSelectionType.YRangeInverted:
                    cursor = "ns-resize";
                    break;

                case CanvasZoneSelectionType.Area:
                case CanvasZoneSelectionType.AreaInverted:
                    cursor = "crosshair";
                    break;
            }
            this.setCursor(cursor);

            for (let panel of this.computedPanels || [])
            {
                panel.areaSelector.type = this.m_newAnnotationType;
                panel.annotationPoint   = null;
            }

            if (!type) this.newlyDefinedAnnotation = null;
            this.redrawCanvas();
        }
    }

    get newAnnotationType(): CanvasZoneSelectionType
    {
        return this.m_newAnnotationType;
    }

    @Input() selectionHandler: ChartRangeSelectionHandler;
    selectedRangeStart: ChartPointWithTransform<any>;
    selectedRangeEnd: ChartPointWithTransform<any>;

    @Input() allowSelections: boolean = true;
    @Input() noVerticalPadding: boolean;

    private m_activityCounter: number = 0;

    @Input() minimumRangeMs: number = 10;

    //--//

    @HostListener("document:keydown", ["$event"]) onKeyDown(event: KeyboardEvent)
    {
        if (event.key === "Escape" && this.m_newAnnotationType)
        {
            let nothingCleared = true;
            for (let computedPanel of this.computedPanels)
            {
                if (computedPanel.areaSelector.selectionStarted)
                {
                    computedPanel.areaSelector.clearSelection();
                    nothingCleared = false;
                    this.redrawCanvas();
                    break;
                }
            }

            if (nothingCleared) this.annotationDefined.emit(undefined);
        }
    }

    @Output() renderCompleted = new EventEmitter<void>();

    @Output() startedFetchingData = new EventEmitter<void>();
    @Output() stoppedFetchingData = new EventEmitter<void>();

    @Output() sourceStateUpdated = new EventEmitter<void>();

    @Output() zoomSourceChanged = new EventEmitter<ProcessedDataSource>();

    @Output() annotationDefined      = new EventEmitter<CanvasZoneSelection>();
    @Output() annotationTooltipMoved = new EventEmitter<CanvasZoneSelection>();

    @ViewChild("chartContainer", {static: true}) private containerRef: ElementRef;
    @ViewChild("chartArea", {static: true}) public chartRef: ElementRef;
    @ViewChild("tooltip", {static: true}) public tooltip: ChartTooltipComponent;

    //--//

    private containerElement: HTMLDivElement;
    private chartElement: HTMLCanvasElement;

    private debouncingWidth: number;
    private debouncingHeight: number;
    chartArea: ChartBox;

    private newSample = new Subject<number>();

    private sourceForZoom: ProcessedDataSource;

    get test_panels(): ProcessedPanel[]
    {
        return this.computedPanels;
    }

    private computedPanels: ProcessedPanel[] = [];
    panelBottom: number;
    panelsReady                              = false;
    stateChanged                             = true;

    ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.containerElement = this.containerRef.nativeElement;
        this.chartElement     = this.chartRef.nativeElement;

        this.chartElement.addEventListener("click", (e: MouseEvent) => this.handleEvent("click", e));
        this.chartElement.addEventListener("mouseleave", (e: MouseEvent) => this.handleEvent("mouseleave", e));
        this.chartElement.addEventListener("mousemove", (e: MouseEvent) => this.handleEvent("mousemove", e));

        this.chartElement.addEventListener("wheel", (e: WheelEvent) => this.handleEvent("wheel", e));

        let dragFn = (e: MouseEvent,
                      mouseDown: boolean,
                      mouseUp: boolean) =>
        {
            if (this.panelsReady) this.handleDrag(e, mouseDown, mouseUp);
        };
        this.subscribeToMouseDrag(this.chartElement, dragFn);

        this.subscribeToObservable(this.newSample.pipe(throttleTime(125, undefined, {
            leading : false,
            trailing: true
        })), (timestamp: number) => this.handleNewSample(timestamp));

        this.refreshSize();
    }

    refreshZoom()
    {
        if (!this.computedPanels?.length) return;

        let sourceForZoom = this.computedPanels[this.computedPanels.length - 1]?.updateZoomability();
        if (sourceForZoom !== this.sourceForZoom)
        {
            this.sourceForZoom = sourceForZoom;
            this.zoomSourceChanged.emit(this.sourceForZoom);
        }

        this.reportConfigurationChanges();
    }

    refreshSize(): boolean
    {
        if (!this.containerElement) return false;

        let width  = this.containerElement.offsetWidth;
        let height = this.containerElement.offsetHeight;
        if (this.debouncingWidth != width || this.debouncingHeight != height)
        {
            this.debouncingWidth  = width;
            this.debouncingHeight = height;
            this.chartArea        = new ChartBox(0, 0, width, height);

            this.reportConfigurationChanges();
        }

        return this.stateIsReadyForCanvas() && !!this.debouncingWidth && !!this.debouncingHeight;
    }

    queueUpdate()
    {
        this.reportConfigurationChanges();
    }

    protected afterConfigurationChanges(): void
    {
        super.afterConfigurationChanges();

        ChartHelpers.scaleCanvas(this.chartElement, this.debouncingWidth, this.debouncingHeight);

        if (this.stateIsReadyForCanvas())
        {
            this.panelsReady  = true;
            this.stateChanged = true;

            this.redrawCanvas();
        }
    }

    panelIndex(computedPanel: ProcessedPanel): number
    {
        return this.computedPanels.indexOf(computedPanel);
    }

    private redrawCanvas()
    {
        if (this.panelsReady && this.stateIsReadyForCanvas())
        {
            let canvas = this.chartElement.getContext("2d");
            let width  = this.chartElement.clientWidth;
            let height = this.chartElement.clientHeight;

            if (this.m_shouldImportData)
            {
                if (this.m_panels?.length > 0)
                {
                    this.importData();
                }
                else
                {
                    this.computedPanels = [];

                    if (this.chartElement)
                    {
                        canvas.clearRect(0, 0, width, height);
                    }
                }

                this.m_shouldImportData = false;
            }

            if (this.stateChanged)
            {
                let offsetY = 0;
                if (this.hasTitle)
                {
                    offsetY += ChartComponent.CHART_TITLE_HEIGHT;
                }
                else if (!this.noVerticalPadding)
                {
                    offsetY += MinEdgePadding;
                }

                // Decide on the grid spacing.
                let reservedHeight = offsetY;
                for (let panel of this.computedPanels)
                {
                    reservedHeight += panel.computeReservedHeight();
                }

                let availableHeightPerPanel = (height - reservedHeight) / this.computedPanels.length;
                if (availableHeightPerPanel <= 0)
                {
                    // Too small, can't render chart.
                    return;
                }

                for (let panel of this.computedPanels)
                {
                    panel.prepareDomain(this.timeRange, this.displayedTimeRange, width, availableHeightPerPanel);
                }

                let hasLeftAxis  = false;
                let hasRightAxis = false;
                for (let panel of this.computedPanels)
                {
                    if (!hasLeftAxis && panel.rawPanel.leftAxis.label) hasLeftAxis = true;
                    if (!hasRightAxis && panel.rawPanel.rightAxis.label) hasRightAxis = true;

                    for (let group of panel.groups)
                    {
                        // Reserve space if the group is using an axis
                        let rawGroup = group.group;
                        if (!hasLeftAxis && rawGroup.useAsLeftValueAxis && rawGroup.label) hasLeftAxis = true;
                        if (!hasRightAxis && rawGroup.useAsRightValueAxis && rawGroup.label) hasRightAxis = true;
                    }

                    if (hasLeftAxis && hasRightAxis) break;
                }

                let leftOffset  = hasLeftAxis ? AxisWidth : MinEdgePadding;
                let rightOffset = hasRightAxis ? AxisWidth : MinEdgePadding;
                for (let panel of this.computedPanels)
                {
                    offsetY = panel.prepareLayout(0, offsetY, width, availableHeightPerPanel,
                                                  leftOffset, rightOffset, panel.rawPanel.topPadding);
                    panel.establishAnnotations();
                }
                this.panelBottom = offsetY;

                this.stateChanged = false;
            }

            let helper = new ChartHelpers(canvas);

            // measure annotations
            this.renderAnnotations(helper, true);

            // Prepare drawing area.
            canvas.clearRect(0, 0, width, height);

            // Render title
            if (this.hasTitle)
            {
                let helper = new ChartHelpers(canvas);
                let font   = new ChartFont();

                font.size  = ChartComponent.CHART_TITLE_HEIGHT - (this.titlePadding * 2);
                font.color = "#000000";

                helper.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, this.chartTitle, font.color, width / 2, 0, BoxAnchor.Top, this.titlePadding, "transparent");
            }

            // render panels and associated source data
            for (let panel of this.computedPanels) panel.render(helper, (html) => this.sanitizeHtml(html));

            // render annotations
            this.renderAnnotations(helper);

            this.renderCompleted.emit();
        }
    }

    private renderAnnotations(helper: ChartHelpers,
                              measuring?: boolean)
    {
        for (let panel of this.computedPanels) panel.renderAnnotations(helper, measuring);
    }

    private stateIsReadyForCanvas()
    {
        return this.containerElement?.clientWidth >= 50 && this.containerElement.clientHeight >= 15 &&
               this.m_panels && ChartTimeRange.isValid(this.timeRange) && ChartTimeRange.isValid(this.displayedTimeRange);
    }

    //--//

    public getCanvasPNG(backgroundColor: string = "white"): string
    {
        return ChartHelpers.getCanvasPNG(<HTMLCanvasElement>this.chartRef.nativeElement, backgroundColor);
    }

    //--//

    public getState(source: ChartPointSource<any>,
                    panelIdx: number): VisualizationDataSourceState
    {
        return this.computedPanels[panelIdx]?.getProcessedSource(source)?.state;
    }

    public allOn(panelIdx: number)
    {
        let processedPanel = this.computedPanels[panelIdx];

        processedPanel.setAllSources(VisualizationDataSourceState.Active, true);
        processedPanel.targetSource = null;

        this.sourceStateUpdated.emit();

        this.redrawCanvas();
    }

    public singleOn(source: ChartPointSource<any>,
                    panelIdx: number): VisualizationDataSourceState
    {
        let processedPanel = this.computedPanels[panelIdx];
        processedPanel.setAllSources(VisualizationDataSourceState.Disabled, false);
        let processedSource = processedPanel.getProcessedSource(source);
        processedSource?.setState(VisualizationDataSourceState.Active, true);
        processedPanel.targetSource = null;

        this.sourceStateUpdated.emit();

        this.redrawCanvas();

        return processedSource?.state;
    }

    public toggleSourceEnabled(source: ChartPointSource<any>,
                               panelIdx: number): VisualizationDataSourceState
    {
        let targetPanel     = this.computedPanels[panelIdx];
        let processedSource = targetPanel.getProcessedSource(source);
        if (!processedSource) return null;

        this.clearTargetIfTarget(processedSource, panelIdx);

        if (processedSource.state === VisualizationDataSourceState.Disabled)
        {
            let newState = targetPanel.restoreSource(processedSource);
            if (isVisible(newState)) this.redrawCanvas();
        }
        else
        {
            processedSource.setState(VisualizationDataSourceState.Disabled, true);
            this.redrawCanvas();
        }

        this.sourceStateUpdated.emit();

        return processedSource.state;
    }

    public cancelDeletion(source: ChartPointSource<any>,
                          panelIdx: number): VisualizationDataSourceState
    {
        let targetPanel = this.computedPanels[panelIdx];
        let newState;
        if (targetPanel)
        {
            newState = targetPanel.restoreSource(source);
            if (isVisible(newState)) this.redrawCanvas();
        }

        return newState;
    }

    public markSourceDeleted(source: ChartPointSource<any>,
                             panelIdx: number,
                             needsRedraw: boolean): VisualizationDataSourceState
    {
        let processedSourceToDelete = this.computedPanels[panelIdx]?.getProcessedSource(source);
        if (processedSourceToDelete)
        {
            this.clearTargetIfTarget(processedSourceToDelete, panelIdx);
            processedSourceToDelete.setState(VisualizationDataSourceState.Deleted, true);
            if (needsRedraw) this.redrawCanvas();
            return processedSourceToDelete.state;
        }

        return null;
    }

    private clearTargetIfTarget(source: ProcessedDataSource,
                                panelIdx: number)
    {
        if (source.state === VisualizationDataSourceState.Target)
        {
            let isTargetFromMouseover = source.group.panel.targetSource !== source;
            this.toggleTargetSource(source.dataSource, panelIdx, isTargetFromMouseover, false);
        }
    }

    public toggleTargetSource(targetSource: ChartPointSource<any>,
                              panelIdx: number,
                              fromMouseover: boolean,
                              redraw: boolean = true)
    {
        let targetComputedPanel = this.computedPanels[panelIdx];
        if (targetComputedPanel)
        {
            targetComputedPanel.toggleTargetSource(targetSource, fromMouseover);
            if (redraw) this.redrawCanvas();

            this.sourceStateUpdated.emit();
        }
    }

    setCursor(style: string)
    {
        this.chartElement.style.cursor = style || "";
    }

    //--//

    private handleNewSample(sampleTimestamp: number)
    {
        if (sampleTimestamp && this.m_zoomState)
        {
            this.m_zoomState.shiftToContain(sampleTimestamp);
        }

        this.importData();
    }

    private importData()
    {
        this.tooltip.remove();
        this.computedPanels = this.m_panels.map((panel) => new ProcessedPanel(this, panel));
        this.panelsReady    = false;

        this.refreshZoom();
    }

    incrementActiveCount()
    {
        if (this.m_activityCounter++ == 0)
        {
            this.startedFetchingData.emit();
        }
    }

    decrementActiveCount()
    {
        if (--this.m_activityCounter == 0)
        {
            this.stoppedFetchingData.emit();
        }
    }

    notifyNewSamples(source: ChartPointSource<any>,
                     timestamp: moment.Moment)
    {
        this.newSample.next(source.provider.rangeEnd ? undefined : timestamp.valueOf());
    }

    reset()
    {
        for (let computedPanel of this.computedPanels)
        {
            computedPanel.reset();
        }
    }

    onMouseMove(x: number,
                y: number): boolean
    {
        if (this.panelsReady)
        {
            if (!this.getAnnotationTooltip(x, y))
            {
                let redraw = false;
                for (let panel of this.computedPanels)
                {
                    if (panel.handleMousemove(x, y)) redraw = true;
                }

                if (redraw)
                {
                    this.redrawCanvas();
                    return true;
                }
            }
            else
            {
                this.onMouseLeave();
            }
        }

        return false;
    }

    onMouseLeave()
    {
        if (this.panelsReady)
        {
            let redraw = false;
            for (let panel of this.computedPanels)
            {
                if (panel.handleMouseleave()) redraw = true;
            }

            if (redraw) this.redrawCanvas();
        }
    }

    private handleEvent<K extends keyof WindowEventMap>(type: K,
                                                        ev: WindowEventMap[K])
    {
        if (!this.panelsReady) return;
        if (this.m_newAnnotationType && type != "mouseleave")
        {
            if (type != "click" && type != "mousemove") return;
        }

        let redraw = false;
        for (let panel of this.computedPanels)
        {
            if (panel.handleEvent(type, ev)) redraw = true;
        }

        if (this.handleChartWideEvent(type, ev)) redraw = true;

        if (redraw) this.redrawCanvas();
    }

    private handleChartWideEvent<K extends keyof WindowEventMap>(type: K,
                                                                 e: WindowEventMap[K]): boolean
    {
        let mouseEvent = <MouseEvent>e;
        let x          = mouseEvent.offsetX;
        let y          = mouseEvent.offsetY;

        let tooltip = this.getAnnotationTooltip(x, y);

        let redraw = false;
        switch (type)
        {
            case "mousemove":
                if (this.zoomingViaScrubber) break;
                if (this.computedPanels.some((panel) => panel.tracingAnnotation)) break;

                if (tooltip || this.movableAnnotationTooltips)
                {
                    this.setCursor(tooltip && this.movableAnnotationTooltips ? "grab" : "pointer");
                    this.reset();
                }
                break;

            case "click":
                if (tooltip)
                {
                    if (tooltip.tooltipDragging)
                    {
                        // this is the event from releasing the drag on this tooltip
                        tooltip.tooltipDragging = false;
                    }
                    else if (!this.movableAnnotationTooltips)
                    {
                        this.movableAnnotationTooltips = true;
                        this.computedPanels[tooltip.panel].reset();
                        redraw = true;
                    }

                    this.setCursor(this.movableAnnotationTooltips ? "grab" : "pointer");
                }
                else
                {
                    this.setCursor(null);
                    this.movableAnnotationTooltips = false;
                    redraw                         = true;
                }

                break;
        }

        return redraw;
    }

    private getAnnotationTooltip(x: number,
                                 y: number): CanvasZoneSelection
    {
        for (let computedPanel of this.computedPanels)
        {
            for (let annotation of computedPanel.rawPanel.annotations)
            {
                if (annotation.tooltipHitCheck(x, y))
                {
                    return annotation;
                }
            }
        }

        return null;
    }

    private handleDrag(e: MouseEvent,
                       mouseDown: boolean,
                       mouseUp: boolean)
    {
        let redraw = false;
        for (let panel of this.computedPanels)
        {
            if (panel.handleDrag(e, mouseDown, mouseUp)) redraw = true;
        }

        if (this.zoomable && this.selectionHandler)
        {
            let points = this.findSourcesUnderMouse(e);

            if (mouseDown)
            {
                if (this.selectedRangeStart)
                {
                    this.selectionHandler.clearRange();

                    this.selectedRangeStart = null;
                    this.selectedRangeEnd   = null;
                    redraw                  = true;
                }

                for (let point of points)
                {
                    let start = point.point.xAsMoment;
                    if (this.selectionHandler.acceptRange(point.point.owningSource, start, start))
                    {
                        this.selectedRangeStart = point;
                        this.selectedRangeEnd   = point;
                        redraw                  = true;
                        break;
                    }
                }
            }
            else if (mouseUp)
            {
                // Nothing to do here.
            }
            else
            {
                let start = this.selectedRangeStart;
                let end   = this.selectedRangeEnd;
                if (start)
                {
                    for (let point of points)
                    {
                        if (point.point.owningSource == start.point.owningSource)
                        {
                            if (point.point.timestampInMillisec < start.point.timestampInMillisec)
                            {
                                start = point;
                            }
                            else
                            {
                                end = point;
                            }

                            if (this.selectionHandler.acceptRange(point.point.owningSource, start.point.xAsMoment, end.point.xAsMoment))
                            {
                                this.selectedRangeStart = start;
                                this.selectedRangeEnd   = end;
                            }
                            else
                            {
                                this.selectedRangeStart = null;
                                this.selectedRangeEnd   = null;
                            }

                            redraw = true;

                            break;
                        }
                    }
                }
            }
        }

        if (redraw) this.redrawCanvas();
    }

    private findSourcesUnderMouse(e: MouseEvent): ChartPointWithTransform<any>[]
    {
        let points = [];
        let x      = e.offsetX;
        let y      = e.offsetY;

        for (let panel of this.computedPanels)
        {
            for (let group of panel.groups)
            {
                for (let source of group.sources)
                {
                    let point = source.fromPixelToSample(x, y);
                    if (point)
                    {
                        points.push(point);
                    }
                }
            }
        }

        return points;
    }
}

//--//

const categoryAxisSize        = 30;
const verticalDataPadding     = 20;
const verticalSpacingForTicks = 40;
const axisLabelPadding        = 13;

class ProcessedPanel
{
    get offsetTop(): string
    {
        return `${this.clipForPanel.y}px`;
    }

    get activeSelection(): CanvasZoneSelection
    {
        return this.owner.newlyDefinedAnnotation;
    }

    get withSubSecondX(): boolean
    {
        return this.majorTickXstep <= 0.5;
    }

    groups: ProcessedGroup[] = [];

    private clipForPanel    = ChartClipArea.EmptyPlaceholder;
    private clipForDataArea = ChartClipArea.EmptyPlaceholder;

    private clipForLeftAxis: ChartBox;
    private clipForRightAxis: ChartBox;
    leftLabelTruncated: boolean;
    rightLabelTruncated: boolean;

    private majorTickXhandler: TimeInterval;
    private majorTickXstep                   = 0.0;
    private majorTicksX: ChartTimelineTick[] = [];

    private groupForCategoryAxis: ProcessedGroup;
    private groupForLeftAxis: ProcessedGroup;
    private groupForRightAxis: ProcessedGroup;
    private clipForCategoryAxis: ChartClipArea;

    private sourceForZoom: ProcessedDataSource;

    private sourceForHighlights: ProcessedDataSource;
    private sourceForExpandedPoints: ProcessedDataSource;

    private highlightPoint: ChartPointWithTransform<any> = null;
    private tooltipPoint: TooltipPoint                   = null;
    private tooltipText: string;

    readonly areaSelector = new CanvasZoneSelector();
    annotationPoint: HighlightedSourceValue;

    get tracingAnnotation(): boolean
    {
        return !!this.areaSelector.type;
    }

    private draggingTooltip: CanvasZoneSelection;
    private draggingTooltipCursor: Vector2;

    private mousedownTarget: HTMLElement;

    targetSource: ProcessedDataSource;
    private mouseoverSource: ChartPointSource<any>;

    constructor(private owner: ChartComponent,
                public readonly rawPanel: ChartPanel)
    {
        for (let group of rawPanel.groups)
        {
            let processedGroup = new ProcessedGroup(this, group);

            this.groups.push(processedGroup);
        }

        if (rawPanel.targetSource) this.targetSource = this.getProcessedSource(rawPanel.targetSource);

        for (let processedGroup of this.groups)
        {
            if (processedGroup.group.useAsCategoryAxis)
            {
                if (!this.groupForCategoryAxis)
                {
                    this.groupForCategoryAxis = processedGroup;
                }
            }

            if (processedGroup.group.useAsLeftValueAxis) this.groupForLeftAxis = processedGroup;
            if (processedGroup.group.useAsRightValueAxis) this.groupForRightAxis = processedGroup;

            for (let source of processedGroup.sources)
            {
                if (!this.sourceForHighlights && source.dataSource.showHighlights)
                {
                    this.sourceForHighlights = source;
                }

                if (!this.sourceForExpandedPoints && source.dataSource.showExpandedPoints)
                {
                    this.sourceForExpandedPoints = source;
                }
            }
        }

        this.startStreamingSamples();
    }

    reset()
    {
        this.clearTooltip();
        this.highlightPoint = null;
        this.setMouseoverTargetSource(null);
    }

    updateZoomability(): ProcessedDataSource
    {
        this.sourceForZoom = null;
        if (this.owner.zoomable)
        {
            for (let group of this.groups)
            {
                for (let source of group.sources)
                {
                    if (!this.sourceForZoom && source.dataSource.zoomable)
                    {
                        this.sourceForZoom = source;
                        break;
                    }
                }
            }
        }

        return this.sourceForZoom;
    }

    private setMouseoverTargetSource(mouseoverSource: ChartPointSource<any>)
    {
        if (this.mouseoverSource != mouseoverSource)
        {
            if (!this.targetSource)
            {
                if (mouseoverSource)
                {
                    let processedMouseoverSource = this.getProcessedSource(mouseoverSource);
                    if (processedMouseoverSource)
                    {
                        this.setAllSources(VisualizationDataSourceState.Muted, false);
                        processedMouseoverSource.setState(VisualizationDataSourceState.Target);
                    }
                }
                else if (this.hasTargetSourceDisplay())
                {
                    this.setAllSources(VisualizationDataSourceState.Active, false);
                }

                this.owner.sourceStateUpdated.emit();
            }

            this.mouseoverSource = mouseoverSource;
        }
    }

    toggleTargetSource(source: ChartPointSource<any>,
                       fromMouseover: boolean)
    {
        if (fromMouseover)
        {
            if (source.state === VisualizationDataSourceState.Target) source = null;
            this.setMouseoverTargetSource(source);
        }
        else
        {
            this.mouseoverSource = null;
            if (!source || this.targetSource?.dataSource === source)
            {
                this.targetSource = null;
                this.setAllSources(VisualizationDataSourceState.Active, false);
            }
            else
            {
                this.setAllSources(VisualizationDataSourceState.Muted, false);

                this.targetSource = this.getProcessedSource(source);
                this.targetSource?.setState(VisualizationDataSourceState.Target, true);
            }
        }
    }

    setAllSources(state: VisualizationDataSourceState,
                  override: boolean)
    {
        for (let group of this.groups)
        {
            for (let source of group.sources)
            {
                source.setState(state, override);
            }
        }
    }

    restoreSource(source: ChartPointSource<any> | ProcessedDataSource): VisualizationDataSourceState
    {
        let processedSource = source instanceof ProcessedDataSource ? source : this.getProcessedSource(source);
        let restoreState    = this.targetSource ? VisualizationDataSourceState.Muted : VisualizationDataSourceState.Active;
        processedSource.restore(restoreState);

        return processedSource.state;
    }

    getProcessedSource(source: ChartPointSource<any>): ProcessedDataSource
    {
        if (source)
        {
            for (let processedGroup of this.groups)
            {
                for (let processedSource of processedGroup.sources)
                {
                    if (source === processedSource.dataSource) return processedSource;
                }
            }
        }

        return null;
    }

    startStreamingSamples()
    {
        for (let group of this.groups)
        {
            group.startStreamingSamples();
        }
    }

    stopStreamingSamples()
    {
        for (let group of this.groups)
        {
            group.stopStreamingSamples();
        }
    }

    incrementActiveCount()
    {
        this.owner.incrementActiveCount();
    }

    decrementActiveCount()
    {
        this.owner.decrementActiveCount();
    }

    notifyNewSamples(source: ChartPointSource<any>,
                     timestamp: moment.Moment)
    {
        this.owner.notifyNewSamples(source, timestamp);
    }

    computeReservedHeight(): number
    {
        let res = 0;

        if (this.groupForCategoryAxis && !this.rawPanel.hideBottomAxis) res += categoryAxisSize;

        return res;
    }

    prepareDomain(requestedRange: ChartTimeRange,
                  displayedRange: ChartTimeRange,
                  width: number,
                  height: number)
    {
        for (let group of this.groups)
        {
            group.prepareDomain(requestedRange, displayedRange, width, height);
        }

        //--//

        let bestFit = TimeInterval.calculateBestTickFit(width, displayedRange.diffAsMs / 1000);

        this.majorTickXhandler = bestFit.handler;
        this.majorTickXstep    = bestFit.step;
        this.majorTicksX       = bestFit.handler.computeTicks(requestedRange, this.rawPanel.zone, bestFit.step, displayedRange);
    }

    prepareLayout(x: number,
                  y: number,
                  width: number,
                  height: number,
                  left: number,
                  right: number,
                  topPadding: number): number
    {
        this.clipForPanel = new ChartClipArea(x, y, width, height);

        this.clipForLeftAxis  = new ChartBox(x, y, left, height);
        this.clipForRightAxis = new ChartBox(x + width - right, y, right, height);

        // Create a modified clip area that covers only the data, minus the space for axes
        this.clipForDataArea = new ChartClipArea(x + left, y + topPadding, width - (left + right), height - topPadding);

        this.areaSelector.prepareLayout(this.clipForDataArea, this.owner.newAnnotationType);

        for (let group of this.groups)
        {
            // Set the clip are to determine where to render data
            group.prepareLayout(this.clipForDataArea);
        }

        //--//
        y += height;

        if (this.groupForCategoryAxis && !this.rawPanel.hideBottomAxis)
        {
            this.clipForCategoryAxis = new ChartClipArea(x, y, width, categoryAxisSize);
            y += categoryAxisSize;
        }

        return y;
    }

    private getAnnotationGroup(annotation: CanvasZoneSelection): ProcessedGroup
    {
        let annotationSource = annotation.chartSource;
        if (annotation.type !== CanvasZoneSelectionType.Point || !annotationSource) return this.groupForLeftAxis;

        return this.groups.find((group) => group.group.sources.some((source) => source === annotationSource));
    }

    establishAnnotations()
    {
        for (let annotation of this.rawPanel.annotations)
        {
            let annotationGroup = this.getAnnotationGroup(annotation);
            annotation.establishForChart(annotationGroup?.transform, annotationGroup?.domain, annotationGroup?.group);
        }
    }

    render(helper: ChartHelpers,
           sanitizer: (html: string) => SafeHtml)
    {
        let font = new ChartFont();

        this.renderAnnotationShading(helper);

        this.renderSources(helper);

        this.renderAlerts(helper);

        this.renderGrid(helper);

        this.renderAxes(helper, font);

        this.renderHighlights(helper, font);

        this.renderTooltip(helper, sanitizer);
    }

    private renderAnnotationShading(helper: ChartHelpers)
    {
        let panelIndex = this.owner.panelIndex(this);

        this.clipForDataArea.applyClipping(helper.canvas, () =>
        {
            for (let annotation of this.rawPanel.annotations)
            {
                if (annotation.panel === panelIndex && annotation.type !== CanvasZoneSelectionType.Point && annotation.showing)
                {
                    CanvasZoneSelector.renderSelection(helper.canvas, this.clipForDataArea, annotation);
                }
            }

            let newAnnotationSelection = this.areaSelector.selection || this.activeSelection;
            if (newAnnotationSelection && newAnnotationSelection.type !== CanvasZoneSelectionType.Point)
            {
                let panel = newAnnotationSelection.panel;
                if (panel === undefined || newAnnotationSelection.panel === panelIndex) CanvasZoneSelector.renderSelection(helper.canvas, this.clipForDataArea, newAnnotationSelection);
            }
        });
    }

    private renderSources(helper: ChartHelpers)
    {
        this.clipForDataArea.applyClipping(helper.canvas, () =>
        {
            let muted: ProcessedDataSource[]   = [];
            let actives: ProcessedDataSource[] = [];

            for (let group of this.groups)
            {
                for (let source of group.sources)
                {
                    switch (source.state)
                    {
                        case VisualizationDataSourceState.Active:
                        case VisualizationDataSourceState.Target:
                            actives.push(source);
                            break;

                        case VisualizationDataSourceState.Muted:
                            muted.push(source);
                            break;
                    }
                }
            }

            const sortFn = (sourceA: ProcessedDataSource,
                            sourceB: ProcessedDataSource) => UtilsService.compareNumbers(sourceA.index, sourceB.index, true);
            muted.sort(sortFn);
            actives.sort(sortFn);

            this.renderSourcePlaceholders(helper.canvas,
                                          [
                                              ...actives,
                                              ...muted
                                          ]);

            this.renderSourceAreas(helper.canvas, actives);

            this.renderSourcePoints(helper, muted, MutedColor);
            this.renderSourceLines(helper.canvas, muted, MutedColor);

            this.renderSourcePoints(helper, actives);
            this.renderSourceLines(helper.canvas, actives);
        });
    }

    private renderSourcePlaceholders(canvas: CanvasRenderingContext2D,
                                     sources: ProcessedDataSource[])
    {
        for (let processedDataSource of sources)
        {
            if (!(processedDataSource.dataSource instanceof PlaceHolderSource))
            {
                return;
            }
        }

        let helpers = new ChartHelpers(canvas);
        CanvasRenderer.renderText(helpers, "No Associated Sources", 2, MutedColor, this.clipForDataArea);
    }

    private renderSourceAreas(canvas: CanvasRenderingContext2D,
                              sources: ProcessedDataSource[])
    {
        for (let processedDataSource of sources)
        {
            let color = ChartColorUtilities.safeChroma(processedDataSource.dataSource.color)
                                           .brighten(0.75)
                                           .desaturate(0.2)
                                           .hex();

            processedDataSource.renderArea(canvas, processedDataSource.group.transform, color, processedDataSource.dataSource.fillArea ? 0.5 : 0);
        }
    }

    private renderSourcePoints(helper: ChartHelpers,
                               sources: ProcessedDataSource[],
                               overrideColor?: string)
    {
        for (let processedDataSource of sources)
        {
            if (processedDataSource.dataSource.showPoints)
            {
                for (let range of processedDataSource.dataSource.ranges)
                {
                    let view = processedDataSource.extractVisibleValues(processedDataSource.group.transform, range, 0);
                    CanvasRenderer.renderPoints(helper, view, overrideColor || processedDataSource.dataSource.color, overrideColor, overrideColor, true);
                }
            }
        }
    }

    private renderSourceLines(canvas: CanvasRenderingContext2D,
                              sources: ProcessedDataSource[],
                              overrideColor?: string)
    {
        for (let processedDataSource of sources)
        {
            let chromaColor = ChartColorUtilities.safeChroma(overrideColor || processedDataSource.dataSource.color)
                                                 .darken(0.2)
                                                 .saturate(0.1)
                                                 .hex();

            processedDataSource.renderLine(canvas, processedDataSource.group.transform, processedDataSource.dataSource.hideDecimation ? 3 : 2.5,
                                           chromaColor, overrideColor || "red", overrideColor || "yellow", 1);
        }
    }

    private renderAlerts(helper: ChartHelpers)
    {
        if (!this.rawPanel.showAlerts) return;

        this.clipForDataArea.applyClipping(helper.canvas, () =>
        {
            if (this.hasTargetSourceDisplay())
            {
                for (let group of this.groups) group.renderMarkers(helper, this.owner.selectedRangeStart, this.owner.selectedRangeEnd, VisualizationDataSourceState.Muted);
                for (let group of this.groups) group.renderMarkers(helper, this.owner.selectedRangeStart, this.owner.selectedRangeEnd, VisualizationDataSourceState.Target);
            }
            else
            {
                for (let group of this.groups) group.renderMarkers(helper, this.owner.selectedRangeStart, this.owner.selectedRangeEnd);
            }
        });
    }

    private renderGrid(helper: ChartHelpers)
    {
        this.clipForDataArea.applyClipping(helper.canvas, () =>
        {
            CanvasRenderer.renderGridOutline(helper.canvas, this.clipForDataArea, this.rawPanel.borderColor, true, !this.rawPanel.hideBottomAxis);

            if (this.groupForCategoryAxis)
            {
                this.groupForCategoryAxis.renderGrid(helper, this.clipForDataArea, this.majorTicksX);
            }
        });
    }

    private renderAxes(helper: ChartHelpers,
                       font: ChartFont)
    {
        this.clipForPanel.applyClipping(helper.canvas, () =>
        {
            this.leftLabelTruncated  = this.renderVerticalAxis(helper, true);
            this.rightLabelTruncated = this.renderVerticalAxis(helper, false);
        });

        if (this.groupForCategoryAxis && !this.rawPanel.hideBottomAxis)
        {
            let canvas = helper.canvas;

            this.clipForCategoryAxis?.applyClipping(canvas, () =>
            {
                canvas.fillStyle = "white";

                for (let tick of this.majorTicksX)
                {
                    if (!this.owner.displayedTimeRange.isInRange(tick.timestampInMillisec, true)) continue;

                    let text      = this.majorTickXhandler.convertToString(tick.moment);
                    let x         = this.groupForCategoryAxis.fromTimestampToXCoordinate(tick.moment);
                    let prevStyle = font.style;
                    if (tick.bold)
                    {
                        font.style = "bold";
                    }
                    helper.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, text, "black", x + 2, this.clipForCategoryAxis.y, BoxAnchor.Top, 5);
                    font.style = prevStyle;
                }
            });
        }
    }

    private renderVerticalAxis(helper: ChartHelpers,
                               left: boolean): boolean
    {
        let labelFont   = new ChartFont();
        labelFont.size  = AxisWidth - axisLabelPadding * 2;
        labelFont.color = "#000000";

        let axis  = left ? this.rawPanel.leftAxis : this.rawPanel.rightAxis;
        let label = axis.label;
        let group = this.groups.find((group) => left ? group.group.useAsLeftValueAxis : group.group.useAsRightValueAxis);
        if (!label && !group) return false;

        let unitsLabel: string;
        if (!label)
        {
            label      = group.group.label;
            unitsLabel = group.group.unitsLabel;
        }

        let axisLocation = left ? RelativeLocation.Left : RelativeLocation.Right;

        if (group) group.renderTicks(helper, this.clipForPanel, this.clipForDataArea, axisLocation);
        return CanvasRenderer.renderAxisLabel(helper, label, axis.color, this.clipForPanel, this.clipForDataArea, axisLabelPadding, axisLocation, unitsLabel);
    }

    private renderHighlights(helper: ChartHelpers,
                             font: ChartFont)
    {
        if (this.highlightPoint && this.sourceForHighlights)
        {
            let canvas = helper.canvas;

            const boxColor = "#258cbb";

            let pixel = this.highlightPoint.toPixel();

            this.clipForPanel.applyClipping(canvas, () =>
            {
                canvas.setLineDash([
                                       5,
                                       5
                                   ]);
                canvas.globalAlpha = 0.5;
                canvas.lineWidth   = 0.5;
                canvas.strokeStyle = boxColor;

                let rightBorder  = this.clipForDataArea.right;
                let bottomBorder = this.clipForDataArea.bottom;

                canvas.beginPath();
                canvas.moveTo(this.clipForDataArea.x, pixel.y);
                canvas.lineTo(rightBorder, pixel.y);
                canvas.stroke();

                canvas.lineWidth = 1;
                canvas.moveTo(pixel.x, this.clipForDataArea.y);
                canvas.lineTo(pixel.x, bottomBorder);
                canvas.stroke();

                canvas.globalAlpha = 1;

                let yLabel                = this.highlightPoint.point.numberValue.toFixed(2);
                const halfHighlightHeight = helper.placeTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, yLabel, this.clipForDataArea.left, pixel.y, BoxAnchor.Left, 5).height / 2;
                const y                   = UtilsService.clamp(this.clipForPanel.top + halfHighlightHeight, this.clipForPanel.bottom - halfHighlightHeight, pixel.y);
                helper.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, yLabel, "white", this.clipForDataArea.left, y, BoxAnchor.Left, 5, boxColor);
            });

            //--//

            if (this.clipForCategoryAxis)
            {
                this.clipForCategoryAxis.applyClipping(canvas, () =>
                {
                    canvas.globalAlpha = 1;

                    const xLabel             = MomentHelper.friendlyFormatVerboseUS(this.highlightPoint.point.xAsMoment, true, this.withSubSecondX);
                    const halfHighlightWidth = helper.placeTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, xLabel, pixel.x, this.clipForCategoryAxis.y, BoxAnchor.Top, 5).width / 2;
                    const x                  = UtilsService.clamp(this.clipForPanel.left + halfHighlightWidth, this.clipForPanel.right - halfHighlightWidth, pixel.x);
                    helper.drawTextInBox(font, TextPlacement.Center, TextOrientation.Horizontal, xLabel, "white", x, this.clipForCategoryAxis.y, BoxAnchor.Top, 5, boxColor);
                });
            }
        }
    }

    private renderTooltip(helper: ChartHelpers,
                          sanitizer: (html: string) => SafeHtml)
    {
        if (this.tooltipPoint && this.sourceForExpandedPoints)
        {
            this.clipForDataArea.applyClipping(helper.canvas, () =>
            {
                this.tooltipPoint.render(helper, this.owner.tooltip, sanitizer);
            });
        }
    }

    public renderAnnotations(helper: ChartHelpers,
                             measuring?: boolean)
    {
        let panelIndex              = this.owner.panelIndex(this);
        const renderSelectionMarker = (selection: CanvasZoneSelection) =>
        {
            if (selection.panel === panelIndex && selection.type === CanvasZoneSelectionType.Point && selection.showing && selection.inView)
            {
                let annotationGroup = this.getAnnotationGroup(selection);
                if (annotationGroup) selection.marker.render(helper, annotationGroup.transform);
            }
        };

        this.rawPanel.annotations.forEach(renderSelectionMarker);

        if (this.annotationPoint)
        {
            let complementaryColor = ChartColorUtilities.getComplementaryColor(this.annotationPoint.point.owningSource.color);
            let pixel              = this.annotationPoint.pixel;

            helper.drawPoint(ChartPointStyle.circle, complementaryColor, 4, pixel.x, pixel.y);
        }

        let selection = this.activeSelection;
        if (selection?.type === CanvasZoneSelectionType.Point)
        {
            let annotationGroup = this.getAnnotationGroup(selection);
            if (annotationGroup)
            {
                selection.establishForChart(annotationGroup.transform, annotationGroup.domain, annotationGroup.group);
                renderSelectionMarker(selection);
            }
        }

        //--/

        let canvas = helper.canvas;
        if (!this.owner.newAnnotationType)
        {
            let dash = [
                3,
                5
            ];

            for (let annotation of this.rawPanel.annotations)
            {
                if (measuring && annotation.tooltipBox) continue;
                if (!annotation.hideTooltip && annotation.showing && annotation.hasCorners)
                {
                    let transform = this.getAnnotationGroup(annotation).transform;

                    const maxWidth       = 300;
                    const tooltipPadding = 10;
                    let anchor           = annotation.marker.toPixel(transform);
                    if (isNaN(anchor.y) || isNaN(anchor.x) || !this.clipForDataArea.hitCheck(anchor.x, anchor.y))
                    {
                        annotation.tooltipBox = null;
                        continue;
                    }
                    let x = anchor.x + annotation.tooltipOffset.x;
                    let y = anchor.y + annotation.tooltipOffset.y;
                    if (annotation.tooltipBox)
                    {
                        x = UtilsService.clamp(this.clipForPanel.left, this.clipForPanel.right - annotation.tooltipBox.width, x);
                        y = UtilsService.clamp(0, this.owner.panelBottom - annotation.tooltipBox.height, y);
                    }

                    let font = new ChartFont(undefined, undefined, 11);

                    annotation.tooltipBox = CanvasRenderer.renderTooltip(helper, font, annotation.title, annotation.description, x, y, maxWidth,
                                                                         CanvasZoneSelection.tooltipBorderRadius, tooltipPadding, undefined,
                                                                         this.owner.movableAnnotationTooltips ? 2 : 0, "#2196f3");

                    let tooltipBox = annotation.tooltipBox;
                    if (!annotation.tooltipBox.hitCheck(anchor.x, anchor.y))
                    {
                        let connectionPoint: Vector2;
                        if (tooltipBox.bottom < anchor.y)
                        {
                            connectionPoint = new Vector2(tooltipBox.x + tooltipBox.width / 2, tooltipBox.bottom);
                        }
                        else if (tooltipBox.y > anchor.y)
                        {
                            connectionPoint = new Vector2(tooltipBox.x + tooltipBox.width / 2, tooltipBox.y);
                        }
                        else if (tooltipBox.x > anchor.x)
                        {
                            connectionPoint = new Vector2(tooltipBox.x, tooltipBox.y + tooltipBox.height / 2);
                        }
                        else
                        {
                            connectionPoint = new Vector2(tooltipBox.right, tooltipBox.y + tooltipBox.height / 2);
                        }

                        canvas.save();
                        canvas.strokeStyle = "#666666";
                        canvas.lineWidth   = 1;
                        canvas.setLineDash(dash);
                        canvas.beginPath();
                        canvas.moveTo(anchor.x, anchor.y);
                        canvas.lineTo(connectionPoint.x, connectionPoint.y);
                        canvas.stroke();
                        canvas.restore();
                    }
                }
                else
                {
                    annotation.tooltipBox = null;
                }
            }
        }
    }

    //--//

    handleEvent<K extends keyof WindowEventMap>(type: K,
                                                e: WindowEventMap[K]): boolean
    {
        let mouseEvent: MouseEvent;
        let x: number;
        let y: number;

        let redraw = false;
        switch (type)
        {
            case "click":
            {
                mouseEvent = <MouseEvent>e;
                x          = mouseEvent.offsetX;
                y          = mouseEvent.offsetY;

                if (this.sourceForHighlights && this.clipForDataArea.hitCheck(x, y))
                {
                    if (this.areaSelector.type === CanvasZoneSelectionType.Point)
                    {
                        const maxDistance = 8;
                        let closestPoint  = this.findClosestValue(x, y, maxDistance, this.targetSource?.dataSource);
                        if (this.annotationPoint != closestPoint)
                        {
                            redraw               = true;
                            this.annotationPoint = closestPoint;
                            if (this.annotationPoint)
                            {
                                let selection   = CanvasZoneSelection.fromChartPoint(this.annotationPoint.point);
                                selection.panel = this.owner.panelIndex(this);

                                this.owner.newlyDefinedAnnotation = selection;
                                this.owner.annotationDefined.emit(selection);
                            }
                        }
                    }
                    else if (this.tooltipPoint)
                    {
                        let point        = this.sourceForHighlights.fromPixelToSample(x, y);
                        let closestPoint = this.getClosestTooltipPoint(point, x, y, this.targetSource?.dataSource, false);
                        if (this.tooltipPoint.isEquivalent(closestPoint)) closestPoint.handleClick();
                    }
                }

                break;
            }

            case "wheel":
            {
                if (!this.owner.zoomable || this.areaSelector.selection) break;

                let wheelEvent = <WheelEvent>e;
                if (!this.clipForDataArea.hitCheck(wheelEvent.offsetX, wheelEvent.offsetY)) break;

                if (this.owner.zoomState?.handleScrollEvent(wheelEvent))
                {
                    this.clearTooltip();
                }

                break;
            }

            case "mouseleave":
                if (this.handleMouseleave()) redraw = true;
                break;

            case "mousemove":
            {
                if (this.owner.zoomingViaScrubber) break;

                mouseEvent = <MouseEvent>e;
                x          = mouseEvent.offsetX;
                y          = mouseEvent.offsetY;

                if (this.owner.zoomState)
                {
                    this.owner.zoomState.mousemoveOnChart();
                }

                if (!this.tracingAnnotation)
                {
                    this.owner.setCursor(null);
                }

                if (this.handleMousemove(x, y)) redraw = true;

                break;
            }
        }

        return redraw;
    }

    handleMouseleave(): boolean
    {
        let redraw = false;
        if (this.highlightPoint != null)
        {
            this.highlightPoint = null;
            redraw              = true;
        }

        if (this.annotationPoint != null)
        {
            this.annotationPoint = null;
            redraw               = true;
        }

        if (this.clearTooltip()) redraw = true;

        if (!this.targetSource && this.hasTargetSourceDisplay())
        {
            this.setMouseoverTargetSource(null);
            redraw = true;
        }

        if (this.areaSelector.clearSelection()) redraw = true;

        return redraw;
    }

    handleMousemove(x: number,
                    y: number): boolean
    {
        let redraw = false;

        if (this.clipForDataArea.hitCheck(x, y))
        {
            if (this.tooltipText && this.clearTooltip()) redraw = true;

            if (this.sourceForHighlights)
            {
                let maxSampleDistance = 8;
                let targetChartSource = this.targetSource?.dataSource;

                let point = this.sourceForHighlights.fromPixelToSample(x, y);
                if (point && (!this.highlightPoint || this.highlightPoint.point != point.point))
                {
                    this.highlightPoint = point;
                    redraw              = true;
                }

                if (this.tracingAnnotation)
                {
                    if (this.areaSelector.type === CanvasZoneSelectionType.Point)
                    {
                        let closestPoint = this.findClosestValue(x, y, maxSampleDistance, targetChartSource);
                        if (this.annotationPoint != closestPoint)
                        {
                            redraw               = true;
                            this.annotationPoint = closestPoint;
                        }

                        this.owner.setCursor(this.annotationPoint ? "pointer" : null);
                    }

                    return redraw;
                }

                if (this.setTooltipPoint(this.getClosestTooltipPoint(point, x, y, targetChartSource, true))) redraw = true;
            }
        }
        else
        {
            if (this.highlightPoint || this.tooltipPoint)
            {
                this.reset();
                redraw = true;
            }

            let axisLabelForTooltip: string;
            if (this.leftLabelTruncated && this.clipForLeftAxis.hitCheck(x, y))
            {
                axisLabelForTooltip = this.groupForLeftAxis?.group.label;
            }
            else if (this.rightLabelTruncated && this.clipForRightAxis.hitCheck(x, y))
            {
                axisLabelForTooltip = this.groupForRightAxis?.group.label;
            }

            if (this.setTooltipText(axisLabelForTooltip, x, y)) redraw = true;
        }

        return redraw;
    }

    private getClosestTooltipPoint(point: ChartPointWithTransform<any>,
                                   x: number,
                                   y: number,
                                   targetChartSource: ChartPointSource<any>,
                                   updateMouseOverSource: boolean,
                                   maxSampleDistance: number = 32): TooltipPoint
    {
        let newTooltipPoint: TooltipPoint = null;
        let mouseOverSource: ChartPointSource<any>;

        if (point != null)
        {
            let maxMarkerDistance = maxSampleDistance / 2; // marker already gets priority: make it possible to highlight points around marker
            let closestValue      = this.findClosestValue(x, y, maxSampleDistance, targetChartSource);
            let closestMarker     = this.findClosestMarker(x, y, targetChartSource);

            if (closestMarker != null && closestMarker.distance < maxMarkerDistance)
            {
                newTooltipPoint = new TooltipPointForMarker(closestMarker.transform, closestMarker.value);

                if (!targetChartSource) mouseOverSource = closestMarker.source;
            }
            else if (closestValue != null) // already did distance check
            {
                newTooltipPoint = new TooltipPointForSample(closestValue.transform, closestValue.point, closestValue.pixel);

                if (!targetChartSource) mouseOverSource = closestValue.source.dataSource;
            }
        }

        if (updateMouseOverSource) this.setMouseoverTargetSource(mouseOverSource);

        return newTooltipPoint;
    }

    handleDrag(e: MouseEvent,
               mouseDown: boolean,
               mouseUp: boolean): boolean
    {
        let redraw = false;
        let x      = e.offsetX;
        let y      = e.offsetY;

        if (this.areaSelector.selectionStarted)
        {
            this.setMouseoverTargetSource(null);
            this.clearTooltip();

            let selectionExecuted = this.areaSelector.handleSelection(e, mouseDown, mouseUp);
            let selection         = this.areaSelector.selection;
            if (selectionExecuted)
            {
                selection.panel = this.owner.panelIndex(this);
                selection.establishValues(this.groupForLeftAxis.transform);
                this.owner.newlyDefinedAnnotation = selection;
                this.owner.annotationDefined.emit(selection);
            }
            else
            {
                this.owner.newlyDefinedAnnotation = null;
            }

            redraw = !!selection || !!this.areaSelector.selection;
        }
        else if (this.draggingTooltipCursor)
        {
            this.handleAnnotationTooltipDrag(this.draggingTooltip, e, mouseDown, mouseUp);
            redraw = true;
        }
        else if (mouseDown)
        {
            for (let annotation of this.rawPanel.annotations)
            {
                if (this.owner.movableAnnotationTooltips && annotation.showing && annotation.tooltipHitCheck(x, y))
                {
                    this.handleAnnotationTooltipDrag(annotation, e, mouseDown, mouseUp);
                    break;
                }
            }

            if (!this.draggingTooltip && this.tracingAnnotation && this.groupForLeftAxis)
            {
                this.areaSelector.handleSelection(e, mouseDown, mouseUp);
            }
        }

        return redraw;
    }

    private handleAnnotationTooltipDrag(annotation: CanvasZoneSelection,
                                        e: MouseEvent,
                                        mouseDown: boolean,
                                        mouseUp: boolean)
    {
        let cursorPosition = new Vector2(e.offsetX, e.offsetY);

        if (mouseDown)
        {
            this.mousedownTarget       = <HTMLElement>e.target;
            this.draggingTooltip       = annotation;
            this.draggingTooltipCursor = cursorPosition;
            this.owner.setCursor("grabbing");
            this.clearTooltip();
            this.setMouseoverTargetSource(null);
        }
        else if (mouseUp)
        {
            this.clearAnnotationTooltipDrag(annotation);
        }
        else if (this.draggingTooltip != null && e.target === this.mousedownTarget)
        {
            let diffVector                       = cursorPosition.differenceVector(this.draggingTooltipCursor);
            this.draggingTooltipCursor           = cursorPosition;
            annotation.tooltipOffset             = annotation.tooltipOffset.add(diffVector);
            this.draggingTooltip.tooltipDragging = true;
            this.highlightPoint                  = null;
            this.owner.setCursor("grabbing");
        }
    }

    private clearAnnotationTooltipDrag(annotation: CanvasZoneSelection)
    {
        this.owner.annotationTooltipMoved.emit(annotation);
        this.draggingTooltip = this.draggingTooltipCursor = this.mousedownTarget = null;
        this.owner.setCursor("grab");
    }

    private hasTargetSourceDisplay(): boolean
    {
        if (this.targetSource) return true;

        for (let group of this.groups)
        {
            for (let source of group.sources)
            {
                if (!isVisible(source.state)) continue;
                return source.state === VisualizationDataSourceState.Muted || source.state === VisualizationDataSourceState.Target;
            }
        }

        return true;
    }

    private findClosestValue(xPixel: number,
                             yPixel: number,
                             maxDistance: number,
                             targetSource?: ChartPointSource<any>): HighlightedSourceValue
    {
        let closest = null;

        for (let group of this.groups)
        {
            let closestForGroup = group.findClosestValue(xPixel, yPixel, maxDistance, targetSource);
            if (closestForGroup)
            {
                if (closest == null || closest.distance > closestForGroup.distance)
                {
                    closest = closestForGroup;
                }
            }
        }

        return closest;
    }

    private findClosestMarker(xPixel: number,
                              yPixel: number,
                              targetSource?: ChartPointSource<any>): HighlightedSourceMarker
    {
        let closest = null;
        for (let group of this.groups)
        {
            let closestForSource = group.findClosestMarker(xPixel, yPixel, targetSource);
            if (closestForSource)
            {
                if (closest == null || closest.distance > closestForSource.distance)
                {
                    closest = closestForSource;
                }
            }
        }

        return closest;
    }

    private clearTooltip(): boolean
    {
        let redraw = false;
        if (this.tooltipPoint)
        {
            this.tooltipPoint = null;
            redraw            = true;
        }

        if (this.tooltipText)
        {
            this.tooltipText = null;
            redraw           = true;
        }

        if (redraw) this.owner.tooltip.remove();
        return redraw;
    }


    private setTooltipPoint(newTooltipPoint: TooltipPoint): boolean
    {
        if (!newTooltipPoint) return this.tooltipPoint && this.clearTooltip();

        if (newTooltipPoint.clickable) this.owner.setCursor("pointer");

        if (!this.tooltipText && this.tooltipPoint == newTooltipPoint) return false;

        this.tooltipText  = null;
        this.tooltipPoint = newTooltipPoint;

        return true;
    }

    private setTooltipText(text: string,
                           x: number,
                           y: number): boolean
    {
        if (text && !isNaN(x) && !isNaN(y))
        {
            this.tooltipText  = text;
            this.tooltipPoint = null;
            this.owner.tooltip.render(x, y, undefined, text);
            return true;
        }

        return false;
    }
}

export class ProcessedGroup
{
    sources: ProcessedDataSource[] = [];

    private m_majorTicksY: number[]       = [];
    private m_majorTicksYValues: string[] = [];

    private m_domain = ChartTimeWindow.EmptyPlaceholder;
    public transform = new ChartValueTransform(ChartTimeWindow.EmptyPlaceholder, ChartClipArea.EmptyPlaceholder);

    get domain(): ChartTimeWindow
    {
        return this.m_domain;
    }

    constructor(public readonly panel: ProcessedPanel,
                public readonly group: ChartGroup)
    {
        for (let source of group.sources)
        {
            if (source) this.sources.push(new ProcessedDataSource(this, source));
        }
    }

    startStreamingSamples()
    {
        for (let processedDataSource of this.sources)
        {
            processedDataSource.startStreamingSamples();
        }
    }

    stopStreamingSamples()
    {
        for (let processedDataSource of this.sources)
        {
            processedDataSource.stopStreamingSamples();
        }
    }

    incrementActiveCount()
    {
        this.panel.incrementActiveCount();
    }

    decrementActiveCount()
    {
        this.panel.decrementActiveCount();
    }

    notifyNewSamples(source: ChartPointSource<any>,
                     timestamp: moment.Moment)
    {
        source.flushMergedRange();

        for (let processedDataSource of this.sources)
        {
            if (processedDataSource.dataSource == source)
            {
                processedDataSource.group.transform.flushCache();
            }
        }

        this.panel.notifyNewSamples(source, timestamp);
    }

    prepareDomain(requestedRange: ChartTimeRange,
                  displayedRange: ChartTimeRange,
                  width: number,
                  height: number)
    {
        let valueRange = new ChartValueRange();
        let onlyEnums  = true;

        this.m_majorTicksYValues = null;

        for (let processedDataSource of this.sources)
        {
            let enumRange = processedDataSource.updateValueRangeForPointsInTimeRange(valueRange, displayedRange.minAsMoment, displayedRange.maxAsMoment, width, height);
            if (!enumRange)
            {
                onlyEnums                = false;
                this.m_majorTicksYValues = null;
            }
            else
            {
                if (!this.m_majorTicksYValues) this.m_majorTicksYValues = enumRange; // Select the first enumerated range.
            }
        }

        if (this.group.rangeOverride) valueRange.forceToRange(this.group.rangeOverride);

        if (!valueRange.diff)
        {
            const buffer = 0.1;
            valueRange.min -= buffer;
            valueRange.max += buffer;
        }

        // Calculate tick spread
        let tickInfo = ChartHelpers.getMajorTickInfo(height, verticalSpacingForTicks, valueRange, onlyEnums);

        // Calculate a value range such that the are (verticalDataPadding / 2) pixels of space between the highest
        // rendered value and the top of the chart and the lowest rendered value and bottom of the chart.
        // Only use at most 1/8th total height for padding per side when chart is small
        let pxBuffer     = Math.min(height / 4, verticalDataPadding);
        let rangePadding = ((pxBuffer * valueRange.diff) / height) / (1 - (pxBuffer / height)) / 2;
        let lower        = valueRange.min - rangePadding;
        let upper        = valueRange.max + rangePadding;

        // don't cut off the last tick's text if it's an enum
        if (onlyEnums && verticalDataPadding < height / 4) upper += rangePadding;

        // Assign chart domain/range
        this.m_domain = new ChartTimeWindow(displayedRange.minInMillisec, displayedRange.maxInMillisec, lower, upper);

        // Calculate ticks to render
        this.m_majorTicksY = tickInfo.generateArray();
    }

    prepareLayout(clip: ChartClipArea)
    {
        this.transform = new ChartValueTransform(this.m_domain, clip);
    }

    renderMarkers(helper: ChartHelpers,
                  selectedRangeStart: ChartPointWithTransform<any>,
                  selectedRangeEnd: ChartPointWithTransform<any>,
                  stateType?: VisualizationDataSourceState)
    {
        for (let processedDataSource of this.sources)
        {
            let state = processedDataSource.state;
            if (isVisible(state) && (!stateType || state === stateType))
            {
                processedDataSource.renderMarkers(helper, this.transform, selectedRangeStart, selectedRangeEnd);
            }
        }
    }

    renderGrid(helper: ChartHelpers,
               inner: ChartClipArea,
               ticksX: ChartTimelineTick[])
    {
        let timestamps = ticksX.map((tick) => tick.timestampInMillisec);
        CanvasRenderer.renderGrid(helper.canvas, this.transform, inner.x, inner.right, inner.y, inner.bottom, timestamps, this.m_majorTicksY);
    }

    renderTicks(helper: ChartHelpers,
                outer: ChartClipArea,
                inner: ChartClipArea,
                axisLocation: RelativeLocation)
    {
        if (this.group.noTicks) return;

        CanvasRenderer.renderTicks(helper, this.transform, this.m_majorTicksY, this.m_majorTicksYValues, undefined, outer, inner, axisLocation, true);
    }

    //--//

    findClosestValue(xPixel: number,
                     yPixel: number,
                     maxDistance: number,
                     targetSource: ChartPointSource<any>): HighlightedSourceValue
    {
        let closest = null;

        if (targetSource)
        {
            let source = this.sources.find((source) => source.dataSource === targetSource);
            if (source) closest = source.findClosestValue(this.transform, xPixel, yPixel, maxDistance);
        }
        else
        {
            for (let source of this.sources)
            {
                if (!isVisible(source.state)) continue;

                let closestForSource = source.findClosestValue(this.transform, xPixel, yPixel, maxDistance);
                if (closestForSource)
                {
                    if (closest == null || closest.distance > closestForSource.distance)
                    {
                        closest = closestForSource;
                    }
                }
            }
        }

        return closest;
    }

    findClosestMarker(xPixel: number,
                      yPixel: number,
                      targetSource?: ChartPointSource<any>): HighlightedSourceMarker
    {
        let closest = null;

        if (targetSource)
        {
            let source = this.sources.find((source) => source.dataSource === targetSource);
            if (source) closest = source.findClosestMarker(this.transform, xPixel, yPixel);
        }
        else
        {
            for (let source of this.sources)
            {
                if (!isVisible(source.state)) continue;

                let closestForSource = source.findClosestMarker(this.transform, xPixel, yPixel);
                if (closestForSource)
                {
                    if (closest == null || closest.distance > closestForSource.distance)
                    {
                        closest = closestForSource;
                    }
                }
            }
        }

        return closest;
    }

    findPixelToSample(source: ProcessedDataSource,
                      x: number,
                      y: number): ChartPointWithTransform<any>
    {
        if (!this.transform)
        {
            return null;
        }

        let point = this.transform.fromPixelToSample(source.dataSource, x, y);
        return point ? new ChartPointWithTransform(point, this.transform) : null;
    }

    fromTimestampToXCoordinate(date: moment.Moment)
    {
        return this.transform.fromTimestampToXCoordinate(date);
    }

    fromMillisecondToXCoordinate(msec: number)
    {
        return this.transform.fromMillisecondToXCoordinate(msec);
    }

    fromValueToYCoordinate(y: number): number
    {
        return this.transform.fromValueToYCoordinate(y);
    }
}

export class ProcessedDataSource
{
    get state(): VisualizationDataSourceState
    {
        return this.dataSource.state;
    }

    get index(): number
    {
        return this.dataSource.index;
    }

    constructor(public readonly group: ProcessedGroup,
                public readonly dataSource: ChartPointSource<any>)
    {
    }

    setState(state: VisualizationDataSourceState,
             override: boolean = false)
    {
        if (override)
        {
            this.dataSource.state = state;
        }
        else if (this.dataSource.state !== VisualizationDataSourceState.Disabled && this.dataSource.state !== state)
        {
            this.dataSource.state = state;
        }
    }

    restore(state: VisualizationDataSourceState): boolean
    {
        return this.dataSource.restore(state);
    }

    startStreamingSamples()
    {
        if (this.dataSource.provider)
        {
            this.dataSource.provider.startStreamingSamples(1000, {
                transitionToActive: () =>
                {
                    this.group.incrementActiveCount();
                },

                transitionToInactive: () =>
                {
                    this.group.decrementActiveCount();
                },

                newSamples: (timestamp: moment.Moment) =>
                {
                    this.group.notifyNewSamples(this.dataSource, timestamp);
                }
            });
        }
        else
        {
            this.group.incrementActiveCount();
            this.group.decrementActiveCount();
        }
    }

    stopStreamingSamples()
    {
        if (this.dataSource.provider)
        {
            this.dataSource.provider.stopStreamingSamples();
        }
    }

    updateValueRangeForPointsInTimeRange(sharedRange: ChartValueRange,
                                         startDate: moment.Moment,
                                         endDate: moment.Moment,
                                         width: number,
                                         height: number): string[]
    {
        if (this.state === VisualizationDataSourceState.Deleted) return null;

        let sourceRange = new ChartValueRange();
        let startMs     = startDate.valueOf();
        let endMs       = endDate.valueOf();

        let showMovingAverage     = this.dataSource.showMovingAverage > 0;
        let onlyShowMovingAverage = showMovingAverage && this.dataSource.onlyShowMovingAverage;

        if (!onlyShowMovingAverage)
        {
            let enumRange = this.dataSource.updateValueRange(sourceRange, startMs, endMs);
            if (enumRange)
            {
                sharedRange.expandToContain(sourceRange);

                return enumRange;
            }
        }
        else
        {
            //
            // Unfortunately, we need to create a temporary transformer, since the range computed here is used to create the panel transformers.
            //
            let domain    = new ChartTimeWindow(startMs, endMs, -1000, 1000);
            let clip      = new ChartClipArea(0, 0, width, height);
            let transform = new ChartValueTransform(domain, clip);

            let view       = this.movingAverage(transform, this.dataSource.getMergedRange());
            let timestamps = view.timestamps;
            let values     = view.values;

            let firstValue = undefined;
            let lastValue  = undefined;

            for (let index = 0; index < timestamps.length; index++)
            {
                let val = values[index];
                if (isFinite(val))
                {
                    let xMillisec = timestamps[index];
                    if (xMillisec < startMs)
                    {
                        firstValue = val;
                    }
                    else if (xMillisec > endMs)
                    {
                        if (lastValue === undefined) lastValue = val;
                    }
                    else
                    {
                        sourceRange.expandForValue(val);
                    }
                }
            }
        }

        if (this.dataSource.rangeOverride)
        {
            sourceRange.forceToRange(this.dataSource.rangeOverride);
        }

        sharedRange.expandToContain(sourceRange);

        return null;
    }

    findClosestValue(transform: ChartValueTransform,
                     xPixel: number,
                     yPixel: number,
                     maxDistance: number): HighlightedSourceValue
    {
        let closest: HighlightedSourceValue = null;

        let showMovingAverage     = this.dataSource.showMovingAverage > 0;
        let onlyShowMovingAverage = showMovingAverage && this.dataSource.onlyShowMovingAverage;

        if (!onlyShowMovingAverage)
        {
            let minMS = transform.fromXCoordinateToMillisecond(xPixel - maxDistance);
            let maxMS = transform.fromXCoordinateToMillisecond(xPixel + maxDistance);

            for (let range of this.dataSource.ranges)
            {
                let minIdx = range.findInsertionPoint(minMS);
                if (minIdx < 0) minIdx = ~minIdx;

                let maxIdx = range.findInsertionPoint(maxMS);
                if (maxIdx < 0) maxIdx = ~maxIdx;

                let timestamps = range.getTimestamps();
                let values     = range.getNumericValues();

                minIdx = Math.max(minIdx - 2, 0);
                maxIdx = Math.min(maxIdx + 2, timestamps.length);

                let previousXCoord: number;
                let previousYCoord: number;

                const toPoint = (index: number) => range.toPoint(index);

                for (let i = minIdx; i < maxIdx; i++)
                {
                    if (!isFinite(values[i]))
                    {
                        previousXCoord = undefined;
                        previousYCoord = undefined;
                        continue;
                    }

                    let xCoord = transform.fromMillisecondToXCoordinate(timestamps[i]);
                    let yCoord = transform.fromValueToYCoordinate(values[i]);

                    if (previousXCoord !== undefined)
                    {
                        switch (this.dataSource.lineType)
                        {
                            case ChartLineType.StepLeft:
                                closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, previousXCoord, previousYCoord, xCoord, previousYCoord, xPixel, yPixel);
                                closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, xCoord, previousYCoord, xCoord, yCoord, xPixel, yPixel);
                                break;

                            case ChartLineType.StepRight:
                                closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, previousXCoord, previousYCoord, previousXCoord, yCoord, xPixel, yPixel);
                                closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, previousXCoord, yCoord, xCoord, yCoord, xPixel, yPixel);
                                break;

                            default:
                                closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, previousXCoord, previousYCoord, xCoord, yCoord, xPixel, yPixel);
                                break;
                        }
                    }

                    previousXCoord = xCoord;
                    previousYCoord = yCoord;
                }
            }
        }
        else
        {
            let view       = this.movingAverage(transform, this.dataSource.getMergedRange());
            let timestamps = view.timestamps;
            let values     = view.values;

            let minTimestamp = view.fromXCoordinateToTimestamp(xPixel - maxDistance);
            let maxTimestamp = view.fromXCoordinateToTimestamp(xPixel + maxDistance);

            let minIdx = UtilsService.binarySearchForFloat64Array(timestamps, timestamps.length, minTimestamp);
            if (minIdx < 0) minIdx = ~minIdx;

            let maxIdx = UtilsService.binarySearchForFloat64Array(timestamps, timestamps.length, maxTimestamp);
            if (maxIdx < 0) maxIdx = ~maxIdx;

            minIdx = Math.max(minIdx - 2, 0);
            maxIdx = Math.min(maxIdx + 2, timestamps.length);

            let previousXCoord: number;
            let previousYCoord: number;

            const toPoint = (index: number) => this.dataSource.convertToPoint(timestamps[index], values[index], ChartPointType.Value, false);

            for (let i = minIdx; i < maxIdx; i++)
            {
                if (!isFinite(values[i]))
                {
                    previousXCoord = undefined;
                    previousYCoord = undefined;
                    continue;
                }

                let xCoord = transform.fromMillisecondToXCoordinate(timestamps[i]);
                let yCoord = transform.fromValueToYCoordinate(values[i]);

                if (previousXCoord !== undefined)
                {
                    closest = this.checkDistanceToLine(closest, maxDistance, transform, toPoint, i, previousXCoord, previousYCoord, xCoord, yCoord, xPixel, yPixel);
                }

                previousXCoord = xCoord;
                previousYCoord = yCoord;
            }
        }

        return closest;
    }

    private checkDistanceToLine(closest: HighlightedSourceValue,
                                maxDistance: number,
                                transform: ChartValueTransform,
                                toPoint: (index: number) => ChartPoint<any>,
                                index: number,
                                x1: number,
                                y1: number,
                                x2: number,
                                y2: number,
                                xPixel: number,
                                yPixel: number): HighlightedSourceValue
    {
        let pixel = ChartHelpers.pointOnLineClosestToTarget(x1, y1, x2, y2, xPixel, yPixel, true);
        let x     = pixel.x;
        let y     = pixel.y;

        let dist = ChartHelpers.pointDistance(xPixel, yPixel, x, y);
        if (dist <= maxDistance)
        {
            if (closest == null || closest.distance > dist)
            {
                if (x !== x2) index--;
                let point = toPoint(index);

                closest = new HighlightedSourceValue(this, point, transform, pixel, dist);
            }
        }

        return closest;
    }

    findClosestMarker(transform: ChartValueTransform,
                      xPixel: number,
                      yPixel: number): HighlightedSourceMarker
    {
        let closest = null;

        for (let marker of this.dataSource.markers)
        {
            let pixel = marker.toPixel(transform);
            let dist  = ChartHelpers.pointDistance(xPixel, yPixel, pixel.x, pixel.y);

            if (closest == null || closest.distance > dist)
            {
                closest = new HighlightedSourceMarker(transform, marker, dist, this.dataSource);
            }
        }

        return closest;
    }

    private shouldAggregate(transform: ChartValueTransform,
                            range: ChartPointRange<any>): boolean
    {
        // Get the pixel-space data values for the given range
        let view = this.extractVisibleValues(transform, range, 0);

        let numPoints = view.length;
        if (numPoints > 1)
        {
            let firstX = view.fromIndexToXCoordinate(0);
            let lastX  = view.fromIndexToXCoordinate(numPoints - 1);

            if (numPoints * 4 > (lastX - firstX))
            {
                // Data is dense, we should aggregate
                return true;
            }
        }

        // Data is sparse, no aggregation needed
        return false;
    }

    renderArea(canvas: CanvasRenderingContext2D,
               transform: ChartValueTransform,
               fillStyle: string,
               alpha: number)
    {
        // Set the opacity for the area for all ranges
        canvas.globalAlpha = alpha;

        let showMovingAverage     = this.dataSource.showMovingAverage > 0;
        let onlyShowMovingAverage = showMovingAverage && this.dataSource.onlyShowMovingAverage;

        if (!onlyShowMovingAverage)
        {
            // Render an area below the data for each discrete range
            for (let range of this.dataSource.ranges)
            {
                let view: ChartPointsRenderView;

                // Check if we should aggregate or not
                if (this.shouldAggregate(transform, range))
                {
                    // Decimate data (aggregate)
                    let views = this.decimateValues(transform, range);

                    // Select the correct data as the upper bound of the area
                    let showDecimation = !this.dataSource.hideDecimation || this.dataSource.state === VisualizationDataSourceState.Target;
                    view               = showDecimation && views.low || views.mid;
                }
                else
                {
                    // Select the raw data as the bound of the area
                    view = this.extractVisibleValues(transform, range, 0);
                }

                this.renderHelper(canvas, transform, view, null, null, null, fillStyle, alpha);
            }
        }
        else
        {
            let view = this.movingAverage(transform, this.dataSource.getMergedRange());

            this.renderHelper(canvas, transform, view, null, null, null, fillStyle, alpha);
        }
    }

    renderLine(canvas: CanvasRenderingContext2D,
               transform: ChartValueTransform,
               width: number,
               strokeStyle: string,
               strokeStyleForMissing: string,
               strokeStyleForNoValue: string,
               alpha: number)
    {
        // Set the line width for all segments
        canvas.lineWidth = width;

        let showMovingAverage     = this.dataSource.showMovingAverage > 0;
        let onlyShowMovingAverage = showMovingAverage && this.dataSource.onlyShowMovingAverage;

        if (!onlyShowMovingAverage)
        {
            // Render all discrete ranges as lines
            for (let range of this.dataSource.ranges)
            {
                // Check if we need to decimate values
                if (this.shouldAggregate(transform, range))
                {
                    // Decimate data (aggregate)
                    let views = this.decimateValues(transform, range);

                    // If we are not hiding decimation and our decimation is valid, render decimation area
                    let showDecimation  = this.dataSource.isDiscrete() || !this.dataSource.hideDecimation || this.dataSource.state === VisualizationDataSourceState.Target;
                    let decimationColor = ChartColorUtilities.safeChroma(strokeStyle)
                                                             .brighten(0.5)
                                                             .desaturate(0.2);
                    if (showDecimation)
                    {
                        canvas.globalAlpha = alpha * 0.666;

                        // Render the upper and lower bounds as a area range
                        // TODO: Allow for showing as line instead of area as well
                        this.renderRangeArea(canvas, views.high, views.low, decimationColor.hex());
                    }

                    if (this.dataSource.isDiscrete())
                    {
                        this.renderHelper(canvas, transform, views.low, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                        this.renderHelper(canvas, transform, views.high, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                    }
                    else
                    {
                        switch (this.dataSource.decimationDisplay)
                        {
                            case "Minimum":
                                this.renderHelper(canvas, transform, views.low, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                                break;

                            case  "Maximum":
                                this.renderHelper(canvas, transform, views.high, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                                break;

                            default:
                                this.renderHelper(canvas, transform, views.mid, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                                break;
                        }
                    }
                }
                else
                {
                    let view = this.extractVisibleValues(transform, range, 0);
                    this.renderHelper(canvas, transform, view, strokeStyle, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
                }
            }
        }

        if (showMovingAverage)
        {
            let view = this.movingAverage(transform, this.dataSource.getMergedRange());

            let chromaColor = onlyShowMovingAverage ? strokeStyle : ChartColorUtilities.safeChroma(strokeStyle)
                                                                                       .brighten(1.4)
                                                                                       .hex();

            this.renderHelper(canvas, transform, view, chromaColor, strokeStyleForMissing, strokeStyleForNoValue, null, alpha);
        }
    }

    private renderHelper(canvas: CanvasRenderingContext2D,
                         transform: ChartValueTransform,
                         view: ChartPointsRenderView,
                         strokeStyle: string,
                         strokeStyleForMissing: string,
                         strokeStyleForNoValue: string,
                         fillStyle: string,
                         alpha: number)
    {
        canvas.globalAlpha = alpha;

        // The range must contain more than two pairs for smoothing to be applied
        if (this.dataSource.lineType == ChartLineType.Smooth && view.length > 2)
        {
            CanvasRenderer.renderSmooth(canvas,
                                        view,
                                        transform.getYZeroOffset(),
                                        strokeStyle,
                                        strokeStyleForMissing,
                                        strokeStyleForNoValue,
                                        this.dataSource.smoothness,
                                        fillStyle);
        }
        else
        {
            CanvasRenderer.renderStraight(canvas,
                                          view,
                                          transform.getYZeroOffset(),
                                          strokeStyle,
                                          strokeStyleForMissing,
                                          strokeStyleForNoValue,
                                          this.dataSource.lineType,
                                          fillStyle);
        }
    }

    renderRangeArea(canvas: CanvasRenderingContext2D,
                    viewUpper: ChartPointsRenderView,
                    viewLower: ChartPointsRenderView,
                    fillStyle: string = null)
    {
        let previousPixelX = viewUpper.fromIndexToXCoordinate(0);
        let previousPixelY = viewUpper.fromIndexToYCoordinate(0);
        let pixelX         = previousPixelX;
        let pixelY         = previousPixelY;

        // Start the path
        canvas.beginPath();
        canvas.moveTo(pixelX, pixelY);

        // Draw upper points
        let numPoints = viewUpper.length;
        for (let i = 1; i < numPoints; i++)
        {
            // Get the next pixel
            pixelX = viewUpper.fromIndexToXCoordinate(i);
            pixelY = viewUpper.fromIndexToYCoordinate(i);

            // Draw and extra horizontal line if stepping
            if (this.dataSource.lineType == ChartLineType.StepLeft)
            {
                // Over then up.
                canvas.lineTo(pixelX, previousPixelY);
            }
            else if (this.dataSource.lineType == ChartLineType.StepRight)
            {
                // Up then over.
                canvas.lineTo(previousPixelX, pixelY);
            }

            // Draw a line to the next point
            canvas.lineTo(pixelX, pixelY);

            // Commit current pixel to previous pixel
            previousPixelX = pixelX;
            previousPixelY = pixelY;
        }

        // Add the connecting bit between series and reset pixel pointers
        previousPixelX = viewLower.fromIndexToXCoordinate(numPoints - 1);
        previousPixelY = viewLower.fromIndexToYCoordinate(numPoints - 1);
        pixelX         = previousPixelX;
        pixelY         = previousPixelY;

        canvas.lineTo(pixelX, pixelY);

        // Draw lower points
        for (let i = numPoints - 2; i >= 0; i--)
        {
            // Get the next pixel
            pixelX = viewLower.fromIndexToXCoordinate(i);
            pixelY = viewLower.fromIndexToYCoordinate(i);

            // Draw and extra horizontal line if stepping
            // These are reversed from the upper line because we are drawing in reverse order
            if (this.dataSource.lineType == ChartLineType.StepRight)
            {
                // Over then up.
                canvas.lineTo(pixelX, previousPixelY);
            }
            else if (this.dataSource.lineType == ChartLineType.StepLeft)
            {
                // Up then over.
                canvas.lineTo(previousPixelX, pixelY);
            }

            // Draw a line to the next point
            canvas.lineTo(pixelX, pixelY);

            // Commit current pixel to previous pixel
            previousPixelX = pixelX;
            previousPixelY = pixelY;
        }

        // Finish the path
        canvas.closePath();

        // Fill the bounded area
        canvas.fillStyle = fillStyle;
        canvas.fill();
    }

    renderMarkers(helper: ChartHelpers,
                  transform: ChartValueTransform,
                  selectedRangeStart: ChartPointWithTransform<any>,
                  selectedRangeEnd: ChartPointWithTransform<any>)
    {
        let canvas = helper.canvas;

        if (selectedRangeStart && selectedRangeEnd && selectedRangeEnd.point.owningSource == this.dataSource)
        {
            let start = selectedRangeStart.point.toPixel(transform);
            let end   = selectedRangeEnd.point.toPixel(transform);

            canvas.save();
            canvas.globalAlpha = 0.5;
            canvas.fillStyle   = "#E0E0E0";
            canvas.fillRect(start.x, transform.clip.top, end.x - start.x, transform.clip.height);
            canvas.restore();
        }

        let overrideColor;
        if (this.state === VisualizationDataSourceState.Muted) overrideColor = MutedColor;

        for (let marker of this.dataSource.markers) marker.render(helper, transform, this.dataSource.markerLine, overrideColor);
    }

    extractVisibleValues(transform: ChartValueTransform,
                         range: ChartPointRange<any>,
                         leftMargin: number): ChartPointsRenderView
    {
        let prefix = `visibleValues/${leftMargin}`;
        return transform.computeIfMissing(range, prefix, (range) =>
        {
            let visibleValues = new ChartPointsRenderView(ChartPointToPixel.generateMapping(transform.domain, transform.clip, 0, 0));

            let startTimestamp = visibleValues.fromXCoordinateToTimestamp(transform.clip.x) - leftMargin * 1000;
            let endTimestamp   = visibleValues.fromXCoordinateToTimestamp(transform.clip.x + transform.clip.width);

            let bounds = range.findInternalRange(startTimestamp, endTimestamp, false);

            // Include the two points outside the window, if present.
            if (bounds.min > 0) bounds.min--;
            if (bounds.max < range.size) bounds.max++;

            if (bounds.min <= bounds.max)
            {
                let rawTimestamps = range.getTimestamps();
                let rawValues     = range.getNumericValues();
                let rawFlags      = range.getFlags();

                visibleValues.timestamps = rawTimestamps.subarray(bounds.min, bounds.max + 1);
                visibleValues.values     = rawValues.subarray(bounds.min, bounds.max + 1);
                visibleValues.flags      = rawFlags.subarray(bounds.min, bounds.max + 1);
            }
            else
            {
                //
                // Outside the range, no visible values.
                //
                visibleValues.timestamps = new Float64Array(0);
                visibleValues.values     = new Float64Array(0);
                visibleValues.flags      = new Int8Array(0);
            }

            return visibleValues;
        });
    }

    decimateValues(transform: ChartValueTransform,
                   range: ChartPointRange<any>): DecimationSummary
    {
        return transform.computeIfMissing(range, "decimatedValues", (range) =>
        {
            let aggregatedValue: AggregatedPixel = null;

            let source            = range.source;
            let enumRange         = source.getEnumeratedRange();
            let minPixelsPerPoint = source.autoAggregation ? 10 : 1;

            let view               = this.extractVisibleValues(transform, range, 0);
            let decimatedRangeLow  = new ChartPointRange(source);
            let decimatedRangeMid  = new ChartPointRange(source);
            let decimatedRangeHigh = new ChartPointRange(source);

            for (let index = 0; index < view.length; index++)
            {
                let x = Math.round(view.fromIndexToXCoordinate(index));

                if (aggregatedValue && Math.abs(x - aggregatedValue.x) >= minPixelsPerPoint)
                {
                    aggregatedValue.addPoint(view, decimatedRangeLow, decimatedRangeMid, decimatedRangeHigh);
                    aggregatedValue = null;
                }

                let value = view.values[index];

                if (!aggregatedValue)
                {
                    aggregatedValue = new AggregatedPixel(x, enumRange);
                }

                aggregatedValue.addNewValue(value, ChartPointRange.asType(view.flags[index]));
            }

            if (aggregatedValue)
            {
                aggregatedValue.addPoint(view, decimatedRangeLow, decimatedRangeMid, decimatedRangeHigh);
            }

            let timestamps = decimatedRangeMid.getTimestamps();
            let flags      = decimatedRangeMid.getFlags();

            let viewLow  = new ChartPointsRenderView(view.transferFunction);
            let viewMid  = new ChartPointsRenderView(view.transferFunction);
            let viewHigh = new ChartPointsRenderView(view.transferFunction);

            // Use the same timestamps array for all three, since they are identical.
            viewLow.timestamps  = timestamps;
            viewMid.timestamps  = timestamps;
            viewHigh.timestamps = timestamps;

            viewLow.values  = decimatedRangeLow.getNumericValues();
            viewMid.values  = decimatedRangeMid.getNumericValues();
            viewHigh.values = decimatedRangeHigh.getNumericValues();

            // Use the same flags array for all three, since they are identical.
            viewLow.flags  = flags;
            viewMid.flags  = flags;
            viewHigh.flags = flags;

            return new DecimationSummary(viewLow, viewMid, viewHigh);
        });
    }

    movingAverage(transform: ChartValueTransform,
                  range: ChartPointRange<any>): ChartPointsRenderView
    {
        let source = range.source;

        let leftMargin = source.showMovingAverage;
        if (leftMargin < 0 || leftMargin >= 1E9) leftMargin = 0;

        let prefix = `movingAverage/${leftMargin}`;
        return transform.computeIfMissing(range, prefix, (range) =>
        {
            let view           = this.extractVisibleValues(transform, range, leftMargin);
            let decimatedRange = new ChartPointRange(source);

            let timestamps = view.timestamps;
            let values     = view.values;
            let flags      = view.flags;
            let viewLength = view.length;

            //
            // Phase 1: compute an approximate average value, we'll build the rest of the data baised to that.
            //
            let approximateAverage = 0;
            let count              = 0;

            for (let index = 0; index < viewLength; index++)
            {
                if (ChartPointRange.asType(flags[index]) == ChartPointType.Value)
                {
                    approximateAverage += values[index];
                    count++;
                }
            }

            if (count > 0)
            {
                approximateAverage /= count;
            }

            //
            // Phase 2: for each point, accumulate the area of the data, from the first point to the current one.
            //
            let lastTimestamp: number                   = undefined;
            let lastValue: number                       = undefined;
            let runningTimeFromRoughAverage             = 0;
            let runningAreaFromRoughAverage             = 0;
            let valuesCumulativeDeltaAroundRoughAverage = new Float64Array(viewLength);

            for (let index = 0; index < viewLength; index++)
            {
                let timestamp = timestamps[index];
                let value     = ChartPointRange.asType(flags[index]) == ChartPointType.Value ? (values[index] - approximateAverage) : lastValue;

                if (lastTimestamp !== undefined)
                {
                    let deltaTime = timestamp - lastTimestamp;
                    let area      = (value + lastValue) * deltaTime / 2;

                    runningTimeFromRoughAverage += deltaTime;
                    runningAreaFromRoughAverage += area;
                }

                lastTimestamp = timestamp;
                lastValue     = value;

                valuesCumulativeDeltaAroundRoughAverage[index] = runningAreaFromRoughAverage;
            }

            let average              = runningTimeFromRoughAverage > 0 ? (runningAreaFromRoughAverage / runningTimeFromRoughAverage) + approximateAverage : approximateAverage;
            let computeMovingAverage = source.showMovingAverage < 1E9;

            //
            // Phase 3: interpolate the moving average at each pixel.
            //
            let minX = Math.floor(transform.clip.x);
            let maxX = transform.fromMillisecondToXCoordinate(lastTimestamp);

            if (computeMovingAverage)
            {
                let cursorLeft  = new MovingAverageCursor(timestamps, valuesCumulativeDeltaAroundRoughAverage);
                let cursorRight = new MovingAverageCursor(timestamps, valuesCumulativeDeltaAroundRoughAverage);
                let delta       = source.showMovingAverage * 1000;

                while (minX <= maxX)
                {
                    let timestamp = view.fromXCoordinateToTimestamp(minX);

                    let interpolatedLeft  = cursorLeft.interpolate(timestamp - delta);
                    let interpolatedRight = cursorRight.interpolate(timestamp);

                    let diffValue = interpolatedRight.value - interpolatedLeft.value;
                    let diffTime  = interpolatedRight.timestamp - interpolatedLeft.timestamp;

                    let movingAverage = diffTime > 0 ? (diffValue / diffTime) + approximateAverage : approximateAverage;
                    decimatedRange.addPointRaw(timestamp, movingAverage, ChartPointType.Value);
                    minX++;
                }
            }
            else
            {
                while (minX <= maxX)
                {
                    let timestampRight = view.fromXCoordinateToTimestamp(minX);

                    decimatedRange.addPointRaw(timestampRight, average, ChartPointType.Value);
                    minX++;
                }
            }

            let viewAverage        = new ChartPointsRenderView(view.transferFunction);
            viewAverage.timestamps = decimatedRange.getTimestamps();
            viewAverage.values     = decimatedRange.getNumericValues();
            viewAverage.flags      = decimatedRange.getFlags();

            return viewAverage;
        });
    }

    fromPixelToSample(x: number,
                      y: number): ChartPointWithTransform<any>
    {
        return this.group.findPixelToSample(this, x, y);
    }
}

class MovingAverageCursor
{
    private m_cursor = 0;

    constructor(private m_timestamps: Float64Array,
                private m_values: Float64Array)
    {
    }

    interpolate(timestamp: number): { timestamp: number, value: number }
    {
        let cursorLeft = this.m_cursor;
        let timestamps = this.m_timestamps;
        let maxCursor  = timestamps.length - 1;

        while (timestamps[cursorLeft] > timestamp && cursorLeft > 0) cursorLeft--;
        while (cursorLeft < maxCursor && timestamps[cursorLeft + 1] <= timestamp) cursorLeft++;

        let cursorRight = cursorLeft;
        while (timestamps[cursorRight] <= timestamp && cursorRight < maxCursor) cursorRight++;

        this.m_cursor = cursorLeft;

        let timestampLeft = timestamps[cursorLeft];
        let valueLeft     = this.m_values[cursorLeft];

        if (cursorLeft == cursorRight)
        {
            return {
                timestamp: timestampLeft,
                value    : valueLeft
            };
        }
        else
        {
            let timestampRight = timestamps[cursorRight];
            let valueRight     = this.m_values[cursorRight];

            let timestampDelta = timestampRight - timestampLeft;
            let valueDelta     = valueRight - valueLeft;
            let value          = valueLeft + (valueDelta / timestampDelta * (timestamp - timestampLeft));

            return {
                timestamp: timestamp,
                value    : value
            };
        }
    }
}

class DecimationSummary
{
    constructor(public readonly low: ChartPointsRenderView,
                public readonly mid: ChartPointsRenderView,
                public readonly high: ChartPointsRenderView)
    {}
}

export interface SplineControlPoints
{
    controlPoint1: ChartPixel;
    controlPoint2: ChartPixel;
}

class HighlightedSourceValue
{
    constructor(public readonly source: ProcessedDataSource,
                public readonly point: ChartPoint<any>,
                public readonly transform: ChartValueConverter<any>,
                public readonly pixel: ChartPixel,
                public readonly distance: number)
    {
    }
}

class HighlightedSourceMarker
{
    constructor(public readonly transform: ChartValueTransform,
                public readonly value: ChartMarker<any>,
                public readonly distance: number,
                public readonly source?: ChartPointSource<any>)
    {
    }
}

//--//

class AggregatedPixel
{
    private m_samples       = 0;
    public m_samplesMissing = 0;
    public m_samplesNoValue = 0;
    private m_low           = 0;
    private m_middle        = 0;
    private m_high          = 0;
    private m_frequency: number[];

    constructor(public readonly x: number,
                enumRange: string[])
    {
        if (enumRange)
        {
            this.m_frequency = new Array<number>(enumRange.length);
            this.m_frequency.fill(0);
        }
    }

    addPoint(view: ChartPointsRenderView,
             decimatedRangeLow: ChartPointRange<any>,
             decimatedRangeMid: ChartPointRange<any>,
             decimatedRangeHigh: ChartPointRange<any>)
    {
        let timestamp = view.fromXCoordinateToTimestamp(this.x);
        let type: ChartPointType;

        if (this.m_samplesMissing / this.m_samples > .3)
        {
            type = ChartPointType.Missing;
        }
        else if (this.m_samplesNoValue / this.m_samples > .3)
        {
            type = ChartPointType.NoValue;
        }
        else
        {
            type = ChartPointType.Value;
        }

        decimatedRangeLow.addPointRaw(timestamp, this.m_low, type);
        decimatedRangeHigh.addPointRaw(timestamp, this.m_high, type);

        if (this.m_frequency)
        {
            let mostFrequent = -1;
            let value        = 0;

            for (let i = 0; i < this.m_frequency.length; i++)
            {
                if (mostFrequent < this.m_frequency[i])
                {
                    value        = i;
                    mostFrequent = this.m_frequency[i];
                }
            }

            decimatedRangeMid.addPointRaw(timestamp, value, type);
        }
        else
        {
            decimatedRangeMid.addPointRaw(timestamp, this.m_middle / this.m_samples, type);
        }
    }

    public addNewValue(value: number,
                       type: ChartPointType)
    {
        if (this.m_samples == 0)
        {
            this.m_low  = value;
            this.m_high = value;
        }
        else
        {
            this.m_low  = Math.min(this.m_low, value);
            this.m_high = Math.max(this.m_high, value);
        }

        this.m_samples++;

        if (this.m_frequency)
        {
            value = UtilsService.clamp(0, this.m_frequency.length - 1, Math.floor(value));

            this.m_frequency[value]++;
        }
        else
        {
            this.m_middle += value;
        }

        switch (type)
        {
            case ChartPointType.Missing:
                this.m_samplesMissing++;
                break;

            case ChartPointType.NoValue:
                this.m_samplesNoValue++;
                break;
        }
    }
}

export interface ChartRangeSelectionHandler
{
    acceptRange(dataSource: ChartPointSource<any>,
                start: moment.Moment,
                end: moment.Moment): boolean;

    clearRange(): void;
}
