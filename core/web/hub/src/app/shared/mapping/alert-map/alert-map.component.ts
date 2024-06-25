import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";
import {LocationExtended} from "app/services/domain/assets.service";

import {AzureMapsService, FenceLayerExtended, HtmlMarker, HtmlMarkerLayer, LongitudeLatitudeBounds, MapExtended} from "app/services/domain/azure-maps.service";
import {AlertLocation} from "app/services/domain/widget-data.service";
import * as Models from "app/services/proxy/model/models";
import {MapPinIcon} from "app/services/proxy/model/models";
import {AppMappingUtilities} from "app/shared/mapping/app-mapping.utilities";
import {MapComponentBase} from "app/shared/mapping/map-component-base.component";
import * as atlas from "azure-maps-control";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";

const PIN_GROUP_FACTOR = 1.5;
const PIN_SIZE         = 36;

@Component({
               selector       : "o3-alert-map[pinConfig]",
               templateUrl    : "../map-component-base.component.html",
               styleUrls      : ["../map-component-base.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class AlertMapComponent extends MapComponentBase
{
    private static readonly redPinDefaultThreshold: number    = 10;
    private static readonly orangePinDefaultThreshold: number = 7;
    private static readonly yellowPinDefaultThreshold: number = 0;

    private static readonly maxAutoZoom: number = 18;

    @Input() pinConfig: Models.AlertMapPinConfig;
    @Input() alertTypes: Models.AlertType[];
    @Input() locationIds: string[];
    @Input() rollupType: Models.LocationType;

    private markerLayer: atlas.layer.Layer;
    private pinMetadata: AlertPinMetadata[] = [];

    private icon: MapPinIcon = MapPinIcon.Pin;
    private baseSize: number;
    private groupSize: number;

    private m_inView: boolean = false;

    @Output() alertFocused = new EventEmitter<AlertPinMetadata>();

    public async ngAfterViewInit()
    {
        await super.ngAfterViewInit();

        this.showZoomReset = true;

        this.baseSize  = this.pinConfig?.pinSize || PIN_SIZE;
        this.groupSize = this.baseSize * PIN_GROUP_FACTOR;

        if (this.pinConfig?.pinIcon) this.icon = this.pinConfig.pinIcon;

        await this.refreshPins();
    }

    public refreshSize(): boolean
    {
        let success = super.refreshSize();
        if (!this.m_inView) this.updateZoom();
        return success;
    }

    public async refreshPins()
    {
        let prevCt       = this.pinMetadata.length;
        this.pinMetadata = await this.fetchPins();

        this.buildMap();

        if (prevCt !== this.pinMetadata.length) this.zoomToPins(this.pinMetadata);
    }

    private buildMap()
    {
        let source      = this.m_mapAzure.newDataSource();
        let markerLayer = new HtmlMarkerLayer<AlertPinMetadata>(source, null, {
            clusterSizeCallback  : function (): number
            {
                return 50;
            },
            markerRenderCallback : (point): HtmlMarker =>
            {
                let meta = point.properties;

                let isStatic   = false;
                let isSeverity = false;
                let markerText = meta.count ? meta.count.toString() : "";

                if (this.pinConfig?.colorMode === Models.AlertMapPinColorMode.Fixed)
                {
                    isStatic   = true;
                    markerText = "";
                }

                if (this.pinConfig?.colorMode === Models.AlertMapPinColorMode.Dynamic && this.pinConfig.dataSource === Models.AlertMapPinDataSource.Severity)
                {
                    isSeverity = true;
                }


                let color = this.pinConfig?.staticColor;
                if (!isStatic) color = isSeverity ? this.getSeverityColor(meta.severity) : this.getColor(meta.count);
                let marker = new atlas.HtmlMarker({
                                                      position   : point.position,
                                                      htmlContent: AppMappingUtilities.makeIcon(markerText, this.baseSize, color, this.icon),
                                                      color      : color
                                                  });

                this.m_mapAzure.addHtmlMarkerCursorChanger(marker, "pointer");
                this.m_mapAzure.onHtmlMarkerEvent("click", marker, (e) =>
                {
                    this.dismissPopup();

                    this.popup = this.disablePopup ? null : this.m_mapAzure.showPopup(this.m_popupContent, point.position);

                    this.alertFocused.emit(meta);
                });

                return <HtmlMarker>marker;
            },
            clusterRenderCallback: (position,
                                    children): HtmlMarker =>
            {
                let count = 0;
                for (let child of children)
                {
                    let meta = <AlertPinMetadata>child.properties;

                    count += meta.count;
                }

                let severity = Models.AlertSeverity.LOW;
                for (let child of children)
                {
                    let meta = <AlertPinMetadata>child.properties;
                    severity = this.pickHigherSeverity(severity, meta.severity);
                }

                let isStatic   = false;
                let isSeverity = false;
                let markerText = count ? count.toString() : "";

                if (this.pinConfig?.colorMode === Models.AlertMapPinColorMode.Fixed)
                {
                    isStatic   = true;
                    markerText = children.length.toString();
                }

                if (this.pinConfig?.colorMode === Models.AlertMapPinColorMode.Dynamic && this.pinConfig.dataSource === Models.AlertMapPinDataSource.Severity)
                {
                    isSeverity = true;
                }

                let color = this.pinConfig?.staticColor;
                if (!isStatic) color = isSeverity ? this.getSeverityColor(severity) : this.getColor(count);
                let cluster = new atlas.HtmlMarker({
                                                       position   : position,
                                                       htmlContent: AppMappingUtilities.makeIcon(markerText, this.groupSize, color, this.icon),
                                                       color      : color
                                                   });

                this.m_mapAzure.addHtmlMarkerCursorChanger(cluster, "pointer");
                this.m_mapAzure.onHtmlMarkerEvent("click", cluster, (e) =>
                {
                    this.dismissPopup();

                    this.zoomToPins(children.map((child) => child.properties));
                });

                return <HtmlMarker>cluster;
            }
        });

        if (this.markerLayer)
        {
            this.m_mapAzure.removeMarkers();
            this.m_mapAzure.removeLayer(this.markerLayer);
        }
        this.markerLayer = markerLayer;
        this.m_mapAzure.addLayer(this.markerLayer);

        // Insert pins into layer
        for (let meta of this.pinMetadata)
        {
            let location = MapExtended.toPoint(meta.alert.position);
            let pin      = new atlas.data.Feature(new atlas.data.Point(location), meta);

            source.add(pin);
        }
    }

    async initializeMap()
    {
        await super.initializeMap();

        let fence = new FenceLayerExtended(this.m_mapAzure);
        this.displayAllGeoFences(fence); // Don't wait for fences
    }

    private getColor(count: number): string
    {
        let countColors = this.getCountColors();
        if (countColors)
        {
            for (let i = countColors.length - 1; i > 0; i--)
            {
                let stop = countColors[i];
                if (stop.stopPoint === Models.ColorStopPoint.MAX)
                {
                    continue;
                }
                if (count >= stop.stopPointValue)
                {
                    return stop.color;
                }
            }

            return countColors[0].color;
        }
        else
        {
            if (count > AlertMapComponent.redPinDefaultThreshold) return ChartColorUtilities.getColorById("Map Colors", "mapred").hex;
            if (count > AlertMapComponent.orangePinDefaultThreshold) return ChartColorUtilities.getColorById("Map Colors", "maporange").hex;
            if (count > AlertMapComponent.yellowPinDefaultThreshold) return ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex;
            return ChartColorUtilities.getColorById("Map Colors", "mapgreen").hex;
        }
    }

    private getCountColors(): Models.ColorSegment[]
    {
        let countColors = this.pinConfig?.countColors;
        return countColors?.length > 0 ? countColors : null;
    }

    private getSeverityColor(severity: Models.AlertSeverity): string
    {
        let severityColors = this.getPinSeverityColors();
        if (severityColors)
        {
            // Find the color for the severity
            let mapping = severityColors.find((mapping) => mapping.severity === severity);

            // Extract the color
            if (mapping) return mapping.color;

            // Return default if invalid config
            return ChartColorUtilities.getColorById("Map Colors", "mapblue").hex;
        }
        else
        {
            switch (severity)
            {
                case Models.AlertSeverity.LOW:
                    return ChartColorUtilities.getColorById("Map Colors", "mapgreen").hex;
                case Models.AlertSeverity.NORMAL:
                    return ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex;
                case Models.AlertSeverity.SIGNIFICANT:
                    return ChartColorUtilities.getColorById("Map Colors", "maporange").hex;
                case Models.AlertSeverity.CRITICAL:
                    return ChartColorUtilities.getColorById("Map Colors", "mapred").hex;
                default:
                    return ChartColorUtilities.getColorById("Map Colors", "mapblue").hex;
            }
        }
    }

    private getPinSeverityColors(): Models.AlertMapSeverityColor[]
    {
        let countColors = this.pinConfig?.severityColors;
        return countColors?.length > 0 ? countColors : null;
    }

    private pickHigherSeverity(first: Models.AlertSeverity,
                               second: Models.AlertSeverity): Models.AlertSeverity
    {
        if (this.severityToValue(first) > this.severityToValue(second)) return first;
        return second;
    }

    private severityToValue(severity: Models.AlertSeverity): number
    {
        switch (severity)
        {
            case Models.AlertSeverity.CRITICAL:
                return 3;
            case Models.AlertSeverity.SIGNIFICANT:
                return 2;
            case Models.AlertSeverity.NORMAL:
                return 1;
            default:
                return 0;
        }
    }

    private dismissPopup()
    {
        this.popupOpen = false;
        this.m_mapAzure.clearPopups();
    }

    private zoomToPins(pins: AlertPinMetadata[])
    {
        let bounds = this.getBounds(pins);
        if (!bounds) return;

        this.m_inView = this.m_mapAzure.setView(bounds, this.groupSize);

        // If the bounding box zooms in past the max zoom, zoom back out to max zoom
        this.m_mapAzure.limitZoom(AlertMapComponent.maxAutoZoom);
    }

    private getBounds(pins: AlertPinMetadata[]): LongitudeLatitudeBounds
    {
        let bounds = new LongitudeLatitudeBounds();
        bounds.addAll(pins.map((pin) => pin.alert.position));

        return bounds.isEmpty ? null : bounds;
    }

    private async fetchPins(): Promise<AlertPinMetadata[]>
    {
        let alertLocationSets = await Promise.all([
                                                      this.fetchAlertsWithSeverity([Models.AlertSeverity.LOW]),
                                                      this.fetchAlertsWithSeverity([Models.AlertSeverity.NORMAL]),
                                                      this.fetchAlertsWithSeverity([Models.AlertSeverity.SIGNIFICANT]),
                                                      this.fetchAlertsWithSeverity([Models.AlertSeverity.CRITICAL])
                                                  ]);
        let alertLocationMaps = alertLocationSets.map(this.mapAlertLocations);
        let locations         = this.allKeys(alertLocationMaps);

        return await Promise.all(locations.map(async (id) =>
                                               {
                                                   let pin = new AlertPinMetadata(alertLocationMaps[0][id],
                                                                                  alertLocationMaps[1][id],
                                                                                  alertLocationMaps[2][id],
                                                                                  alertLocationMaps[3][id]);

                                                   pin.alertUrl    = await this.getAlertUrl(pin.alert);
                                                   pin.locationUrl = this.getLocationUrl(pin.alert);

                                                   return pin;
                                               }));
    }

    private async fetchAlertsWithSeverity(severities: Models.AlertSeverity[]): Promise<AlertLocation[]>
    {
        // Fetch alert locations
        let azureMaps      = this.inject(AzureMapsService);
        let alertLocations = await this.app.domain.widgetData.getAlertLocations(async (loc) =>
                                                                                {
                                                                                    let geocode = await azureMaps.bestMatch(loc.address);
                                                                                    return geocode?.location;
                                                                                }, this.locationIds, this.alertTypes, severities, this.rollupType);

        // Discard locations without position data
        alertLocations = alertLocations.filter((alert) => alert.position !== undefined);

        // Return the final set of alert locations
        return alertLocations;
    }

    private mapAlertLocations(alertLocations: AlertLocation[]): Lookup<AlertLocation>
    {
        return UtilsService.extractLookup(alertLocations);
    }

    private allKeys(alertLocations: Lookup<AlertLocation>[]): string[]
    {
        let keys = new Set<string>();
        for (let map of alertLocations)
        {
            let currKeys = UtilsService.extractKeysFromMap(map);
            for (let key of currKeys) keys.add(key);
        }

        return Array.from(keys);
    }

    private async getAlertUrl(alert: AlertLocation): Promise<string>
    {
        // Collect all sub-locations
        let top = await this.app.domain.locations.getExtended([LocationExtended.newIdentity(alert.id)]);
        let all = await top[0].getInnerDeep();

        // Collect all sub location ids
        let allIds = all.map((model) => { return model.typedModel.sysId; });

        // determine the click through url
        let params = [];
        params.push({
                        param: "locationID",
                        value: allIds.join(",")
                    });
        if (this.alertTypes && this.alertTypes.length)
        {
            params.push({
                            param: "alertTypeID",
                            value: this.alertTypes.join(",")
                        });
        }

        return "#" + this.app.ui.navigation.formatUrl("/alerts/summary", [], params);
    }

    private getLocationUrl(alert: AlertLocation): string
    {
        // determine the click through url
        let params = [];
        params.push({
                        param: "locationID",
                        value: alert.id
                    });

        return "#" + this.app.ui.navigation.formatUrl("/devices/summary", [], params);
    }

    protected updatePopup(): void
    {
    }

    public updateZoom(): void
    {
        this.zoomToPins(this.pinMetadata);
    }
}

export class AlertPinMetadata
{
    public alertUrl: string;
    public locationUrl: string;

    constructor(public low: AlertLocation,
                public normal: AlertLocation,
                public significant: AlertLocation,
                public critical: AlertLocation)

    {}

    get alert(): AlertLocation
    {
        return this.critical || this.significant || this.normal || this.low || null;
    }

    get count(): number
    {
        let total = 0;
        if (this.critical) total += this.critical.count;
        if (this.significant) total += this.significant.count;
        if (this.normal) total += this.normal.count;
        if (this.low) total += this.low.count;

        return total;
    }

    get severity(): Models.AlertSeverity
    {
        if (this.critical && this.critical.count > 0) return Models.AlertSeverity.CRITICAL;
        if (this.significant && this.significant.count > 0) return Models.AlertSeverity.SIGNIFICANT;
        if (this.normal && this.normal.count > 0) return Models.AlertSeverity.NORMAL;

        return Models.AlertSeverity.LOW;
    }
}
