import {Type} from "@angular/core";

import {MetricBlocklyWorkspaceData} from "app/customer/configuration/metrics/wizard/metric-blockly-workspace-data";
import * as Base from "app/customer/engines/shared/base";
import {UnitEditorField} from "app/customer/engines/shared/common";
import * as DateTime from "app/customer/engines/shared/datetime";
import {InputParameterBlock} from "app/customer/engines/shared/input";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";

import {UtilsService} from "framework/services/utils.service";
import {BlockDef} from "framework/ui/blockly/block";

@BlockDef({
              blockContext: "Metrics",
              blockName   : "set_output_to_scalar",
              model       : Models.MetricsEngineStatementSetOutputToScalar
          })
export class MetricsEngineStatementSetOutputToScalarBlock extends Base.StatementBlock<Models.MetricsEngineStatementSetOutputToScalar>
{
    protected initFields(): void
    {
        let block_source = this.registerConnectedBlock<Models.EngineExpression>("source",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.scalar = value,
                                                                                (model) => model.scalar,
                                                                                Models.MetricsEngineValueScalar);

        this.appendConnectedBlock(block_source)
            .appendField("set output to scalar");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "set_output_to_series",
              model       : Models.MetricsEngineStatementSetOutputToSeries
          })
export class MetricsEngineStatementSetOutputToSeriesBlock extends Base.StatementBlock<Models.MetricsEngineStatementSetOutputToSeries>
{
    protected initFields(): void
    {
        let block_source = this.registerConnectedBlock<Models.EngineExpression>("source",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.series = value,
                                                                                (model) => model.series,
                                                                                Models.MetricsEngineValueSeries);

        this.appendConnectedBlock(block_source)
            .appendField("set output to series");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "set_output_to_series_with_name",
              model       : Models.MetricsEngineStatementSetOutputToSeriesWithName
          })
export class MetricsEngineStatementSetOutputToSeriesWithNameBlock extends Base.StatementBlock<Models.MetricsEngineStatementSetOutputToSeriesWithName>
{
    protected initFields(): void
    {
        let field_val = this.registerStringField("name",
                                                 (model,
                                                  value) =>
                                                 {
                                                     if (value) this.setShadow(false);
                                                     model.name = value;
                                                 },
                                                 (model) => model.name);

        let block_source = this.registerConnectedBlock<Models.EngineExpression>("source",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.series = value,
                                                                                (model) => model.series,
                                                                                Models.MetricsEngineValueSeries);

        this.appendDummyInput()
            .appendField("set output named")
            .appendField(new Blockly.FieldTextInput(""), field_val);

        this.appendConnectedBlock(block_source)
            .appendField("to series");

        this.setInputsInline(true);
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_create_vector3",
              model       : Models.MetricsEngineCreateVector3,
              outputType  : Models.MetricsEngineValueSetOfSeries
          })
export class MetricsEngineCreateVector3Block extends Base.ExpressionBlock<Models.MetricsEngineCreateVector3>
{
    protected initFields(): void
    {
        super.initFields();

        let block_x = this.registerConnectedBlock<Models.EngineExpression>("XSeries",
                                                                           false,
                                                                           (model,
                                                                            value) => model.xSeries = value,
                                                                           (model) => model.xSeries,
                                                                           Models.MetricsEngineValueSeries);

        let block_y = this.registerConnectedBlock<Models.EngineExpression>("YSeries",
                                                                           false,
                                                                           (model,
                                                                            value) => model.ySeries = value,
                                                                           (model) => model.ySeries,
                                                                           Models.MetricsEngineValueSeries);

        let block_z = this.registerConnectedBlock<Models.EngineExpression>("ZSeries",
                                                                           false,
                                                                           (model,
                                                                            value) => model.zSeries = value,
                                                                           (model) => model.zSeries,
                                                                           Models.MetricsEngineValueSeries);

        this.appendDummyInput()
            .appendField("create vector from");

        this.appendConnectedBlock(block_x)
            .appendField("X")
            .setAlign(Blockly.ALIGN_RIGHT);

        this.appendConnectedBlock(block_y)
            .appendField("Y")
            .setAlign(Blockly.ALIGN_RIGHT);

        this.appendConnectedBlock(block_z)
            .appendField("Z")
            .setAlign(Blockly.ALIGN_RIGHT);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_vector_add",
              model       : Models.MetricsEngineOperatorVectorBinaryAdd,
              outputType  : Models.MetricsEngineValueSetOfSeries
          })
