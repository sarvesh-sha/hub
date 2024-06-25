import {Component, Injector} from "@angular/core";

import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";

import * as Models from "app/services/proxy/model/models";

import {DatatablePair, ProviderForMappableList} from "app/shared/tables/provider-for-map";

@Component({
               selector   : "o3-lookup-table",
               templateUrl: "./lookup-table.component.html"
           })
export class LookupTableComponent extends BlockTableComponent<Models.LookupEntry, LookupProvider>
{
    provider: LookupProvider;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new LookupProvider(this, "lookupEntries", "Lookup Entries", "Key", "Replacement");
    }

    protected getItemName(): string
    {
        return "Lookup Entries";
    }

    protected getProvider(): LookupProvider
    {
        return this.provider;
    }

    protected async initExtra()
    {
    }

    protected parseContents(contents: string,
                            fileName: string): Models.LookupEntry[]
    {
        let data: Models.LookupEntry[];

        if (fileName.endsWith(".csv"))
        {
            data = [];
            for (let line of contents.split("\n"))
            {
                let parts = line.split("\t");
                if (parts.length == 2)
                {
                    data.push(Models.LookupEntry.newInstance({
                                                                 key          : parts[0],
                                                                 value        : parts[1],
                                                                 caseSensitive: false
                                                             }));
                }
            }
        }
        else
        {
            data = JSON.parse(contents);
            for (let item of data)
            {
                Models.LookupEntry.fixupPrototype(item);
            }
        }

        return data;
    }
}

class LookupProvider extends ProviderForMappableList<Models.LookupEntry>
{
    public getKey(item: Models.LookupEntry): string
    {
        return item.key;
    }

    public setKey(item: Models.LookupEntry,
                  key: string): void
    {
        item.key = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.LookupEntry
    {
        return Models.LookupEntry.newInstance({
                                                  key  : "",
                                                  value: ""
                                              });
    }

    protected getText(data: Models.LookupEntry): string
    {
        return `"${data.value || ""}" Case ${data.caseSensitive ? "Sensitive" : "Insensitive"}`;
    }

    protected shouldInclude(data: Models.LookupEntry,
                            filterLowercase: string): boolean
    {
        return this.contains(data.key, filterLowercase) || this.contains(data.value, filterLowercase);
    }

    public sort(a: Models.LookupEntry,
                b: Models.LookupEntry): number
    {
        return 0;
    }
}
