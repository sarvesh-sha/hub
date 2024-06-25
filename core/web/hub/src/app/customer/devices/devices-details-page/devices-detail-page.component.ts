import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";
import {DeviceElementDataExporter} from "app/customer/device-elements/device-element-data-exporter";
import {DeviceElementsListComponent} from "app/customer/device-elements/device-elements-list.component";
import {AssetExtended, DeviceExtended, LocationExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";
import {BookmarkEditorDialogComponent} from "app/shared/bookmarks/bookmark-editor-dialog.component";
import {DeviceElementFiltersAdapterComponent} from "app/shared/filter/asset/device-element-filters-adapter.component";
import {AlertTimelineItem, TimelineItem} from "app/shared/timelines/timeline.component";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-devices-detail-page",
               templateUrl: "./devices-detail-page.component.html",
               styleUrls  : ["./devices-detail-page.component.scss"]
           })
export class DevicesDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    deviceID: string;

    device: DeviceExtended;
    deviceManualTags: string[] = [];
    deviceRemoveChecks: Models.ValidationResult[];
    deviceNoDeleteReason: string;

    deviceUpdateChecks: Models.ValidationResult[];
    deviceNoUpdateReason: string;

    location: LocationExtended;
    locationModel: Models.Location;
    locationModelReady = false;

    operationalStates: ControlOption<Models.AssetState>[];

    deviceHealth: Models.DeviceHealthSummary;
    history: AlertTimelineItem[];

    private customerNotes: string;

    //--//

    filters             = new Models.DeviceElementFilterRequest();
    chips: FilterChip[] = [];

    //--//

    @ViewChild("deviceForm") deviceForm: NgForm;
    @ViewChild("tabGroup") tabGroup: TabGroupComponent;
    @ViewChild(DeviceElementsListComponent) controlPointsList: DeviceElementsListComponent;
    @ViewChild(DeviceElementFiltersAdapterComponent) filtersAdapter: DeviceElementFiltersAdapterComponent;

    //--//

    childCpBookmarks: BookmarkSet[] = [];
    existingBookmark: Models.BookmarkConfiguration;
    bookmarkName: string;

    filteringLoaded: boolean;
    deviceElemLocalFiltering: boolean;

    get bookmarkOptionLabel(): string
    {
        return this.existingBookmark ? "Edit Bookmark" : "Add to Bookmarks";
    }

    get pristine(): boolean
    {
        if (!this.device) return true;
        if (!this.deviceForm) return true;
        if (!this.deviceForm.pristine) return false;

        return UtilsService.equivalentStrings(this.customerNotes, this.device.typedModel.customerNotes) && UtilsService.compareArraysAsSets(this.deviceManualTags, this.device.model.manualTags);
    }

    workflowOverlayConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    //--//

    constructor(inj: Injector)
    {
        super(inj);

        this.device = this.app.domain.assets.wrapTypedModel(DeviceExtended, new Models.Device());
    }

    protected async onNavigationComplete()
    {
        this.deviceID = this.getPathParameter("id");

        await this.init();
        await this.loadDevice();
    }

    private async init()
    {
        this.filteringLoaded          = false;
        this.deviceElemLocalFiltering = await this.app.domain.settings.isTransportation();
        this.filteringLoaded          = true;
    }

    protected shouldDelayNotifications(): boolean
    {
        return !this.deviceForm.pristine;
    }

    async loadDevice()
    {
        this.deviceNoDeleteReason = "<checking...>";

        // load device info
        let device = await this.app.domain.assets.getTypedExtendedById(DeviceExtended, this.deviceID);
        if (!device)
        {
            this.exit();
            return;
        }

        this.device = device;

        if (this.device instanceof AssetExtended && !(this.device instanceof DeviceExtended))
        {
            this.app.ui.navigation.go("/equipment/equipment", [this.deviceID]);
            return;
        }

        this.deviceManualTags = this.device.model.manualTags;
        this.customerNotes    = this.device.model.customerNotes;

        await this.device.getAll();

        this.deviceRemoveChecks   = await this.device.checkRemove();
        this.deviceNoDeleteReason = this.fromValidationToReason("Remove is disabled because:", this.deviceRemoveChecks);

        this.deviceUpdateChecks   = await this.device.checkUpdate();
        this.deviceNoUpdateReason = this.fromValidationToReason("Update is disabled because:", this.deviceUpdateChecks);

        this.deviceHealth = await this.app.domain.devices.getHealthByID(this.deviceID);

        await this.refreshLocation();

        let name          = this.device.model.name;
        this.bookmarkName = this.location ? `${name} in ${this.location.model.name}` : name;

        this.operationalStates = await this.app.domain.assets.getOperationalStates();

        // set breadcrumbs
        this.app.ui.navigation.breadcrumbCurrentLabel = this.device ? this.device.typedModel.productName : "<unknown>";

        // load alert history
        let history  = await this.device.getAlertHistory();
        this.history = AlertTimelineItem.createList(history.map((ext) => ext.model));

        this.existingBookmark = await this.app.domain.bookmarks.getBookmarkByRecordID(this.device.model.sysId);
        await this.loadBookmarks();

        this.deviceForm.form.markAsPristine();
        this.detectChanges();
    }

    openBookmarkOverlay()
    {
        BookmarkEditorDialogComponent.open(this, !!this.existingBookmark, this.bookmarkName,
                                           (name,
                                            description) => this.saveBookmark(name, description),
                                           this.existingBookmark);
    }

    async locationChanged(selectedLocationID: string)
    {
        this.device.setLocation(selectedLocationID);

        await this.refreshLocation();

        this.deviceForm.form.markAsDirty();
    }

    private async refreshLocation()
    {
        this.location           = await this.device.getLocation();
        this.locationModel      = this.location ? this.location.typedModel : new Models.Location();
        this.locationModelReady = true;
    }

    async relistObjects()
    {
        await this.device.startRediscovery(true, false);
    }

    async rereadObjects()
    {
        await this.device.startRediscovery(false, true);
    }

    @ReportError
    async save()
    {
        await this.device.save();

        this.app.framework.errors.success("Device updated", -1);

        await this.cancel();
    }

    async cancel()
    {
        await this.device.refresh();

        await this.loadDevice();
    }

    @ReportError
    async remove()
    {
        if (this.device)
        {
            if (await this.confirmOperation("Click Yes to confirm deletion of this Device."))
            {
                let name = this.device.model.name || this.device.model.sysId;
                let msg  = this.app.framework.errors.success(`Deleting device '${name}'...`, -1);

                let promise = this.device.remove();

                // Navigate away without waiting for deletion, since it can take a long time.
                this.exit();

                if (await promise)
                {
                    this.app.framework.errors.dismiss(msg);
                    this.app.framework.errors.success(`Device '${name}' deleted`, -1);
                }
            }
        }
    }

    async loadBookmarks()
    {
        let bookmarks         = await this.app.domain.bookmarks.getBookmarksByParentRecordID(this.device.model.sysId);
        this.childCpBookmarks = bookmarks.map((bookmark) => new BookmarkSet(bookmark));
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
            this.existingBookmark = await this.app.domain.bookmarks.generateBookmark(this.bookmarkName, description, Models.BookmarkType.DEVICE, this.device.model.sysId);
        }

        if (this.existingBookmark) this.app.framework.errors.success("Bookmark saved", -1);

        return this.existingBookmark;
    }

    exportToExcel()
    {
        let fileName       = DownloadDialogComponent.fileName(this.device.model.name + "_control_points", ".xlsx");
        let dataDownloader = new DeviceElementDataExporter(this.app.domain, fileName, this.deviceID);
        DownloadDialogComponent.openWithGenerator(this, "Export Device Elements", fileName, dataDownloader);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    viewAlert(item: TimelineItem): void
    {
        if (item)
        {
            this.app.ui.navigation.go("/alerts/alert", [item.eventId]);
        }
    }
}
