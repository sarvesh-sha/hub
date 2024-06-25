import {Component} from "@angular/core";
import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import {ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import {SchedulingTypeOptions} from "app/services/domain/reporting.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-report-delivery-step",
               templateUrl: "./report-wizard-delivery-step.component.html",
               providers  : [
                   WizardStep.createProvider(ReportWizardDeliveryStepComponent)
               ]
           })
export class ReportWizardDeliveryStepComponent extends WizardStep<ReportWizardState>
{
    scheduleOptions = SchedulingTypeOptions;

    get deliveryOptions(): DeliveryOptionsExtended
    {
        return this.data?.schedulingOptions?.deliveryOptions;
    }

    public getLabel(): string
    {
        return "Delivery Options";
    }

    public isEnabled(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return false;
    }

    public isValid(): boolean
    {
        return this.data.reportDefinition && !!this.data.schedulingOptions.model && this.data.schedulingOptions.deliveryOptions.isValid();
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
