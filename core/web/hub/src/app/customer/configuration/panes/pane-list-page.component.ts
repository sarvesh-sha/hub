import {Component, Injector} from "@angular/core";

import {PaneWizardDialogComponent, PaneWizardState} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-pane-list",
               templateUrl: "./pane-list-page.component.html"
           })
export class PaneListPageComponent extends BaseApplicationComponent implements IDatatableDataProvider<string, Models.PaneConfiguration, Models.PaneConfiguration>
{
    table: DatatableManager<string, Models.PaneConfiguration, Models.PaneConfiguration>;

    constructor(inj: Injector)
    {
        super(inj);
        this.table = new DatatableManager(this, () => this.getViewState());
    }

    public ngOnInit(): void
    {
        super.ngOnInit();

        this.table.refreshData();
    }

    public getItemName(): string
    {
        return "panes";
    }

    public getList(): Promise<string[]>
    {
        return this.app.domain.panes.getPaneIds();
    }

    public getPage(offset: number,
                   limit: number): Promise<Models.PaneConfiguration[]>
    {
        let pageIds = this.table.slicePage(offset, limit);
        return mapInParallel(pageIds, (id) => this.app.domain.panes.getConfig(id));
    }

    public async new()
    {
        await PaneWizardDialogComponent.open(new PaneWizardState(this.app.domain), this);
    }

    public itemClicked(columnId: string,
                       item: Models.PaneConfiguration): void
    {
        this.app.ui.navigation.push([item.id]);
    }

    public async transform(rows: Models.PaneConfiguration[]): Promise<Models.PaneConfiguration[]>
    {
        return rows;
    }

    public async import()
    {
        let pane = await ImportDialogComponent.open(this, "Import Panes", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string): Promise<Models.PaneConfiguration> => this.app.domain.panes.parseImport(contents)
        });

        if (pane)
        {
            await this.app.domain.panes.saveConfig(pane);
            await this.table.refreshData();
        }
    }
}
