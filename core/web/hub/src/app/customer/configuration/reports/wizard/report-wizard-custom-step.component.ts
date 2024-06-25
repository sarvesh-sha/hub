import {Component, Injector} from "@angular/core";
import {ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-report-custom-step",
               templateUrl: "./report-wizard-custom-step.component.html",
               providers  : [
                   WizardStep.createProvider(ReportWizardCustomStepComponent)
               ]
           })
export class ReportWizardCustomStepComponent extends WizardStep<ReportWizardState>
{
    get rangeId(): Models.TimeRangeId
    {
        return this.data.details?.getSchedulingOptions().model.range;
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    getLabel(): string { return "Report Builder"; }

    public isEnabled(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onStepSelected()
    {
    }

    public async onNext()
    {
        return false;
    }
}
