import {Component, Injector} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {ColumnConfiguration, DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {NumberWithSeparatorsPipe} from "framework/ui/formatting/string-format.pipe";

@Component({
               selector   : "o3-statistics-datagram-sessions-page",
               templateUrl: "./statistics-datagram-sessions-page.component.html"
           })
export class StatisticsDatagramSessionsPageComponent extends SharedSvc.BaseApplicationComponent
{
    sessionsDetails: SessionsDetails;

    //--//

    constructor(inj: Injector)
    {
        super(inj);
    }

    public ngAfterViewInit(): void
    {
        super.ngAfterViewInit();

        this.fetchSessions();
    }

    async fetchSessions()
    {
        let rawSessions = await this.app.domain.apis.adminTasks.getDatagramSessions();

        let sessions = rawSessions.map((raw) => new SessionDetails(raw));

        this.sessionsDetails = new SessionsDetails(this, sessions);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}

class SessionsDetails implements IDatatableDataProvider<SessionDetails, SessionDetails, SessionDetails>
{
    private m_filter: string;

    get filter(): string
    {
        return this.m_filter;
    }

    set filter(value: string)
    {
        this.m_filter = value;

        this.table.refreshData();
    }

    table: DatatableManager<SessionDetails, SessionDetails, SessionDetails>;

    constructor(private component: StatisticsDatagramSessionsPageComponent,
                public readonly sessions: SessionDetails[])
    {
        this.table = new DatatableManager<SessionDetails, SessionDetails, SessionDetails>(this, () => null);
    }

    //--//

    public wasDestroyed(): boolean
    {
        return this.component.wasDestroyed();
    }

    public detectChanges()
    {
        this.component.detectChanges();
    }

    //--//

    public getTableConfigId(): string
    {
        return null;
    }

    async setColumnConfigs(configs: ColumnConfiguration[]): Promise<boolean>
    {
        return true;
    }

    async getColumnConfigs(): Promise<ColumnConfiguration[]>
    {
        return null;
    }

    public getItemName(): string
    {
        return "Sessions";
    }

    public async getList(): Promise<SessionDetails[]>
    {
        return this.sessions.filter((hostDetail) =>
                                    {
                                        if (this.m_filter)
                                        {
                                            if (this.contains(hostDetail.model.displayName))
                                            {
                                                return true;
                                            }

                                            return false;
                                        }

                                        return true;
                                    });
    }

    public async getPage(offset: number,
                         limit: number): Promise<SessionDetails[]>
    {
        return this.table.slicePage(offset, limit);
    }

    public async itemClicked(columnId: string,
                             item: SessionDetails)
    {
        let agent = await this.component.app.domain.deploymentAgents.getExtendedById(item.model.contextSysId);
        if (agent)
        {
            let host = await agent.getOwningDeployment();

            this.component.app.ui.navigation.go("/deployments", [
                "item",
                host.model.sysId,
                "agent",
                agent.model.sysId
            ]);
        }
    }

    public async transform(rows: SessionDetails[]): Promise<SessionDetails[]>
    {
        return rows;
    }

    private contains(val: string)
    {
        return val && val.toLowerCase()
                         .indexOf(this.m_filter.toLowerCase()) >= 0;
    }
}

class SessionDetails
{
    stats: string;

    constructor(public readonly model: Models.MessageBusDatagramSession)
    {
        let s = model.statistics;
        if (s)
        {
            this.stats = `${s.sessions} sessions` +
                         ` # TX: ${NumberWithSeparatorsPipe.format(s.packetTxBytes)} bytes (${NumberWithSeparatorsPipe.format(s.packetTxBytesResent)} resent) over ${NumberWithSeparatorsPipe.format(s.packetTx)} packets and ${NumberWithSeparatorsPipe.format(s.messageTx)} messages` +
                         ` # RX: ${NumberWithSeparatorsPipe.format(s.packetRxBytes)} bytes (${NumberWithSeparatorsPipe.format(s.packetRxBytesResent)} resent) over ${NumberWithSeparatorsPipe.format(s.packetRx)} packets and ${NumberWithSeparatorsPipe.format(s.messageRx)} messages`;
        }
    }
}
