import * as Models from "app/services/proxy/model/models";

export class TimeDurationExtended
{
    constructor(public readonly model: Models.TimeDuration)
    {

    }

    public static newModel()
    {
        return Models.TimeDuration.newInstance(
            {
                unit  : Models.ChronoUnit.DAYS,
                amount: 0
            }
        );
    }

    public static getTimeOffsetString(offset: Models.TimeDuration): string
    {
        if (offset && offset.amount)
        {
            let unitAbbreviation = "h";
            if (offset.unit === Models.ChronoUnit.DAYS)
            {
                unitAbbreviation = "d";
            }
            else if (offset.unit === Models.ChronoUnit.WEEKS)
            {
                unitAbbreviation = "w";
            }

            return `(${offset.amount > 0 ? "+" : ""}${offset.amount}${unitAbbreviation})`;
        }

        return "";
    }

    public static getTimeOffset(offset: Models.TimeDuration): number
    {
        if (offset?.amount && offset?.unit)
        {
            switch (offset?.unit)
            {
                case Models.ChronoUnit.NANOS:
                    return offset.amount * 1E-9;

                case Models.ChronoUnit.MICROS:
                    return offset.amount * 1E-6;

                case Models.ChronoUnit.MILLIS:
                    return offset.amount * 1E-3;

                case Models.ChronoUnit.SECONDS:
                    return offset.amount;

                case Models.ChronoUnit.MINUTES:
                    return offset.amount * 60;

                case Models.ChronoUnit.HOURS:
                    return offset.amount * 60 * 60;

                case Models.ChronoUnit.DAYS:
                    return offset.amount * 24 * 60 * 60;

                case Models.ChronoUnit.WEEKS:
                    return offset.amount * 7 * 24 * 60 * 60;

                case Models.ChronoUnit.MONTHS:
                    return offset.amount * 30 * 24 * 60 * 60;

                case Models.ChronoUnit.YEARS:
                    return offset.amount * 365 * 24 * 60 * 60;
            }
        }

        return 0;
    }
}
