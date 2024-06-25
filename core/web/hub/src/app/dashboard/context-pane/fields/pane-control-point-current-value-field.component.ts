import {ChangeDetectionStrategy, Component, Injector, Input} from "@angular/core";

import {DeviceElementsDetailPageComponent} from "app/customer/device-elements/device-elements-detail-page.component";
import {PaneFieldComponent} from "app/dashboard/context-pane/fields/pane-field.component";
import {AggregationHelper} from "app/services/domain/aggregation.helper";
import {DeviceElementExtended} from "app/services/domain/assets.service";
import * as Models from "app/services/proxy/model/models";

@Component({
               selector       : "o3-pane-control-point-current-value-field",
               templateUrl    : "./pane-control-point-current-value-field.component.html",
               changeDetection: ChangeDetectionStrategy.OnPush
           })
export class PaneControlPointCurrentValueFieldComponent extends PaneFieldComponent
{
    private m_controlPoint: DeviceElementExtended;
    private m_units: Models.EngineeringUnitsFactors;

    private value: string | number = "";
    private unitsDisplay: string   = "";

    public get text(): string
    {
        let text = typeof this.value === "string" ? this.value : AggregationHelper.numberWithUnitDisplay(this.value, this.unitsDisplay);

        if (this.suffix) text = `${text} ${this.suffix}`;

        return text;
    }

    @Input()
    public suffix: string = "";

    @Input()
    public set units(units: Models.EngineeringUnitsFactors)
    {
        this.m_units = units;
        if (this.m_units)
        {
            this.getUnits();
        }
    }

    @Input()
    public set controlPoint(controlPoint: DeviceElementExtended)
    {
        this.m_controlPoint = controlPoint;
        if (this.m_controlPoint)
        {
            if (!this.label)
            {
                this.label = `Current ${this.m_controlPoint.typedModel.name}`;
            }

            this.getValue();
        }
    }

    @Input()
    public set controlPointId(id: Models.RecordIdentity)
    {
        this.initControlPoint(id);
    }

    private async initControlPoint(id: Models.RecordIdentity)
    {
        this.controlPoint = await this.app.domain.assets.getTypedExtendedByIdentity(DeviceElementExtended, id);
    }

    public isClickable(): boolean
    {
        return !!this.m_controlPoint;
    }

    constructor(inj: Injector)
    {
        super(inj);
    }

    onClick()
    {
        if (this.m_controlPoint)
        {
            DeviceElementsDetailPageComponent.navigate(this.app, this.m_controlPoint);
        }
    }

    private async getValue()
    {
        this.isLoading = true;
        this.detectChanges();

        let schema    = await this.m_controlPoint.getSchemaProperty(DeviceElementExtended.PRESENT_VALUE);
        let lastValue = await this.m_controlPoint.getLastValue(DeviceElementExtended.PRESENT_VALUE, this.m_units);
        if (lastValue)
        {
            let value = lastValue.value;

            if (schema?.values)
            {
                let enVal = schema.values.find((val) => val.value === value);
                value     = enVal ? enVal.name : value;
            }

            this.value = value;
        }

        this.isLoading = false;
        this.markForCheck();
    }

    private async getUnits()
    {
        let ext = await this.app.domain.units.resolveDescriptor(this.m_units, false);
        if (ext)
        {
            this.unitsDisplay = ext.model.displayName;
            if (this.unitsDisplay?.charAt(0) == "<") this.unitsDisplay = ext.model.description || this.unitsDisplay;
            this.markForCheck();
        }
    }
}
