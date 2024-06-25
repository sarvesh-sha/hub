import {Component, Input} from "@angular/core";
import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import {AppDomainContext} from "app/services/domain/domain.module";
import {ControlPointsSelectionExtended} from "app/services/domain/report-definitions.service";
import * as Models from "app/services/proxy/model/models";
import {OverlayConfig} from "framework/ui/overlays/overlay.component";

@Component({
               selector   : "o3-alert-rule-input-field",
               templateUrl: "./alert-rule-input-field.component.html"
           })
export class AlertRuleInputFieldComponent
{
    private m_inputParam: Models.EngineInputParameter;
    private m_pristineState: string;
    private m_literalExtended: InputParameterExtended<any>;
    private m_readonly = false;

    public dialogConfig = OverlayConfig.newInstance({
                                                        showCloseButton : true,
                                                        containerClasses: ["dialog-lg"]
                                                    });

    constructor(private m_domain: AppDomainContext)
    {
    }

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

        if (this.m_inputParam instanceof Models.EngineInputParameterNumber)
        {
            this.m_literalExtended = new NumberInputParameterExtended(this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.EngineInputParameterBoolean)
        {
            this.m_literalExtended = new BooleanInputParameterExtended(this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.EngineInputParameterString)
        {
            this.m_literalExtended = new StringInputParameterExtended(this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.AlertEngineInputParameterControlPoint)
        {
            this.m_literalExtended = new ControlPointInputParameterExtended(this.m_domain, this.m_readonly, this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.AlertEngineInputParameterControlPointsSelection)
        {
            this.m_literalExtended = new ControlPointsInputParameterExtended(this.m_domain, this.m_readonly, this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.EngineInputParameterDateTime)
        {
            this.m_literalExtended = new DateTimeInputParameterExtended(this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.EngineInputParameterDuration)
        {
            this.m_literalExtended = new DurationInputParameterExtended(this.m_inputParam);
        }

        if (this.m_inputParam instanceof Models.AlertEngineInputParameterDeliveryOptions)
        {
            this.m_literalExtended = new DeliveryOptionsInputParameterExtended(this.m_domain, this.m_readonly, this.m_inputParam);
        }
    }

    public isPristine(): boolean
    {
        return JSON.stringify(this.m_inputParam) === this.m_pristineState;
    }

    public markPristine(): void
    {
        this.m_pristineState = JSON.stringify(this.m_inputParam);
    }

    public rebuild(): void
    {
        this.inputParam = this.m_inputParam;
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

    public get controlPointValue()
    {
        if (this.m_inputParam instanceof Models.AlertEngineInputParameterControlPoint)
        {
            return <ControlPointInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }

    public get controlPointsValue()
    {
        if (this.m_inputParam instanceof Models.AlertEngineInputParameterControlPointsSelection)
        {
            return <ControlPointsInputParameterExtended>this.m_literalExtended;
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

    public get deliveryOptionsValue()
    {
        if (this.m_inputParam instanceof Models.AlertEngineInputParameterDeliveryOptions)
        {
            return <DeliveryOptionsInputParameterExtended>this.m_literalExtended;
        }
        return undefined;
    }
}

abstract class InputParameterExtended<T extends Models.EngineInputParameter>
{
    constructor(protected m_literal: T)
    {
    }
}

class ControlPointInputParameterExtended extends InputParameterExtended<Models.AlertEngineInputParameterControlPoint>
{
    private m_selectionExtended: ControlPointsSelectionExtended;

    constructor(private m_domain: AppDomainContext,
                private m_readonly: boolean,
                m_literal: Models.AlertEngineInputParameterControlPoint)
    {
        super(m_literal);
    }

    public get selection(): ControlPointsSelectionExtended
    {
        if (!this.m_selectionExtended)
        {
            let options = new Models.ControlPointsSelection();
            if (this.m_literal.value)
            {
                options.identities = [this.m_literal.value];
            }

            this.m_selectionExtended = new ControlPointsSelectionExtended(this.m_domain, options);
        }

        return this.m_selectionExtended;
    }

    public get display(): string
    {
        return this.selection.getDisplayText(true, this.m_readonly);
    }

    public sync()
    {
        this.m_literal.value = this.selection.identities[0];
    }
}

class ControlPointsInputParameterExtended extends InputParameterExtended<Models.AlertEngineInputParameterControlPointsSelection>
{
    private m_selectionExtended: ControlPointsSelectionExtended;

    constructor(private m_domain: AppDomainContext,
                private m_readonly: boolean,
                m_literal: Models.AlertEngineInputParameterControlPointsSelection)
    {
        super(m_literal);
    }

    public get selection(): ControlPointsSelectionExtended
    {
        if (!this.m_selectionExtended)
        {
            if (!this.m_literal.value)
            {
                this.m_literal.value = new Models.ControlPointsSelection();
            }

            this.m_selectionExtended = new ControlPointsSelectionExtended(this.m_domain, this.m_literal.value);
        }

        return this.m_selectionExtended;
    }

    public get display(): string
    {
        return this.selection.getDisplayText(false, this.m_readonly);
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

class DeliveryOptionsInputParameterExtended extends InputParameterExtended<Models.AlertEngineInputParameterDeliveryOptions>
{
    private m_selectionExtended: DeliveryOptionsExtended;

    constructor(private m_domain: AppDomainContext,
                private m_readonly: boolean,
                m_literal: Models.AlertEngineInputParameterDeliveryOptions)
    {
        super(m_literal);
    }

    public get selection(): DeliveryOptionsExtended
    {
        if (!this.m_selectionExtended)
        {
            if (!this.m_literal.value)
            {
                this.m_literal.value = new Models.DeliveryOptions();
            }

            this.m_selectionExtended = new DeliveryOptionsExtended(this.m_literal.value, this.m_domain, false);
        }

        return this.m_selectionExtended;
    }

    public get display(): string
    {
        return this.selection.getDisplayText(this.m_readonly);
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
