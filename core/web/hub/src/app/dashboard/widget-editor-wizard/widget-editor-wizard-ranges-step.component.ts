import {Component, Input, QueryList, ViewChild, ViewChildren} from "@angular/core";

import {ScheduleSelectorOverlayComponent} from "app/dashboard/overlays/schedule-selector-overlay.component";
import {WidgetEditorWizardState} from "app/dashboard/widget-editor-wizard/widget-editor-wizard-dialog.component";
import * as Models from "app/services/proxy/model/models";
import {DataAggregationType} from "app/shared/aggregation/data-aggregation.component";
import {DaysOfWeek, FilterableTimeRangeExtended, RangeSelectionExtended, RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";

import {ModifiableTableComponent} from "framework/ui/shared/modifiable-table.component";
import {WizardStep} from "framework/ui/wizards/wizard-step";

@Component({
               selector   : "o3-widget-editor-wizard-ranges-step",
               templateUrl: "./widget-editor-wizard-ranges-step.component.html",
               styleUrls  : [
                   "./widget-editor-wizard-dialog.component.scss",
                   "./widget-editor-wizard-ranges-step.component.scss"
               ],
               providers  : [WizardStep.createProvider(WidgetEditorWizardRangesStepComponent)]
           })
export class WidgetEditorWizardRangesStepComponent extends WizardStep<WidgetEditorWizardState>
{
    @ViewChild("test_ranges") test_ranges: ModifiableTableComponent<Models.FilterableTimeRange>;
    @ViewChildren(ScheduleSelectorOverlayComponent) weeklyFilterDialogs: QueryList<ScheduleSelectorOverlayComponent>;

    private suggestedRanges: Models.TimeRangeId[] = [
        Models.TimeRangeId.Last24Hours,
        Models.TimeRangeId.Last7Days,
        Models.TimeRangeId.Last30Days
    ];

    @Input() maxNumRanges: number = 8;

    @Input() set selectedRanges(ranges: Models.FilterableTimeRange[])
    {
        if (ranges)
        {
            this.selectedPresetRanges = ranges.filter((filterableRange) => filterableRange.range.range)
                                              .map((presetRange) => presetRange.range.range);
            this.m_selectedRanges     = ranges;
            this.syncCustomFilters();
        }
    }

    get isAlertTable(): boolean
    {
        return this.data.editor.widget instanceof Models.AlertTableWidgetConfiguration;
    }

    get isAggregationTable(): boolean
    {
        return this.data.editor.widget instanceof Models.AggregationTableWidgetConfiguration;
    }

    get selectedRanges(): Models.FilterableTimeRange[]
    {
        return this.m_selectedRanges;
    }

    get rangeMode(): boolean
    {
        return this.data.editor.visualizationRanges;
    }

    set rangeMode(mode: boolean)
    {
        this.data.editor.visualizationRanges = mode;
    }

    get stepDescription(): string
    {
        if (this.isAggregationTable)
        {
            return "Over which time range(s) should your control point groups aggregate?";
        }
        else
        {
            return "Over which time range(s) should the alerts be from?";
        }
    }

    private m_selectedRanges: Models.FilterableTimeRange[];
    selectedPresetRanges: Models.TimeRangeId[];
    customFilters: Models.FilterableTimeRange[] = [];

    public getLabel() { return "Time ranges"; }

    public isEnabled()
    {
        if (this.data.editor.allowWidgetTypes(Models.AggregationTableWidgetConfiguration)) return this.data.editor.dataAggregationExt.type === DataAggregationType.Groups;
        return this.data.editor.allowWidgetTypes(Models.AlertTableWidgetConfiguration);
    }

    public isValid(): boolean
    {
        if (!this.data.editor.selectedRanges?.length) return false;
        if (this.data.editor.selectedRanges.length !== this.data.editor.app.domain.widgetData.getValidRanges(this.data.editor.selectedRanges).length) return false;
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

    public addRange(): void
    {
        let newRange        = new Models.RangeSelection();
        let nextTimeRangeId = this.getFirstNotIncluded(this.suggestedRanges, this.selectedPresetRanges);

        if (nextTimeRangeId)
        {
            newRange.range = nextTimeRangeId;
            this.selectedPresetRanges.push(nextTimeRangeId);
        }
        this.data.editor.selectedRanges.push(FilterableTimeRangeExtended.newInstance(newRange));
    }

    private getFirstNotIncluded(fromArr: Models.TimeRangeId[],
                                uniqueArr: Models.TimeRangeId[]): Models.TimeRangeId
    {
        return fromArr.find((potentiallyIncluded) =>
                                !uniqueArr.some((alreadyIncluded) => alreadyIncluded === potentiallyIncluded));
    }

    public logRangeRemoval(removedItem: Models.FilterableTimeRange): void
    {
        if (removedItem.range && removedItem.range.range)
        {
            this.selectedPresetRanges.splice(this.selectedPresetRanges.indexOf(removedItem.range.range), 1);
        }
        if (removedItem.filter && removedItem.name)
        {
            this.customFilters.splice(this.customFilters.indexOf(removedItem), 1);
        }
    }

    public rangeChange(range: Models.RangeSelection,
                       index: number)
    {
        this.data.editor.selectedRanges[index].range = range;
    }

    public openWeeklyFilterDialog(index: number)
    {
        if (index >= 0 && index < this.selectedRanges.length)
        {
            if (!this.data.editor.selectedRanges[index].filter)
            {
                this.data.editor.selectedRanges[index].filter = RecurringWeeklyScheduleExtended.generateFullWeekSchedule();
            }
            this.weeklyFilterDialogs.toArray()[index].toggleOverlay();
        }
    }

    public closeOverlay(index: number)
    {
        this.weeklyFilterDialogs.toArray()[index].closeOverlay();
    }

    public syncCustomFilters(): void
    {
        this.customFilters.length = 0;
        for (let range of this.m_selectedRanges)
        {
            if (range.filter && range.name) this.customFilters.push(range);
        }
    }

    public setToPreset(id: string): void
    {
        if (id === "default")
        {
            this.defaultSetup();
        }
        else
        {
            let preset = Models.TimeRangeId[<Models.TimeRangeId>id];
            if (preset)
            {
                this.selectedPresetRanges       = DaysOfWeek.map(() => preset);
                this.data.editor.selectedRanges = this.weekConfig(preset);
                this.selectedRanges             = this.data.editor.selectedRanges;
            }
        }

        this.data.editor.syncTableConfigs();
    }

    public setRangeMode(mode: boolean)
    {
        this.rangeMode = mode;
        this.data.editor.syncTableConfigs();
    }

    private weekConfig(preset: Models.TimeRangeId): Models.FilterableTimeRange[]
    {
        let range      = RangeSelectionExtended.newModel(preset);
        let weekConfig = DaysOfWeek.map(
            (day) =>
            {
                let prettyDay = day.charAt(0) + day.slice(1)
                                                   .toLowerCase();
                return Models.FilterableTimeRange.newInstance(
                    {
                        name           : prettyDay,
                        isFilterApplied: true,
                        range          : range,
                        filter         : RecurringWeeklyScheduleExtended.generateWeeklySchedule([day])
                    }
                );
            });

        weekConfig.push(Models.FilterableTimeRange.newInstance(
            {
                name           : "Totals",
                isFilterApplied: true,
                range          : range,
                filter         : RecurringWeeklyScheduleExtended.generateFullWeekSchedule()
            }
        ));

        return weekConfig;
    }

    private defaultSetup(): void
    {
        this.selectedPresetRanges = [
            Models.TimeRangeId.Last24Hours,
            Models.TimeRangeId.Last7Days,
            Models.TimeRangeId.Last30Days
        ];

        this.data.editor.selectedRanges = this.selectedPresetRanges.map(
            presetRange => FilterableTimeRangeExtended.newInstance(RangeSelectionExtended.newModel(presetRange)));
        this.selectedRanges             = this.data.editor.selectedRanges;
    }

    public async onStepSelected()
    {
        if (!this.selectedRanges || !this.selectedRanges.length)
        {
            this.defaultSetup();
        }

        this.data.editor.syncTableConfigs();
    }
}
