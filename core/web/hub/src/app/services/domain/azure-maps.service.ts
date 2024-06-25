import {HttpClient, HttpParams} from "@angular/common/http";
import {Injectable} from "@angular/core";

import {default as turf_distance} from "@turf/distance";
import {lineString as turf_lineString, point as turf_point, polygon as turf_polygon} from "@turf/helpers";
import {default as turf_kinks} from "@turf/kinks";
import {default as turf_lineIntersect} from "@turf/line-intersect";
import {default as turf_nearestPointOnLine} from "@turf/nearest-point-on-line";
import {default as turf_pointsWithinPolygon} from "@turf/points-within-polygon";

import {AppEnvironmentConfiguration} from "app/app.service";
import * as Models from "app/services/proxy/model/models";
import {AppMappingUtilities} from "app/shared/mapping/app-mapping.utilities";

import * as atlas from "azure-maps-control";
import {Padding} from "azure-maps-control";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";

import {Future} from "framework/utils/concurrency";

@Injectable({providedIn: "root"})
export class AzureMapsService
{
    constructor(private env: AppEnvironmentConfiguration,
                private http: HttpClient)
    {
    }

    public getKey(): string
    {
        return this.env.mapsApiKey;
    }

    public async allocateMap(mapContainer: HTMLElement): Promise<MapExtended>
    {
        let loaded: Future<void> = new Future<void>();

        //Initialize a map instance.
        let map = new atlas.Map(mapContainer, {
            showLogo             : false,
            authOptions          : {
                authType       : atlas.AuthenticationType.subscriptionKey,
                subscriptionKey: this.env.mapsApiKey
            },
            disableTelemetry     : true,
            preserveDrawingBuffer: true
        });

        map.events.add("ready", () =>
        {
            //Add the zoom control to the map.
            map.controls.add(new atlas.control.ZoomControl(), {position: atlas.ControlPosition.TopRight});

            loaded.resolve(null);
        });

        await loaded;

        return new MapExtended(map);
    }

    public async searchAddress(address: string): Promise<SearchAddressResult[]>
    {
        let matches: SearchAddressResult[] = [];

        if (address)
        {
            const queryEndpoint = "https://atlas.microsoft.com/search/address/json";

            // Set up parameters
            let params = new HttpParams();
            params     = params.append("subscription-key", this.env.mapsApiKey);
            params     = params.append("api-version", "1.0");
            params     = params.append("query", address);


            // Execute search
            let response: SearchAddressResponse = <SearchAddressResponse>await this.http.get(queryEndpoint, {params})
                                                                                   .toPromise();

            // Return null in no result found or error
            if (response.results)
            {
                for (let result of response.results)
                {
                    if (result.position && result.address)
                    {
                        matches.push(result);
                    }
                }
            }
        }

        return matches;
    }

    public async searchLocation(loc: Models.LongitudeLatitude): Promise<SearchResultAddress[]>
    {
        let matches: SearchResultAddress[] = [];

        if (loc)
        {
            const queryEndpoint = "https://atlas.microsoft.com/search/address/reverse/json";

            // Set up parameters
            let params = new HttpParams();
            params     = params.append("subscription-key", this.env.mapsApiKey);
            params     = params.append("api-version", "1.0");
            params     = params.append("query", `${loc.latitude},${loc.longitude}`);

            // Execute search
            let response: SearchAddressReverseResponse = <SearchAddressReverseResponse>await this.http.get(queryEndpoint, {params})
                                                                                                 .toPromise();

            // Return null in no result found or error
            if (response.addresses)
            {
                for (let result of response.addresses)
                {
                    if (result.position && result.address)
                    {
                        matches.push(result.address);
                    }
                }
            }
        }

        return matches;
    }

    public async getTimeZoneByCoordinates(pos: Models.LongitudeLatitude): Promise<string>
    {
        if (pos)
        {
            const queryEndpoint = "https://atlas.microsoft.com/timezone/byCoordinates/json";

            // Set up parameters
            let params = new HttpParams();
            params     = params.append("subscription-key", this.env.mapsApiKey);
            params     = params.append("api-version", "1.0");
            params     = params.append("query", `${pos.latitude},${pos.longitude}`);

            // Execute search
            let response: TimezoneByCoordinatesResponse = <TimezoneByCoordinatesResponse>await this.http.get(queryEndpoint, {params})
                                                                                                   .toPromise();

            // Return null in no result found or error
            if (response.TimeZones)
            {
                for (let result of response.TimeZones)
                {
                    return result.Id;
                }
            }
        }

        return null;
    }

    public async bestMatch(address: string): Promise<GeocodeResult>
    {
        if (address == undefined) return null;

        let matches = await this.searchAddress(address);

        let result = matches[0];
        if (result)
        {
            let geocodeResult      = new GeocodeResult();
            geocodeResult.input    = address;
            geocodeResult.address  = result.address.freeformAddress;
            geocodeResult.location = Models.LongitudeLatitude.newInstance({
                                                                              longitude: result.position.lon,
                                                                              latitude : result.position.lat
                                                                          });
            return geocodeResult;
        }

        return null;
    }

    public async computeRoute(start: Models.LongitudeLatitude,
                              end: Models.LongitudeLatitude,
                              ...waypoints: Models.LongitudeLatitude[]): Promise<RouteDirectionsResult[]>
    {
        const queryEndpoint = "https://atlas.microsoft.com/route/directions/json";

        // Set up parameters
        let params = new HttpParams();
        params     = params.append("subscription-key", this.env.mapsApiKey);
        params     = params.append("api-version", "1.0");
        params     = params.append("query", AzureMapsService.toRouteQuery([
                                                                              start,
                                                                              ...waypoints,
                                                                              end
                                                                          ]));

        // Execute search
        let response: RouteDirectionsResponse = await this.http.get(queryEndpoint, {params})
                                                          .toPromise();

        // Return null in no result found or error
        if (response.routes && response.routes.length > 0)
        {
            return response.routes;
        }

        return null;
    }

    convertToPath(route: RouteDirectionsResult): Models.LongitudeLatitude[]
    {
        let points = [];

        for (let leg of route.legs)
        {
            for (let point of leg.points)
            {
                let pt = Models.LongitudeLatitude.newInstance({
                                                                  longitude: point.longitude,
                                                                  latitude : point.latitude
                                                              });

                points.push(pt);
            }
        }

        return points;
    }

    private static toRouteQuery(points: Models.LongitudeLatitude[])
    {
        return points.map(
                         pt => `${pt.latitude},${pt.longitude}`)
                     .join(":");
    }
}

function distanceInPixels(map: atlas.Map,
                          a: atlas.data.Position | Models.LongitudeLatitude,
                          b: atlas.data.Position | Models.LongitudeLatitude): number
{
    let a2 = a instanceof Models.LongitudeLatitude ? MapExtended.toPoint(a) : a;
    let b2 = b instanceof Models.LongitudeLatitude ? MapExtended.toPoint(b) : b;

    let [aPixel, bPixel] = atlas.math.mercatorPositionsToPixels([
                                                                    a2,
                                                                    b2
                                                                ], map.getCamera().zoom);

    return atlas.Pixel.getDistance(aPixel, bPixel);
}

