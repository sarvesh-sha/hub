import {Component} from "@angular/core";

import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-font-scaling-step",
               templateUrl: "./widget-editor-wizard-font-scaling-step.component.html",
               styleUrls  : ["./widget-editor-wizard-font-scaling-step.component.scss"],
               providers  : [WizardStep.createProvider(WidgetEditorWizardFontScalingStepComponent)]
           })
export class WidgetEditorWizardFontScalingStepComponent extends WizardStep<WidgetEditorWizardState>
{
    private get widget(): Models.WidgetConfiguration
    {
        return this.data.editor.widget;
    }

    fontMultiplierValid: boolean = true;

    get manualFontScaling(): boolean
    {
        return this.widget.manualFontScaling;
    }

    set manualFontScaling(manual: boolean)
    {
        this.widget.manualFontScaling = manual;
        this.widget.fontMultiplier    = manual ? this.m_fontMultiplier : 0;
    }

    private m_fontMultiplier: number = 1;
    get fontMultiplier(): number
    {
        return this.m_fontMultiplier;
    }

    set fontMultiplier(multiplier: number)
    {
        this.m_fontMultiplier      = multiplier;
        this.widget.fontMultiplier = multiplier;
    }

    public getLabel(): string
    {
        return "Font Scaling";
    }

    public isEnabled(): boolean
    {
        return this.data.editor.allowWidgetTypes(Models.AggregationWidgetConfiguration);
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return !this.manualFontScaling || this.fontMultiplierValid;
    }

    public async onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected()
    {
    }

    public async onData(): Promise<any>
    {
        super.onData();

        this.m_fontMultiplier = this.widget.fontMultiplier || 1;
    }
}
