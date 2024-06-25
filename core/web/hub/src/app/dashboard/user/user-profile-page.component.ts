import {Component, Injector} from "@angular/core";
import {UserWizardDialogComponent, UserWizardState} from "app/customer/configuration/users/wizard/user-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserGroupExtended} from "app/services/domain/user-groups.service";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-user-profile-page",
               templateUrl: "./user-profile-page.component.html"
           })
export class UserProfilePageComponent extends SharedSvc.BaseComponentWithRouter
{
    user: Models.User;
    roles: RoleExtended[]       = [];
    groups: UserGroupExtended[] = [];

    roleOptions: ControlOption<string>[];
    userRoles: Array<string>;

    groupOptions: ControlOption<string>[];
    userGroups: Array<string>;

    constructor(inj: Injector)
    {
        super(inj);

        this.user = new Models.User();
    }

    protected onNavigationComplete()
    {
        this.user = this.app.domain.users.user;

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


        for (let roleId of this.user.roles)
        {
            userRoles.push(roleId.sysId);
        }

        for (let group of this.user.groups)
        {
            userGroups.push(group.sysId);
        }


        this.roleOptions = roleOptions;
        this.userRoles   = userRoles;

        this.groupOptions = groupOptions;
        this.userGroups   = userGroups;
    }

    async edit()
    {
        if (await UserWizardDialogComponent.open(new UserWizardState(this.user), this))
        {
            this.load();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
