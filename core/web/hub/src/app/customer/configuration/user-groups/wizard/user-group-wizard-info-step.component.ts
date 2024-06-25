import {Component} from "@angular/core";
import {UserGroupsWizardState} from "app/customer/configuration/user-groups/wizard/user-group-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-user-group-wizard-info-step",
               templateUrl: "./user-group-wizard-info-step.component.html",
               providers  : [
                   WizardStep.createProvider(UserGroupWizardInfoStep)
               ]
           })
export class UserGroupWizardInfoStep extends WizardStep<UserGroupsWizardState>
{
    public getLabel()
    {
        return "Name & Description";
    }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        return !!this.data.userGroup.name && !!this.data.userGroup.description;
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
