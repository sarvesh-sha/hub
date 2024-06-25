import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";

import moment from "framework/utils/moment";

export class ChartTimeWindow
{
    static readonly EmptyPlaceholder = new ChartTimeWindow(0, 1, 0, 1);

    constructor(public readonly startMillisecond: number,
                public readonly endMillisecond: number,
                public readonly minValue: number,
                public readonly maxValue: number)
    {
    }

    get timeRangeInMilliseconds(): number
    {
        return this.endMillisecond - this.startMillisecond;
    }

    get valueRange(): number
    {
        return this.maxValue - this.minValue;
    }

    isInValueRange(value: number): boolean
    {
        return this.minValue <= value && value <= this.maxValue;
    }

    isInTimeRange(ms: number): boolean
    {
        return this.startMillisecond <= ms && ms < this.endMillisecond;
    }
}

export class ChartTimeRange
{
    get diffAsMs(): number
    {
        if (this.minInMillisec === undefined || this.maxInMillisec === undefined) return undefined;

        return this.maxInMillisec - this.minInMillisec;
    }

    get minAsMoment(): moment.Moment
    {
        return MomentHelper.parse(this.minInMillisec);
    }

    set minAsMoment(value: moment.Moment)
    {
        this.minInMillisec = value.valueOf();
    }

    get maxAsMoment(): moment.Moment
    {
        return MomentHelper.parse(this.maxInMillisec);
    }

    set maxAsMoment(value: moment.Moment)
    {
        this.maxInMillisec = value.valueOf();
    }

    static isValid(o: ChartTimeRange)
    {
        return o && o.minInMillisec !== undefined && o.maxInMillisec !== undefined;
    }

    public minInMillisec: number;
    public maxInMillisec: number;

    constructor(min?: number | moment.Moment,
                max?: number | moment.Moment)
    {
        if (min) this.minInMillisec = typeof min == "number" ? min : min.valueOf();
        if (max) this.maxInMillisec = typeof max == "number" ? max : max.valueOf();
    }

    shift(ms: number)
    {
        if (!isNaN(this.minInMillisec)) this.minInMillisec += ms;
        if (!isNaN(this.maxInMillisec)) this.maxInMillisec += ms;
    }

    shiftToBeContained(containedRange: ChartTimeRange): boolean
    {
        if (this.diffAsMs < containedRange.diffAsMs) return false;

        // grab the diff that separates containedRange from being completely contained by this
        let diff = Math.max(0, this.minInMillisec - containedRange.minInMillisec);
        diff     = diff || Math.min(0, this.maxInMillisec - containedRange.maxInMillisec);

        // add the diff to make containedRange completely contained by this
        containedRange.shift(diff);
        return true;
    }

    clone(): ChartTimeRange
    {
        return new ChartTimeRange(this.minInMillisec, this.maxInMillisec);
    }

    isSame(other: ChartTimeRange): boolean
    {
        return other && this.minInMillisec == other.minInMillisec && this.maxInMillisec == other.maxInMillisec;
    }

    isSameMin(other: ChartTimeRange): boolean
    {
        return other && this.minInMillisec == other.minInMillisec;
    }

    isSameMax(other: ChartTimeRange): boolean
    {
        return other && this.maxInMillisec == other.maxInMillisec;
    }

    expandToContain(time: number)
    {
        if (!isNaN(time))
        {
            if (isNaN(this.minInMillisec) || this.minInMillisec > time)
            {
                this.minInMillisec = time;
            }

            if (isNaN(this.maxInMillisec) || this.maxInMillisec < time)
            {
                this.maxInMillisec = time;
            }
        }
    }

    expandRange(min: number,
                max: number)
    {
        if (!isNaN(min))
        {
            if (this.minInMillisec === undefined || this.minInMillisec > min) this.minInMillisec = min;
        }

        if (!isNaN(max))
        {
            if (this.maxInMillisec === undefined || this.maxInMillisec < max) this.maxInMillisec = max;
        }
    }

