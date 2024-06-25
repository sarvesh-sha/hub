import {ChangeDetectionStrategy, Component, EventEmitter, Input, Output} from "@angular/core";

import {FenceLayerExtended, PinLayerExtended, PolygonEditingLayerExtended} from "app/services/domain/azure-maps.service";

import * as Models from "app/services/proxy/model/models";
import {AppMappingUtilities} from "app/shared/mapping/app-mapping.utilities";
import {MapComponentBase} from "app/shared/mapping/map-component-base.component";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";

@Component({
               selector       : "o3-pin-map",
               templateUrl    : "../map-component-base.component.html",
               styleUrls      : ["../map-component-base.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PinMapComponent extends MapComponentBase
{
    private m_zoom: number = 15;
    private m_location: Models.LongitudeLatitude;
    private m_fences: Models.GeoFence[];

    private m_pin: PinLayerExtended;
    private m_fence: FenceLayerExtended;
    private m_polygonEditing: PolygonEditingLayerExtended;
    private m_editable: boolean;

    @Input()
    public set location(value: Models.LongitudeLatitude)
    {
        if (!AppMappingUtilities.sameLocation(this.m_location, value))
        {
            this.m_location = value;
            this.updateLocation(true);
        }
    }

    @Output() locationChange: EventEmitter<Models.LongitudeLatitude> = new EventEmitter();

    @Input()
    public set fences(value: Models.GeoFence[])
    {
        if (this.m_fences != value)
        {
            this.m_fences = value;
            this.updateLocation(true);
        }
    }

    @Input()
    public set allowEdit(value: boolean)
    {
        this.m_editable = value;
    }

    protected updatePopup()
    {
    }

    public updateZoom(): void
    {
        this.updateLocation(false);
    }

    async initializeMap()
    {
        await super.initializeMap();

        this.m_pin            = new PinLayerExtended(this.m_mapAzure);
        this.m_fence          = new FenceLayerExtended(this.m_mapAzure);
        this.m_polygonEditing = new PolygonEditingLayerExtended(this.m_mapAzure);

        // Configure map pin and location
        this.updateLocation(true);

        if (this.m_editable)
        {
            let polygon = Models.LocationPolygon.newInstance({points: [this.m_location]});

            let extEdit = this.m_polygonEditing.setPolygon(polygon, true, true, ChartColorUtilities.getColorById("Map Colors", "mapred").hex);

            extEdit.checkMove = (index,
                                 newPoint) =>
            {
                return true;
            };

            extEdit.checkDelete = (index) =>
            {
                return false;
            };

            extEdit.onChange = (change) =>
            {
                this.m_location = polygon.points[0];

                this.locationChange.emit(this.m_location);
                this.updateLocation(false);
            };
        }
    }

    private updateLocation(updateCamera: boolean)
    {
        // Do not set if map not ready
        if (!this.m_mapAzure || !this.m_location) return;

        this.m_pin.clear();
        this.m_pin.addPoint(this.m_location, ChartColorUtilities.getColorById("Map Colors", "mapblue").hex);

        this.m_fence.clear();
        for (let fence of this.m_fences || [])
        {
            if (fence instanceof Models.GeoFenceByPolygon)
            {
                this.m_fence.addFence(fence, ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex);
            }
        }

        if (updateCamera)
        {
            // Set the map view
            this.m_mapAzure.setCameraPosition(this.m_location, this.m_zoom);

            this.refreshSize();
        }
    }
}
