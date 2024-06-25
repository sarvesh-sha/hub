import {Component, ElementRef, ViewChild} from "@angular/core";

import {AssetStructureWizardDialogComponent, AssetStructureWizardState} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DatatableManager, IDatatableDataProvider} from "framework/ui/datatables/datatable-manager";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector   : "o3-asset-structure-list",
               templateUrl: "./asset-structure-list-page.component.html"
           })
export class AssetStructureListPageComponent extends BaseApplicationComponent implements IDatatableDataProvider<string, Models.SharedAssetGraph, Models.SharedAssetGraph>
{
    table: DatatableManager<string, Models.SharedAssetGraph, Models.SharedAssetGraph> = new DatatableManager(this, () => this.getViewState());

    readonly newStructureLabel = "New Asset Structure...";

    @ViewChild("test_tabGroup") test_tabGroup: TabGroupComponent;
    @ViewChild("test_assetStructureList", {read: ElementRef}) test_assetStructureList: ElementRef;

    public ngOnInit(): void
    {
        super.ngOnInit();

        this.table.refreshData();
    }

    public getItemName(): string
    {
        return "asset structures";
    }

    public getList(): Promise<string[]>
    {
        return this.app.domain.assetGraphs.getGraphIds();
    }

    public getPage(offset: number,
                   limit: number): Promise<Models.SharedAssetGraph[]>
    {
        let pageIds = this.table.slicePage(offset, limit);
        return mapInParallel(pageIds, (id) => this.app.domain.assetGraphs.getConfig(id));
    }

    public async new()
    {
        await AssetStructureWizardDialogComponent.open(new AssetStructureWizardState(this.app.domain), this);
    }

    public itemClicked(columnId: string,
                       item: Models.SharedAssetGraph): void
    {
        this.app.ui.navigation.push([item.id]);
    }

    public async transform(rows: Models.SharedAssetGraph[]): Promise<Models.SharedAssetGraph[]>
    {
        return rows;
    }

    public async import()
    {
        let graph = await ImportDialogComponent.open(this, "Import Asset Structure Configuration", {
            returnRawBlobs: () => false,
            parseFile     : (contents: string): Promise<Models.SharedAssetGraph> => this.app.domain.assetGraphs.parseImport(contents)
        });

        if (graph)
        {
            await this.app.domain.assetGraphs.saveConfig(graph);
            await this.table.refreshData();
        }
    }
}
