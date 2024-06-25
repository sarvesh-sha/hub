import {Component, EventEmitter, Injector, Input, Output} from "@angular/core";

import {LocationParentDialog} from "app/customer/workflows/location-parent-dialog";
import {AssetExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, FilterDebouncer, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-location-list",
               templateUrl: "./location-list.component.html"
           })
export class LocationListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, LocationExtended, LocationFlat>
{
    private m_filterDebouncer: FilterDebouncer<LocationFilter, Models.LocationFilterRequest>;

    public get parent(): Models.Location
    {
        return this.m_filterDebouncer.getProperty("parent");
    }

    @Input()
    public set parent(value: Models.Location)
    {
        this.m_filterDebouncer.setProperty("parent", value);
    }

    //--//

    private m_selection: Set<string> = new Set<string>();

    private m_selectionManager: SimpleSelectionManager<Models.RecordIdentity, LocationFlat, string>;

    //--//

    @Input() withSelection: boolean                      = true;
    @Output() selectionChange: EventEmitter<Set<string>> = new EventEmitter<Set<string>>();

    @Input()
    set selection(value: Set<string>)
    {
        if (this.m_selection != value)
        {
            this.m_selection = value;
            if (this.m_selectionManager) this.m_selectionManager.selection = value;
        }
    }

    //--//

    @Input() withNavigation: boolean = true;
    @Input() withFiltering: boolean  = false;

    get filterText(): string
    {
        return this.m_filterDebouncer.getProperty("filterText");
    }

    set filterText(value: string)
    {
        this.m_filterDebouncer.setProperty("filterText", value);
    }

    table: DatatableManager<Models.RecordIdentity, AssetExtended, LocationFlat>;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new LocationFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<LocationFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters        = Models.LocationFilterRequest.deepClone(baseFilters) || new Models.LocationFilterRequest();
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

        this.table = this.newTableWithAutoRefresh(this.app.domain.assets, this);

        // Create selection manager early so we can bind events properly
        this.m_selectionManager           = this.table.enableSimpleSelection((ri) => ri.sysId, (flat) => flat.extended.typedModel.sysId);
        this.m_selectionManager.selection = this.m_selection;
        this.selectionChange              = this.m_selectionManager.selectionChange;
    }

    public ngOnInit(): void
    {
        super.ngOnInit();
        if (!this.withSelection)
        {
            this.table.selectionManager = null;
        }
    }

    getItemName(): string { return "Locations"; }

    public getTableConfigId(): string { return "locations"; }

    contextMenu(event: DatatableContextMenuEvent<LocationFlat>)
    {
        let clickedId = event.row?.extended?.model.sysId;
        if (clickedId)
        {
            switch (event.columnProperty)
            {
                case "name":
                    event.root.addItem("Set Parent", async () =>
                    {
                        await LocationParentDialog.open(this, clickedId, this.table);
                        this.table.refreshData();
                    });
                    break;
            }
        }
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);

        return response.results;
    }

    getPage(offset: number,
            limit: number): Promise<LocationExtended[]>
    {
        return this.app.domain.assets.getTypedPageFromTable(LocationExtended, this.table, offset, limit);
    }

    async transform(rows: LocationExtended[]): Promise<LocationFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row,
                                          index) =>
                                   {
                                       let result      = new LocationFlat();
                                       result.extended = row;
                                       result.name     = row.model.name;

                                       return result;
                                   });
    }

    itemClicked(columnId: string,
                item: LocationFlat)
    {
        if (columnId !== "check" && this.withNavigation) this.app.ui.navigation.go("/configuration/locations/location", [item.extended.model.sysId]);
    }

    private async getExtended(identities: Models.RecordIdentity[]): Promise<LocationExtended[]>
    {
        // Fetch all extended models
        return await this.app.domain.locations.getExtended(identities);
    }
}

class LocationFilter
{
    parent: Models.Location;
    filterText: string;
}

class LocationFlat
{
    extended: LocationExtended;
    name: string;
}
