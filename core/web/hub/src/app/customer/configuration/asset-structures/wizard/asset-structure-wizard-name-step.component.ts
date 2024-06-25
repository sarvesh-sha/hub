import {Component, ElementRef, ViewChild} from "@angular/core";
import {AssetStructureWizardState} from "app/customer/configuration/asset-structures/wizard/asset-structure-wizard-dialog.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-asset-structure-wizard-name-step",
               templateUrl: "./asset-structure-wizard-name-step.component.html",
               providers  : [
                   WizardStep.createProvider(AssetStructureWizardNameStepComponent)
               ]
           })
export class AssetStructureWizardNameStepComponent extends WizardStep<AssetStructureWizardState>
{
    @ViewChild("test_name", {read: ElementRef}) test_name: ElementRef;

    getLabel() { return "Name"; }

    isEnabled()
    {
        return this.data.editableGraphName;
    }

    isValid()
    {
        return !!this.data?.graph?.name;
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
        let graph = this.data.graph;
        if (!graph.name)
        {
            graph.name = graph.suggestName();
        }
    }
}
