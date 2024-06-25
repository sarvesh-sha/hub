import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import {FenceLayerExtended, LineLayerExtended, LineLayerFeature, LongitudeLatitudeBounds, MapExtended, PinLayerExtended, PinLayerFeature} from "app/services/domain/azure-maps.service";

import * as Models from "app/services/proxy/model/models";
import {MapComponentBase} from "app/shared/mapping/map-component-base.component";
import * as atlas from "azure-maps-control";
import {MapMouseEvent} from "azure-maps-control";
import {UtilsService} from "framework/services/utils.service";
import {ChartHelpers} from "framework/ui/charting/app-charting-utilities";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";
import {VisualizationDataSourceState} from "framework/ui/charting/core/data-sources";
import {ChartTimeRange} from "framework/ui/charting/core/time";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {Subject} from "rxjs";
import {throttleTime} from "rxjs/operators";

@Component({
               selector       : "o3-path-map",
               templateUrl    : "../map-component-base.component.html",
               styleUrls      : ["../map-component-base.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PathMapComponent extends MapComponentBase
{
    private static readonly pinRadius: number = 5.5;

    private static readonly leftOffsetPx: number = 120;

    private static readonly maxDurationPopupLogicMs: number = 1000;
    private static readonly maxPxPerSecond: number          = 50;
    private static readonly requiredHoverDurationMs: number = 250;

    public ready: boolean = false;

    private m_zoom: number               = 15;
    private m_lines: LineLayerExtended[] = [];
    private m_pin: PinLayerExtended;

    private segmentsUpdateThrottler = new Subject<void>();

    get loaded(): boolean
    {
        return !this.m_pathPointsByLongitude;
    }

    private m_range: ChartTimeRange;
    @Input() set range(range: ChartTimeRange)
    {
        if (this.m_range !== range)
        {
            this.m_range = range;
            this.updatePaths();
        }
    }

    get range(): ChartTimeRange
    {
        return this.m_range;
    }

    private m_selectedPathPoint: GpsPathPoint;
    @Input() set selectedPathPoint(location: GpsPathPoint)
    {
        this.m_selectedPathPoint = location;
        if (this.popup && this.m_selectedPathPoint) this.popup.setOptions({position: this.popupLocation});
    }

    private get popupLocation(): atlas.data.Position
    {
        if (!this.m_selectedPathPoint || !this.m_popupLocked) return this.m_cursorSource;

        return new atlas.data.Position(this.m_selectedPathPoint.latLong.longitude, this.m_selectedPathPoint.latLong.latitude);
    }

    private m_pathPointsByLongitude: GpsPathPoint[];
    private m_targetPath: GpsPath;
    private m_paths: GpsPath[] = [];

    @Input() set paths(paths: GpsPath[])
    {
        if (!!paths?.length && paths !== this.m_paths)
        {
            let noNewPaths = UtilsService.compareArraysAsSets(this.m_paths, paths);
            this.m_paths   = paths;

            if (noNewPaths)
            {
                this.updateTargetPath();
                this.updatePaths();
            }
            else
            {
                let pathSegments = paths.map((path) => path?.segments || []);
                if (pathSegments.some((path) => path.length > 0)) this.processPathSegments(pathSegments);

                this.queueSegmentsRebuild();
            }
        }
    }

    private overallBounds: LongitudeLatitudeBounds;

    private m_cursorSource: atlas.data.Position;
    private m_nearbyLocations: GpsPathPoint[];

    @Output() nearbyLocationsUpdated = new EventEmitter<GpsPathPoint[]>();

    private mouseMoveLog: { ms: number, position: atlas.Pixel, distFromPrevious: number }[] = [];
    private hoverTimer: number;

    private m_popupLocked: boolean;

    private subsRegistered: boolean = false;

    @Output() popupLocked = new EventEmitter<boolean>();

    constructor(inj: Injector)
    {
        super(inj);

        this.showZoomReset = true;

        this.subscribeToObservable(this.segmentsUpdateThrottler.pipe(throttleTime(100, undefined, {
            leading : true,
            trailing: true
        })), () => this.rebuildPaths());
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        if (this.hoverTimer)
        {
            clearTimeout(this.hoverTimer);
            this.hoverTimer = null;
        }
    }

    protected async initializeMap()
    {
        await super.initializeMap();

        this.subsRegistered = false;

        this.m_pin = new PinLayerExtended(this.m_mapAzure, false, PathMapComponent.pinRadius);

        this.queueSegmentsRebuild();

        let fence = new FenceLayerExtended(this.m_mapAzure);
        this.displayAllGeoFences(fence); // Don't wait for fences
    }

    protected updatePopup()
    {
        if (!this.m_popupContent) return;

        if (this.popup) this.clearPopups();

        this.popup = this.m_mapAzure.showCustomPopup({
                                                         closeButton: false,
                                                         content    : this.m_popupContent,
                                                         position   : this.m_cursorSource,
                                                         pixelOffset: [
                                                             PathMapComponent.leftOffsetPx,
                                                             0
                                                         ],
                                                         showPointer: false,
                                                         fillColor  : this.m_popupBackgroundColor
                                                     });

        this.registerMapSub(this.m_mapAzure.onPopupEvent("close", this.popup, (e) => this.clearPopups()));
    }

    private processMouseMove(position: MapMouseEvent)
    {
        let currMs = MomentHelper.now()
                                 .valueOf();
        let prev   = this.mouseMoveLog[this.mouseMoveLog.length - 1];
        this.mouseMoveLog.push({
                                   ms              : currMs,
                                   position        : position.pixel,
                                   distFromPrevious: prev ? atlas.Pixel.getDistance(prev.position, position.pixel) : 0
                               });
        let oldestMs = currMs - PathMapComponent.maxDurationPopupLogicMs;
        let staleIdx = this.mouseMoveLog.findIndex((mousemoveItem) => mousemoveItem.ms <= oldestMs);
        if (staleIdx >= 0) this.mouseMoveLog = this.mouseMoveLog.slice(staleIdx + 1);

        let durationToConsider = currMs - this.mouseMoveLog[0].ms;
        let distanceTravelled  = 0;
        for (let i = 1; i < this.mouseMoveLog.length; i++) distanceTravelled += this.mouseMoveLog[i].distFromPrevious;

        if (this.hoverTimer) clearTimeout(this.hoverTimer);
        this.hoverTimer = setTimeout(
            () =>
            {
                this.adjustLocations(position.position);
                this.hoverTimer = null;
            }, PathMapComponent.requiredHoverDurationMs);

        let approximatePxPerSecond = durationToConsider === 0 ? PathMapComponent.maxDurationPopupLogicMs : 1000 * distanceTravelled / durationToConsider;
        this.adjustLocations(position.position, approximatePxPerSecond);
    }

    private adjustLocations(position: atlas.data.Position,
                            pxPerSecond: number = 0)
    {
        if (!this.initialized || this.m_popupLocked) return;
        if (pxPerSecond > PathMapComponent.maxPxPerSecond)
        {
            this.popupOpen = false;
            return;
        }

        this.m_cursorSource = position;
        if (this.popup)
        {
            this.popup.setOptions({position: this.m_cursorSource});
        }
        else
        {
            this.updatePopup();
        }

        this.nearbyLocationsUpdated.emit(this.m_nearbyLocations = this.getLocations(position));
    }

    private lockUnlockPopup(event: MapMouseEvent)
    {
        if (this.popup && this.m_selectedPathPoint)
        {
            this.m_popupLocked = !this.m_popupLocked;
            let xOffset        = this.m_popupLocked ? 0 : PathMapComponent.leftOffsetPx;
            this.popup.setOptions({
                                      position   : this.popupLocation,
                                      pixelOffset: [
                                          xOffset,
                                          0
                                      ],
                                      showPointer: this.m_popupLocked
                                  });
        }

        if (!this.m_popupLocked) this.adjustLocations(event.position);

        this.popupLocked.emit(this.m_popupLocked);
    }

    private clearPopups()
    {
        this.m_mapAzure.clearPopups();
        this.popup = this.m_popupLocked = null;
    }

    private getLocations(position: atlas.data.Position): GpsPathPoint[]
    {
        const allowablePxDiff = LineLayerExtended.strokeWidth * 2;
        let maxDiffLongitude  = allowablePxDiff / this.m_mapAzure.distanceInPixels(position, new atlas.data.Position(position[0] + 1, position[1]));
        let maxDistMeters     = MapExtended.measureDistance(position, new atlas.data.Position(position[0] + maxDiffLongitude, position[1]));

        let lowLongitude = UtilsService.binarySearch<GpsPathPoint>(this.m_pathPointsByLongitude, position[0] - maxDiffLongitude, (pathPoint) => pathPoint.latLong.longitude);
        if (lowLongitude < 0) lowLongitude = Math.max(~lowLongitude - 1, 0);

        let highLongitude = UtilsService.binarySearch<GpsPathPoint>(this.m_pathPointsByLongitude, position[0] + maxDiffLongitude, (pathPoint) => pathPoint.latLong.longitude);
        if (highLongitude < 0) highLongitude = Math.min(~highLongitude + 1, this.m_pathPointsByLongitude.length - 1);

        let closePoints: { dist: number, point: GpsPathPoint }[] = [];
        for (let i = lowLongitude; i <= highLongitude; i++)
        {
            let pathPoint  = this.m_pathPointsByLongitude[i];
            let distMeters = MapExtended.measureDistance(position, pathPoint.latLong);
            if (distMeters < maxDistMeters)
            {
                let currBestDist = closePoints[pathPoint.pathSegmentIdx]?.dist;
                if (currBestDist === undefined || currBestDist > distMeters)
                {
                    closePoints[pathPoint.pathSegmentIdx] = {
                        dist : distMeters,
                        point: pathPoint
                    };
                }
            }
        }

        return closePoints.map((distPoint) => distPoint.point);
    }

    public getCanvasPNG(): string
    {
        return ChartHelpers.getCanvasPNG(this.getCanvas());
    }

    private processPathSegments(pathSegments: GpsPathSegment[][])
    {
        let segmentSortFn = (a: GpsPathPoint,
                             b: GpsPathPoint) => UtilsService.compareNumbers(a.latLong?.longitude, b.latLong?.longitude, true);

        let sortedPaths = pathSegments.map(
                                          (path) => path.map(
                                              (segment) =>
                                                  segment.timePath
                                                         .map((pathPoint) => pathPoint.clone())
                                                         .sort(segmentSortFn)))
                                      .map((path) => UtilsService.mergeSortedArrays(path, segmentSortFn));

        this.m_pathPointsByLongitude = UtilsService.mergeSortedArrays(sortedPaths, segmentSortFn);
    }

    private queueSegmentsRebuild()
    {
        this.ready = false;

        this.segmentsUpdateThrottler.next();
    }

    private rebuildPaths()
    {
        if (!this.initialized) return;

        for (let line of this.m_lines) line.clear();
        this.m_pin.clear();

        let someLayersAdded = false;
        let bounds          = new LongitudeLatitudeBounds();
        for (let i = 0; i < this.m_paths.length; i++)
        {
            let path      = this.m_paths[i];
            let lineLayer = this.m_lines[i];
            if (!lineLayer)
            {
                someLayersAdded = true;
                lineLayer       = this.m_lines[i] = new LineLayerExtended(this.m_mapAzure);
            }

            for (let segment of path.segments)
            {
                segment.addLine(lineLayer, false);
                bounds.merge(segment.bounds);
            }

            path.lineLayerExt = lineLayer;
        }

        if (someLayersAdded) this.m_mapAzure.moveLayer(this.m_pin.layer);

        if (!this.subsRegistered && this.m_pathPointsByLongitude)
        {
            this.registerMapSub(this.m_mapAzure.onEvent("mousemove", (e) => this.processMouseMove(e)));
            this.registerMapSub(this.m_mapAzure.onEvent("click", (e) => this.lockUnlockPopup(e)));
            this.subsRegistered = true;
        }

        this.ready = this.m_paths.length > 0;

        this.updateTargetPath();
        this.updatePaths();

        this.resetCameraView(bounds);
    }

    private updatePaths()
    {
        if (!this.ready) return;

        for (let gpsPath of this.m_paths)
        {
            if (!isNaN(gpsPath.segmentWithPinIdx)) gpsPath.segments[gpsPath.segmentWithPinIdx].hidePin();
        }

        this.overallBounds = new LongitudeLatitudeBounds();
        for (let gpsPath of this.m_paths)
        {
            gpsPath.update(this.m_pin, this.m_range);

            this.overallBounds.merge(gpsPath.bounds);
        }

        for (let gpsPath of this.m_paths)
        {
            if (this.m_targetPath && this.m_targetPath !== gpsPath) continue;
            this.updatePin(gpsPath);
        }
        if (this.m_targetPath) this.updatePin(this.m_targetPath);
    }

    private updatePin(gpsPath: GpsPath)
    {
        if (gpsPath.state === VisualizationDataSourceState.Disabled) return;

        if (!isNaN(gpsPath.segmentWithPinIdx)) gpsPath.segments[gpsPath.segmentWithPinIdx].addPin(this.m_pin);
    }

    private updateTargetPath()
    {
        if (!this.initialized) return;

        let targetPath = this.m_paths.find((path) => path.state === VisualizationDataSourceState.Target);
        if (this.m_targetPath !== targetPath)
        {
            let pinLayer = this.m_pin.layer;
            if (targetPath)
            {
                this.m_mapAzure.moveLayer(targetPath.lineLayerExt.layer, pinLayer);
                this.updatePin(targetPath);
            }
            else if (this.m_targetPath)
            {
                let prevTargetLocation = this.m_targetPath.locationId;
                let previousTargetIdx  = this.m_paths.findIndex((gpsPath) => gpsPath.locationId === prevTargetLocation);
                for (let i = previousTargetIdx + 1; i < this.m_paths.length; i++)
                {
                    let gpsPath = this.m_paths[i];

                    this.m_mapAzure.moveLayer(gpsPath.lineLayerExt.layer, pinLayer);
                    this.updatePin(gpsPath);
                }
            }

            this.m_targetPath = targetPath;
        }
    }

    public updateZoom(): void
    {
        this.resetCameraView(this.m_targetPath?.bounds);
    }

    private resetCameraView(bounds: LongitudeLatitudeBounds = this.overallBounds)
    {
        if (!bounds.isEmpty)
        {
            this.m_mapAzure.setView(bounds,
                                    {
                                        top   : 50,
                                        left  : 15,
                                        right : 15,
                                        bottom: 15
                                    });

            // unless target gps asset, limit zoom to something meaningful
            if (bounds === this.overallBounds) this.m_mapAzure.limitZoom(this.m_zoom);
        }
    }
}

export class GpsPath
{
    public segmentWithPinIdx: number;
    public bounds: LongitudeLatitudeBounds;
    public lineLayerExt: LineLayerExtended;

    private hasLine: boolean;

    public state: VisualizationDataSourceState = VisualizationDataSourceState.Active;

    constructor(public segments: GpsPathSegment[],
                public readonly locationId: string)
    {
    }

    update(pin: PinLayerExtended,
           range: ChartTimeRange)
    {
        this.bounds = new LongitudeLatitudeBounds();

        if (this.state === VisualizationDataSourceState.Disabled)
        {
            this.hideLine();
        }
        else
        {
            let noPin  = true;
            let pinIdx = undefined;
            for (let i = 0; i < this.segments.length; i++)
            {
                let segment = this.segments[i];
                let withPin = segment.updateSegment(this.lineLayerExt, pin, range);
                if (withPin)
                {
                    noPin  = false;
                    pinIdx = i;
                }
                else if (noPin && withPin === undefined)
                {
                    pinIdx = i;
                }

                if (segment.hasLine)
                {
                    this.bounds.merge(segment.bounds);
                    this.hasLine = true;
                }
            }

            this.segmentWithPinIdx = pinIdx;
        }
    }

    hideLine()
    {
        if (this.hasLine)
        {
            for (let segment of this.segments)
            {
                if (segment.hasLine) segment.lineOpacity = 0;
            }

            this.hasLine = false;
        }
    }
}

export class GpsPathSegment
{
    get locationId(): string
    {
        return this.timePath[0]?.locationId;
    }

    readonly bounds = new LongitudeLatitudeBounds();

    readonly path: Models.LongitudeLatitude[];

    readonly firstTimestamp: number;
    readonly lastTimestamp: number;

    private lineIsPartial: boolean = false;
    private lineIsVisible: boolean = false;
    private line: LineLayerFeature;
    private pin: PinLayerFeature;

    get hasLine(): boolean
    {
        return this.lineIsVisible;
    }

    private lowIdx: number;
    private highIdx: number;

    private readonly defaultPinColor: string = ChartColorUtilities.getColorById("Map Colors", "mapblue").hex;
    private m_pinColor: string;
    private m_lineColor: string;

    set pinColor(color: string)
    {
        this.m_pinColor = color;
    }

    set lineColor(color: string)
    {
        this.m_lineColor = color;
    }

    set lineOpacity(opacity: number)
    {
        this.lineIsVisible = opacity > 0;
        this.line.setOpacity(opacity);
    }

    //--//

    constructor(public readonly timePath: GpsPathPoint[],
                private readonly index: number,
                color?: string)
    {
        if (color)
        {
            this.lineColor = color;
        }
        else
        {
            this.resetColor();
        }

        if (this.timePath)
        {
            this.path = this.timePath.map((point) => point.latLong);

            let numLocations = this.timePath.length;
            if (numLocations > 0)
            {
                this.firstTimestamp = this.timePath[0].timestamp;
                this.lastTimestamp  = this.timePath[numLocations - 1].timestamp;
            }
        }

        this.bounds.addAll(this.path);
    }

    public resetColor()
    {
        this.m_pinColor  = this.defaultPinColor;
        let colorKey     = Object.keys(ChartColorUtilities.getPalette("Map Colors"))[this.index % 4];
        this.m_lineColor = ChartColorUtilities.getColorById("Map Colors", colorKey).hex;
    }

    public hidePin()
    {
        this.pin?.setOpacity(0);
    }

    /**
     *
     * returns true if this segment has the pin; false if no; undefined if it should be placed at end or in subsequent segments
     *
     * @param lineExt
     * @param pinExt
     * @param range
     */
    public updateSegment(lineExt: LineLayerExtended,
                         pinExt: PinLayerExtended,
                         range: ChartTimeRange): boolean
    {
        let lowIdx  = 0;
        let highIdx = this.timePath.length - 1;
        if (range)
        {
            lowIdx = UtilsService.binarySearch(this.timePath, range.minInMillisec, (point) => point.timestamp);
            if (lowIdx < 0)
            {
                lowIdx = ~lowIdx;
                if (lowIdx === this.timePath.length)
                {
                    this.lineOpacity = 0;
                    return false;
                }
                else if (this.timePath[lowIdx].timestamp < range.minInMillisec)
                {
                    lowIdx++;
                }
            }

            highIdx = UtilsService.binarySearch(this.timePath, range.maxInMillisec, (point) => point.timestamp);
            if (highIdx < 0)
            {
                highIdx = ~highIdx;
                if (highIdx === this.timePath.length || this.timePath[highIdx].timestamp > range.maxInMillisec) highIdx--;
            }
        }

        if (lowIdx > highIdx)
        {
            this.lineOpacity = 0;
            return false;
        }
        else
        {
            let willBePartial = lowIdx !== 0 || highIdx !== this.path.length - 1;
            if ((this.lineIsPartial || willBePartial) && (this.lowIdx !== lowIdx || this.highIdx !== highIdx))
            {
                this.deconstructLine();
                this.addLine(lineExt, true, lowIdx, highIdx);
            }
            else
            {
                this.lineOpacity = 1;
            }

            return this.highIdx < this.timePath.length - 1 ? true : undefined;
        }
    }

    private deconstructPin()
    {
        if (this.pin)
        {
            this.pin.remove();
            this.pin = undefined;
        }
    }

    private deconstructLine()
    {
        this.line.remove();
        this.line          = undefined;
        this.lineIsPartial = undefined;
    }

    public redraw()
    {
        if (this.line) this.line.setColor(this.m_lineColor);
        if (this.pin) this.pin.setColor(this.m_pinColor);
    }

    public addLine(lineLayer: LineLayerExtended,
                   inView: boolean,
                   lowIdx: number  = 0,
                   highIdx: number = this.path.length - 1)
    {
        this.lowIdx        = lowIdx;
        this.highIdx       = highIdx;
        this.lineIsPartial = lowIdx !== 0 || highIdx !== this.path.length - 1;
        this.line          = lineLayer.addLine(this.lineIsPartial ? this.path.slice(lowIdx, highIdx + 1) : this.path, this.m_lineColor, inView);
        this.lineIsVisible = inView && lowIdx <= highIdx;
    }

    public addPin(pinLayer: PinLayerExtended)
    {
        if (this.pin) this.deconstructPin();
        this.pin = pinLayer.addPoint(this.path[this.highIdx], this.m_pinColor);
    }
}

export class GpsPathPoint
{
    readonly relatedValues: Map<string, string>;

    constructor(readonly locationId: string,
                readonly latLong: Models.LongitudeLatitude,
                readonly timestamp: number,
                tooltipEntryIds: string[],
                tooltipEntryValues: string[],
                readonly pathSegmentIdx: number)
    {
        if (tooltipEntryIds.length !== tooltipEntryValues.length) tooltipEntryIds = tooltipEntryValues = [];

        this.relatedValues = new Map();
        for (let i = 0; i < tooltipEntryValues.length; i++)
        {
            if (tooltipEntryValues[i]) this.relatedValues.set(tooltipEntryIds[i], tooltipEntryValues[i]);
        }
    }

    clone(): GpsPathPoint
    {
        return new GpsPathPoint(this.locationId, this.latLong, this.timestamp, Array.from(this.relatedValues.keys()), Array.from(this.relatedValues.values()), this.pathSegmentIdx);
    }
}
