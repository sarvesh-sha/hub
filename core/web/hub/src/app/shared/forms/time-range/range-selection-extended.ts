import {AggregationHelper} from "app/services/domain/aggregation.helper";
import * as Models from "app/services/proxy/model/models";
import {ChartTimeRange, ComparisonChartRangeSelection, TimeRange, TimeRanges, TimeRangeValues} from "framework/ui/charting/core/time";
import {MomentHelper} from "framework/ui/formatting/date-format.pipe";
import moment from "framework/utils/moment";

export class RangeSelectionExtended
{
    public readonly model: Models.RangeSelection;
    public readonly rangeResolved: TimeRange;
    public readonly rangeValues: TimeRangeValues;

    constructor(model: Models.RangeSelection)
    {
        this.model = model || RangeSelectionExtended.newModel();
        RangeSelectionExtended.fixupModel(this.model);

        this.rangeResolved = this.model ? TimeRanges.resolve(this.model.range, false) : null;
        this.rangeValues   = this.rangeResolved ? this.rangeResolved.getTimeRangeValues(this.model.zone) : null;
    }

    public static fixupModel(model: Models.RangeSelection)
    {
        if (!model.zoneCreated)
        {
            model.zoneCreated = MomentHelper.getLocalZone();
        }
    }

    public static newModel(timeRange: Models.TimeRangeId = Models.TimeRangeId.Last24Hours): Models.RangeSelection
    {
        return Models.RangeSelection.newInstance({
                                                     range      : timeRange,
                                                     zone       : null,
                                                     start      : null,
                                                     end        : null,
                                                     zoneCreated: MomentHelper.getLocalZone()
                                                 });
    }

    public static newRange(start: Date,
                           end: Date,
                           zone?: string): Models.RangeSelection
    {
        return Models.RangeSelection.newInstance({
                                                     start      : start,
                                                     end        : end,
                                                     zone       : zone,
                                                     zoneCreated: MomentHelper.getLocalZone()
                                                 });
    }

    public static getDisplayName(range: Models.RangeSelection): string
    {
        if (!range) return "";
        let rangeExtended = new RangeSelectionExtended(range);
        return rangeExtended.displayName;
    }

    public static getFilterableDisplayName(range: Models.FilterableTimeRange): string
    {
        if (!range || !range.range) return "";
        let name = this.getDisplayName(range.range);
        if (range.isFilterApplied) name += ": " + range.name;
        return name;
    }

    public static fromTimeRangeId(id: Models.TimeRangeId)
    {
        let range   = new Models.RangeSelection();
        range.range = id;

        return new RangeSelectionExtended(range);
    }

    public static areEquivalent(rangeA: Models.RangeSelection,
                                rangeB: Models.RangeSelection): boolean
    {
        if (!rangeA && !rangeB) return true;
        if (!rangeA || !rangeB) return false;
        if (rangeA.range != rangeB.range) return false;
        if (rangeA.zone != rangeB.zone) return false;
        if (rangeA.start != rangeB.start) return false;
        if (rangeA.end != rangeB.end) return false;
        if (rangeA.zoneCreated != rangeB.zoneCreated) return false;

        return true;
    }

    get displayName(): string
    {
        if (this.rangeResolved && this.rangeResolved != TimeRanges.CustomRange) return this.rangeResolved.displayName;

        // let standardFormat: string = "M/D [at] h:mm a";
        let hr24Format: string = "M/D [at] k:mm";

        let m1 = MomentHelper.parse(this.model.start);
        let m2 = MomentHelper.parse(this.model.end);

        let dayStart1 = MomentHelper.startOf(m1, "day");
        let dayStart2 = MomentHelper.startOf(m2, "day");

        if (dayStart1.valueOf() === m1.valueOf() && dayStart2.valueOf() === m2.valueOf())
        {
            hr24Format = "M/D";
        }

        let p1 = m1.format(hr24Format)
                   .trim();
        let p2 = m2.format(hr24Format)
                   .trim();

        return `${p1} to ${p2}`;
    }

