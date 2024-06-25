import {Component, Injector} from "@angular/core";
import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";
import * as Models from "app/services/proxy/model/models";
import {DatatablePair, ProviderForMappableList} from "app/shared/tables/provider-for-map";

@Component({
               selector   : "o3-regex-replace-table",
               templateUrl: "./regex-replace-table.component.html"
           })
export class RegexReplaceTableComponent extends BlockTableComponent<Models.RegexReplacement, RegexReplaceProvider>
{
    provider: RegexReplaceProvider;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new RegexReplaceProvider(this, "regexReplacements", "Regex Replacements", "Regex", "Replacement");
    }

    protected getItemName(): string
    {
        return "Regex Replacements";
    }

    protected getProvider(): RegexReplaceProvider
    {
        return this.provider;
    }

    protected async initExtra()
    {
    }

    protected parseContents(contents: string): Models.RegexReplacement[]
    {
        let data: Models.RegexReplacement[] = JSON.parse(contents);
        for (let item of data)
        {
            Models.RegexReplacement.fixupPrototype(item);
        }

        return data;
    }
}

class RegexReplaceProvider extends ProviderForMappableList<Models.RegexReplacement>
{
    public getKey(item: Models.RegexReplacement): string
    {
        return item.regex;
    }

    public setKey(item: Models.RegexReplacement,
                  key: string): void
    {
        item.regex = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.RegexReplacement
    {
        return Models.RegexReplacement.newInstance({
                                                       regex      : "",
                                                       replacement: ""
                                                   });
    }

    protected getText(data: Models.RegexReplacement): string
    {
        return `"${data.replacement || ""}" Case ${data.caseSensitive ? "Sensitive" : "Insensitive"}${data.comment ? " - " + data.comment : ""}`;
    }

    protected shouldInclude(data: Models.RegexReplacement,
                            filterLowercase: string): boolean
    {
        return this.contains(data.regex + data.replacement, filterLowercase);
    }

    public sort(a: Models.RegexReplacement,
                b: Models.RegexReplacement): number
    {
        return 0;
    }
}
