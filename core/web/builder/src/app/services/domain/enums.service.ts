import {Injectable} from "@angular/core";
import {ApiService} from "app/services/domain/api.service";

import * as Models from "app/services/proxy/model/models";
import {Lookup, UtilsService} from "framework/services/utils.service";

@Injectable()
export class EnumsService
{
    private lookup: Lookup<Models.EnumDescriptor[]> = {};

    constructor(private api: ApiService)
    {
    }

    async getInfos(enumType: string,
                   sort: boolean): Promise<Models.EnumDescriptor[]>
    {
        let infos = this.lookup[enumType];
        if (!infos)
        {
            infos = await this.api.enums.describe(enumType);

            if (sort)
            {
                // Wait and sort the results, to make it nicer for the caller.
                infos.sort((a,
                            b) => UtilsService.compareStrings(a.displayName, b.displayName, true));
            }

            this.lookup[enumType] = infos;
        }

        return infos;
    }

    public static find<T>(entries: Models.EnumDescriptor[],
                          target: T): Models.EnumDescriptor
    {
        for (let desc of entries)
        {
            if (desc.id == <any>target)
            {
                return desc;
            }
        }

        return null;
    }
}
