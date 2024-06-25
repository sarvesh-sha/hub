import {Component, ElementRef, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {enumControlOptionsWithAll, triStateChange} from "app/shared/selection/enum-selection";

import {ControlOption} from "framework/ui/control-option";
import {DatatableSelectionChangeSummary} from "framework/ui/datatables/datatable-manager";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-type-step",
               templateUrl: "./widget-editor-wizard-alert-type-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertTypeStepComponent)]
           })
export class WidgetEditorWizardAlertTypeStepComponent extends WizardStep<WidgetEditorWizardState>
{
    alertTypeOptions: ControlOption<string>[] = [];
    alertTypeSelection: Set<string>           = new Set<string>();
    alertTypeEnums: Models.EnumDescriptor[];

    @ViewChild("test_alerts", {read: ElementRef}) test_alerts: ElementRef;

    public getLabel() { return "Alert Types"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AlertWidgetConfiguration, Models.AlertTableWidgetConfiguration);
    }

    public isValid()
    {
        for (let key in this.data.editor.selectedAlertType)
        {
            if (this.data.editor.selectedAlertType[key])
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
        this.alertTypeSelection = this.data.editor.readSelectionData(this.data.editor.selectedAlertType);
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.alertTypeEnums   = await this.data.app.domain.alerts.describeTypes();
        this.alertTypeOptions = enumControlOptionsWithAll(this.alertTypeEnums, "Any Alert Type");
    }

    onOptionChange(change: DatatableSelectionChangeSummary<string>)
    {
        this.alertTypeSelection            = triStateChange(change, this.alertTypeSelection);
        this.data.editor.selectedAlertType = this.data.editor.writeSelectionData(this.alertTypeSelection);
        this.data.editor.syncAlertTypes();
    }
}
