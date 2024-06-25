import {Component, Input} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {VerticalViewWindow} from "framework/ui/charting/vertical-view-window";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-chart-set-step",
               templateUrl: "./custom-report-field-wizard-chart-set-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardChartSetStepComponent)]
           })
export class CustomReportFieldWizardChartSetStepComponent extends WizardStep<CustomFieldData>
{
    @Input() viewWindow: VerticalViewWindow;

    get typedElement(): Models.CustomReportElementChartSet
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementChartSet);
    }

    //--//

    public getLabel(): string
    {
        return "Chart";
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
        if (!this.wizard.stepForm?.valid) return false;
        return !!this.typedElement?.charts.length;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
