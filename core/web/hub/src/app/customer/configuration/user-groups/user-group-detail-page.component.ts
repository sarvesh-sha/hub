import {Component, Injector} from "@angular/core";
import {ReportError} from "app/app.service";
import {UserGroupWizardDialogComponent, UserGroupsWizardState} from "app/customer/configuration/user-groups/wizard/user-group-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserGroupExtended} from "app/services/domain/user-groups.service";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-user-group-detail-page",
               templateUrl: "./user-group-detail-page.component.html",
               styleUrls  : [
                   "./user-group-detail-page.component.scss"
               ]
           })
export class UserGroupDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    userGroupId: string;
    userGroup: Models.UserGroup;

    roles: RoleExtended[]       = [];
    groups: UserGroupExtended[] = [];

    roleOptions: ControlOption<string>[];
    userRoles: Array<string>;

    groupOptions: ControlOption<string>[];
    userGroups: Array<string>;

    constructor(inj: Injector)
    {
        super(inj);

        this.userGroup = new Models.UserGroup();
    }

    protected onNavigationComplete()
    {
        this.userGroupId = this.getPathParameter("id");

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
            // Don't allow self-selection.
            if (this.userGroupId && group.model.sysId == this.userGroupId) continue;

            groupOptions.push(new ControlOption<string>(group.model.sysId, group.model.name));
        }

        if (this.userGroupId)
        {
            this.app.domain.settings.logger.info(`Loading User Group: ${this.userGroupId}`);
            let userGroup                                 = await this.app.domain.userGroups.get(this.userGroupId, null);
            this.userGroup                                = userGroup;
            this.app.ui.navigation.breadcrumbCurrentLabel = userGroup.name;
            this.app.domain.settings.logger.info(`Loaded User Group: ${JSON.stringify(this.userGroup)}`);

            for (let roleId of this.userGroup.roles)
            {
                userRoles.push(roleId.sysId);
            }

            for (let group of this.userGroup.subGroups)
            {
                userGroups.push(group.sysId);
            }
        }

        this.roleOptions = roleOptions;
        this.userRoles   = userRoles;

        this.groupOptions = groupOptions;
        this.userGroups   = userGroups;
    }

    async edit()
    {
        if (await UserGroupWizardDialogComponent.open(new UserGroupsWizardState(this.userGroup), this))
        {
            await this.load();
        }
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation(`Click Yes to confirm deletion of the user group for ${this.userGroup.name}.`))
        {
            let validations = await this.app.domain.userGroups.deleteUserGroup(this.userGroup);
            if (validations?.entries?.length === 0) this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
