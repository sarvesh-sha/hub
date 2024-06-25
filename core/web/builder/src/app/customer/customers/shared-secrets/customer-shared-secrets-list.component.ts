import {Component, Injector, Input} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import {CustomerSharedSecretExtended} from "app/services/domain/customer-shared-secrets.service";
import {CustomerExtended} from "app/services/domain/customers.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-customer-shared-secrets-list",
               templateUrl: "./customer-shared-secrets-list.component.html"
           })
export class CustomerSharedSecretsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, CustomerSharedSecretExtended, CustomerSharedSecretExtended>
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

    table: DatatableManager<Models.RecordIdentity, CustomerSharedSecretExtended, CustomerSharedSecretExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.customerSharedSecrets, this);
    }

    getItemName(): string
    {
        return "Customer Shared Secrets";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.sharedSecrets)
        {
            return [];
        }

        return this.extended.model.sharedSecrets;
    }

    getPage(offset: number,
            limit: number): Promise<CustomerSharedSecretExtended[]>
    {
        return this.app.domain.customerSharedSecrets.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: CustomerSharedSecretExtended[]): Promise<CustomerSharedSecretExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: CustomerSharedSecretExtended)
    {
        this.app.ui.navigation.push([
                                        "secret",
                                        item.model.sysId
                                    ]);
    }
}
