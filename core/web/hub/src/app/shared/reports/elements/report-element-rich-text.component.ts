import {Component, Injector} from "@angular/core";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";

@Component({
               selector   : "o3-report-element-rich-text",
               templateUrl: "./report-element-rich-text.component.html"
           })
export class ReportElementRichTextComponent extends ReportElementBaseComponent<ReportElementRichTextData, ReportElementRichTextConfiguration>
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

export class ReportElementRichTextConfiguration extends ReportElementConfigurationBase
{
    constructor(public data: any[],
                public backgroundColor: string)
    {
        super();
    }

    static newReportModel(data: any[],
                          backgroundColor: string)
    {
        let model           = new ReportElementModel();
        model.type          = ReportElementType.RichText;
        model.configuration = new ReportElementRichTextConfiguration(data, backgroundColor);
        return model;
    }
}

export class ReportElementRichTextData extends ReportElementDataBase
{
}
