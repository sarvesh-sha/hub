import {Injectable} from "@angular/core";

import {ReportError} from "app/app.service";
import {ApiService} from "app/services/domain/api.service";
import {RoleExtended, RolesService} from "app/services/domain/roles.service";
import {UserGroupExtended, UserGroupsService} from "app/services/domain/user-groups.service";
import {UsersService} from "app/services/domain/users.service";

import * as Models from "app/services/proxy/model/models";

import {ErrorService} from "framework/services/error.service";
import {Logger, LoggingService} from "framework/services/logging.service";
import {Memoizer} from "framework/utils/memoizers";

@Injectable()
export class SettingsService
{
    private static readonly c_sysPref_InstanceConfiguration: string = "sys_instanceConfiguration";

    readonly logger: Logger;

    constructor(private api: ApiService,
                private errors: ErrorService,
                private users: UsersService,
                private groups: UserGroupsService,
                private roles: RolesService,
                private logService: LoggingService)
    {
        this.logger = logService.getLogger(SettingsService);
    }

    /**
     * Get the users list.
     */
    @ReportError
    public async getUsersList(includeLdap: boolean): Promise<Models.User[]>
    {
        let users = await this.api.users.getAll();

        return users.filter((u) => includeLdap || u.origin === null);
    }

    /**
     * Get users - excluding the current user.
     */
    async getOtherUsersList(): Promise<Models.User[]>
    {
        let userID: string;
        if (this.users.isAuthenticated)
        {
            let user = this.users.user;
            userID   = user.sysId;
        }

        let result = await this.getUsersList(true);
        return result.filter(
            user => user.sysId != userID);
    }

    /**
     * Get the user by ID.
     */
    @ReportError
    public getUserByID(id: string): Promise<Models.User>
    {
        return this.api.users.get(id);
    }

    /**
     * Get the groups list.
     */
    @ReportError
    public getGroupsList(): Promise<UserGroupExtended[]>
    {
        return this.groups.getExtendedAll();
    }

    /**
     * Get the roles list.
     */
    @ReportError
    public getRolesList(): Promise<RoleExtended[]>
    {
        return this.roles.getExtendedAll();
    }

    //--//

    /**
     * Get the preference nested under the target path.
     * @param path
     */
    getPreferenceSubkeys(path: string): Promise<string[]>
    {
        return this.api.systemPreferences.listSubKeys(path);
    }

    /**
     * Get the preference values by their path.
     * @param path
     */
    getPreferenceValues(path: string): Promise<string[]>
    {
        return this.api.systemPreferences.listValues(path);
    }

    @Memoizer
    getInstanceConfiguration(): Promise<Models.InstanceConfiguration>
    {
        return this.getTypedPreference<Models.InstanceConfiguration>(null, SettingsService.c_sysPref_InstanceConfiguration, Models.InstanceConfiguration.fixupPrototype);
    }

    @Memoizer
    async isCRE(): Promise<boolean>
    {
        let instanceConfig = await this.getInstanceConfiguration();
        return instanceConfig instanceof Models.InstanceConfigurationForCRE;
    }

    @Memoizer
    async isTransportation(): Promise<boolean>
    {
        let instanceConfig = await this.getInstanceConfiguration();
        return instanceConfig instanceof Models.InstanceConfigurationForTransportation;
    }

    /**
     * Get a preference by its key.
     * @param path
     * @param name
     */
    async getPreference(path: string,
                        name: string): Promise<string>
    {
        let res = await this.api.systemPreferences.getValue(path, name);
        return res ? res.value : null;
    }

    /**
     * Get a preference value by its key.
     * @param path
     * @param name
     * @param fixup
     */
    async getTypedPreference<T>(path: string,
                                name: string,
                                fixup: (value: T) => void): Promise<T>
    {
        let value = await this.getPreference(path, name);
        if (value)
        {
            let valueTyped = <T>JSON.parse(value);
            if (valueTyped) fixup(valueTyped);
            return valueTyped;
        }

        return null;
    }

    /**
     * Checks the value of a typed preference entry.
     * @param path
     * @param name
     * @param value
     * @param fixup
     */
    async checkTypedPreferenceValue<T>(path: string,
                                       name: string,
                                       value: string,
                                       fixup: (value: T) => void): Promise<T>
    {
        if (await this.api.systemPreferences.checkValueFormat(path, name, value))
        {
            let valueTyped = <T>JSON.parse(value);
            if (valueTyped) fixup(valueTyped);
            return valueTyped;
        }

        return null;
    }

    /**
     * Set a preference value.
     * @param path
     * @param name
     * @param value
     */
    setPreference(path: string,
                  name: string,
                  value: string): Promise<string>
    {
        return this.api.systemPreferences.setValue(path, name, value);
    }

    /**
     * Set a preference value.
     * @param path
     * @param name
     * @param value
     */
    setTypedPreference<T>(path: string,
                          name: string,
                          value: T): Promise<string>
    {
        return this.api.systemPreferences.setValue(path, name, JSON.stringify(value));
    }

    /**
     * Delete a preference value.
     * @param path
     * @param name
     */
    removePreference(path: string,
                     name: string): Promise<boolean>
    {
        return this.api.systemPreferences.removeValue(path, name);
    }
}
