import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {RepositoryExtended} from "app/services/domain/repositories.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-repositories-list",
               templateUrl: "./repositories-list.component.html"
           })
export class RepositoriesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, RepositoryExtended, RepositoryExtended>
{
    table: DatatableManager<Models.RecordIdentity, RepositoryExtended, RepositoryExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.repositories, this);
    }

    getItemName(): string
    {
        return "Repositories";
    }

    getList(): Promise<Models.RecordIdentity[]>
    {
        return this.app.domain.repositories.getList();
    }

    getPage(offset: number,
            limit: number): Promise<RepositoryExtended[]>
    {
        return this.app.domain.repositories.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: RepositoryExtended[]): Promise<RepositoryExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: RepositoryExtended)
    {
        this.app.ui.navigation.push([
                                        "..",
                                        "item",
                                        item.model.sysId
                                    ]);
    }
}

