import {Component, Injector, Input} from "@angular/core";
import {ReportError} from "app/app.service";

import * as SharedSvc from "app/services/domain/base.service";
import {CustomerServiceBackupExtended} from "app/services/domain/customer-service-backups.service";
import {CustomerServiceExtended} from "app/services/domain/customer-services.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, DatatableSelectionManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-customer-service-backups-list",
               templateUrl: "./customer-service-backups-list.component.html"
           })
export class CustomerServiceBackupsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<CustomerServiceBackupExtended, CustomerServiceBackupExtended, CustomerServiceBackupExtended>
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

    table: DatatableManager<CustomerServiceBackupExtended, CustomerServiceBackupExtended, CustomerServiceBackupExtended>;
    private tableSelectionManager: BackupSelectionManager;

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.table                  = this.newTableWithAutoRefresh(this.app.domain.customerServiceBackups, this);
        this.tableSelectionManager  = new BackupSelectionManager(this.table);
        this.table.selectionManager = this.tableSelectionManager;
        this.table.defaultLimit     = 25;
    }

    getItemName(): string
    {
        return "Customer Service Backups";
    }

    async getList(): Promise<CustomerServiceBackupExtended[]>
    {
        if (!this.m_extended)
        {
            return [];
        }

        return this.app.domain.customerServiceBackups.getExtendedBatch(this.m_extended.model.backups);
    }

    async getPage(offset: number,
                  limit: number): Promise<CustomerServiceBackupExtended[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: CustomerServiceBackupExtended[]): Promise<CustomerServiceBackupExtended[]>
    {
        return rows;
    }

    async itemClicked(columnId: string,
                      item: CustomerServiceBackupExtended)
    {
        let cust = await this.extended.getOwningCustomer();

        this.app.ui.navigation.go("/customers/item", [
            cust.model.sysId,
            "service",
            this.extended.model.sysId,
            "backup",
            item.model.sysId
        ]);
    }

    anyBackupSelected(): boolean
    {
        return this.tableSelectionManager.selection.size > 0;
    }

    @ReportError
    async deleteBackups()
    {
        let candidates = this.tableSelectionManager.selection;

        if (candidates.size > 0 && await this.confirmOperation(`Check Yes to delete ${candidates.size} backup(s).`))
        {
            this.table.disableRefreshWhileProcessing(async () =>
                                                     {
                                                         for (let backup of candidates)
                                                         {
                                                             await backup.remove();
                                                         }

                                                         this.m_extended = await this.m_extended.refresh<CustomerServiceExtended>();
                                                     });
        }
    }
}

export class BackupSelectionManager extends DatatableSelectionManager<CustomerServiceBackupExtended, CustomerServiceBackupExtended, CustomerServiceBackupExtended>
{
    constructor(table: DatatableManager<CustomerServiceBackupExtended, any, CustomerServiceBackupExtended>)
    {
        super(table, false, false, (k) => k, (v) => v);
    }

    public handleItemChecked(row: CustomerServiceBackupExtended): void
    {
        let wasSelected = this.isChecked(row);

        this.setChecked(row, !wasSelected);

        if (row.model.trigger == Models.BackupKind.Upgrade)
        {
            for (let row2 of this.table.slice(this.table.indexOfKey(row) + 1, this.table.count))
            {
                if (this.isChecked(row2) != wasSelected)
                {
                    break;
                }

                this.setChecked(row2, !wasSelected);
            }
        }
    }
}
