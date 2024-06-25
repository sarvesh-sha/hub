import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {DeploymentAgentExtended} from "app/services/domain/deployment-agents.service";
import {DeploymentHostExtended} from "app/services/domain/deployment-hosts.service";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

@Component({
               selector   : "o3-deployment-agents-list",
               templateUrl: "./deployment-agents-list.component.html"
           })
export class DeploymentAgentsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<DeploymentAgentExtended, DeploymentAgentExtended, AgentInstance>
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

    table: DatatableManager<DeploymentAgentExtended, DeploymentAgentExtended, AgentInstance>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table = this.newTableWithAutoRefresh(this.app.domain.deploymentAgents, this);
    }

    getItemName(): string
    {
        return "Deployment Agents";
    }

    async getList(): Promise<DeploymentAgentExtended[]>
    {
        if (!this.extended)
        {
            return [];
        }

        return await this.extended.getAgents();
    }

    async getPage(offset: number,
                  limit: number): Promise<DeploymentAgentExtended[]>
    {
        return this.table.slicePage(offset, limit);
    }

    async transform(rows: DeploymentAgentExtended[]): Promise<AgentInstance[]>
    {
        let results = [];

        for (let row of rows)
        {
            let res        = new AgentInstance();
            res.ext        = row;
            res.statusText = await row.getStatusDesc();
            results.push(res);
        }

        return results;
    }

    itemClicked(columnId: string,
                item: AgentInstance)
    {
        this.app.ui.navigation.push([
                                        "agent",
                                        item.ext.model.sysId
                                    ]);
    }
}

class AgentInstance
{
    ext: DeploymentAgentExtended;

    statusText: string;
}
