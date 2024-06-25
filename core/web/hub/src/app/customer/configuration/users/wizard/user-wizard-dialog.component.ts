import {Component, Inject, Injector} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";
import {ControlOption} from "framework/ui/control-option";
import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./user-wizard-dialog.component.html"
           })
export class UserWizardDialogComponent extends WizardDialogComponent<UserWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                public apis: ApiService,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: UserWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, UserWizardDialogComponent);
    }
}

export class UserWizardState extends WizardDialogState
{
    user: Models.User;

    roleOptions: ControlOption<Models.RecordIdentity>[];
    groupOptions: ControlOption<Models.RecordIdentity>[];

    password: string;
    passwordConfirmation: string;

    constructor(model?: Models.User)
    {
        super(!model);
        this.user = model ? model : Models.User.newInstance({});
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Escape and return false if passwords do not match
        if (this.password != this.passwordConfirmation)
        {
            throw comp.app.framework.errors.error("PASSWORDS_DONT_MATCH", "Your password confirmation does not match.");
            return false;
        }

        // Save the model and record the result
        let result = false;
        let user: Models.User;
        try
        {
            let request = Models.UserCreationRequest.newInstance({
                                                                     firstName   : this.user.firstName,
                                                                     lastName    : this.user.lastName,
                                                                     emailAddress: this.user.emailAddress,
                                                                     phoneNumber : this.user.phoneNumber,
                                                                     password    : this.password,
                                                                     roles       : this.user.roles
                                                                 });

            let settings = comp.app.domain.settings;
            settings.logger.info(`Creating User: ${this.user.emailAddress}`);
            user = await comp.app.domain.users.createUser(request);
            settings.logger.info(`User Saved: ${JSON.stringify(user)}`);
            result = true;
        }
        catch (e)
        {}

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/users/user", [user.sysId]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            comp.app.domain.settings.logger.info(`Saving User: ${this.user.sysId}`);
            this.user = await comp.app.domain.users.saveUser(this.user);
            comp.app.domain.settings.logger.info(`User Saved: ${JSON.stringify(this.user)}`);
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
        if (this.user?.roles)
        {
            this.user.roles = this.user.roles
                                  .map((role) =>
                                       {
                                           let match = this.roleOptions.find(
                                               option => option.id.sysId === role.sysId);
                                           return match ? match.id : null;
                                       })
                                  .filter(
                                      role => !!role);
        }

        if (this.user?.groups)
        {
            this.user.groups = this.user.groups
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