//--//

export interface RouteDirectionsResponse
{
    routes?: RouteDirectionsResult[];
}

export interface RouteDirectionsResult
{
    legs?: RouteResultLeg[];

    // sections?: RouteResultSection[];

    summary?: RouteDirectionsSummary;
}

export interface RouteDirectionsSummary
{
    arrivalTime?: string;
    departureTime?: string;
    lengthInMeters?: number;

    trafficDelayInSeconds?: number;
    travelTimeInSeconds?: number;
}

export interface RouteResultLeg
{
    points?: RouteCoordinate[];

    summary: RouteResultLegSummary;
}

export interface RouteCoordinate
{
    latitude?: number;
    longitude?: number;
}

export interface RouteResultLegSummary
{
    departureTime?: string;
    arrivalTime?: string;

    lengthInMeters?: number;

    batteryConsumptionInkWh?: number;
    fuelConsumptionInLiters?: number;

    historicTrafficTravelTimeInSeconds?: number;
    liveTrafficIncidentsTravelTimeInSeconds?: number;
    noTrafficTravelTimeInSeconds?: number;
    trafficDelayInSeconds?: number;
    travelTimeInSeconds?: number;
}

//--//

export class GeocodeResult
{
    public input: string;
    public address: string;
    public location: Models.LongitudeLatitude;
}

export class LongitudeLatitudeBounds
{
    public box: atlas.data.BoundingBox;

    public get southwestPosition()
    {
        return this.box ? MapExtended.fromPoint(atlas.data.BoundingBox.getSouthWest(this.box)) : null;
    }

    public get northeastPosition()
    {
        return this.box ? MapExtended.fromPoint(atlas.data.BoundingBox.getNorthEast(this.box)) : null;
    }

    public get center(): Models.LongitudeLatitude
    {
        return this.box ? MapExtended.fromPoint(atlas.data.BoundingBox.getCenter(this.box)) : null;
    }

    public get isEmpty(): boolean
    {
        return !this.box;
    }

    public add(location: Models.LongitudeLatitude)
    {
        if (location)
        {
            this.addAll([location]);
        }
    }

    public addAll(locations: Models.LongitudeLatitude[])
    {
        let positions = [];

        for (let location of locations)
        {
            if (location)
            {
                positions.push(MapExtended.toPoint(location));
            }
        }

        if (positions.length == 0) return;

        let box = atlas.data.BoundingBox.fromPositions(positions);

        this.box = this.box ? atlas.data.BoundingBox.merge(this.box, box) : box;
    }

    public merge(bounds: LongitudeLatitudeBounds)
    {
        if (bounds && bounds.box)
        {
            this.box = this.box ? atlas.data.BoundingBox.merge(this.box, bounds.box) : atlas.data.BoundingBox.fromBoundingBox(bounds.box);
        }
    }
}

//--//

export interface HtmlMarkerLayerOptions<T> extends atlas.LayerOptions
{
    /**
     * A callback function that provides the size of the clusters, in pixels.
     */
    clusterSizeCallback?: () => number;

    /**
     * A callback function that customizes the rendering of a HtmlMarker. This is called each time the map is moved.
     */
    markerRenderCallback?: (point: FeatureDetails<T>) => HtmlMarker;

    /**
     * A callback function that customizes the rendering of a ClusteredHtmlMarker. This is called each time the map is moved.
     */
    clusterRenderCallback?: (position: atlas.data.Position,
                             children: Array<FeatureDetails<T>>) => HtmlMarker;
}

export interface HtmlMarker extends atlas.HtmlMarker
{
    id: string;
    properties: any;
}

export class HtmlMarkerLayer<T> extends atlas.layer.BubbleLayer
{
    private m_options = <HtmlMarkerLayerOptions<T>>{
        filter               : undefined,
        minZoom              : 0,
        maxZoom              : 24,
        visible              : true,
        markerRenderCallback : (point) =>
        {
            return new atlas.HtmlMarker({
                                            position: point.position
                                        });
        },
        clusterRenderCallback: (position,
                                children) =>
        {
            return new atlas.HtmlMarker({
                                            position: position,
                                            text    : children.length.toString()
                                        });
        }
    };

    private m_map: atlas.Map;

    private m_markers: HtmlMarker[] = [];

    private m_sourceOptions: string;
    private m_sourceShapeCount: number = 0;
    private m_datasourceCache: string  = null;

    private m_optionsChanged: boolean = false;

    private m_markerCache: Lookup<HtmlMarker> = {};

    /**
     * Constructs a new HtmlMarkerLayer.
     * @param source The id or instance of a data source which the layer will render.
     * @param id The id of the layer. If not specified a random one will be generated.
     * @param options The options of the Html marker layer.
     */
    constructor(source?: string | atlas.source.Source,
                id?: string,
                options?: HtmlMarkerLayerOptions<T>)
    {
        super(source, id, {
            color      : "transparent",
            radius     : 1,
            strokeWidth: 0
        });

        this.setOptions(options);
    }

    /**
     * Gets the options of the Html Marker layer.
     */
    public getOptions(): HtmlMarkerLayerOptions<T>
    {
        return this.m_options;
    }

    /**
     * Sets the options of the Html marker layer.
     * @param options The new options of the Html marker layer.
     */
    public setOptions(options: HtmlMarkerLayerOptions<T>)
    {
        let newBaseOptions: atlas.BubbleLayerOptions = {};

        if (options.source && this.m_options.source !== options.source)
        {
            this.m_options.source = options.source;
            newBaseOptions.source = options.source;
            this.clearCache();
        }

        if (options.sourceLayer && this.m_options.sourceLayer !== options.sourceLayer)
        {
            this.m_options.sourceLayer = options.sourceLayer;
            newBaseOptions.sourceLayer = options.sourceLayer;
            this.clearCache();
        }

        if (options.filter && this.m_options.filter !== options.filter)
        {
            this.m_options.filter = options.filter;
            newBaseOptions.filter = options.filter;
            this.clearCache();
        }

        if (typeof options.minZoom === "number" && this.m_options.minZoom !== options.minZoom)
        {
            this.m_options.minZoom = options.minZoom;
            newBaseOptions.minZoom = options.minZoom;
        }

        if (typeof options.maxZoom === "number" && this.m_options.maxZoom !== options.maxZoom)
        {
            this.m_options.maxZoom = options.maxZoom;
            newBaseOptions.maxZoom = options.maxZoom;
        }

        if (typeof options.visible !== "undefined" && this.m_options.visible !== options.visible)
        {
            this.m_options.visible = options.visible;
            newBaseOptions.visible = options.visible;
        }

        if (options.clusterSizeCallback && this.m_options.clusterSizeCallback != options.clusterSizeCallback)
        {
            this.m_options.clusterSizeCallback = options.clusterSizeCallback;
            this.clearCache();
        }

        if (options.markerRenderCallback && this.m_options.markerRenderCallback != options.markerRenderCallback)
        {
            this.m_options.markerRenderCallback = options.markerRenderCallback;
            this.clearCache();
        }

        if (options.clusterRenderCallback && this.m_options.clusterRenderCallback != options.clusterRenderCallback)
        {
            this.m_options.clusterRenderCallback = options.clusterRenderCallback;
            this.clearCache();
        }

        this.m_optionsChanged = true;
        super.setOptions(newBaseOptions);
    }


