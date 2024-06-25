import {Component, Injector, OnChanges} from "@angular/core";
import {ReportWizardState} from "app/customer/configuration/reports/wizard/report-wizard-dialog.component";
import {ChartTimeUtilities, TimeRange, TimeRanges} from "framework/ui/charting/core/time";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-report-name-step",
               templateUrl: "./report-wizard-name-step.component.html",
               providers  : [
                   WizardStep.createProvider(ReportWizardNameStepComponent)
               ]
           })
export class ReportWizardNameStepComponent extends WizardStep<ReportWizardState> implements OnChanges
{
    timeRangeOptions = ChartTimeUtilities.getTimeRangeControlOptions();

    timeRange: TimeRange;

    constructor(inj: Injector)
    {
        super(inj);
    }

    public getLabel() { return "Name & Description"; }

    public isEnabled(): boolean
    {
        return true;
    }

    public isValid(): boolean
    {
        if (!this.data.reportDefinition?.model.title) return false;
        if (!this.data.schedulingOptions.model.range) return false;
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

        this.timeRange = TimeRanges.resolve(this.data.schedulingOptions.model.range, false);
    }

    public async onStepSelected()
    {
    }

    public updateTimeRange(): void
    {
        this.data.schedulingOptions.model.range = <any>this.timeRange.id;
    }
}
