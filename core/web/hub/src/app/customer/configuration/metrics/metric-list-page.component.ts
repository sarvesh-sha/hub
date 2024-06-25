import {Component, Injector} from "@angular/core";
import {MetricsWizardState, MetricWizardDialogComponent} from "app/customer/configuration/metrics/wizard/metric-wizard-dialog.component";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import {MetricsDefinitionExtended} from "app/services/domain/metrics-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-metric-list-page",
               templateUrl: "./metric-list-page.component.html",
               styleUrls  : ["./metric-list-page.component.scss"]
           })
export class MetricListPageComponent extends SharedSvc.BaseApplicationComponent implements IDatatableDataProvider<Models.RecordIdentity, MetricsDefinitionExtended, MetricsDefinitionRecord>
{
    table: DatatableManager<Models.RecordIdentity, MetricsDefinitionExtended, MetricsDefinitionRecord>;

    constructor(inj: Injector)
    {
        super(inj);
        this.table = this.newTableWithAutoRefresh(this.app.domain.metricsDefinitions, this);
    }

    public getTableConfigId(): string { return "metrics"; }

    public getItemName(): string
    {
        return "Metrics";
    }

    public async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.getFilters();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        return this.app.domain.metricsDefinitions.getList(filters);
    }

    public getPage(offset: number,
                   limit: number): Promise<MetricsDefinitionExtended[]>
    {
        return this.app.domain.metricsDefinitions.getPageFromTable(this.table, offset, limit);
    }

    public itemClicked(columnId: string,
                       item: MetricsDefinitionRecord)
    {
        this.app.ui.navigation.push([
                                        "metric",
                                        item.id
                                    ]);
    }

    public async transform(rows: MetricsDefinitionExtended[]): Promise<MetricsDefinitionRecord[]>
    {
        return rows.map((metrics) =>
                        {
                            return {
                                metrics: metrics,
                                id     : metrics.model.sysId
                            };
                        });
    }

    async new()
    {
        await MetricWizardDialogComponent.open(new MetricsWizardState(), this);
    }

    public async export()
    {
        let allDefinitions = await this.app.domain.metricsDefinitions.getExtendedList(this.getFilters());

        let exports: MetricsDefinitionImportExport[] = [];
        for (let definition of allDefinitions)
        {
            let version = await definition.getRelease();

            exports.push({
                             definition: definition.getExport(),
                             details   : version.getDetails()
                         });
        }

        DownloadDialogComponent.open<MetricsDefinitionImportExport[]>(this, "Export Metrics", DownloadDialogComponent.fileName("metrics"), exports);
    }

    public async deleteAll()
    {
        let reallySure = await this.confirmOperation("Do you really want to delete all metrics? This action cannot be undone.");
        if (reallySure)
        {
            let allMetrics = await this.app.domain.metricsDefinitions.getList();
            await inParallel(allMetrics, async (metrics) => { await this.app.domain.apis.metricsDefinitions.remove(metrics.sysId, false); });
        }
    }

    public async import()
    {
        let result = await ImportDialogComponent.open(this, "Import Metrics", new MetricsDefinitionImportHandler(this.app.domain));
        if (result)
        {
            await this.importDefinition(result);
        }
    }

    private async importDefinition(contents: MetricsDefinitionImportExport[])
    {
        for (let {
                     definition,
                     details
                 } of contents)
        {
            let definitionExtended = this.app.domain.metricsDefinitions.wrapModel(definition);
            definitionExtended     = await definitionExtended.save(true);

            let versionExtended           = this.app.domain.metricsDefinitionVersions.allocateInstance();
            versionExtended.model.details = details;
            versionExtended.setDefinition(definitionExtended);
            await versionExtended.save();
        }
    }

    private getFilters(): Models.MetricsDefinitionFilterRequest
    {
        let filters = new Models.MetricsDefinitionFilterRequest();

        return filters;
    }
}

export class MetricsDefinitionImportHandler implements ImportHandler<MetricsDefinitionImportExport[]>
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
        let parsed: MetricsDefinitionImportExport[]       = JSON.parse(result);
        let validImports: MetricsDefinitionImportExport[] = [];

        for (let {
                     definition,
                     details
                 } of parsed)
        {
            let definitionImport            = new Models.RawImport();
            definitionImport.contentsAsJSON = JSON.stringify(definition);
            let detailsImport               = new Models.RawImport();
            detailsImport.contentsAsJSON    = JSON.stringify(details);

            let validImport: MetricsDefinitionImportExport = {
                definition: await this.domain.apis.metricsDefinitions.parseImport(definitionImport),
                details   : await this.domain.apis.metricsDefinitionVersions.parseImport(detailsImport)
            };

            validImports.push(validImport);
        }

        return validImports;
    }
}

export interface MetricsDefinitionImportExport
{
    definition: Models.MetricsDefinition;
    details: Models.MetricsDefinitionDetails;
}

class MetricsDefinitionRecord
{
    metrics: MetricsDefinitionExtended;
    id: string;
}
