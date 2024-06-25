import {Component, EventEmitter, Injector, Input, Output, SimpleChanges, ViewChild} from "@angular/core";
import * as SharedSvc from "app/services/domain/base.service";
import * as Models from "app/services/proxy/model/models";
import {RangeSelectionExtended} from "app/shared/forms/time-range/range-selection-extended";
import {ChartTimeUtilities, TimeRange, TimeRanges, TimeRangeType} from "framework/ui/charting/core/time";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import {SelectComponent} from "framework/ui/forms/select.component";

@Component({
               selector   : "o3-range-selector",
               templateUrl: "./range-selector.component.html",
               styleUrls  : ["./range-selector.component.scss"]
           })
export class RangeSelectorComponent extends SharedSvc.BaseApplicationComponent
{
    // Time range input to sync across all charts
    @Input() selection: Models.RangeSelection = RangeSelectionExtended.newModel();
    @Output() selectionChange                 = new EventEmitter<Models.RangeSelection>();

    private previousSelection: Models.TimeRangeId;

    // Time range options override, replaces the default set of time ranges if given
    @Input() rangeOptionType: TimeRangeType = TimeRangeType.All;
    @Input() rangeOverrides: TimeRange[];

    // Additional optional inputs
    @Input() allowCustom: boolean   = false;
    @Input() allowTimezone: boolean = false;
    @Input() hideLabel: boolean     = false;
    @Input() readonly: boolean      = false;
    @Input() readonlyTooltip: string;

    private m_range: TimeRange;
    start: Date;
    end: Date;

    //--//

    zone: string;

    @Input() set enabledOptions(val: Models.TimeRangeId[])
    {
        this.m_enabledOptions = val;
        this.refreshEnabledRanges();
    }

    @Input() set disabledOptions(val: Models.TimeRangeId[])
    {
        this.m_disabledOptions = val;
        this.refreshEnabledRanges();
    }

    private m_enabledOptions: Models.TimeRangeId[];
    private m_disabledOptions: Models.TimeRangeId[];
    private m_ranges: ControlOption<TimeRange>[];

    enabledRanges: ControlOption<TimeRange>[];

    @ViewChild("test_rangeSelector") test_rangeSelector: SelectComponent<TimeRange>;

    private refreshEnabledRanges()
    {
        let ranges = this.m_ranges;
        if (ranges)
        {
            if (this.m_enabledOptions)
            {
                ranges = ranges.filter(
                    c => !(c.id && this.m_enabledOptions.indexOf(<Models.TimeRangeId><any>c.id.id) < 0));
            }

            if (this.m_disabledOptions)
            {
                ranges = ranges.filter(
                    c => !(c.id && this.m_disabledOptions.indexOf(<Models.TimeRangeId><any>c.id.id) >= 0));
            }

            this.enabledRanges = ranges;
        }
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    get isCustomRange(): boolean
    {
        return this.allowCustom && !TimeRanges.extractId<Models.TimeRangeId>(this.m_range);
    }

    get rangeDropdownValue(): TimeRange
    {
        return this.m_range;
    }

    ngOnInit()
    {
        super.ngOnInit();
        this.init();
    }

    ngOnChanges(changes: SimpleChanges)
    {
        super.ngOnChanges(changes);
        this.init();
    }

    private static presetToCustomRange(presetRange: TimeRange,
                                       zone?: string): Models.RangeSelection
    {
        let range = presetRange.getTimeRangeValues().range;

        let start = new Date(range.minInMillisec);
        start.setSeconds(0, 0);
        let end = new Date(range.maxInMillisec);
        end.setSeconds(0, 0);

        return RangeSelectionExtended.newRange(start, end, zone);
    }

    rangeLabel(): string
    {
        return this.hideLabel ? null : "Time Range";
    }

    rangeChanged(value: TimeRange): void
    {
        // If switching to custom, set the to the last select range (or 24 hours as default)
        if (value == TimeRanges.CustomRange)
        {
            let base           = this.m_range || TimeRanges.Last24Hours;
            let rangeSelection = RangeSelectorComponent.presetToCustomRange(base, this.zone);
            this.start         = new Date(rangeSelection.start);
            this.end           = new Date(rangeSelection.end);
        }

        this.m_range = value ? value : null;

        this.makeSelection();
    }

    startChanged(value: Date): void
    {
        this.start = value || null;
        this.makeSelection();
    }

    endChanged(value: Date): void
    {
        this.end = value || null;
        this.makeSelection();
    }

    init(): void
    {
        if (this.selection) RangeSelectionExtended.fixupModel(this.selection);

        if (this.rangeOverrides)
        {
            this.m_ranges = ChartTimeUtilities.getExplicitTimeRangeControlOptions(this.rangeOverrides, this.allowCustom);
        }
        else
        {
            this.m_ranges = ChartTimeUtilities.getTimeRangeControlOptions(this.rangeOptionType, this.allowCustom);
        }

        this.refreshEnabledRanges();

        if (this.selection)
        {
            this.m_range = TimeRanges.resolve(this.selection.range, true);
            if (this.selection.zone) this.zone = this.selection.zone;
            if (this.selection.start) this.start = new Date(this.selection.start);
            if (this.selection.end) this.end = new Date(this.selection.end);
            this.previousSelection = this.selection.range;
        }
    }

    makeSelection()
    {
        this.selection = Models.RangeSelection.newInstance({
                                                               range      : TimeRanges.extractId<Models.TimeRangeId>(this.m_range),
                                                               zone       : this.zone,
                                                               start      : this.start,
                                                               end        : this.end,
                                                               zoneCreated: MomentHelper.getLocalZone()
                                                           });

        if (this.disabledOptions && this.previousSelection)
        {
            this.disabledOptions.splice(this.disabledOptions.findIndex((val) => val === this.previousSelection),
                                        1);
            this.previousSelection = this.selection.range;
            if (this.selection.range)
            {
                this.disabledOptions.push(this.selection.range);
            }
        }

        this.selectionChange.emit(this.selection);
    }
}
