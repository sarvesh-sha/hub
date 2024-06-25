import {Component, Injector} from "@angular/core";
import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";
import * as Models from "app/services/proxy/model/models";
import {DatatablePair, IProviderForMapHost, ProviderForMappableList} from "app/shared/tables/provider-for-map";

@Component({
               selector   : "o3-subnet-table",
               templateUrl: "./subnet-table.component.html"
           })
export class SubnetTableComponent extends BlockTableComponent<Models.FilteredSubnet, SubnetProvider> implements IProviderForMapHost
{
    provider: SubnetProvider;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new SubnetProvider(this, "FilteredSubnet", "Subnets", "CIDR", "Notes");
    }

    protected async initExtra()
    {
    }

    protected getItemName(): string
    {
        return "Subnets";
    }

    protected getProvider(): SubnetProvider
    {
        return this.provider;
    }

    protected parseContents(contents: string): Models.FilteredSubnet[]
    {
        let data: Models.FilteredSubnet[] = JSON.parse(contents);
        for (let item of data)
        {
            Models.FilteredSubnet.fixupPrototype(item);
        }

        return data;
    }
}

class SubnetProvider extends ProviderForMappableList<Models.FilteredSubnet>
{
    constructor(private m_host: SubnetTableComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.FilteredSubnet): string
    {
        return item.cidr;
    }

    public setKey(item: Models.FilteredSubnet,
                  key: string): void
    {
        item.cidr = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.FilteredSubnet
    {
        return Models.FilteredSubnet.newInstance({
                                                     cidr : "",
                                                     notes: ""
                                                 });
    }

    protected getText(data: Models.FilteredSubnet): string
    {
        return data.notes;
    }

    protected shouldInclude(data: Models.FilteredSubnet,
                            filterLowercase: string): boolean
    {
        return this.contains(data.cidr, filterLowercase) || this.contains(data.notes, filterLowercase);
    }

    public sort(a: Models.FilteredSubnet,
                b: Models.FilteredSubnet): number
    {
        return 0;
    }
}