    overlapMs(otherRange: ChartTimeRange): number
    {
        let firstStart  = this.minInMillisec < otherRange.minInMillisec ? this : otherRange;
        let secondStart = this === firstStart ? otherRange : this;

        if (firstStart.maxInMillisec <= secondStart.minInMillisec) return 0;
        return Math.min(firstStart.maxInMillisec - secondStart.minInMillisec, secondStart.diffAsMs);
    }

    clipInRange(other: number): number
    {
        return other == null ? undefined : UtilsService.clamp(this.minInMillisec, this.maxInMillisec, other);
    }

    isInRange(timestampMS: number,
              inclusive: boolean = false): boolean
    {
        if (timestampMS == null) return false;

        if (inclusive)
        {
            if (this.minInMillisec > timestampMS) return false;
            if (this.maxInMillisec < timestampMS) return false;
        }
        else
        {
            if (this.minInMillisec >= timestampMS) return false;
            if (this.maxInMillisec <= timestampMS) return false;
        }

        return true;
    }
}

export class ComparisonChartRangeSelection extends ChartTimeRange
{
    comparisonOffset: number = 1;

    comparisonOffsetUnit: moment.unitOfTime.DurationConstructor = "hour";
}

export class TimeRangeValues
{
    range: ChartTimeRange;

    comparisonRange: ComparisonChartRangeSelection;

    constructor()
    {
        this.range           = new ChartTimeRange();
        this.comparisonRange = new ComparisonChartRangeSelection();
    }
}

export class TimeRange
{
    public readonly controlOption: ControlOption<TimeRange>;

    constructor(public readonly id: TimeRangeId,
                public readonly displayName: string,
                public readonly amount: number,
                public readonly unit: moment.unitOfTime.DurationConstructor,
                public readonly shouldTruncate: boolean,
                public readonly usePreviousRange: boolean)
    {
        this.controlOption = new ControlOption(this, displayName);
    }

    getSmaller(): TimeRange
    {
        let pos = timeRanges.indexOf(this);
        while (true)
        {
            let range = timeRanges[--pos];
            if (!range)
            {
                return null;
            }

            if (range.shouldTruncate == this.shouldTruncate && range.usePreviousRange == this.usePreviousRange)
            {
                return range;
            }
        }
    }

    getBigger(): TimeRange
    {
        let pos = timeRanges.indexOf(this);
        while (true)
        {
            let range = timeRanges[++pos];
            if (!range)
            {
                return null;
            }

            if (range.shouldTruncate == this.shouldTruncate && range.usePreviousRange == this.usePreviousRange)
            {
                return range;
            }
        }
    }

    getTimeRangeValues(zone?: string,
                       end?: Date): TimeRangeValues
    {
        // Special case for Custom Range.
        if (this.amount == 0)
        {
            return null;
        }

        let result = new TimeRangeValues();

        // establish time range
        let now = end ? MomentHelper.parse(end) : MomentHelper.now();
        let startMoment: moment.Moment;
        let endMoment: moment.Moment;

        now = MomentHelper.toZone(now, zone);

        if (this.shouldTruncate)
        {
            startMoment = MomentHelper.startOf(now, this.unit);
            endMoment   = MomentHelper.add(startMoment, this.amount, this.unit);
        }
        else
        {
            startMoment = MomentHelper.subtract(now, this.amount, this.unit);
            endMoment   = now;
        }

        if (this.usePreviousRange)
        {
            startMoment = MomentHelper.subtract(startMoment, this.amount, this.unit);
            endMoment   = MomentHelper.subtract(endMoment, this.amount, this.unit);
        }

        let rangeMin           = startMoment;
        let rangeMax           = MomentHelper.min(now, endMoment);
        let comparisonRangeMin = rangeMin;
        let comparisonRangeMax = rangeMax;

        if (this.unit)
        {
            comparisonRangeMin = MomentHelper.subtract(startMoment, this.amount, this.unit);
            comparisonRangeMax = MomentHelper.subtract(endMoment, this.amount, this.unit);
        }

        result.range.minInMillisec = rangeMin.valueOf();
        result.range.maxInMillisec = rangeMax.valueOf();

        if (this.unit)
        {
            result.comparisonRange.minInMillisec = comparisonRangeMin.valueOf();
            result.comparisonRange.maxInMillisec = comparisonRangeMax.valueOf();

            result.comparisonRange.comparisonOffset     = this.amount;
            result.comparisonRange.comparisonOffsetUnit = this.unit;
        }

        return result;
    }
}

