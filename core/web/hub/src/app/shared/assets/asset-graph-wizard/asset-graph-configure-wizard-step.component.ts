import {Component} from "@angular/core";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";

import * as Models from "app/services/proxy/model/models";

import {AssetGraphWizardState} from "app/shared/assets/asset-graph-wizard/asset-graph-wizard.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-asset-graph-configure-wizard-step",
               templateUrl: "./asset-graph-configure-wizard-step.component.html",
               providers  : [
                   WizardStep.createProvider(AssetGraphConfigureWizardStep)
               ]
           })
export class AssetGraphConfigureWizardStep extends WizardStep<AssetGraphWizardState>
{
    public getLabel(): string
    {
        return "Configure Structure";
    }

    public isEnabled(): boolean
    {
        return !!this.data;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.data?.graph) return false;

        return !!this.data.graph.sharedGraphs?.length;
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.markForCheck();
    }

    public onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected()
    {
    }

    public get graph(): Models.AssetGraph
    {
        if (this.data.graph.sharedGraphs?.length)
        {
            return this.data.graph.sharedGraphs[0].graph;
        }

        return null;
    }

    public set graph(graph: Models.AssetGraph)
    {
        this.data.graph.sharedGraphs = [SharedAssetGraphExtended.newModel(graph, null, null)];
    }
}
