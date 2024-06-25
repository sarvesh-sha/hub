import * as moment from "moment-timezone";

export class MomentHelper
{
    public static now(): moment.Moment
    {
        return MomentHelper.parse(moment.now());
    }

    public static isSame(a: moment.Moment,
                         b: moment.Moment): boolean
    {
        if (!a || !b) return !a == !b;

        return a.isSame(b);
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

    public static parse(value: Date | moment.Moment | string | number): moment.Moment
    {
        // under systemjs, moment is actually exported as the default export, so we account for that
        const momentConstructor: (value?: any) => moment.Moment = (<any>moment).default || moment;

        return momentConstructor(value);
    }

    public static parseOrNull(value: Date | moment.Moment | string | number | undefined | null): moment.Moment
    {
        if (!value) return null;

        return MomentHelper.parse(value);
    }

    public static parseAsDate(value: Date | moment.Moment | string | number): Date
    {
        return MomentHelper.parse(value)
                           .toDate();
    }
}
