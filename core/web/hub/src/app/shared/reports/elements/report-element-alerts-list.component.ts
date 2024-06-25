import {Component, Injector} from "@angular/core";
import {AlertExtended} from "app/services/domain/events.service";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-alerts-list",
               templateUrl: "./report-element-alerts-list.component.html"
           })
export class ReportElementAlertsListComponent extends ReportElementBaseComponent<ReportElementAlertsListData, ReportElementAlertsListConfiguration>
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

export class ReportElementAlertsListData extends ReportElementDataBase
{
    alerts: AlertExtended[];
}

export class ReportElementAlertsListConfiguration extends ReportElementConfigurationBase
{
    public static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.AlertsList;
        model.configuration = new ReportElementAlertsListConfiguration();
        return model;
    }
}
