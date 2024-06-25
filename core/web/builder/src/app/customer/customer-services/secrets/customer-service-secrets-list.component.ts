import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceSecretExtended} from "app/services/domain/customer-service-secrets.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";

import * as Models from "app/services/proxy/model/models";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-customer-service-secrets-list",
               templateUrl: "./customer-service-secrets-list.component.html"
           })
export class CustomerServiceSecretsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, CustomerServiceSecretExtended, CustomerServiceSecretExtended>
{
    private m_extended: CustomerServiceExtended;

    public get extended(): CustomerServiceExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: CustomerServiceExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, CustomerServiceSecretExtended, CustomerServiceSecretExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.customerServiceSecrets, this);
    }

    getItemName(): string
    {
        return "Customer Service Secrets";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.secrets)
        {
            return [];
        }

        return this.extended.model.secrets;
    }

    getPage(offset: number,
            limit: number): Promise<CustomerServiceSecretExtended[]>
    {
        return this.app.domain.customerServiceSecrets.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: CustomerServiceSecretExtended[]): Promise<CustomerServiceSecretExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: CustomerServiceSecretExtended)
    {
        this.app.ui.navigation.push([
                                        "secret",
                                        item.model.sysId
                                    ]);
    }
}
