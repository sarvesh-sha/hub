import {Component} from "@angular/core";

import {CustomFieldData} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";
import * as Models from "app/services/proxy/model/models";
import {ReportDateRangeBlot, ReportDateTimeRangeBlot} from "app/shared/reports/elements/rich-text/rich-text-report-date-range.component";
import {RichTextReportDateMode, RichTextReportDateSelectionDialogComponent} from "app/shared/reports/elements/rich-text/rich-text-report-date-selection-dialog.component";

import {UtilsService} from "framework/services/utils.service";
import {RichTextEditorComponent} from "framework/ui/markdown/rich-text-editor.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-rich-text-step",
               templateUrl: "./custom-report-field-wizard-rich-text-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardRichTextStepComponent)]
           })
export class CustomReportFieldWizardRichTextStepComponent extends WizardStep<CustomFieldData>
{
    get typedElement(): Models.CustomReportElementRichText
    {
        return UtilsService.asTyped(this.data.element, Models.CustomReportElementRichText);
    }

    public async createDateRange(editor: RichTextEditorComponent)
    {
        let selection = await RichTextReportDateSelectionDialogComponent.execute(this);
        switch (selection)
        {
            case RichTextReportDateMode.Date:
                editor.insertComponentBlot(ReportDateRangeBlot);
                break;

            case RichTextReportDateMode.DateTime:
                editor.insertComponentBlot(ReportDateTimeRangeBlot);
                break;
        }
    }

    //--//

    public getLabel(): string
    {
        return "Rich Text";
    }

    public isEnabled(): boolean
    {
        return !!this.typedElement;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        return this.wizard.stepForm?.valid;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onStepSelected()
    {
    }
}
