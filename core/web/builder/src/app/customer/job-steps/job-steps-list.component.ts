import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {JobStepExtended} from "app/services/domain/job-steps.service";
import {JobExtended} from "app/services/domain/jobs.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-job-steps-list",
               templateUrl: "./job-steps-list.component.html"
           })
export class JobStepsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, JobStepExtended, JobStepExtended>
{
    private m_extended: JobExtended;

    public get extended(): JobExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: JobExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, JobStepExtended, JobStepExtended>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.jobSteps, this);
    }

    getItemName(): string
    {
        return "Job Steps";
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.extended || !this.extended.model.steps)
        {
            return [];
        }

        let steps = [...this.extended.model.steps];
        return steps.reverse();
    }

    getPage(offset: number,
            limit: number): Promise<JobStepExtended[]>
    {
        return this.app.domain.jobSteps.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: JobStepExtended[]): Promise<JobStepExtended[]>
    {
        return rows; // Nothing to do.
    }

    itemClicked(columnId: string,
                item: JobStepExtended)
    {
        this.app.ui.navigation.push([
                                        "..",
                                        "item",
                                        item.model.sysId
                                    ]);
    }
}

