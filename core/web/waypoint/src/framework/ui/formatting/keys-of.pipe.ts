import {Pipe, PipeTransform} from "@angular/core";

import {UtilsService} from "framework/services/utils.service";

@Pipe({name: "o3KeysOf"})
export class KeysOfPipe implements PipeTransform
{
    transform(value: { [key: string]: any },
              ...args: any[]): string[]
    {
        return UtilsService.extractKeysFromMap(value);
    }
}
