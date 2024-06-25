import {ChangeDetectionStrategy, Component, Injector, Input} from "@angular/core";
import {ClassificationDetailPageComponent} from "app/customer/configuration/classification/classification-detail-page.component";

import {ApiService} from "app/services/domain/api.service";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";
import {IProviderForMapHost} from "app/shared/tables/provider-for-map";
import {Lookup, UtilsService} from "framework/services/utils.service";
import {BaseComponent} from "framework/ui/components";
import {mapInParallel} from "framework/utils/concurrency";

@Component({
               selector       : "o3-validation-run",
               templateUrl    : "./validation-run.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ValidationRunComponent extends BaseComponent implements IProviderForMapHost
{
    private m_data: ValidationConfig;

    constructor(public host: ClassificationDetailPageComponent,
                public apis: ApiService,
                inj: Injector)
    {
        super(inj);
    }

    @Input()
    public set data(data: ValidationConfig)
    {
        this.m_data = data;
        if (this.m_data)
        {
            this.start();
        }
    }

    public get data(): ValidationConfig
    {
        return this.m_data;
    }

    async start()
    {
        await this.validate();
        await this.refreshVisibility();
    }

    async validate()
    {
        const errors: NormalizationValidationError[]                   = [];
        const equipmentLookup                                          = new Map<string, Models.NormalizationEquipment>();
        const equipmentClassLookup                                     = new Map<string, Set<string>>();
        const equipmentMap                                             = new Map<string, Map<Models.PointClass, Set<string>>>();
        const danglingPoints                                           = new Set<string>();
        const pointRules                                               = new Map<string, Models.ValidationPointClassRule>();
        const equipRules                                               = new Map<string, Models.ValidationEquipmentRule>();
        const pointChecks: [string, Models.ValidationPointClassRule][] = [];
        this.data.elementsToProcess                                    = this.host.normalizationState.rows.length;
        this.data.elementsProcessed                                    = 0;
        this.data.equipmentToProcess                                   = 0;
        this.data.equipmentProcessed                                   = 0;

        for (let pointRule of this.host.rules.validation.pointClassRules)
        {
            pointRules.set(pointRule.pointClassId, pointRule);
        }

        for (let equipRule of this.host.rules.validation.equipmentRules)
        {
            equipRules.set(equipRule.equipmentClassId, equipRule);
        }

        for (let point of this.host.normalizationState.rows)
        {
            const pointClass = point.rawPointClass;
            const nonLeaves  = new Set<string>(Object.keys(point.details.equipmentRelationships));
            const leaves     = Object.keys(point.details.equipments)
                                     .filter((e) => !nonLeaves.has(e));

            if (!leaves.length && !!pointClass)
            {
                danglingPoints.add(point.details.sysId);
            }

            for (let leaf of leaves)
            {
                const equip = point.details.equipments[leaf];
                if (!equipmentLookup.has(leaf))
                {
                    equipmentLookup.set(leaf, equip);
                }

                if (!equipmentClassLookup.has(equip.equipmentClassId))
                {
                    equipmentClassLookup.set(equip.equipmentClassId, new Set<string>());
                }

                equipmentClassLookup.get(equip.equipmentClassId)
                                    .add(leaf);

                if (!equipmentMap.has(leaf))
                {
                    equipmentMap.set(leaf, new Map<Models.PointClass, Set<string>>());
                }

                const pointClasses = equipmentMap.get(leaf);
                if (!pointClasses.has(pointClass))
                {
                    pointClasses.set(pointClass, new Set<string>());
                }

                const points = pointClasses.get(pointClass);
                points.add(point.details.sysId);
            }

            let pointProcessed = true;

            if (pointClass && pointRules.has(pointClass.id + ""))
            {
                const pointRule  = pointRules.get(pointClass.id + "");
                const objectType = point.details.details.objectType || "";
                const isBinary   = objectType.startsWith("binary");
                if (pointRule.allowableObjectTypes?.length && !pointRule.allowableObjectTypes.indexOf(objectType))
                {
                    errors.push(new NormalizationValidationErrorPointObjectType(point.details.sysId, pointRule.pointClassId, objectType));
                }

                if (pointRule.isBinary !== isBinary)
                {
                    errors.push(NormalizationValidationErrorPointRange.createBinary(pointRule, point.details.sysId, isBinary));
                }
                else if (pointRule.minValue || pointRule.maxValue)
                {
                    pointProcessed = false;
                    pointChecks.push([
                                         point.details.sysId,
                                         pointRule
                                     ]);
                }
            }

            if (pointProcessed)
            {
                this.data.elementsProcessed++;
            }
        }

        this.data.equipmentToProcess = equipmentMap.size;

        for (let [equipmentKey, pointClasses] of equipmentMap.entries())
        {
            const equipment = equipmentLookup.get(equipmentKey);
            const equipRule = equipRules.get(equipment.equipmentClassId);

            for (let [pointClass, points] of pointClasses.entries())
            {
                if (equipRule?.points)
                {
                    let rule = equipRule.points.find((p) => p.pointClassId === pointClass?.id + "");
                    if (!pointClass || !rule)
                    {
                        errors.push(new NormalizationValidationErrorUnknownPoints(equipment, [...points]));
                    }
                }
                if (!pointClass || points.size <= 1) continue;
                errors.push(new NormalizationValidationErrorDuplicatePointClass(pointClass, equipment, [...points]));
            }

            for (let pointRule of equipRule?.points || [])
            {
                const pointClass     = this.host.getPointClass(pointRule.pointClassId);
                const pointsForClass = pointClasses.get(pointClass) || new Set<string>();
                const numPoints      = pointsForClass.size;
                if (numPoints < pointRule.minNumber || numPoints > pointRule.maxNumber)
                {
                    errors.push(new NormalizationValidationErrorPointCardinality(equipment, [...pointsForClass], pointRule.pointClassId, pointRule.minNumber, pointRule.maxNumber));
                }
            }

            this.data.equipmentProcessed++;
        }

        for (let equipRule of equipRules.values())
        {
            const equipments = equipmentClassLookup.get(equipRule.equipmentClassId) || new Set<string>();
            if (equipRule.minNumber || equipRule.maxNumber)
            {
                if (equipments.size < equipRule.minNumber || equipments.size > equipRule.maxNumber)
                {
                    errors.push(new NormalizationValidationErrorEquipmentCardinality(UtilsService.mapIterable(equipments, (e) => equipmentLookup.get(e)),
                                                                                     equipRule.equipmentClassId,
                                                                                     equipRule.location?.sysId,
                                                                                     equipRule.locationNames,
                                                                                     equipRule.minNumber,
                                                                                     equipRule.maxNumber));
                }
            }
        }

        while (pointChecks.length)
        {
            let batch  = pointChecks.splice(0, 500);
            let assets = await this.host.app.domain.assets.getExtendedBatch(batch.map(([sysId]) => DeviceElementExtended.newIdentity(sysId)));

            for (let i = 0; i < batch.length; i++)
            {
                let asset = assets[i];
                let rule  = batch[i][1];

                if (asset instanceof DeviceElementExtended)
                {
                    if (asset.typedModel.contents?.hasOwnProperty("present_value"))
                    {
                        let value = asset.typedModel.contents["present_value"];
                        if (value < rule.minValue || value > rule.maxValue)
                        {
                            errors.push(NormalizationValidationErrorPointRange.create(rule, asset.model.sysId, value));
                        }
                    }
                }
            }

            this.data.elementsProcessed += batch.length;
            this.detectChanges();
        }

        errors.push(new NormalizationValidationErrorDanglingPoints([...danglingPoints]));

        this.data.errors = errors;
    }

    async refreshVisibility()
    {
        if (!this.data.errors)
        {
            return;
        }

        let equipmentCardinalityErrors: NormalizationValidationErrorEquipmentCardinality[] = [];
        let pointCardinalityErrors: NormalizationValidationErrorPointCardinality[]         = [];
        let unknownPointErrors: NormalizationValidationErrorUnknownPoints[]                = [];
        let duplicatePointErrors: NormalizationValidationErrorDuplicatePointClass[]        = [];
        let danglingPoints: string[]                                                       = [];
        this.data.pointRangeErrors                                                         = [];
        this.data.pointRangeMessages                                                       = {};
        this.data.pointObjectErrors                                                        = [];
        this.data.pointObjectMessages                                                      = {};

        for (let error of this.data.errors)
        {
            switch (error.type)
            {
                case NormalizationValidationErrorType.EquipmentCardinality:
                    equipmentCardinalityErrors.push(<any>error);
                    break;

                case NormalizationValidationErrorType.DuplicatePointClass:
                    duplicatePointErrors.push(<any>error);
                    break;

                case NormalizationValidationErrorType.PointCardinality:
                    let errCard = <NormalizationValidationErrorPointCardinality>error;
                    let length  = errCard.pointIds?.length || 0;
                    if (!this.data.showMissingPoints && errCard.expectedMin > length) continue;
                    if (!this.data.showExtraPoints && errCard.expectedMax < length) continue;

                    pointCardinalityErrors.push(errCard);
                    break;

                case NormalizationValidationErrorType.UnknownPoints:
                    unknownPointErrors.push(<any>error);
                    break;

                case NormalizationValidationErrorType.PointRange:
                    let errRange = <NormalizationValidationErrorPointRange>error;
                    if (this.data.hideZeroRange && errRange.lastValue === 0) continue;
                    this.data.pointRangeErrors.push(errRange.sysId);
                    this.data.pointRangeMessages[errRange.sysId] = error.message;
                    break;

                case NormalizationValidationErrorType.DanglingPoint:
                    let errDangling = <NormalizationValidationErrorDanglingPoints>error;
                    danglingPoints.push(...errDangling.sysIds);
                    break;

                case NormalizationValidationErrorType.ObjectType:
                    let errObj = <NormalizationValidationErrorPointRange>error;
                    if (this.data.hideZeroRange && errObj.lastValue === 0) continue;
                    this.data.pointObjectErrors.push(errObj.sysId);
                    this.data.pointObjectMessages[errObj.sysId] = error.message;
                    break;
            }
        }

        await Promise.all([
                              this.initializeEquipmentCardinality(equipmentCardinalityErrors),
                              this.initializePointCardinality(pointCardinalityErrors),
                              this.initializeUnknownPoint(unknownPointErrors),
                              this.initializeDuplicatePoints(duplicatePointErrors)
                          ]);

        this.data.danglingPointErrors = danglingPoints;

        this.markForCheck();
    }

    private async initializeEquipmentCardinality(errors: NormalizationValidationErrorEquipmentCardinality[])
    {
        this.data.equipmentCardinalityErrors = errors.map((err) =>
                                                          {
                                                              let ec       = this.host.getEquipmentClassOption(err.equipmentClassId);
                                                              let location = err.locationNames?.join(" - ") || "Any Location";
                                                              return new EquipmentCardinalityErrorExtended(err, ec.label, location);
                                                          });
    }

    private async initializePointCardinality(errors: NormalizationValidationErrorPointCardinality[])
    {
        this.data.pointCardinalityErrors = await mapInParallel(errors, async (err) =>
        {
            let pc = this.host.getPointClassOption(err.pointClassId);
            return new PointCardinalityErrorExtended(err, err.equipment, pc.label);
        });
    }

    private async initializeUnknownPoint(errors: NormalizationValidationErrorUnknownPoints[])
    {
        this.data.unknownPointErrors = await mapInParallel(errors, async (err) =>
        {
            return new UnknownPointErrorExtended(err, err.equipment);
        });
    }

    private async initializeDuplicatePoints(errors: NormalizationValidationErrorDuplicatePointClass[])
    {
        this.data.duplicatePointErrors = await mapInParallel(errors, async (err) =>
        {
            let pc = this.host.getPointClassOption(err.pointClass.id + "");
            return new DuplicatePointErrorExtended(err, err.equipment, pc.label);
        });
    }

    //--//

    public setDirty()
    {
    }

    //--//

    downloadChanges()
    {
        let data: string[] = [];
    }
}

export class ValidationConfig
{
    public hideZeroRange: boolean;

    public showMissingPoints: boolean = true;

    public showExtraPoints: boolean = true;

    public processing: boolean;

    public refreshDelay: number;

    //--//

    public handle: string;

    public equipmentToProcess: number;

    public equipmentProcessed: number;

    public elementsToProcess: number;

    public elementsProcessed: number;

    public errors: NormalizationValidationError[];

    public equipmentCardinalityErrors: EquipmentCardinalityErrorExtended[];

    public duplicatePointErrors: DuplicatePointErrorExtended[];

    public pointCardinalityErrors: PointCardinalityErrorExtended[];

    public unknownPointErrors: UnknownPointErrorExtended[];

    public danglingPointErrors: string[];

    public pointRangeErrors: string[];

    public pointRangeMessages: Lookup<string>;

    public pointObjectErrors: string[];

    public pointObjectMessages: Lookup<string>;
}

class EquipmentCardinalityErrorExtended
{
    constructor(public model: NormalizationValidationErrorEquipmentCardinality,
                public equipmentClass: string,
                public location: string)
    {}
}

class PointCardinalityErrorExtended
{
    constructor(public model: NormalizationValidationErrorPointCardinality,
                public equipment: Models.NormalizationEquipment,
                public pointClass: string)
    {}
}

class UnknownPointErrorExtended
{
    constructor(public model: NormalizationValidationErrorUnknownPoints,
                public equipment: Models.NormalizationEquipment)
    {}
}

class DuplicatePointErrorExtended
{
    constructor(public model: NormalizationValidationErrorDuplicatePointClass,
                public equipment: Models.NormalizationEquipment,
                public pointClass: string)
    {}
}

export enum NormalizationValidationErrorType
{
    EquipmentCardinality = "EquipmentCardinality",
    PointCardinality     = "PointCardinality",
    UnknownPoints        = "UnknownPoints",
    PointRange           = "PointRange",
    DanglingPoint        = "DanglingPoint",
    ObjectType           = "ObjectType",
    DuplicatePointClass  = "DuplicatePointClass"
}

export abstract class NormalizationValidationError
{
    protected constructor(public message: string,
                          public type: NormalizationValidationErrorType)
    {}
}

export class NormalizationValidationErrorDuplicatePointClass extends NormalizationValidationError
{
    constructor(public pointClass: Models.PointClass,
                public equipment: Models.NormalizationEquipment,
                public points: string[])
    {
        super(`Equipment has more than one point for class "${pointClass.pointClassName}"`, NormalizationValidationErrorType.DuplicatePointClass);
    }
}

export class NormalizationValidationErrorDanglingPoints extends NormalizationValidationError
{
    constructor(public sysIds: string[])
    {
        super("Points classified and not assigned to any equipment", NormalizationValidationErrorType.DanglingPoint);
    }
}

export class NormalizationValidationErrorPointRange extends NormalizationValidationError
{
    constructor(message: string,
                public sysId: string,
                public pointClassId: string,
                public isBinary: boolean,
                public lastValue: number,
                public minValue: number,
                public maxValue: number)
    {
        super(message, NormalizationValidationErrorType.PointRange);
    }

    static create(rule: Models.ValidationPointClassRule,
                  sysId: string,
                  lastValue: number)
    {
        return new NormalizationValidationErrorPointRange(`${lastValue} outside range ${rule.minValue} - ${rule.maxValue}`, sysId, rule.pointClassId, false, lastValue, rule.minValue, rule.maxValue);
    }

    static createBinary(rule: Models.ValidationPointClassRule,
                        sysId: string,
                        isBinary: boolean)
    {
        return new NormalizationValidationErrorPointRange(isBinary ? "Expected analog point" : "Expected binary point", sysId, rule.pointClassId, isBinary, 0, 0, 0);
    }
}


export class NormalizationValidationErrorPointObjectType extends NormalizationValidationError
{
    constructor(public sysId: string,
                public pointClassId: string,
                public objectType: string)
    {
        super(`Object type ${objectType} not allowed`, NormalizationValidationErrorType.ObjectType);
    }
}

export class NormalizationValidationErrorPointCardinality extends NormalizationValidationError
{
    constructor(public equipment: Models.NormalizationEquipment,
                public pointIds: string[],
                public pointClassId: string,
                public expectedMin: number,
                public expectedMax: number)
    {
        super(`Expected ${getRangeString(expectedMin, expectedMax)} point(s), but found ${pointIds.length} point(s)`, NormalizationValidationErrorType.PointCardinality);
    }
}


export class NormalizationValidationErrorEquipmentCardinality extends NormalizationValidationError
{
    constructor(public equipments: Models.NormalizationEquipment[],
                public equipmentClassId: string,
                public locationSysId: string,
                public locationNames: string[],
                minNumber: number,
                maxNumber: number)
    {
        super(`Expected ${getRangeString(minNumber, maxNumber)} equipment, but found ${equipments.length} equipment`, NormalizationValidationErrorType.EquipmentCardinality);
    }
}

export class NormalizationValidationErrorUnknownPoints extends NormalizationValidationError
{
    constructor(public equipment: Models.NormalizationEquipment,
                public pointIds: string[])
    {
        super(`Found ${pointIds.length} unmatched points`, NormalizationValidationErrorType.UnknownPoints);
    }
}

function getRangeString(min: number,
                        max: number)
{
    if (min != max)
    {
        return `${min} - ${max}`;
    }

    return `${min}`;
}
