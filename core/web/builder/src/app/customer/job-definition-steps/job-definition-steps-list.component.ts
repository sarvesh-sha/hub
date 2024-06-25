import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {JobDefinitionStepExtended} from "app/services/domain/job-definition-steps.service";
import {JobDefinitionExtended} from "app/services/domain/job-definitions.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-job-definition-steps-list",
               templateUrl: "./job-definition-steps-list.component.html"
           })
export class JobDefinitionStepsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, JobDefinitionStepExtended, JobDefinitionStepExtended>
{
    private m_extended: JobDefinitionExtended;

    public get extended(): JobDefinitionExtended
    {
        return this.m_extended;
    }

    @Input() public set extended(value: JobDefinitionExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, JobDefinitionStepExtended, JobDefinitionStepExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.jobDefinitionSteps, this);
    }

    getItemName(): string
    {
        return "Job Definition Steps";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.steps)
        {
            return [];
        }

        return this.extended.model.steps;
    }

    getPage(offset: number,
            limit: number): Promise<JobDefinitionStepExtended[]>
    {
        return this.app.domain.jobDefinitionSteps.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: JobDefinitionStepExtended[]): Promise<JobDefinitionStepExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: JobDefinitionStepExtended)
    {
        this.app.ui.navigation.push([
                                        "step",
                                        item.model.sysId
                                    ]);
    }
}

