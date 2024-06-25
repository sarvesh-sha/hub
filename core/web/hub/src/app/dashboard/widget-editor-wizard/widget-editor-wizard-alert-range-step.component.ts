import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-range-step",
               templateUrl: "./widget-editor-wizard-alert-range-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertRangeStepComponent)]
           })
export class WidgetEditorWizardAlertRangeStepComponent extends WizardStep<WidgetEditorWizardState>
{
    public getLabel() { return "Time range"; }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AlertFeedWidgetConfiguration);
    }

    public isValid(): boolean
    {
        return !!this.data.editor.timeRange;
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
}
