import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-refresh-step",
               templateUrl: "./widget-editor-wizard-refresh-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardRefreshStepComponent)]
           })
export class WidgetEditorWizardRefreshStepComponent extends WizardStep<WidgetEditorWizardState>
{
    refresh: boolean    = false;
    refreshRate: number = 5 * 60;

    refreshOptions: ControlOption<number>[] = [
        new ControlOption(60, "1 Minute"),
        new ControlOption(5 * 60, "5 Minutes"),
        new ControlOption(15 * 60, "15 Minutes")
    ];

    public updateRefreshRate()
    {
        this.data.editor.widget.refreshRateInSeconds = this.refresh ? this.refreshRate : null;
    }

    public async onData()
    {
        await super.onData();

        let refreshRate = this.data.editor.widget.refreshRateInSeconds;
        this.refresh    = !!refreshRate;
        if (this.refresh) this.refreshRate = refreshRate;
    }

    public getLabel()
    {
        return "Refresh Rate";
    }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration,
                                                 Models.AggregationWidgetConfiguration,
                                                 Models.AggregationTrendWidgetConfiguration,
                                                 Models.AlertMapWidgetConfiguration,
                                                 Models.AlertFeedWidgetConfiguration,
                                                 Models.AlertTableWidgetConfiguration);
    }

    public isValid()
    {
        return !!this.data.editor.widget;
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
