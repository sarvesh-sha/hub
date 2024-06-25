import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {JobDefinitionExtended} from "app/services/domain/job-definitions.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-job-definitions-list",
               templateUrl: "./job-definitions-list.component.html"
           })
export class JobDefinitionsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, JobDefinitionExtended, JobDefinitionExtended>
{
    table: DatatableManager<Models.RecordIdentity, JobDefinitionExtended, JobDefinitionExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.jobDefinitions, this);
    }

    getItemName(): string
    {
        return "Job Definitions";
    }

    getList(): Promise<Models.RecordIdentity[]>
    {
        return this.app.domain.jobDefinitions.getList();
    }

    getPage(offset: number,
            limit: number): Promise<JobDefinitionExtended[]>
    {
        return this.app.domain.jobDefinitions.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: JobDefinitionExtended[]): Promise<JobDefinitionExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: JobDefinitionExtended)
    {
        this.app.ui.navigation.push([
                                        "..",
                                        "item",
                                        item.model.sysId
                                    ]);
    }
}