    getMin(): moment.Moment
    {
        let date: moment.Moment;
        if (this.rangeValues)
        {
            date = this.rangeValues.range.minAsMoment;
            date = MomentHelper.toZone(date, this.model.zone);
        }
        else
        {
            date = moment(this.model.start);
            date = MomentHelper.toZone(date, this.model.zone, true);
        }

        return date;
    }

    getMax(): moment.Moment
    {
        let date: moment.Moment;
        if (this.rangeValues)
        {
            date = this.rangeValues.range.maxAsMoment;
            date = MomentHelper.toZone(date, this.model.zone);
        }
        else
        {
            date = moment(this.model.end);
            date = MomentHelper.toZone(date, this.model.zone, true);
        }

        return date;
    }

    getChartRange(): ChartTimeRange
    {
        let range         = new ChartTimeRange();
        range.minAsMoment = this.getMin();
        range.maxAsMoment = this.getMax();

        return range;
    }

    getPreviousChartRange(): ComparisonChartRangeSelection
    {
        if (this.rangeValues) return this.rangeValues.comparisonRange;

        let previous = new ComparisonChartRangeSelection();
        let min      = this.getMin();
        let max      = this.getMax();
        let diff     = moment.duration(max.diff(min));

        let offsetUnits: moment.unitOfTime.DurationConstructor = "minutes";
        let offset                                             = diff.as("minutes");
        let previousMax                                        = moment(min);
        let previousMin                                        = moment(min)
            .subtract(diff);

        previous.minAsMoment          = previousMin;
        previous.maxAsMoment          = previousMax;
        previous.comparisonOffset     = offset;
        previous.comparisonOffsetUnit = offsetUnits;

        return previous;
    }

    splitBasedOnGranularity(granularity: Models.AggregationGranularity): Models.RangeSelection[]
    {
        let timeUnit = AggregationHelper.getMomentUnitFromGranularity(granularity);
        if (!timeUnit)
        {
            return [this.model];
        }

        let ranges: Models.RangeSelection[] = [];
        let start                           = this.getMin();
        let end                             = this.getMax();

        start = MomentHelper.startOf(start, timeUnit);

        while (start.valueOf() < end.valueOf())
        {
            let rangeStart = start.toDate();
            start.add(1, timeUnit);
            let rangeEnd = start.toDate();

            let range = Models.RangeSelection.newInstance({
                                                              start      : rangeStart,
                                                              end        : rangeEnd,
                                                              zone       : this.model.zone,
                                                              zoneCreated: this.model.zone || MomentHelper.getLocalZone()
                                                          });
            ranges.push(range);
        }

        return ranges;
    }
}

export const TimeZoneLocal: string = "local";

export const DaysOfWeek: Models.DayOfWeek[] = [
    Models.DayOfWeek.SUNDAY,
    Models.DayOfWeek.MONDAY,
    Models.DayOfWeek.TUESDAY,
    Models.DayOfWeek.WEDNESDAY,
    Models.DayOfWeek.THURSDAY,
    Models.DayOfWeek.FRIDAY,
    Models.DayOfWeek.SATURDAY
];

export class RecurringWeeklyScheduleExtended
{
    public static secPerQuarterHour: number = 60 * 15; // 900
    public static secPerDay: number         = RecurringWeeklyScheduleExtended.secPerQuarterHour * 4 * 24; // 86400
    public static quarterHourPerDay: number = RecurringWeeklyScheduleExtended.secPerDay / RecurringWeeklyScheduleExtended.secPerQuarterHour; // 96

    private filterMap: boolean[];
    private filterMapValid: boolean = false;

    constructor(private m_schedule: Models.RecurringWeeklySchedule)
    {
    }

    set schedule(newSched: Models.RecurringWeeklySchedule)
    {
        this.filterMapValid = false;
        this.m_schedule     = RecurringWeeklyScheduleExtended.deepCopySchedule(newSched);
    }

