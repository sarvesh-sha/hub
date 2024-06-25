import {Component, Injector} from "@angular/core";

import {AppContext} from "app/app.service";
import {AssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {AssetGraphWizardState} from "app/shared/assets/asset-graph-wizard/asset-graph-wizard.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-asset-graph-bindings-wizard-step",
               templateUrl: "./asset-graph-bindings-wizard-step.component.html",
               providers  : [
                   WizardStep.createProvider(AssetGraphBindingsWizardStep)
               ]
           })
export class AssetGraphBindingsWizardStep extends WizardStep<AssetGraphWizardState>
{
    private graph: Models.TimeSeriesGraphConfiguration;

    graphExt: AssetGraphExtended;

    private app: AppContext;

    constructor(inj: Injector)
    {
        super(inj);

        this.app = new AppContext(inj);
    }

    public getLabel(): string
    {
        return "Select Bindings";
    }

    public isEnabled(): boolean
    {
        return !!this.data;
    }

    public isNextJumpable(): boolean
    {
        return false;
    }

    public isValid(): boolean
    {
        return !!this.data?.graphSelections?.length;
    }

    public onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.updateGraph();
    }

    public async onStepSelected()
    {
        await this.updateGraph();
    }

    private async updateGraph()
    {
        if (!this.data) return;

        let newGraph = this.data.graph;
        if (!this.graph || AssetGraphBindingsWizardStep.extractGraph(newGraph) !== AssetGraphBindingsWizardStep.extractGraph(this.graph))
        {
            this.graph = Models.TimeSeriesGraphConfiguration.newInstance(newGraph);

            this.graphExt = new AssetGraphExtended(this.app.domain, AssetGraphBindingsWizardStep.extractGraph(this.graph));
        }

        if (this.data.graphSelections)
        {
            this.data.graphSelections = this.data.graphSelections.filter((id) => !!this.graphExt.getNodeById(id));
        }

        this.markForCheck();
    }

    private static extractGraph(graphConfig: Models.TimeSeriesGraphConfiguration): Models.AssetGraph
    {
        return graphConfig.sharedGraphs?.[0]?.graph;
    }
}
