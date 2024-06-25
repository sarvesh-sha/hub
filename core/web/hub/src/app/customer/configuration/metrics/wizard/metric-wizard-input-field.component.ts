import {Component, Input} from "@angular/core";
import * as Models from "app/services/proxy/model/models";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-metric-input-field",
               templateUrl: "./metric-wizard-input-field.component.html"
           })
export class MetricWizardInputFieldComponent
{
    private m_inputParam: Models.EngineInputParameter;
    private m_pristineState: string;
    private m_literalExtended: InputParameterExtended<any>;
    private m_readonly = false;

    public dialogConfig = OverlayConfig.newInstance({showCloseButton: true});

    @Input()
    public set readonly(readonly: boolean)
    {
        this.m_readonly = readonly;
    }

    public get readonly(): boolean
    {
        return this.m_readonly;
    }

    @Input()
    public set inputParam(inputParam: Models.EngineInputParameter)
    {
        this.m_inputParam = inputParam;
        this.markPristine();

        this.m_literalExtended = InputParameterExtended.extract(this.m_inputParam);
    }

    public isPristine(): boolean
    {
        return JSON.stringify(this.m_inputParam) === this.m_pristineState;
    }

    public markPristine(): void
    {
        this.m_pristineState = JSON.stringify(this.m_inputParam);
    }

    public get title()
    {
        return this.m_inputParam.title;
    }

    public get description()
    {
        return this.m_inputParam.description;
    }

    public get numberValue()
    {
        if (this.m_inputParam instanceof Models.EngineInputParameterNumber)
        {
            return <NumberInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get booleanValue()
    {
        if (this.m_inputParam instanceof Models.EngineInputParameterBoolean)
        {
            return <BooleanInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get stringValue()
    {
        if (this.m_inputParam instanceof Models.EngineInputParameterString)
        {
            return <StringInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get dateTimeValue()
    {
        if (this.m_inputParam instanceof Models.EngineInputParameterDateTime)
        {
            return <DateTimeInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get durationValue()
    {
        if (this.m_inputParam instanceof Models.EngineInputParameterDuration)
        {
            return <DurationInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get scalarValue()
    {
        if (this.m_inputParam instanceof Models.MetricsEngineInputParameterScalar)
        {
            return <ScalarInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }
}

export abstract class InputParameterExtended<T extends Models.EngineInputParameter>
{
    constructor(protected m_literal: T)
    {
    }

    static extract(input: Models.EngineInputParameter): InputParameterExtended<any>
    {
        if (input instanceof Models.EngineInputParameterNumber)
        {
            return new NumberInputParameterExtended(input);
        }

        if (input instanceof Models.EngineInputParameterBoolean)
        {
            return new BooleanInputParameterExtended(input);
        }

        if (input instanceof Models.EngineInputParameterString)
        {
            return new StringInputParameterExtended(input);
        }

        if (input instanceof Models.EngineInputParameterDateTime)
        {
            return new DateTimeInputParameterExtended(input);
        }

        if (input instanceof Models.EngineInputParameterDuration)
        {
            return new DurationInputParameterExtended(input);
        }

        if (input instanceof Models.MetricsEngineInputParameterScalar)
        {
            return new ScalarInputParameterExtended(input);
        }

        return null;
    }
}

class StringInputParameterExtended extends InputParameterExtended<Models.EngineInputParameterString>
{
    public get value(): string
    {
        return this.m_literal.value;
    }

    public set value(val: string)
    {
        this.m_literal.value = val;
    }
}

class DurationInputParameterExtended extends InputParameterExtended<Models.EngineInputParameterDuration>
{
    constructor(m_literal: Models.EngineInputParameterDuration)
    {
        super(m_literal);
    }

    public get amountValue(): number
    {
        return this.m_literal.amount;
    }

    public set amountValue(val: number)
    {
        this.m_literal.amount = val;
    }

    public get unitsValue(): Models.ChronoUnit
    {
        return this.m_literal.unit;
    }

    public set unitsValue(val: Models.ChronoUnit)
    {
        this.m_literal.unit = val;
    }
}

class DateTimeInputParameterExtended extends InputParameterExtended<Models.EngineInputParameterDateTime>
{
    public get value(): Date
    {
        if (typeof this.m_literal.value === "string")
        {
            this.m_literal.value = new Date(this.m_literal.value);
        }

        return this.m_literal.value;
    }

    public set value(val: Date)
    {
        this.m_literal.value = val;
    }
}


class NumberInputParameterExtended extends InputParameterExtended<Models.EngineInputParameterNumber>
{
    public get value(): number
    {
        return this.m_literal.value;
    }

    public set value(val: number)
    {
        this.m_literal.value = val;
    }
}

class BooleanInputParameterExtended extends InputParameterExtended<Models.EngineInputParameterBoolean>
{
    public get value(): boolean
    {
        return this.m_literal.value;
    }

    public set value(val: boolean)
    {
        this.m_literal.value = val;
    }
}

class ScalarInputParameterExtended extends InputParameterExtended<Models.MetricsEngineInputParameterScalar>
{
    public get value(): number
    {
        return this.m_literal.value;
    }

    public set value(value: number)
    {
        this.m_literal.value = value;
    }

    public get units(): Models.EngineeringUnitsFactors
    {
        return this.m_literal.units;
    }

    public set units(units: Models.EngineeringUnitsFactors)
    {
        this.m_literal.units = units;
    }
}
