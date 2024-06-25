import {Directive, EventEmitter, Injector, Input, Output} from "@angular/core";
import {PageEvent} from "@angular/material/paginator";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {DataExplorerPageComponent} from "app/customer/visualization/data-explorer-page.component";
import {AssetHideDialog} from "app/customer/workflows/asset-hide-dialog";
import {AssetRenameDialog} from "app/customer/workflows/asset-rename-dialog";
import {AssetSamplingDialog} from "app/customer/workflows/asset-sampling-dialog";
import {AssetSamplingPeriodDialog} from "app/customer/workflows/asset-sampling-period-dialog";
import {AssignEquipmentDialog} from "app/customer/workflows/assign-equipment-dialog";
import {PointClassModificationDialog} from "app/customer/workflows/point-class-modification-dialog";
import {AssetExtended, AssetsService, DeviceElementExtended, LocationExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {ControlPointPreviewComponent} from "app/shared/assets/chart-preview/control-point-preview.component";
import {PreviewInvokerComponent} from "app/shared/utils/preview-invoker/preview-invoker.component";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

import {DatatableManager, FilterDebouncer, IDatatableDataProvider, SimpleSelectionManager} from "framework/ui/datatables/datatable-manager";
import {DatatableContextMenuEvent} from "framework/ui/datatables/datatable.component";
import {OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";
import {Future, inParallel, mapInParallel} from "framework/utils/concurrency";
import {Unsubscribable} from "rxjs";


@Directive()
export abstract class DeviceElementsListBase<T extends DeviceElementFlat> extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, DeviceElementExtended, T>
{
    // Hint to enable template syntax <... disabled> instead of <... [disabled]="true">
    static ngAcceptInputType_excludeHidden: boolean | "";

    private idToBacnetAddress: Lookup<Models.BACnetDeviceAddress> = {};

    private m_filterDebouncer: FilterDebouncer<DeviceElementFilter, Models.DeviceElementFilterRequest>;
    private m_initialize: Promise<void>;

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
    public set filters(value: Models.DeviceElementFilterRequest)
    {
        this.m_filterDebouncer.setExternalFilter(value);
    }

    public get filters(): Models.DeviceElementFilterRequest
    {
        return this.m_filterDebouncer.getExternalFilter();
    }

    //--//

    @Input() public limit: number;

    @Output() public limitExceededUpdated = new EventEmitter<boolean>();

    @Input()
    public set pointIds(pointIds: string[])
    {
        this.m_filterDebouncer.setProperty("pointIds", pointIds);
    }

    public get pointIds(): string[]
    {
        return this.m_filterDebouncer.getProperty("pointIds");
    }

    @Input()
    public set excludeHidden(val: boolean)
    {
        this.m_filterDebouncer.setProperty("excludeHidden", val);
    }

    public get excludeHidden(): boolean
    {
        return this.m_filterDebouncer.getProperty("excludeHidden");
    }

    //--//

    @Input() label: string;

    //--//

    private m_isCRE: boolean;

    public get isCRE(): boolean
    {
        return this.m_isCRE;
    }

    //--//

    private previews: Map<string, OverlayDialogRef<any>> = new Map<string, OverlayDialogRef<any>>();
    private toggleSubject: Unsubscribable;

    constructor(inj: Injector)
    {
        super(inj);

        this.m_filterDebouncer = new FilterDebouncer(() =>
                                                     {
                                                         return new DeviceElementFilter();
                                                     },
                                                     () =>
                                                     {
                                                         return this.getViewStateValue<DeviceElementFilter>("TABLE_FILTERS");
                                                     },
                                                     (state) =>
                                                     {
                                                         this.setViewStateValue("TABLE_FILTERS", state);
                                                     },
                                                     (state,
                                                      baseFilters) =>
                                                     {
                                                         let filters = Models.DeviceElementFilterRequest.deepClone(baseFilters) || new Models.DeviceElementFilterRequest();

                                                         if (state.parent)
                                                         {
                                                             filters.parentIDs = [state.parent.sysId];
                                                         }

                                                         if (state.pointIds)
                                                         {
                                                             filters.sysIds = state.pointIds;
                                                         }

                                                         if (state.filterText?.length > 1)
                                                         {
                                                             filters.likeFilter = state.filterText;
                                                         }
                                                         else
                                                         {
                                                             filters.likeFilter = undefined;
                                                         }

                                                         if (state.excludeHidden)
                                                         {
                                                             filters.isNotHidden = state.excludeHidden;
                                                         }

                                                         return filters;
                                                     },
                                                     (filtersChanged: boolean) =>
                                                     {
                                                         if (filtersChanged)
                                                         {
                                                             this.m_cache.clear();

                                                             this.table.resetPagination();

                                                             this.m_selectionManager.checkAllItems(false);
                                                         }

                                                         this.table.refreshData();
                                                     });

        this.table              = this.newTableWithAutoRefresh(this.app.domain.assets, this);
        this.m_selectionManager = this.table.enableSimpleSelection((key) => key.sysId, (de) => de.extended.typedModel.sysId);

        this.m_initialize = this.initialize();
    }

    abstract newRow(): T;

    ngOnInit()
    {
        // Listen for any and all toggle events
        this.toggleSubject = PreviewInvokerComponent.onToggle.subscribe((id: string) =>
                                                                        {
                                                                            // Attempt to toggle the preview
                                                                            let preview = ControlPointPreviewComponent.toggle(this, id, true);

                                                                            // Check if it opened or close (null when closed)
                                                                            if (preview)
                                                                            {
                                                                                // Track the preview
                                                                                this.previews.set(id, preview);

                                                                                // If the preview is closed externally, stop tacking it
                                                                                preview.afterClose()
                                                                                       .subscribe(() =>
                                                                                                  {
                                                                                                      this.previews.delete(id);
                                                                                                  });
                                                                            }
                                                                            else
                                                                            {
                                                                                // Stop tracking the preview if we still were
                                                                                this.previews.delete(id);
                                                                            }
                                                                        });
    }

    ngOnDestroy()
    {
        // Stop listening to toggle events
        this.toggleSubject.unsubscribe();

        // Close all previews
        for (let preview of this.previews.values())
        {
            preview.close();
        }
    }

    private async initialize(): Promise<void>
    {
        let pointClassOptions        = await this.app.bindings.getPointClasses(false, null);
        let equipmentClassOptions    = await this.app.bindings.getEquipmentClasses(false, null);
        this.m_pointClassToLabel     = this.mapOptionsToLabel(pointClassOptions);
        this.m_equipmentClassToLabel = this.mapOptionsToLabel(equipmentClassOptions);

        this.m_isCRE = await this.app.domain.settings.isCRE();
    }

    private mapOptionsToLabel(options: ControlOption<string>[])
    {
        return UtilsService.extractMappedLookup(options, (option) => option.label);
    }

    private m_pointClassToLabel: Lookup<string>;
    private m_equipmentClassToLabel: Lookup<string>;

    private m_pointToEquipment: Lookup<string>                 = {};
    private m_equipmentFetching: Lookup<Future<EquipmentFlat>> = {};
    private m_equipment: Lookup<EquipmentFlat>                 = {};
    private m_equipmentChanged: boolean;

    //--//

    table: DatatableManager<Models.RecordIdentity, AssetExtended, T>;
    private readonly m_cache = new Map<String, Promise<DeviceElementSummary>>();

    private m_selectionManager: SimpleSelectionManager<Models.RecordIdentity, T, string>;

    getItemName(): string { return "Control Points"; }

    async getList(): Promise<Models.RecordIdentity[]>
    {
        await this.m_initialize;

        let filters    = this.m_filterDebouncer.generateFilter();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        let response = await this.performFilter(filters);

        if (AssetsService.hasAppliedFilters(filters, !this.parent))
        {
            let filters2 = new Models.DeviceElementFilterRequest();

            if (this.excludeHidden)
            {
                filters2.isNotHidden = this.excludeHidden;
            }

            if (filters.sysIds?.length)
            {
                this.table.config.messages.totalMessage = `of ${filters.sysIds?.length} Control Points`;
            }
            else
            {
                if (this.parent)
                {
                    filters2.parentIDs = [this.parent.sysId];
                }

                let count                               = await this.app.domain.assets.getCount(filters2);
                this.table.config.messages.totalMessage = `of ${count} Control Points`;
            }
        }

        return response;
    }

    private async performFilter(filters: Models.DeviceElementFilterRequest): Promise<Models.RecordIdentity[]>
    {
        if (this.limit)
        {
            filters.maxResults = this.limit;
        }

        if (filters.parentIDs?.length && filters.parentTagsQuery)
        {
            let deviceFilters             = Models.DeviceElementFilterRequest.newInstance(filters);
            deviceFilters.parentTagsQuery = null;
            let equipmentFilters          = Models.DeviceElementFilterRequest.newInstance(filters);
            equipmentFilters.parentIDs    = [];

            let [deviceResp, equipmentResp] = await Promise.all([
                                                                    this.app.domain.assets.getListWithPaging(deviceFilters),
                                                                    this.app.domain.assets.getListWithPaging(equipmentFilters)
                                                                ]);

            let deviceSet = new Set<string>(deviceResp.map((id) => id.sysId));
            let result    = equipmentResp.filter((id) => deviceSet.has(id.sysId));
            if (result.length > this.limit)
            {
                this.limitExceededUpdated.emit(true);
                return [];
            }

            this.limitExceededUpdated.emit(false);
            return result;
        }
        else
        {
            let count = await this.app.domain.assets.getCount(filters);
            if (count > this.limit)
            {
                this.limitExceededUpdated.emit(true);
                return [];
            }
            else
            {
                let response = await this.app.domain.assets.getList(filters);
                this.limitExceededUpdated.emit(false);
                return response.results;
            }
        }
    }

    async setNextPage(event: PageEvent)
    {
        this.table.offset = event.pageIndex;
        await this.table.refreshPage();
    }

    async getPage(offset: number,
                  limit: number): Promise<DeviceElementExtended[]>
    {
        let rows = await this.app.domain.assets.getTypedPageFromTable(DeviceElementExtended, this.table, offset, limit);

        // Don't wait!!!
        this.lazyEquipment(rows);

        return rows;
    }

    async transform(rows: DeviceElementExtended[]): Promise<T[]>
    {
        if (this.parent instanceof Models.Device)
        {
            let parentDeviceDesc = this.parent.identityDescriptor;
            if (parentDeviceDesc instanceof Models.BACnetDeviceDescriptor && parentDeviceDesc.address)
            {
                this.idToBacnetAddress[this.parent.sysId] = parentDeviceDesc.address;
            }
        }
        else
        {
            let parentRecordIds = [];
            let idsAdded        = new Set();
            for (let row of rows)
            {
                let parentId = row.model.parentAsset?.sysId;
                if (parentId && !this.idToBacnetAddress[parentId] && !idsAdded.has(parentId))
                {
                    idsAdded.add(parentId);
                    parentRecordIds.push(AssetExtended.newIdentityRaw(parentId));
                }
            }

            let parentExts = await this.app.domain.assets.getExtendedBatch(parentRecordIds);
            for (let parentExt of parentExts)
            {
                let identityDescriptor = parentExt?.model.identityDescriptor;
                if (identityDescriptor instanceof Models.BACnetDeviceDescriptor && identityDescriptor.address)
                {
                    this.idToBacnetAddress[parentExt.model.sysId] = identityDescriptor.address;
                }
            }
        }

        return await mapInParallel(rows,
                                   async (row) =>
                                   {
                                       let result      = this.newRow();
                                       result.extended = row;
                                       result.sysId    = row.model.sysId;

                                       let equipId = this.m_pointToEquipment[row.model.sysId];
                                       if (equipId)
                                       {
                                           let equipment         = this.m_equipment[equipId];
                                           result.equipment      = equipment.name;
                                           result.equipmentPath  = equipment.path;
                                           result.equipmentClass = this.m_equipmentClassToLabel[equipment.equipmentClassId];
                                       }

                                       let classID       = row.typedModel.pointClassId;
                                       result.pointClass = this.m_pointClassToLabel ? this.m_pointClassToLabel[classID] : classID;

                                       result.location = await row.getLocation();
                                       if (result.location)
                                       {
                                           result.locationPath = await result.location.getRecursivePath();
                                       }

                                       result.bacnet = this.idToBacnetAddress[row.model.parentAsset?.sysId];

                                       // Start checking children, but dont' wait.
                                       result.lazyLoad(this, this.m_cache);

                                       return result;
                                   });
    }

    async lazyEquipment(rows: DeviceElementExtended[])
    {
        await inParallel(rows, (row) => this.fetchEquipment(row));

        if (this.m_equipmentChanged)
        {
            this.m_equipmentChanged = false;

            this.table.refreshPage();
        }
    }

    async fetchEquipment(row: AssetExtended)
    {
        let equipId = this.m_pointToEquipment[row.model.sysId];
        if (equipId !== undefined) return;

        let equipments = await row.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
        if (equipments?.length)
        {
            let equipment = equipments[0];
            equipId       = equipment.model.sysId;
        }
        else
        {
            equipId = null;
        }

        if (equipId)
        {
            if (!this.m_equipmentFetching[equipId])
            {
                let future                        = new Future<EquipmentFlat>();
                this.m_equipmentFetching[equipId] = future;

                let equip   = await this.app.domain.assets.getExtendedById(equipId);
                let summary = {
                    name            : equip.model.name,
                    sysId           : equip.model.sysId,
                    equipmentClassId: equip.model.equipmentClassId,
                    path            : ""
                };

                while (equip)
                {
                    let [parent] = await equip.getExtendedParentsOfRelation(Models.AssetRelationship.controls);
                    if (parent)
                    {
                        summary.path = summary.path ? `${parent.model.name} - ${summary.path}` : parent.model.name;
                    }
                    equip = parent;
                }

                this.m_equipment[equipId] = summary;

                future.resolve(summary);
            }

            await this.m_equipmentFetching[equipId];
        }

        this.m_pointToEquipment[row.model.sysId] = equipId;
        this.m_equipmentChanged                  = true;
    }

    static async fetchLocation(row: AssetExtended): Promise<void>
    {
        let loc = await row.getLocation();
        if (loc)
        {
            await loc.getRecursivePath();
        }
    }

    async itemClicked(columnId: string,
                      item: T)
    {
        DeviceElementsDetailPageComponent.navigate(this.app, item.extended);
    }

    contextMenu(event: DatatableContextMenuEvent<T>)
    {
        let clickedId = event.row?.extended?.model.sysId;
        if (clickedId)
        {
            switch (event.columnProperty)
            {
                case "pointClass":
                    if (this.m_isCRE)
                    {
                        event.root.addItem("Change point class", async () =>
                        {
                            PointClassModificationDialog.open(this, clickedId, this.table);
                        });
                    }
                    break;

                case "name":
                    event.root.addItem("Rename", async () =>
                    {
                        AssetRenameDialog.open(this, clickedId, this.table);
                    });
                    break;

                case "isSampling":
                    if (this.m_isCRE)
                    {
                        let isSampling = event.row.extended.hasSamplingSettings;
                        event.root.addItem(`${isSampling ? "Disable" : "Enable"} Sampling`, async () =>
                        {
                            AssetSamplingDialog.open(this, clickedId, this.table, !isSampling);
                        });

                        if (isSampling)
                        {
                            event.root.addItem("Set sampling period", async () =>
                            {
                                AssetSamplingPeriodDialog.open(this, clickedId, this.table);
                            });
                        }
                    }
                    break;

                case "hidden":
                    if (this.m_isCRE)
                    {
                        let isHidden = event.row.extended.model.hidden;
                        event.root.addItem(`Make ${isHidden ? "visible" : "hidden"}`, async () =>
                        {
                            AssetHideDialog.open(this, clickedId, this.table, !isHidden);
                        });
                    }
                    break;
            }

            if (this.m_isCRE)
            {
                event.root.addItem("Assign to Equipment", async () =>
                {
                    if (await AssignEquipmentDialog.open(this, clickedId, this.table))
                    {
                        this.m_cache.clear();
                        this.table.refreshData();
                    }
                });
            }

            let isSampling = event.row.extended.hasSamplingSettings;
            if (isSampling)
            {
                let selectionIds = this.table.selectionManager?.selection;
                if (selectionIds?.size >= 2)
                {
                    event.root.addItem("Show All in Data Explorer", async () =>
                    {
                        let ids: string[] = [];

                        for (let selectionId of selectionIds)
                        {
                            ids.push(selectionId);
                        }

                        await DataExplorerPageComponent.visualizeDeviceElements(this, this.app.ui.navigation, this.app.ui.viewstate, ids);
                    });
                }
            }
        }
    }
}

class DeviceElementSummary
{
    hasWorkflows: boolean;

    static async lazyLoad(extended: DeviceElementExtended): Promise<DeviceElementSummary>
    {
        let res = new DeviceElementSummary();

        let activeWorkflows = await extended.getActiveWorkflows();
        res.hasWorkflows    = activeWorkflows?.length > 0;

        return res;
    }
}

class DeviceElementFilter
{
    parent: Models.Asset;
    pointIds: string[];
    filterText: string;
    excludeHidden: boolean;
}

export class DeviceElementFlat
{
    sysId: string;

    extended: DeviceElementExtended;

    location: LocationExtended;

    locationPath: string;

    hasWorkflows: boolean;

    pointClass: string;

    bacnet: Models.BACnetDeviceAddress;

    equipment: string;

    equipmentPath: string;

    equipmentClass: string;

    async lazyLoad<T extends DeviceElementFlat>(comp: DeviceElementsListBase<T>,
                                                cache: Map<String, Promise<DeviceElementSummary>>)
    {
        let entry = cache.get(this.extended.model.sysId);
        if (!entry)
        {
            entry = DeviceElementSummary.lazyLoad(this.extended);

            cache.set(this.extended.model.sysId, entry);
        }

        let val = await entry;

        this.hasWorkflows = val.hasWorkflows;
        comp.detectChanges();
    }
}

export class EquipmentFlat
{
    sysId: string;
    name: string;
    path: string;
    equipmentClassId: string;
}
