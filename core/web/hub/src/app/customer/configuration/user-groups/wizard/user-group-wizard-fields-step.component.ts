import {Component} from "@angular/core";
import {UserGroupsWizardState} from "app/customer/configuration/user-groups/wizard/user-group-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-user-group-wizard-fields-step",
               templateUrl: "./user-group-wizard-fields-step.component.html",
               providers  : [
                   WizardStep.createProvider(UserGroupWizardFieldsStep)
               ]
           })
export class UserGroupWizardFieldsStep extends WizardStep<UserGroupsWizardState>
{
    public getLabel()
    {
        return "Roles & Sub-groups";
    }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return true;
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
