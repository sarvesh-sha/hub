import {ChangeDetectionStrategy, Component, Input, Optional} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import * as Models from "app/services/proxy/model/models";
import {ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector       : "o3-classification-rules-class-editor",
               styleUrls      : ["./classification-detail-page.component.scss"],
               templateUrl    : "./classification-rules-class-editor.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationRulesClassEditorComponent
{
    pointClasses: ProviderForPointClass;
    equipClasses: ProviderForEquipmentClass;
    locationClasses: ProviderForLocationClass;

    @Input()
    public pointClassOptions: ControlOption<string>[] = [];

    private m_rules: Models.NormalizationRules;

    @Input()
    public set rules(rules: Models.NormalizationRules)
    {
        this.m_rules = rules;
        if (this.m_rules)
        {
            this.bind();
        }
    }

    public get rules(): Models.NormalizationRules
    {
        return this.m_rules;
    }

    constructor(@Optional() public host: ClassificationDetailPageComponent)
    {
        this.pointClasses = new ProviderForPointClass(host, "pointClass", "Point Class(es)", "Name", "Description");

        this.equipClasses = new ProviderForEquipmentClass(host, "equipClass", "Equipment Class(es)", "Name", "Description");

        this.locationClasses = new ProviderForLocationClass(host, "locationClass", "Location Class(es)", "Name", "Description");
    }

    get units()
    {
        return this.host.units.map((u) => new ControlOption(u.id, `${u.label} - ${u.id}`));
    }

    bind()
    {
        this.pointClasses.bindList(this.rules.pointClasses);

        this.equipClasses.bindList(this.rules.equipmentClasses);

        this.locationClasses.bindList(this.rules.locationClasses);
    }

    exportPointClasses()
    {
        this.host.exportSection("Point Class Export", "PointClass", this.rules.pointClasses);
    }

    async importPointClasses()
    {
        await this.host.importSection("Point Class Import", (contents) =>
        {
            let rules          = this.rules;
            rules.pointClasses = JSON.parse(contents);
            return JSON.stringify(rules);
        });

        await this.host.initClassOptions();
    }

    exportEquipmentClasses()
    {
        this.host.exportSection("Equipment Class Export", "EquipmentClass", this.rules.equipmentClasses);
    }

    async importEquipmentClasses()
    {
        await this.host.importSection("Equipment Class Import", (contents) =>
        {
            let rules              = this.rules;
            rules.equipmentClasses = JSON.parse(contents);
            return JSON.stringify(rules);
        });

        await this.host.initClassOptions();
    }

    exportLocationClasses()
    {
        this.host.exportSection("Location Class Export", "LocationClass", this.rules.locationClasses);
    }

    async importLocationClasses()
    {
        await this.host.importSection("Location Class Import", (contents) =>
        {
            let rules             = this.rules;
            rules.locationClasses = JSON.parse(contents);
            return JSON.stringify(rules);
        });

        await this.host.initClassOptions();
    }
}

class ProviderForPointClass extends ProviderForMappableList<Models.PointClass>
{
    constructor(private m_host: ClassificationDetailPageComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.PointClass): string
    {
        return item.pointClassName;
    }

    public setKey(item: Models.PointClass,
                  key: string): void
    {
        item.pointClassName = key;
    }

    public sort(a: Models.PointClass,
                b: Models.PointClass): number
    {
        return a.id - b.id;
    }

    public isOrdered(): boolean
    {
        return false;
    }

    protected allocate(): Models.PointClass
    {
        return Models.PointClass.newInstance({
                                                 unitId              : -1,
                                                 kindId              : -1,
                                                 disabled            : false,
                                                 ignorePointIfMatched: false,
                                                 type                : Models.PointClassType.Value
                                             });
    }

    protected getText(data: Models.PointClass): string
    {
        if (!data) return "";

        let res = [data.pointClassDescription];

        if (data.id)
        {
            res.push(`ID: ${data.id}`);
        }

        if (data.type)
        {
            res.push(`Type: ${data.type}`);
        }

        if (data.unit)
        {
            let unit = this.m_host.getUnit(data.unit);
            res.push(`Unit: ${unit ? unit.label : data.unit}`);
        }

        if (data.aliasPointClassId)
        {
            let pc = this.m_host.getPointClassOption(data.aliasPointClassId);
            res.push(`Alias: ${pc ? pc.label : ""}`);
        }

        res.push(`Ignored: ${data.ignorePointIfMatched}`);

        return res.join(" | ");
    }

    protected shouldInclude(data: Models.PointClass,
                            filterLowercase: string): boolean
    {
        if (!data) return false;
        if (this.contains(data.pointClassName, filterLowercase)) return true;
        return this.contains(data.pointClassDescription, filterLowercase);
    }

    unitChanged(unit: string[])
    {
        this.edited.value.unit = unit.length > 0 ? UtilsService.parseEnumValue(Models.EngineeringUnits, unit[0]) : null;
    }

    aliasPointClassChanged(ids: string[])
    {
        this.edited.value.aliasPointClassId = ids.length > 0 ? ids[0] : null;
    }

    public async save()
    {
        super.save();
        await this.m_host.initClassOptions();
    }
}

class ProviderForEquipmentClass extends ProviderForMappableList<Models.EquipmentClass>
{
    constructor(private m_host: ClassificationDetailPageComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.EquipmentClass): string
    {
        return item.equipClassName;
    }

    public setKey(item: Models.EquipmentClass,
                  key: string): void
    {
        item.equipClassName = key;
    }

    public sort(a: Models.EquipmentClass,
                b: Models.EquipmentClass): number
    {
        return a.id - b.id;
    }

    public isOrdered(): boolean
    {
        return false;
    }

    protected allocate(): Models.EquipmentClass
    {
        return new Models.EquipmentClass();
    }

    protected getText(data: Models.EquipmentClass): string
    {
        if (!data) return "";

        let res = [data.description];

        if (data.id)
        {
            res.push(`ID: ${data.id}`);
        }

        return res.join(" | ");
    }

    protected shouldInclude(data: Models.EquipmentClass,
                            filterLowercase: string): boolean
    {
        if (!data) return false;
        if (this.contains(data.equipClassName, filterLowercase)) return true;
        return this.contains(data.description, filterLowercase);
    }

    public async save()
    {
        super.save();
        await this.m_host.initClassOptions();
    }
}


class ProviderForLocationClass extends ProviderForMappableList<Models.LocationClass>
{
    constructor(private m_host: ClassificationDetailPageComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    public getKey(item: Models.LocationClass): string
    {
        return item.id;
    }

    public setKey(item: Models.LocationClass,
                  key: string): void
    {
        item.id = <Models.LocationType>key;
    }

    public sort(a: Models.LocationClass,
                b: Models.LocationClass): number
    {
        return UtilsService.compareStrings(a.id, b.id, true);
    }

    public isOrdered(): boolean
    {
        return false;
    }

    protected allocate(): Models.LocationClass
    {
        return new Models.LocationClass();
    }

    protected getText(data: Models.LocationClass): string
    {
        if (!data) return "";

        let res = [];

        if (data.description)
        {
            res.push(data.description);
        }

        if (data.azureDigitalTwin)
        {
            res.push(`ADT: ${data.azureDigitalTwin}`);
        }

        return res.join(" | ");
    }

    protected shouldInclude(data: Models.LocationClass,
                            filterLowercase: string): boolean
    {
        if (!data) return false;
        if (this.contains(data.id, filterLowercase)) return true;
        return this.contains(data.description, filterLowercase);
    }

    public async save()
    {
        super.save();
        await this.m_host.initClassOptions();
    }
}
