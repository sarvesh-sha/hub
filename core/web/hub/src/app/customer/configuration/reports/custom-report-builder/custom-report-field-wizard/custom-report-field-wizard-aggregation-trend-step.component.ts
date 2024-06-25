import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-aggregation-trend-step",
               templateUrl: "./custom-report-field-wizard-aggregation-trend-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardAggregationTrendStepComponent)]
           })
export class CustomReportFieldWizardAggregationTrendStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementAggregationTrend
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementAggregationTrend);
    }

    //--//

    public getLabel(): string
    {
        return "Aggregation Trend";
    }

    public isEnabled(): boolean
    {
        return !!this.typedElement;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return this.wizard.stepForm?.valid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
