import {Component, Injector, ViewChild} from "@angular/core";
import {NgForm} from "@angular/forms";

import {ReportError} from "app/app.service";

import {LegacyImportsRunDialogComponent} from "app/customer/configuration/legacy-imports/legacy-imports-run-dialog.component";

import * as SharedSvc from "app/services/domain/base.service";
import {ImportedMetadataExtended} from "app/services/domain/data-imports.service";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";

@Component({
               selector   : "o3-legacy-imports-detail-page",
               templateUrl: "./legacy-imports-detail-page.component.html",
               styleUrls  : ["./legacy-imports-detail-page.component.scss"]
           })
export class LegacyImportsDetailPageComponent extends SharedSvc.BaseComponentWithRouter
{
    legacyImportId: string;
    legacyImport: ImportedMetadataExtended;

    @ViewChild("legacyForm", {static: true}) legacyForm: NgForm;

    //--//

    constructor(inj: Injector)
    {
        super(inj);
    }

    protected onNavigationComplete()
    {
        this.legacyImportId = this.getPathParameter("id");

        this.load();
    }

    load()
    {
        if (this.legacyImportId)
        {
            this.loadNormalization();
        }
    }

    async loadNormalization()
    {
        // load normalization info
        this.legacyImport = await this.app.domain.dataImports.getExtendedById(this.legacyImportId);
        if (!this.legacyImport)
        {
            this.exit();
            return;
        }

        // set breadcrumbs
        this.app.ui.navigation.breadcrumbCurrentLabel = `Version ${this.legacyImport.model.version}`;
    }

    @ReportError
    async makeActive()
    {
        await this.app.domain.apis.dataImports.makeActive(this.legacyImport.model.sysId);
        this.exit();
    }

    @ReportError
    async remove()
    {
        if (await this.confirmOperation("Click Yes to confirm deletion of this import."))
        {
            await this.app.domain.apis.dataImports.remove(this.legacyImport.model.sysId);
            this.exit();
        }
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async exportData()
    {
        await this.legacyImport.loadForExport();

        DownloadDialogComponent.open(this,
                                     "Legacy Data Export",
                                     DownloadDialogComponent.fileName("legacy_data_v" + this.legacyImport.model.version),
                                     this.legacyImport.model.metadata);
    }

    runImport()
    {
        LegacyImportsRunDialogComponent.open(this, this.legacyImportId);
    }
}
