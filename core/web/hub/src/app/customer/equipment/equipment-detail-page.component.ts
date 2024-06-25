import {Component, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {DeviceElementDataExporter} from "app/customer/device-elements/device-element-data-exporter";
import {DeviceElementsListComponent} from "app/customer/device-elements/device-elements-list.component";
import {EquipmentDataExporter} from "app/customer/equipment/equipment-data-exporter";
import {EquipmentListComponent} from "app/customer/equipment/equipment-list.component";
import {AssetExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {DeviceElementFiltersAdapterComponent} from "app/shared/filter/asset/device-element-filters-adapter.component";
import {EquipmentFiltersAdapterComponent} from "app/shared/filter/asset/equipment-filters-adapter.component";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-equipment-detail-page",
               templateUrl: "./equipment-detail-page.component.html",
               styleUrls  : ["./equipment-detail-page.component.scss"]
           })
export class EquipmentDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    equipmentId: string;

    equipment: AssetExtended;
    equipmentManualTags: string[]     = [];
    equipmentClassDisplayName: string = "Loading...";
    equipmentRemoveChecks: Models.ValidationResult[];
    equipmentNoDeleteReason: string;

    equipmentUpdateChecks: Models.ValidationResult[];
    equipmentNoUpdateReason: string;

    counts: { numChildEquipment: number, numChildControlPoints: number };

    parentEquipment: AssetExtended;

    operationalStates: ControlOption<Models.AssetState>[];

    history: Models.AlertHistory[];

    location: LocationExtended;
    locationModel: Models.Location;
    locationModelReady = false;

    //--//

    equipmentFilters: Models.AssetFilterRequest;
    deviceElemFilters: Models.DeviceElementFilterRequest;

    equipmentChips: FilterChip[];
    deviceElemChips: FilterChip[];

    filteringLoaded: boolean;
    deviceElemLocalFiltering: boolean;

    private m_sampling: Models.FilterPreferenceBoolean = Models.FilterPreferenceBoolean.Yes;
    set sampling(sampling: Models.FilterPreferenceBoolean)
    {
        this.m_sampling = sampling;
        DeviceElementFiltersAdapterComponent.updateFiltersFromSamplingInput(this.deviceElemFilters, this.m_sampling);
    }

    get sampling(): Models.FilterPreferenceBoolean
    {
        return this.m_sampling;
    }

    private customerNotes: string;

    existingBookmark: Models.BookmarkConfiguration;
    bookmarkName: string;

    get bookmarkOptionLabel(): string
    {
        return this.existingBookmark ? "Edit Bookmark" : "Add to Bookmarks";
    }

    get pristine(): boolean
    {
        if (!this.equipment) return true;
        if (!this.equipmentForm) return true;
        if (!this.equipmentForm.pristine) return false;

        return UtilsService.equivalentStrings(this.customerNotes, this.equipment.model.customerNotes) && UtilsService.compareArraysAsSets(this.equipmentManualTags, this.equipment.model.manualTags);
    }

    //--//

    workflowOverlayConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    //--//

    @ViewChild(DeviceElementsListComponent) deviceElemList: DeviceElementsListComponent;
    @ViewChild(EquipmentListComponent) childEquipList: EquipmentListComponent;

    @ViewChild(DeviceElementFiltersAdapterComponent) deviceElemFiltersAdapter: DeviceElementFiltersAdapterComponent;
    @ViewChild(EquipmentFiltersAdapterComponent) equipmentFiltersAdapter: EquipmentFiltersAdapterComponent;

    @ViewChild("equipmentForm") equipmentForm: NgForm;

    protected async onNavigationComplete()
    {
        this.equipmentId = this.getPathParameter("id");
        if (this.equipmentId)
        {
            await this.init();
            await this.loadEquipment();
        }
    }

    async init()
    {
        this.filteringLoaded = false;

        this.equipmentFilters = new Models.AssetFilterRequest();

        this.deviceElemLocalFiltering = await this.app.domain.settings.isTransportation();
        this.deviceElemFilters        = new Models.DeviceElementFilterRequest();

        this.filteringLoaded = true;
    }

    async loadEquipment()
    {
        // load equipment info
        this.equipment = await this.app.domain.assets.getTypedExtendedById(AssetExtended, this.equipmentId);
        if (!this.equipment)
        {
            this.exit();
            return;
        }

        this.customerNotes = this.equipment.typedModel.customerNotes;

        this.counts = await this.equipment.getChildrenCounts();

        let classes                    = await this.app.bindings.getEquipmentClasses(true, null);
        this.equipmentClassDisplayName = classes.find((c) => c.id === this.equipment?.model.equipmentClassId)?.label || "No Equipment Class";

        await this.equipment.getAll();

        await this.findParent();

        this.equipmentRemoveChecks   = await this.equipment.checkRemove();
        this.equipmentNoDeleteReason = this.fromValidationToReason("Remove is disabled because:", this.equipmentRemoveChecks);

        this.equipmentUpdateChecks   = await this.equipment.checkUpdate();
        this.equipmentNoUpdateReason = this.fromValidationToReason("Update is disabled because:", this.equipmentUpdateChecks);

        this.equipmentManualTags = this.equipment.model.manualTags;

        await this.refreshLocation();

        let name          = this.equipment.model.name;
        this.bookmarkName = this.location ? `${name} in ${this.location.model.name}` : name;

        this.operationalStates = await this.app.domain.assets.getOperationalStates();

        // set breadcrumbs
        this.app.ui.navigation.breadcrumbCurrentLabel = this.equipment.model.name ? this.equipment.model.name : "<unknown>";

        // load alert history
        let history  = await this.equipment.getAlertHistory();
        this.history = history.map((ext) => ext.model);

        this.existingBookmark = await this.app.domain.bookmarks.getBookmarkByRecordID(this.equipment.model.sysId);

        this.subscribeOneShot(this.equipment,
                              async (ext,
                                     action) =>
                              {
                                  await this.loadEquipment();
                              });

        this.equipmentForm.form.markAsPristine();
        this.detectChanges();
    }

    openBookmarkOverlay()
    {
        BookmarkEditorDialogComponent.open(this, !!this.existingBookmark, this.bookmarkName,
                                           (name,
                                            description) => this.saveBookmark(name, description),
                                           this.existingBookmark);
    }

    private async findParent()
    {
        this.parentEquipment = await this.equipment.walkParentRelations((parent) => parent.model.isEquipment, Models.AssetRelationship.controls);
    }

    async locationChanged(selectedLocationID: string)
    {
        this.equipment.setLocation(selectedLocationID);

        await this.refreshLocation();

        this.equipmentForm.form.markAsDirty();
    }

    private async refreshLocation()
    {
        this.location           = await this.equipment.getLocation();
        this.locationModel      = this.location ? this.location.typedModel : new Models.Location();
        this.locationModelReady = true;
    }

    exportControlPointsToExcel()
    {
        let fileName       = DownloadDialogComponent.fileName(this.equipment.model.name + "_control_points", ".xlsx");
        let dataDownloader = new DeviceElementDataExporter(this.app.domain, fileName, this.equipmentId);
        DownloadDialogComponent.openWithGenerator(this, "Export Device Elements", fileName, dataDownloader);
    }

    exportEquipmentToExcel()
    {
        let fileName       = DownloadDialogComponent.fileName("equipment_summary", ".xlsx");
        let dataDownloader = new EquipmentDataExporter(this.app.domain, fileName, this.equipmentId);
        DownloadDialogComponent.openWithGenerator(this, "Export Equipment", fileName, dataDownloader);
    }

    @ReportError
    private async saveBookmark(name: string,
                               description: string): Promise<Models.BookmarkConfiguration>
    {
        if (this.existingBookmark)
        {
            this.existingBookmark.description = description;
            await this.app.domain.bookmarks.updateBookmark(this.existingBookmark);
        }
        else
        {
            this.existingBookmark = await this.app.domain.bookmarks.generateBookmark(this.bookmarkName, description, Models.BookmarkType.EQUIPMENT, this.equipment.model.sysId);
        }

        if (this.existingBookmark) this.app.framework.errors.success("Bookmark saved", -1);

        return this.existingBookmark;
    }

    @ReportError
    async remove()
    {
        if (this.equipment)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Equipment."))
            {
                let name = this.equipment.model.name || this.equipment.model.sysId;
                let msg  = this.app.framework.errors.success(`Deleting equipment '${name}'...`, -1);

                let promise = this.equipment.remove();

                // Navigate away without waiting for deletion, since it can take a long time.
                this.exit();

                if (await promise)
                {
                    this.app.framework.errors.dismiss(msg);
                    this.app.framework.errors.success(`Equipment '${name}' deleted`, -1);
                }
            }
        }
    }

    @ReportError
    async save()
    {
        await this.equipment.save();

        this.app.framework.errors.success("Equipment updated", -1);

        await this.cancel();
    }

    async cancel()
    {
        await this.equipment.refresh();

        await this.loadEquipment();
    }

    navigateTo(targetEquipment: AssetExtended)
    {
        if (targetEquipment) this.app.ui.navigation.go("/equipment/equipment", [targetEquipment.model.sysId]);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }
}
