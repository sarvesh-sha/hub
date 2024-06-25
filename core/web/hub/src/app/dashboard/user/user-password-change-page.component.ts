import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import * as SharedSvc from "app/services/domain/base.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-user-password-change-page",
               templateUrl: "./user-password-change-page.component.html"
           })
export class UserPasswordChangePageComponent extends SharedSvc.BaseApplicationComponent
{

    user: Models.User;
    currentPassword: string;
    newPassword: string;
    passwordConfirmation: string;

    @ViewChild("passwordForm", {static: true}) passwordForm: NgForm;

    constructor(inj: Injector)
    {
        super(inj);
    }

    ngAfterViewInit()
    {
        this.user = this.app.domain.users.user;
    }

    @ReportError
    async save()
    {
        if (this.newPassword != this.passwordConfirmation)
        {
            throw this.app.framework.errors.error("PASSWORDS_DONT_MATCH", "Your password confirmation does not match.");
        }

        this.app.domain.users.logger.info(`Saving User Password...`);
        this.user = await this.app.domain.users.changePassword(this.currentPassword, this.newPassword);
        this.app.domain.users.logger.info(`User Password Saved: ${JSON.stringify(this.user)}`);
        this.app.ui.navigation.pop();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
