import {Component, Injector, ViewChild} from "@angular/core";

import {HostsListComponent} from "app/customer/maintenance/hosts/hosts-list/hosts-list.component";
import {HostExtended} from "app/services/domain/assets.service";

import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {HostFiltersAdapterComponent} from "app/shared/filter/asset/host-filters-adapter.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-hosts-summary-page",
               templateUrl: "./hosts-summary-page.component.html"
           })
export class HostsSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    summary: Models.SummaryResult[];

    filtersLoaded: boolean;
    hasFilters: boolean;
    localFiltering: boolean;
    filters: Models.HostFilterRequest;
    chips: FilterChip[];

    charts: any = {bar: null};

    private m_filtersAdapter: HostFiltersAdapterComponent;
    @ViewChild(HostFiltersAdapterComponent) set filtersAdapter(adapter: HostFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): HostFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    @ViewChild(HostsListComponent) hostsList: HostsListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let locationId                 = this.getPathParameter("locationID");
        this.localFiltering            = !!locationId;
        this.filters                   = new Models.HostFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering) this.filters.locationIDs = locationId.split(",");

        this.filtersLoaded = true;
    }

    refresh()
    {
        // location inclusivity only allowed when passed in
        this.filters.locationInclusive = false;

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }
}

export class HostImportHandler implements ImportHandler<Models.HostAsset>
{
    constructor(private domain: AppDomainContext)
    {
    }

    returnRawBlobs(): boolean
    {
        return false;
    }

    async parseFile(result: string)
    {
        let host: Models.HostAsset = JSON.parse(result);

        let hostImport            = new Models.RawImport();
        hostImport.contentsAsJSON = JSON.stringify(host);

        let parsed = await this.domain.apis.assets.parseImport(hostImport);
        if (parsed instanceof Models.HostAsset)
        {
            parsed.sysId           = null;
            parsed.lastCheckedDate = null;
            parsed.lastUpdatedDate = null;
            parsed.createdOn       = null;
            parsed.updatedOn       = null;
            return parsed;
        }

        return null;
    }
}
