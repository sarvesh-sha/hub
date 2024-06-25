import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-name-step",
               templateUrl: "./custom-report-field-wizard-name-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardNameStepComponent)]
           })
export class CustomReportFieldWizardNameStepComponent extends WizardStep<CustomFieldData>
{
    get elementWithLabel(): ReportElementWithLabel
    {
        if (this.data.element instanceof Models.CustomReportElementAggregatedValue ||
            this.data.element instanceof Models.CustomReportElementAggregationTable ||
            this.data.element instanceof Models.CustomReportElementAggregationTrend ||
            this.data.element instanceof Models.CustomReportElementAlertFeed)
        {
            return this.data.element;
        }

        return null;
    }

    public getLabel(): string
    {
        return "Name";
    }

    public isEnabled(): boolean
    {
        switch (this.data.elementType)
        {
            case "CustomReportElementAggregatedValue":
            case "CustomReportElementAggregationTable":
            case "CustomReportElementAggregationTrend":
            case "CustomReportElementAlertFeed":
                return true;
        }

        return false;
    }

    public isValid(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onStepSelected()
    {
    }
}

interface ReportElementWithLabel
{
    label: string;
}
