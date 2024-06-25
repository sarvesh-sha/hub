import {Component, Injector, Input} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import {CustomerSharedUserExtended} from "app/services/domain/customer-shared-users.service";
import {CustomerExtended} from "app/services/domain/customers.service";
import {RoleExtended} from "app/services/domain/roles.service";
import {UserExtended} from "app/services/domain/user-management.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-customer-shared-users-list",
               templateUrl: "./customer-shared-users-list.component.html"
           })
export class CustomerSharedUsersListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, CustomerSharedUserExtended, CustomerSharedUserExtended>
{
    private m_extended: CustomerExtended;

    public get extended(): CustomerExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: CustomerExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, CustomerSharedUserExtended, CustomerSharedUserExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.customerSharedUsers, this);
    }

    getItemName(): string
    {
        return "Customer Shared Users";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.sharedUsers)
        {
            return [];
        }

        return this.extended.model.sharedUsers;
    }

    getPage(offset: number,
            limit: number): Promise<CustomerSharedUserExtended[]>
    {
        return this.app.domain.customerSharedUsers.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: CustomerSharedUserExtended[]): Promise<CustomerSharedUserExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: CustomerSharedUserExtended)
    {
        this.app.ui.navigation.push([
                                        "user",
                                        item.model.sysId
                                    ]);
    }
}
