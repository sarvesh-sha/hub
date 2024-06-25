import {Injectable} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {SettingsService} from "app/services/domain/settings.service";
import {ControlOption} from "framework/ui/control-option";

@Injectable()
export class ControlBindingService
{
    constructor(private settingsService: SettingsService)
    {

    }

    async getRoles(): Promise<ControlOption<string>[]>
    {
        let roles = await this.settingsService.getRolesList();
        return SharedSvc.BaseService.mapOptions(roles, (role) =>
        {
            if (!role) return null;

            let option   = new ControlOption<string>();
            option.id    = role.model.sysId;
            option.label = role.model.displayName;
            return option;
        });
    }

    async getUsers(): Promise<ControlOption<string>[]>
    {
        let users = await this.settingsService.getUsersList();
        return SharedSvc.BaseService.mapOptions(users, (user) =>
        {
            if (!user) return null;

            let option   = new ControlOption<string>();
            option.id    = user.sysId;
            option.label = `${user.firstName} ${user.lastName}`;
            return option;
        });
    }
}
