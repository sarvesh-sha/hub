import {Component, ElementRef, ViewChild} from "@angular/core";

import {ScheduleSelectorOverlayComponent} from "app/dashboard/overlays/schedule-selector-overlay.component";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {RangeSelectionExtended, RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";
import {RangeSelectorComponent} from "app/shared/forms/time-range/range-selector.component";

import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-aggregation-range-step",
               templateUrl: "./widget-editor-wizard-aggregation-range-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-aggregation-range-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardAggregationRangeStepComponent)]
           })
export class WidgetEditorWizardAggregationRangeStepComponent extends WizardStep<WidgetEditorWizardState>
{
    @ViewChild(ScheduleSelectorOverlayComponent) filterOverlay: ScheduleSelectorOverlayComponent;

    @ViewChild("test_rangeSelector") test_rangeSelector: RangeSelectorComponent;
    @ViewChild("test_showRange", {read: ElementRef}) test_showRange: ElementRef;

    get aggregationSummary(): Models.AggregationWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AggregationWidgetConfiguration && this.data.editor.widget;
    }

    get aggregationTrend(): Models.AggregationTrendWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AggregationTrendWidgetConfiguration && this.data.editor.widget;
    }

    get aggregationTable(): Models.AggregationTableWidgetConfiguration
    {
        return this.data.editor.widget instanceof Models.AggregationTableWidgetConfiguration && this.data.editor.widget;
    }

    get granularityOption(): Models.AggregationGranularity
    {
        let aggTrend = this.aggregationTrend;
        return aggTrend ? aggTrend.granularity : Models.AggregationGranularity.None;
    }

    set granularityOption(granularity: Models.AggregationGranularity)
    {
        let aggTrend = this.aggregationTrend;
        if (aggTrend) aggTrend.granularity = granularity;
    }

    get showRange(): boolean
    {
        let aggSummary = this.aggregationSummary;
        return aggSummary ? !aggSummary.hideRange : true;
    }

    set showRange(show: boolean)
    {
        let aggSummary = this.aggregationSummary;
        if (aggSummary) aggSummary.hideRange = !show;
    }

    get stepDescription(): string
    {
        if (this.aggregationSummary)
        {
            return "Over what range should your control point group aggregate?";
        }
        else if (this.aggregationTable)
        {
            return "Over what range should your aggregations be done?";
        }
        else
        {
            return "Over what range should your control point group(s) aggregate?";
        }
    }

    public getLabel() { return "Aggregation range"; }

    public isEnabled()
    {
        if (this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration)) return this.data.editor.dataAggregationExt.type === DataAggregationType.Bindings;
        return this.data.editor.allowWidgetTypes(Models.AggregationWidgetConfiguration, Models.AggregationTrendWidgetConfiguration);
    }

    public isValid(): boolean
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

    public openFilterDialog()
    {
        if (!this.data.editor.selectedRanges[0].filter)
        {
            this.data.editor.selectedRanges[0].filter = RecurringWeeklyScheduleExtended.generateFullWeekSchedule();
        }
        this.filterOverlay.toggleOverlay();
    }

    public async onStepSelected()
    {
        if (!this.data.editor.selectedRanges)
        {
            this.data.editor.selectedRanges = [
                Models.FilterableTimeRange.newInstance({
                                                           name           : "",
                                                           isFilterApplied: false,
                                                           range          : RangeSelectionExtended.newModel(),
                                                           filter         : RecurringWeeklyScheduleExtended.generateFullWeekSchedule()
                                                       })
            ];
        }

        if (this.aggregationSummary) this.data.editor.syncAggregationConfig();
        if (this.aggregationTrend) this.data.editor.syncAggregationTrendConfig();
        if (this.aggregationTable) this.data.editor.syncTableConfigs();
    }
}
