import {Pipe, PipeTransform} from "@angular/core";

import {Lookup, UtilsService} from "framework/services/utils.service";

@Pipe({name: "o3KeysOf"})
export class KeysOfPipe implements PipeTransform
{
    transform(value: Lookup<any>,
              ...args: any[]): string[]
    {
        return UtilsService.extractKeysFromMap(value);
    }
}
