import {Component, ElementRef, ViewChild} from "@angular/core";

import {WidgetEditorWizardWidgetPreviewStep} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-widget-preview-step.directive";
import * as Models from "app/services/proxy/model/models";

import {UtilsService} from "framework/services/utils.service";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-text-step",
               templateUrl: "./widget-editor-wizard-text-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-text-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardTextStepComponent)]
           })
export class WidgetEditorWizardTextStepComponent extends WidgetEditorWizardWidgetPreviewStep<Models.TextWidgetConfiguration>
{
    get typedWidget(): Models.TextWidgetConfiguration
    {
        return UtilsService.asTyped(this.data.editor.widget, Models.TextWidgetConfiguration);
    }

    fontMultiplierValid: boolean = true;

    get text(): string
    {
        return this.typedWidget.text;
    }

    set text(text: string)
    {
        this.updateWidget((widget) => widget.text = text);
    }

    get color(): string
    {
        return this.typedWidget.color;
    }

    set color(color: string)
    {
        this.updateWidget((widget) => widget.color = color);
    }

    get textAlign(): Models.HorizontalAlignment
    {
        return this.typedWidget.alignment || Models.HorizontalAlignment.Center;
    }

    set textAlign(alignment: Models.HorizontalAlignment)
    {
        this.updateWidget((widget) => widget.alignment = alignment);
    }

    get wrapContent(): boolean
    {
        return !this.typedWidget.preventWrapping;
    }

    set wrapContent(wrap: boolean)
    {
        let preventWrap = !wrap;
        this.updateWidget((widget) => widget.preventWrapping = preventWrap);
    }

    get manualFontScaling(): boolean
    {
        return this.typedWidget?.manualFontScaling;
    }

    set manualFontScaling(manual: boolean)
    {
        let fontMultiplier = manual ? this.m_fontMultiplier : 0;
        this.updateWidget((widget) =>
                          {
                              widget.manualFontScaling = manual;
                              widget.fontMultiplier    = fontMultiplier;
                          });
    }

    private m_fontMultiplier: number = 1;
    get fontMultiplier(): number
    {
        return this.m_fontMultiplier;
    }

    set fontMultiplier(multiplier: number)
    {
        this.m_fontMultiplier = multiplier;
        this.updateWidget((widget) => widget.fontMultiplier = multiplier);
    }

    @ViewChild("test_text", {read: ElementRef}) test_text: ElementRef;

    protected updatePreviewConfig()
    {
        this.previewConfig = Models.TextWidgetConfiguration.newInstance(this.previewConfig);
    }

    public getLabel(): string
    {
        return "Text";
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
        if (this.manualFontScaling && !this.fontMultiplierValid) return false;
        return !!this.typedWidget.text;
    }

    public async onNext(): Promise<boolean>
    {
        return undefined;
    }

    public async onStepSelected()
    {
        this.initialize();

        this.widgetPreview?.refresh();
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.m_fontMultiplier = this.data.editor.widget.fontMultiplier || this.m_fontMultiplier;

        this.initialize();
    }

    private initialize()
    {
        let widget = this.typedWidget;
        if (!this.previewConfig && widget)
        {
            this.previewConfig = Models.TextWidgetConfiguration.deepClone(widget);
        }
    }
}