export class MetricsEngineOperatorVectorBinaryAddBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorVectorBinaryAdd>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSetOfSeries, Models.MetricsEngineValueSetOfSeries);

        this.appendConnectedBlock(this.block_a)
            .appendField("vector");

        this.appendConnectedBlock(this.block_b)
            .appendField("plus vector");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_vector_sub",
              model       : Models.MetricsEngineOperatorVectorBinarySubtract,
              outputType  : Models.MetricsEngineValueSetOfSeries
          })
export class MetricsEngineOperatorVectorBinarySubtractBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorVectorBinarySubtract>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSetOfSeries, Models.MetricsEngineValueSetOfSeries);

        this.appendConnectedBlock(this.block_a)
            .appendField("vector");

        this.appendConnectedBlock(this.block_b)
            .appendField("minus vector");

        this.setInputsInline(true);
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_constant",
              model       : Models.MetricsEngineLiteralScalar,
              outputType  : Models.MetricsEngineValueScalar
          })
export class LiteralScalarBlock extends Base.ExpressionBlock<Models.MetricsEngineLiteralScalar>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        super.initFields();

        this.m_unitEditorField = new UnitEditorField(this);

        let field_val = this.registerFloatField("val",
                                                (model,
                                                 value) => model.value = value,
                                                (model) => model.value);


        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.units = value,
                                (model) => model.units);

        this.appendDummyInput()
            .appendField("scalar")
            .appendField(new Blockly.FieldNumber(0), field_val)
            .appendField("with units")
            .appendField(this.m_unitEditorField.field);
    }

    protected initForToolbox(model: Models.MetricsEngineLiteralScalar): void
    {
        model.value = 0;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_input",
              model       : Models.MetricsEngineInputParameterScalar,
              outputType  : Models.MetricsEngineValueScalar
          })
export class InputParameterScalarBlock extends InputParameterBlock<Models.MetricsEngineInputParameterScalar>
{
    private m_unitEditorField: UnitEditorField;

    protected appendValueField(input: Blockly.Input)
    {
        this.m_unitEditorField = new UnitEditorField(this);

        let field_val = this.registerFloatField("val",
                                                (model,
                                                 value) => model.value = value,
                                                (model) => model.value);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.units = value,
                                (model) => model.units);

        input.appendField(new Blockly.FieldNumber(0), field_val)
             .appendField("with units")
             .appendField(this.m_unitEditorField.field);
    }

    protected initForToolbox(model: Models.MetricsEngineInputParameterScalar): void
    {
        model.value = 0;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_series_input",
              model       : Models.MetricsEngineInputParameterSeries,
              outputType  : Models.MetricsEngineValueSeries
          })
export class InputParameterSeriesBlock extends InputParameterBlock<Models.MetricsEngineInputParameterSeries>
{
    protected appendValueField(input: Blockly.Input)
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <MetricBlocklyWorkspaceData>component.data;

        function getFunctions(): string[][]
        {
            return data.resolveNodes();
        }

        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.nodeId = value,
                                                 (model) => model.nodeId);

        input.appendField(new Blockly.FieldDropdown(getFunctions), field_val);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_series_input_with_timeoffset",
              model       : Models.MetricsEngineInputParameterSeriesWithTimeOffset,
              outputType  : Models.MetricsEngineValueSeries
          })
