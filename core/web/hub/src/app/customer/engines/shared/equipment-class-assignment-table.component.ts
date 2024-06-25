import {Component, Injector, Input} from "@angular/core";
import {BlockTableComponent} from "app/customer/engines/shared/block-table.component";
import * as Models from "app/services/proxy/model/models";
import {DatatablePair, IProviderForMapHost, ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector   : "o3-equipment-class-assignment-table",
               templateUrl: "./equipment-class-assignment-table.component.html"
           })
export class EquipmentClassAssignmentTableComponent extends BlockTableComponent<Models.EquipmentClassAssignment, EquipmentClassAssignmentProvider> implements IProviderForMapHost
{
    provider: EquipmentClassAssignmentProvider;
    equipmentClassOptions: ControlOption<string>[] = [];
    private m_rules: Models.NormalizationRules;

    constructor(inj: Injector)
    {
        super(inj);
        this.provider = new EquipmentClassAssignmentProvider(this, "EquipmentClassAssignment", "Equipment Class Assignments", "Regex", "Equipment Class");
    }

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
    }

    getEquipmentClassOption(id: string): ControlOption<string>
    {
        return this.equipmentClassOptions.find((opt) => opt.id === id);
    }

    protected async initExtra()
    {
        this.equipmentClassOptions = await this.app.bindings.getEquipmentClasses(false, this.m_rules);
    }

    protected getItemName(): string
    {
        return "Equipment Class Assignments";
    }

    protected getProvider(): EquipmentClassAssignmentProvider
    {
        return this.provider;
    }

    protected parseContents(contents: string): Models.EquipmentClassAssignment[]
    {
        let data: Models.EquipmentClassAssignment[] = JSON.parse(contents);
        for (let item of data)
        {
            Models.EquipmentClassAssignment.fixupPrototype(item);
        }

        return data;
    }
}

class EquipmentClassAssignmentProvider extends ProviderForMappableList<Models.EquipmentClassAssignment>
{
    constructor(private m_host: EquipmentClassAssignmentTableComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.EquipmentClassAssignment): string
    {
        return item.regex;
    }

    public setKey(item: Models.EquipmentClassAssignment,
                  key: string): void
    {
        item.regex = key;
    }

    public isOrdered(): boolean
    {
        return true;
    }

    protected allocate(): Models.EquipmentClassAssignment
    {
        return Models.EquipmentClassAssignment.newInstance({
                                                               regex: ""
                                                           });
    }

    protected getText(data: Models.EquipmentClassAssignment): string
    {
        let ec = this.m_host.getEquipmentClassOption(data.equipmentClassId);
        return `"${ec ? ec.label : ""}" Case ${data.caseSensitive ? "Sensitive" : "Insensitive"}`;
    }

    protected shouldInclude(data: Models.EquipmentClassAssignment,
                            filterLowercase: string): boolean
    {
        let ec = this.m_host.getEquipmentClassOption(data.equipmentClassId);
        return this.contains(ec ? ec.label : "", filterLowercase);
    }

    public sort(a: Models.EquipmentClassAssignment,
                b: Models.EquipmentClassAssignment): number
    {
        return 0;
    }
}
