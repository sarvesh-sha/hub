import {ChangeDetectionStrategy, Component, EventEmitter, Injector, Input, Output} from "@angular/core";
import {ClassificationDetailPageComponent, formatEquipment} from "app/customer/configuration/classification/classification-detail-page.component";
import * as Models from "app/services/proxy/model/models";
import {BaseComponent} from "framework/ui/components";

@Component({
               selector       : "o3-classification-test",
               templateUrl    : "./classification-test.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class ClassificationTestComponent extends BaseComponent
{
    @Input() sample: Models.DeviceElementNormalizationSample;

    @Output() sampleChange = new EventEmitter<Models.DeviceElementNormalizationSample>();

    conversionOutput: string[] = [];

    mappingOutput: string[] = [];

    constructor(private m_host: ClassificationDetailPageComponent,
                inj: Injector)
    {
        super(inj);
    }

    async normalizationTest()
    {
        this.m_host.updateRules();

        let res = await this.m_host.app.domain.apis.normalization.testNormalization(this.sample);

        let output = [];

        output.push(`Original: "${this.sample.details.objectName}"`);

        for (let item of res.normalizationHistory)
        {
            let prefix: string = `Step ${output.length}: `;
            if (item.match)
            {
                output.push(`${prefix}"${item.after}" | ${item.match.reason} - ${item.match.input} - ${item.match.output}`);
            }
            else
            {
                output.push(`${prefix}"${item.after}"`);
            }
        }

        output.push(`Output: "${res.normalizedName}"`);

        if (res.locations?.length > 0)
        {
            const locName = res.locations.map((l) => l.name)
                               .join("/");
            output.push(`Location: ${locName}`);
        }

        const formattedEquip = formatEquipment(res.equipments, res.equipmentRelationships);
        for (let equip of formattedEquip)
        {
            output.push(`Equipment: ${equip}`);

        }

        this.conversionOutput = output;
        this.markForCheck();
    }

    async classificationTest()
    {
        this.m_host.updateRules();

        let res = await this.m_host.app.domain.apis.normalization.testClassification(this.sample);

        let output = [];

        let result = "";
        if (res.currentResult.id)
        {
            let pc = this.m_host.normalization.model.rules.pointClasses.find((pc) => pc.id === +res.currentResult.id);
            if (pc)
            {
                result = `${pc.pointClassName} - ${pc.pointClassDescription}`;
            }
            else
            {
                result = `Unknown point class - ${res.currentResult.id}`;
            }
        }
        else
        {
            result = `Unclassified`;
        }

        result += ` | Positive Score: ${res.currentResult.positiveScore} Negative Score: ${res.currentResult.negativeScore} - Reason: ${res.currentResult.reason}`;
        if (res.matchingDimensions?.length)
        {
            result += ` ${res.matchingDimensions.join(", ")}`;
        }
        output.push(result);

        if (res.normalizationTags)
        {
            output.push(`Tags: ${res.normalizationTags.join(", ")}`);
        }

        this.mappingOutput = output;
        this.markForCheck();
    }
}