    public onAdd(map: atlas.Map): void
    {
        super.onAdd(map);

        if (map)
        {
            map.events.add("moveend", () => { this.updateMarkers(); });
            map.events.add("render", () => { this.checkLayerForChanges(); });

            this.m_map = map;
        }
    }

    public onRemove(): void
    {
        super.onRemove();

        if (this.m_map)
        {
            this.m_map.events.remove("moveend", () => { this.updateMarkers(); });
            this.m_map.events.remove("render", () => { this.checkLayerForChanges(); });
            this.m_map = null;
        }
    }

    //TODO: Update this when data source supports updated events.
    private checkLayerForChanges()
    {
        if (this.m_map)
        {
            let source = this.resolveSource();

            let opt: string       = null;
            let dataSourceChanged = false;

            if (source instanceof atlas.source.DataSource)
            {
                opt = JSON.stringify(source.getOptions());

                let count = source.getShapes().length;
                if (this.m_sourceShapeCount !== count)
                {
                    this.m_sourceShapeCount = count;
                    dataSourceChanged       = true;
                }
                else
                {
                    let d = JSON.stringify(source.toJson());

                    if (d !== this.m_datasourceCache)
                    {
                        this.m_datasourceCache = d;
                        dataSourceChanged      = true;
                    }
                }
            }
            else if (source instanceof atlas.source.VectorTileSource)
            {
                opt = JSON.stringify(source.getOptions());
            }

            //Check to see if any changes have occurred to the data source.
            if (dataSourceChanged || opt != this.m_sourceOptions)
            {
                this.m_sourceOptions = opt;
                this.clearCache();
            }

            this.updateMarkers();
        }
    }

    private clearCache()
    {
        //Give data source a moment to update.
        setTimeout(() =>
                   {
                       this.m_markerCache = {}; //Clear marker cache.
                       if (this.m_map) this.m_map.markers.remove(this.m_markers);
                       this.m_markers = [];
                       this.updateMarkers();
                   }, 100);
    }

    private async updateMarkers()
    {
        if (!this.m_map) return;

        let camera = this.m_map.getCamera();
        if (camera.zoom >= this.m_options.minZoom && camera.zoom <= this.m_options.maxZoom)
        {
            let source = this.resolveSource();
            if (source instanceof atlas.source.DataSource)
            {
                let newMarkers = [];

                let shapes = this.m_map.layers.getRenderedShapes(null, [this.getId()], this.getOptions().filter);
                let points = await this.extractFeatures(shapes);

                let clusters: FeatureCluster<T>[] = [];
                let clusterSize                   = this.m_options.clusterSizeCallback ? this.m_options.clusterSizeCallback() : 30;

                for (let point of points)
                {
                    let cluster = this.findCluster(clusters, point, clusterSize);
                    if (!cluster)
                    {
                        cluster = new FeatureCluster<T>();
                        clusters.push(cluster);
                    }

                    cluster.add(point);
                }

                for (let cluster of clusters)
                {
                    let marker = this.m_markerCache[cluster.id];
                    if (marker == null)
                    {
                        let points = cluster.points;
                        if (points.length > 1)
                        {
                            if (this.m_options.clusterRenderCallback)
                            {
                                marker = this.m_options.clusterRenderCallback(cluster.position, points);
                            }
                        }
                        else
                        {
                            if (this.m_options.markerRenderCallback)
                            {
                                marker = this.m_options.markerRenderCallback(points[0]);
                            }
                        }
                    }

                    if (marker)
                    {
                        marker.properties = points.length > 1 ? cluster : points[0];
                        marker.id         = cluster.id;

                        //Make sure position is set.
                        marker.setOptions({
                                              position: cluster.position
                                          });

                        this.m_markerCache[cluster.id] = marker;
                        newMarkers.push(marker);
                    }
                }

                for (let marker of this.m_markers)
                {
                    if (newMarkers.indexOf(marker) === -1)
                    {
                        this.m_map.markers.remove(marker);
                    }
                }

                for (let marker of newMarkers)
                {
                    if (this.m_markers.indexOf(marker) === -1)
                    {
                        this.m_map.markers.add(marker);
                    }
                }

                this.m_markers = newMarkers;
            }
        }
    }

    private resolveSource(): atlas.source.Source
    {
        let s = this.getSource();
        if (typeof s === "string")
        {
            return this.m_map.sources.getById(s);
        }
        else
        {
            return s;
        }
    }

    private async extractFeatures(shapes: Array<atlas.data.Feature<atlas.data.Geometry, any> | atlas.Shape>): Promise<FeatureDetails<T>[]>
    {
        let points: FeatureDetails<T>[] = [];

        for (let shape of shapes)
        {
            if (shape instanceof atlas.Shape)
            {
                if (shape.getType() === "Point")
                {
                    let res        = new FeatureDetails<T>();
                    res.id         = shape.getId() + "";
                    res.position   = <atlas.data.Position>shape.getCoordinates();
                    res.properties = <T>shape.getProperties();

                    points.push(res);
                }
            }
        }

        return points;
    }

    private findCluster(clusters: FeatureCluster<T>[],
                        point: FeatureDetails<T>,
                        clusterSize: number)
    {
        for (let cluster of clusters)
        {
            let distance = distanceInPixels(this.m_map, point.position, cluster.position);
            if (distance < clusterSize)
            {
                return cluster;
            }
        }

        return null;
    }
}

export class FeatureDetails<T>
{
    id: string;
    position: atlas.data.Position;
    properties: T;
}

export class FeatureCluster<T>
{
    private m_id: string;
    private m_points: FeatureDetails<T>[] = [];

    get points(): FeatureDetails<T>[]
    {
        return this.m_points;
    }

    get id(): string
    {
        if (!this.m_id)
        {
            this.m_id = this.m_points.map((p) => p.id)
                            .sort((a,
                                   b) => UtilsService.compareStrings(a, b, true))
                            .join("/");
        }

        return this.m_id;
    }

    get position(): atlas.data.Position
    {
        let longitude = 0;
        let latitude  = 0;
        let count     = 0;

        for (let point of this.m_points)
        {
            longitude += point.position[0];
            latitude += point.position[1];
            count++;
        }

        return new atlas.data.Position(longitude / count, latitude / count);
    }

