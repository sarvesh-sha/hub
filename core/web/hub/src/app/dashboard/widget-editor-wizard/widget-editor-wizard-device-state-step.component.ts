import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {ALL_ENUMS_ID, triStateChange} from "app/shared/selection/enum-selection";

import {ControlOption} from "framework/ui/control-option";
import {DatatableSelectionChangeSummary} from "framework/ui/datatables/datatable-manager";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-device-state-step",
               templateUrl: "./widget-editor-wizard-device-state-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardDeviceStateStepComponent)]
           })
export class WidgetEditorWizardDeviceStateStepComponent extends WizardStep<WidgetEditorWizardState>
{
    deviceStateOptions: ControlOption<string>[] = [];
    deviceStateSelection: Set<string>           = new Set<string>();

    async ngAfterViewInit()
    {
        this.deviceStateOptions = [
            new ControlOption<string>(ALL_ENUMS_ID, "All"),
            new ControlOption<string>("Missing", "Missing"),
            new ControlOption<string>("New", "New")
        ];
    }

    public async onData()
    {
        await super.onData();

        this.deviceStateSelection = this.data.editor.readSelectionData(this.data.editor.selectedStates);
    }

    public getLabel() { return "State"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.DeviceSummaryWidgetConfiguration);
    }

    public isValid()
    {
        for (let i in this.data.editor.selectedStates)
        {
            if (this.data.editor.selectedStates.hasOwnProperty(i) && this.data.editor.selectedStates[i])
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
    }

    onOptionChange(change: DatatableSelectionChangeSummary<string>)
    {
        this.deviceStateSelection       = triStateChange(change, this.deviceStateSelection);
        this.data.editor.selectedStates = this.data.editor.writeSelectionData(this.deviceStateSelection);
        this.data.editor.syncDeviceWidgetConfig();
    }
}
