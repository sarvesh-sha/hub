import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, SimpleChanges, ViewChild} from "@angular/core";

import {ControlPointMetadata, DeletionAffectedAnnotation, InteractableSource, InteractableSourcesChart, TimeSeriesAnnotationConfigurationExtended, TimeSeriesAxisConfigurationExtended, TimeSeriesChartConfigurationExtended, TimeSeriesChartHandler, TimeSeriesPanelConfigurationExtended, TimeSeriesSourceConfigurationExtended, TimeSeriesSourceHost, ToggleableNumericRangeExtended} from "app/customer/visualization/time-series-utils";
import * as SharedSvc from "app/services/domain/base.service";
import {EngineeringUnitsDescriptorExtended, UnitsService} from "app/services/domain/units.service";
import * as Models from "app/services/proxy/model/models";
import {AddTimeSeriesSourceEvent} from "app/shared/charting/add-related-source/add-related-source.component";
import {AnnotationOverlayComponent} from "app/shared/charting/annotations/annotation-overlay.component";
import {SourceConfigurerComponent} from "app/shared/charting/source-configurer/source-configurer.component";
import {TimeSeriesChartConfigurationComponent} from "app/shared/charting/time-series-chart/time-series-chart-configuration.component";
import {TimeSeriesChartingComponent} from "app/shared/charting/time-series-container/common";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {AxisWidth, CanvasZoneSelection, CanvasZoneSelectionType, ChartAxis, ChartGroup, ChartPanel, MinEdgePadding} from "framework/ui/charting/app-charting-utilities";
import {ChartTimelineComponent, ChartZoomState} from "framework/ui/charting/chart-timeline.component";
import {ChartComponent, ChartRangeSelectionHandler, ProcessedDataSource} from "framework/ui/charting/chart.component";
import {Vector2} from "framework/ui/charting/charting-math";
import {isVisible, VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {inParallel, mapInParallel, mapInParallelNoNulls} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";

@Component({
               selector       : "o3-time-series-chart",
               templateUrl    : "./time-series-chart.component.html",
               styleUrls      : ["./time-series-chart.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class TimeSeriesChartComponent extends SharedSvc.BaseApplicationComponent implements TimeSeriesChartingComponent,
                                                                                            InteractableSourcesChart
{
    invalidSource = false;

    panels: ChartPanel[]                  = [];
    private m_reloadPanels                = false;
    private m_changedSources: Set<string> = new Set();

    annotations: CanvasZoneSelection[] = [];
    title: string                      = null;

    m_chartTimeRange = new ChartTimeRange();
    get chartTimeRange(): ChartTimeRange
    {
        return this.m_chartTimeRange;
    }

    private m_staticTimeRange: ChartTimeRange;
    get staticTimeRange(): ChartTimeRange
    {
        return this.m_staticTimeRange || this.m_chartTimeRange;
    }

    editSourceExt: TimeSeriesSourceConfigurationExtended;

    newAnnotationType: CanvasZoneSelectionType;
    newAnnotationSelection: CanvasZoneSelection;
    onlyPointAnnotations = false;

    private manipulatingScrubber = false;

    private m_debouncer = new AsyncDebouncer<void>(30, () => this.onChangeInternal());

    private sourceSub: Subscription;

    private m_editConfigExt: TimeSeriesChartConfigurationExtended;

    private m_configExt: TimeSeriesChartConfigurationExtended = TimeSeriesChartConfigurationExtended.emptyInstance(this.app);
    @Input() set configExt(configExt: TimeSeriesChartConfigurationExtended)
    {
        if (configExt && configExt !== this.m_configExt)
        {
            this.m_configExt    = configExt;
            this.m_reloadPanels = true;

            if (this.sourceSub) this.sourceSub.unsubscribe();
            this.sourceSub = this.subscribeToObservable(this.m_configExt.configChanged, (needsRebuild: boolean) =>
            {
                if (needsRebuild)
                {
                    this.m_reloadPanels = true;
                    this.onChange();
                }
                else
                {
                    this.sourcesChanged();
                }

                this.chartUpdated.emit();
            });

            this.newAnnotationType = this.newAnnotationSelection = undefined;
        }
    }

    get configExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_configExt;
    }

    get activeConfigExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_editConfigExt || this.m_configExt;
    }

    get activeChartHandler(): TimeSeriesChartHandler
    {
        return this.activeConfigExt.chartHandler;
    }

    //--//

    zoomSource: ProcessedDataSource;

    private m_externalZoomState: ChartZoomState;
    @Input() set externalZoomState(zoomState: ChartZoomState)
    {
        this.m_chartTimeRange    = null;
        this.m_externalZoomState = zoomState;
    }

    get externalZoomState(): ChartZoomState
    {
        return this.m_externalZoomState;
    }

    chartZoomState: ChartZoomState;
    private m_staticZoomState: ChartZoomState;

    get zoomState(): ChartZoomState
    {
        if (this.m_externalZoomState) return this.m_externalZoomState;

        if (this.zoomable)
        {
            return this.chartZoomState;
        }
        else if (!this.m_staticZoomState)
        {
            this.m_staticZoomState = new ChartZoomState(null, this.staticTimeRange);
        }

        return this.m_staticZoomState;
    }

    private m_rangeChanged = false;
    private m_range: Models.RangeSelection;
    @Input() set range(range: Models.RangeSelection)
    {
        this.m_range        = range;
        this.m_reloadPanels = true;
        this.m_rangeChanged = true;
    }

    get range(): Models.RangeSelection
    {
        return this.m_range;
    }

    @Input() showSources: boolean       = false;
    @Input() hideAxes: boolean          = false;
    @Input() noVerticalPadding: boolean = false;

    get showTimeline(): boolean
    {
        return this.zoomable && !this.m_externalZoomState;
    }

    private m_hasLeftAxis: boolean;
    private m_hasRightAxis: boolean;

    get timelineLeft(): number
    {
        return this.m_hasLeftAxis ? AxisWidth : MinEdgePadding;
    }

    get timelineRight(): number
    {
        return this.m_hasRightAxis ? AxisWidth : MinEdgePadding;
    }

    @Input() selectionHandler: ChartRangeSelectionHandler;

    @Input() allowAnnotations = false;

    private m_zoomable = true;
    @Input() set zoomable(zoomable: boolean)
    {
        if (!zoomable)
        {
            this.m_staticTimeRange = this.chartZoomState?.displayedRange;
            this.chartZoomState    = null;
            this.m_staticZoomState = null;
        }
        this.m_zoomable = zoomable;
    }

    get zoomable(): boolean
    {
        return this.m_zoomable;
    }

    @Output() startedFetchingData = new EventEmitter<void>();
    @Output() stoppedFetchingData = new EventEmitter<void>();
    @Output() configExtChange     = new EventEmitter<TimeSeriesChartConfigurationExtended>();

    @Output() hideSourcesOverriden = new EventEmitter<boolean>();
    @Output() sourceStatesUpdated  = new EventEmitter<Lookup<VisualizationDataSourceState>>();

    @Output() chartUpdated = new EventEmitter<boolean>();

    @ViewChild(ChartComponent, {static: true}) optio3Chart: ChartComponent;
    @ViewChild(ChartTimelineComponent) chartTimeline: ChartTimelineComponent;
    @ViewChild(AnnotationOverlayComponent) annotationDialog: AnnotationOverlayComponent;
    @ViewChild(SourceConfigurerComponent, {static: true}) sourceConfigurer: SourceConfigurerComponent;
    @ViewChild(TimeSeriesChartConfigurationComponent, {static: true}) chartConfigurer: TimeSeriesChartConfigurationComponent;

    private readonly host: TimeSeriesSourceHost;

    constructor(inj: Injector,
                private m_elementRef: ElementRef)
    {
        super(inj);

        this.host = new TimeSeriesSourceHost(this);
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);

        this.onChange();
    }

    onChange()
    {
        this.m_debouncer.invoke();
    }

    sourceChanged(timeShiftChanged: boolean)
    {
        if (timeShiftChanged)
        {
            this.m_changedSources.add(this.editSourceExt.identifier);
        }

        this.onChange();
    }

    //--//

    configurationChanged(chartExt: TimeSeriesChartConfigurationExtended,
                         write: boolean)
    {
        chartExt?.adoptSourceData(this.activeConfigExt);
        if (write)
        {
            if (chartExt)
            {
                chartExt.model.display.size = this.configExt.model.display.size;
                this.configExt              = chartExt;
                this.configExtChange.emit(this.m_configExt);
            }
        }
        else
        {
            this.m_reloadPanels  = this.m_editConfigExt != chartExt;
            this.m_editConfigExt = chartExt;
            this.onChange();

            this.hideSourcesOverriden.emit(chartExt?.model.display.hideSources);
        }
    }

    //--//

    setCursor(cursorCss: string)
    {
        this.m_elementRef.nativeElement.style.cursor = cursorCss || "";
    }

    draggingScrubber(cursor?: string)
    {
        let dragging = !!cursor;
        if (this.manipulatingScrubber !== dragging)
        {
            this.setCursor(cursor);

            this.manipulatingScrubber = dragging;
            if (dragging)
            {
                this.optio3Chart.setCursor(null);
                this.optio3Chart.reset();
            }
        }
    }

    getNumSources(): number
    {
        return this.configExt.sourcesExt.reduce((cum,
                                                 sourceExt) => sourceExt.valid ? cum + 1 : cum, 0);
    }

    toggleConfigurer()
    {
        this.sourceConfigurer.closeOverlay();
        this.chartConfigurer.toggleOverlay();
    }

    toggleColorConfigurer(): void
    {}

    onMouseMove(x: number,
                y: number): boolean
    {
        return this.optio3Chart.onMouseMove(x, y);
    }

    onMouseLeave()
    {
        this.optio3Chart.onMouseLeave();
    }

    //--//

    toggleAnnotations()
    {
        this.annotationDialog.toggleOverlay();
    }

    registerNewAnnotationType(type: CanvasZoneSelectionType)
    {
        this.newAnnotationType = type;
        this.markForCheck();
    }

    annotationDefined(selection: CanvasZoneSelection)
    {
        this.newAnnotationSelection = selection;
        if (!selection) this.newAnnotationType = undefined;

        this.annotationDialog.openOverlay();
        this.markForCheck();
    }

    updateAnnotationTooltipOffset(selection: CanvasZoneSelection)
    {
        let index      = this.annotations.indexOf(selection);
        let annotation = this.activeConfigExt.model.annotations[index];
        if (annotation)
        {
            annotation.tooltipOffsetX = selection.tooltipOffset.x;
            annotation.tooltipOffsetY = selection.tooltipOffset.y;
        }

        this.annotationChanged();
    }

    deleteAnnotation(annotation: CanvasZoneSelection,
                     index: number)
    {
        let deletedAnnotation = this.activeConfigExt.model.annotations.splice(index, 1)[0];

        let panel                 = this.panels[deletedAnnotation.panel];
        let panelAnnotationsIndex = panel.annotations.indexOf(annotation);
        panel.annotations.splice(panelAnnotationsIndex, 1);

        this.annotationChanged();
    }

    reinsertAnnotation(affected: DeletionAffectedAnnotation,
                       panelIdx: number): number
    {
        let index = Math.min(affected.idx, this.annotations.length);
        this.annotations.splice(index, 0, affected.selection);

        let rawPanelAnnotations = this.panels[panelIdx].annotations;
        rawPanelAnnotations.push(affected.selection);

        let indices = UtilsService.extractMappedLookup(rawPanelAnnotations, (selection) => this.annotations.indexOf(selection));
        rawPanelAnnotations.sort((a,
                                  b) => UtilsService.compareNumbers(indices[a.id], indices[b.id], true));

        return index;
    }

    updatePanelAnnotations(selection: CanvasZoneSelection)
    {
        this.newAnnotationType = undefined;
        let annotationIndex    = this.annotations.indexOf(selection);
        let activeConfigExt    = this.activeConfigExt;
        if (annotationIndex < 0)
        {
            this.annotations.push(selection);
            this.panels[selection.panel].annotations.push(selection);

            let annotationConfig = TimeSeriesChartComponent.newAnnotationModel(selection, activeConfigExt.sourcesExt);
            let annotationExt    = new TimeSeriesAnnotationConfigurationExtended(annotationConfig, activeConfigExt.panelsExt[selection.panel]);
            activeConfigExt.annotationsExt.push(annotationExt);
            activeConfigExt.validateAnnotations();
            this.annotations = UtilsService.arrayCopy(this.annotations);
        }
        else
        {
            let model         = activeConfigExt.model.annotations[annotationIndex];
            model.hideTooltip = selection.hideTooltip;
            model.title       = selection.title;
            model.description = selection.description;
        }

        this.annotationChanged();
    }

    private annotationChanged()
    {
        this.configExt.configChanged.next(false);
        this.optio3Chart.queueUpdate();
    }

    private async setUpAnnotations()
    {
        this.onlyPointAnnotations = this.panels.every((panel) => panel.groups.every((group) => !group.useAsLeftValueAxis));

        for (let panel of this.panels) panel.annotations = [];

        let activeConfigExt = this.activeConfigExt;
        let annotations     = await mapInParallelNoNulls(activeConfigExt.model.annotations || [], async (annotation) =>
        {
            let annotationType = annotation?.type;
            if (!annotationType) return null;

            let type;
            let selection;
            switch (annotationType)
            {
                case Models.TimeSeriesAnnotationType.Point:
                    type = CanvasZoneSelectionType.Point;

                    let sourceId = annotation.sourceId;
                    let sourceDesc;
                    if (sourceId)
                    {
                        const uuidLength     = 36;
                        let associatedSource = activeConfigExt.sourcesExt.find((sourceExt) => sourceExt.identifier == sourceId || sourceExt.model.uuid === sourceId);
                        if (associatedSource)
                        {
                            let sourceData = associatedSource.getChartData();
                            let point      = sourceData?.findPoint(annotation.minX);
                            if (point)
                            {
                                selection = CanvasZoneSelection.fromChartPoint(point);
                            }
                        }
                        else if (sourceId?.length === uuidLength * 2)
                        {
                            let cpMeta = await ControlPointMetadata.fromId(this.app, sourceId.substring(uuidLength));
                            sourceDesc = await cpMeta?.standardDescription();
                        }
                    }

                    if (!selection)
                    {
                        selection               = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.Point, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                        selection.invalidReason = "Source not present with selected context(s)";
                        if (sourceDesc) selection.invalidReason += "\n" + sourceDesc;
                    }
                    break;

                case Models.TimeSeriesAnnotationType.XRange:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.XRange, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                case Models.TimeSeriesAnnotationType.XRangeInverted:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.XRangeInverted, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                case Models.TimeSeriesAnnotationType.YRange:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.YRange, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                case Models.TimeSeriesAnnotationType.YRangeInverted:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.YRangeInverted, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                case Models.TimeSeriesAnnotationType.Area:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.Area, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                case Models.TimeSeriesAnnotationType.AreaInverted:
                    selection = CanvasZoneSelection.fromValues(CanvasZoneSelectionType.AreaInverted, annotation.minX, annotation.minY, annotation.maxX, annotation.maxY);
                    break;

                default:
                    return null;
            }

            let panel = this.panels[annotation.panel];
            if (!panel) return null;

            selection.panel         = annotation.panel;
            selection.tooltipOffset = new Vector2(annotation.tooltipOffsetX, annotation.tooltipOffsetY);
            selection.title         = annotation.title;
            selection.description   = annotation.description;
            selection.hideTooltip   = annotation.hideTooltip;

            panel.annotations.push(selection);

            return selection;
        }) || [];

        if (!!this.annotations?.length)
        {
            let idToAnnotation = UtilsService.extractLookup(this.annotations);
            for (let annotation of annotations) annotation.copyState(idToAnnotation[annotation?.id]);
        }

        this.annotations = annotations;
        this.chartUpdated.emit();
    }

    //--//

    sourcesChanged()
    {
        if (this.editSourceExt?.markedForDeletion) this.sourceConfigurer.toggleOverlay(false);

        this.sourceStatesUpdated.emit(this.activeConfigExt.getSourcesStates(this));
    }

    async buildGroups(panel: TimeSeriesPanelConfigurationExtended,
                      axis: TimeSeriesAxisConfigurationExtended): Promise<ChartGroup[]>
    {
        let hasAxis              = false;
        let groups: ChartGroup[] = [];

        if (axis.model.groupedFactors.length == 0)
        {
            let unitsCache: Lookup<Models.EngineeringUnitsFactors> = {};

            let sources = await panel.filterSources(this.host.app.domain.units, axis, undefined);
            for (let source of sources)
            {
                let factors                                          = source.unitsFactors;
                unitsCache[UnitsService.computeHash(factors, false)] = factors;
            }

            for (let hash in unitsCache)
            {
                let factors = unitsCache[hash];

                axis.model.groupedFactors.push(Models.TimeSeriesAxisGroupConfiguration.newInstance({
                                                                                                       keyFactors     : factors,
                                                                                                       selectedFactors: factors
                                                                                                   }));
            }
        }

        let axisIndex     = panel.indexOfAxis(axis);
        let display       = axis.model.displayFactors;
        let noMoreSources = axis.lastSourceDeleting;
        for (let groupConfig of axis.model.groupedFactors)
        {
            let group                 = new ChartGroup();
            group.useAsLeftValueAxis  = !noMoreSources && axisIndex === 0 && UnitsService.areEquivalent(groupConfig.keyFactors, display);
            group.useAsRightValueAxis = !noMoreSources && axisIndex === 1 && UnitsService.areEquivalent(groupConfig.keyFactors, display);

            let sources = await panel.filterSources(this.host.app.domain.units, axis, groupConfig.keyFactors || null);
            for (let source of sources)
            {
                group.sources.push(source.getChartData());
            }

            // Get the unit metadata from the first source
            if (!axis.hideLabel)
            {
                if (groupConfig.selectedFactors)
                {
                    let ext = await this.app.domain.units.resolveDescriptor(groupConfig.selectedFactors, false);
                    this.setUpLabel(group, ext);
                }
                else if (groupConfig.keyFactors)
                {
                    let ext = await this.app.domain.units.resolveRootDescriptor(groupConfig.keyFactors);
                    this.setUpLabel(group, ext);
                }
            }

            let toggleableNumericRangeExt = new ToggleableNumericRangeExtended(groupConfig.override);
            if (toggleableNumericRangeExt.isActive) group.rangeOverride = toggleableNumericRangeExt.chartRange;

            hasAxis = hasAxis || (group.useAsLeftValueAxis || group.useAsRightValueAxis) && !!group.label;

            groups.push(group);
        }

        if (axisIndex === 0)
        {
            this.m_hasLeftAxis ||= hasAxis;
        }
        else
        {
            this.m_hasRightAxis ||= hasAxis;
        }

        // Return all the groups for this axis
        return groups;
    }

    public setUpLabel(group: ChartGroup,
                      ext: EngineeringUnitsDescriptorExtended)
    {
        if (ext)
        {
            group.label      = ext.generateLabel();
            group.unitsLabel = ext.model.displayName;
        }
    }

    private getSourceExt(sourceId: string): TimeSeriesSourceConfigurationExtended
    {
        return this.activeConfigExt.sourcesExt.find((sourceExt) => sourceExt.identifier === sourceId);
    }

    getSourceState(sourceId: string): VisualizationDataSourceState
    {
        let sourceExt = this.getSourceExt(sourceId);
        if (!sourceExt) return null;

        return this.optio3Chart.getState(sourceExt.getChartData(), sourceExt.model.panel);
    }

    getSource(sourceId: string): InteractableSource
    {
        return this.getSourceExt(sourceId);
    }

    isDeletable(sourceId: string): boolean
    {
        let sourceExt = this.getSourceExt(sourceId);
        if (!sourceExt) return true;

        return !sourceExt.model.pointBinding;
    }

    configureSource(sourceId: string)
    {
        this.editSourceExt = this.getSourceExt(sourceId);
        this.sourceConfigurer.openOverlay();
    }

    async addSource(sourceInfo: AddTimeSeriesSourceEvent)
    {
        let source              = await TimeSeriesSourceConfigurationExtended.resolveFromIdAndDimension(this.host, sourceInfo.sysId);
        source.model.timeOffset = sourceInfo.duration;

        await this.configExt.applySourceChanges([source], [], true);
        await this.onChange();

        this.editSourceExt = source;
    }

    isReady(): boolean
    {
        return this.panels.length > 0;
    }

    toggleTarget(sourceId: string,
                 fromMouseover: boolean)
    {
        let sourceExt = this.getSourceExt(sourceId);
        if (!sourceExt) return;

        this.optio3Chart.toggleTargetSource(sourceExt.getChartData(), sourceExt.model.panel, fromMouseover, true);
    }

    toggleEnabled(sourceId: string)
    {
        let sourceExt = this.getSourceExt(sourceId);
        if (!sourceExt) return;

        this.optio3Chart.toggleSourceEnabled(sourceExt.getChartData(), sourceExt.model.panel);
    }

    multiToggleEnabled(originSourceId: string)
    {
        let originSourceExt = this.getSourceExt(originSourceId);
        if (!originSourceExt) return;

        let panel           = originSourceExt.model.panel;
        let viewableSources = this.getSources(panel);

        let turnOnAllSources = false;
        if (viewableSources.length <= 1) turnOnAllSources = viewableSources.length === 0 || viewableSources[0].getChartData() === originSourceExt.getChartData();

        if (turnOnAllSources)
        {
            this.optio3Chart.allOn(panel);
        }
        else
        {
            this.optio3Chart.singleOn(originSourceExt.getChartData(), originSourceExt.model.panel);
        }
    }

    getSources(panelIdx?: number,
               onlyVisible: boolean = true): TimeSeriesSourceConfigurationExtended[]
    {
        panelIdx = panelIdx ?? undefined;
        return this.activeConfigExt.sourcesExt.filter((sourceExt) =>
                                                      {
                                                          if (!isNaN(panelIdx) && panelIdx !== sourceExt.model.panel) return false;
                                                          if (!onlyVisible) return true;

                                                          let state = this.getSourceState(sourceExt.identifier);
                                                          return !state && sourceExt || isVisible(state);
                                                      });
    }

    refreshSize(): boolean
    {
        this.chartTimeline?.refreshSize();
        return this.optio3Chart.refreshSize();
    }

    updateZoomability(refreshChart: boolean = false)
    {
        let activeConfigExt = this.activeConfigExt;
        for (let panelExt of activeConfigExt.panelsExt)
        {
            for (let source of panelExt.sources)
            {
                source.updateZoomable(false);
            }
        }

        // If zoom is enabled, flag the last panel to include zoom
        if (this.zoomable)
        {
            let lastPanelExt = activeConfigExt.getLastPanelExtended();
            if (lastPanelExt)
            {
                for (let source of lastPanelExt.sources)
                {
                    source.updateZoomable(true);
                }
            }
        }

        if (refreshChart) this.optio3Chart.refreshZoom();
    }

    private async onChangeInternal()
    {
        // If no time range, flag and exit
        let configExt = this.activeConfigExt;
        if (!this.m_range || !configExt)
        {
            this.invalidSource = true;
            return;
        }

        // Flag as valid
        this.invalidSource = false;

        // Update time ranges
        let rangeExtended = new RangeSelectionExtended(this.m_range);
        if (this.m_rangeChanged || !this.m_chartTimeRange)
        {
            let min                = rangeExtended.getMin();
            let max                = rangeExtended.getMax();
            this.m_chartTimeRange  = new ChartTimeRange(min, max);
            this.m_staticZoomState = null;

            if (this.m_externalZoomState && this.m_rangeChanged)
            {
                this.m_externalZoomState.outerRange = this.m_chartTimeRange;
            }

            this.m_rangeChanged = false;
        }

        let reloadedPanels = this.m_reloadPanels;
        if (reloadedPanels || this.m_changedSources.size)
        {
            this.m_reloadPanels = false;

            await inParallel(configExt.sourcesExt, async (sourceExt) =>
            {
                // Sync the source cache and fetch missing data
                await sourceExt.bindToDataSource(this.host);
                await sourceExt.fetch(this.host, rangeExtended, this.m_changedSources.has(sourceExt.identifier));

                sourceExt.updateDataConfig(configExt.model.display, rangeExtended.model.zone);
            });
            this.m_changedSources = new Set();

            // Build up panels, groups and sources as described
            let configModel = configExt.model;
            this.title      = configModel.display.title;

            this.m_hasLeftAxis = this.m_hasRightAxis = false;
            this.panels        = await mapInParallel(configExt.panelsExt, async (panelExt) =>
            {
                let panel = new ChartPanel();

                // Build up groups for this panel
                panel.groups = [
                    ...await this.buildGroups(panelExt, panelExt.leftAxisExtended),
                    ...await this.buildGroups(panelExt, panelExt.rightAxisExtended)
                ];
                panel.establishGroupForGrid();

                panel.leftAxis  = this.buildAxis(panelExt.leftAxisExtended);
                panel.rightAxis = this.buildAxis(panelExt.rightAxisExtended);

                // Add timezone offset if needed
                if (this.m_range) panel.zone = rangeExtended.model.zone;

                // Set additional flags and params
                panel.hideBottomAxis = panel.hideSideAxes = this.hideAxes;
                if (configModel.display.panelSpacing) panel.topPadding = configModel.display.panelSpacing;
                panel.showAlerts = configModel.display.showAlerts;

                // Return the panel
                return panel;
            });

            for (let panelExt of configExt.panelsExt)
            {
                for (let source of panelExt.sources) source.updateDeletedState();
            }
        }
        else
        {
            for (let sourceExt of this.activeConfigExt.sourcesExt) sourceExt.updateDataConfig(configExt.model.display);
        }

        this.updateZoomability();

        if (reloadedPanels)
        {
            this.markForCheck();
        }
        else
        {
            this.optio3Chart.queueUpdate();
        }

        this.chartUpdated.emit();
    }

    private buildAxis(axisExt: TimeSeriesAxisConfigurationExtended): ChartAxis
    {
        let model  = axisExt.model;
        let axis   = new ChartAxis();
        axis.label = model.label;
        axis.color = model.color;

        if (axisExt.panel.indexOfAxis(axisExt) === 0)
        {
            this.m_hasLeftAxis ||= !!axis.label;
        }
        else if (axisExt.panel.indexOfAxis(axisExt) === 1)
        {
            this.m_hasRightAxis ||= !!axis.label;
        }

        return axis;
    }

    public async dataFetched()
    {
        this.stoppedFetchingData.emit();

        await this.setUpAnnotations();

        this.optio3Chart.queueUpdate();
    }

    public getCanvasPNG(): string
    {
        return this.optio3Chart && this.optio3Chart.getCanvasPNG();
    }

    public getCanvasTitle(): string
    {
        return this.title || "Chart";
    }

    private static newAnnotationModel(selection: CanvasZoneSelection,
                                      sourcesExt: TimeSeriesSourceConfigurationExtended[]): Models.TimeSeriesAnnotationConfiguration
    {
        if (!selection?.hasValues) return null;

        let type;
        let sourceId;
        switch (selection.type)
        {
            case CanvasZoneSelectionType.Point:
                type = Models.TimeSeriesAnnotationType.Point;

                let chartSource      = selection.chartSource;
                let associatedSource = sourcesExt.find((sourceExt) => sourceExt.getChartData() === chartSource);
                if (associatedSource) sourceId = associatedSource.identifier;
                break;

            case CanvasZoneSelectionType.XRange:
                type = Models.TimeSeriesAnnotationType.XRange;
                break;

            case CanvasZoneSelectionType.XRangeInverted:
                type = Models.TimeSeriesAnnotationType.XRangeInverted;
                break;

            case CanvasZoneSelectionType.YRange:
                type = Models.TimeSeriesAnnotationType.YRange;
                break;

            case CanvasZoneSelectionType.YRangeInverted:
                type = Models.TimeSeriesAnnotationType.YRangeInverted;
                break;

            case CanvasZoneSelectionType.Area:
                type = Models.TimeSeriesAnnotationType.Area;
                break;

            case CanvasZoneSelectionType.AreaInverted:
                type = Models.TimeSeriesAnnotationType.AreaInverted;
                break;
        }

        return Models.TimeSeriesAnnotationConfiguration.newInstance(
            {
                sourceId   : sourceId,
                hideTooltip: selection.hideTooltip,
                title      : selection.title,
                description: selection.description,
                panel      : selection.panel,
                type       : type,
                minX       : selection.minX,
                maxX       : selection.maxX,
                minY       : selection.minY,
                maxY       : selection.maxY
            }
        );
    }
}
