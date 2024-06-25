import {Component, Inject, Injector} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {LocationExtended} from "app/services/domain/assets.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {LocationFieldset} from "app/services/domain/locations.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./location-wizard-dialog.component.html"
           })
export class LocationWizardDialogComponent extends WizardDialogComponent<LocationWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: LocationWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, LocationWizardDialogComponent);
    }
}

export class LocationWizardState extends WizardDialogState
{
    outerId: string;
    model: Models.Location;

    locationTypes: ControlOption<Models.LocationType>[];
    fieldConfigurations: LocationFieldset[];
    locationFields: LocationFieldset;

    constructor(model?: Models.Location)
    {
        super(!model);
        this.model = model ? model : Models.Location.newInstance({});
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            let location = <LocationExtended>comp.app.domain.assets.wrapModel(this.model);
            location.navigateTo();
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            let location = <LocationExtended>comp.app.domain.assets.wrapModel(this.model);
            this.model   = (await location.save()).typedModel;
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            this.locationTypes       = await comp.app.domain.locations.getLocationTypes();
            this.fieldConfigurations = await comp.app.domain.locations.getLocationFieldsets();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    getLocationFields(locationType: Models.LocationType): LocationFieldset
    {
        return locationType ? this.fieldConfigurations.find((fieldset) => fieldset.locationTypeInfo.id == locationType) : null;
    }

    setLocationFields(locationType: Models.LocationType): void
    {
        this.locationFields = this.getLocationFields(locationType);
    }

    canHaveAddress(): boolean
    {
        if (this.locationFields?.hasAddress())
        {
            return true;
        }

        return false;
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
        return !!this.model.geo;
    }

    hasLocation(): boolean
    {
        let geo = this.getFixedLocation();
        return !!geo;
    }

    setFixedLocation(location: Models.LongitudeLatitude): void
    {
        if (this.canHaveGeo() && location)
        {
            this.model.geo = location;
        }
    }

    getFixedLocation(): Models.LongitudeLatitude
    {
        let geo = this.model.geo;

        if (geo)
        {
            if (geo.longitude === undefined || Number.isNaN(geo.longitude)) return null;
            if (geo.latitude === undefined || Number.isNaN(geo.latitude)) return null;
        }

        return geo;
    }
}
