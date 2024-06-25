import {Pipe, PipeTransform} from "@angular/core";

import moment from "framework/utils/moment";
import {duration} from "moment";

@Pipe({name: "o3ShortDate"})
export class ShortDateFormatPipe implements PipeTransform
{
    transform(value: Date | moment.Moment | string | number,
              ...args: any[]): string
    {
        if (!value) return "";
        return MomentHelper.parse(value)
                           .format("l");
    }
}

@Pipe({name: "o3ShortTime"})
export class ShortTimeFormatPipe implements PipeTransform
{
    transform(value: Date | moment.Moment | string | number,
              ...args: any[]): string
    {
        if (!value) return "";
        return MomentHelper.parse(value)
                           .format("LT");
    }
}

@Pipe({name: "o3Time"})
export class TimeFormatPipe implements PipeTransform
{
    transform(value: Date | moment.Moment | string | number,
              ...args: any[]): string
    {
        if (!value) return "";
        return MomentHelper.parse(value)
                           .format("LTS");
    }
}

@Pipe({name: "o3LongDate"})
export class LongDateFormatPipe implements PipeTransform
{
    transform(value: Date | moment.Moment | string | number,
              ...args: any[]): string
    {
        if (!value) return "";
        return MomentHelper.parse(value)
                           .format("ll");
    }
}

@Pipe({name: "o3LongDateTime"})
export class LongDateTimeFormatPipe implements PipeTransform
{
    transform(value: Date | moment.Moment | string | number,
              ...args: any[]): string
    {
        if (!value) return "";
        return MomentHelper.parse(value)
                           .format("L LTS");
    }
}


@Pipe({name: "o3Elapsed"})
export class ElapsedFormatPipe implements PipeTransform
{
    transform(value: Date,
              start: Date): string
    {
        if (!value) return "";

        let moment = MomentHelper.parse(value);
        return moment.from(start, true);
    }
}

export class MomentHelper
{
    public static now(): moment.Moment
    {
        return MomentHelper.parse(moment.now());
    }

    public static nowWithLocalZone(): moment.Moment
    {
        return MomentHelper.toZone(MomentHelper.now(), MomentHelper.getLocalZone());
    }

    public static isSame(a: moment.Moment,
                         b: moment.Moment): boolean
    {
        if (!a || !b) return !a == !b;

        return a.isSame(b);
    }

    public static compareDates(a: Date,
                               b: Date): number
    {
        let timestampA = MomentHelper.parseOrNull(a);
        let timestampB = MomentHelper.parseOrNull(b);
        return MomentHelper.compare(timestampA, timestampB);
    }

    public static compare(a: moment.Moment,
                          b: moment.Moment): number
    {
        if (!a) return !b ? 0 : -1;  // Any date is after a null.

        if (!b) return 1; // Any date is after a null.

        return a.isBefore(b) ? -1 : a.isAfter(b) ? 1 : 0;
    }

    public static min(a: moment.Moment,
                      b: moment.Moment): moment.Moment
    {
        if (!a) return b;
        if (!b) return a;

        return a.isSameOrBefore(b) ? a : b;
    }

    public static max(a: moment.Moment,
                      b: moment.Moment): moment.Moment
    {
        if (!a) return b;
        if (!b) return a;

        return a.isSameOrAfter(b) ? a : b;
    }

    public static startOf(moment: moment.Moment,
                          unitOfTime: moment.unitOfTime.StartOf): moment.Moment
    {
        return moment.clone()
                     .startOf(unitOfTime);
    }

    public static endOf(moment: moment.Moment,
                        unitOfTime: moment.unitOfTime.StartOf): moment.Moment
    {
        return moment.clone()
                     .endOf(unitOfTime);
    }

    public static getDuration(amount: moment.DurationInputArg1,
                              unit: moment.unitOfTime.DurationConstructor): number
    {
        return duration(amount, unit)
            .asMilliseconds();
    }

    public static add(moment: moment.Moment,
                      amount?: moment.DurationInputArg1,
                      unit?: moment.unitOfTime.DurationConstructor): moment.Moment
    {
        return moment.clone()
                     .add(amount, unit);
    }

    public static subtract(moment: moment.Moment,
                           amount?: moment.DurationInputArg1,
                           unit?: moment.unitOfTime.DurationConstructor): moment.Moment
    {
        return moment.clone()
                     .subtract(amount, unit);
    }

    public static parse(value: Date | moment.Moment | string | number,
                        zone?: string): moment.Moment
    {
        // under systemjs, moment is actually exported as the default export, so we account for that
        const momentConstructor: (value?: any) => moment.Moment = (<any>moment).default || moment;

        let result = momentConstructor(value);
        return MomentHelper.toZone(result, zone);
    }

    public static parseOrNull(value: Date | moment.Moment | string | number | undefined | null,
                              zone?: string): moment.Moment
    {
        if (!value) return null;

        return MomentHelper.parse(value, zone);
    }

    public static parseAsDate(value: Date | moment.Moment | string | number): Date
    {
        return MomentHelper.parse(value)
                           .toDate();
    }

    public static fileNameFormat(value: moment.Moment = MomentHelper.now()): string
    {
        return value.format("YYYYMMDD_HHmmss");
    }

    public static friendlyFormatConciseUS(value: moment.Moment | number = MomentHelper.now()): string
    {
        if (typeof value === "number") value = MomentHelper.parse(value);

        let start = "M/D";
        let end   = " [at] h:mm A";
        if (value.year() !== new Date().getFullYear())
        {
            start += "/YY";
        }

        return value.format(start + end);
    }

    public static friendlyFormatVerboseUS(value: moment.Moment,
                                          withDay: boolean = true,
                                          withMs: boolean  = false): string
    {
        withMs = withMs ?? value.valueOf() % 10 != 0;

        let formatString = "ddd MMM ";
        if (withDay)
        {
            formatString += "D ";
        }

        if (value.year() !== new Date().getFullYear())
        {
            formatString += "Y ";
        }

        formatString += withMs ? "h:mm:ss.SSS a" : "h:mm:ss a";

        return value.format(formatString);
    }

    public static utcOffset(zone: moment.MomentZone | string = moment.tz.guess(),
                            timestamp: number                = new Date().valueOf()): number
    {
        let zone2 = typeof zone === "string" ? MomentHelper.timeZone(zone) : zone;
        return zone2.utcOffset(timestamp);
    }

    public static toZone(value: moment.Moment,
                         zoneName: string,
                         keepLocalTime: boolean = false)
    {
        if (!value || !zoneName) return value;

        if (zoneName == "local")
        {
            zoneName = MomentHelper.getLocalZone();
        }

        return value.tz(zoneName, keepLocalTime);
    }

    public static timeZone(zoneName: string): moment.MomentZone
    {
        if (!zoneName || zoneName == "local")
        {
            zoneName = MomentHelper.getLocalZone();
        }

        return moment.tz.zone(zoneName);
    }

    public static getZoneNames(): string[]
    {
        return moment.tz.names();
    }

    public static getLocalZone(): string
    {
        return moment.tz.guess();
    }
}
