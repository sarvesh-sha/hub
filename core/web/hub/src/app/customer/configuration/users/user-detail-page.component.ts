import {Component, Injector} from "@angular/core";
import {UserWizardDialogComponent, UserWizardState} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserGroupExtended} from "app/services/domain/user-groups.service";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-user-detail-page",
               templateUrl: "./user-detail-page.component.html",
               styleUrls  : [
                   "./user-detail-page.component.scss"
               ]
           })
export class UserDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    userID: string;
    user: Models.User;
    roles: RoleExtended[]       = [];
    groups: UserGroupExtended[] = [];

    roleOptions: ControlOption<string>[];
    userRoles: Array<string>;

    groupOptions: ControlOption<string>[];
    userGroups: Array<string>;

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
        this.roles  = await this.app.domain.settings.getRolesList();
        this.groups = await this.app.domain.settings.getGroupsList();

        let roleOptions: ControlOption<string>[] = [];
        let userRoles: string[]                  = [];

        let groupOptions: ControlOption<string>[] = [];
        let userGroups: string[]                  = [];

        for (let role of this.roles)
        {
            roleOptions.push(new ControlOption<string>(role.model.sysId, role.model.name));
        }

        for (let group of this.groups)
        {
            groupOptions.push(new ControlOption<string>(group.model.sysId, group.model.name));
        }

        if (this.userID)
        {
            this.app.domain.settings.logger.info(`Loading User: ${this.userID}`);
            let user                                      = await this.app.domain.settings.getUserByID(this.userID);
            this.user                                     = user;
            this.app.ui.navigation.breadcrumbCurrentLabel = (user.firstName + " " + user.lastName);
            this.app.domain.settings.logger.info(`Loaded User: ${JSON.stringify(this.user)}`);

            for (let roleId of this.user.roles)
            {
                userRoles.push(roleId.sysId);
            }

            for (let group of this.user.groups)
            {
                userGroups.push(group.sysId);
            }
        }

        this.roleOptions = roleOptions;
        this.userRoles   = userRoles;

        this.groupOptions = groupOptions;
        this.userGroups   = userGroups;
    }

    async impersonate()
    {
        await this.app.domain.users.impersonate(this.user.sysId);

        this.app.ui.navigation.go("/");
    }

    async edit()
    {
        if (await UserWizardDialogComponent.open(new UserWizardState(this.user), this))
        {
            this.load();
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
