import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {AssetsService} from "app/services/domain/assets.service";

import * as Models from "app/services/proxy/model/models";

@Injectable()
export class DevicesService
{
    constructor(private api: ApiService,
                private assets: AssetsService)
    {
    }

    /**
     * Get the health for a device.
     */
    @ReportError
    public getHealthByID(id: string): Promise<Models.DeviceHealthSummary>
    {
        return this.api.devices.getDeviceHealth(id);
    }
}
