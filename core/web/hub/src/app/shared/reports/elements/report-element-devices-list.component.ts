import {Component, Injector} from "@angular/core";

import {DeviceExtended, LocationExtended} from "app/services/domain/assets.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-devices-list",
               templateUrl: "./report-element-devices-list.component.html"
           })
export class ReportElementDevicesListComponent extends ReportElementBaseComponent<ReportElementDevicesListData, ReportElementDevicesListConfiguration>
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    afterConfigurationChanges()
    {
        this.markAsComplete();
    }
}

export class ReportElementDevicesListData extends ReportElementDataBase
{
    devices: DeviceFlat[];
}

export class DeviceFlat
{
    location: LocationExtended;

    constructor(public extended: DeviceExtended)
    {
    }
}

export class ReportElementDevicesListConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.DevicesList;
        model.configuration = new ReportElementDevicesListConfiguration();
        return model;
    }
}
