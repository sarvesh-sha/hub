import {Component, Inject, Injector} from "@angular/core";

import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {BaseApplicationComponent} from "app/services/domain/base.service";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";
import {WizardDialogComponent, WizardDialogState} from "app/shared/overlays/wizard-dialog.component";

import {OVERLAY_DATA, OverlayDialogRef} from "framework/ui/overlays/overlay-dialog";

@Component({
               templateUrl: "./asset-structure-wizard-dialog.component.html"
           })
export class AssetStructureWizardDialogComponent extends WizardDialogComponent<AssetStructureWizardState>
{
    constructor(public dialogRef: OverlayDialogRef<boolean>,
                inj: Injector,
                @Inject(OVERLAY_DATA) public data: AssetStructureWizardState)
    {
        super(dialogRef, inj, data);
    }

    public static async open(cfg: WizardDialogState,
                             base: BaseApplicationComponent): Promise<boolean>
    {
        return await super.open(cfg, base, AssetStructureWizardDialogComponent);
    }
}

export class AssetStructureWizardState extends WizardDialogState
{
    graph: SharedAssetGraphExtended;
    normalization: Models.NormalizationRules;

    constructor(public domainContext: AppDomainContext,
                graph?: SharedAssetGraphExtended,
                public readonly host?: GraphConfigurationHost,
                public readonly editableGraphName: boolean = true)
    {
        super(!graph);
        this.graph = graph || new SharedAssetGraphExtended(domainContext, new Models.SharedAssetGraph());
    }

    public async create(comp: BaseApplicationComponent,
                        goto: boolean): Promise<boolean>
    {
        // Save the model and record the result
        let result = await this.save(comp);

        // If save successful and goto set, navigate to record
        if (result && goto)
        {
            comp.app.ui.navigation.go("/configuration/asset-structures", [this.graph.id]);
        }

        // Return save result
        return result;
    }

    public async save(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            await this.graph.save();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }

    public async load(comp: BaseApplicationComponent): Promise<boolean>
    {
        try
        {
            this.normalization = await comp.app.bindings.getActiveNormalizationRules();
            return true;
        }
        catch (e)
        {
            return false;
        }
    }
}
