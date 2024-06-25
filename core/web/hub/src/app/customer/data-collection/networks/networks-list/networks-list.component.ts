import {Component, Injector, Input} from "@angular/core";

import {GatewaySelectionDialogComponent} from "app/customer/data-collection/gateways/gateway-selection-dialog.component";

import {AssetExtended, AssetsService, NetworkExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, DatatableSelectionManager, FilterDebouncer, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-networks-list",
               templateUrl: "./networks-list.component.html"
           })
export class NetworksListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, NetworkExtended, NetworkFlat>
{
    private m_filterDebouncer: FilterDebouncer<NetworkFilter, Models.NetworkFilterRequest>;

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
    public set filters(value: Models.NetworkFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.NetworkFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    @Input()
    public set networks(value: string[])
    {
        this.m_filterDebouncer.setProperty("networks", value);
    }

    public get networks(): string[]
    {
        return this.m_filterDebouncer.getProperty("networks");
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, NetworkFlat>;

    selectionManager: DatatableSelectionManager<Models.RecordIdentity, NetworkFlat, string>;

    constructor(inj: Injector)
    {
        super(inj);


        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new NetworkFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<NetworkFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters = Models.NetworkFilterRequest.deepClone(baseFilters) || new Models.NetworkFilterRequest();

                                                         if (state.networks)
                                                         {
                                                             filters.sysIds = state.networks;
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

    getItemName(): string { return "Networks"; }

    getTableConfigId(): string { return "networks"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);

        if (AssetsService.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.assets.getCount(new Models.NetworkFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Networks`;
        }

        return response.results;
    }

    getPage(offset: number,
            limit: number): Promise<NetworkExtended[]>
    {
        return this.app.domain.assets.getTypedPageFromTable(NetworkExtended, this.table, offset, limit);
    }

    async transform(rows: NetworkExtended[]): Promise<NetworkFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new NetworkFlat();
                                       result.extended = row;

                                       let loc             = await row.getLocation();
                                       result.locationName = loc ? await loc.getRecursiveName() : "<unknown>";

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: NetworkFlat)
    {
        this.app.ui.navigation.go("/networks/network", [item.extended.model.sysId]);
    }

    contextMenu(event: DatatableContextMenuEvent<NetworkFlat>)
    {
        let selectionIds = this.table.selectionManager?.selection;
        if (selectionIds?.size > 0)
        {
            event.root.addItem("Transfer to Gateway", async () =>
            {
                let dialogService = await GatewaySelectionDialogComponent.open(this, "select", "Select");
                if (dialogService != null)
                {
                    for (let selectionId of selectionIds)
                    {
                        let network = await this.app.domain.assets.getTypedExtendedById(NetworkExtended, selectionId);
                        await dialogService.gateway.bindNetwork(network, false, false, false);
                    }
                }
            });
        }
    }
}

class NetworkFilter
{
    networks: string[];
    filterText: string;
}

class NetworkFlat
{
    extended: NetworkExtended;

    locationName: string;
}
