import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";
import {DeploymentTaskExtended} from "app/services/domain/deployment-tasks.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-deployment-tasks-list",
               templateUrl: "./deployment-tasks-list.component.html"
           })
export class DeploymentTasksListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<DeploymentTaskExtended, DeploymentTaskExtended, TaskDetail>
{
    private m_extended: DeploymentHostExtended;

    public get extended(): DeploymentHostExtended
    {
        return this.m_extended;
    }

    @Input()
    public set extended(value: DeploymentHostExtended)
    {
        this.m_extended = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<DeploymentTaskExtended, DeploymentTaskExtended, TaskDetail>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.deploymentTasks, this, 100, 1000, 100);
    }

    getItemName(): string
    {
        return "deployment Tasks";
    }

    async getList(): Promise<DeploymentTaskExtended[]>
    {
        if (!this.extended)
        {
            return [];
        }

        return await this.extended.getTasks();
    }

    async getPage(offset: number,
                  limit: number): Promise<DeploymentTaskExtended[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: DeploymentTaskExtended[]): Promise<TaskDetail[]>
    {
        return rows.map((row) =>
                        {
                            let details      = new TaskDetail();
                            details.extended = row;
                            return details;
                        });
    }

    itemClicked(columnId: string,
                item: TaskDetail)
    {
        this.app.ui.navigation.push([
                                        "task",
                                        item.extended.model.sysId
                                    ]);
    }
}

class TaskDetail
{
    extended: DeploymentTaskExtended;
}
