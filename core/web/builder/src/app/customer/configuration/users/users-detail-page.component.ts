import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector   : "o3-users-detail-page",
               templateUrl: "./users-detail-page.component.html",
               styleUrls  : [
                   "./users-detail-page.component.scss"
               ]
           })
export class UsersDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    userID: string;
    user: Models.User;
    roles: RoleExtended[]                 = [];
    userRoles: { [key: string]: boolean } = {};

    get isCurrentUser()
    {
        if (this.user && this.app.domain.users.user && this.user.sysId == this.app.domain.users.user.sysId) return true;
        return false;
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.user = new Models.User();
    }

    protected onNavigationComplete()
    {
        this.userID = this.getPathParameter("id");

        this.load();
    }

    async load()
    {
        this.app.domain.settings.logger.info(`Loading Roles..`);
        this.roles = await this.app.domain.settings.getRolesList();
        this.app.domain.settings.logger.info(`Roles Loaded.`);

        if (this.userID)
        {
            this.app.domain.settings.logger.info(`Loading User: ${this.userID}`);
            let user                                      = await this.app.domain.settings.getUserByID(this.userID);
            this.user                                     = user;
            this.app.ui.navigation.breadcrumbCurrentLabel = (user.firstName + " " + user.lastName);
            this.app.domain.settings.logger.info(`Loaded User: ${JSON.stringify(this.user)}`);

            this.userRoles = {};
            for (let roleId of this.user.roles)
            {
                this.userRoles[roleId.sysId] = true;
            }
        }
    }

    async save()
    {
        if (this.user)
        {
            // sync roles
            this.user.roles = this.app.domain.users.mapRoles(this.userRoles);

            // save the user
            this.app.domain.settings.logger.info(`Saving User: ${this.userID}`);
            this.user = await this.app.domain.users.saveUser(this.user);
            this.app.domain.settings.logger.info(`User Saved: ${JSON.stringify(this.user)}`);

            this.exit();
        }
    }

    async remove()
    {
        if (!this.isCurrentUser)
        {
            if (await this.confirmOperation(`Click Yes to confirm deletion of the user account for ${this.user.firstName} ${this.user.lastName}.`))
            {
                let validations = await this.app.domain.users.deleteUser(this.user);
                if (validations && validations.entries && validations.entries.length == 0) this.exit();
            }
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
