import * as Base from "app/customer/engines/shared/base";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "Shared",
              blockName   : "number_constant",
              model       : Models.EngineLiteralNumber,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class LiteralNumberBlock extends Base.ExpressionBlock<Models.EngineLiteralNumber>
{
    protected initFields(): void
    {
        super.initFields();

        let field_val = this.registerFloatField("val",
                                                (model,
                                                 value) => model.value = value,
                                                (model) => model.value);

        this.appendDummyInput()
            .appendField(new Blockly.FieldNumber(0), field_val);
    }

    protected initForToolbox(model: Models.EngineLiteralNumber): void
    {
        model.value = 0;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "number_arithmetic",
              model       : Models.EngineArithmeticOperatorBinary,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class BinaryExpressionArithmeticBlock extends Base.BinaryOperatorBlock<Models.EngineArithmeticOperatorBinary>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveNumber, Models.EngineValuePrimitiveNumber);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineArithmeticOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getOperations()), field_op);

        this.appendConnectedBlock(this.block_b);
    }

    private getOperations()
    {
        return [
            [
                "+",
                Models.CommonEngineArithmeticOperation.Plus
            ],
            [
                "-",
                Models.CommonEngineArithmeticOperation.Minus
            ],
            [
                "*",
                Models.CommonEngineArithmeticOperation.Multiply
            ],
            [
                "/",
                Models.CommonEngineArithmeticOperation.Divide
            ],
            [
                "^",
                Models.CommonEngineArithmeticOperation.Power
            ]
        ];
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "number_arithmetic_percentage",
              model       : Models.EngineArithmeticPercentageOperatorBinary,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class BinaryExpressionArithmeticPercentageBlock extends Base.BinaryOperatorBlock<Models.EngineArithmeticPercentageOperatorBinary>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveNumber, Models.EngineValuePrimitiveNumber);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineArithmeticOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getOperations()), field_op);

        this.appendConnectedBlock(this.block_b);

        this.appendDummyInput()
            .appendField("%");
    }

    private getOperations()
    {
        return [
            [
                "+",
                Models.CommonEngineArithmeticOperation.Plus
            ],
            [
                "-",
                Models.CommonEngineArithmeticOperation.Minus
            ]
        ];
    }
}
