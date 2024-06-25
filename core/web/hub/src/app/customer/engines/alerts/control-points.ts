import {Type} from "@angular/core";

import * as Base from "app/customer/engines/shared/base";
import {UnitEditorField} from "app/customer/engines/shared/common";
import * as Input from "app/customer/engines/shared/input";

import {AppDomainContext} from "app/services/domain/domain.module";
import {ControlPointsSelectionExtended} from "app/services/domain/report-definitions.service";

import * as Models from "app/services/proxy/model/models";
import {ControlPointSelectorComponent} from "app/shared/search/control-point-selector.component";

import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";

export class ControlPointSelectionField extends ModalDialogField<ControlPointSelectorComponent, typeof ControlPointSelectorComponent, Models.RecordIdentity>
{
    private m_controlPoints: ControlPointsSelectionExtended;

    constructor(block: Block<any>)
    {
        super(block, ControlPointSelectorComponent, "Select control point");
    }

    public getModel(): Models.RecordIdentity
    {
        return this.m_controlPoints.identities[0];
    }

    protected getText(): string
    {
        return this.m_controlPoints.getDisplayText(true);
    }

    protected initComponent(component: ControlPointSelectorComponent): void
    {
        component.selection = this.m_controlPoints;
        component.limit     = 1;
    }

    protected initData(data: Models.RecordIdentity): void
    {
        let domain    = this.m_block.injectContext(AppDomainContext);
        let selection = Models.ControlPointsSelection.newInstance({identities: []});
        if (data)
        {
            selection.identities.push(data);
        }
        this.m_controlPoints = new ControlPointsSelectionExtended(domain, selection);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "control_point",
              model       : Models.AlertEngineLiteralControlPoint,
              outputType  : Models.AlertEngineValueControlPoint
          })
export class LiteralControlPointBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralControlPoint>
{
    private controlPointField: ControlPointSelectionField;

    protected initFields(): void
    {
        super.initFields();

        this.controlPointField = new ControlPointSelectionField(this);
        this.registerModalField(this.controlPointField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        this.appendDummyInput()
            .appendField(this.controlPointField.field);
    }
}

export class ControlPointsSelectionField extends ModalDialogField<ControlPointSelectorComponent, typeof ControlPointSelectorComponent, Models.ControlPointsSelection>
{
    private m_controlPoints: ControlPointsSelectionExtended;

    constructor(block: Block<any>)
    {
        super(block, ControlPointSelectorComponent, "Select control points");
    }

    public getModel(): Models.ControlPointsSelection
    {
        return this.m_controlPoints.model;
    }

    protected getText(): string
    {
        return this.m_controlPoints.getDisplayText();
    }

    protected initComponent(component: ControlPointSelectorComponent): void
    {
        component.selection = this.m_controlPoints;
    }

    protected initData(data: Models.ControlPointsSelection): void
    {
        let domain           = this.m_block.injectContext(AppDomainContext);
        this.m_controlPoints = new ControlPointsSelectionExtended(domain, data);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "control_points_selection",
              model            : Models.AlertEngineLiteralControlPointsSelection,
              outputType       : Models.EngineValueList,
              outputElementType: Models.AlertEngineValueControlPoint
          })
export class LiteralControlPointsSelectionBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralControlPointsSelection>
{
    private controlPointsField: ControlPointsSelectionField;

    protected initFields(): void
    {
        super.initFields();

        this.controlPointsField = new ControlPointsSelectionField(this);
        this.registerModalField(this.controlPointsField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        this.appendDummyInput()
            .appendField(this.controlPointsField.field);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralControlPointsSelection): void
    {
        model.value = new Models.ControlPointsSelection();
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "control_point_last_sample",
              model       : Models.AlertEngineOperatorUnaryControlPointLastSample,
              outputType  : Models.AlertEngineValueSample
          })
export class ControlPointPresentValueBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointLastSample>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("last sample of");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_sample",
              model       : Models.AlertEngineExpressionBinaryControlPointSample,
              outputType  : Models.AlertEngineValueSample
          })
export class ControlPointGetSampleBlock extends Base.BinaryOperatorBlock<Models.AlertEngineExpressionBinaryControlPointSample>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint, Models.EngineValueDateTime);

        this.appendConnectedBlock(this.block_a)
            .appendField("get sample from");

        this.appendConnectedBlock(this.block_b)
            .appendField("at time");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "get_sample_range",
              model            : Models.AlertEngineExpressionBinaryControlPointSampleRange,
              outputType       : Models.EngineValueList,
              outputElementType: Models.AlertEngineValueSample
          })
