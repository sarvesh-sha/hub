import {Type} from "@angular/core";

import * as Base from "app/customer/engines/shared/base";
import {ExportableStringSetField} from "app/customer/engines/shared/exportable-string-set.component";
import * as Variables from "app/customer/engines/shared/variables";

import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";

import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "Shared",
              blockName   : "for_each",
              model       : Models.EngineStatementForEach
          })
export class StatementForEachBlock extends Base.StatementBlock<Models.EngineStatementForEach>
{
    private static readonly c_field_LIST = "list";
    private static readonly c_field_DO   = "do";

    private initialVarName: string;

    private field_val: string;

    protected initFields(): void
    {
        let component       = this.getComponent<AppBlocklyWorkspaceComponent>();
        let variableHolder  = component.variableHolder;
        this.initialVarName = variableHolder.generateUniqueName();

        this.field_val = this.registerStringField("varname",
                                                  (model,
                                                   value) =>
                                                  {
                                                      if (!this.isInFlyout && !this.isInsertionMarker())
                                                      {
                                                          variableHolder.addVariable(value);
                                                      }

                                                      let varDesc = this.getVar();
                                                      if (varDesc)
                                                      {
                                                          model.variable = varDesc.toModel();
                                                      }
                                                  },
                                                  (model) =>
                                                  {
                                                      if (model.variable) return model.variable.name;

                                                      return this.initialVarName;
                                                  });

        this.appendTypedValueInput(StatementForEachBlock.c_field_LIST, Models.EngineValueList)
            .appendField("for each item")
            .appendField(new Blockly.FieldDropdown(() => this.getVariables()), this.field_val)
            .appendField("in");
        this.appendStatementInput(StatementForEachBlock.c_field_DO)
            .appendField("do");
    }

    public toXml(): string
    {
        let text = super.toXml();
        // Don't include variable field to prevent clashes while initializing.
        return text.replace(/<field name="varname">.*<\/field>/, "");
    }

    private getVariables()
    {
        let component      = this.getComponent<AppBlocklyWorkspaceComponent>();
        let variableHolder = component.variableHolder;
        let vars           = variableHolder.getAllVariables();

        let options = vars.map((variable) => [
            variable.name,
            variable.name
        ]);

        if (this.initialVarName && (this.isInFlyout || vars.length === 0 || !variableHolder.getVariableByName(this.initialVarName, false)))
        {
            options.push([
                             this.initialVarName,
                             this.initialVarName
                         ]);
        }

        return options;
    }

    public getVar(): Variables.VariableDescriptor
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        return component.variableHolder.getVariableByName(this.getFieldString(this.field_val));
    }

    public getItemType(): Type<Models.EngineValue>
    {
        let assignmentBlock = Block.cast(this.getInputTargetBlock(StatementForEachBlock.c_field_LIST));
        if (assignmentBlock)
        {
            let desc = assignmentBlock.getDescriptor();
            if (desc)
            {
                return desc.config.outputElementType;
            }
        }

        return undefined;
    }

    protected initFromModel(model: Models.EngineStatementForEach): void
    {
        let component      = this.getComponent<AppBlocklyWorkspaceComponent>();
        let variableHolder = component.variableHolder;
        if (!this.isInFlyout && model.variable)
        {
            // Already had a variable, we can delete the temporary one we created.
            variableHolder.removeVariable(this.initialVarName);
            this.initialVarName = null;
        }

        this.createConnectedBlock(StatementForEachBlock.c_field_LIST, model.list);
        this.createStatements(StatementForEachBlock.c_field_DO, model.statements);

        super.initFromModel(model);
    }

    public toModel(): Models.EngineStatementForEach
    {
        let model        = super.toModel();
        model.list       = this.getInputModel(StatementForEachBlock.c_field_LIST);
        model.statements = this.getStatementsModels(StatementForEachBlock.c_field_DO);

        return model;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "list_length",
              model       : Models.EngineOperatorUnaryListLength,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class ListLengthBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryListLength>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue);

        this.appendConnectedBlock(this.block_a)
            .appendField("length of");
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "list_literal",
              model       : Models.EngineLiteralList,
              outputType  : Models.EngineValueList
          })
export class ListLiteralBlock extends Base.ExpressionBlock<Models.EngineLiteralList> implements Base.ListHelperHost
{
    private m_helper: Base.ListHelper<ListLiteralBlock, Models.EngineLiteralList>;

