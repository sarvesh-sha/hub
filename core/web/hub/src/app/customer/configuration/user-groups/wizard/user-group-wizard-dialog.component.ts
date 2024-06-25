import {Component, Inject, Injector} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./user-group-wizard-dialog.component.html"
           })
export class UserGroupWizardDialogComponent extends WizardDialogComponent<UserGroupsWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: UserGroupsWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, UserGroupWizardDialogComponent);
    }
}

export class UserGroupsWizardState extends WizardDialogState
{
    userGroup: Models.UserGroup;

    roles: Models.RecordIdentity[]  = [];
    groups: Models.RecordIdentity[] = [];

    roleOptions: ControlOption<Models.RecordIdentity>[];
    userRoles: Array<Models.RecordIdentity>;

    groupOptions: ControlOption<Models.RecordIdentity>[];
    userGroups: Array<Models.RecordIdentity>;

    constructor(model?: Models.UserGroup)
    {
        super(!model);
        this.userGroup = model ? model : Models.UserGroup.newInstance({});
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean = true): Promise<boolean>
    {
        // Save the model and record the result
        let result = false;
        try
        {
            comp.app.domain.settings.logger.info(`Creating User Group: ${this.userGroup.name}`);
            this.userGroup = await comp.app.domain.userGroups.createUserGroup(Models.UserGroupCreationRequest.newInstance(this.userGroup));
            comp.app.domain.settings.logger.info(`User Saved: ${JSON.stringify(this.userGroup)}`);
            result = true;
        }
        catch (e)
        {}

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/user-groups/user-group", [this.userGroup.sysId]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            comp.app.domain.settings.logger.info(`Saving User: ${this.userGroup.sysId}`);
            this.userGroup = await comp.app.domain.userGroups.saveUserGroup(this.userGroup);
            comp.app.domain.settings.logger.info(`User Saved: ${JSON.stringify(this.userGroup)}`);
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            let roles  = await comp.app.domain.settings.getRolesList();
            let groups = await comp.app.domain.settings.getGroupsList();

            this.roleOptions  = [];
            this.groupOptions = [];

            for (let role of roles)
            {
                this.roleOptions.push(new ControlOption<Models.RecordIdentity>(role.getIdentity(), role.model.name));
            }

            for (let group of groups)
            {
                this.groupOptions.push(new ControlOption<Models.RecordIdentity>(group.getIdentity(), group.model.name));
            }

            this.sync();

            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    private sync()
    {
        if (this.userGroup?.roles)
        {
            this.userGroup.roles = this.userGroup.roles
                                       .map((role) =>
                                            {
                                                let match = this.roleOptions.find(
                                                    option => option.id.sysId === role.sysId);
                                                return match ? match.id : null;
                                            })
                                       .filter(
                                           role => !!role);
        }

        if (this.userGroup?.subGroups)
        {
            this.userGroup.subGroups = this.userGroup.subGroups
                                           .map((group) =>
                                                {
                                                    let match = this.groupOptions.find(
                                                        option => option.id.sysId === group.sysId);
                                                    return match ? match.id : null;
                                                })
                                           .filter(
                                               group => !!group);
        }
    }
}
