import {Pipe, PipeTransform} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";

@Pipe({name: "o3Pluralize"})
export class PluralizePipe implements PipeTransform
{
    transform(text: string,
              count: number = 2): string
    {
        return UtilsService.pluralize(text, count);
    }
}
