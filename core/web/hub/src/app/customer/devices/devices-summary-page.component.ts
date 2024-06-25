import {Component, Injector, ViewChild} from "@angular/core";
import {DevicesDataExporter} from "app/customer/devices/devices-data-exporter";
import {DigineousState, DigineousWizardDialogComponent} from "app/customer/devices/devices-details-page/digineous/digineous-wizard-dialog.component";
import {DevicesListComponent} from "app/customer/devices/devices-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import {BookmarkSet} from "app/services/domain/bookmark.service";
import * as Models from "app/services/proxy/model/models";
import {DeviceFiltersAdapterComponent} from "app/shared/filter/asset/device-filters-adapter.component";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-devices-summary-page",
               templateUrl: "./devices-summary-page.component.html"
           })
export class DevicesSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    filtersLoaded: boolean;

    filters: Models.DeviceFilterRequest;
    chips: FilterChip[];
    localFiltering: boolean;
    hasFilters: boolean = false;

    @ViewChild(DevicesListComponent) devicesList: DevicesListComponent;

    private m_filtersAdapter: DeviceFiltersAdapterComponent;
    @ViewChild(DeviceFiltersAdapterComponent) set filtersAdapter(adapter: DeviceFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): DeviceFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    bookmarks: BookmarkSet[] = [];

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let locationId                 = this.getPathParameter("locationID");
        this.localFiltering            = !!locationId;
        this.filters                   = new Models.DeviceFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering)
        {
            if (locationId)
            {
                this.filters.locationIDs       = locationId.split(",");
                this.filters.locationInclusive = true;
            }
        }

        await this.loadBookmarks();

        this.filtersLoaded = true;
    }

    refresh()
    {
        // location inclusivity only allowed when passed in
        this.filters.locationInclusive = false;

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }

    async loadBookmarks()
    {
        let bookmarks  = await this.app.domain.bookmarks.getBookmarksOfType(Models.BookmarkType.DEVICE);
        this.bookmarks = bookmarks.map((bookmark) => new BookmarkSet(bookmark));
    }

    exportToExcel()
    {
        let fileName       = DownloadDialogComponent.fileName("devices_summary", ".xlsx");
        let dataDownloader = new DevicesDataExporter(this.app.domain, fileName);
        DownloadDialogComponent.openWithGenerator(this, "Export Devices", fileName, dataDownloader);
    }

    //--//

    get enableDigineous()
    {
        return this.app.domain.digineous.isEnabled;
    }

    async configureDigineous()
    {
        while (true)
        {
            let cfg   = new DigineousState();
            cfg.rules = await this.app.bindings.getActiveNormalizationRules();

            if (await DigineousWizardDialogComponent.open(this, cfg))
            {
                await cfg.execute(this, this.app);
            }
            else
            {
                break;
            }
        }
    }
}
