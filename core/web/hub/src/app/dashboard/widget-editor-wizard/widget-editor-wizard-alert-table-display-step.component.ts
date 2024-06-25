import {Component, ViewChild} from "@angular/core";

import {WidgetEditorWizardWidgetPreviewStep} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview-step.directive";
import * as Models from "app/services/proxy/model/models";
import {AlertSeverityConfigurerComponent} from "app/shared/alerts/alert-severity/alert-severity-configurer.component";

import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-alert-table-display-step",
               templateUrl: "./widget-editor-wizard-alert-table-display-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAlertTableDisplayStepComponent)]
           })
export class WidgetEditorWizardAlertTableDisplayStepComponent extends WidgetEditorWizardWidgetPreviewStep<Models.AlertTableWidgetConfiguration>
{
    get typedWidget(): Models.AlertTableWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.AlertTableWidgetConfiguration);
    }

    get severityColors(): Models.AlertMapSeverityColor[]
    {
        return this.typedWidget?.severityColors;
    }

    set severityColors(severityColors: Models.AlertMapSeverityColor[])
    {
        this.updateWidget((widget) => widget.severityColors = severityColors);
    }

    get groupBy(): Models.SummaryFlavor
    {
        return this.typedWidget?.groupBy;
    }

    set groupBy(groupBy: Models.SummaryFlavor)
    {
        this.updateWidget((widget) => widget.groupBy = groupBy);
    }

    get rollupType(): string
    {
        return this.typedWidget?.rollupType || "";
    }

    set rollupType(rollupType: string)
    {
        this.updateWidget((widget) => widget.rollupType = <any>rollupType || null);
    }

    groupByOptions: ControlOption<Models.SummaryFlavor>[] = [
        new ControlOption(Models.SummaryFlavor.rule, "Rule"),
        new ControlOption(Models.SummaryFlavor.severity, "Severity"),
        new ControlOption(Models.SummaryFlavor.status, "Status"),
        new ControlOption(Models.SummaryFlavor.type, "Type"),
        new ControlOption(Models.SummaryFlavor.location, "Location")
    ];

    rollupOptions: ControlOption<string>[] = [];

    @ViewChild(AlertSeverityConfigurerComponent) severityConfigurer: AlertSeverityConfigurerComponent;

    public getLabel(): string
    {
        return "Display";
    }

    public isEnabled(): boolean
    {
        return !!this.typedWidget;
    }

    public isValid(): boolean
    {
        if (!this.severityConfigurer) return true;
        return this.severityConfigurer.valid;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onData(): Promise<void>
    {
        super.onData();

        let widget = this.typedWidget;
        if (widget && !widget.groupBy) widget.groupBy = Models.SummaryFlavor.location;
    }

    public async onStepSelected()
    {
        this.previewConfig = Models.AlertTableWidgetConfiguration.deepClone(this.typedWidget);
        if (!this.rollupOptions.length)
        {
            this.rollupOptions = await this.data.editor.app.bindings.getUsedLocationTypeOptions();
            this.rollupOptions.unshift(new ControlOption("", "None"));
        }
    }

    protected updatePreviewConfig(): void
    {
        this.previewConfig = Models.AlertTableWidgetConfiguration.newInstance(this.previewConfig);
    }
}
