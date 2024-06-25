import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";
import {RepositoryCheckoutExtended} from "app/services/domain/repository-checkouts.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-repository-checkouts-list",
               templateUrl: "./repository-checkouts-list.component.html"
           })
export class RepositoryCheckoutsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, RepositoryCheckoutExtended, RepositoryCheckoutExtended>
{
    private m_extended: RepositoryExtended;

    public get extended(): RepositoryExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: RepositoryExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, RepositoryCheckoutExtended, RepositoryCheckoutExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.repositoryCheckouts, this);
    }

    getItemName(): string
    {
        return "Repository Checkouts";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.checkouts)
        {
            return [];
        }

        return this.extended.model.checkouts;
    }

    getPage(offset: number,
            limit: number): Promise<RepositoryCheckoutExtended[]>
    {
        return this.app.domain.repositoryCheckouts.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: RepositoryCheckoutExtended[]): Promise<RepositoryCheckoutExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: RepositoryCheckoutExtended)
    {
        this.app.ui.navigation.push([
                                        "checkout",
                                        item.model.sysId
                                    ]);
    }
}

