import {Component, ViewChild} from "@angular/core";

import {AssetStructureWizardState} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {SharedAssetGraphExtended} from "app/services/domain/asset-graph.service";
import * as Models from "app/services/proxy/model/models";
import {AssetGraphStepComponent} from "app/shared/assets/asset-graph-step/asset-graph-step.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-asset-structure-wizard-data-step",
               templateUrl: "./asset-structure-wizard-data-step.component.html",
               providers  : [
                   WizardStep.createProvider(AssetStructureWizardDataStepComponent)
               ]
           })
export class AssetStructureWizardDataStepComponent extends WizardStep<AssetStructureWizardState>
{
    @ViewChild("test_graphStep") test_graphStep: AssetGraphStepComponent;

    graph: Models.AssetGraph;

    getLabel() { return "Data"; }

    isEnabled()
    {
        return true;
    }

    isValid()
    {
        return !!this.data?.graph?.isValid(false);
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
        this.graph = this.data.graph.model;
    }

    public sync()
    {
        let model       = this.data.graph.modelClone();
        model.graph     = this.graph;
        this.data.graph = new SharedAssetGraphExtended(this.data.domainContext, model);
    }
}