export class ControlPointGetSampleRangeBlock extends Base.BinaryOperatorBlock<Models.AlertEngineExpressionBinaryControlPointSampleRange>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint, Models.EngineValueDateTimeRange);

        this.appendConnectedBlock(this.block_a)
            .appendField("get samples from");

        this.appendConnectedBlock(this.block_b)
            .appendField("for time range");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "get_new_samples",
              model            : Models.AlertEngineOperatorUnaryControlPointNewSamples,
              outputType       : Models.EngineValueList,
              outputElementType: Models.AlertEngineValueSample
          })
export class ControlPointGetNewSamplesBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointNewSamples>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("get new samples from");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_sample_aggregate",
              model       : Models.AlertEngineExpressionBinaryControlPointSampleAggregate,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class ControlPointGetSampleAggregateBlock extends Base.BinaryOperatorBlock<Models.AlertEngineExpressionBinaryControlPointSampleAggregate>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint, Models.EngineValueDateTimeRange);

        this.m_unitEditorField = new UnitEditorField(this);

        let fieldAggregate = this.registerStringField("aggregate",
                                                      (model,
                                                       value) => model.aggregate = UtilsService.getEnumValue(Models.AlertSampleAggregate, value),
                                                      (model) => model.aggregate);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.unitsFactors = value,
                                (model) => model.unitsFactors);

        this.appendConnectedBlock(this.block_a)
            .appendField("compute")
            .appendField(new Blockly.FieldDropdown(ControlPointGetSampleAggregateBlock.getAggregateNames()), fieldAggregate)
            .appendField("as")
            .appendField(this.m_unitEditorField.field)
            .appendField("from");

        this.appendConnectedBlock(this.block_b)
            .appendField("for time range");

        this.setInputsInline(true);
    }

    private static getAggregateNames()
    {
        return [
            [
                "minimum",
                Models.AlertSampleAggregate.min
            ],
            [
                "maximum",
                Models.AlertSampleAggregate.max
            ],
            [
                "average",
                Models.AlertSampleAggregate.average
            ],
            [
                "difference",
                Models.AlertSampleAggregate.delta
            ],
            [
                "increment",
                Models.AlertSampleAggregate.increment
            ],
            [
                "decrement",
                Models.AlertSampleAggregate.decrement
            ]
        ];
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_sample_property",
              model       : Models.AlertEngineOperatorUnarySampleGetProperty,
              outputType  : Models.EngineValuePrimitive
          })
export class SampleGetPropertyBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnarySampleGetProperty>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueSample);

        this.m_unitEditorField = new UnitEditorField(this);

        let setOutputType = (newProperty: string) =>
        {
            let output = this.getOutputTypeFromProperty(newProperty);
            if (output)
            {
                this.setOutput(true, Block.computeTypeClosure(output));
            }

            return newProperty;
        };

        let field_propertyName = this.registerStringField("propertyName",
                                                          (model,
                                                           value) => model.property = UtilsService.getEnumValue(Models.AlertEngineSampleProperty, value),
                                                          (model) => model.property);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.unitsFactors = value,
                                (model) => model.unitsFactors);

        this.appendConnectedBlock(this.block_a)
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(SampleGetPropertyBlock.getPropertyNames(), setOutputType), field_propertyName)
            .appendField("as")
            .appendField(this.m_unitEditorField.field)
            .appendField("from sample");
    }

    public inferOutputType(): Type<Models.EngineValue>
    {
        let output = this.getOutputTypeFromPropertyEnum(this.toModel().property);
        return output ? output : super.inferOutputType();
    }

    private getOutputTypeFromProperty(property: string): Type<Models.EngineValue>
    {
        return this.getOutputTypeFromPropertyEnum(UtilsService.getEnumValue(Models.AlertEngineSampleProperty, property));
    }

    private getOutputTypeFromPropertyEnum(prop: Models.AlertEngineSampleProperty): Type<Models.EngineValue>
    {
        switch (prop)
        {
            case Models.AlertEngineSampleProperty.PresentValue:
                return Models.EngineValuePrimitive;

            case Models.AlertEngineSampleProperty.OutOfService:
                return Models.EngineValuePrimitiveBoolean;

            case Models.AlertEngineSampleProperty.InAlarm:
                return Models.EngineValuePrimitiveBoolean;
        }

        return undefined;
    }

    private static getPropertyNames()
    {
        return [
            [
                "present value",
                Models.AlertEngineSampleProperty.PresentValue
            ],
            [
                "is in alarm",
                Models.AlertEngineSampleProperty.InAlarm
            ],
            [
                "is out of service",
                Models.AlertEngineSampleProperty.OutOfService
            ]
        ];
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_controlpoint_property",
              model       : Models.AlertEngineOperatorUnaryGetProperty,
              outputType  : Models.EngineValuePrimitive
          })
