import {Component, Injector} from "@angular/core";
import {AlertRuleWizardDialogComponent, AlertRuleWizardState} from "app/customer/configuration/alert-rules/wizard/alert-rule-wizard-dialog.component";
import {AlertDefinitionExtended} from "app/services/domain/alert-definitions.service";
import * as SharedSvc from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent, ImportHandler} from "framework/ui/dialogs/import-dialog.component";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {inParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-alert-rule-list-page",
               templateUrl: "./alert-rule-list-page.component.html",
               styleUrls  : ["./alert-rule-list-page.component.scss"]
           })
export class AlertRuleListPageComponent extends SharedSvc.BaseComponentWithRouter implements IDatatableDataProvider<Models.RecordIdentity, AlertDefinitionExtended, AlertDefinitionRecord>
{
    table: DatatableManager<Models.RecordIdentity, AlertDefinitionExtended, AlertDefinitionRecord>;
    isLibrary: boolean;

    constructor(inj: Injector)
    {
        super(inj);
        this.table = this.newTableWithAutoRefresh(this.app.domain.alertDefinitions, this);
    }

    public label()
    {
        return `Alert Rules ${this.isLibrary ? "Library" : ""}`;
    }

    public getTableConfigId(): string { return "alert-rules"; }

    protected onNavigationComplete()
    {
        // Disabled for now until we revisit library concept
        /*
        this.isLibrary = this.getDataParameter("isLibrary");
        if (this.isLibrary)
        {
            this.app.ui.navigation.breadcrumbCurrentLabel = "Library";
        }
        */
    }

    public manageLibrary()
    {
        this.app.ui.navigation.push(["library"], []);
    }

    public getItemName(): string
    {
        return "Alert Rules";
    }

    public async getList(): Promise<Models.RecordIdentity[]>
    {
        let filters    = this.getFilters();
        filters.sortBy = this.mapSortBindings(this.table.sort);

        return this.app.domain.alertDefinitions.getList(filters);
    }

    public getPage(offset: number,
                   limit: number): Promise<AlertDefinitionExtended[]>
    {
        return this.app.domain.alertDefinitions.getPageFromTable(this.table, offset, limit);
    }

    public itemClicked(columnId: string,
                       item: AlertDefinitionRecord)
    {
        switch (columnId)
        {
            case "active":
                return;
        }

        this.app.ui.navigation.push([
                                        "alert-rule",
                                        item.id
                                    ]);
    }

    public async transform(rows: AlertDefinitionExtended[]): Promise<AlertDefinitionRecord[]>
    {
        return rows.map((alert) =>
                        {
                            return {
                                alert: alert,
                                id   : alert.model.sysId
                            };
                        });
    }

    async new()
    {
        await AlertRuleWizardDialogComponent.open(new AlertRuleWizardState(), this);
    }

    public async export()
    {
        let allDefinitions = await this.app.domain.alertDefinitions.getExtendedList(this.getFilters());

        let exports: AlertDefinitionImportExport[] = [];
        for (let definition of allDefinitions)
        {
            let version = await definition.getRelease();

            exports.push({
                             definition: definition.getExport(),
                             details   : version.getDetails()
                         });
        }

        let timestamp = MomentHelper.fileNameFormat();
        DownloadDialogComponent.open<AlertDefinitionImportExport[]>(this, "Export Alert Rules", `alert-rules__${timestamp}.json`, exports);
    }

    public async deleteAll()
    {
        let reallySure = await this.confirmOperation("Do you really want to delete all alert rules? This action cannot be undone.");
        if (reallySure)
        {
            let allRules = await this.app.domain.alertDefinitions.getList();
            await inParallel(allRules, async (rule) => { await this.app.domain.apis.alertDefinitions.remove(rule.sysId, false); });
        }
    }

    public async import()
    {
        let result = await ImportDialogComponent.open(this, "Import Alert Rules", new AlertDefinitionImportHandler(this.app.domain));
        if (result)
        {
            await this.importDefinition(result);
        }
    }

    private async importDefinition(contents: AlertDefinitionImportExport[])
    {
        for (let {
                     definition,
                     details
                 } of contents)
        {
            let definitionExtended = this.app.domain.alertDefinitions.wrapModel(definition);
            definitionExtended     = await definitionExtended.save();

            let versionExtended           = this.app.domain.alertDefinitionVersions.allocateInstance();
            versionExtended.model.details = details;
            versionExtended.setDefinition(definitionExtended);
            await versionExtended.save();
        }
    }

    private getFilters(): Models.AlertDefinitionFilterRequest
    {
        let filters      = new Models.AlertDefinitionFilterRequest();
        filters.purposes = [];
        if (this.isLibrary)
        {
            filters.purposes.push(Models.AlertDefinitionPurpose.Library);
        }
        else
        {
            filters.purposes.push(Models.AlertDefinitionPurpose.Definition);
        }

        return filters;
    }
}

export class AlertDefinitionImportHandler implements ImportHandler<AlertDefinitionImportExport[]>
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
        let parsed: AlertDefinitionImportExport[]       = JSON.parse(result);
        let validImports: AlertDefinitionImportExport[] = [];

        for (let {
                     definition,
                     details
                 } of parsed)
        {
            let definitionImport            = new Models.RawImport();
            definitionImport.contentsAsJSON = JSON.stringify(definition);
            let detailsImport               = new Models.RawImport();
            detailsImport.contentsAsJSON    = JSON.stringify(details);

            let validImport: AlertDefinitionImportExport = {
                definition: await this.domain.apis.alertDefinitions.parseImport(definitionImport),
                details   : await this.domain.apis.alertDefinitionVersions.parseImport(detailsImport)
            };

            validImports.push(validImport);
        }

        return validImports;
    }
}

export interface AlertDefinitionImportExport
{
    definition: Models.AlertDefinition;
    details: Models.AlertDefinitionDetails;
}

class AlertDefinitionRecord
{
    alert: AlertDefinitionExtended;
    id: string;
}
