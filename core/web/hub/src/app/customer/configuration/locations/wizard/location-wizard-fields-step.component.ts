import {Component} from "@angular/core";
import {LocationWizardState} from "app/customer/configuration/locations/wizard/location-wizard-dialog.component";
import {AzureMapsService, SearchAddressResult} from "app/services/domain/azure-maps.service";
import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {of, Subject} from "rxjs";
import {debounceTime, delay, flatMap} from "rxjs/operators";

@Component({
               selector   : "o3-location-wizard-fields-step",
               templateUrl: "./location-wizard-fields-step.component.html",
               providers  : [
                   WizardStep.createProvider(LocationWizardFieldsStep)
               ]
           })
export class LocationWizardFieldsStep extends WizardStep<LocationWizardState>
{
    mapLocation: Models.LongitudeLatitude;

    searchLast: string;
    searchResults: SearchAddressResult[] = [];
    searchTrigger: Subject<string>       = new Subject<string>();
    searching: boolean                   = false;

    public getLabel() { return "Details"; }

    public async setData(data: LocationWizardState): Promise<void>
    {
        await super.setData(data);

        // wire up search trigger
        this.searchTrigger
            .pipe(debounceTime(250), flatMap((search) =>
                                             {
                                                 return of(search)
                                                     .pipe(delay(100));
                                             }))
            .subscribe((searchText) =>
                       {
                           if (searchText) this.performSearch(searchText);
                       });
    }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return this.isNextJumpable();
    }

    public isNextJumpable()
    {
        return this.data.model.name != null && this.data.model.type != null;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
        this.searchLast = this.data.model.address;

        this.data.setLocationFields(this.data.model.type);

        // initialize or nullify address
        if (this.data.canHaveGeo())
        {
            if (!this.data.hasGeo()) this.data.model.geo = new Models.LongitudeLatitude();

            // initialize map values
            this.mapLocation = this.data.getFixedLocation() || await this.geocodeAddress();
        }
        else
        {
            this.data.model.geo = null;
        }
    }

    //--//

    async onSearchTextChanged(searchText: string)
    {
        this.searchTrigger.next(searchText);
    }

    async performSearch(searchText: string)
    {
        if (this.searchLast != searchText)
        {
            this.searching     = true;
            this.searchResults = await this.getAzureMaps()
                                           .searchAddress(searchText);
            this.searching     = false;
            this.searchLast    = searchText;
        }
    }

    private getAzureMaps()
    {
        return this.inject(AzureMapsService);
    }

    async selectSearchResult(result: SearchAddressResult)
    {
        this.data.model.address = result.address.freeformAddress;

        if (result.position)
        {
            this.mapLocation = Models.LongitudeLatitude.newInstance({
                                                                        longitude: result.position.lon,
                                                                        latitude : result.position.lat
                                                                    });

            this.data.model.geo = this.mapLocation;

            let timeZone = await this.getAzureMaps()
                                     .getTimeZoneByCoordinates(this.mapLocation);
            if (timeZone)
            {
                this.data.model.timeZone = timeZone;
            }
        }
    }

    async geocodeAddress(): Promise<Models.LongitudeLatitude>
    {
        let result = await this.getAzureMaps()
                               .bestMatch(this.data.model.address);
        if (!result) return null;

        return result.location;
    }

    public async updateLocation(location: Models.LongitudeLatitude)
    {
        this.mapLocation = location;

        let searchResults = await this.getAzureMaps()
                                      .searchLocation(location);

        if (searchResults?.length > 0)
        {
            this.data.model.address = searchResults[0].freeformAddress;
        }
    }
}
