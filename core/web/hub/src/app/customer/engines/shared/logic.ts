import * as Base from "app/customer/engines/shared/base";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {DynamicToolboxCategory} from "framework/ui/blockly/toolbox-category";

@BlockDef({
              blockContext: "Shared",
              blockName   : "logic_compare",
              model       : Models.EngineExpressionBinaryLogicCompare,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicCompareBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryLogicCompare>
{
    private static readonly c_field_OP = "OP";

    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue, Models.EngineValue);
    }

    protected initFromModel(model: Models.EngineExpressionBinaryLogicCompare): void
    {
        this.setField(LogicCompareBlock.c_field_OP, this.getOperationString(model.operation));

        super.initFromModel(model);
    }

    public toModel(): Models.EngineExpressionBinaryLogicCompare
    {
        let logicCompare       = super.toModel();
        logicCompare.operation = this.getOperation(this.getFieldString(LogicCompareBlock.c_field_OP));
        return logicCompare;
    }

    private getOperationString(op: Models.CommonEngineCompareOperation)
    {
        switch (op)
        {
            case Models.CommonEngineCompareOperation.Equal:
                return "EQ";

            case Models.CommonEngineCompareOperation.NotEqual:
                return "NEQ";

            case Models.CommonEngineCompareOperation.GreaterThan:
                return "GT";

            case Models.CommonEngineCompareOperation.GreaterThanOrEqual:
                return "GTE";

            case Models.CommonEngineCompareOperation.LessThan:
                return "LT";

            case Models.CommonEngineCompareOperation.LessThanOrEqual:
                return "LTE";

            default:
                return "";
        }
    }

    private getOperation(op: string): Models.CommonEngineCompareOperation
    {
        switch (op)
        {
            case "NEQ":
                return Models.CommonEngineCompareOperation.NotEqual;

            case "GT":
                return Models.CommonEngineCompareOperation.GreaterThan;

            case "GTE":
                return Models.CommonEngineCompareOperation.GreaterThanOrEqual;

            case "LT":
                return Models.CommonEngineCompareOperation.LessThan;

            case "LTE":
                return Models.CommonEngineCompareOperation.LessThanOrEqual;

            case "EQ":
            default:
                return Models.CommonEngineCompareOperation.Equal;
        }
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_approx_equals",
              model       : Models.EngineExpressionBinaryApproximateEquality,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class ApproximateEqualityBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryApproximateEquality>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue, Models.EngineValue);

        let field_val = this.registerFloatField("epsilon",
                                                (model,
                                                 value) => model.epsilon = value,
                                                (model) => model.epsilon);


        this.appendConnectedBlock(this.block_a);

        this.appendConnectedBlock(this.block_b)
            .appendField("==");

        this.appendDummyInput()
            .appendField("within")
            .appendField(new Blockly.FieldNumber(0), field_val);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_range_check",
              model       : Models.EngineExpressionRangeCheck,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class RangeCheckBlock extends Base.ExpressionBlock<Models.EngineExpressionRangeCheck>
{
    protected initFields(): void
    {
        super.initFields();

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitive);

        let block_min = this.registerConnectedBlock<Models.EngineExpression>("minRange",
                                                                             false,
                                                                             (model,
                                                                              value) => model.minRange = value,
                                                                             (model) => model.minRange,
                                                                             Models.EngineValuePrimitive);

        let block_max = this.registerConnectedBlock<Models.EngineExpression>("maxRange",
                                                                             false,
                                                                             (model,
                                                                              value) => model.maxRange = value,
                                                                             (model) => model.maxRange,
                                                                             Models.EngineValuePrimitive);

        this.appendConnectedBlock(block_value)
            .appendField("value");

        this.appendConnectedBlock(block_min)
            .appendField("is between");

        this.appendConnectedBlock(block_max)
            .appendField("and");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_logic_operation",
              model       : Models.EngineExpressionBinaryLogicOperation,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicOperationBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryLogicOperation>
{
    private static readonly c_field_OP = "op";

    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveBoolean, Models.EngineValuePrimitiveBoolean);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([
                                                       [
                                                           "or",
                                                           "or"
                                                       ],
                                                       [
                                                           "and",
                                                           "and"
                                                       ]
                                                   ]), LogicOperationBlock.c_field_OP);

        this.appendConnectedBlock(this.block_b);
    }

    protected initFromModel(model: Models.EngineExpressionBinaryLogicOperation): void
    {
        this.setField(LogicOperationBlock.c_field_OP, model.and ? "and" : "or");

        super.initFromModel(model);
    }

    public toModel(): Models.EngineExpressionBinaryLogicOperation
    {
        let logicalOperation = super.toModel();
        logicalOperation.and = this.getFieldString(LogicOperationBlock.c_field_OP) === "and";
        return logicalOperation;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_logic_not",
              model       : Models.EngineOperatorUnaryLogicNot,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicNotBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryLogicNot>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveBoolean);

        this.appendConnectedBlock(this.block_a)
            .appendField("not");
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_is_not_null",
              model       : Models.EngineOperatorUnaryIsNotNull,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicIsNotNullBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsNotNull>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is not null");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_is_null",
              model       : Models.EngineOperatorUnaryIsNull,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicIsNullBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsNull>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is null");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_is_valid_number",
              model       : Models.EngineOperatorUnaryIsValidNumber,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicIsValidNumberBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsValidNumber>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveNumber);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is a valid number");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_is_not_valid_number",
              model       : Models.EngineOperatorUnaryIsNotValidNumber,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LogicIsNotValidNumberBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsNotValidNumber>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveNumber);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is not a valid number");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "list_empty",
              model       : Models.EngineOperatorUnaryIsEmpty,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class IsEmptyBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsEmpty>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is empty");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_list_not_empty",
              model       : Models.EngineOperatorUnaryIsNotEmpty,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class IsNotEmptyBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryIsNotEmpty>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("is not empty");

        this.setInputsInline(true);
    }
}