export class InputParameterSeriesWithTimeOffsetBlock extends InputParameterBlock<Models.MetricsEngineInputParameterSeriesWithTimeOffset>
{
    private durationField: DateTime.DurationField;

    protected appendValueField(input: Blockly.Input)
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <MetricBlocklyWorkspaceData>component.data;

        function getFunctions(): string[][]
        {
            return data.resolveNodes();
        }

        let field_val = this.registerStringField("val",
                                                 (model,
                                                  value) => model.nodeId = value,
                                                 (model) => model.nodeId);

        this.durationField = new DateTime.DurationField(this);
        this.registerModalField(this.durationField,
                                (model,
                                 value) =>
                                {
                                    model.timeShift     = value.amount;
                                    model.timeShiftUnit = value.unit;
                                },
                                (model) =>
                                {
                                    return {
                                        amount: model.timeShift,
                                        unit  : model.timeShiftUnit
                                    };
                                });

        input.appendField(new Blockly.FieldDropdown(getFunctions), field_val);
        input.appendField("with offset of");
        input.appendField(this.durationField.field);
    }

    protected initForToolbox(model: Models.MetricsEngineInputParameterSeriesWithTimeOffset): void
    {
        model.timeShift     = 1;
        model.timeShiftUnit = Models.ChronoUnit.DAYS;
        super.initForToolbox(model);
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_as_series",
              model       : Models.MetricsEngineOperatorUnaryAsSeries,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorUnaryAsSeriesBlock extends Base.UnaryOperatorBlock<Models.MetricsEngineOperatorUnaryAsSeries>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries);

        this.appendConnectedBlock(this.block_a)
            .appendField("as series");
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_logic_not",
              model       : Models.MetricsEngineOperatorUnaryNot,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorUnaryNotBlock extends Base.UnaryOperatorBlock<Models.MetricsEngineOperatorUnaryNot>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries);

        this.appendConnectedBlock(this.block_a)
            .appendField("invert boolean series");
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_bistable",
              model       : Models.MetricsEngineOperatorBinaryBistable,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorBinaryBistableBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorBinaryBistable>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueSeries);

        this.appendDummyInput()
            .appendField("create bistable series");

        this.appendConnectedBlock(this.block_a)
            .appendField("set if")
            .setAlign(Blockly.ALIGN_RIGHT);

        this.appendConnectedBlock(this.block_b)
            .appendField("reset if")
            .setAlign(Blockly.ALIGN_RIGHT);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_enumerated_series",
              model       : Models.MetricsEngineCreateEnumeratedSeries,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineCreateEnumeratedSeriesBlock extends Base.ExpressionBlock<Models.MetricsEngineCreateEnumeratedSeries> implements Base.ListHelperHost
{
    private m_helper: Base.ListHelper<MetricsEngineCreateEnumeratedSeriesBlock, Models.MetricsEngineCreateEnumeratedSeries>;

    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("create enumerated series");

        this.m_helper = new Base.ListHelper(this);
        this.m_helper.init();
    }

    protected initFromModel(model: Models.MetricsEngineCreateEnumeratedSeries): void
    {
        this.m_helper.setInputs(model.value);

        super.initFromModel(model);
    }

    public toModel(): Models.MetricsEngineCreateEnumeratedSeries
    {
        let model   = super.toModel();
        model.value = this.m_helper.collectInputs();

        return model;
    }

    public regenerateShape()
    {

        this.m_helper.updateShapeForInputs();
    }

    public handleNewInput(index: number,
                          input: Blockly.Input): Type<Models.EngineValue>
    {
        return Models.MetricsEngineSelectValue;
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_multistable_series",
              model       : Models.MetricsEngineCreateMultiStableSeries,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineCreateMultiStableSeriesBlock extends Base.ExpressionBlock<Models.MetricsEngineCreateMultiStableSeries> implements Base.ListHelperHost
{
    private m_helper: Base.ListHelper<MetricsEngineCreateMultiStableSeriesBlock, Models.MetricsEngineCreateMultiStableSeries>;

    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("create multistable series");

        this.m_helper = new Base.ListHelper(this);
        this.m_helper.init();
    }

    protected initFromModel(model: Models.MetricsEngineCreateMultiStableSeries): void
    {
        this.m_helper.setInputs(model.value);

        super.initFromModel(model);
    }

    public toModel(): Models.MetricsEngineCreateMultiStableSeries
    {
        let model   = super.toModel();
        model.value = this.m_helper.collectInputs();

        return model;
    }

    public regenerateShape()
    {
        this.m_helper.updateShapeForInputs();
    }

    public handleNewInput(index: number,
                          input: Blockly.Input): Type<Models.EngineValue>
    {
        return Models.MetricsEngineSelectValue;
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_select_value",
              model       : Models.MetricsEngineOperatorUnarySelectValue,
              outputType  : Models.MetricsEngineSelectValue
          })
