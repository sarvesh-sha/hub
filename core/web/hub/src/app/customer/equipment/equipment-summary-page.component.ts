import {Component, ViewChild} from "@angular/core";

import {EquipmentDataExporter} from "app/customer/equipment/equipment-data-exporter";
import {EquipmentListComponent} from "app/customer/equipment/equipment-list.component";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {EquipmentFiltersAdapterComponent} from "app/shared/filter/asset/equipment-filters-adapter.component";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-equipment-summary-page",
               templateUrl: "./equipment-summary-page.component.html"
           })
export class EquipmentSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    filtersLoaded: boolean;
    localFiltering: boolean;
    chips: FilterChip[];
    filters: Models.AssetFilterRequest;

    hasFilters: boolean;

    workflowOverlayConfig = OverlayConfig.newInstance({containerClasses: ["dialog-xl"]});

    private m_filtersAdapter: EquipmentFiltersAdapterComponent;
    @ViewChild(EquipmentFiltersAdapterComponent) set filtersAdapter(adapter: EquipmentFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): EquipmentFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    @ViewChild(EquipmentListComponent) equipmentList: EquipmentListComponent;

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let locationId                 = this.getPathParameter("locationID");
        this.localFiltering            = !!locationId;
        this.filters                   = new Models.AssetFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering) this.filters.locationIDs = locationId.split(",");

        this.filtersLoaded = true;
    }

    refresh()
    {
        this.filters.locationInclusive = false;

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }

    exportToExcel()
    {
        let fileName       = DownloadDialogComponent.fileName("equipment_summary", ".xlsx");
        let dataDownloader = new EquipmentDataExporter(this.app.domain, fileName);
        DownloadDialogComponent.openWithGenerator(this, "Export Equipment", fileName, dataDownloader);
    }
}
