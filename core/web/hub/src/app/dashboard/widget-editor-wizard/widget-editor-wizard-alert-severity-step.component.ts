import {Component, ViewChild} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {AlertSeverityConfigurerComponent} from "app/shared/alerts/alert-severity/alert-severity-configurer.component";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-severity-step",
               templateUrl: "./widget-editor-wizard-alert-severity-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertSeverityStepComponent)]
           })
export class WidgetEditorWizardAlertSeverityStepComponent extends WizardStep<WidgetEditorWizardState>
{
    get typedWidget(): Models.AlertTableWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AlertTableWidgetConfiguration);
    }

    @ViewChild(AlertSeverityConfigurerComponent) severityConfigurer: AlertSeverityConfigurerComponent;

    public getLabel(): string
    {
        return "Alert Severity";
    }

    public isEnabled(): boolean
    {
        return !!this.typedWidget;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.severityConfigurer) return true;
        return this.severityConfigurer.valid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected(): Promise<void>
    {
    }
}