export enum TimeRangeId
{
    Last15Minutes   = "Last15Minutes",
    Last30Minutes   = "Last30Minutes",
    Last60Minutes   = "Last60Minutes",
    Hour            = "Hour",
    PreviousHour    = "PreviousHour",
    Last3Hours      = "Last3Hours",
    Last6Hours      = "Last6Hours",
    Last12Hours     = "Last12Hours",
    Last24Hours     = "Last24Hours",
    Today           = "Today",
    Yesterday       = "Yesterday",
    Last2Days       = "Last2Days",
    Last3Days       = "Last3Days",
    Last7Days       = "Last7Days",
    Week            = "Week",
    PreviousWeek    = "PreviousWeek",
    Month           = "Month",
    PreviousMonth   = "PreviousMonth",
    Last30Days      = "Last30Days",
    Quarter         = "Quarter",
    PreviousQuarter = "PreviousQuarter",
    Last3Months     = "Last3Months",
    Year            = "Year",
    PreviousYear    = "PreviousYear",
    Last365Days     = "Last365Days",
    CustomRange     = "<CustomRange>"
}

// @formatter:off
export namespace TimeRanges
{
    export const Last15Minutes = new TimeRange(TimeRangeId.Last15Minutes  , "Last 15 Minutes" , 15, "minute" , false, false);
    export const Last30Minutes = new TimeRange(TimeRangeId.Last30Minutes  , "Last 30 Minutes" , 30, "minute" , false, false);
    export const Last60Minutes = new TimeRange(TimeRangeId.Last60Minutes  , "Last 60 Minutes" ,  1, "hour"   , false, false);
    export const Hour          = new TimeRange(TimeRangeId.Hour           , "This Hour"       ,  1, "hour"   , true , false);
    export const LastHour      = new TimeRange(TimeRangeId.PreviousHour   , "Previous Hour"   ,  1, "hour"   , true , true );
    export const Last3Hours    = new TimeRange(TimeRangeId.Last3Hours     , "Last 3 Hours"    ,  3, "hour"   , false, false);
    export const Last6Hours    = new TimeRange(TimeRangeId.Last6Hours     , "Last 6 Hours"    ,  6, "hour"   , false, false);
    export const Last12Hours   = new TimeRange(TimeRangeId.Last12Hours    , "Last 12 Hours"   , 12, "hour"   , false, false);
    export const Last24Hours   = new TimeRange(TimeRangeId.Last24Hours    , "Last 24 Hours"   , 24, "hour"   , false, false);
    export const Today         = new TimeRange(TimeRangeId.Today          , "Today"           ,  1, "day"    , true , false);
    export const Yesterday     = new TimeRange(TimeRangeId.Yesterday      , "Yesterday"       ,  1, "day"    , true , true );
    export const Last2Days     = new TimeRange(TimeRangeId.Last2Days      , "Last 2 Days"     ,  2, "day"    , false, false);
    export const Last3Days     = new TimeRange(TimeRangeId.Last3Days      , "Last 3 Days"     ,  3, "day"    , false, false);
    export const Last7Days     = new TimeRange(TimeRangeId.Last7Days      , "Last 7 Days"     ,  7, "day"    , false, false);
    export const Week          = new TimeRange(TimeRangeId.Week           , "This Week"       ,  1, "week"   , true , false);
    export const LastWeek      = new TimeRange(TimeRangeId.PreviousWeek   , "Previous Week"   ,  1, "week"   , true , true );
    export const Month         = new TimeRange(TimeRangeId.Month          , "This Month"      ,  1, "month"  , true , false);
    export const LastMonth     = new TimeRange(TimeRangeId.PreviousMonth  , "Previous Month"  ,  1, "month"  , true , true );
    export const Last30Days    = new TimeRange(TimeRangeId.Last30Days     , "Last 30 Days"    ,  1, "month"  , false, false);
    export const Quarter       = new TimeRange(TimeRangeId.Quarter        , "This Quarter"    ,  1, "quarter", true , false);
    export const LastQuarter   = new TimeRange(TimeRangeId.PreviousQuarter, "Previous Quarter",  1, "quarter", true , true );
    export const Last3Months   = new TimeRange(TimeRangeId.Last3Months    , "Last 3 Months"   ,  3, "month"  , false, false);
    export const Year          = new TimeRange(TimeRangeId.Year           , "This Year"       ,  1, "year"   , true , false);
    export const LastYear      = new TimeRange(TimeRangeId.PreviousYear   , "Previous Year"   ,  1, "year"   , true , true );
    export const Last365Days   = new TimeRange(TimeRangeId.Last365Days    , "Last 365 Days"   ,  1, "year"   , false, false);
    export const CustomRange   = new TimeRange(TimeRangeId.CustomRange    , "Custom Range"    ,  0, null     , false, false);

