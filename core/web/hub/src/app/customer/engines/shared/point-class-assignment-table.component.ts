import {Component, Injector, Input} from "@angular/core";
import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";
import * as Models from "app/services/proxy/model/models";
import {ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-point-class-assignment-table",
               templateUrl: "./point-class-assignment-table.component.html"
           })
export class PointClassAssignmentTableComponent extends BlockTableComponent<Models.PointClassAssignment, PointClassAssignmentProvider>
{
    provider: PointClassAssignmentProvider;
    pointClassOptions: ControlOption<string>[] = [];
    private m_rules: Models.NormalizationRules;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new PointClassAssignmentProvider(this, "PointClassAssignment", "Point Class Assignments", "Regex", "Point Class");
    }

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
    }

    protected getProvider(): PointClassAssignmentProvider
    {
        return this.provider;
    }

    protected async initExtra()
    {
        this.pointClassOptions = await this.app.bindings.getPointClasses(false, this.m_rules);
    }

    protected getItemName(): string
    {
        return "Point Class Assignments";
    }

    protected parseContents(contents: string): Models.PointClassAssignment[]
    {
        let data: Models.PointClassAssignment[] = JSON.parse(contents);
        for (let item of data)
        {
            Models.PointClassAssignment.fixupPrototype(item);
        }

        return data;
    }

    getPointClassOption(id: string): ControlOption<string>
    {
        return this.pointClassOptions.find((opt) => opt.id === id);
    }
}

class PointClassAssignmentProvider extends ProviderForMappableList<Models.PointClassAssignment>
{
    constructor(private m_host: PointClassAssignmentTableComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.PointClassAssignment): string
    {
        return item.regex;
    }

    public setKey(item: Models.PointClassAssignment,
                  key: string): void
    {
        item.regex = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.PointClassAssignment
    {
        return Models.PointClassAssignment.newInstance({
                                                           regex: ""
                                                       });
    }

    protected getText(data: Models.PointClassAssignment): string
    {
        let pc = this.m_host.getPointClassOption(data.pointClassId);
        return `"${pc ? pc.label : ""}" Case ${data.caseSensitive ? "Sensitive" : "Insensitive"}${data.comment ? " - " + data.comment : ""}`;
    }

    protected shouldInclude(data: Models.PointClassAssignment,
                            filterLowercase: string): boolean
    {
        let pc = this.m_host.getPointClassOption(data.pointClassId);
        return this.contains(pc ? pc.label : "", filterLowercase);
    }

    public sort(a: Models.PointClassAssignment,
                b: Models.PointClassAssignment): number
    {
        return 0;
    }
}
