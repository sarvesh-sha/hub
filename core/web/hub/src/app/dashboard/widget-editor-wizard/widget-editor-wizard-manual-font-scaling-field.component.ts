import {Component, EventEmitter, Input, Output} from "@angular/core";

import * as SharedSvc from "framework/ui/components";

@Component({
               selector   : "o3-widget-editor-wizard-manual-font-scaling-field",
               templateUrl: "./widget-editor-wizard-manual-font-scaling-field.component.html",
               styleUrls  : ["./widget-editor-wizard-manual-font-scaling-field.component.scss"]
           })
export class WidgetEditorWizardManualFontScalingFieldComponent extends SharedSvc.BaseComponent
{
    private static readonly MIN_FONT_MULTIPLIER: number = 0.05;
    private static readonly MAX_FONT_MULTIPLIER: number = 50;

    get minFontMultiplier(): number
    {
        return WidgetEditorWizardManualFontScalingFieldComponent.MIN_FONT_MULTIPLIER;
    }

    get maxFontMultiplier(): number
    {
        return WidgetEditorWizardManualFontScalingFieldComponent.MAX_FONT_MULTIPLIER;
    }

    @Input() fontMultiplier: number = 1;

    @Output() fontMultiplierChange = new EventEmitter<number>();
    @Output() fontMultiplierValid  = new EventEmitter<boolean>();

    fontMultiplierChanged()
    {
        const valid = !isNaN(this.fontMultiplier ?? NaN) &&
                      this.fontMultiplier >= this.minFontMultiplier &&
                      this.fontMultiplier <= this.maxFontMultiplier;

        if (valid) this.fontMultiplierChange.emit(this.fontMultiplier);
        this.fontMultiplierValid.emit(valid);
    }
}
