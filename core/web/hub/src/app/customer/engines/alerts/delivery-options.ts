import {DeliveryOptionsExtended} from "app/customer/configuration/common/delivery-options";
import {DeliveryOptionsComponent} from "app/customer/configuration/common/delivery-options.component";

import * as Base from "app/customer/engines/shared/base";
import * as Input from "app/customer/engines/shared/input";
import {AppDomainContext} from "app/services/domain/domain.module";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";

import {UtilsService} from "framework/services/utils.service";
import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

export class DeliveryOptionsField extends ModalDialogField<DeliveryOptionsComponent, typeof DeliveryOptionsComponent, Models.DeliveryOptions>
{
    constructor(block: Block<any>)
    {
        super(block, DeliveryOptionsComponent, "Select users or roles");
    }

    private m_deliveryOptions: DeliveryOptionsExtended;
    private m_lastTooltip: string;

    protected initData(data: Models.DeliveryOptions): void
    {
        this.m_deliveryOptions = new DeliveryOptionsExtended(data, this.m_block.injectContext(AppDomainContext), false);
    }

    protected initComponent(component: DeliveryOptionsComponent): void
    {
        component.deliveryOptions = this.m_deliveryOptions;
    }

    public getModel(): Models.DeliveryOptions
    {
        return this.m_deliveryOptions.model;
    }

    protected getText(): string
    {
        this.updateTooltip();

        return this.m_deliveryOptions.getDisplayText();
    }

    private async updateTooltip()
    {
        let tooltip = await this.m_deliveryOptions.getResolvedText();
        if (tooltip != this.m_lastTooltip)
        {
            this.m_block.setTooltip(tooltip);
            this.m_lastTooltip = tooltip;
        }
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "delivery_options",
              model       : Models.AlertEngineLiteralDeliveryOptions,
              outputType  : Models.AlertEngineValueDeliveryOptions
          })
export class LiteralDeliveryOptionsBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralDeliveryOptions>
{
    private deliverOptionsField: DeliveryOptionsField;

    protected initFields(): void
    {
        super.initFields();

        this.deliverOptionsField = new DeliveryOptionsField(this);
        this.registerModalField(this.deliverOptionsField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        this.appendDummyInput()
            .appendField(this.deliverOptionsField.field);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralDeliveryOptions): void
    {
        model.value = new Models.DeliveryOptions();
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "delivery_options_input",
              model       : Models.AlertEngineInputParameterDeliveryOptions,
              outputType  : Models.AlertEngineValueDeliveryOptions
          })
export class InputParameterDeliveryOptionsBlock extends Input.InputParameterBlock<Models.AlertEngineInputParameterDeliveryOptions>
{
    private deliverOptionsField: DeliveryOptionsField;

    protected appendValueField(input: Blockly.Input)
    {
        this.deliverOptionsField = new DeliveryOptionsField(this);
        this.registerModalField(this.deliverOptionsField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        input.appendField(this.deliverOptionsField.field);
    }

    protected initForToolbox(model: Models.AlertEngineInputParameterDeliveryOptions): void
    {
        model.value = new Models.DeliveryOptions();
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "delivery_options_ops",
              model       : Models.AlertEngineOperatorBinaryForDeliveryOptions,
              outputType  : Models.AlertEngineValueDeliveryOptions
          })
export class BinaryExpressionDeliveryOptionsBlock extends Base.BinaryOperatorBlock<Models.AlertEngineOperatorBinaryForDeliveryOptions>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueDeliveryOptions, Models.AlertEngineValueDeliveryOptions);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineSetOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("combine delivery options");

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getOperations()), field_op);

        this.appendConnectedBlock(this.block_b);
    }

    private getOperations()
    {
        return [
            [
                "Join",
                Models.CommonEngineSetOperation.Add
            ],
            [
                "Subtract",
                Models.CommonEngineSetOperation.Subtract
            ],
            [
                "Intersect",
                Models.CommonEngineSetOperation.Intersect
            ]
        ];
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_email_delivery_options_from_location",
              model       : Models.AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation,
              outputType  : Models.AlertEngineValueDeliveryOptions
          })
export class GetEmailDeliveryOptionsFromLocationBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetEmailDeliveryOptionsFromLocation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("get email delivery options from location of");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_sms_delivery_options_from_location",
              model       : Models.AlertEngineOperatorUnaryGetSmsDeliveryOptionsFromLocation,
              outputType  : Models.AlertEngineValueDeliveryOptions
          })
export class GetSmsDeliveryOptionsFromLocationBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetSmsDeliveryOptionsFromLocation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("get SMS delivery options from location of");
    }
}
