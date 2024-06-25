import {ContentChild, Directive, ElementRef, Input, TemplateRef, ViewChild} from "@angular/core";
import {LocationExtended} from "app/services/domain/assets.service";

import {AzureMapsService, FenceLayerExtended, MapEventRegistration, MapExtended} from "app/services/domain/azure-maps.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import * as atlas from "azure-maps-control";
import {ChartColorUtilities} from "framework/ui/charting/core/colors";

@Directive()
export abstract class MapComponentBase extends BaseApplicationComponent
{
    protected popup: atlas.Popup;

    @Input() showZoomReset: boolean = false;

    @Input() set popupOpen(open: boolean)
    {
        if (!this.popup || this.m_disablePopup) return;

        if (open)
        {
            if (!this.popup.isOpen()) this.popup.open();
        }
        else if (this.popup.isOpen()) this.popup.close();
    }

    protected m_popupBackgroundColor: string = "#FFFFFF";
    @Input() set popupBackgroundColor(color: string)
    {
        if (this.m_popupBackgroundColor !== color)
        {
            this.m_popupBackgroundColor = color;
            if (this.popup) this.popup.setOptions({fillColor: color});
        }
    }

    private m_disablePopup: boolean = false;
    @Input() set disablePopup(disable: boolean)
    {
        if (typeof disable === "boolean" && this.m_disablePopup !== disable)
        {
            this.m_disablePopup = disable;
            if (!this.m_disablePopup) this.updatePopup();
        }
    }

    get disablePopup(): boolean
    {
        return this.m_disablePopup;
    }

    @ViewChild("mapContainer", {static: true}) mapRef: ElementRef;

    protected m_popupContent: HTMLElement;
    @ViewChild("popupContent") set popupContent(element: ElementRef)
    {
        this.m_popupContent = element?.nativeElement;
        this.updatePopup();
    }

    @ContentChild("popupTemplate") popupTemplate: TemplateRef<any>;

    protected m_mapAzure: MapExtended;
    private mapSubs: MapEventRegistration[] = [];

    get initialized(): boolean
    {
        return !!this.m_mapAzure;
    }

    public async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        await this.initializeMap();
    }

    public ngOnDestroy(): void
    {
        super.ngOnDestroy();

        if (this.m_mapAzure) this.clearSubs();
    }

    protected abstract updatePopup(): void;

    public abstract updateZoom(): void;

    protected async initializeMap()
    {
        if (this.m_mapAzure) this.clearSubs();
        let azureMaps   = this.inject(AzureMapsService);
        this.m_mapAzure = await azureMaps.allocateMap(this.mapRef.nativeElement);
    }

    public refreshSize(): boolean
    {
        if (this.m_mapAzure)
        {
            this.m_mapAzure.resize();
            return true;
        }

        return false;
    }

    protected registerMapSub(registration: MapEventRegistration)
    {
        this.mapSubs.push(registration);
    }

    protected getCanvas(): HTMLCanvasElement
    {
        if (!this.m_mapAzure) return null;

        return this.m_mapAzure.getCanvas();
    }

    private clearSubs()
    {
        this.mapSubs.forEach((sub) => sub.cancel());
        this.mapSubs = [];
    }

    protected async displayAllGeoFences(fenceLayer: FenceLayerExtended)
    {
        let filters                = new Models.LocationFilterRequest();
        filters.hasGeoFences       = true;
        let response               = await this.app.domain.assets.getList(filters);
        let locationsWithGeoFences = await this.app.domain.assets.getTypedPage(LocationExtended, response.results, 0, 1000);

        fenceLayer.clear();

        for (let locExt of locationsWithGeoFences)
        {
            for (let fence of locExt.typedModel.fences || [])
            {
                if (fence instanceof Models.GeoFenceByPolygon)
                {
                    fenceLayer.addFence(fence, ChartColorUtilities.getColorById("Map Colors", "mapyellow").hex);
                }
            }
        }
    }
}
