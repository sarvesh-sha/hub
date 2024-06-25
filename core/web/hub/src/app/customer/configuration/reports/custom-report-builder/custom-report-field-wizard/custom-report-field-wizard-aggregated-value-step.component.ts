import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-aggregated-value-step",
               templateUrl: "./custom-report-field-wizard-aggregated-value-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardAggregatedValueStepComponent)]
           })
export class CustomReportFieldWizardAggregatedValueStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementAggregatedValue
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementAggregatedValue);
    }

    //--//

    public getLabel(): string
    {
        return "Aggregated Value";
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
