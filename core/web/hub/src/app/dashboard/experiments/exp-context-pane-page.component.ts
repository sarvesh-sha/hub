import {Component, Injector} from "@angular/core";
import {UUID} from "angular2-uuid";

import {ContextPaneComponent} from "app/dashboard/context-pane/panes/context-pane.component";
import {ExperimentsBasePageComponent} from "app/dashboard/experiments/exp-base-page.component";
import * as Models from "app/services/proxy/model/models";
import {ControlOption} from "framework/ui/control-option";

import {DownloadDialogComponent} from "framework/ui/dialogs/download-dialog.component";
import {ImportDialogComponent} from "framework/ui/dialogs/import-dialog.component";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

import * as PalfingerConfig from "./pane-palfinger.json";
import * as SimulatedAHUConfig from "./pane-simulated-ahu.json";
import * as SimulatedVAVConfig from "./pane-simulated-vav.json";
import * as SwiftConfig from "./pane-swift.json";

@Component({
               selector   : "o3-experiments-context-pane-page",
               templateUrl: "./exp-context-pane-page.component.html",
               styleUrls  : ["./exp-context-pane-page.component.scss"]
           })
export class ExperimentsContextPanePageComponent extends ExperimentsBasePageComponent
{
    contextPaneDialogConfig: OverlayConfig;

    selectedSample: string;

    dynamicModels: Models.Pane[];
    dynamicConfigRaw: string = "";
    dynamicConfig: Models.PaneConfiguration;
    dynamicPossibleAsset: ControlOption<string>[];
    dynamicSelectedAsset: string;


    constructor(inj: Injector)
    {
        super(inj);

        this.contextPaneDialogConfig = ContextPaneComponent.getOverlayConfig(this.app.ui.overlay);
    }

    async ngOnInit()
    {
        super.ngOnInit();
        if (this.dynamicConfig)
        {
            this.dynamicConfigRaw = JSON.stringify(this.dynamicConfig, null, 4);
        }
    }

    async upload()
    {
        if (!this.dynamicConfig.id)
        {
            this.dynamicConfig.id = UUID.UUID();
        }
        await this.app.domain.panes.saveConfig(this.dynamicConfig);
    }

    async validate()
    {
        try
        {
            this.dynamicConfig   = await this.parse(this.dynamicConfigRaw);
            let possibleAssetIds = await this.app.domain.panes.getValidAssets(this.dynamicConfig);
            for (let options of possibleAssetIds.values())
            {
                let assets = await this.app.domain.assets.getExtendedBatch(options);
                if (assets)
                {
                    this.dynamicPossibleAsset = assets.map((asset) => new ControlOption<string>(asset.model.sysId, asset.model.name));
                    this.dynamicSelectedAsset = this.dynamicPossibleAsset.length > 0 ? this.dynamicPossibleAsset[0].id : null;
                }
            }

        }
        catch (err)
        {
            this.app.framework.errors.error("Error", "Error parsing JSON");
        }
    }

    async test()
    {
        let context        = Models.AssetGraphContextAsset.newInstance({sysId: this.dynamicSelectedAsset});
        this.dynamicModels = await this.app.domain.panes.evaluate(this.dynamicConfig, [context]);
    }

    async import()
    {
        let importedConfig = await ImportDialogComponent.open(this, "Import Pane Configuration", {
            returnRawBlobs: () => false,
            parseFile     : async (contents: string) => this.parse(contents)
        });

        if (importedConfig)
        {
            this.dynamicConfig    = importedConfig;
            this.dynamicConfigRaw = JSON.stringify(this.dynamicConfig, null, 4);
        }
    }

    async export()
    {
        return DownloadDialogComponent.open(this, "Export Pane Configuration", "pane-config.json", this.dynamicConfig);
    }

    selectedSampleChanged()
    {
        if (this.selectedSample === "palfinger")
        {
            this.dynamicConfig = <any>PalfingerConfig;
        }
        else if (this.selectedSample === "swift")
        {
            this.dynamicConfig = <any>SwiftConfig;
        }
        else if (this.selectedSample === "simulated_ahu")
        {
            this.dynamicConfig = <any>SimulatedAHUConfig;
        }
        else if (this.selectedSample === "simulated_vav")
        {
            this.dynamicConfig = <any>SimulatedVAVConfig;
        }
        else
        {
            return;
        }

        this.dynamicConfigRaw = JSON.stringify(this.dynamicConfig, null, 4);
    }

    private parse(contents: string): Promise<Models.PaneConfiguration>
    {
        return this.app.domain.panes.parseImport(contents);
    }
}
