import {Component, Injector} from "@angular/core";
import {ReportError} from "app/app.service";
import {UserGroupsWizardState, UserGroupWizardDialogComponent} from "app/customer/configuration/user-groups/wizard/user-group-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserGroupExtended} from "app/services/domain/user-groups.service";
import * as Models from "app/services/proxy/model/models";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

@Component({
               selector   : "o3-user-group-list-page",
               templateUrl: "./user-group-list-page.component.html"
           })
export class UserGroupListPageComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<string, UserGroupExtended, UserGroupDecoded>
{
    table: DatatableManager<string, UserGroupExtended, UserGroupDecoded>;
    roles: RoleExtended[];
    users: Lookup<UserGroupDecoded> = {};

    constructor(inj: Injector)
    {
        super(inj);

        this.table       = this.newTableWithAutoRefresh(this.app.domain.userGroups, this);
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
        return "User Groups";
    }

    async getList(): Promise<string[]>
    {
        this.users = {};
        let keys   = [];

        let userGroups = await this.app.domain.settings.getGroupsList();
        for (let userGroup of userGroups)
        {
            let userDecoded = new UserGroupDecoded();
            userDecoded.ext = userGroup;

            let roles: string[] = [];

            for (let role of userGroup.model.roles)
            {
                roles.push(this.getRoleDisplayName(role));
            }

            roles.sort((a,
                        b) => UtilsService.compareStrings(a, b, true));

            userDecoded.rolesText = roles.join(" / ");

            this.users[userGroup.model.sysId] = userDecoded;
            keys.push(userGroup.model.sysId);
        }

        if (this.table.sort && this.table.sort.length)
        {
            let property = this.table.sort[0].prop;
            keys.sort((a,
                       b) =>
                      {
                          let userA = this.users[a];
                          let userB = this.users[b];
                          let valA: string;
                          let valB: string;

                          switch (property)
                          {
                              case "name":
                                  valA = userA.ext.model.name;
                                  valB = userB.ext.model.name;
                                  break;

                              case "description":
                                  valA = userA.ext.model.description;
                                  valB = userB.ext.model.description;
                                  break;

                              case "rolesText":
                                  valA = userA.rolesText;
                                  valB = userB.rolesText;
                                  break;
                          }

                          return UtilsService.compareStrings(valA, valB, this.table.sort[0].dir == "asc");
                      });
        }

        return keys;
    }

    async getPage(offset: number,
                  limit: number): Promise<UserGroupExtended[]>
    {
        return this.table.slicePage(offset, limit)
                   .map((key) => this.users[key].ext);
    }

    async transform(rows: UserGroupExtended[]): Promise<UserGroupDecoded[]>
    {
        return rows.map((ext) => this.users[ext.model.sysId]);
    }

    itemClicked(columnId: string,
                details: UserGroupDecoded)
    {
        this.app.ui.navigation.go("/configuration/user-groups/user-group", [details.ext.model.sysId]);
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

    async new()
    {
        await UserGroupWizardDialogComponent.open(new UserGroupsWizardState(), this);
    }

    @ReportError
    async import()
    {
        let result = await ImportDialogComponent.open(this, "Import User Groups", {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string) => JSON.parse(contents)
        });

        if (result)
        {
            this.app.domain.apis.userGroups.batchImport(result);
        }
    }

    @ReportError
    async export()
    {
        let batch = await this.app.domain.apis.userGroups.batchExport();

        let timestamp = MomentHelper.fileNameFormat();

        DownloadDialogComponent.open<Models.UserGroupImportExport>(this, "Export User Groups", `user_groups_${timestamp}.json`, batch);
    }
}

class UserGroupDecoded
{
    ext: UserGroupExtended;
    rolesText: string;
}
