import * as Base from "app/customer/engines/shared/base";
import {UnitEditorWrapperComponent} from "app/customer/engines/shared/unit-editor-wrapper.component";
import {AppDomainContext} from "app/services/domain/domain.module";
import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

export class UnitEditorField extends ModalDialogField<UnitEditorWrapperComponent, typeof UnitEditorWrapperComponent, Models.EngineeringUnitsFactors>
{
    private m_units: Models.EngineeringUnitsFactors;
    private m_displayName = "Loading...";

    constructor(block: Block<any>)
    {
        super(block, UnitEditorWrapperComponent, "Select control point");
    }

    public getModel(): Models.EngineeringUnitsFactors
    {
        return this.m_units;
    }

    protected getText(): string
    {
        return this.m_displayName;
    }

    protected initComponent(component: UnitEditorWrapperComponent): void
    {
        component.units = this.m_units;
        component.unitsChange.subscribe((units: Models.EngineeringUnitsFactors) =>
                                        {
                                            this.m_units = units;
                                            this.refreshDisplay();
                                        });
    }

    protected initData(data: Models.EngineeringUnitsFactors): void
    {
        if (data)
        {
            this.m_units = data;
        }
        else
        {
            this.m_units = Models.EngineeringUnitsFactors.newInstance({
                                                                          numeratorUnits  : [],
                                                                          denominatorUnits: []
                                                                      });
        }

        this.refreshDisplay();
    }

    private async refreshDisplay()
    {
        let domain = this.m_block.injectContext(AppDomainContext);
        let ext    = await domain.units.resolveDescriptor(this.m_units, false);
        if (ext)
        {
            this.m_displayName = ext.model.displayName;
            this.refreshText();
        }
    }
}


@BlockDef({
              blockContext: "Shared",
              blockName   : "literal_engineering_units",
              model       : Models.EngineLiteralEngineeringUnits,
              outputType  : Models.EngineValueEngineeringUnits
          })
export class LiteralEngineeringUnitsBlock extends Base.ExpressionBlock<Models.EngineLiteralEngineeringUnits>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        super.initFields();

        this.m_unitEditorField = new UnitEditorField(this);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.unitsFactors = value,
                                (model) => model.unitsFactors);

        this.appendDummyInput()
            .appendField(this.m_unitEditorField.field);
    }

    protected initForToolbox(model: Models.EngineLiteralEngineeringUnits): void
    {
        model.unitsFactors = Models.EngineeringUnitsFactors.newInstance({
                                                                            numeratorUnits  : [Models.EngineeringUnits.degrees_fahrenheit],
                                                                            denominatorUnits: [],
                                                                            primary         : Models.EngineeringUnits.degrees_fahrenheit
                                                                        });
        super.initForToolbox(model);
    }
}


@BlockDef({
              blockContext: "Shared",
              blockName   : "compare_units",
              model       : Models.EngineExpressionBinaryCompareEngineeringUnits,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class CompareEngineeringUnitsBlock extends Base.BinaryOperatorBlock<Models.EngineExpressionBinaryCompareEngineeringUnits>
{
    private static readonly c_field_OP = "OP";

    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValueEngineeringUnits, Models.EngineValueEngineeringUnits);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown([
                                                       [
                                                           "equals units",
                                                           "EQ"
                                                       ],
                                                       [
                                                           "does not equal units",
                                                           "NEQ"
                                                       ]
                                                   ]), CompareEngineeringUnitsBlock.c_field_OP);

        this.appendConnectedBlock(this.block_b);
    }

    protected initFromModel(model: Models.EngineExpressionBinaryCompareEngineeringUnits): void
    {
        this.setField(CompareEngineeringUnitsBlock.c_field_OP, this.getOperationString(model.operation));

        super.initFromModel(model);
    }

    public toModel(): Models.EngineExpressionBinaryCompareEngineeringUnits
    {
        let logicCompare       = super.toModel();
        logicCompare.operation = this.getOperation(this.getFieldString(CompareEngineeringUnitsBlock.c_field_OP));
        return logicCompare;
    }

    protected initForToolbox(model: Models.EngineExpressionBinaryCompareEngineeringUnits): void
    {
        model.operation = Models.CommonEngineCompareOperation.Equal;

        super.initForToolbox(model);
    }

    private getOperationString(op: Models.CommonEngineCompareOperation)
    {
        switch (op)
        {
            case Models.CommonEngineCompareOperation.Equal:
                return "EQ";

            case Models.CommonEngineCompareOperation.NotEqual:
                return "NEQ";

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

            case "EQ":
            default:
                return Models.CommonEngineCompareOperation.Equal;
        }
    }
}

@BlockDef({
              blockContext: "Shared",
              blockName   : "memoize",
              model       : Models.EngineExpressionMemoize,
              outputType  : Models.EngineValue
          })
export class MemoizeBlock extends Base.ExpressionBlock<Models.EngineExpressionMemoize>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("memoize");

        let value = this.registerConnectedBlock<Models.EngineExpression>("VALUE",
                                                                         false,
                                                                         (model,
                                                                          value) => model.value = value,
                                                                         (model) => model.value);

        this.appendConnectedBlock(value);

        this.setInputsInline(true);
    }
}
