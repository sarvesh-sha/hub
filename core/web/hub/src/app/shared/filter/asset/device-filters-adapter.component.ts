import {Component} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {LocationFiltersAdapter} from "app/shared/filter/filters-adapter";

@Component({
               selector   : "o3-device-filters-adapter[request]",
               templateUrl: "./device-filters-adapter.component.html"
           })
export class DeviceFiltersAdapterComponent extends LocationFiltersAdapter<Models.DeviceFilterRequest>
{
    protected updateGlobalFilters()
    {
        super.updateGlobalFilters();

        this.filtersSvc.likeDeviceManufacturerName = this.m_request.likeDeviceManufacturerName;
        this.filtersSvc.likeDeviceProductName      = this.m_request.likeDeviceProductName;
        this.filtersSvc.likeDeviceModelName        = this.m_request.likeDeviceModelName;
    }

    protected syncWithGlobalFilters()
    {
        super.syncWithGlobalFilters();

        this.m_request.likeDeviceManufacturerName = this.filtersSvc.likeDeviceManufacturerName;
        this.m_request.likeDeviceProductName      = this.filtersSvc.likeDeviceProductName;
        this.m_request.likeDeviceModelName        = this.filtersSvc.likeDeviceModelName;
    }

    protected emptyRequestInstance(): Models.DeviceFilterRequest
    {
        return new Models.DeviceFilterRequest();
    }

    protected newRequestInstance(request?: Models.DeviceFilterRequest): Models.DeviceFilterRequest
    {
        return Models.DeviceFilterRequest.newInstance(request);
    }

    protected async appendChips(chips: FilterChip[]): Promise<void>
    {
        await super.appendChips(chips);

        let deviceChips = await Promise.all(
            [
                new FilterChip("Model #",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceModelName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceModelName ? [this.m_request.likeDeviceModelName] : []),
                new FilterChip("Product Name",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceProductName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceProductName ? [this.m_request.likeDeviceProductName] : []),
                new FilterChip("Manufacturer",
                               () =>
                               {
                                   this.resetEditRequest();
                                   this.m_editRequest.likeDeviceManufacturerName = undefined;
                                   this.applyFilterEdits();
                               },
                               () => this.m_request.likeDeviceManufacturerName ? [this.m_request.likeDeviceManufacturerName] : [])
            ]);

        for (let chip of deviceChips) chips.push(chip);
    }

    protected areEquivalent(requestA: Models.DeviceFilterRequest,
                            requestB: Models.DeviceFilterRequest): boolean
    {
        if (!super.areEquivalent(requestA, requestB)) return false;
        if (requestA.likeDeviceManufacturerName != requestB.likeDeviceManufacturerName) return false;
        if (requestA.likeDeviceProductName != requestB.likeDeviceProductName) return false;
        if (requestA.likeDeviceModelName != requestB.likeDeviceModelName) return false;

        return true;
    }
}