//--//

// Override default message
Blockly.Msg.CONTROLS_IF_MSG_THEN = "then";

@BlockDef({
              blockContext: "Shared",
              blockName   : "controls_if",
              model       : Models.EngineStatementLogicIf
          })
export class StatementLogicIfBlock extends Base.StatementBlock<Models.EngineStatementLogicIf>
{
    protected initFields()
    {
        // System Block
        // Inputs: IF0, DO0, IF1, DO1, ..., ELSE

        // Add system mutator (necessary for scratch pad compatibility)
        this.addMutator((container) =>
                        {
                            if (this.elseifCount_)
                            {
                                container.setAttribute("elseif", this.elseifCount_ + "");
                            }
                            if (this.elseCount_)
                            {
                                container.setAttribute("else", "1");
                            }
                        }, (container) =>
                        {
                            this.elseifCount_ = parseInt(container.getAttribute("elseif"), 10) || 0;
                            this.elseCount_   = parseInt(container.getAttribute("else"), 10) || 0;
                            (<any>this).rebuildShape_();
                        });
    }

    // Counts are from mutator, defining just for type checking;
    public elseifCount_: number;
    public elseCount_: number;

    protected initFromModel(model: Models.EngineStatementLogicIf): void
    {
        let ifElseBlocks  = model.ifElseBlocks || [];
        let hasElseBlock  = model.elseStatements;
        this.elseifCount_ = Math.max(0, ifElseBlocks.length - 1);
        this.elseCount_   = hasElseBlock ? 1 : 0;
        this.updateShape();

        for (let i = 0; i < ifElseBlocks.length; i++)
        {
            let conditionBlock = ifElseBlocks[i];

            this.createConnectedBlock("IF" + i, conditionBlock.condition);
            this.createStatements("DO" + i, conditionBlock.statements);
        }

        if (hasElseBlock)
        {
            this.createStatements("ELSE", model.elseStatements);
        }
    }

    public toModel(): Models.EngineStatementLogicIf
    {
        let logicIf          = super.toModel();
        logicIf.ifElseBlocks = [];

        if (this.getInput("ELSE") !== null)
        {
            logicIf.elseStatements = this.getStatementsModels("ELSE");
        }

        for (let i = 0; i < this.elseifCount_ + 1; i++)
        {
            let ifBlock = Models.EngineConditionBlock.newInstance({
                                                                      condition : this.getInputModel("IF" + i),
                                                                      statements: this.getStatementsModels("DO" + i)
                                                                  });

            logicIf.ifElseBlocks.push(ifBlock);
        }

        return logicIf;
    }

    private updateShape()
    {
        (<any>this).updateShape_();
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_logic_literal",
              model       : Models.EngineLiteralBoolean,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class LiteralBooleanBlock extends Base.ExpressionBlock<Models.EngineLiteralBoolean>
{
    private static readonly c_field_VAL = "val";

    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([
                                                       [
                                                           "true",
                                                           "true"
                                                       ],
                                                       [
                                                           "false",
                                                           "false"
                                                       ]
                                                   ]), LiteralBooleanBlock.c_field_VAL);
    }

    protected initFromModel(model: Models.EngineLiteralBoolean): void
    {
        this.setField(LiteralBooleanBlock.c_field_VAL, model.value ? "true" : "false");

        super.initFromModel(model);
    }

    public toModel(): Models.EngineLiteralBoolean
    {
        let model   = super.toModel();
        model.value = this.getFieldString(LiteralBooleanBlock.c_field_VAL) === "true";
        return model;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_logic_literal_null",
              model       : Models.EngineLiteralNull
          })
export class LiteralNullBlock extends Base.ExpressionBlock<Models.EngineLiteralNull>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("null");
    }
}

export class LogicCategory extends DynamicToolboxCategory
{
    public getCategoryName(): string
    {
        return "Logic";
    }

    public generateBlocks(): Element[]
    {
        let elements = [];

        for (let definition of this.m_definitions)
        {
            elements.push(this.getElement(definition));
            if (definition == StatementLogicIfBlock)
            {
                elements.push(this.getIfElseBlock());
            }
        }

        return elements;
    }

    private getIfElseBlock()
    {
        let model   = Models.EngineStatementLogicIf.newInstance({elseStatements: []});
        let block   = Block.fromModel(this.m_workspace.workspace, model);
        let element = block.toDom();
        block.remove();

        return element;
    }
}
