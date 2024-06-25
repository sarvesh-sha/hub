import {Component, Injector, Input, ViewChild} from "@angular/core";

import {AssetLocationDialog} from "app/customer/workflows/asset-location-dialog";
import {AssetRenameDialog} from "app/customer/workflows/asset-rename-dialog";
import {ChildEquipmentDialog} from "app/customer/workflows/child-equipment-dialog";
import {EquipmentClassModificationDialog} from "app/customer/workflows/equipment-class-modification-dialog";
import {EquipmentParentDialog} from "app/customer/workflows/equipment-parent-dialog";
import {EquipmentRemoveDialog} from "app/customer/workflows/equipment-remove-dialog";
import {WorkflowsSummaryPageComponent} from "app/customer/workflows/workflows-summary-page.component";
import {AssetExtended, AssetsService, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {DatatableManager, FilterDebouncer, IDatatableDataProvider, SimpleExpansionManager, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent, DatatableDetailsTemplateDirective, DatatableRowActivateEvent} from "framework/ui/datatables/datatable.component";

import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-equipment-list",
               templateUrl: "./equipment-list.component.html"
           })
export class EquipmentListComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, AssetExtended, EquipmentFlat>
{
    private m_filterDebouncer: FilterDebouncer<EquipmentFilter, Models.AssetFilterRequest>;

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
    public set filters(value: Models.AssetFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.AssetFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    @Input()
    public set equipmentIds(value: string[])
    {
        this.m_filterDebouncer.setProperty("equipmentIds", value);
    }

    public get equipmentIds(): string[]
    {
        return this.m_filterDebouncer.getProperty("equipmentIds");
    }

    //--//

    @Input() itemName: string = "Equipment";

    //--//

    @Input() label: string;

    //--//

    @Input() tree: boolean = false;

    //--//

    private m_equipmentClassToLabel: Lookup<string>;

    private m_isCRE: boolean;

    public get isCRE(): boolean
    {
        return this.m_isCRE;
    }

    @ViewChild("detailsTemplate", {static: true}) detailsTemplate: DatatableDetailsTemplateDirective;

    public get details(): DatatableDetailsTemplateDirective
    {
        return this.tree ? this.detailsTemplate : null;
    }

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new EquipmentFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<EquipmentFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters        = Models.AssetFilterRequest.deepClone(baseFilters) || new Models.AssetFilterRequest();
                                                         filters.likeFilter = state.filterText;

                                                         if (state.equipmentIds)
                                                         {
                                                             filters.sysIds = state.equipmentIds;
                                                         }

                                                         filters.likeFilter = state.filterText;

                                                         if (!filters.tagsQuery)
                                                         {
                                                             filters.tagsQuery = new Models.TagsConditionIsEquipment();
                                                         }

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
                                                             this.table?.resetPagination();
                                                         }