export class ControlPointGetPropertyBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetProperty>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.m_unitEditorField = new UnitEditorField(this);

        let setOutputType = (newProperty: string) =>
        {
            let output = this.getOutputTypeFromProperty(newProperty);
            if (output)
            {
                this.setOutput(true, Block.computeTypeClosure(output));
            }

            return newProperty;
        };

        let field_propertyName = this.registerStringField("propertyName",
                                                          (model,
                                                           value) => model.property = UtilsService.getEnumValue(Models.AlertEngineSampleProperty, value),
                                                          (model) => model.property);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.unitsFactors = value,
                                (model) => model.unitsFactors);

        this.appendConnectedBlock(this.block_a)
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(ControlPointGetPropertyBlock.getPropertyNames(), setOutputType), field_propertyName)
            .appendField("as")
            .appendField(this.m_unitEditorField.field)
            .appendField("from control point");
    }

    public inferOutputType(): Type<Models.EngineValue>
    {
        let output = this.getOutputTypeFromPropertyEnum(this.toModel().property);
        return output ? output : super.inferOutputType();
    }

    private getOutputTypeFromProperty(property: string): Type<Models.EngineValue>
    {
        return this.getOutputTypeFromPropertyEnum(UtilsService.getEnumValue(Models.AlertEngineSampleProperty, property));
    }

    private getOutputTypeFromPropertyEnum(prop: Models.AlertEngineSampleProperty): Type<Models.EngineValue>
    {
        switch (prop)
        {
            case Models.AlertEngineSampleProperty.PresentValue:
                return Models.EngineValuePrimitive;

            case Models.AlertEngineSampleProperty.OutOfService:
                return Models.EngineValuePrimitiveBoolean;

            case Models.AlertEngineSampleProperty.InAlarm:
                return Models.EngineValuePrimitiveBoolean;
        }

        return undefined;
    }

    private static getPropertyNames()
    {
        return [
            [
                "present value",
                Models.AlertEngineSampleProperty.PresentValue
            ],
            [
                "is in alarm",
                Models.AlertEngineSampleProperty.InAlarm
            ],
            [
                "is out of service",
                Models.AlertEngineSampleProperty.OutOfService
            ]
        ];
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "set_controlpoint_property",
              model       : Models.AlertEngineStatementSetProperty
          })
export class ControlPointSetPropertyBlock extends Base.StatementBlock<Models.AlertEngineStatementSetProperty>
{
    private m_unitEditorField: UnitEditorField;

    protected initFields(): void
    {
        this.m_unitEditorField = new UnitEditorField(this);

        let field_propertyName = this.registerStringField("propertyName",
                                                          (model,
                                                           value) => model.property = UtilsService.getEnumValue(Models.AlertEngineSampleProperty, value),
                                                          (model) => model.property);

        this.registerModalField(this.m_unitEditorField,
                                (model,
                                 value) => model.unitsFactors = value,
                                (model) => model.unitsFactors);

        let block_controlpoint = this.registerConnectedBlock<Models.EngineExpression>("controlpoint",
                                                                                      false,
                                                                                      (model,
                                                                                       value) => model.controlPoint = value,
                                                                                      (model) => model.controlPoint,
                                                                                      Models.AlertEngineValueControlPoint);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValue);


        this.appendConnectedBlock(block_controlpoint)
            .appendField("set")
            .appendField(new Blockly.FieldDropdown(ControlPointSetPropertyBlock.getPropertyNames()), field_propertyName)
            .appendField("as")
            .appendField(this.m_unitEditorField.field)
            .appendField("for control point");

        this.appendConnectedBlock(block_value)
            .appendField("to");

