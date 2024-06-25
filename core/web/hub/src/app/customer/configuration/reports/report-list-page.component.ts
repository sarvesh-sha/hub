import {Component, Injector} from "@angular/core";

import {ReportWizardDialogComponent, ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import {AzuredigitaltwinExporter} from "app/customer/data-collection/networks/azuredigitaltwin-exporter";
import {DiscoveryDataExporter} from "app/customer/data-collection/networks/discovery-data-exporter";
import {NetworkExtended} from "app/services/domain/assets.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ReportDefinitionVersionExtended} from "app/services/domain/report-definition-versions.service";
import {ReportDefinitionExtended} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";

import {Lookup, UtilsService} from "framework/services/utils.service";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {FilterChip} from "framework/ui/shared/filter-chips-container.component";

@Component({
               selector   : "o3-report-list-page",
               templateUrl: "./report-list-page.component.html"
           })
export class ReportListPageComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, ReportDefinitionExtended, ReportDefinitionRecord>
{
    table: DatatableManager<Models.RecordIdentity, ReportDefinitionExtended, ReportDefinitionRecord>;

    filters             = new Models.DeviceElementFilterRequest();
    chips: FilterChip[] = [];

    private m_reportFilters: Models.ReportDefinitionFilterRequest;

    constructor(inj: Injector)
    {
        super(inj);
        this.table = this.newTableWithAutoRefresh(this.app.domain.reportDefinitions, this);
    }

    public getItemName(): string
    {
        return "Reports";
    }

    public getTableConfigId(): string { return "reports"; }

    public async getList(): Promise<Models.RecordIdentity[]>
    {
        if (!this.m_reportFilters)
        {
            this.m_reportFilters = new Models.ReportDefinitionFilterRequest();
        }

        let filters    = this.m_reportFilters;
        filters.sortBy = this.mapSortBindings(this.table.sort);

        return await this.app.domain.reportDefinitions.getList(filters);
    }

    public async getPage(offset: number,
                         limit: number): Promise<ReportDefinitionExtended[]>
    {
        return this.app.domain.reportDefinitions.getPageFromTable(this.table, offset, limit);
    }

    public itemClicked(columnId: string,
                       item: ReportDefinitionRecord)
    {
        switch (columnId)
        {
            case "active":
                return;

            default:
                this.app.ui.navigation.push([
                                                "report",
                                                item.id
                                            ]);
        }
    }

    public async transform(rows: ReportDefinitionExtended[]): Promise<ReportDefinitionRecord[]>
    {
        let reports: ReportDefinitionRecord[] = [];
        for (let report of rows)
        {
            let currentVersion = await report.getHead();
            if (!currentVersion) continue;

            reports.push({
                             report     : report,
                             description: report.model.description,
                             title      : report.model.title,
                             id         : report.model.sysId,
                             isActive   : report.model.active
                         });
        }

        return reports;
    }

    public async exportDiscoveryReport(filters: Models.DeviceElementFilterRequest)
    {
        let allNetworks = await this.app.domain.assets.getTypedExtendedAll(NetworkExtended, new Models.NetworkFilterRequest());

        let fileName       = DownloadDialogComponent.fileName("discovery", ".xlsx");
        let dataDownloader = new DiscoveryDataExporter(this.app.domain, fileName, allNetworks, filters);
        DownloadDialogComponent.openWithGenerator(this, "Export Reports", fileName, dataDownloader);
    }

    public async publishToADT()
    {
        let fileName       = DownloadDialogComponent.fileName("AzureDigitalTwinReport", ".txt");
        let dataDownloader = new AzuredigitaltwinExporter(this.app.domain, fileName);
        DownloadDialogComponent.openWithGenerator(this, "Publish to Azure Digital Twin", fileName, dataDownloader);
    }

    public async export()
    {
        let list           = await this.app.domain.reportDefinitions.getList(new Models.ReportDefinitionFilterRequest());
        let allDefinitions = await this.app.domain.reportDefinitions.getExtendedBatch(list);

        let allVersions: ReportDefinitionVersionExtended[] = [];
        for (let definition of allDefinitions)
        {
            allVersions.push(await definition.getRelease());
        }

        let timestamp = MomentHelper.fileNameFormat();
        DownloadDialogComponent.open<ReportDefinitionImportExport>(this, "Export Reports", `reports__${timestamp}.json`, {
            reportDefinitions       : allDefinitions.map((d) => d.model),
            reportDefinitionVersions: allVersions.map((v) => v.model)
        });
    }

    public async import()
    {
        let result = await ImportDialogComponent.open(this, "Import Reports", new ReportDefinitionImportHandler(this.app.domain));
        if (result)
        {
            await this.importDefinition(result);
        }
    }

    private async importDefinition(contents: ReportDefinitionImportExport)
    {
        let list           = await this.app.domain.reportDefinitions.getList(new Models.ReportDefinitionFilterRequest());
        let allDefinitions = await this.app.domain.reportDefinitions.getExtendedBatch(list);
        let existingTitles = allDefinitions.map((d) => d.model.title);

        let {
                reportDefinitions,
                reportDefinitionVersions
            } = contents;
        if (reportDefinitions && reportDefinitionVersions && reportDefinitionVersions.length)
        {
            let savedDefinitions: Lookup<ReportDefinitionExtended>     = {};
            let savedVersions: Lookup<ReportDefinitionVersionExtended> = {};

            for (let definition of reportDefinitions)
            {
                let oldId         = definition.sysId;
                definition.sysId  = "";
                definition.active = false;

                let matchingDef = allDefinitions.find((d) => d.model.title === definition.title);
                if (matchingDef)
                {
                    definition.title = UtilsService.getUniqueTitle(definition.title, existingTitles);
                    existingTitles.push(definition.title);
                }

                let definitionExtended = this.app.domain.reportDefinitions.wrapModel(definition);
                definitionExtended     = await definitionExtended.save();
                let newDefinitionId    = definitionExtended.model.sysId;

                savedDefinitions[oldId] = savedDefinitions[newDefinitionId] = definitionExtended;

            }

            reportDefinitionVersions.sort((a,
                                           b) => a.version - b.version);

            for (let version of reportDefinitionVersions)
            {
                let oldId     = version.sysId;
                version.sysId = "";

                version.definition.sysId = savedDefinitions[version.definition.sysId].model.sysId;

                if (version.predecessor)
                {
                    version.predecessor.sysId = savedVersions[version.predecessor.sysId]?.model.sysId;
                }

                let versionExtended = this.app.domain.reportDefinitionVersions.wrapModel(version);
                versionExtended     = await versionExtended.save();
                let newVersionId    = versionExtended.model.sysId;

                savedVersions[oldId] = savedVersions[newVersionId] = versionExtended;
                version.sysId        = newVersionId;
            }

            // Restore successor links
            for (let version of reportDefinitionVersions)
            {
                let newVersion = savedVersions[version.sysId];

                if (version.predecessor)
                {
                    let newPredecessor = savedVersions[version.predecessor.sysId];
                    if (newVersion && newPredecessor)
                    {
                        await newPredecessor.link(newVersion);
                    }
                }
            }

            for (let definition of reportDefinitions)
            {
                let newHead = savedVersions[definition.headVersion.sysId];
                if (newHead)
                {
                    await newHead.makeHead();
                }

                let newRelease = savedVersions[definition.releaseVersion.sysId];
                if (newRelease)
                {
                    await newRelease.makeRelease();
                }
            }
        }
    }

    async new()
    {
        await ReportWizardDialogComponent.open(new ReportWizardState(), this);
    }
}

class ReportDefinitionImportHandler implements ImportHandler<ReportDefinitionImportExport>
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
        let parsed: ReportDefinitionImportExport                      = JSON.parse(result);
        let validDefinitions: Models.ReportDefinition[]               = [];
        let validDefinitionVersions: Models.ReportDefinitionVersion[] = [];

        for (let definition of parsed.reportDefinitions)
        {
            let definitionImport = Models.RawImport.newInstance({contentsAsJSON: JSON.stringify(definition)});
            validDefinitions.push(await this.domain.apis.reportDefinitions.parseImport(definitionImport));
        }

        let versions = parsed.reportDefinitionVersions || [];
        for (let version of versions)
        {
            let versionImport = Models.RawImport.newInstance({contentsAsJSON: JSON.stringify(version)});
            validDefinitionVersions.push(await this.domain.apis.reportDefinitionVersions.parseImport(versionImport));
        }

        return {
            reportDefinitions       : validDefinitions,
            reportDefinitionVersions: validDefinitionVersions
        };
    }
}

export interface ReportDefinitionImportExport
{
    reportDefinitions: Models.ReportDefinition[];
    reportDefinitionVersions: Models.ReportDefinitionVersion[];
}

export class ReportDefinitionRecord
{
    report: ReportDefinitionExtended;
    id: string;
    title: string;
    description: string;
    isActive: boolean;
}