                                                         this.table?.refreshData();
                                                     });

        this.generateEquipToLabel();
    }

    public getTableConfigId(): string { return "equipment"; }

    private async generateEquipToLabel(): Promise<void>
    {
        let equipmentClasses         = await this.app.bindings.getEquipmentClasses(false, null);
        this.m_equipmentClassToLabel = UtilsService.extractMappedLookup(equipmentClasses, (equipmentClass) => equipmentClass.label);
        this.resetTable();
        this.m_isCRE = await this.app.domain.settings.isCRE();
    }

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, EquipmentFlat>;
    private readonly m_cache = new Map<String, Promise<EquipmentSummary>>();

    private m_selectionManager: SimpleSelectionManager<Models.RecordIdentity, EquipmentFlat, string>;
    private m_expansionManager: EquipmentExpansionManager<Models.RecordIdentity, EquipmentFlat, string>;

    //--//

    private resetTable(andPagination: boolean = false,
                       andSelection: boolean  = false,
                       andExpansion: boolean  = false)
    {
        this.m_cache.clear();

        if (!this.table)
        {
            this.table                  = this.newTableWithAutoRefresh(this.app.domain.assets, this);
            this.m_selectionManager     = this.table.enableSimpleSelection((key) => key.sysId, (eq) => eq.extended.typedModel.sysId);
            this.m_expansionManager     = new EquipmentExpansionManager(this.table, false, true, (key) => key.sysId, (eq) => eq.extended.typedModel.sysId);
            this.table.expansionManager = this.m_expansionManager;
        }
        else
        {
            if (andPagination) this.table.resetPagination();
            if (andSelection) this.m_selectionManager.checkAllItems(false);
            if (andExpansion) this.m_expansionManager.expandAllItems(false, null);
            this.table.refreshData();
        }
    }

    contextMenu(event: DatatableContextMenuEvent<EquipmentFlat>)
    {
        let clickedId = event.row?.extended?.model.sysId;
        if (clickedId)
        {
            switch (event.columnProperty)
            {
                case "equipmentClass":
                    if (this.m_isCRE)
                    {
                        event.root.addItem("Change equipment class", async () =>
                        {
                            EquipmentClassModificationDialog.open(this, clickedId, this.table);
                        });
                    }
                    break;

                case "name":
                    event.root.addItem("Rename", async () =>
                    {
                        AssetRenameDialog.open(this, clickedId, this.table);
                    });
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

            if (this.m_isCRE)
            {
                event.root.addItem("Set Parent", async () =>
                {
                    if (await EquipmentParentDialog.open(this, clickedId, this.table))
                    {
                        this.resetTable();
                    }
                });

                event.root.addItem("Remove equipment", async () =>
                {
                    if (await EquipmentRemoveDialog.open(this, clickedId, this.table))
                    {
                        this.resetTable(false, true);
                    }
                });

                event.root.addItem("Add child equipment", async () =>
                {
                    ChildEquipmentDialog.open(this, clickedId, this.table);
                });
            }
        }
    }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.app.domain.assets.getList(filters);
        let ids      = response.results;

        let topLevelEquipments: Set<string>;
        if (!this.parent && !this.equipmentIds && this.tree)
        {
            topLevelEquipments = new Set<string>();

            for (let topEquipment of await this.app.domain.assets.getTopLevelEquipments())
            {
                topLevelEquipments.add(topEquipment.sysId);
            }

            ids = ids.filter((id) => topLevelEquipments.has(id.sysId));
        }

        if (this.hasAppliedFilters(filters))
        {
            if (topLevelEquipments)
            {
                this.table.config.messages.totalMessage = `of ${topLevelEquipments.size} Equipment`;
            }
            else if (this.parent)
            {
                let allEquipmentRequest = Models.AssetFilterRequest.newInstance({
                                                                                    tagsQuery: new Models.TagsConditionIsEquipment(),
                                                                                    parentIDs: [this.parent.sysId]
                                                                                });

                let unfilteredCount = await this.app.domain.assets.getCount(allEquipmentRequest);

                this.table.config.messages.totalMessage = `of ${unfilteredCount} Equipment`;
            }
            else if (this.equipmentIds)
            {
                this.table.config.messages.totalMessage = `of ${this.equipmentIds.length} Equipment`;
            }
        }
        return ids;
    }

    getItemName(): string
    {
        return this.itemName;
    }

    async getPage(offset: number,
                  limit: number): Promise<AssetExtended[]>
    {
        return this.app.domain.assets.getPageFromTable(this.table, offset, limit);
    }

    async transform(rows: AssetExtended[]): Promise<EquipmentFlat[]>
    {
        return await mapInParallel(rows,
                                   async (row) =>
                                   {
                                       let result      = new EquipmentFlat();
                                       result.extended = row;

                                       let classID           = row.typedModel.equipmentClassId;
                                       result.equipmentClass = this.m_equipmentClassToLabel ? this.m_equipmentClassToLabel[classID] : classID;

                                       result.location = await row.getLocation();

                                       if (result.location)
                                       {
                                           result.locationPath = await result.location.getRecursivePath();
                                       }

                                       // Start checking children, but dont' wait.
                                       result.lazyLoad(this, this.m_cache);

                                       return result;
                                   });
    }

    handleRowClick(activityEvent: DatatableRowActivateEvent<EquipmentFlat>)
    {
        let equipment = activityEvent.row;
        if (equipment) this.itemClicked(activityEvent.columnId, equipment);
    }

    itemClicked(columnId: string,
                item: EquipmentFlat)
    {
        this.app.ui.navigation.go("/equipment/equipment", [item.extended.model.sysId]);
    }

    goToWorkflows(item: EquipmentFlat)
    {
        WorkflowsSummaryPageComponent.navigate(this.app, {assetIDs: [item.extended.model.sysId]});
    }

    private hasAppliedFilters(filters: Models.AssetFilterRequest)
    {
        let hasAppliedFilters = AssetsService.hasAppliedFilters(filters, false, false);
        // Only consider filtered if we have binary condition for tagsQuery
        return hasAppliedFilters || filters?.tagsQuery instanceof Models.TagsConditionBinary;
    }
}

class EquipmentSummary
{
    numChildEquipment: number;
    numChildControlPoints: number;
    hasWorkflows: boolean;

    static async lazyLoad(extended: AssetExtended): Promise<EquipmentSummary>
    {
        let entry = new EquipmentSummary();

        let counts = await extended.getChildrenCounts();

        entry.numChildEquipment     = counts.numChildEquipment;
        entry.numChildControlPoints = counts.numChildControlPoints;

        let activeWorkflows = await extended.getActiveWorkflows();
        entry.hasWorkflows  = activeWorkflows?.length > 0;

        return entry;
    }
}

class EquipmentFilter
{
    parent: Models.Asset;
    equipmentIds: string[];
    filterText: string;
}

class EquipmentFlat
{
    extended: AssetExtended;

    hasWorkflows: boolean;

    equipmentClass: string;

    numChildEquipment: number | string;
    numChildControlPoints: number | string;

    location: LocationExtended;
    locationPath: string;

    async lazyLoad(comp: EquipmentListComponent,
                   cache: Map<String, Promise<EquipmentSummary>>)
    {
        let entry = cache.get(this.extended.model.sysId);
        if (!entry)
        {
            this.numChildEquipment     = "<counting...>";
            this.numChildControlPoints = "<counting...>";
            comp.detectChanges();

            entry = EquipmentSummary.lazyLoad(this.extended);

            cache.set(this.extended.model.sysId, entry);
        }

        let val = await entry;

        this.numChildEquipment     = val.numChildEquipment;
        this.numChildControlPoints = val.numChildControlPoints;
        this.hasWorkflows          = val.hasWorkflows;

        comp.detectChanges();
    }
}

class EquipmentExpansionManager<K, T extends EquipmentFlat, ID> extends SimpleExpansionManager<K, T, ID>
{
    public canExpand(row: EquipmentFlat): boolean
    {
        return row.numChildEquipment > 0;
    }
}