    add(point: FeatureDetails<T>)
    {
        if (!this.m_points.find((p) => p.id === point.id))
        {
            this.m_id = undefined;
            this.m_points.push(point);
        }
    }
}

type CursorType = "pointer" | "grab";

type PopupEvent = "drag" | "dragend" | "dragstart" | "open" | "close";

type HtmlMarkerEvent = "click" | "contextmenu" | "dblclick" | "drag" | "dragstart" | "dragend" | "keydown" | "keypress" | "keyup" | "mousedown" | "mousemove" | "mouseout" | "mouseover" | "mouseup";

type LayerEvent = "mousedown" | "mouseup" | "mouseover" | "mousemove" | "click" | "dblclick" | "mouseout" | "mouseenter" | "mouseleave" | "contextmenu";

type MapEvent = "mousedown" | "mouseup" | "mouseover" | "mousemove" | "click" | "dblclick" | "mouseout" | "contextmenu";

export class MapExtended
{
    constructor(private readonly m_map: atlas.Map)
    {
    }

    public newDataSource(options?: atlas.DataSourceOptions): atlas.source.DataSource
    {
        let dataSource = new atlas.source.DataSource(undefined, options);

        this.m_map.sources.add(dataSource);
        return dataSource;
    }

    //--//

    public static measureDistance(p1: Models.LongitudeLatitude | atlas.data.Position,
                                  p2: Models.LongitudeLatitude | atlas.data.Position,
                                  units: atlas.math.DistanceUnits = atlas.math.DistanceUnits.meters)
    {
        p1 = p1 instanceof Models.LongitudeLatitude ? new atlas.data.Position(p1.longitude, p1.latitude) : p1;
        p2 = p2 instanceof Models.LongitudeLatitude ? new atlas.data.Position(p2.longitude, p2.latitude) : p2;

        return atlas.math.getDistanceTo(p1, p2, units);
    }

    public static newCircle(center: Models.LongitudeLatitude,
                            radius: number): atlas.data.Feature<atlas.data.Point, any>
    {
        return MapExtended.newPoint(center, {
            subType: "Circle",
            radius : 1000
        });
    }

    public static newPoint<P>(center: Models.LongitudeLatitude,
                              properties?: P): atlas.data.Feature<atlas.data.Point, P>
    {
        return new atlas.data.Feature(new atlas.data.Point(new atlas.data.Position(center.longitude, center.latitude)), properties);
    }

    public static newPolygon<P>(polygon: Models.LocationPolygon,
                                properties?: P): atlas.data.Feature<atlas.data.Polygon, P>
    {
        return new atlas.data.Feature(new atlas.data.Polygon(MapExtended.toLinearRing(polygon)), properties);
    }

    public static newLine<P>(points: Models.LongitudeLatitude[],
                             properties?: P): atlas.data.Feature<atlas.data.LineString, P>
    {
        return new atlas.data.Feature(new atlas.data.LineString(MapExtended.toLine(points)), properties);
    }


    public static newPolygonWithHoles<P>(boundary: Models.LocationPolygon,
                                         innerExclusions: Array<Models.LocationPolygon>,
                                         properties?: P): atlas.data.Feature<atlas.data.Polygon, P>
    {
        let polygons: atlas.data.Position[][] = [];

        polygons.push(MapExtended.toLinearRing(boundary));

        for (let innerExclusion of innerExclusions)
        {
            polygons.push(MapExtended.toLinearRing(innerExclusion));
        }

        return new atlas.data.Feature(new atlas.data.Polygon(polygons), properties);
    }

    public static fromPoint(point: atlas.data.Position): Models.LongitudeLatitude
    {
        return Models.LongitudeLatitude.newInstance({
                                                        longitude: point[0],
                                                        latitude : point[1]
                                                    });
    }

    public static toPoint(point: Models.LongitudeLatitude): atlas.data.Position
    {
        return new atlas.data.Position(point.longitude, point.latitude);
    }

    public static toLine(points: Models.LongitudeLatitude[]): atlas.data.Position[]
    {
        let pointsOut: atlas.data.Position[] = [];

        for (let point of points)
        {
            pointsOut.push(MapExtended.toPoint(point));
        }

        return pointsOut;
    }

    private static toLinearRing(polygon: Models.LocationPolygon): atlas.data.Position[]
    {
        let points = MapExtended.toLine(polygon.points);

        // Close the polygon.
        points.push(points[0]);

        return points;
    }

    /**
     *
     * @param layer: z-index will be altered
     * @param beforeLayer: if set, layer will be rendered immediately below beforeLayer;
     *                     if not set, layer will be rendered on top of all map layers
     */
    public moveLayer(layer: atlas.layer.Layer,
                     beforeLayer?: atlas.layer.Layer)
    {
        this.m_map.layers.move(layer, beforeLayer);
    }

    public positionToPixel(position: atlas.data.Position | Models.LongitudeLatitude): atlas.Pixel
    {
        position = position instanceof Models.LongitudeLatitude ? MapExtended.toPoint(position) : position;
        return this.m_map.positionsToPixels([position])[0];
    }

    public distanceInPixels(a: atlas.data.Position | Models.LongitudeLatitude,
                            b: atlas.data.Position | Models.LongitudeLatitude): number
    {
        return distanceInPixels(this.m_map, a, b);
    }

    public closestSegment(target: Models.LongitudeLatitude,
                          polygon: Models.LocationPolygon): DistanceFromPolygon
    {
        if (!polygon.points || polygon.points.length < 2) return null;

        let line     = turf_lineString(MapExtended.toLinearRing(polygon));
        let targetPt = MapExtended.toPoint(target);

        let nearestPoint = turf_nearestPointOnLine(line, targetPt);

        return new DistanceFromPolygon(polygon, targetPt, nearestPoint.properties.index, nearestPoint.geometry.coordinates, this);
    }

    public closestPointToAFence(fence: Models.GeoFenceByPolygon,
                                target: Models.LongitudeLatitude): DistanceFromPolygon
    {
        let closest = this.closestSegment(target, fence.boundary);

        if (fence.innerExclusions)
        {
            for (let inner of fence.innerExclusions)
            {
                closest = DistanceFromPolygon.selectNearest(closest, this.closestSegment(target, inner));
            }
        }

        if (closest) closest.fence = fence;

        return closest;
    }

    public static isPointInsidePolygon(polygon: Models.LocationPolygon,
                                       point: Models.LongitudeLatitude): boolean
    {
        let res = turf_pointsWithinPolygon(turf_point(MapExtended.toPoint(point)), turf_polygon([MapExtended.toLinearRing(polygon)]));
        return res && res.features && res.features.length > 0;
    }

    public static checkValidFence(fence: Models.GeoFenceByPolygon,
                                  targetPolygon: Models.LocationPolygon,
                                  newPolygon: Models.LocationPolygon): boolean
    {
        if (MapExtended.computeSelfIntersections(newPolygon))
        {
            return false;
        }

        if (MapExtended.polygonsIntersect(fence.boundary, targetPolygon, newPolygon))
        {
            return false;
        }

        for (let inner of fence.innerExclusions || [])
        {
            if (MapExtended.polygonsIntersect(inner, targetPolygon, newPolygon))
            {
                return false;
            }
        }

        return true;
    }

