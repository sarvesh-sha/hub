import {Component, Injector, Input} from "@angular/core";

import {AssetExtended, AssetsService, HostExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, DatatableSelectionManager, FilterDebouncer, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-hosts-list",
               templateUrl: "./hosts-list.component.html"
           })
export class HostsListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, HostExtended, HostFlat>
{
    private m_filterDebouncer: FilterDebouncer<HostFilter, Models.HostFilterRequest>;

    @Input() withFiltering: boolean = false;

    @Input()
    public set filterText(value: string)
    {
        this.m_filterDebouncer.setProperty("filterText", value);
    }

    public get filterText(): string
    {
        return this.m_filterDebouncer.getProperty("filterText");
    }

    @Input()
    public set filters(value: Models.HostFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.HostFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    //--//

    @Input()
    public set hosts(value: string[])
    {
        this.m_filterDebouncer.setProperty("hosts", value);
    }

    public get hosts(): string[]
    {
        return this.m_filterDebouncer.getProperty("hosts");
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, HostFlat>;

    selectionManager: DatatableSelectionManager<Models.RecordIdentity, HostFlat, string>;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new HostFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<HostFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters = Models.HostFilterRequest.deepClone(baseFilters) || new Models.HostFilterRequest();

                                                         if (state.hosts)
                                                         {
                                                             filters.sysIds = state.hosts;
                                                         }

                                                         if (state.filterText?.length > 1)
                                                         {
                                                             filters.likeFilter = state.filterText;
                                                         }
                                                         else
                                                         {
                                                             filters.likeFilter = undefined;
                                                         }

                                                         return filters;
                                                     },
                                                     (filtersChanged: boolean) =>
                                                     {
                                                         if (filtersChanged)
                                                         {
                                                             this.table.resetPagination();
                                                         }

                                                         this.table.refreshData();
                                                     });

        this.table            = this.newTableWithAutoRefresh(this.app.domain.assets, this);
        this.selectionManager = this.table.enableSimpleSelection((key) => key.sysId, (value) => value.extended.model.sysId);
    }

    getItemName(): string { return "Hosts"; }

    getTableConfigId(): string { return "hosts"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);

        if (AssetsService.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.assets.getCount(new Models.HostFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Hosts`;
        }

        return response.results;
    }

    getPage(offset: number,
            limit: number): Promise<HostExtended[]>
    {
        return this.app.domain.assets.getTypedPageFromTable(HostExtended, this.table, offset, limit);
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

class HostFilter
{
    hosts: string[];
    filterText: string;
}

class HostFlat
{
    extended: HostExtended;
}