export class MetricsEngineOperatorUnarySelectValueBlock extends Base.UnaryOperatorBlock<Models.MetricsEngineOperatorUnarySelectValue>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries);

        let field_val = this.registerStringField("case",
                                                 (model,
                                                  value) =>
                                                 {
                                                     if (value) this.setShadow(false);
                                                     model.identifier = value;
                                                 },
                                                 (model) => model.identifier);

        this.appendDummyInput()
            .appendField("use")
            .appendField(new Blockly.FieldTextInput(""), field_val);

        this.appendConnectedBlock(this.block_a)
            .appendField("where");

        this.appendDummyInput()
            .appendField("is true");

        this.setInputsInline(true);
    }
}

//--//

const arithmeticOperations = [
    [
        "adding",
        Models.CommonEngineArithmeticOperation.Plus
    ],
    [
        "subtracting",
        Models.CommonEngineArithmeticOperation.Minus
    ],
    [
        "multiplying by",
        Models.CommonEngineArithmeticOperation.Multiply
    ],
    [
        "dividing by",
        Models.CommonEngineArithmeticOperation.Divide
    ]
];

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_arithmetic",
              model       : Models.MetricsEngineOperatorBinary,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorBinaryBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorBinary>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValue, Models.MetricsEngineValue);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineArithmeticOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("transform");

        this.appendDummyInput()
            .appendField("by")
            .appendField(new Blockly.FieldDropdown(arithmeticOperations), field_op);

        this.appendConnectedBlock(this.block_b);

        this.setInputsInline(true);
    }
}

//--//

const logicOperations = [
    [
        "or",
        Models.CommonEngineLogicOperation.Or
    ],
    [
        "not or",
        Models.CommonEngineLogicOperation.NotOr
    ],
    [
        "and",
        Models.CommonEngineLogicOperation.And
    ],
    [
        "not and",
        Models.CommonEngineLogicOperation.NotAnd
    ],
    [
        "xor",
        Models.CommonEngineLogicOperation.Xor
    ],
    [
        "not xor",
        Models.CommonEngineLogicOperation.NotXor
    ]
];

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_number_logic",
              model       : Models.MetricsEngineOperatorBinaryLogic,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorBinaryLogicBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorBinaryLogic>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValue);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineLogicOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("combine boolean series");

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(logicOperations), field_op)
            .appendField("series");

        this.appendConnectedBlock(this.block_b);

        this.setInputsInline(true);
    }
}

//--//

