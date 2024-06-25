import {Component, Injector, Input} from "@angular/core";

import {AssetExtended, AssetsService, GatewayExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, FilterDebouncer, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-gateways-list",
               templateUrl: "./gateways-list.component.html"
           })
export class GatewaysListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, GatewayExtended, GatewayFlat>
{
    private m_filterDebouncer: FilterDebouncer<GatewayFilter, Models.GatewayFilterRequest>;

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
    public set filters(value: Models.GatewayFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.GatewayFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, GatewayFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new GatewayFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<GatewayFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters        = Models.GatewayFilterRequest.deepClone(baseFilters) || new Models.GatewayFilterRequest();
                                                         filters.likeFilter = state.filterText;

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

        this.table = this.newTableWithAutoRefresh(this.app.domain.assets, this);
    }

    getItemName(): string { return "Gateways"; }

    getTableConfigId(): string { return "gateways"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);

        if (AssetsService.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.assets.getCount(new Models.GatewayFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Gateways`;
        }

        return response.results;
    }

    getPage(offset: number,
            limit: number): Promise<GatewayExtended[]>
    {
        return this.app.domain.assets.getTypedPageFromTable(GatewayExtended, this.table, offset, limit);
    }

    async transform(rows: GatewayExtended[]): Promise<GatewayFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new GatewayFlat();
                                       result.extended = row;

                                       let loc             = await row.getLocation();
                                       result.locationName = loc ? await loc.getRecursiveName() : "<unknown>";

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: GatewayFlat)
    {
        this.app.ui.navigation.go("/gateways/gateway", [item.extended.model.sysId]);
    }
}

class GatewayFilter
{
    filterText: string;
}

class GatewayFlat
{
    extended: GatewayExtended;

    locationName: string;
}