    private static polygonsIntersect(otherPolygon: Models.LocationPolygon,
                                     targetPolygon: Models.LocationPolygon,
                                     newPolygon: Models.LocationPolygon): boolean
    {
        if (otherPolygon == targetPolygon)
        {
            return false;
        }

        let lineForOtherPolygon = turf_lineString(MapExtended.toLinearRing(otherPolygon));
        let lineForNewPolygon   = turf_lineString(MapExtended.toLinearRing(newPolygon));

        let intersection = turf_lineIntersect(lineForNewPolygon, lineForOtherPolygon);
        return intersection && intersection.features && intersection.features.length > 0;
    }

    public static computeSelfIntersections(polygon: Models.LocationPolygon): Models.LongitudeLatitude[]
    {
        let line = turf_lineString(MapExtended.toLinearRing(polygon));

        let results = turf_kinks(line);
        if (results && results.features && results.features.length)
        {
            return results.features.map((feature) => MapExtended.fromPoint(feature.geometry.coordinates));
        }

        return null;
    }

    //--//

    public removeLayer<T extends atlas.layer.Layer>(layer: T)
    {
        this.m_map.layers.remove(layer);
    }

    public addLayer<T extends atlas.layer.Layer>(layer: T): T
    {
        this.m_map.layers.add(layer);

        return layer;
    }

    public newLineLayer(dataSource: atlas.source.DataSource,
                        options?: atlas.LineLayerOptions): atlas.layer.LineLayer
    {
        let layer = new atlas.layer.LineLayer(dataSource, undefined, options);

        return this.addLayer(layer);
    }

    public newPolygonLayer(dataSource: atlas.source.DataSource,
                           options?: atlas.PolygonLayerOptions): atlas.layer.PolygonLayer
    {
        let layer = new atlas.layer.PolygonLayer(dataSource, undefined, options);

        return this.addLayer(layer);
    }

    public newBubbleLayer(source: atlas.source.Source,
                          options?: atlas.BubbleLayerOptions): atlas.layer.BubbleLayer
    {
        let layer = new atlas.layer.BubbleLayer(source, undefined, options);

        return this.addLayer(layer);
    }

    //--//

    public removeMarkers()
    {
        this.m_map.markers.clear();
    }

    public removeMarker(markers: atlas.HtmlMarker | atlas.HtmlMarker[])
    {
        this.m_map.markers.remove(markers);
    }

    public addMarker(marker: atlas.HtmlMarker)
    {
        this.m_map.markers.add(marker);
    }

    public addMarkers(markers: atlas.HtmlMarker[])
    {
        this.m_map.markers.add(markers);
    }

    public addHtmlMarkerCursorChanger(target: atlas.HtmlMarker,
                                      mouseoverCursor: CursorType,
                                      mouseleaveCursor: CursorType = "grab")
    {
        this.m_map.events.add("mouseover", target, () => this.m_map.getCanvasContainer().style.cursor = mouseoverCursor);
        this.m_map.events.add("mouseout", target, () => this.m_map.getCanvasContainer().style.cursor = mouseleaveCursor);
    }

    public addLayerCursorChanger(target: atlas.layer.Layer,
                                 mouseoverCursor: CursorType,
                                 mouseleaveCursor: CursorType = "grab")
    {
        this.m_map.events.add("mouseover", target, () => this.m_map.getCanvasContainer().style.cursor = mouseoverCursor);
        this.m_map.events.add("mouseout", target, () => this.m_map.getCanvasContainer().style.cursor = mouseleaveCursor);
    }

    public onPopupEvent(eventType: PopupEvent | PopupEvent[],
                        target: atlas.Popup | atlas.Popup[],
                        callback: (e: atlas.TargetedEvent) => void)
    {
        let eventTypes: PopupEvent[] = (typeof eventType === "string") ? [eventType] : eventType;

        for (let eventTypeSingle of eventTypes)
        {
            this.m_map.events.add(eventTypeSingle, target, callback);
        }

        return new MapEventRegistration(() =>
                                        {
                                            for (let eventTypeSingle of eventTypes)
                                            {
                                                this.m_map.events.remove(eventTypeSingle, target, callback);
                                            }
                                        });
    }

    public onHtmlMarkerEvent(eventType: HtmlMarkerEvent | HtmlMarkerEvent[],
                             target: atlas.HtmlMarker | atlas.HtmlMarker[],
                             callback: (e: atlas.TargetedEvent) => void): MapEventRegistration
    {
        let eventTypes: HtmlMarkerEvent[] = (typeof eventType === "string") ? [eventType] : eventType;

        for (let eventTypeSingle of eventTypes)
        {
            this.m_map.events.add(eventTypeSingle, target, callback);
        }

        return new MapEventRegistration(() =>
                                        {
                                            for (let eventTypeSingle of eventTypes)
                                            {
                                                this.m_map.events.remove(eventTypeSingle, target, callback);
                                            }
                                        });
    }

    public onLayerEvent(eventType: LayerEvent | LayerEvent[],
                        target: atlas.layer.Layer,
                        callback: (e: atlas.MapMouseEvent) => void): MapEventRegistration
    {
        let eventTypes: LayerEvent[] = (typeof eventType === "string") ? [eventType] : eventType;

        for (let eventTypeSingle of eventTypes)
        {
            this.m_map.events.add(eventTypeSingle, target, callback);
        }

        return new MapEventRegistration(() =>
                                        {
                                            for (let eventTypeSingle of eventTypes)
                                            {
                                                this.m_map.events.remove(eventTypeSingle, target, <any>callback);
                                            }
                                        });
    }

    public onEvent(eventType: MapEvent | MapEvent[],
                   callback: (e: atlas.MapMouseEvent) => void): MapEventRegistration
    {
        let eventTypes: MapEvent[] = (typeof eventType === "string") ? [eventType] : eventType;

        for (let eventTypeSingle of eventTypes)
        {
            this.m_map.events.add(eventTypeSingle, callback);
        }

        return new MapEventRegistration(() =>
                                        {
                                            for (let eventTypeSingle of eventTypes)
                                            {
                                                this.m_map.events.remove(eventTypeSingle, <any>callback);
                                            }
                                        });
    }

    public showPopup(content: HTMLElement | string,
                     position: atlas.data.Position,
                     pixelOffset?: atlas.Pixel): atlas.Popup
    {
        return this.showCustomPopup({
                                        content    : content,
                                        position   : position,
                                        pixelOffset: pixelOffset
                                    });
    }

    public showCustomPopup(options: atlas.PopupOptions): atlas.Popup
    {
        let popup = new atlas.Popup(options || {});

        popup.open(this.m_map);

        return popup;
    }

    public clearPopups()
    {
        this.m_map.popups.clear();
    }

    //--//

