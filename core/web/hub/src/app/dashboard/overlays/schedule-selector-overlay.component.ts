import {Component, ElementRef, EventEmitter, Input, Output, ViewChild} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {RecurringWeeklyScheduleExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ScheduleSelectorComponent} from "app/shared/forms/time-range/schedule-selector.component";
import {UtilsService} from "framework/services/utils.service";
import {OverlayBase} from "framework/ui/overlays/overlay-base";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-schedule-selector-overlay",
               templateUrl: "./schedule-selector-overlay.component.html",
               styleUrls  : ["./schedule-selector-overlay.component.scss"]
           })
export class ScheduleSelectorOverlayComponent extends OverlayBase
{
    overlayConfig = OverlayConfig.newInstance({
                                                  minWidth           : 900,
                                                  showBackdrop       : true,
                                                  closableViaBackdrop: false
                                              });

    @ViewChild("name") set nameElement(elem: ElementRef)
    {
        if (elem && !this.filterName) setTimeout(() => elem.nativeElement.focus(), 0);
    }

    @ViewChild(ScheduleSelectorComponent) private scheduleSelector: ScheduleSelectorComponent;

    get pristine(): boolean
    {
        return UtilsService.equivalentStrings(this.filterName, this.m_model.name) &&
               !this.filterApplied === !this.m_model.isFilterApplied &&
               this.m_modelFilterExtended.equals(this.filter);
    }

    filter: Models.RecurringWeeklySchedule;
    filterApplied: boolean = false;
    filterName: string     = "";

    private m_model: Models.FilterableTimeRange;
    private m_modelFilterExtended: RecurringWeeklyScheduleExtended;

    @Input() set model(filterableTimeRange: Models.FilterableTimeRange)
    {
        if (!filterableTimeRange.filter) filterableTimeRange.filter = RecurringWeeklyScheduleExtended.generateFullWeekSchedule();
        this.filter        = RecurringWeeklyScheduleExtended.deepCopySchedule(filterableTimeRange.filter);
        this.filterApplied = !!filterableTimeRange.isFilterApplied;
        if (filterableTimeRange.name) this.filterName = filterableTimeRange.name;
        this.m_model               = filterableTimeRange;
        this.m_modelFilterExtended = new RecurringWeeklyScheduleExtended(this.m_model.filter);
    }

    @Input() otherFilters: Models.FilterableTimeRange[];

    @Input() onWord: string  = "Enabled";
    @Input() offWord: string = "Disabled";

    @Output() selectionChange: EventEmitter<Models.FilterableTimeRange> = new EventEmitter<Models.FilterableTimeRange>();

    public toggleFilter(targetStatus?: boolean)
    {
        this.filterApplied = targetStatus ?? !this.filterApplied;
    }

    public saveFilter(): void
    {
        this.m_model.filter          = this.filter;
        this.m_model.name            = this.filterName;
        this.m_model.isFilterApplied = this.filterApplied;
        this.m_modelFilterExtended   = new RecurringWeeklyScheduleExtended(this.m_model.filter);
        this.selectionChange.emit(this.m_model);
        this.resetScheduleSelectorFilter();
        this.closeOverlay();
    }

    public revert(): void
    {
        this.resetScheduleSelectorFilter();
        // filter reference remains intact: was edited in place in the schedule selector component
        this.filterName    = this.m_model.name;
        this.filterApplied = this.m_model.isFilterApplied;
    }

    private resetScheduleSelectorFilter(): void
    {
        this.filter = RecurringWeeklyScheduleExtended.deepCopySchedule(this.m_model.filter);
        if (this.scheduleSelector)
        {
            this.scheduleSelector.model = this.filter;
            this.scheduleSelector.zoomOut();
        }
    }
}
