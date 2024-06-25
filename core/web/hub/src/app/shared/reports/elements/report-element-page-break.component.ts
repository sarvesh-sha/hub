import {Component, Injector} from "@angular/core";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-page-break",
               templateUrl: "./report-element-page-break.component.html"
           })
export class ReportElementPageBreakComponent extends ReportElementBaseComponent<ReportElementPageBreakData, ReportElementPageBreakConfiguration>
{
    constructor(inj: Injector)
    {
        super(inj);
    }

    async afterConfigurationChanges()
    {
        this.markAsComplete();
    }
}

export class ReportElementPageBreakConfiguration extends ReportElementConfigurationBase
{
    constructor()
    {
        super();
    }

    static newReportModel()
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.PageBreak;
        model.configuration = new ReportElementPageBreakConfiguration();
        return model;
    }
}

export class ReportElementPageBreakData extends ReportElementDataBase
{
}
