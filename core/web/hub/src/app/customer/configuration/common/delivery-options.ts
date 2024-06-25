import {AppDomainContext} from "app/services/domain/domain.module";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserGroupExtended} from "app/services/domain/user-groups.service";
import {UserExtended} from "app/services/domain/user-management.service";
import * as Models from "app/services/proxy/model/models";

import {Memoizer, ResetMemoizers} from "framework/utils/memoizers";

export class DeliveryOptionsExtended
{
    selectedUsers: string[]  = [];
    selectedGroups: string[] = [];
    selectedRoles: string[]  = [];

    constructor(private m_model: Models.DeliveryOptions = new Models.DeliveryOptions(),
                private m_domain: AppDomainContext,
                initCurrentUser: boolean)
    {
        if (!m_model.users?.length)
        {
            m_model.users = [];

            if (m_domain && initCurrentUser)
            {
                m_model.users.push(UserExtended.newIdentity(m_domain.users.user.sysId));
            }
        }

        if (!m_model.groups)
        {
            m_model.groups = [];
        }

        if (!m_model.roles)
        {
            m_model.roles = [];
        }

        this.selectedUsers  = m_model.users.map((id) => id.sysId);
        this.selectedGroups = m_model.groups.map((id) => id.sysId);
        this.selectedRoles  = m_model.roles.map((id) => id.sysId);
    }

    get model()
    {
        return this.m_model;
    }

    @ResetMemoizers
    refresh()
    {
        this.m_model.users  = this.selectedUsers.map((id) => UserExtended.newIdentity(id));
        this.m_model.groups = this.selectedGroups.map((id) => UserGroupExtended.newIdentity(id));
        this.m_model.roles  = this.selectedRoles.map((id) => RoleExtended.newIdentity(id));
    }

    isValid(): boolean
    {
        return this.m_model && this.m_model.users && this.m_model.groups && this.m_model.roles && (this.m_model.users.length > 0 || this.m_model.groups.length > 0 || this.m_model.roles.length > 0);
    }

    removeUser(sysId: string)
    {
        this.selectedUsers = this.selectedUsers.filter((u) => u !== sysId);
        this.refresh();
    }

    removeGroup(sysId: string)
    {
        this.selectedGroups = this.selectedGroups.filter((u) => u !== sysId);
        this.refresh();
    }

    removeRole(sysId: string)
    {
        this.selectedRoles = this.selectedRoles.filter((r) => r !== sysId);
        this.refresh();
    }

    getDisplayText(readonly: boolean = false): string
    {
        let parts: string[] = [];

        this.appendText(parts, this.m_model.users, "user");
        this.appendText(parts, this.m_model.groups, "group");
        this.appendText(parts, this.m_model.roles, "role");

        if (parts.length > 0)
        {
            return parts.join(", ");
        }
        else if (readonly)
        {
            return "No users or roles selected";
        }
        else
        {
            return "Select users or roles";
        }
    }

    @Memoizer
    async getResolvedText(): Promise<string>
    {
        let parts: string[] = [];

        for (let user of this.selectedUsers)
        {
            let model = await this.m_domain.apis.users.get(user);
            if (model)
            {
                parts.push(`${model.firstName} ${model.lastName}`);
            }
        }

        for (let group of this.selectedGroups)
        {
            let model = await this.m_domain.apis.userGroups.get(group);
            if (model)
            {
                parts.push(`Group '${model.name}'`);
            }
        }

        for (let role of this.selectedRoles)
        {
            let model = await this.m_domain.apis.roles.get(role);
            if (model)
            {
                parts.push(`Role '${model.name}'`);
            }
        }

        return parts.join(", ");
    }

    private appendText(parts: string[],
                       values: Array<Models.RecordIdentity>,
                       suffix: string)
    {
        if (values?.length > 0)
        {
            parts.push(`${values.length} ${suffix}${values.length > 1 ? "s" : ""}`);
        }
    }
}
