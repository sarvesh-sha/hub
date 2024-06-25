import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";

import {RoleExtended} from "app/services/domain/roles.service";

import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-users-creation-page",
               templateUrl: "./users-creation-page.component.html"
           })
export class UsersCreationPageComponent extends SharedSvc.BaseApplicationComponent
{
    user: Models.UserCreationRequest;
    roles: RoleExtended[] = [];
    userRoles: any        = {};

    password: string;
    passwordConfirmation: string;

    constructor(inj: Injector)
    {
        super(inj);

        this.user = new Models.UserCreationRequest();
    }

    ngAfterViewInit()
    {
        this.loadRoles();
    }

    async load()
    {
        this.app.ui.navigation.breadcrumbCurrentLabel = "New User";
        this.app.domain.settings.logger.debug(`New User Ready.`);
    }

    async loadRoles()
    {
        let settings = this.app.domain.settings;
        settings.logger.debug(`Loading Roles..`);
        this.roles = await settings.getRolesList();
        settings.logger.debug(`Roles Loaded.`);

        this.load();
    }

    async save()
    {
        if (this.password != this.passwordConfirmation)
        {
            throw this.app.framework.errors.error("PASSWORDS_DONT_MATCH", "Your password confirmation does not match.");
        }

        let settings = this.app.domain.settings;
        settings.logger.info(`Creating User: ${this.user.emailAddress}`);
        this.user.password = this.password;

        // sync roles
        this.user.roles = this.app.domain.users.mapRolesFromName(this.userRoles);

        await this.app.domain.users.createUser(this.user);
        settings.logger.info(`User Saved: ${JSON.stringify(this.user)}`);
        this.exit();
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