        this.setInputsInline(true);
    }

    private static getPropertyNames()
    {
        return [
            [
                "present value",
                Models.AlertEngineSampleProperty.PresentValue
            ],
            [
                "is in alarm",
                Models.AlertEngineSampleProperty.InAlarm
            ],
            [
                "is out of service",
                Models.AlertEngineSampleProperty.OutOfService
            ]
        ];
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_sample_time",
              model       : Models.AlertEngineOperatorUnarySampleGetTime,
              outputType  : Models.EngineValueDateTime
          })
export class SampleGetTimeBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnarySampleGetTime>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueSample);

        this.appendConnectedBlock(this.block_a)
            .appendField("get time of sample");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "control_point_input",
              model       : Models.AlertEngineInputParameterControlPoint,
              outputType  : Models.AlertEngineValueControlPoint
          })
export class InputParameterControlPointBlock extends Input.InputParameterBlock<Models.AlertEngineInputParameterControlPoint>
{
    private controlPointField: ControlPointSelectionField;

    protected appendValueField(input: Blockly.Input)
    {
        this.controlPointField = new ControlPointSelectionField(this);
        this.registerModalField(this.controlPointField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        input.appendField(this.controlPointField.field);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "control_points_selection_input",
              model            : Models.AlertEngineInputParameterControlPointsSelection,
              outputType       : Models.EngineValueList,
              outputElementType: Models.AlertEngineValueControlPoint
          })
export class InputParameterControlPointsSelectionBlock extends Input.InputParameterBlock<Models.AlertEngineInputParameterControlPointsSelection>
{
    private controlPointsField: ControlPointsSelectionField;

    protected appendValueField(input: Blockly.Input)
    {
        this.controlPointsField = new ControlPointsSelectionField(this);
        this.registerModalField(this.controlPointsField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        input.appendField(this.controlPointsField.field);
    }

    protected initForToolbox(model: Models.AlertEngineInputParameterControlPointsSelection): void
    {
        model.value = new Models.ControlPointsSelection();
        super.initForToolbox(model);
    }
}

//--//

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "set_controlpoint_metadata",
              model       : Models.AlertEngineStatementSetControlPointMetadata
          })
export class ControlPointSetMetadataBlock extends Base.StatementBlock<Models.AlertEngineStatementSetControlPointMetadata>
{
    protected initFields(): void
    {
        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        let block_controlpoint = this.registerConnectedBlock<Models.EngineExpression>("controlpoint",
                                                                                      false,
                                                                                      (model,
                                                                                       value) => model.controlPoint = value,
                                                                                      (model) => model.controlPoint,
                                                                                      Models.AlertEngineValueControlPoint);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValue);

        this.appendConnectedBlock(block_controlpoint)
            .appendField("set metadata")
            .appendField(new Blockly.FieldTextInput(""), field_key)
            .appendField("of control point");

        this.appendConnectedBlock(block_value)
            .appendField("to");
        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_controlpoint_metadata_number",
              model       : Models.AlertEngineOperatorUnaryControlPointMetadataNumber,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class ControlPointGetMetadataNumberBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointMetadataNumber>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendConnectedBlock(this.block_a)
            .appendField("get metadata")
            .appendField(new Blockly.FieldTextInput(""), field_key)
            .appendField("as number from control point");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_controlpoint_metadata_string",
              model       : Models.AlertEngineOperatorUnaryControlPointMetadataString,
              outputType  : Models.EngineValuePrimitiveString
          })
export class ControlPointGetMetadataStringBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointMetadataString>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendConnectedBlock(this.block_a)
            .appendField("get metadata")
            .appendField(new Blockly.FieldTextInput(""), field_key)
            .appendField("as string from control point");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_controlpoint_metadata_datetime",
              model       : Models.AlertEngineOperatorUnaryControlPointMetadataTimestamp,
              outputType  : Models.EngineValueDateTime
          })
export class ControlPointGetMetadataTimestampBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryControlPointMetadataTimestamp>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendConnectedBlock(this.block_a)
            .appendField("get metadata")
            .appendField(new Blockly.FieldTextInput(""), field_key)
            .appendField("as timestamp from control point");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "get_controlpoint_timezone_from_location",
              model       : Models.AlertEngineOperatorUnaryGetTimeZoneFromLocation,
              outputType  : Models.EngineValueTimeZone
          })
export class GetTimeZoneFromLocationBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryGetTimeZoneFromLocation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueControlPoint);

        this.appendConnectedBlock(this.block_a)
            .appendField("get Time Zone from location of");
    }
}