    protected initFields(): void
    {
        super.initFields();

        this.m_helper = new Base.ListHelper(this);
        this.m_helper.init();
    }

    protected initFromModel(model: Models.EngineLiteralList): void
    {
        this.m_helper.setInputs(model.value);

        super.initFromModel(model);
    }

    public toModel(): Models.EngineLiteralList
    {
        let model   = super.toModel();
        model.value = this.m_helper.collectInputs();
        return model;
    }

    public regenerateShape()
    {
        if (this.m_helper.itemCount && this.getInput("EMPTY"))
        {
            this.removeInput("EMPTY");
        }
        else if (!this.m_helper.itemCount && !this.getInput("EMPTY"))
        {
            this.appendDummyInput("EMPTY")
                .appendField("create empty list");
        }

        this.m_helper.updateShapeForInputs();
    }

    public handleNewInput(index: number,
                          input: Blockly.Input): Type<Models.EngineValue>
    {
        if (index == 0)
        {
            input.appendField("create list with");
        }

        return null;
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "break",
              model       : Models.EngineStatementBreak
          })
export class StatementBreakBlock extends Base.AppBlock<Models.EngineStatementBreak>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("break");
        this.setPreviousStatement(true);
        this.setNextStatement(false);
    }

    protected initFromModel(): void
    {
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "continue",
              model       : Models.EngineStatementContinue
          })
export class StatementContinueBlock extends Base.AppBlock<Models.EngineStatementContinue>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("continue");
        this.setPreviousStatement(true);
        this.setNextStatement(false);
    }

    protected initFromModel(): void
    {
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_list__join",
              model       : Models.EngineExpressionBinaryListJoin,
              outputType  : Models.EngineValuePrimitiveString
          })
export class ListJoinBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryListJoin>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("join");

        this.appendConnectedBlock(this.block_b)
            .appendField("with");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_list__get",
              model       : Models.EngineExpressionBinaryListGet,
              outputType  : Models.EngineValue
          })
export class ListGetBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryListGet>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValuePrimitiveNumber);

        this.appendConnectedBlock(this.block_b)
            .appendField("get");

        this.appendConnectedBlock(this.block_a)
            .appendField("from");
        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_list__contains",
              model       : Models.EngineExpressionBinaryListContains,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class ListContainsBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryListContains>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValue);

        this.appendConnectedBlock(this.block_a)
            .appendField("list");

        this.appendConnectedBlock(this.block_b)
            .appendField("contains");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "o3_as_list",
              model       : Models.EngineOperatorUnaryAsList,
              outputType  : Models.EngineValueList
          })
export class ListAsListBlock extends Base.UnaryOperatorBlock<Models.EngineOperatorUnaryAsList>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValue);

        this.appendConnectedBlock(this.block_a)
            .appendField("as list");
    }
}

@BlockDef({
              blockContext     : "Shared",
              blockName        : "o3_string_set_literal",
              model            : Models.EngineLiteralStringSet,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString
          })
export class LiteralStringSetBlock extends Base.ExpressionBlock<Models.EngineLiteralStringSet>
{
    private m_termsField: ExportableStringSetField;

    protected initFields(): void
    {
        super.initFields();

        this.m_termsField = new ExportableStringSetField(this, "Strings");
        this.registerModalField(this.m_termsField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        this.appendDummyInput()
            .appendField(this.m_termsField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.EngineLiteralStringSet)
    {
        model.value = [];
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext     : "Shared",
              blockName        : "o3_string_set_operation",
              model            : Models.EngineOperatorBinaryForStringSet,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString
          })
export class StringSetOperationBlock extends Base.BinaryOperatorBlock<Models.EngineOperatorBinaryForStringSet>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueList, Models.EngineValueList);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineSetOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("combine strings");

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(this.getOperations()), field_op);

        this.appendConnectedBlock(this.block_b);
    }

    private getOperations()
    {
        return [
            [
                "union",
                Models.CommonEngineSetOperation.Add
            ],
            [
                "difference",
                Models.CommonEngineSetOperation.Subtract
            ],
            [
                "intersection",
                Models.CommonEngineSetOperation.Intersect
            ]
        ];
    }
}

