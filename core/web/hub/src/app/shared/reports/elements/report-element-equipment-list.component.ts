import {Component, Injector} from "@angular/core";

import {LocationExtended, LogicalAssetExtended} from "app/services/domain/assets.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-equipment-list",
               templateUrl: "./report-element-equipment-list.component.html"
           })
export class ReportElementEquipmentListComponent extends ReportElementBaseComponent<ReportElementEquipmentListData, ReportElementEquipmentListConfiguration>
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

export class ReportElementEquipmentListData extends ReportElementDataBase
{
    equipment: EquipmentFlat[];
}

export class EquipmentFlat
{
    location: LocationExtended;

    equipmentClass: string;

    numChildEquipment: number     = 0;
    numChildControlPoints: number = 0;

    constructor(public extended: LogicalAssetExtended)
    {
    }
}

export class ReportElementEquipmentListConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.EquipmentList;
        model.configuration = new ReportElementEquipmentListConfiguration();
        return model;
    }
}
