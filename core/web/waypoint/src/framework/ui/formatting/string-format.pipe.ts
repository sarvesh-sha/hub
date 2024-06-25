import {Pipe, PipeTransform} from "@angular/core";

@Pipe({name: "o3TitleCase"})
export class TitleCaseFormatPipe implements PipeTransform
{
    transform(value: any): string
    {
        if (!value) return "";

        return value.toString()
                    // insert a space between lower & upper
                    .replace(/([a-z])([A-Z])/g, "$1 $2")
                    // space before last upper in a sequence followed by lower
                    .replace(/\b([A-Z]+)([A-Z])([a-z])/, "$1 $2$3")
                    // uppercase the first character
                    .replace(/^./, function (str: string) { return str.toUpperCase(); });
    }
}
