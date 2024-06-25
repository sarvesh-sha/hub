import {Component, ViewChild} from "@angular/core";
import {UserWizardState} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";
import {SelectComponent} from "framework/ui/forms/select.component";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-user-wizard-fields-step",
               templateUrl: "./user-wizard-fields.step.html",
               providers  : [
                   WizardStep.createProvider(UserWizardFieldsStep)
               ]
           })
export class UserWizardFieldsStep extends WizardStep<UserWizardState>
{
    @ViewChild("test_roleSelection") test_roleSelect: SelectComponent<Models.RecordIdentity>;

    public getLabel()
    {
        return "Phone Number & Roles";
    }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return this.isNextJumpable();
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public onStepSelected(): Promise<void>
    {
        return undefined;
    }
}
