import {Component} from "@angular/core";

import {DeviceElementFlat, DeviceElementsListBase} from "app/customer/device-elements/device-elements-list-base";
import {WorkflowsSummaryPageComponent} from "app/customer/workflows/workflows-summary-page.component";


@Component({
               selector   : "o3-device-elements-list",
               templateUrl: "./device-elements-list.component.html"
           })
export class DeviceElementsListComponent extends DeviceElementsListBase<DeviceElementFlat>
{
    public newRow(): DeviceElementFlat
    {
        return new DeviceElementFlat();
    }

    public getTableConfigId(): string { return "device-elements"; }

    goToWorkflows(item: DeviceElementFlat)
    {
        WorkflowsSummaryPageComponent.navigate(this.app, {assetIDs: [item.extended.model.sysId]});
    }
}
