import {Component} from "@angular/core";

import {PaneWizardState} from "app/customer/configuration/panes/wizard/pane-wizard-dialog.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import {GraphConfigurationHost} from "app/shared/assets/configuration/graph-configuration-host";

import {WizardStep} from "framework/ui/wizards/wizard-step";

import {Subscription} from "rxjs";
import {Subject} from "rxjs/internal/Subject";

@Component({
               selector   : "o3-pane-wizard-graph-step",
               templateUrl: "./pane-wizard-graph-step.component.html",
               providers  : [WizardStep.createProvider(PaneWizardGraphStepComponent)]
           })
export class PaneWizardGraphStepComponent extends WizardStep<PaneWizardState>
{
    readonly graphId: string = SharedAssetGraphExtended.LOCAL_GRAPH_ID;

    nodeIdsUpdateSub: Subscription;
    graphHost: GraphConfigurationHost;

    getLabel() { return "Asset Structure"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        if (!this.data?.pane?.model) return false;
        return this.data.pane.graph?.isValid(false);
    }

    isNextJumpable()
    {
        return true;
    }

    async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
        this.buildGraphHost();
    }

    graphChanged()
    {
        this.buildGraphHost();

        this.data.pane.resetGraph();
    }

    private buildGraphHost()
    {
        if (!this.graphHost && this.data.pane.model.graph)
        {
            let graphs     = [SharedAssetGraphExtended.newModel(this.data.pane.model.graph, SharedAssetGraphExtended.LOCAL_GRAPH_ID, null)];
            this.graphHost = {
                hostContext  : "Pane",
                graphsChanged: new Subject(),
                getGraphs    : () => graphs,
                resolveGraphs: () => SharedAssetGraphExtended.loadGraphs(this.data.pane.domain, graphs),
                canRemove    : () => false,
                canRemoveNode: (graphId,
                                nodeId) => !this.data.nodeInUse(nodeId)
            };
        }
    }
}