    public getCanvas(): HTMLCanvasElement
    {
        return this.m_map.getCanvas();
    }

    //--//

    public getCameraPosition(): atlas.data.Position
    {
        return this.m_map.getCamera().center;
    }

    public setCameraPosition(center: Models.LongitudeLatitude,
                             zoom?: number)
    {
        this.m_map.setCamera({
                                 center: new atlas.data.Position(center.longitude, center.latitude),
                                 zoom  : zoom
                             });
    }

    /**
     * returns true if map was able to successfully set the view to the bounds (and padding) provided
     *
     * @param bounds
     * @param padding
     */
    public setView(bounds: LongitudeLatitudeBounds,
                   padding?: number | Padding): boolean
    {
        if (bounds.isEmpty) return false;
        try
        {
            this.m_map.setCamera({
                                     bounds : bounds.box,
                                     padding: padding
                                 });
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public get zoom(): number
    {
        return this.m_map.getCamera().zoom;
    }

    public set zoom(value: number)
    {
        this.m_map.setCamera({
                                 zoom: value
                             });
    }

    public limitZoom(maxZoom: number)
    {
        if (this.zoom > maxZoom)
        {
            this.zoom = maxZoom;
        }
    }

    public resize(): void
    {
        this.m_map.resize();
    }

    public static adjustFillColor(lineColor: string,
                                  fillColor?: string): string
    {
        if (!fillColor)
        {
            fillColor = ChartColorUtilities.safeChroma(lineColor)
                                           .brighten()
                                           .hex();
        }

        return fillColor;
    }
}

export class DistanceFromPolygon
{
    public fence: Models.GeoFenceByPolygon;

    readonly distance: number;
    readonly interpolatedPoint: Models.LongitudeLatitude;
    readonly distanceInPixels: number;

    constructor(public readonly target: Models.LocationPolygon,
                source: atlas.data.Position,
                public readonly closestIndex: number,
                interpolated: atlas.data.Position,
                host: MapExtended)
    {
        this.interpolatedPoint = MapExtended.fromPoint(interpolated);
        this.distance          = turf_distance(source, interpolated);
        this.distanceInPixels  = host.distanceInPixels(source, interpolated);
    }

    static selectNearest(a: DistanceFromPolygon,
                         b: DistanceFromPolygon)
    {
        if (!a) return b;
        if (!b) return a;

        return a.distance < b.distance ? a : b;
    }
}

export class MapEventRegistration
{
    constructor(private callback: () => void)
    {
    }

    cancel()
    {
        if (this.callback)
        {
            this.callback();
            this.callback = null;
        }
    }
}

export abstract class BaseLayerExtended
{
    private m_source                                               = new atlas.source.DataSource();
    private m_featuresExt: Lookup<BaseLayerFeature<any, any, any>> = {};
    private m_featureFromLastEvent: BaseLayerFeature<any, any, any>;

    constructor(protected readonly m_mapAzure: MapExtended)
    {
        this.m_source = m_mapAzure.newDataSource();
    }

    clear()
    {
        this.m_source.clear();
        this.m_featuresExt = {};
    }

    protected newBubbleLayer(options?: atlas.BubbleLayerOptions): atlas.layer.BubbleLayer
    {
        return this.m_mapAzure.newBubbleLayer(this.m_source, options);
    }

    protected newLineLayer(options?: atlas.LineLayerOptions): atlas.layer.LineLayer
    {
        return this.m_mapAzure.newLineLayer(this.m_source, options);
    }

    protected newPolygonLayer(options?: atlas.PolygonLayerOptions): atlas.layer.PolygonLayer
    {
        return this.m_mapAzure.newPolygonLayer(this.m_source, options);
    }

    registerFeature<F extends BaseLayerFeature<any, any, any>>(featureExt: F): F
    {
        this.m_source.add(featureExt.shape);
        this.m_featuresExt[featureExt.id] = featureExt;
        return featureExt;
    }

    removeFeature<F extends BaseLayerFeature<any, any, any>>(featureExt: F)
    {
        this.m_source.remove(featureExt.id);
        delete this.m_featuresExt[featureExt.id];
    }

    protected registerMouseEvents(layer: atlas.layer.Layer): MapEventRegistration
    {
        return this.m_mapAzure.onLayerEvent([
                                                "mouseenter",
                                                "mouseover",
                                                "mouseleave",
                                                "mousedown",
                                                "mousemove",
                                                "mouseup",
                                                "click",
                                                "dblclick",
                                                "contextmenu"
                                            ], layer, (e) => this.dispatch(e));
    }

    protected dispatch(e: atlas.MapMouseEvent)
    {
        switch (e.type)
        {
            case "mouseleave":
                if (this.m_featureFromLastEvent)
                {
                    this.m_featureFromLastEvent.onEvent(e.type, e.position, e.pixel);
                }
                break;

            default:
                this.m_featureFromLastEvent = null;

                for (let shape of e.shapes)
                {
                    let id;

                    if (shape instanceof atlas.Shape)
                    {
                        id = shape.getId();
                    }
                    else
                    {
                        id = shape.id;
                    }

                    let featureExt = this.m_featuresExt[id];
                    if (featureExt)
                    {
                        if (featureExt.onEvent)
                        {
                            this.m_featureFromLastEvent = featureExt;
                            featureExt.onEvent(e.type, e.position, e.pixel);
                        }
                    }
                }
                break;
        }
    }
}

export abstract class BaseLayerFeature<L extends BaseLayerExtended, G extends atlas.data.Geometry, P>
{
    public shape: atlas.Shape;

    get id(): string
    {
        return this.shape.getId() + "";
    }

    get properties(): P
    {
        return <P>this.shape.getProperties();
    }

    onEvent: (eventType: string,
              position: atlas.data.Position,
              pixel: atlas.Pixel) => void;

    constructor(public readonly host: L,
                private featureGenerator: () => atlas.data.Feature<G, P>)
    {
        this.refresh();
    }

    refresh()
    {
        this.remove();

        this.shape = new atlas.Shape(this.featureGenerator());

        this.host.registerFeature(this);
    }

    remove()
    {
        if (this.shape) this.host.removeFeature(this);
    }
}

export class LineLayerExtended extends BaseLayerExtended
{
    public static readonly strokeWidth: number = 2.25;

    private m_layer: atlas.layer.LineLayer;

    get layer(): atlas.layer.LineLayer
    {
        return this.m_layer;
    }

    constructor(mapAzure: MapExtended,
                changeCursorOnHover: boolean = false)
    {
        super(mapAzure);

        this.m_layer = this.newLineLayer({
                                             strokeColor  : [
                                                 "get",
                                                 "lineColor"
                                             ],
                                             strokeOpacity: [
                                                 "get",
                                                 "alpha"
                                             ],
                                             strokeWidth  : LineLayerExtended.strokeWidth
                                         });

        if (changeCursorOnHover) this.m_mapAzure.addLayerCursorChanger(this.m_layer, "pointer");

        this.registerMouseEvents(this.m_layer);
    }

    addLine(path: Models.LongitudeLatitude[],
            color: string,
            inView: boolean): LineLayerFeature
    {
        return new LineLayerFeature(this, path, color, inView ? 1 : 0);
    }
}

export class LineLayerProperties
{
    lineColor: string;
    alpha: number;
}

export class LineLayerFeature extends BaseLayerFeature<LineLayerExtended, atlas.data.LineString, LineLayerProperties>
{
    constructor(lineLayer: LineLayerExtended,
                path: Models.LongitudeLatitude[],
                private lineColor: string,
                private alpha: number = 1)
    {
        super(lineLayer, () => MapExtended.newLine(path, {
            lineColor: lineColor,
            alpha    : alpha
        }));
    }

    setColor(color: string)
    {
        this.lineColor = color;
        this.setProperties();
    }

    setOpacity(alpha: number)
    {
        this.alpha = alpha;
        this.setProperties();
    }

    private setProperties()
    {
        this.shape.setProperties({
                                     lineColor: this.lineColor,
                                     alpha    : this.alpha
                                 });
    }
}

//--//

export class PinLayerExtended extends BaseLayerExtended
{
    private static readonly strokeWidthFactor: number = 3 / 8;

    private m_layer: atlas.layer.BubbleLayer;

    get layer(): atlas.layer.BubbleLayer
    {
        return this.m_layer;
    }

    set alpha(alpha: number)
    {
        this.m_layer.setOptions({
                                    strokeOpacity: alpha,
                                    opacity      : alpha
                                });
    }

    constructor(mapAzure: MapExtended,
                changeCursorOnHover: boolean = false,
                radius: number               = 8)
    {
        super(mapAzure);

        if (changeCursorOnHover) this.m_mapAzure.addLayerCursorChanger(this.m_layer, "pointer");

        this.m_layer = this.newBubbleLayer({
                                               color        : [
                                                   "get",
                                                   "pinFillColor"
                                               ],
                                               strokeColor  : [
                                                   "get",
                                                   "pinColor"
                                               ],
                                               strokeOpacity: [
                                                   "get",
                                                   "alpha"
                                               ],
                                               opacity      : [
                                                   "get",
                                                   "alpha"
                                               ],
                                               strokeWidth  : PinLayerExtended.strokeWidthFactor * radius,
                                               radius       : radius
                                           });
    }

    addPoint(center: Models.LongitudeLatitude,
             color: string): PinLayerFeature
    {
        return new PinLayerFeature(this, center, color);
    }
}

export class PinLayerProperties
{
    pinColor: string;
    pinFillColor: string;
    alpha: number;
}

export class PinLayerFeature extends BaseLayerFeature<PinLayerExtended, atlas.data.Point, PinLayerProperties>
{
    private pinFillColor: string;

    constructor(pinLayer: PinLayerExtended,
                point: Models.LongitudeLatitude,
                private color: string,
                private alpha: number = 1)
    {
        super(pinLayer, () => MapExtended.newPoint(point, {
            pinColor    : color,
            pinFillColor: color && MapExtended.adjustFillColor(color),
            alpha       : alpha
        }));
        if (color) this.pinFillColor = MapExtended.adjustFillColor(color);
    }

    setColor(color: string)
    {
        this.color        = color;
        this.pinFillColor = MapExtended.adjustFillColor(color);
        this.setProperties();
    }

    setOpacity(alpha: number)
    {
        this.alpha = alpha;
        this.setProperties();
    }

    private setProperties()
    {
        this.shape.setProperties({
                                     pinColor    : this.color,
                                     pinFillColor: this.pinFillColor,
                                     alpha       : this.alpha
                                 });
    }
}

//--//

export class FenceLayerExtended extends BaseLayerExtended
{
    private m_outlineLayer: atlas.layer.LineLayer;
    private m_insideLayer: atlas.layer.PolygonLayer;

    constructor(mapAzure: MapExtended)
    {
        super(mapAzure);

        this.m_outlineLayer = this.newLineLayer({
                                                    strokeColor: [
                                                        "get",
                                                        "lineColor"
                                                    ],
                                                    strokeWidth: 2
                                                });

        this.m_insideLayer = this.newPolygonLayer({
                                                      fillColor  : [
                                                          "get",
                                                          "fillColor"
                                                      ],
                                                      fillOpacity: 0.2
                                                  });

        this.registerMouseEvents(this.m_insideLayer);
    }

    addFence(fence: Models.GeoFenceByPolygon,
             lineColor: string,
             fillColor?: string): FenceLayerFeature
    {
        let properties: FenceLayerProperties =
                {
                    lineColor: lineColor,
                    fillColor: MapExtended.adjustFillColor(lineColor, fillColor)
                };

        return new FenceLayerFeature(this, fence, () =>
        {
            if (fence.innerExclusions && fence.innerExclusions.length > 0)
            {
                return MapExtended.newPolygonWithHoles(fence.boundary, fence.innerExclusions, properties);
            }
            else
            {
                return MapExtended.newPolygon(fence.boundary, properties);
            }
        });
    }
}

export class FenceLayerProperties
{
    lineColor: string;
    fillColor: string;
}

export class FenceLayerFeature extends BaseLayerFeature<FenceLayerExtended, atlas.data.Polygon, FenceLayerProperties>
{
    constructor(host: FenceLayerExtended,
                public readonly source: Models.GeoFenceByPolygon,
                callback: () => atlas.data.Feature<atlas.data.Polygon, FenceLayerProperties>)
    {
        super(host, callback);
    }
}

//--//

export class PolygonEditingLayerExtended extends BaseLayerExtended
{
    private m_outlineLayer: atlas.layer.LineLayer;
    private m_insideLayer: atlas.layer.PolygonLayer;
    private m_markers: atlas.HtmlMarker[]                  = []; // We can't delete any markers, otherwise Azure Maps throws errors.
    private m_markersActive: atlas.HtmlMarker[]            = []; // So we keep track of which markers are still active here.
    private m_markersRegistrations: MapEventRegistration[] = [];

    constructor(mapAzure: MapExtended)
    {
        super(mapAzure);

        this.m_outlineLayer = this.newLineLayer({
                                                    strokeColor: [
                                                        "get",
                                                        "lineColor"
                                                    ],
                                                    strokeWidth: 2
                                                });

        this.m_insideLayer = this.newPolygonLayer({
                                                      fillColor  : [
                                                          "get",
                                                          "fillColor"
                                                      ],
                                                      fillOpacity: 0.2
                                                  });

        this.registerMouseEvents(this.m_insideLayer);
    }

    public clear()
    {
        super.clear();

        for (let reg of this.m_markersRegistrations)
        {
            reg.cancel();
        }

        this.m_mapAzure.removeMarker(this.m_markers);

        this.m_markers              = [];
        this.m_markersActive        = [];
        this.m_markersRegistrations = [];
    }

    setPolygon(boundary: Models.LocationPolygon,
               allowEvents: boolean,
               isOpen: boolean,
               lineColor: string,
               fillColor?: string): PolygonEditingLayerFeature
    {
        this.clear();

        let properties         = new PolygonEditingLayerProperties();
        properties.allowEvents = allowEvents;
        properties.isOpen      = isOpen;
        properties.lineColor   = lineColor;

        if (fillColor)
        {
            properties.fillColor = fillColor;
            this.m_insideLayer.setOptions({visible: true});
        }
        else
        {
            this.m_insideLayer.setOptions({visible: false});
        }

        let featureExt = new PolygonEditingLayerFeature(this, () =>
        {
            if (isOpen)
            {
                return MapExtended.newLine(boundary.points, properties);
            }
            else
            {
                return MapExtended.newPolygon(boundary, properties);
            }
        });

        const size      = 16;
        let htmlContent = AppMappingUtilities.makeDotIcon(null, size, lineColor);
        let pixelOffset = new atlas.Pixel(-size / 2, -size / 2);

        for (let point of boundary.points)
        {
            let marker = new atlas.HtmlMarker({
                                                  position   : MapExtended.toPoint(point),
                                                  anchor     : "top-left",
                                                  htmlContent: htmlContent,
                                                  pixelOffset: pixelOffset,
                                                  color      : lineColor,
                                                  draggable  : true
                                              });

            this.m_markers.push(marker);
            this.m_markersActive.push(marker);
        }

        this.m_mapAzure.addMarkers(this.m_markers);

        if (allowEvents)
        {
            this.m_markersRegistrations.push(this.m_mapAzure.onHtmlMarkerEvent("drag", this.m_markers, (e) =>
            {
                let marker = e.target;
                if (marker instanceof atlas.HtmlMarker)
                {
                    let index = this.m_markersActive.indexOf(marker);
                    if (index >= 0)
                    {
                        let options  = marker.getOptions();
                        let newPoint = MapExtended.fromPoint(options.position);

                        if (featureExt.checkMove)
                        {
                            if (!featureExt.checkMove(index, newPoint))
                            {
                                marker.setOptions({position: MapExtended.toPoint(boundary.points[index])});
                                return;
                            }
                        }

                        boundary.points[index] = newPoint;
                        featureExt.refresh();

                        if (featureExt.onChange)
                        {
                            featureExt.onChange("moved");
                        }
                    }
                }
            }));

            this.m_markersRegistrations.push(this.m_mapAzure.onHtmlMarkerEvent("contextmenu", this.m_markers, async (e) =>
            {
                let marker = e.target;
                if (marker instanceof atlas.HtmlMarker)
                {
                    let index = this.m_markersActive.indexOf(marker);
                    if (index >= 0)
                    {
                        if (featureExt.checkDelete)
                        {
                            if (!featureExt.checkDelete(index))
                            {
                                return;
                            }
                        }

                        let options = marker.getOptions();
                        marker.setOptions({visible: false});

                        this.m_markersActive.splice(index, 1);
                        boundary.points.splice(index, 1);
                        featureExt.refresh();

                        if (featureExt.onChange)
                        {
                            featureExt.onChange("deleted");
                        }
                    }
                }
            }));
        }

        return featureExt;
    }
}

export class PolygonEditingLayerProperties
{
    allowEvents: boolean;
    isOpen: boolean;
    lineColor: string;
    fillColor: string;
}

export class PolygonEditingLayerFeature extends BaseLayerFeature<PolygonEditingLayerExtended, atlas.data.Polygon | atlas.data.LineString, PolygonEditingLayerProperties>
{
    checkAdd: (index: number,
               newPoint: Models.LongitudeLatitude) => boolean;

    checkMove: (index: number,
                newPoint: Models.LongitudeLatitude) => boolean;

    checkDelete: (index: number) => boolean;

    onChange: (change: "added" | "deleted" | "moved") => void;
}

//--//

export class SearchAddressResponse
{
    results: SearchAddressResult[];

    summary: SearchAddressSummary;
}

export class SearchAddressResult
{
    // The address of the result
    address: SearchResultAddress;

    // Optional section. Reference ids for use with the Get Search Polygon API.
    dataSources: DataSources;

    // Entry Points array
    entryPoints: SearchResultEntryPoint[];

    // Id property
    id: string;

    // A location represented as a latitude and longitude.
    position: CoordinateAbbreviated;

    // The value within a result set to indicate the relative matching score between results.
    // You can use this to determine that result x is twice as likely to be as relevant as result y if the value of x is 2x the value of y.
    // The values vary between queries and is only meant as a relative value for one result set.
    score: number;

    // One of:
    //    POI
    //    Street
    //    Geography
    //    Point Address
    //    Address Range
    //    Cross Street
    type: string;

    // The viewport that covers the result represented by the top-left and bottom-right coordinates of the viewport.
    viewport: SearchResultViewport;
}

export class SearchAddressReverseResponse
{
    addresses: SearchAddressReverseResult[];

    summary: SearchAddressSummary;
}

export class SearchAddressReverseResult
{
    // The address of the result
    address: SearchResultAddress;

    matchType: "AddressPoint" | "HouseNumberRange" | "Street";

    // A location represented as a latitude and longitude.
    position: string;
}

export class SearchAddressSummary
{
    fuzzyLevel: number;
    numResults: number;
    offset: number;

    query: string;
    queryTime: number;
    queryType: string;

    totalResults: number;
}

export class SearchResultAddress
{
    buildingNumber: string;

    country: string;
    countryCode: string;
    countryCodeISO3: string;

    countrySecondarySubdivision: string;
    countrySubdivision: string;
    countrySubdivisionName: string;

    countryTertiarySubdivision: string;

    crossStreet: string;
    extendedPostalCode: string;
    freeformAddress: string;

    // An address component which represents the name of a geographic area or locality that groups a number of addressable objects for addressing purposes, without being an administrative unit.
    // This field is used to build the freeformAddress property.
    localName: string;

    municipality: string;
    municipalitySubdivision: string;

    postalCode: string;
    routeNumbers: number[];

    street: string;
    streetName: string;
    streetNameAndNumber: string;
    streetNumber: string;
}

export class SearchResultEntryPoint
{
    position: CoordinateAbbreviated;
    type: string;
}

export class SearchResultViewport
{
    btmRightPoint: CoordinateAbbreviated;
    topLeftPoint: CoordinateAbbreviated;
}

export class CoordinateAbbreviated
{
    lat: number;
    lon: number;
}

export class DataSources
{
// Information about the geometric shape of the result. Only present if type == Geography.
    geometry: DataSourcesGeometry;
}

export class DataSourcesGeometry
{
    // Pass this as geometryId to the Get Search Polygon API to fetch geometry information for this result.
    id: string;
}

//--//

export class TimezoneByCoordinatesResponse
{
    TimeZones: TimezoneByCoordinatesSummary[];
}

export class TimezoneByCoordinatesSummary
{
    Id: string;
}
