import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {enumControlOptionsWithAll, triStateChange} from "app/shared/selection/enum-selection";

import {ControlOption} from "framework/ui/control-option";
import {DatatableSelectionChangeSummary} from "framework/ui/datatables/datatable-manager";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-status-step",
               templateUrl: "./widget-editor-wizard-alert-status-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertStatusStepComponent)]
           })
export class WidgetEditorWizardAlertStatusStepComponent extends WizardStep<WidgetEditorWizardState>
{
    alertStatusOptions: ControlOption<string>[] = [];
    alertStatusSelection: Set<string>           = new Set<string>();
    alertStatusEnums: Models.EnumDescriptor[];

    public async onData()
    {
        await super.onData();

        this.alertStatusEnums   = await this.data.app.domain.alerts.describeStates();
        this.alertStatusOptions = enumControlOptionsWithAll(this.alertStatusEnums, "Any Alert Status");

        this.readAlertStates();
    }

    public getLabel() { return "Alert Status"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AlertTableWidgetConfiguration);
    }

    public isValid()
    {
        for (let i in this.data.editor.selectedAlertStatus)
        {
            if (this.data.editor.selectedAlertStatus.hasOwnProperty(i) && this.data.editor.selectedAlertStatus[i])
            {
                return true;
            }
        }
        return false;
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
        this.readAlertStates();
    }

    readAlertStates()
    {
        this.alertStatusSelection = this.data.editor.readSelectionData(this.data.editor.selectedAlertStatus);
    }

    writeAlertStates()
    {
        this.data.editor.selectedAlertStatus = this.data.editor.writeSelectionData(this.alertStatusSelection);
        this.data.editor.syncAlertTypes();
    }

    onOptionChange(change: DatatableSelectionChangeSummary<string>)
    {
        this.alertStatusSelection = triStateChange(change, this.alertStatusSelection);
        this.writeAlertStates();
    }
}
