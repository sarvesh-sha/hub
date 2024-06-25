import {Component, Injector, Input} from "@angular/core";
import {AssetLocationDialog} from "app/customer/workflows/asset-location-dialog";
import {AssetRenameDialog} from "app/customer/workflows/asset-rename-dialog";

import {AssetExtended, AssetsService, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, FilterDebouncer, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-devices-list",
               templateUrl: "./devices-list.component.html"
           })
export class DevicesListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, DeviceExtended, DeviceFlat>
{
    private m_filterDebouncer: FilterDebouncer<DeviceFilter, Models.DeviceFilterRequest>;

    @Input()
    public set parent(value: Models.Asset)
    {
        this.m_filterDebouncer.setProperty("parent", value);
    }

    public get parent(): Models.Asset
    {
        return this.m_filterDebouncer.getProperty("parent");
    }

    //--//

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
    public set filters(value: Models.DeviceFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.DeviceFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, DeviceFlat>;
    private m_selectionManager: SimpleSelectionManager<Models.RecordIdentity, DeviceFlat, string>;

    private m_isCRE: boolean;

    public get isCRE(): boolean
    {
        return this.m_isCRE;
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new DeviceFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<DeviceFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters        = Models.DeviceFilterRequest.deepClone(baseFilters) || new Models.DeviceFilterRequest();
                                                         filters.likeFilter = state.filterText;

                                                         if (state.parent)
                                                         {
                                                             filters.parentIDs = [state.parent.sysId];
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

        this.table              = this.newTableWithAutoRefresh(this.app.domain.assets, this);
        this.m_selectionManager = this.table.enableSimpleSelection((key) => key.sysId, (dev) => dev.extended.typedModel.sysId);
        this.init();
    }

    private async init()
    {
        this.m_isCRE = await this.app.domain.settings.isCRE();
    }

    getItemName(): string { return "Devices"; }

    public getTableConfigId(): string { return "devices"; }

    contextMenu(event: DatatableContextMenuEvent<DeviceFlat>)
    {
        let clickedId = event.row?.extended?.model.sysId;
        if (clickedId)
        {
            switch (event.columnProperty)
            {
                case "name":
                    if (this.m_isCRE)
                    {
                        event.root.addItem("Rename", async () =>
                        {
                            AssetRenameDialog.open(this, clickedId, this.table);
                        });
                    }
                    break;

                case "location":
                    if (this.m_isCRE)
                    {
                        event.root.addItem("Set location", async () =>
                        {
                            AssetLocationDialog.open(this, clickedId, this.table);
                        });
                    }
                    break;
            }
        }
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);

        if (AssetsService.hasAppliedFilters(filters))
        {
            let count                               = await this.app.domain.assets.getCount(new Models.DeviceFilterRequest());
            this.table.config.messages.totalMessage = `of ${count} Devices`;
        }

        return response.results;
    }

    getPage(offset: number,
            limit: number): Promise<DeviceExtended[]>
    {
        return this.app.domain.assets.getTypedPageFromTable(DeviceExtended, this.table, offset, limit);
    }

    async transform(rows: DeviceExtended[]): Promise<DeviceFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new DeviceFlat();
                                       result.extended = row;
                                       result.location = await row.getLocation();

                                       result.identityDescriptor  = row.getIdentityDescriptor();
                                       result.transportDescriptor = row.getTransportDescriptor();

                                       if (result.location)
                                       {
                                           result.locationPath = await result.location.getRecursivePath();
                                       }

                                       result.manufacturerName = row.typedModel.manufacturerName;
                                       result.productName      = row.typedModel.productName;
                                       result.modelNumber      = row.typedModel.modelName;

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: DeviceFlat)
    {
        this.app.ui.navigation.go("/devices/device", [item.extended.model.sysId]);
    }
}

class DeviceFilter
{
    parent: Models.Asset;
    filterText: string;
}

class DeviceFlat
{
    extended: DeviceExtended;

    identityDescriptor: string;
    transportDescriptor: string;

    location: LocationExtended;

    locationPath: string;

    manufacturerName: string;
    productName: string;
    modelNumber: string;
}
