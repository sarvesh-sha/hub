import {Component, ViewChild} from "@angular/core";

import {AssetStructureWizardDialogComponent, AssetStructureWizardState} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BaseComponentWithRouter} from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {TabGroupComponent} from "framework/ui/tab-group/tab-group.component";

@Component({
               selector   : "o3-asset-structure-details",
               templateUrl: "./asset-structure-details-page.component.html"
           })
export class AssetStructureDetailsPageComponent extends BaseComponentWithRouter
{
    sharedGraph: Models.SharedAssetGraph;

    async onNavigationComplete()
    {
        let id                                        = this.getPathParameter("id");
        this.sharedGraph                              = await this.app.domain.assetGraphs.getConfig(id);
        this.app.ui.navigation.breadcrumbCurrentLabel = this.sharedGraph.name;
    }

    export()
    {
        DownloadDialogComponent.open(this, "Export Asset Structure Configuration", DownloadDialogComponent.fileName("asset-structure"), this.sharedGraph);
    }

    exit()
    {
        this.app.ui.navigation.pop();
    }

    async edit()
    {
        let sharedGraph = Models.SharedAssetGraph.deepClone(this.sharedGraph);
        let cfg         = new AssetStructureWizardState(this.app.domain, new SharedAssetGraphExtended(this.app.domain, sharedGraph));
        if (await AssetStructureWizardDialogComponent.open(cfg, this))
        {
            this.sharedGraph = await this.app.domain.assetGraphs.getConfig(cfg.graph.id);
        }
    }

    async remove()
    {
        if (await this.confirmOperation(`Permanently delete the Asset Structure '${this.sharedGraph.name}'?`))
        {
            await this.app.domain.assetGraphs.remove(this.sharedGraph.id);
            this.exit();
        }
    }

    @ViewChild("test_tabGroup") test_tabGroup: TabGroupComponent;
}