    export function resolve(id: any, includeCustom: boolean): TimeRange
    {
        for (let timeRange of timeRanges)
        {
            if (timeRange.id == id)
            {
                return timeRange;
            }
        }

        return CustomRange;
    }

    export function extractId<T>(range: TimeRange): T
    {
        return range && range != TimeRanges.CustomRange ? <T> <any> range.id : null;
    }
}
// @formatter:on

export const timeRanges: TimeRange[] = [
    TimeRanges.Last15Minutes,
    TimeRanges.Last30Minutes,
    TimeRanges.Last60Minutes,
    TimeRanges.Hour,
    TimeRanges.LastHour,
    TimeRanges.Last3Hours,
    TimeRanges.Last6Hours,
    TimeRanges.Last12Hours,
    TimeRanges.Last24Hours,
    TimeRanges.Today,
    TimeRanges.Yesterday,
    TimeRanges.Last2Days,
    TimeRanges.Last3Days,
    TimeRanges.Last7Days,
    TimeRanges.Week,
    TimeRanges.LastWeek,
    TimeRanges.Month,
    TimeRanges.LastMonth,
    TimeRanges.Last30Days,
    TimeRanges.Quarter,
    TimeRanges.LastQuarter,
    TimeRanges.Last3Months,
    TimeRanges.Year,
    TimeRanges.LastYear,
    TimeRanges.Last365Days
];

//--//

export class ChartTimeUtilities
{
    public static relativeTimeRanges: TimeRange[] = [
        TimeRanges.Last60Minutes,
        TimeRanges.Last3Hours,
        TimeRanges.Last6Hours,
        TimeRanges.Last12Hours,
        TimeRanges.Last24Hours,
        TimeRanges.Last2Days,
        TimeRanges.Last3Days,
        TimeRanges.Last7Days,
        TimeRanges.Last30Days,
        TimeRanges.Last3Months,
        TimeRanges.Last365Days
    ];

    static getTimeRangeControlOptions(rangeType?: TimeRangeType,
                                      includeCustom: boolean = false): ControlOption<TimeRange>[]
    {
        let ranges: TimeRange[];
        switch (rangeType)
        {
            case TimeRangeType.Relative:
                ranges = this.relativeTimeRanges;
                break;

            default:
                ranges = timeRanges;
                break;
        }

        let controlOptions = ranges.map((timeRange) => timeRange.controlOption);
        if (includeCustom) controlOptions.push(TimeRanges.CustomRange.controlOption);

        return controlOptions;
    }

    static getExplicitTimeRangeControlOptions(ranges: TimeRange[],
                                              includeCustom: boolean = false): ControlOption<TimeRange>[]
    {
        let controlOptions = ranges.map((timeRange) => timeRange.controlOption);
        if (includeCustom) controlOptions.push(TimeRanges.CustomRange.controlOption);

        return controlOptions;
    }
}

export enum TimeRangeType
{
    All,
    Relative
}
