import {Component, ElementRef, ViewChild} from "@angular/core";
import {UserWizardState} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-user-wizard-info-step",
               templateUrl: "./user-wizard-info.step.html",
               providers  : [
                   WizardStep.createProvider(UserWizardInfoStep)
               ]
           })
export class UserWizardInfoStep extends WizardStep<UserWizardState>
{
    @ViewChild("test_firstName", {read: ElementRef}) test_firstName: ElementRef;
    @ViewChild("test_lastName", {read: ElementRef}) test_lastName: ElementRef;
    @ViewChild("test_email", {read: ElementRef}) test_email: ElementRef;
    @ViewChild("test_password", {read: ElementRef}) test_password: ElementRef;
    @ViewChild("test_passwordConfirm", {read: ElementRef}) test_passwordConfirm: ElementRef;

    public getLabel()
    {
        return this.data?.isNew ? "Name, Email & Password" : "Name & Email";
    }

    public isEnabled()
    {
        return true;
    }

    public isValid()
    {
        let hasPrimary: boolean = !!this.data.user.firstName && !!this.data.user.lastName && !!this.data.user.emailAddress;
        let hasNew: boolean     = this.data.isNew ? this.passwordsMatch() : true;
        return hasPrimary && hasNew;
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

    private passwordsMatch()
    {
        return !!this.data.password && !!this.data.passwordConfirmation && this.data.password === this.data.passwordConfirmation;
    }
}
