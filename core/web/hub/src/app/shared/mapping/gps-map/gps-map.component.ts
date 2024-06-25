import {ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Injector, Input, Output, ViewChild} from "@angular/core";

import {InteractableSource, InteractableSourcesChart, TimeSeriesChartConfigurationExtended, TimeSeriesSourceConfigurationExtended} from "app/customer/visualization/time-series-utils";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {AssetExtended, DeviceElementExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {TimeSeriesChartingComponent} from "app/shared/charting/time-series-container/common";
import {ColorPickerConfigurationComponent} from "app/shared/colors/color-picker-configuration.component";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {GpsMapConfigurerComponent} from "app/shared/mapping/gps-map/gps-map-configurer.component";
import {TimeScrubberComponent} from "app/shared/mapping/gps-map/time-scrubber.component";
import {GpsPath, GpsPathPoint, GpsPathSegment, PathMapComponent} from "app/shared/mapping/path-map/path-map.component";

import {Logger, LoggingService} from "framework/services/logging.service";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities, PaletteId} from "framework/ui/charting/core/colors";
import {VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {StandardFormOverlayComponent} from "framework/ui/overlays/standard-form-overlay.component";
import {inParallel, mapInParallel} from "framework/utils/concurrency";
import {AsyncDebouncer} from "framework/utils/debouncers";

import {Subscription} from "rxjs";


@Component({
               selector       : "o3-gps-map[range][gpsAsset], o3-gps-map[range][configExt]",
               templateUrl    : "./gps-map.component.html",
               styleUrls      : ["./gps-map.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class GpsMapComponent extends SharedSvc.BaseApplicationComponent implements TimeSeriesChartingComponent,
                                                                                   InteractableSourcesChart
{
    private readonly logger: Logger;

    // two subsequent location readings are separated into different segments if > maxDurationBetweenSegments sec elapses
    public static readonly maxDurationBetweenSegments: number = 3600; // in seconds
    private static readonly notTargetColor                    = "#d8d8d8";

    private m_buildDebouncer       = new AsyncDebouncer<void>(50, () => this.rebuild());
    private m_popupChangeDebouncer = new AsyncDebouncer<void>(10, () => this.handlePopupChange());

    locationIdToGpsSources: Lookup<GpsMapSource[]>;

    paths: GpsPath[] = [];

    private activeRangeExt: RangeSelectionExtended;
    deadZoneRanges: { lowMs: number, highMs: number }[];

    subRange: ChartTimeRange;

    selectedPathLocation: GpsPathPoint;
    nearbyPathLocations: GpsPathPoint[];
    nearbyLocationOptions: ControlOption<GpsPathPoint>[];

    ready: boolean   = false;
    hasData: boolean = false;

    popupLocked: boolean = false;

    scrubberIsMoving: boolean = false;

    private m_range: Models.RangeSelection;

    @Input() set range(value: Models.RangeSelection)
    {
        this.m_range = value;
        this.hasData = false;
        this.ready   = false;

        this.configurationChanged();
    }

    get range(): Models.RangeSelection
    {
        return this.m_range;
    }

    private m_gpsAsset: AssetExtended[];
    @Input() set gpsAsset(value: AssetExtended)
    {
        if (!value) return;
        if (this.m_gpsAsset && this.m_gpsAsset[0] === value) return;

        this.m_gpsAsset = [value];

        if (this.m_configExt?.mapSources) return;

        this.reset();
    }

    private readySub: Subscription;
    private sourceChangeSub: Subscription;

    private m_configExt: TimeSeriesChartConfigurationExtended;

    @Input() set configExt(config: TimeSeriesChartConfigurationExtended)
    {
        if (!config || this.m_configExt === config) return;

        if (this.m_configExt)
        {
            this.readySub.unsubscribe();
            this.sourceChangeSub.unsubscribe();
        }

        this.m_configExt = config;

        this.readySub        = this.subscribeToObservable(config.mapSourcesReady, () => this.updatePopupSetup());
        this.sourceChangeSub = this.subscribeToObservable(config.configChanged, () =>
        {
            this.hasData = false;
            this.ready   = false;
            this.detectChanges();
            this.reset();
        });

        this.colorConfigurer?.closeOverlay();
        this.colorSettings = Models.ColorConfiguration.newInstance({paletteName: <PaletteId>config.model.palette || "Map Colors"});

        this.reset();
    }

    get configExt(): TimeSeriesChartConfigurationExtended
    {
        return this.m_configExt;
    }

    private get mapSources(): AssetExtended[]
    {
        return this.configExt?.mapSources || [];
    }

    private get sourcesExt(): TimeSeriesSourceConfigurationExtended[]
    {
        return this.configExt?.sourcesExt || [];
    }

    get gpsAssets(): AssetExtended[]
    {
        return this.m_configExt?.mapSources || this.m_gpsAsset || [];
    }

    hasPopupEntries: boolean = false;

    locationNameLookup: Lookup<string>;

    colorSettings: Models.ColorConfiguration;

    colorOverlayConfig: OverlayConfig;

    private m_pathMap: PathMapComponent;
    @ViewChild(PathMapComponent) set pathMap(pathMap: PathMapComponent)
    {
        this.m_pathMap = pathMap;
        this.refreshSize();
    }

    @ViewChild("mapContainer", {static: true}) mapContainer: ElementRef<HTMLElement>;

    get noAmbiguityPopupTitle(): string
    {
        let onlyOption = this.nearbyLocationOptions[0];

        return !!onlyOption.children.length ? onlyOption.children[0].label : onlyOption.label;
    }

    get loadingText(): string
    {
        return this.ready ? "There is no location history to display." : "Loading location history...";
    }

    @ViewChild(TimeScrubberComponent) scrubber: TimeScrubberComponent;
    @ViewChild(GpsMapConfigurerComponent) popupConfigurer: GpsMapConfigurerComponent;
    @ViewChild(StandardFormOverlayComponent) colorConfigurer: StandardFormOverlayComponent;

    @Output() configExtUpdated    = new EventEmitter<void>();
    @Output() sourceStatesUpdated = new EventEmitter<Lookup<VisualizationDataSourceState>>();
    @Output() chartUpdated        = new EventEmitter<boolean>();
    @Output() stoppedFetchingData = new EventEmitter<void>();

    constructor(inj: Injector,
                loggingService: LoggingService)
    {
        super(inj);

        this.logger = loggingService.getLogger(GpsMapComponent);

        this.colorOverlayConfig = ColorPickerConfigurationComponent.colorOverlayConfig(true);
    }

    public getNumSources(): number
    {
        return this.mapSources.length;
    }

    public toggleConfigurer()
    {
        this.popupConfigurer?.toggleOverlay();
    }

    public toggleColorConfigurer()
    {
        if (this.gpsAssets?.length > 1) this.colorConfigurer?.toggleOverlay();
    }

    public onMouseMove(x: number,
                       y: number): boolean
    {
        return false;
    }

    public onMouseLeave(): void
    {
    }

    public updatePopup(locations: GpsPathPoint[])
    {
        this.nearbyPathLocations = locations;
        this.updateOptions();
    }

    public updateSubRange(range: ChartTimeRange)
    {
        this.subRange = range;
        this.updateOptions();
        this.markForCheck();
    }

    public refreshSize(): boolean
    {
        return this.mapContainer.nativeElement.clientHeight && this.m_pathMap.refreshSize();
    }

    public updateZoomability()
    {
        this.scrubber?.notifyLayoutChange();
    }

    public processPopupLockChange(locked: boolean)
    {
        if (this.popupLocked !== locked)
        {
            this.popupLocked = locked;
            this.detectChanges();
        }
    }

    private reset()
    {
        this.subRange = null;

        // trigger scrubber reset
        if (this.m_range) this.updateZoomability();

        this.updatePopupSetup();
    }

    private updateOptions()
    {
        if (!this.m_pathMap?.initialized || !this.nearbyPathLocations?.length || this.scrubberIsMoving)
        {
            this.nearbyLocationOptions = [];
            this.selectedPathLocation  = null;
            return;
        }

        let assetIds                    = new Set<string>(this.gpsAssets.map((asset) => asset.model.location.sysId));
        let relevantNearbyPathLocations = this.nearbyPathLocations.filter((pathLocation) => assetIds.has(pathLocation.locationId));

        if (this.subRange) relevantNearbyPathLocations = relevantNearbyPathLocations.filter((pathPoint) => this.subRange.isInRange(pathPoint.timestamp, true));

        let targetPath = this.paths.find((path) => path.state === VisualizationDataSourceState.Target);
        if (targetPath)
        {
            let targetLocationId        = targetPath.locationId;
            relevantNearbyPathLocations = relevantNearbyPathLocations.filter((pathPoint) => targetLocationId === pathPoint.locationId);
        }
        else
        {
            let enabledLocations = this.paths.filter((path) => path.state !== VisualizationDataSourceState.Disabled)
                                       .map((path) => path.locationId);

            relevantNearbyPathLocations = relevantNearbyPathLocations.filter(
                (pathPoint) => enabledLocations.some((locationId) => pathPoint.locationId === locationId));
        }

        if (relevantNearbyPathLocations.length > 0)
        {
            let pathLocationsByTime   = relevantNearbyPathLocations.sort((pathPointA,
                                                                          pathPointB) => UtilsService.compareNumbers(pathPointB.timestamp, pathPointA.timestamp, true));
            let locationOptionsLookup = new Map<string, ControlOption<GpsPathPoint>>();
            for (let pathLocation of pathLocationsByTime)
            {
                if (!locationOptionsLookup.has(pathLocation.locationId))
                {
                    locationOptionsLookup.set(pathLocation.locationId,
                                              new ControlOption(null, this.locationNameLookup[pathLocation.locationId], undefined, true));
                }

                let timestamp = MomentHelper.parse(pathLocation.timestamp, this.range?.zone);

                locationOptionsLookup.get(pathLocation.locationId)
                                     .children
                                     .push(new ControlOption(pathLocation, MomentHelper.friendlyFormatConciseUS(timestamp)));
            }

            let nestedOptions = [...locationOptionsLookup.values()];
            if (this.gpsAssets.length > 1)
            {
                this.nearbyLocationOptions = nestedOptions;
                this.selectedPathLocation  = this.nearbyLocationOptions[0]?.children[0]?.id;
            }
            else
            {
                this.nearbyLocationOptions = nestedOptions[0].children;
                this.selectedPathLocation  = this.nearbyLocationOptions[0]?.id;
            }
        }
        else
        {
            this.nearbyLocationOptions = [];
            this.selectedPathLocation  = null;
        }

        this.updateHasEntries();
    }

    public updateHasEntries()
    {
        this.hasPopupEntries = true;

        if (this.gpsAssets.length > 1 || !this.selectedPathLocation) return;

        let gpsMapSources = this.locationIdToGpsSources[this.selectedPathLocation.locationId];
        for (let gpsMapSource of gpsMapSources || [])
        {
            if (this.selectedPathLocation.relatedValues.get(gpsMapSource.deviceElementExt.model.sysId)) return;
        }

        this.hasPopupEntries = false;
    }

    private async setUpDefaultPopup()
    {
        let parentIds     = this.gpsAssets.map((asset) => asset.model.sysId);
        let deviceElemReq = Models.DeviceElementFilterRequest.newInstance({parentIDs: parentIds});
        let listResponse  = await this.app.domain.assets.getList(deviceElemReq);
        let deviceElems   = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, listResponse.results);

        // check if we got a graph and popup config or if a fresher set of gps assets came in during calculation
        if (this.m_configExt?.resolvedGraphs.size && this.m_configExt.model.tooltip ||
            !UtilsService.compareArraysAsSets(parentIds, this.gpsAssets.map((asset) => asset.model.sysId)))
        {
            return;
        }

        this.regenerateGpsSourceLookup(deviceElems.filter((deviceElem) => !deviceElem.isCoordinate)
                                                  .map((deviceElem) => new GpsMapSource(null, deviceElem)));

        this.configurationChanged();
    }

    private async updatePopupSetup()
    {
        let config        = this.m_configExt?.model;
        let tooltipConfig = config?.tooltip;
        let graph         = config?.graph;
        if (!tooltipConfig || !graph)
        {
            await this.setUpDefaultPopup();
            return;
        }

        if (!this.gpsAssets.length)
        {
            return;
        }

        let allRecordIds: Models.RecordIdentity[]       = [];
        let deviceElemIdToBindingLookup: Lookup<string> = {};

        let locationIds = this.gpsAssets.map((gpsAsset) => gpsAsset.model.location.sysId);
        graph.contexts  = [Models.AssetGraphContextLocations.newInstance({locationSysIds: locationIds})];

        let graphsExt = await SharedAssetGraphExtended.loadGraphs(this.app.domain, graph.sharedGraphs);
        for (const graphExt of graphsExt.values())
        {
            let responseHolder = await graphExt.resolveWithContext(graph.contexts);

            let uniquenessMaintainer = new Set<string>();
            await inParallel(tooltipConfig.entries, async (entry) =>
            {
                let identitiesByContext = await responseHolder.resolveIdentitiesByContext(entry.binding, true);
                for (let gpsAsset of this.gpsAssets)
                {
                    let locationId = gpsAsset.model.location.sysId;
                    for (let recordId of identitiesByContext[locationId] || [])
                    {
                        // avoid situation in which two bindings resolve to overlapping point(s)
                        let sysId = recordId.sysId;
                        if (!uniquenessMaintainer.has(sysId))
                        {
                            deviceElemIdToBindingLookup[sysId] = entry.binding.nodeId;
                            uniquenessMaintainer.add(sysId);

                            allRecordIds.push(recordId);
                        }
                    }
                }
            });
        }

        let deviceElemsExt = await this.app.domain.assets.getTypedExtendedBatch(DeviceElementExtended, allRecordIds);

        // check if we got a new config during calculation
        if (config !== this.m_configExt?.model) return;

        let gpsMapSources = deviceElemsExt.map((deviceElem) => new GpsMapSource(deviceElemIdToBindingLookup[deviceElem.model.sysId], deviceElem));
        this.regenerateGpsSourceLookup(gpsMapSources);

        this.configurationChanged();
    }

    private regenerateGpsSourceLookup(gpsMapSources: GpsMapSource[])
    {
        let lookup: Lookup<GpsMapSource[]> = {};
        for (let gpsAsset of this.gpsAssets) lookup[gpsAsset.model.location.sysId] = [];
        for (let gpsMapSource of gpsMapSources)
        {
            let sysId = gpsMapSource?.deviceElementExt?.model?.location?.sysId;
            if (sysId) lookup[sysId]?.push(gpsMapSource);
        }

        this.locationIdToGpsSources = lookup;
    }

    private configurationChanged()
    {
        // Short if no device or range
        if (!this.gpsAssets || !this.m_range) return;

        this.m_buildDebouncer.invoke();
    }

    public onChange()
    {
        this.configurationChanged();
    }

    private async rebuild()
    {
        if (!this.locationIdToGpsSources) return;

        this.ensureSourceColors();

        // Fetch fresh data
        await this.getData();

        if (!this.subRange)
        {
            this.updateSubRange(this.activeRangeExt.getChartRange());
        }
        else
        {
            // always want to update popup options; updateOptions is triggered from updateSubRange
            this.updateOptions();
        }

        this.updatePathColors();

        this.markForCheck();

        this.chartUpdated.emit();
    }

    private async getData()
    {
        this.activeRangeExt = new RangeSelectionExtended(this.m_range);
        let start           = this.activeRangeExt.getMin();
        let end             = this.activeRangeExt.getMax();

        let locationNames = await mapInParallel(this.gpsAssets, async (gpsAsset) =>
        {
            let locationExt = await gpsAsset.getLocation();
            return locationExt.getRecursiveName();
        });

        this.locationNameLookup = {};
        for (let i = 0; i < this.gpsAssets.length; i++) this.locationNameLookup[this.gpsAssets[i].model.location.sysId] = locationNames[i];

        let maxGapForSegment = this.app.domain.users.instanceConfiguration instanceof Models.InstanceConfigurationForDigitalMatter ? 7200 : 1000;
        let hasData          = false;
        let requestsArr      = await this.getPopupEntryRequests();
        let paths            = await mapInParallel(this.gpsAssets, async (gpsAsset,
                                                                          assetIdx) =>
        {
            let travelLog = await gpsAsset.getTravelLog(start, end, maxGapForSegment, GpsMapComponent.maxDurationBetweenSegments);
            let requests  = requestsArr[assetIdx];

            let otherControlPointsResponse = await this.app.domain.assets.getInterpolatedValues(requests, start, end);
            let otherControlPointsValues   = otherControlPointsResponse?.results.map((results) => results.values) || [];

            let friendlyUnits = await mapInParallel(requests, async (request) =>
            {
                let descriptor = await this.app.domain.units.resolveDescriptor(request.convertTo, false);
                return descriptor.model.displayName;
            });

            let controlPointIdx = -1;
            let timestamps      = otherControlPointsResponse?.timestamps || [];
            let numTimestamps   = timestamps.length;
            let cpSysIds        = this.locationIdToGpsSources[gpsAsset.model.location.sysId]?.map((gpsSource) => gpsSource.deviceElementExt.model.sysId) || [];
            let color           = undefined;
            if (this.gpsAssets.length > 1) color = ChartColorUtilities.getColor(assetIdx, <PaletteId>this.colorSettings.paletteName);

            let segments: GpsPathSegment[] = [];
            let numSegments                = travelLog?.segments?.length || 0;
            for (let segmentIndex = 0; segmentIndex < numSegments; segmentIndex++)
            {
                let resultSegment = travelLog.segments[segmentIndex];
                let numSamples    = resultSegment.timestamps.length;

                let path = new Array<GpsPathPoint>(numSamples);

                for (let sampleIndex = 0; sampleIndex < numSamples; sampleIndex++)
                {
                    let timestamp = resultSegment.timestamps[sampleIndex];
                    while (controlPointIdx + 1 < numTimestamps && timestamps[controlPointIdx + 1] < timestamp) controlPointIdx++;

                    let cpValues: string[] = otherControlPointsValues.map(
                        (values,
                         idx) =>
                        {
                            let value = values[controlPointIdx];
                            let units = friendlyUnits[idx];

                            if (isNaN(value) || !units) return "";

                            return `${UtilsService.getRoundedValue(value, 1)} ${units}`;
                        });

                    let loc = Models.LongitudeLatitude.newInstance({
                                                                       longitude: resultSegment.longitudes[sampleIndex],
                                                                       latitude : resultSegment.latitudes[sampleIndex]
                                                                   });

                    path[sampleIndex] = new GpsPathPoint(gpsAsset.model.location.sysId, loc, timestamp * 1_000, cpSysIds, cpValues, segmentIndex);
                }

                segments.push(new GpsPathSegment(path, segmentIndex, color));
            }

            if (numSegments > 0 && this.subRange)
            {
                let firstSegment = segments[0];
                let lastSegment  = segments[numSegments - 1];

                if (firstSegment.firstTimestamp < this.subRange.minInMillisec) this.subRange.minInMillisec = firstSegment.firstTimestamp;
                if (lastSegment.lastTimestamp > this.subRange.maxInMillisec) this.subRange.maxInMillisec = lastSegment.lastTimestamp;
            }

            if (segments.length > 0) hasData = true;

            let newPath = new GpsPath(segments, gpsAsset.model.location.sysId);

            let oldPath = this.paths.find((path) => path.locationId === newPath.locationId);
            if (oldPath) newPath.state = oldPath.state;

            return newPath;
        });

        this.stoppedFetchingData.emit();

        this.ready   = true;
        this.paths   = paths;
        this.hasData = hasData;

        if (hasData) this.buildDeadZoneRanges();
    }

    private async getPopupEntryRequests(): Promise<Models.TimeSeriesPropertyRequest[][]>
    {
        let tooltipConfig = this.m_configExt?.model.tooltip;
        return mapInParallel(this.gpsAssets, async (gpsAsset) =>
        {
            let gpsSources = this.locationIdToGpsSources[gpsAsset.model.location.sysId];
            let requests   = [];

            if (tooltipConfig)
            {
                for (let entry of tooltipConfig.entries)
                {
                    let relevantSources = gpsSources?.filter((gpsSource) => gpsSource.bindingId === entry.binding.nodeId) || [];
                    for (let source of relevantSources)
                    {
                        requests.push(this.newTimeSeriesRequest(source.deviceElementExt.model.sysId, entry.unitsFactors));
                    }
                }
            }
            else if (gpsSources)
            {
                for (let gpsSource of gpsSources)
                {
                    let property = await gpsSource.deviceElementExt.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
                    if (property)
                    {
                        let preferredUnits = await this.app.domain.units.findPreferred(property.unitsFactors);
                        requests.push(this.newTimeSeriesRequest(gpsSource.deviceElementExt.model.sysId, preferredUnits));
                    }
                }
            }

            return requests;
        });
    }

    private newTimeSeriesRequest(id: string,
                                 units: Models.EngineeringUnitsFactors): Models.TimeSeriesPropertyRequest
    {
        return Models.TimeSeriesPropertyRequest.newInstance(
            {
                sysId    : id,
                prop     : DeviceElementExtended.PRESENT_VALUE,
                convertTo: units
            }
        );
    }

    private buildDeadZoneRanges()
    {
        let deadRanges: { lowMs: number, highMs: number }[] = [];

        let pathOfInterest;
        if (this.paths.length === 1)
        {
            pathOfInterest = this.paths[0];
        }
        else
        {
            let viewablePaths = this.paths.filter((path) => path.state !== VisualizationDataSourceState.Disabled);
            pathOfInterest    = viewablePaths.find((path) => path.state === VisualizationDataSourceState.Target);
            if (!pathOfInterest && viewablePaths.length === 1) pathOfInterest = viewablePaths[0];
        }

        if (pathOfInterest)
        {
            let prevHigh = this.activeRangeExt.getMin()
                               .valueOf();

            let segments = pathOfInterest.segments;
            for (let i = 0; i < segments.length; i++)
            {
                let segment = segments[i];
                let low     = segment.firstTimestamp;
                if (low - prevHigh > GpsMapComponent.maxDurationBetweenSegments)
                {
                    deadRanges.push({
                                        lowMs : prevHigh + 1,
                                        highMs: low - 1
                                    });
                }

                prevHigh = segment.lastTimestamp;
            }

            let overallHigh = this.activeRangeExt
                                  .getMax()
                                  .valueOf();
            let overallMin  = this.activeRangeExt
                                  .getMin()
                                  .valueOf();
            if (overallHigh - prevHigh > GpsMapComponent.maxDurationBetweenSegments)
            {
                deadRanges.push({
                                    lowMs : prevHigh + 1,
                                    highMs: overallHigh
                                });
            }
            if (deadRanges.length > 0 && deadRanges[0].lowMs === overallMin + 1)
            {
                deadRanges[0].lowMs--;
            }
        }

        this.deadZoneRanges = deadRanges;
    }

    public ensureSourceColors()
    {
        let changed    = false;
        let sourcesExt = this.sourcesExt;
        if (sourcesExt.length > 1)
        {
            const palette: PaletteId   = <PaletteId>this.colorSettings.paletteName;
            let sourceColors: string[] = [];
            for (let sourceExt of sourcesExt)
            {
                let color = ChartColorUtilities.nextBestColor(sourceColors, palette);
                if (color !== sourceExt.color)
                {
                    sourceExt.model.color = color;
                    changed               = true;
                }
            }
        }
        else
        {
            const singleSourcePinColor = "#7aa4ff";
            let singleSource           = sourcesExt[0];
            if (singleSource && singleSource.color !== singleSourcePinColor)
            {
                singleSource.model.color = singleSourcePinColor;
                changed                  = true;
            }
        }

        if (changed)
        {
            this.updatePathColors();
            this.chartUpdated.emit(true);
        }
    }

    private updatePathColors()
    {
        if (!this.paths?.length) return;

        if ((this.gpsAssets?.length || 0) <= 1)
        {
            this.applySegmentsColors(0);
        }
        else
        {
            let targetIdx = this.paths.findIndex((path) => path.state === VisualizationDataSourceState.Target);
            if (targetIdx >= 0)
            {
                for (let i = 0; i < this.paths.length; i++)
                {
                    let color = i === targetIdx ? undefined : GpsMapComponent.notTargetColor;
                    this.applySegmentsColors(i, color);
                }
            }
            else
            {
                for (let i = 0; i < this.paths.length; i++)
                {
                    let assetId   = this.gpsAssets[i].model.sysId;
                    let sourceExt = this.sourcesExt.find((sourceExt) => sourceExt.mapSource.model.sysId === assetId);
                    this.applySegmentsColors(i, sourceExt.model.color);
                }
            }
        }
    }

    private applySegmentsColors(index: number,
                                color?: string)
    {
        let path = this.paths[index];
        if (color)
        {
            for (let segment of path.segments)
            {
                segment.lineColor = color;
                segment.pinColor  = color;
                segment.redraw();
            }
        }
        else
        {
            for (let segment of path.segments)
            {
                segment.resetColor();
                segment.redraw();
            }
        }
    }

    public saveColorChanges()
    {
        this.m_configExt.model.palette = this.colorSettings.paletteName;
    }

    public revertColorChanges()
    {
        let newPalette: PaletteId = <PaletteId>this.m_configExt.model.palette || "Map Colors";
        if (newPalette !== this.colorSettings.paletteName)
        {
            this.colorSettings.paletteName = newPalette;
            this.ensureSourceColors();
        }
    }

    public updatePopupConfig(popupConfig: Models.TimeSeriesTooltipConfiguration)
    {
        if (!this.m_configExt) return;
        this.m_configExt.model.tooltip = popupConfig;
        this.popupConfigChange();
        this.chartUpdated.emit();
    }

    public updateGraph(graph: Models.TimeSeriesGraphConfiguration)
    {
        if (!this.m_configExt) return;
        this.m_configExt.model.graph = graph;
        this.popupConfigChange();
        this.chartUpdated.emit();
    }

    public popupConfigChange()
    {
        if (!this.m_configExt) return;

        this.m_popupChangeDebouncer.invoke();
    }

    private async handlePopupChange()
    {
        await this.updatePopupSetup();
        this.configExtUpdated.emit();
    }

    public getCanvasPNG(): string
    {
        return this.m_pathMap && this.m_pathMap.getCanvasPNG();
    }

    public getCanvasTitle(): string
    {
        return "location_map";
    }

    private setAllPathState(state: VisualizationDataSourceState,
                            override: boolean = false)
    {
        for (let path of this.paths)
        {
            if (override || path.state !== VisualizationDataSourceState.Disabled) path.state = state;
        }
    }

    public isReady(): boolean
    {
        return this.mapSources.length > 1;
    }

    public toggleTarget(sourceId: string,
                        fromMouseover: boolean)
    {
        if (fromMouseover) return;

        let sourceExt = this.getSourceExt(sourceId);
        let path      = this.paths.find((path) => path.locationId === sourceExt.mapSource.model.location.sysId);
        if (path)
        {
            if (path.state === VisualizationDataSourceState.Target)
            {
                this.setAllPathState(VisualizationDataSourceState.Active);
            }
            else
            {
                this.setAllPathState(VisualizationDataSourceState.Muted);
                path.state = VisualizationDataSourceState.Target;
            }

            this.updatePathColors();

            if (this.selectedPathLocation) this.updateOptions();

            this.buildDeadZoneRanges();

            this.paths = UtilsService.arrayCopy(this.paths);

            this.emitSourceStates();
            this.markForCheck();
        }
    }

    public getSources(panelIdx: number,
                      onlyVisible: boolean = true): InteractableSource[]
    {
        return this.sourcesExt.filter((sourceExt) => !onlyVisible || this.isVisibleSource(sourceExt.mapSource));
    }

    private isVisibleSource(gpsSource: AssetExtended): boolean
    {
        let locationId = gpsSource?.model.location.sysId;
        let path       = this.paths.find((path) => path.locationId === locationId);
        return path?.state !== VisualizationDataSourceState.Disabled;
    }

    public multiToggleEnabled(originSourceId: string)
    {
        let originSourceExt = this.getSourceExt(originSourceId);
        let viewableSources = this.getSources(null);

        let turnOnAllSources = false;
        if (viewableSources.length <= 1) turnOnAllSources = viewableSources.length === 0 || viewableSources[0].getChartData() === originSourceExt.getChartData();

        if (turnOnAllSources)
        {
            this.enableAllSources();
        }
        else
        {
            this.enableOnly(originSourceId);
        }

        this.markForCheck();
        this.emitSourceStates();
    }

    private enableAllSources(): void
    {
        this.setAllPathState(VisualizationDataSourceState.Active, true);

        this.buildDeadZoneRanges();

        this.paths = UtilsService.arrayCopy(this.paths);
    }

    private enableOnly(sourceId: string): void
    {
        let sourceExt    = this.getSourceExt(sourceId);
        let onLocationId = sourceExt.mapSource.model.location.sysId;
        let onPath       = this.paths.find((path) => path.locationId === onLocationId);
        if (onPath)
        {
            this.setAllPathState(VisualizationDataSourceState.Disabled);
            onPath.state = VisualizationDataSourceState.Active;

            this.buildDeadZoneRanges();

            this.paths = UtilsService.arrayCopy(this.paths);
        }
    }

    public toggleEnabled(sourceId: string): void
    {
        let sourceExt  = this.getSourceExt(sourceId);
        let locationId = sourceExt.mapSource.model.location.sysId;
        let path       = this.paths.find((path) => path.locationId === locationId);
        if (path)
        {
            switch (path.state)
            {
                case VisualizationDataSourceState.Target:
                    this.setAllPathState(VisualizationDataSourceState.Active);
                    this.updatePathColors();
                // fall through
                case VisualizationDataSourceState.Active:
                case VisualizationDataSourceState.Muted:
                    path.state = VisualizationDataSourceState.Disabled;
                    break;

                case VisualizationDataSourceState.Disabled:
                    let hasTarget = this.paths.some((path) => path.state === VisualizationDataSourceState.Target);
                    path.state    = hasTarget ? VisualizationDataSourceState.Muted : VisualizationDataSourceState.Active;
                    break;
            }

            this.buildDeadZoneRanges();

            this.paths = UtilsService.arrayCopy(this.paths);
            this.markForCheck();
            this.emitSourceStates();
        }
    }

    private emitSourceStates()
    {
        this.sourceStatesUpdated.emit(this.m_configExt.getSourcesStates(this));
    }

    public getSourceState(sourceId: string): VisualizationDataSourceState
    {
        let sourceExt        = this.getSourceExt(sourceId);
        let sourceLocationId = sourceExt?.mapSource?.model.location.sysId;
        let path             = this.paths.find((path) => path.locationId === sourceLocationId);
        return path?.state;
    }

    public getSource(sourceId: string): InteractableSource
    {
        return this.getSourceExt(sourceId);
    }

    public isDeletable(sourceId: string): boolean
    {
        return false;
    }

    private getSourceExt(sourceId: string): TimeSeriesSourceConfigurationExtended
    {
        return this.sourcesExt.find((sourceExt) => sourceId === sourceExt.identifier);
    }

    public configureSource(sourceId: string)
    {
        return;
    }
}

class GpsMapSource
{
    constructor(readonly bindingId: string,
                readonly deviceElementExt: DeviceElementExtended)
    {}
}