    private setUpFilterMap(): void
    {
        this.filterMap = Array.from(new Array(RecurringWeeklyScheduleExtended.quarterHourPerDay * 7), () => false);

        for (let dayIdx = 0; dayIdx < this.m_schedule.days.length; dayIdx++)
        {
            let qhIdxOffset = RecurringWeeklyScheduleExtended.quarterHourPerDay * dayIdx;
            let ranges      = this.m_schedule.days[dayIdx].dailySchedule.ranges;
            for (let rangeIdx = 0; rangeIdx < ranges.length; rangeIdx++)
            {
                let startQH = Math.trunc(ranges[rangeIdx].offsetSeconds / RecurringWeeklyScheduleExtended.secPerQuarterHour);
                let endQH   = Math.min(Math.trunc((ranges[rangeIdx].offsetSeconds + ranges[rangeIdx].durationSeconds)
                                                  / RecurringWeeklyScheduleExtended.secPerQuarterHour), RecurringWeeklyScheduleExtended.quarterHourPerDay - 1);
                for (let i = startQH; i <= endQH; i++)
                {
                    this.filterMap[qhIdxOffset + i] = true;
                }
            }
        }
        this.filterMapValid = true;
    }

    /**
     *
     * returns quarter hour index into week if enabled and undefined if not
     *
     * @param offsetSecondsIntoWeek must be less than total num seconds in a week
     */
    private getEnabledQuarterHourIndex(offsetSecondsIntoWeek: number): number
    {
        if (!this.filterMapValid) this.setUpFilterMap();
        let idx = Math.trunc(offsetSecondsIntoWeek / RecurringWeeklyScheduleExtended.secPerQuarterHour);
        return this.filterMap[idx] ? idx : undefined;
    }

    public isOffsetEnabled(offsetSecondsIntoWeek: number): boolean
    {
        return this.getEnabledQuarterHourIndex(offsetSecondsIntoWeek) !== undefined;
    }

    /**
     * offsetSeconds1 must be from earlier point in time than offsetSeconds2
     */
    public ofSameEnabledOffsetRange(offsetSeconds1: number,
                                    offsetSeconds2: number): boolean
    {
        let offset1 = this.getEnabledQuarterHourIndex(offsetSeconds1),
            offset2;
        if (offset1 !== undefined) offset2 = this.getEnabledQuarterHourIndex(offsetSeconds2);
        if (offset2 !== undefined)
        {
            while (offset1 !== offset2)
            {
                if (this.filterMap[offset1])
                {
                    offset1 = (offset1 + 1) % this.filterMap.length;
                }
                else
                {
                    break;
                }
            }
        }

        return offset2 && offset1 === offset2;
    }

    public getOffsetIntoWeek(firstTimestampInSeconds: number): number
    {
        let firstTimestampInMilli = firstTimestampInSeconds * 1_000;

        let prevSunday = MomentHelper.startOf(MomentHelper.parse(firstTimestampInMilli), "week");

        return (firstTimestampInMilli - prevSunday.valueOf()) / 1000;
    }

    equals(other: Models.RecurringWeeklySchedule): boolean
    {
        return this.m_schedule === other || this.m_schedule.days.every((day,
                                                                        dayIdx) => this.daysAreEqual(day, other.days[dayIdx]));
    }

    private daysAreEqual(day1: Models.DailyScheduleWithDayOfWeek,
                         day2: Models.DailyScheduleWithDayOfWeek): boolean
    {
        return day1 === day2 ||
               (day1.dayOfWeek === day2.dayOfWeek &&
                day1.dailySchedule.ranges.length === day2.dailySchedule.ranges.length &&
                (day1.dailySchedule.ranges.every((range,
                                                  rangeIdx) => this.rangesAreEqual(range, day2.dailySchedule.ranges[rangeIdx]))));
    }

    private rangesAreEqual(range1: Models.RelativeTimeRange,
                           range2: Models.RelativeTimeRange): boolean
    {
        return range1.offsetSeconds === range2.offsetSeconds && range1.durationSeconds === range2.durationSeconds;
    }

    public static scheduleIsValid(sched: Models.RecurringWeeklySchedule,
                                  isWeekly: boolean = true): boolean
    {
        return sched && sched.days && (!isWeekly || sched.days.length === 7) && sched.days.every((day) =>
               day.dayOfWeek && day.dailySchedule && day.dailySchedule.ranges && day.dailySchedule.ranges.every((range) =>
               range.durationSeconds !== undefined && range.offsetSeconds !== undefined &&
               range.durationSeconds >= 0 && range.offsetSeconds >= 0 && range.offsetSeconds < RecurringWeeklyScheduleExtended.secPerDay));
    }

