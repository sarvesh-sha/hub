import {Component} from "@angular/core";

import {CustomFieldData, ReportItem} from "app/customer/configuration/reports/custom-report-builder/custom-report-field-wizard/custom-report-field-wizard.component";

import {ControlOption} from "framework/ui/control-option";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-custom-report-field-type-step",
               templateUrl: "./custom-report-field-wizard-type-step.component.html",
               providers  : [WizardStep.createProvider(CustomReportFieldWizardTypeStepComponent)]
           })
export class CustomReportFieldWizardTypeStepComponent extends WizardStep<CustomFieldData>
{
    selectedContainer: Set<string>;

    itemTypes: ControlOption<ReportItem>[] = [
        new ControlOption("CustomReportElementAggregatedValue", "Aggregation Summary"),
        new ControlOption("CustomReportElementAggregationTable", "Data Aggregation"),
        new ControlOption("CustomReportElementAggregationTrend", "Aggregation Trend"),
        new ControlOption("CustomReportElementAlertFeed", "Alert Feed"),
        new ControlOption("CustomReportElementAlertTable", "Alert Table"),
        new ControlOption("CustomReportElementChartSet", "Chart"),
        new ControlOption("CustomReportElementDeviceElementList", "Device Element List"),
        new ControlOption("CustomReportElementRichText", "Rich Text")
    ];

    private hasPageBreak: boolean = false;

    public getLabel(): string
    {
        return "Type";
    }

    public isEnabled(): boolean
    {
        return this.data && this.data.newItem;
    }

    public isValid(): boolean
    {
        return true;
    }

    public isNextJumpable(): boolean
    {
        return true;
    }

    public async onNext(): Promise<boolean>
    {
        return false;
    }

    public async onData(): Promise<any>
    {
        await super.onData();

        this.selectedContainer = new Set([this.data.elementType]);

        // should be at index=6 to maintain alphabetical order
        const pageBreakIdx = 6;

        if (this.data.withPageBreak)
        {
            if (!this.hasPageBreak)
            {
                this.itemTypes.splice(pageBreakIdx, 0, new ControlOption("CustomReportElementPageBreak", "Page Break"));
                this.hasPageBreak = true;
            }
        }
        else
        {
            if (this.hasPageBreak)
            {
                this.itemTypes.splice(pageBreakIdx, 1);
                this.hasPageBreak = false;
            }
        }
    }

    public async onStepSelected()
    {
    }

    public async updateSelected()
    {
        this.data.elementType = this.selectedContainer.values()
                                    .next().value;

        await this.data.ensureData();
    }
}
