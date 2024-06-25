import {Component, Injector, ViewChild} from "@angular/core";
import {ReportError} from "app/app.service";
import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import {LocationFencesEditorComponent} from "app/customer/configuration/locations/location-fences-editor.component";
import {LocationWizardDialogComponent, LocationWizardState} from "app/customer/configuration/locations/wizard/location-wizard-dialog.component";
import {LocationExtended} from "app/services/domain/assets.service";
import {AzureMapsService} from "app/services/domain/azure-maps.service";
import * as SharedSvc from "app/services/domain/base.service";
import {LocationFieldset} from "app/services/domain/locations.service";
import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {OverlayComponent} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-location-detail-page",
               templateUrl: "./location-detail-page.component.html"
           })
export class LocationDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    private locationRoot: string = "/configuration/locations/location";

    locationID: string;
    outerLocationID: string;
    location: LocationExtended;
    outerLocation: LocationExtended;
    locationFields: LocationFieldset;
    loaded: boolean = false;

    address: string;
    mapLocation: Models.LongitudeLatitude;

    fieldConfigurations: LocationFieldset[];

    emailOptions: Models.DeliveryOptions;
    emailOptionsExt: DeliveryOptionsExtended;

    smsOptions: Models.DeliveryOptions;
    smsOptionsExt: DeliveryOptionsExtended;

    @ViewChild("emailOverlay", {static: true}) emailOverlay: OverlayComponent;
    @ViewChild("smsOverlay", {static: true}) smsOverlay: OverlayComponent;

    constructor(inj: Injector,
                private azureMaps: AzureMapsService)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        this.loaded = false;
        // because the component is not reloaded if the route does not change (as is the case when loading a child location), we must reset all of our data before proceeding
        this.reset();

        this.locationID      = this.getPathParameter("id");
        this.outerLocationID = this.getPathParameter("outerId");

        this.load();
    }

    reset(): void
    {
        this.locationID          = null;
        this.outerLocationID     = null;
        this.address             = null;
        this.mapLocation         = null;
        this.fieldConfigurations = null;
        this.locationFields      = null;
        this.outerLocation       = null;
    }

    async load()
    {
        this.fieldConfigurations = await this.app.domain.locations.getLocationFieldsets();

        this.app.domain.assets.logger.info(`Loading Location: ${this.locationID}`);
        this.location = await this.app.domain.assets.getTypedExtendedById(LocationExtended, this.locationID);
        if (!this.location)
        {
            this.exit();
            return;
        }

        this.app.domain.assets.logger.info(`Location Loaded.`);

        // set the fieldset associated to the location's type
        this.locationFields = this.getLocationFields(this.location.typedModel.type);
        await this.initAddress();

        // get the parent location if there is one
        this.outerLocation = await this.location.getParent();

        await this.initBreadcrumbs();

        this.loaded = true;
    }

    async initAddress()
    {
        let typedModel = this.location.typedModel;

        // initialize or nullify address
        if (this.canHaveAddress())
        {
            // store the initial address value to determine if the address has changed upon save
            this.address = this.location.typedModel?.address;
        }
        else
        {
            typedModel.address = null;
        }

        // initialize or nullify address
        if (this.canHaveGeo())
        {
            if (!this.hasGeo()) typedModel.geo = new Models.LongitudeLatitude();

            // initialize map values
            await this.initMap();
        }
        else
        {
            typedModel.geo = null;
        }
    }

    async initMap()
    {
        this.mapLocation = this.getFixedLocation() || await this.geocodeAddress();
    }

    async initBreadcrumbs()
    {
        let outerLocations: LocationExtended[] = [];
        let location                           = this.location;
        while (true)
        {
            location = await location.getParent();
            if (!location)
            {
                break;
            }

            outerLocations.splice(0, 0, location);
        }

        for (let outerLocation of outerLocations)
        {
            // push the parent location to the breadcrumb stack
            this.app.ui.navigation.addBreadcrumb(outerLocation.model.name, this.app.ui.navigation.formatUrl(this.locationRoot, [outerLocation.model.sysId]));
        }

        this.app.ui.navigation.breadcrumbCurrentLabel = this.location.model.name;
    }

    getLocationFields(locationType: Models.LocationType): LocationFieldset
    {
        if (locationType)
        {
            return this.fieldConfigurations.find((fieldset) =>
                                                 {
                                                     return <any>fieldset.locationTypeInfo.id == locationType;
                                                 });
        }
        return null;
    }

    canHaveAddress(): boolean
    {
        if (this.locationFields?.hasAddress())
        {
            return true;
        }
        return false;
    }

    hasAddress(): boolean
    {
        return (this.location?.typedModel.address || "").length > 0;
    }

    canHaveGeo(): boolean
    {
        if (this.locationFields && this.locationFields.hasLatLng())
        {
            return true;
        }
        return false;
    }

    hasGeo(): boolean
    {
        return !!this.location?.typedModel.geo;
    }

    getFixedLocation(): Models.LongitudeLatitude
    {
        let geo = this.location.typedModel.geo;

        if (geo)
        {
            if (geo.longitude === undefined || Number.isNaN(geo.longitude)) return null;
            if (geo.latitude === undefined || Number.isNaN(geo.latitude)) return null;
        }

        return geo;
    }

    async geocodeAddress(): Promise<Models.LongitudeLatitude>
    {
        if (!this.hasAddress())
        {
            return null;
        }

        let result = await this.azureMaps.bestMatch(this.location.typedModel.address);
        if (!result) return null;

        return result.location;
    }

    async remove()
    {
        if (this.location)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Location."))
            {
                if (await this.location.remove())
                {
                    this.app.framework.errors.success("Location deleted", -1);
                    this.exit();
                }
            }
        }
    }

    async editFences()
    {
        if (await LocationFencesEditorComponent.open(this, this.location, this.mapLocation))
        {
            this.location = await this.location.save();

            this.app.framework.errors.success("Updated geofences", -1);

            await this.load();
        }
    }

    exit(): void
    {
        this.app.ui.navigation.pop();
    }

    async edit()
    {
        let cfg = new LocationWizardState(this.location.typedModel);
        if (await LocationWizardDialogComponent.open(cfg, this))
        {
            this.location = LocationExtended.newInstance(this.app.domain.assets, cfg.model);
            await this.load();
        }
    }

    async newLocation()
    {
        // Create a new wizard state
        let cfg = new LocationWizardState();

        // Inject a parent asset
        cfg.model.parentAsset = this.location.getIdentity();

        // Start configuring new model
        await LocationWizardDialogComponent.open(cfg, this);
    }

    //--//

    async editEmailOptions()
    {
        this.emailOptions    = await this.location.getEmailOptions();
        this.emailOptionsExt = new DeliveryOptionsExtended(Models.DeliveryOptions.newInstance(this.emailOptions), this.app.domain, false);
        this.emailOptions    = Models.DeliveryOptions.newInstance(this.emailOptionsExt.model);

        this.emailOverlay.toggleOverlay();
    }

    @ReportError
    async saveEmailOptions()
    {
        this.emailOptions = this.emailOptionsExt.model;
        await this.location.setEmailOptions(this.emailOptions);
    }

    isEmailPristine()
    {
        return UtilsService.compareJson(this.emailOptions, this.emailOptionsExt?.model);
    }

    //--//

    async editSmsOptions()
    {
        this.smsOptions    = await this.location.getSmsOptions();
        this.smsOptionsExt = new DeliveryOptionsExtended(Models.DeliveryOptions.newInstance(this.smsOptions), this.app.domain, false);
        this.smsOptions    = Models.DeliveryOptions.newInstance(this.smsOptionsExt.model);

        this.smsOverlay.toggleOverlay();
    }

    @ReportError
    async saveSmsOptions()
    {
        this.smsOptions = this.smsOptionsExt.model;
        await this.location.setSmsOptions(this.smsOptions);
    }

    isSmsPristine()
    {
        return UtilsService.compareJson(this.smsOptions, this.smsOptionsExt?.model);
    }
}
