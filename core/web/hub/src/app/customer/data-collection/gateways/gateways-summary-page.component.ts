import {Component, Injector, ViewChild} from "@angular/core";

import {GatewayExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {ExportsApi} from "app/services/proxy/api/ExportsApi";
import * as Models from "app/services/proxy/model/models";
import {GatewayFiltersAdapterComponent} from "app/shared/filter/asset/gateway-filters-adapter.component";
import {ExcelExporter} from "app/shared/utils/excel-exporter";

import {DownloadDialogComponent, DownloadGenerator, DownloadResults} from "framework/ui/dialogs/download-dialog.component";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-gateways-summary-page",
               templateUrl: "./gateways-summary-page.component.html"
           })
export class GatewaysSummaryPageComponent extends SharedSvc.BaseComponentWithRouter
{
    summary: Models.SummaryResult[];

    filtersLoaded: boolean;
    localFiltering: boolean;
    filters: Models.GatewayFilterRequest;
    chips: FilterChip[];

    charts: any = {bar: null};

    @ViewChild(GatewayFiltersAdapterComponent) filtersAdapter: GatewayFiltersAdapterComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected async onNavigationComplete()
    {
        this.filtersLoaded = false;

        // set the cache values if locationID is provided
        let locationId                 = this.getPathParameter("locationID");
        this.localFiltering            = !!locationId;
        this.filters                   = new Models.GatewayFilterRequest();
        this.filters.locationInclusive = true;

        if (this.localFiltering) this.filters.locationIDs = locationId.split(",");

        this.filtersLoaded = true;
    }

    refresh()
    {
        this.filters.locationInclusive = false;
    }

    download()
    {
        window.open(GatewayExtended.getUrlForDownload(this.app.domain, DownloadDialogComponent.fileName("GatewayLogs", ".zip")), "_blank");
    }

    async exportToExcel()
    {
        let filters       = new Models.GatewayFilterRequest();
        let exts          = await this.app.domain.assets.getTypedExtendedAll(GatewayExtended, filters);
        let fileName      = DownloadDialogComponent.fileName("gateways", ".xlsx");
        const sheetName   = "Gateway List";
        let dataGenerator = new GatewayListDownloader(this.app.domain.apis.exports, exts, fileName, sheetName);

        return DownloadDialogComponent.openWithGenerator(this, sheetName, fileName, dataGenerator);
    }
}

export class GatewayListDownloader implements DownloadGenerator
{
    constructor(private m_exportsApi: ExportsApi,
                private m_exts: GatewayExtended[],
                private m_fileName: string,
                private m_sheetName: string)
    {
    }

    public getProgressPercent(): number
    {
        return NaN;
    }

    public getProgressMessage(): string
    {
        return null;
    }

    public async makeProgress(dialog: DownloadDialogComponent): Promise<boolean>
    {
        return true;
    }

    public async sleepForProgress(): Promise<void>
    {
        // We don't need to sleep.
    }

    public isDeterminate(): boolean
    {
        return false;
    }

    public async getResults(fileName: string): Promise<DownloadResults>
    {
        let exporter = new ExcelExporter(this.m_exportsApi, this.m_sheetName);

        let dateFormatter = "m/d/yy h:mm:ss";
        exporter.addColumnHeader("Id");
        exporter.addColumnHeader("Name");
        exporter.addColumnHeader("Created On", dateFormatter);
        exporter.addColumnHeader("Last Heartbeat", dateFormatter);
        exporter.addColumnHeader("State");

        for (let ext of this.m_exts)
        {
            let row = await exporter.addRow();
            row.push(ext.typedModel.instanceId, ext.model.name, ext.model.createdOn, ext.model.lastUpdatedDate, ext.typedModel.state);
        }

        return exporter.getResults(this.m_fileName);
    }
}