const thresholdOperations = [
    [
        "less than",
        Models.CommonEngineCompareOperation.LessThan
    ],
    [
        "less than or equal",
        Models.CommonEngineCompareOperation.LessThanOrEqual
    ],
    [
        "greater than or equal",
        Models.CommonEngineCompareOperation.GreaterThanOrEqual
    ],
    [
        "greater than",
        Models.CommonEngineCompareOperation.GreaterThan
    ]
];

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold",
              model       : Models.MetricsEngineOperatorThreshold,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorThreshold>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueScalar);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineCompareOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("threshold for");

        this.appendConnectedBlock(this.block_b)
            .appendField("if")
            .appendField(new Blockly.FieldDropdown(thresholdOperations), field_op);

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold_count",
              model       : Models.MetricsEngineOperatorThresholdCount,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdCountBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorThresholdCount>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueScalar);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineCompareOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("count how many times");

        this.appendConnectedBlock(this.block_b)
            .appendField("is")
            .appendField(new Blockly.FieldDropdown(thresholdOperations), field_op);

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold_duration",
              model       : Models.MetricsEngineOperatorThresholdDuration,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdDurationBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorThresholdDuration>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueScalar);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineCompareOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("accumulate how long");

        this.appendConnectedBlock(this.block_b)
            .appendField("is")
            .appendField(new Blockly.FieldDropdown(thresholdOperations), field_op);

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold_partial_duration",
              model       : Models.MetricsEngineOperatorThresholdPartialDuration,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdPartialDurationBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorThresholdPartialDuration>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueScalar);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) => model.operation = UtilsService.getEnumValue(Models.CommonEngineCompareOperation, value),
                                                (model) => model.operation);

        this.appendConnectedBlock(this.block_a)
            .appendField("measure how long");

        this.appendConnectedBlock(this.block_b)
            .appendField("is")
            .appendField(new Blockly.FieldDropdown(thresholdOperations), field_op);

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold_enum",
              model       : Models.MetricsEngineOperatorThresholdEnum,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdEnumBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorThresholdDuration>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.EngineValueList);

        this.appendConnectedBlock(this.block_a)
            .appendField("threshold for");

        this.appendConnectedBlock(this.block_b)
            .appendField("in");

        this.setInputsInline(true);
    }
}

const thresholdRangeOptions = [
    [
        "inside, endpoints excluded",
        "exclusive/normal"
    ],
    [
        "inside, endpoints included",
        "inclusive/normal"
    ],
    [
        "outside, endpoints included",
        "exclusive/invert"
    ],
    [
        "outside, endpoints excluded",
        "inclusive/invert"
    ]
];

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_threshold_range",
              model       : Models.MetricsEngineOperatorThresholdRange,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsThresholdRangeBlock extends Base.ExpressionBlock<Models.MetricsEngineOperatorThresholdRange>
{
    protected initFields(): void
    {
        super.initFields();

        let block_series = this.registerConnectedBlock<Models.EngineExpression>("Series",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.series = value,
                                                                                (model) => model.series,
                                                                                Models.MetricsEngineValueSeries);

        let block_lower = this.registerConnectedBlock<Models.EngineExpression>("YSeries",
                                                                               false,
                                                                               (model,
                                                                                value) => model.lowerBound = value,
                                                                               (model) => model.lowerBound,
                                                                               Models.MetricsEngineValueScalar);

        let block_upper = this.registerConnectedBlock<Models.EngineExpression>("ZSeries",
                                                                               false,
                                                                               (model,
                                                                                value) => model.upperBound = value,
                                                                               (model) => model.upperBound,
                                                                               Models.MetricsEngineValueScalar);

        let field_op = this.registerStringField("op",
                                                (model,
                                                 value) =>
                                                {
                                                    let parts       = value.split("/");
                                                    model.inclusive = parts[0] == "inclusive";
                                                    model.invert    = parts[1] == "invert";
                                                },
                                                (model) => `${model.inclusive ? "inclusive" : "exclusive"}/${model.invert ? "invert" : "normal"}`);

        this.appendConnectedBlock(block_series)
            .appendField("true if series");

        this.appendDummyInput()
            .appendField(new Blockly.FieldDropdown(thresholdRangeOptions), field_op);

        this.appendConnectedBlock(block_lower)
            .appendField("range between");

        this.appendConnectedBlock(block_upper)
            .appendField("and");

        this.setInputsInline(true);
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_filter",
              model       : Models.MetricsEngineOperatorFilter,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsFilterBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorFilter>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.MetricsEngineValueSeries);

        this.appendConnectedBlock(this.block_a)
            .appendField("filter series");

        this.appendConnectedBlock(this.block_b)
            .appendField("when values are positive in");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_filter_inside_schedule",
              model       : Models.MetricsEngineOperatorFilterInsideSchedule,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsFilterInsideScheduleBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorFilterInsideSchedule>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.EngineValueWeeklySchedule);

        this.appendConnectedBlock(this.block_a)
            .appendField("filter series");

        this.appendConnectedBlock(this.block_b)
            .appendField("to only values inside schedule");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_filter_outside_schedule",
              model       : Models.MetricsEngineOperatorFilterOutsideSchedule,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsFilterOutsideScheduleBlock extends Base.BinaryOperatorBlock<Models.MetricsEngineOperatorFilterOutsideSchedule>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.MetricsEngineValueSeries, Models.EngineValueWeeklySchedule);

        this.appendConnectedBlock(this.block_a)
            .appendField("filter series");

        this.appendConnectedBlock(this.block_b)
            .appendField("to only values outside schedule");

        this.setInputsInline(true);
    }
}

