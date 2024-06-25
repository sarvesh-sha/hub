import {ChangeDetectionStrategy, Component, Input} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";
import {LocationExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";
import {DatatablePair, ProviderForMappableList} from "app/shared/tables/provider-for-map";
import {UtilsService} from "framework/services/utils.service";
import {ControlOption} from "framework/ui/control-option";

@Component({
               selector       : "o3-classification-validation-rules-editor",
               templateUrl    : "./classification-validation-rules-editor.component.html",
               styleUrls      : ["./classification-detail-page.component.scss"],
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationValidationRulesEditorComponent
{
    equipmentRules: ProviderForEquipmentRule;
    pointClassRules: ProviderForPointClassRule;

    objectTypes: ControlOption<string>[];

    private m_rules: Models.ValidationRules;

    @Input()
    public set rules(rules: Models.ValidationRules)
    {
        this.m_rules = rules;
        if (this.m_rules)
        {
            this.bind();
        }
    }

    public get rules(): Models.ValidationRules
    {
        return this.m_rules;
    }

    constructor(public host: ClassificationDetailPageComponent)
    {
        this.equipmentRules  = new ProviderForEquipmentRule(this, "equipmentRules", "Equipment Rules", "Equipment Class", "Details");
        this.pointClassRules = new ProviderForPointClassRule(this, "pointClassRules", "Point Class Rules", "Point Class", "Details");
        this.objectTypes     = this.host.app.bindings.getBacnetObjectTypes();
    }

    async bind()
    {
        await this.host.loaded;
        this.equipmentRules.bindList(this.rules.equipmentRules);
        this.pointClassRules.bindList(this.rules.pointClassRules);
    }

    addPointRow()
    {
        this.equipmentRules.edited.value.points.push(Models.ValidationEquipmentRulePointClassCriteria.newInstance({
                                                                                                                      minNumber: 1,
                                                                                                                      maxNumber: 1
                                                                                                                  }));
    }

    getEquipmentClasses(): string[]
    {
        return this.rules.equipmentRules.map((r) => r.equipmentClassId);
    }

    getCurrentPointClasses(): string[]
    {
        if (!this.equipmentRules.edited) return [];
        return this.equipmentRules.edited.value.points.map((p) => p.pointClassId);
    }

    getPointClasses(): string[]
    {
        return this.rules.pointClassRules.map((r) => r.pointClassId);
    }

    exportEquipmentRules()
    {
        this.host.exportSection("Equipment Rules Export", "EquipmentRules", this.rules.equipmentRules);
    }

    async importEquipmentRules()
    {
        return this.host.importSection("Equipment Rules Import", (contents) =>
        {
            let rules = this.host.rules.validation;
            try
            {
                rules.equipmentRules = JSON.parse(contents);
            }
            catch (e)
            {
                rules.equipmentRules = this.importCSV(contents);
            }

            return JSON.stringify(this.host.rules);
        });
    }

    private importCSV(contents: string): Models.ValidationEquipmentRule[]
    {
        let rules: Models.ValidationEquipmentRule[] = [];

        let lines = contents.split("\n");

        for (let line of lines)
        {
            try
            {
                let parts = line.split(",")
                                .map((v) => v.trim());

                let [name, location, equipmentClass, quantityRaw, ...points] = parts;

                let locations = location.split("/")
                                        .map((v) => v.trim())
                                        .filter((v) => !!v);

                let [equipMin, equipMax] = this.parseQuantity(quantityRaw);

                let rule = Models.ValidationEquipmentRule.newInstance({
                                                                          name            : name,
                                                                          equipmentClassId: equipmentClass,
                                                                          locationNames   : locations,
                                                                          minNumber       : equipMin,
                                                                          maxNumber       : equipMax,
                                                                          points          : []
                                                                      });

                for (let i = 0; i < points.length; i += 2)
                {
                    let pointClass = points[i];
                    if (pointClass)
                    {
                        let [min, max] = this.parseQuantity(points[i + 1]);
                        let point      = Models.ValidationEquipmentRulePointClassCriteria.newInstance({
                                                                                                          pointClassId: pointClass,
                                                                                                          minNumber   : min,
                                                                                                          maxNumber   : max
                                                                                                      });
                        rule.points.push(point);
                    }
                }

                rules.push(rule);
            }
            catch (err)
            {}
        }

        return rules;
    }

    private parseQuantity(value: string): [number, number]
    {
        value      = value ?? "";
        let values = value.split("-")
                          .map((v) => v.trim())
                          .filter((v) => !!v);
        let min    = 1;
        let max    = 1;

        if (values.length)
        {
            min = parseInt(values.shift());
            max = min;
        }

        if (values.length)
        {
            max = parseInt(values.shift());
        }

        if (isNaN(min) || isNaN(max))
        {
            throw Error("Failed to parse quantity " + value);
        }

        return [
            min,
            max
        ];
    }

    exportPointClassRules()
    {
        this.host.exportSection("Point Class Rules Export", "PointClassRules", this.rules.pointClassRules);
    }

    async importPointClassRules()
    {
        return this.host.importSection("Point Class Rules Import", (contents) =>
        {
            let rules             = this.host.rules.validation;
            rules.pointClassRules = JSON.parse(contents);
            return JSON.stringify(this.host.rules);
        });
    }
}

class ProviderForEquipmentRule extends ProviderForMappableList<Models.ValidationEquipmentRule>
{
    locationSysId: string;

    constructor(private m_host: ClassificationValidationRulesEditorComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host.host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    protected allocate(): Models.ValidationEquipmentRule
    {
        return Models.ValidationEquipmentRule.newInstance({
                                                              equipmentClassId: "",
                                                              points          : []
                                                          });
    }

    public getKey(item: Models.ValidationEquipmentRule): string
    {
        let ec = this.m_host.host.getEquipmentClassOption(item.equipmentClassId);
        return [
            item.name,
            ec?.label || item.equipmentClassId,
            item.locationNames?.join(" - ") || ""
        ].filter((s) => !!s)
         .join(" | ");
    }

    public setKey(item: Models.ValidationEquipmentRule,
                  key: string): void
    {
        this.edited.key = this.getKey(item);
    }

    protected getText(data: Models.ValidationEquipmentRule): string
    {
        if (!data) return "";

        let res: string[] = [];

        if (data.minNumber != data.maxNumber)
        {
            res.push(`${data.minNumber} - ${data.maxNumber} equipment`);
        }
        else
        {
            res.push(`${data.minNumber} equipment`);
        }

        if (data.points.length !== 1)
        {
            res.push(`${data.points.length} points`);
        }
        else
        {
            res.push(`1 point`);
        }

        return res.join(" | ");
    }

    get valid(): boolean
    {
        let valid = super.valid;
        if (!valid) return false;

        let rule = this.edited.value;
        if (!rule.equipmentClassId || !rule.points || (!rule.minNumber && !rule.maxNumber))
        {
            return false;
        }

        let usedClasses = new Set<string>();
        return !rule.points.some((p) =>
                                 {
                                     if (!p.pointClassId || usedClasses.has(p.pointClassId))
                                     {
                                         return true;
                                     }

                                     usedClasses.add(p.pointClassId);
                                     return p.minNumber && !p.maxNumber;
                                 });
    }

    public isOrdered(): boolean
    {
        return false;
    }

    protected shouldInclude(data: Models.ValidationEquipmentRule,
                            filterLowercase: string): boolean
    {
        if (!data) return false;
        return this.contains(this.getKey(data), filterLowercase);
    }

    public sort(a: Models.ValidationEquipmentRule,
                b: Models.ValidationEquipmentRule): number
    {
        return UtilsService.compareStrings(this.getKey(a), this.getKey(b), true);
    }

    public itemClicked(columnId: string,
                       item: DatatablePair<string, Models.ValidationEquipmentRule>)
    {
        super.itemClicked(columnId, item);

        this.locationSysId = this.getLocationId(item.value.locationNames, item.value.location?.sysId);
    }

    private getLocationId(locationNames: string[],
                          locationId: string): string
    {
        let options = this.m_host.host.locations;

        if (!locationNames?.length)
        {
            return locationId;
        }

        let option: ControlOption<string> = null;

        for (let name of locationNames)
        {
            option = options.find((opt) => opt.label === name);
            if (option)
            {
                options = option.children || [];
            }
        }

        return option?.id || locationId;
    }

    public async updateLocation()
    {
        let id = this.locationSysId;
        if (!id)
        {
            this.edited.value.locationNames = [];
            this.edited.value.location      = null;
            return;
        }

        let location                    = await this.m_host.host.app.domain.assets.getTypedExtendedById(LocationExtended, id);
        let name                        = await location.getRecursiveName();
        this.edited.value.locationNames = name.split(" - ");
        this.edited.value.location      = LocationExtended.newIdentity(id);
        this.m_host.host.detectChanges();
    }
}

class ProviderForPointClassRule extends ProviderForMappableList<Models.ValidationPointClassRule>
{
    constructor(private m_host: ClassificationValidationRulesEditorComponent,
                subViewPrefix: string,
                displayName: string,
                labelForKey: string,
                labelForValue: string)
    {
        super(m_host.host, subViewPrefix, displayName, labelForKey, labelForValue);
    }

    protected allocate(): Models.ValidationPointClassRule
    {
        return Models.ValidationPointClassRule.newInstance({
                                                               pointClassId: "",
                                                               minValue    : 0,
                                                               maxValue    : 0
                                                           });
    }

    public getKey(item: Models.ValidationPointClassRule): string
    {
        let ec = this.m_host.host.getPointClassOption(item.pointClassId);
        return ec?.label || item.pointClassId;
    }

    public setKey(item: Models.ValidationPointClassRule,
                  key: string): void
    {
        this.edited.key = this.getKey(item);
    }

    protected getText(data: Models.ValidationPointClassRule): string
    {
        if (!data) return "";

        if (data.isBinary) return "Binary point";

        return `Values in range ${data.minValue} - ${data.maxValue}`;
    }

    get valid(): boolean
    {
        let valid = super.valid;
        if (!valid) return false;

        let rule = this.edited.value;
        return rule.pointClassId && !isNaN(rule.minValue) && !isNaN(rule.maxValue);
    }

    public isOrdered(): boolean
    {
        return false;
    }

    protected shouldInclude(data: Models.ValidationPointClassRule,
                            filterLowercase: string): boolean
    {
        if (!data) return false;
        return this.contains(this.getKey(data), filterLowercase);
    }

    public sort(a: Models.ValidationPointClassRule,
                b: Models.ValidationPointClassRule): number
    {
        return UtilsService.compareStrings(this.getKey(a), this.getKey(b), true);
    }
}
