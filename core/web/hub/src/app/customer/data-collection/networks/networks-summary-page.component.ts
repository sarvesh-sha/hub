import {Component, Injector, ViewChild} from "@angular/core";

import {NetworksListComponent} from "app/customer/data-collection/networks/networks-list/networks-list.component";
import {NetworksWizardDialogComponent, NetworkWizardState} from "app/customer/data-collection/networks/networks-wizard/networks-wizard.component";

import {SimulatedDataDialogComponent} from "app/dashboard/experiments/simulated-data-dialog.component";

import {NetworkExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";

import {NetworkFiltersAdapterComponent} from "app/shared/filter/asset/network-filters-adapter.component";

import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-networks-summary-page",
               templateUrl: "./networks-summary-page.component.html"
           })
export class NetworksSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    summary: Models.SummaryResult[];

    filtersLoaded: boolean;
    hasFilters: boolean;
    localFiltering: boolean;
    filters: Models.NetworkFilterRequest;
    chips: FilterChip[];

    charts: any = {bar: null};

    private m_filtersAdapter: NetworkFiltersAdapterComponent;
    @ViewChild(NetworkFiltersAdapterComponent) set filtersAdapter(adapter: NetworkFiltersAdapterComponent)
    {
        if (adapter && this.m_filtersAdapter !== adapter)
        {
            this.m_filtersAdapter = adapter;
            this.hasFilters       = this.m_filtersAdapter.hasFilters;
            this.detectChanges();
        }
    }

    get filtersAdapter(): NetworkFiltersAdapterComponent
    {
        return this.m_filtersAdapter;
    }

    @ViewChild(NetworksListComponent) networksList: NetworksListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        let locationId                 = this.getPathParameter("locationID");
        this.localFiltering            = !!locationId;
        this.filters                   = new Models.NetworkFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering) this.filters.locationIDs = locationId.split(",");

        this.filtersLoaded = true;
    }

    async addNetwork()
    {
        await NetworksWizardDialogComponent.open(new NetworkWizardState(this.app, null), this);
    }

    createSimulatedData()
    {
        SimulatedDataDialogComponent.open(this);
    }

    refresh()
    {
        // location inclusivity only allowed when passed in
        this.filters.locationInclusive = false;

        this.hasFilters = this.m_filtersAdapter?.hasFilters;
    }

    async importNetwork()
    {
        let result = await ImportDialogComponent.open(this, "Import Network", new NetworkImportHandler(this.app.domain));
        if (result)
        {
            let network = NetworkExtended.newInstance(this.app.domain.assets, result);
            await network.save();
        }
    }
}

export class NetworkImportHandler implements ImportHandler<Models.NetworkAsset>
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
        let network: Models.NetworkAsset = JSON.parse(result);

        let networkImport            = new Models.RawImport();
        networkImport.contentsAsJSON = JSON.stringify(network);

        let parsed = await this.domain.apis.assets.parseImport(networkImport);
        if (parsed instanceof Models.NetworkAsset)
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