//--//

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_gps_distance",
              model       : Models.MetricsEngineOperatorGpsDistance,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorGpsDistanceBlock extends Base.ExpressionBlock<Models.MetricsEngineOperatorGpsDistance>
{
    protected initFields(): void
    {
        super.initFields();

        let block_latitude = this.registerConnectedBlock<Models.EngineExpression>("Latitude",
                                                                                  false,
                                                                                  (model,
                                                                                   value) => model.latitude = value,
                                                                                  (model) => model.latitude,
                                                                                  Models.MetricsEngineValueSeries);

        let block_longitude = this.registerConnectedBlock<Models.EngineExpression>("Longitude",
                                                                                   false,
                                                                                   (model,
                                                                                    value) => model.longitude = value,
                                                                                   (model) => model.longitude,
                                                                                   Models.MetricsEngineValueSeries);

        let block_speed = this.registerConnectedBlock<Models.EngineExpression>("Speed",
                                                                               false,
                                                                               (model,
                                                                                value) => model.speed = value,
                                                                               (model) => model.speed,
                                                                               Models.MetricsEngineValueSeries);


        this.appendConnectedBlock(block_latitude)
            .appendField("compute distance travelled using latitude");

        this.appendConnectedBlock(block_longitude)
            .appendField("longitude");

        this.appendConnectedBlock(block_speed)
            .appendField("and speed");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "Metrics",
              blockName   : "metrics_gps_sun_elevation",
              model       : Models.MetricsEngineOperatorGpsSunElevation,
              outputType  : Models.MetricsEngineValueSeries
          })
export class MetricsEngineOperatorGpsSunElevationBlock extends Base.ExpressionBlock<Models.MetricsEngineOperatorGpsSunElevation>
{
    protected initFields(): void
    {
        super.initFields();

        let block_latitude = this.registerConnectedBlock<Models.EngineExpression>("Latitude",
                                                                                  false,
                                                                                  (model,
                                                                                   value) => model.latitude = value,
                                                                                  (model) => model.latitude,
                                                                                  Models.MetricsEngineValueSeries);

        let block_longitude = this.registerConnectedBlock<Models.EngineExpression>("Longitude",
                                                                                  false,
                                                                                  (model,
                                                                                   value) => model.longitude = value,
                                                                                  (model) => model.longitude,
                                                                                  Models.MetricsEngineValueSeries);


        this.appendConnectedBlock(block_latitude)
            .appendField("compute Sun elevation using latitude");

        this.appendConnectedBlock(block_longitude)
            .appendField("and longitude");

        this.setInputsInline(true);
    }
}
