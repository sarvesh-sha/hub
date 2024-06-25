import {Component, Injector} from "@angular/core";
import {ReportElementBaseComponent, ReportElementConfigurationBase, ReportElementDataBase, ReportElementModel, ReportElementType} from "app/shared/reports/elements/report-element-base.component";
import {ControlOption} from "framework/ui/control-option";
import {LogoService} from "framework/ui/logo/logo.service";

@Component({
               selector   : "o3-report-element-header",
               templateUrl: "./report-element-header.component.html"
           })
export class ReportElementHeaderComponent extends ReportElementBaseComponent<ReportElementHeaderData, ReportElementHeaderConfiguration>
{
    constructor(inj: Injector,
                public logo: LogoService)
    {
        super(inj);
    }

    afterConfigurationChanges()
    {
        this.markAsComplete();
    }

    get activatedOptions(): ControlOption<string>[]
    {
        let activated: ControlOption<string>[] = [];

        for (let option of this.data.options)
        {
            if (this.hasFilter(option.id)) activated.push(option);
        }

        return activated;
    }

    hasFilter(id: string): boolean
    {
        if (this.data.filters && this.data.filters[id] && (<string[]>this.data.filters[id]).length > 0) return true;
        return false;
    }

    filterDescription(option: ControlOption<string>): string
    {
        if (this.hasFilter(option.id))
        {
            // grab our selected filter values
            let selectedValues = <string[]>this.data.filters[option.id];

            // check if the children define the available options
            let availableOptions = option.children;

            return ControlOption.getLabels(selectedValues, availableOptions);
        }

        return null;
    }
}

export class ReportElementHeaderConfiguration extends ReportElementConfigurationBase
{
    title: string;

    public static newReportModel(title: string)
    {
        let model  = new ReportElementModel();
        model.type = ReportElementType.Header;

        let config          = new ReportElementHeaderConfiguration();
        config.title        = title;
        model.configuration = config;

        return model;
    }
}

export class ReportElementHeaderData extends ReportElementDataBase
{
    rangeStart: Date;

    rangeEnd: Date;

    reportName: string;

    reportDescription: string;

    options: ControlOption<string>[] = [];

    filters: any;

    logoBase64: string;

    logoLeft: boolean;
}