    public static deepCopySchedule(schedule: Models.RecurringWeeklySchedule): Models.RecurringWeeklySchedule
    {
        return Models.RecurringWeeklySchedule.deepClone(schedule);
    }

    public static generateFullWeekSchedule(): Models.RecurringWeeklySchedule
    {
        return RecurringWeeklyScheduleExtended.generateWeeklySchedule(DaysOfWeek);
    }

    public static generateWeeklySchedule(daysOn: Models.DayOfWeek[]): Models.RecurringWeeklySchedule
    {
        return Models.RecurringWeeklySchedule.newInstance(
            {
                days: DaysOfWeek.map((day) =>
                                     {
                                         let included: boolean = daysOn.some((onDay) => onDay === day);

                                         return Models.DailyScheduleWithDayOfWeek.newInstance(
                                             {
                                                 dayOfWeek    : day,
                                                 dailySchedule: Models.DailySchedule.newInstance(
                                                     {
                                                         ranges: included ?
                                                             [
                                                                 Models.RelativeTimeRange.newInstance({
                                                                                                          offsetSeconds  : 0,
                                                                                                          durationSeconds: RecurringWeeklyScheduleExtended.secPerDay
                                                                                                      })
                                                             ] :
                                                             []
                                                     })
                                             }
                                         );
                                     })
            });
    }
}

export class FilterableTimeRangeExtended
{
    public filterableTimeRange: Models.FilterableTimeRange;

    constructor(filterableTimeRange: Models.FilterableTimeRange)
    {
        if (!filterableTimeRange.name) filterableTimeRange.name = "";
        filterableTimeRange.isFilterApplied = !filterableTimeRange.isFilterApplied;
        if (!filterableTimeRange.filter) filterableTimeRange.filter = RecurringWeeklyScheduleExtended.generateFullWeekSchedule();
        this.filterableTimeRange = filterableTimeRange;
    }

    equals(other: Models.FilterableTimeRange): boolean
    {
        let thisFilterableRange = this.filterableTimeRange;

        return RangeSelectionExtended.areEquivalent(thisFilterableRange.range, other.range) &&
               new RecurringWeeklyScheduleExtended(thisFilterableRange.filter).equals(other.filter) &&
               thisFilterableRange.isFilterApplied === other.isFilterApplied &&
               thisFilterableRange.name === other.name;
    }

    functionallyEquivalentFilter(other: Models.FilterableTimeRange): boolean
    {
        let thisFilterableRange = this.filterableTimeRange;

        if (thisFilterableRange.isFilterApplied)
        {
            // other filter applied and is equivalent to this filter or other filter is not applied and this filter has all allowed
            if (other.isFilterApplied)
            {
                return new RecurringWeeklyScheduleExtended(thisFilterableRange.filter).equals(other.filter);
            }
            else
            {
                return thisFilterableRange.filter.days.every((day) => day.dailySchedule.ranges.every(
                    (range) => range.offsetSeconds === 0 && range.durationSeconds === RecurringWeeklyScheduleExtended.secPerDay));
            }
        }
        else
        {
            return !other.isFilterApplied || // other filter not applied or is allowing all
                   other.filter.days.every(
                       (day) => day.dailySchedule.ranges.every(
                           (range) => range.offsetSeconds === 0 && range.durationSeconds === RecurringWeeklyScheduleExtended.secPerDay));
        }
    }

    static newInstance(range: Models.RangeSelection,
                       filter?: Models.RecurringWeeklySchedule,
                       name: string             = "",
                       isFilterApplied: boolean = false): Models.FilterableTimeRange
    {
        return Models.FilterableTimeRange.newInstance({
                                                          name           : name,
                                                          isFilterApplied: isFilterApplied,
                                                          range          : range,
                                                          filter         : filter ?
                                                              RecurringWeeklyScheduleExtended.deepCopySchedule(filter) :
                                                              RecurringWeeklyScheduleExtended.generateFullWeekSchedule()
                                                      });
    }
}
