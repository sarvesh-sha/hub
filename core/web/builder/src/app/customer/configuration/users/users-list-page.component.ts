import {Component, Injector} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserExtended} from "app/services/domain/user-management.service";

import * as Models from "app/services/proxy/model/models";
import {UtilsService} from "framework/services/utils.service";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-users-list-page",
               templateUrl: "./users-list-page.component.html"
           })
export class UsersListPageComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<string, UserExtended, UserDecoded>
{
    table: DatatableManager<string, UserExtended, UserDecoded>;
    roles: RoleExtended[];
    users: { [key: string]: UserDecoded } = {};

    constructor(inj: Injector)
    {
        super(inj);

        this.table       = this.newTableWithAutoRefresh(this.app.domain.userManagement, this);
        this.table.limit = 20;
    }

    public getTableConfigId(): string { return "users"; }

    async ngAfterViewInit()
    {
        super.ngAfterViewInit();

        this.roles = await this.app.domain.settings.getRolesList();
        this.table.refreshData();
    }

    getItemName(): string
    {
        return "Users";
    }

    async getList(): Promise<string[]>
    {
        this.users = {};
        let keys   = [];

        let users = await this.app.domain.settings.getUsersList();
        for (let user of users)
        {
            let userDecoded = new UserDecoded();
            userDecoded.ext = this.app.domain.userManagement.wrapModel(user);

            let roles: string[] = [];

            for (let role of user.roles)
            {
                roles.push(this.getRoleDisplayName(role));
            }

            roles.sort((a,
                        b) => UtilsService.compareStrings(a, b, true));

            userDecoded.rolesText = roles.join(" / ");

            this.users[user.sysId] = userDecoded;
            keys.push(user.sysId);
        }

        if (this.table.sort && this.table.sort.length)
        {
            let property  = this.table.sort[0].prop;
            let direction = this.table.sort[0].dir == "asc" ? 1 : -1;

            keys.sort((a,
                       b) =>
                      {
                          let userA = this.users[a];
                          let userB = this.users[b];
                          let valA: string;
                          let valB: string;

                          switch (property)
                          {
                              case "ext.model.firstName":
                                  valA = userA.ext.model.firstName;
                                  valB = userB.ext.model.firstName;
                                  break;

                              case "ext.model.lastName":
                                  valA = userA.ext.model.lastName;
                                  valB = userB.ext.model.lastName;
                                  break;

                              case "ext.model.emailAddress":
                                  valA = userA.ext.model.emailAddress;
                                  valB = userB.ext.model.emailAddress;
                                  break;

                              case "rolesText":
                                  valA = userA.rolesText;
                                  valB = userB.rolesText;
                                  break;
                          }

                          return UtilsService.compareStrings(valA, valB, true) * direction;
                      });
        }

        return keys;
    }

    async getPage(offset: number,
                  limit: number): Promise<UserExtended[]>
    {
        return this.table.slicePage(offset, limit)
                   .map((key) => this.users[key].ext);
    }

    async transform(rows: UserExtended[]): Promise<UserDecoded[]>
    {
        return rows.map((ext) => this.users[ext.model.sysId]);
    }

    itemClicked(columnId: string,
                details: UserDecoded)
    {
        this.app.ui.navigation.go("/configuration/users/user", [details.ext.model.sysId]);
    }

    getRoleDisplayName(id: Models.RecordIdentity): string
    {
        if (this.roles)
        {
            for (let role of this.roles)
            {
                if (role.model.sysId == id.sysId)
                {
                    return role.model.displayName;
                }
            }
        }

        return "";
    }

    new()
    {
        this.app.ui.navigation.go("/configuration/users/new");
    }
}

class UserDecoded
{
    ext: UserExtended;
    rolesText: string;
}
