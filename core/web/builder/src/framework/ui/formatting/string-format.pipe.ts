import {Pipe, PipeTransform} from "@angular/core";

@Pipe({name: "o3TitleCase"})
export class TitleCaseFormatPipe implements PipeTransform
{
    transform(value: any): string
    {
        if (!value) return "";

        return value.toString()
                    .replace(/([a-z])([A-Z])/g, "$1 $2") // insert a space between lower & upper
                    .replace(/\b([A-Z]+)([A-Z])([a-z])/, "$1 $2$3") // space before last upper in a sequence followed by lower
                    .replace(/^./, function (str: string) { return str.toUpperCase(); }); // uppercase the first character
    }
}

@Pipe({name: "o3NumberWithSeparators"})
export class NumberWithSeparatorsPipe implements PipeTransform
{
    transform(value: number): string
    {
        return NumberWithSeparatorsPipe.format(value);
    }

    public static format(value: number): string
    {
        if (value === undefined || value === null) return "";

        let formatter = new Intl.NumberFormat(undefined, {
            useGrouping: true
        });

        return formatter.format(value);
    }
}

@Pipe({name: "o3Currency"})
export class CurrencyPipe implements PipeTransform
{
    transform(value: number,
              currency?: string): string
    {
        return CurrencyPipe.format(value, currency);
    }

    public static format(value: number,
                         currency?: string): string
    {
        if (value === undefined || value === null) return "";

        let formatter = new Intl.NumberFormat(undefined, {
            style   : "currency",
            currency: currency || "USD"
        });

        return formatter.format(value);
    }
}
