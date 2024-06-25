import {Component, Injector, Input} from "@angular/core";

import * as SharedSvc from "app/services/domain/base.service";
import {HostExtended} from "app/services/domain/hosts.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, DatatableSelectionManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-hosts-list",
               templateUrl: "./hosts-list.component.html"
           })
export class HostsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, HostExtended, HostFlat>
{
    private m_hosts: string[];

    public get hosts(): string[]
    {
        return this.m_hosts;
    }

    @Input()
    public set hosts(value: string[])
    {
        this.m_hosts = value;
        this.table.refreshData();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, HostExtended, HostFlat>;

    selectionManager: DatatableSelectionManager<Models.RecordIdentity, HostFlat, string>;

    constructor(inj: Injector)
    {
        super(inj);

        this.table            = this.newTableWithAutoRefresh(this.app.domain.hosts, this);
        this.selectionManager = this.table.enableSimpleSelection((key) => key.sysId, (value) => value.extended.model.sysId);
    }

    getItemName(): string { return "Hosts"; }

    getTableConfigId(): string { return "hosts"; }

    getList(): Promise<Models.RecordIdentity[]>
    {
        return this.app.domain.hosts.getList();
    }

    getPage(offset: number,
            limit: number): Promise<HostExtended[]>
    {
        return this.app.domain.hosts.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: HostExtended[]): Promise<HostFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new HostFlat();
                                       result.extended = row;
                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: HostFlat)
    {
        this.app.ui.navigation.go("/hosts/host", [item.extended.model.sysId]);
    }
}

class HostFlat
{
    extended: HostExtended;
}
