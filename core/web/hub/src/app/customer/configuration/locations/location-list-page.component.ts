import {Component} from "@angular/core";
import {LocationWizardDialogComponent, LocationWizardState} from "app/customer/configuration/locations/wizard/location-wizard-dialog.component";
import {LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";

@Component({
               selector   : "o3-location-list-page",
               templateUrl: "./location-list-page.component.html"
           })
export class LocationListPageComponent extends SharedSvc.BaseApplicationComponent
{
    loading: boolean = false;

    config: any = {
        messages: {
            emptyMessage: "Loading Locations...",
            totalMessage: "total"
        }
    };

    locations: LocationExtended[];

    ngOnInit()
    {
        super.ngOnInit();

        this.loadLocations();
    }

    async loadLocations()
    {
        this.loading = true;

        try
        {
            this.locations                    = await this.app.domain.locations.getTopLocations();
            this.config.messages.emptyMessage = "No Locations to display.";
        }
        catch (error)
        {
            this.config.messages.emptyMessage = "No Locations to display.";
        }

        this.loading = false;
    }

    async new()
    {
        await LocationWizardDialogComponent.open(new LocationWizardState(), this);
    }
}
