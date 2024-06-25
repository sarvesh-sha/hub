import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/panes/wizard/pane-field-wizard/pane-field-wizard.component";

import {DatatableSelectionChangeSummary} from "framework/ui/datatables/datatable-manager";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-pane-field-type-step",
               templateUrl: "./pane-field-wizard-type-step.component.html",
               providers  : [WizardStep.createProvider(PaneFieldWizardTypeStepComponent)]
           })
export class PaneFieldWizardTypeStepComponent extends WizardStep<CustomFieldData>
{
    fieldSelection: Set<string> = new Set(["aggregatedValue"]);

    public getLabel() { return "Type"; }

    public isEnabled()
    {
        return this.data && !!this.data.fieldType;
    }

    public isValid()
    {
        return true;
    }

    public isNextJumpable()
    {
        return true;
    }

    public async onNext()
    {
        return false;
    }

    public async onStepSelected()
    {
    }

    updateFieldSelection(change: DatatableSelectionChangeSummary<string>)
    {
        this.data.fieldType = change.target;
        this.fieldSelection = new Set([this.data.fieldType]);

        this.data.typeChanged();
    }
}
