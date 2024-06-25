import {Component, Injector, ViewChild} from "@angular/core";

import {ReportError} from "app/app.service";

import {LegacyImportsListComponent} from "app/customer/configuration/legacy-imports/legacy-imports-list.component";

import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";

@Component({
               selector   : "o3-legacy-imports-summary-page",
               templateUrl: "./legacy-imports-summary-page.component.html"
           })
export class LegacyImportsSummaryPageComponent extends SharedSvc.BaseApplicationComponent
{
    @ViewChild("legacyList", {static: true}) legacyList: LegacyImportsListComponent;

    constructor(inj: Injector)
    {
        super(inj);
    }

    @ReportError
    async importData()
    {
        let result = await ImportDialogComponent.open(this, "Legacy Data Import", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string) =>
            {
                let raw = Models.RawImport.newInstance({contentsAsJSON: contents});
                return this.app.domain.apis.dataImports.parseImport(raw);
            }
        });

        if (result != null)
        {
            let model = await this.app.domain.apis.dataImports.create(result);

            this.app.ui.navigation.go("/legacy-imports/item", [model.sysId]);
        }
    }
}


