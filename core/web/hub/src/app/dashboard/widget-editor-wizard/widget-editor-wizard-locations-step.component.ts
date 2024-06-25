import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-locations-step",
               templateUrl: "./widget-editor-wizard-locations-step.component.html",
               styleUrls  : ["./widget-editor-wizard-dialog.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardLocationsStepComponent)]
           })
export class WidgetEditorWizardLocationsStepComponent extends WizardStep<WidgetEditorWizardState>
{
    private m_particularLocations: string[] = [];

    allLocations: boolean = true;

    get alertMap(): Models.AlertMapWidgetConfiguration
    {
        if (this.data.editor.widget instanceof Models.AlertMapWidgetConfiguration) return this.data.editor.widget;
        return null;
    }

    rollupOptions: ControlOption<string>[] = [];

    public getLabel()
    {
        return "Locations";
    }

    public isEnabled()
    {
        return this.data.editor.allowWidgetTypes(Models.AlertMapWidgetConfiguration,
                                                 Models.AlertFeedWidgetConfiguration,
                                                 Models.AlertSummaryWidgetConfiguration,
                                                 Models.AlertTableWidgetConfiguration,
                                                 Models.AlertTrendWidgetConfiguration,
                                                 Models.DeviceWidgetConfiguration);
    }

    public isValid()
    {
        return this.allLocations || this.data.editor.selectedLocations.length > 0;
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
        this.m_particularLocations = this.data.editor.selectedLocations;
        this.allLocations          = !this.m_particularLocations.length;
        if (this.alertMap && !this.rollupOptions.length) this.rollupOptions = await this.data.editor.app.bindings.getUsedLocationTypeOptions();
    }

    public async allLocationsChanged()
    {
        if (this.allLocations)
        {
            this.m_particularLocations         = this.data.editor.selectedLocations;
            this.data.editor.selectedLocations = [];
        }
        else
        {
            this.data.editor.selectedLocations = this.m_particularLocations;
        }

        await this.updateSelections();
    }

    public async updateSelections()
    {
        await this.data.editor.syncLocations();
        this.detectChanges();
    }
}
