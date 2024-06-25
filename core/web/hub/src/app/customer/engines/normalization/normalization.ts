import {NormalizationRulesBlocklyWorkspaceData} from "app/customer/configuration/classification/normalization-rules-blockly-workspace.data";
import {SubnetTableComponent} from "app/customer/engines/normalization/subnet-table.component";
import * as Base from "app/customer/engines/shared/base";
import {EquipmentClassAssignmentTableComponent} from "app/customer/engines/shared/equipment-class-assignment-table.component";
import {PointClassAssignmentTableComponent} from "app/customer/engines/shared/point-class-assignment-table.component";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";
import * as Models from "app/services/proxy/model/models";
import * as Blockly from "blockly";
import {UtilsService} from "framework/services/utils.service";

import {Block, BlockDef} from "framework/ui/blockly/block";
import {ModalDialogField} from "framework/ui/blockly/modal-dialog-field";
import {SelectField} from "framework/ui/blockly/select-field";
import {ControlOption} from "framework/ui/control-option";

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_input_value",
              model       : Models.NormalizationEngineExpressionGetInputValue,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetInputValueBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetInputValue>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get input value");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_output_value",
              model       : Models.NormalizationEngineStatementSetOutputValue
          })
export class SetOutputValueBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetOutputValue>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_value)
            .appendField("set control point name to");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetOutputValue): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

//--//

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_identifier",
              model       : Models.NormalizationEngineExpressionGetControlPointIdentifier,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointIdentifierBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointIdentifier>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point identifier");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_description",
              model       : Models.NormalizationEngineExpressionGetControlPointDescription,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointDescriptionBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointDescription>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point description");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_name",
              model       : Models.NormalizationEngineExpressionGetControlPointName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_name_raw",
              model       : Models.NormalizationEngineExpressionGetControlPointNameRaw,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointNameRawBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointNameRaw>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get raw control point name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_name_override",
              model       : Models.NormalizationEngineExpressionGetControlPointOverrideName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointOverrideNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointOverrideName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point name override");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_location",
              model       : Models.NormalizationEngineExpressionGetControlPointLocation,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointLocationBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointLocation>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point location");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_units",
              model       : Models.NormalizationEngineExpressionGetControlPointUnits,
              outputType  : Models.EngineValueEngineeringUnits
          })
export class GetControlPointUnitsBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointUnits>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point units");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_units_string",
              model       : Models.NormalizationEngineExpressionGetControlPointUnitsString,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointUnitsStringBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointUnitsString>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point units as string");
    }
}

//--//

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_identifier",
              model       : Models.NormalizationEngineExpressionGetControllerIdentifier,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerIdentifierBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerIdentifier>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller identifier");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_description",
              model       : Models.NormalizationEngineExpressionGetControllerDescription,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerDescriptionBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerDescription>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller description");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_backupName",
              model       : Models.NormalizationEngineExpressionGetControllerBackupName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerBackupNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerBackupName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller backup name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_name",
              model       : Models.NormalizationEngineExpressionGetControllerName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_model",
              model       : Models.NormalizationEngineExpressionGetControllerModel,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerModelBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerModel>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller model name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_vendor",
              model       : Models.NormalizationEngineExpressionGetControllerVendor,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerVendorBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerVendor>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller vendor name");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_location",
              model       : Models.NormalizationEngineExpressionGetControllerLocation,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerLocationBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerLocation>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controller location");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_dashboard_name",
              model       : Models.NormalizationEngineExpressionGetDashboardName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetDashboardNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetDashboardName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get name from legacy import");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_dashboard_equipment_name",
              model       : Models.NormalizationEngineExpressionGetDashboardEquipmentName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetDashboardEquipmentNameBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetDashboardEquipmentName>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get equipment from legacy import");
    }
}

//--//

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_equipment",
              model       : Models.NormalizationEngineExpressionGetEquipment,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetEquipmentBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetEquipment>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get equipment");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_equipment_class",
              model       : Models.NormalizationEngineExpressionGetEquipmentClass,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetEquipmentClassBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetEquipmentClass>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get equipment class");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_equipment_class_id",
              model       : Models.NormalizationEngineOperatorUnaryGetEquipmentClassId,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetEquipmentClassIdBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetEquipmentClassId>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.NormalizationEngineValueEquipment);

        this.appendConnectedBlock(this.block_a)
            .appendField("get equipment class id of");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_equipment_class_name",
              model       : Models.NormalizationEngineOperatorUnaryGetEquipmentClassName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetEquipmentClassNameBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetEquipmentClassName>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("get equipment class name of");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__literal_equipment_class",
              model       : Models.NormalizationEngineLiteralEquipmentClass,
              outputType  : Models.EngineValuePrimitiveString
          })
export class LiteralEquipmentClassBlock extends Base.ExpressionBlock<Models.NormalizationEngineLiteralEquipmentClass>
{
    protected initFields(): void
    {
        super.initFields();

        let equipClassField = new SelectField(this, "Select Equipment Class", () => this.getDropdownOptions());

        this.registerModalField(equipClassField,
                                (model,
                                 value) => model.value = value,
                                (model) => model.value);

        this.appendDummyInput()
            .appendField(equipClassField.field);
    }

    protected initForToolbox(model: Models.NormalizationEngineLiteralEquipmentClass): void
    {
        model.value = "1";
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <NormalizationRulesBlocklyWorkspaceData>component.data;
        return data.getEquipmentClassDropdownOptions();
    }
}


@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_equipment",
              model       : Models.NormalizationEngineStatementSetEquipment
          })
export class SetEquipmentBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetEquipment>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_value)
            .appendField("set equipment as");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetEquipment): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_tags",
              model       : Models.NormalizationEngineStatementSetTags
          })
export class TagsSetBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetTags>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValueList);

        this.appendConnectedBlock(block_value)
            .appendField("set tags to");
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_tags",
              model            : Models.NormalizationEngineExpressionGetTags,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString

          })
export class TagsGetBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetTags>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get tags");
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__tokenize_string",
              model            : Models.NormalizationEngineOperatorUnaryTokenizeString,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString
          })
export class TokenizeStringBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryTokenizeString>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("tokenize string");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__clear_equipment",
              model       : Models.NormalizationEngineStatementClearEquipment
          })
export class ClearEquipmentBlock extends Base.StatementBlock<Models.NormalizationEngineStatementClearEquipment>
{
    protected initFields(): void
    {
        this.appendDummyInput()
            .appendField("clear all equipment");
    }
}

//--//

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_imported_structure",
              model            : Models.NormalizationEngineExpressionGetImportedStructure,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString

          })
export class GetImportedStructureBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetImportedStructure>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get point structure from legacy import");
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_location",
              model            : Models.NormalizationEngineExpressionGetLocation,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString

          })
export class GetLocationBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetLocation>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get location");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__push_location",
              model       : Models.NormalizationEngineStatementPushLocation
          })
export class PushLocationBlock extends Base.StatementBlock<Models.NormalizationEngineStatementPushLocation>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        let field_locationType = this.registerStringField("locationType",
                                                          (model,
                                                           value) => model.type = UtilsService.getEnumValue(Models.LocationType, value),
                                                          (model) => model.type || Models.LocationType.OTHER);

        this.appendConnectedBlock(block_value)
            .appendField("push location with type")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.LocationType, false)), field_locationType)
            .appendField("and name");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementPushLocation): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}


@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__push_equipment_location",
              model       : Models.NormalizationEngineStatementPushEquipmentLocation
          })
export class PushEquipmentLocationBlock extends Base.StatementBlock<Models.NormalizationEngineStatementPushEquipmentLocation>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        let block_equipment = this.registerConnectedBlock<Models.EngineExpression>("equipment",
                                                                                   false,
                                                                                   (model,
                                                                                    value) => model.equipment = value,
                                                                                   (model) => model.equipment,
                                                                                   Models.NormalizationEngineValueEquipment);

        let field_locationType = this.registerStringField("locationType",
                                                          (model,
                                                           value) => model.type = UtilsService.getEnumValue(Models.LocationType, value),
                                                          (model) => model.type || Models.LocationType.OTHER);

        this.appendConnectedBlock(block_equipment)
            .appendField("push location for");

        this.appendConnectedBlock(block_value)
            .appendField("with type")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.LocationType, false)), field_locationType)
            .appendField("and name");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementPushEquipmentLocation): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_metadata",
              model       : Models.NormalizationEngineStatementSetMetadata
          })
export class SetMetadataBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetMetadata>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString, Models.EngineValuePrimitiveNumber);

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendConnectedBlock(block_value)
            .appendField("set metadata")
            .appendField(new Blockly.FieldTextInput(), field_key)
            .appendField("to");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetMetadata): void
    {
        model.key   = "";
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_metadata_string",
              model       : Models.NormalizationEngineExpressionGetMetadataString,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetMetadataStringBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetMetadataString>
{
    protected initFields(): void
    {
        super.initFields();

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendDummyInput()
            .appendField("get metadata")
            .appendField(new Blockly.FieldTextInput(), field_key)
            .appendField("as string");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_metadata_number",
              model       : Models.NormalizationEngineExpressionGetMetadataNumber,
              outputType  : Models.EngineValuePrimitiveNumber
          })
export class GetMetadataNumberBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetMetadataNumber>
{
    protected initFields(): void
    {
        super.initFields();

        let field_key = this.registerStringField("key",
                                                 (model,
                                                  value) => model.key = value,
                                                 (model) => model.key);

        this.appendDummyInput()
            .appendField("get metadata")
            .appendField(new Blockly.FieldTextInput(), field_key)
            .appendField("as number");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controlpoint_class",
              model       : Models.NormalizationEngineExpressionGetControlPointClass,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControlPointClassBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointClass>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get control point class");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_point_class_name",
              model       : Models.NormalizationEngineOperatorUnaryGetPointClassName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetPointClassNameBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetPointClassName>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("get point class name of");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_point_class_description",
              model       : Models.NormalizationEngineOperatorUnaryGetPointClassDescription,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetPointClassDescriptionBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetPointClassDescription>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("get point class description of");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__normalize",
              model       : Models.NormalizationEngineOperatorUnaryNormalize,
              outputType  : Models.EngineValuePrimitiveString
          })
export class NormalizeTextBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryNormalize>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(this.block_a)
            .appendField("normalize");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_samplingperiod",
              model       : Models.NormalizationEngineStatementSetSamplingPeriod
          })
export class SetSamplingPeriodBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetSamplingPeriod>
{
    protected initFields(): void
    {
        let field_sampling = this.registerIntegerField("samplingPeriod",
                                                       (model,
                                                        value) => model.samplingPeriod = value,
                                                       (model) => model.samplingPeriod);
        this.appendDummyInput()
            .appendField("set sampling period to")
            .appendField(new Blockly.FieldNumber(0, 0), field_sampling);
        this.setWarningText("Set 0 to disable sampling");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetSamplingPeriod): void
    {
        model.samplingPeriod = 900;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_pointclass",
              model       : Models.NormalizationEngineStatementSetPointClass
          })
export class SetPointClassBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetPointClass>
{
    protected initFields(): void
    {
        let pointClassField = new SelectField(this, "Select point class", () => this.getDropdownOptions());
        this.registerModalField(pointClassField,
                                (model,
                                 value) => model.pointClassId = value !== "unclassified" ? value : null,
                                (model) => model.pointClassId);

        this.appendDummyInput()
            .appendField("set point class to")
            .appendField(pointClassField.field);
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetPointClass): void
    {
        model.pointClassId = "1";
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component    = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data         = <NormalizationRulesBlocklyWorkspaceData>component.data;
        let pointClasses = data && data.rules.pointClasses || [];

        let options: ControlOption<string>[] = [];
        for (let pc of pointClasses)
        {
            options.push(new ControlOption<string>(`${pc.id}`, `${pc.pointClassName} - ${pc.pointClassDescription}`));
        }

        options.sort((a,
                      b) => UtilsService.compareStrings(a.label, b.label, true));

        options.unshift(new ControlOption<string>("unclassified", "Unclassified"));

        return options;
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_engineeringUnits",
              model       : Models.NormalizationEngineStatementSetEngineeringUnits
          })
export class SetEngineeringUnitsBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetEngineeringUnits>
{
    protected initFields(): void
    {
        let units       = UtilsService.getEnumValues<string>(Models.EngineeringUnits)
                                      .map((v) => new ControlOption<string>(v, v));
        let field_units = new SelectField(this, "Select units", () => units);

        this.registerModalField(field_units, (model,
                                              value) => model.units = UtilsService.getEnumValue(Models.EngineeringUnits, value) || Models.EngineeringUnits.no_units,
                                (model) => model.units || Models.EngineeringUnits.no_units);
        this.appendDummyInput()
            .appendField("set units to")
            .appendField(field_units.field);
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetEngineeringUnits): void
    {
        model.units = Models.EngineeringUnits.degrees_fahrenheit;
        super.initForToolbox(model);
    }
}

export class PointClassAssignmentTableField extends ModalDialogField<PointClassAssignmentTableComponent, typeof PointClassAssignmentTableComponent, Models.PointClassAssignment[]>
{
    private m_assignments: Models.PointClassAssignment[];

    constructor(block: Block<any>)
    {
        super(block, PointClassAssignmentTableComponent, "Setup Point Class Assignments");
    }

    protected initData(data: Models.PointClassAssignment[]): void
    {
        if (data)
        {
            this.m_assignments = data;
        }
        else if (!this.m_assignments)
        {
            this.m_assignments = [];
        }
    }

    protected initComponent(component: PointClassAssignmentTableComponent): void
    {
        let workspace   = this.m_block.getComponent<AppBlocklyWorkspaceComponent>();
        let data        = <NormalizationRulesBlocklyWorkspaceData>workspace.data;
        component.rules = data && data.rules;
        component.data  = this.m_assignments;
    }

    public getModel(): Models.PointClassAssignment[]
    {
        return this.m_assignments.map((r) => Models.PointClassAssignment.newInstance(r));
    }

    protected getText(): string
    {
        return `${this.m_assignments.length} point class assignments`;
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_set_point_class_from_term_scoring",
              model       : Models.NormalizationEngineStatementSetPointClassFromTermScoring
          })
export class SetPointClassFromTermScoringBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetPointClassFromTermScoring>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_value)
            .appendField("classify based on term scoring");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_set_point_class_table",
              model       : Models.NormalizationEngineStatementSetPointClassTable
          })
export class SetPointClassTableBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetPointClassTable>
{
    private m_pointClassAssignmentField: PointClassAssignmentTableField;

    protected initFields(): void
    {
        this.m_pointClassAssignmentField = new PointClassAssignmentTableField(this);

        this.registerModalField(this.m_pointClassAssignmentField,
                                (model,
                                 value) => model.assignments = value,
                                (model) => model.assignments);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);


        this.appendConnectedBlock(block_value)
            .appendField("run")
            .appendField(this.m_pointClassAssignmentField.field)
            .appendField("on");
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementSetPointClassTable): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

export class EquipmentClassAssignmentTableField extends ModalDialogField<EquipmentClassAssignmentTableComponent, typeof EquipmentClassAssignmentTableComponent, Models.EquipmentClassAssignment[]>
{
    private m_assignments: Models.EquipmentClassAssignment[];

    constructor(block: Block<any>)
    {
        super(block, EquipmentClassAssignmentTableComponent, "Setup Equipment Class Assignments");
    }

    protected initData(data: Models.EquipmentClassAssignment[]): void
    {
        if (data)
        {
            this.m_assignments = data;
        }
        else if (!this.m_assignments)
        {
            this.m_assignments = [];
        }
    }

    protected initComponent(component: EquipmentClassAssignmentTableComponent): void
    {
        let workspace   = this.m_block.getComponent<AppBlocklyWorkspaceComponent>();
        let data        = <NormalizationRulesBlocklyWorkspaceData>workspace.data;
        component.rules = data && data.rules;
        component.data  = this.m_assignments;
    }

    public getModel(): Models.EquipmentClassAssignment[]
    {
        return this.m_assignments.map((r) => Models.EquipmentClassAssignment.newInstance(r));
    }

    protected getText(): string
    {
        return `${this.m_assignments.length} equipment class assignments`;
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__set_equipment_and_classhint",
              model       : Models.NormalizationEngineStatementSetEquipmentAndClassHint
          })
export class SetEquipmentAndClassHint extends Base.StatementBlock<Models.NormalizationEngineStatementSetEquipmentAndClassHint>
{
    protected initFields(): void
    {
        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);
        this.appendConnectedBlock(block_value)
            .appendField("set equipment as");


        let block_hint = this.registerConnectedBlock<Models.EngineExpression>("hint",
                                                                              true,
                                                                              (model,
                                                                               value) => model.hint = value,
                                                                              (model) => model.hint,
                                                                              Models.EngineValuePrimitiveString);
        this.appendConnectedBlock(block_hint)
            .appendField("and class hint as");
    }


    protected initForToolbox(model: Models.NormalizationEngineStatementSetEquipmentAndClassHint): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        model.hint  = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "o3_set_equipment_class_table",
              model       : Models.NormalizationEngineStatementSetEquipmentClassTable
          })
export class SetEquipmentClassTableBlock extends Base.StatementBlock<Models.NormalizationEngineStatementSetEquipmentClassTable>
{
    private m_equipmentClassAssignmentField: EquipmentClassAssignmentTableField;

    protected initFields(): void
    {
        this.m_equipmentClassAssignmentField = new EquipmentClassAssignmentTableField(this);

        this.registerModalField(this.m_equipmentClassAssignmentField,
                                (model,
                                 value) => model.assignments = value,
                                (model) => model.assignments);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               false,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.NormalizationEngineValueEquipment);


        this.appendConnectedBlock(block_value)
            .appendField("run")
            .appendField(this.m_equipmentClassAssignmentField.field)
            .appendField("on");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__create_equipment",
              model       : Models.NormalizationEngineExpressionCreateEquipment,
              outputType  : Models.NormalizationEngineValueEquipment
          })
export class CreateEquipmentBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionCreateEquipment>
{
    protected initFields(): void
    {
        super.initFields();

        let equipClassField = new SelectField(this, "Select Equipment Class", () => this.getDropdownOptions());
        this.registerModalField(equipClassField,
                                (model,
                                 value) => model.equipmentClassId = value,
                                (model) => model.equipmentClassId);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);
        this.appendConnectedBlock(block_value)
            .appendField("create equipment as");

        this.appendDummyInput()
            .appendField("with class")
            .appendField(equipClassField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineExpressionCreateEquipment): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <NormalizationRulesBlocklyWorkspaceData>component.data;
        return data.getEquipmentClassDropdownOptions();
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__create_child_equipment",
              model       : Models.NormalizationEngineExpressionCreateChildEquipment,
              outputType  : Models.NormalizationEngineValueEquipment
          })
export class CreateChildEquipmentBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionCreateChildEquipment>
{
    protected initFields(): void
    {
        super.initFields();

        let equipClassField = new SelectField(this, "Select Equipment Class", () => this.getDropdownOptions());
        this.registerModalField(equipClassField,
                                (model,
                                 value) => model.equipmentClassId = value,
                                (model) => model.equipmentClassId);

        let block_name = this.registerConnectedBlock<Models.EngineExpression>("name",
                                                                              true,
                                                                              (model,
                                                                               value) => model.name = value,
                                                                              (model) => model.name,
                                                                              Models.EngineValuePrimitiveString);

        let block_parent = this.registerConnectedBlock<Models.EngineExpression>("parent",
                                                                                false,
                                                                                (model,
                                                                                 value) => model.parent = value,
                                                                                (model) => model.parent,
                                                                                Models.NormalizationEngineValueEquipment);
        this.appendConnectedBlock(block_name)
            .appendField("create child equipment as");

        this.appendConnectedBlock(block_parent)
            .appendField("for parent");

        this.appendDummyInput()
            .appendField("with class")
            .appendField(equipClassField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineExpressionCreateChildEquipment): void
    {
        model.name = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <NormalizationRulesBlocklyWorkspaceData>component.data;
        return data.getEquipmentClassDropdownOptions();
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__push_equipment_expression",
              model       : Models.NormalizationEngineExpressionPushEquipment,
              outputType  : Models.NormalizationEngineValueEquipment
          })
export class PushEquipmentExpressionBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionPushEquipment>
{
    protected initFields(): void
    {
        super.initFields();

        let equipClassField = new SelectField(this, "Select Equipment Class", () => this.getDropdownOptions());
        this.registerModalField(equipClassField,
                                (model,
                                 value) => model.equipmentClassId = value,
                                (model) => model.equipmentClassId);

        let block_name = this.registerConnectedBlock<Models.EngineExpression>("name",
                                                                              true,
                                                                              (model,
                                                                               value) => model.value = value,
                                                                              (model) => model.value,
                                                                              Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_name)
            .appendField("push equipment as");

        this.appendDummyInput()
            .appendField("with class")
            .appendField(equipClassField.field);
        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineExpressionPushEquipment): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <NormalizationRulesBlocklyWorkspaceData>component.data;
        return data.getEquipmentClassDropdownOptions();
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__push_equipment_table_expression",
              model       : Models.NormalizationEngineExpressionPushEquipmentTable,
              outputType  : Models.NormalizationEngineValueEquipment
          })
export class PushEquipmentTableExpressionBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionPushEquipmentTable>
{
    private m_equipmentClassAssignmentField: EquipmentClassAssignmentTableField;

    protected initFields(): void
    {
        super.initFields();

        this.m_equipmentClassAssignmentField = new EquipmentClassAssignmentTableField(this);

        this.registerModalField(this.m_equipmentClassAssignmentField,
                                (model,
                                 value) => model.assignments = value,
                                (model) => model.assignments);


        let block_name = this.registerConnectedBlock<Models.EngineExpression>("name",
                                                                              true,
                                                                              (model,
                                                                               value) => model.value = value,
                                                                              (model) => model.value,
                                                                              Models.EngineValuePrimitiveString);

        this.appendConnectedBlock(block_name)
            .appendField("push equipment as");

        this.appendDummyInput()
            .appendField("if matching")
            .appendField(this.m_equipmentClassAssignmentField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineExpressionPushEquipmentTable): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__push_equipment_and_class",
              model       : Models.NormalizationEngineStatementPushEquipmentWithClass
          })
export class PushEquipmentAndClassBlock extends Base.StatementBlock<Models.NormalizationEngineStatementPushEquipmentWithClass>
{
    protected initFields(): void
    {
        let equipClassField = new SelectField(this, "Select Equipment Class", () => this.getDropdownOptions());
        this.registerModalField(equipClassField,
                                (model,
                                 value) => model.equipmentClassId = value,
                                (model) => model.equipmentClassId);

        let block_value = this.registerConnectedBlock<Models.EngineExpression>("value",
                                                                               true,
                                                                               (model,
                                                                                value) => model.value = value,
                                                                               (model) => model.value,
                                                                               Models.EngineValuePrimitiveString);
        this.appendConnectedBlock(block_value)
            .appendField("push equipment as");

        this.appendDummyInput()
            .appendField("with class")
            .appendField(equipClassField.field);
        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineStatementPushEquipmentWithClass): void
    {
        model.value = Models.EngineLiteralString.newInstance({value: ""});
        super.initForToolbox(model);
    }

    private getDropdownOptions(): ControlOption<string>[]
    {
        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <NormalizationRulesBlocklyWorkspaceData>component.data;
        return data.getEquipmentClassDropdownOptions();
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_equipments",
              model            : Models.NormalizationEngineExpressionGetEquipments,
              outputType       : Models.EngineValueList,
              outputElementType: Models.EngineValuePrimitiveString

          })
export class GetEquipmentsBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetEquipments>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get equipments");
    }
}

export class SubnetTableField extends ModalDialogField<SubnetTableComponent, typeof SubnetTableComponent, Models.FilteredSubnet[]>
{
    private m_subnets: Models.FilteredSubnet[];

    constructor(block: Block<any>)
    {
        super(block, SubnetTableComponent, "Setup Subnets");
    }

    protected initData(data: Models.FilteredSubnet[]): void
    {
        if (data)
        {
            this.m_subnets = data;
        }
        else if (!this.m_subnets)
        {
            this.m_subnets = [];
        }
    }

    protected initComponent(component: SubnetTableComponent): void
    {
        component.data = this.m_subnets;
    }

    public getModel(): Models.FilteredSubnet[]
    {
        return this.m_subnets.map((r) => Models.FilteredSubnet.newInstance(r));
    }

    protected getText(): string
    {
        return `${this.m_subnets.length} subnets`;
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_in_subnet",
              model       : Models.NormalizationEngineExpressionGetControllerInSubnet,
              outputType  : Models.EngineValuePrimitiveBoolean
          })
export class GetControllerInSubnetBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControllerInSubnet>
{
    private m_subnetField: SubnetTableField;

    protected initFields(): void
    {
        super.initFields();

        this.m_subnetField = new SubnetTableField(this);

        this.registerModalField(this.m_subnetField,
                                (model,
                                 value) => model.subnets = value,
                                (model) => model.subnets);


        this.appendDummyInput()
            .appendField("controller in subnet")
            .appendField(this.m_subnetField.field);

        this.setInputsInline(false);
    }

    protected initForToolbox(model: Models.NormalizationEngineExpressionGetControllerInSubnet): void
    {
        model.subnets = [];
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_controllers",
              model            : Models.NormalizationEngineExpressionGetControllers,
              outputType       : Models.EngineValueList,
              outputElementType: Models.NormalizationEngineValueController
          })
export class GetControllersBlock extends Base.ExpressionBlock<Models.NormalizationEngineExpressionGetControlPointIdentifier>
{
    protected initFields(): void
    {
        super.initFields();

        this.appendDummyInput()
            .appendField("get controllers");
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_controller_property",
              model       : Models.NormalizationEngineOperatorUnaryGetControllerProperty,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetControllerPropertyBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetControllerProperty>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.NormalizationEngineValueController);

        let field_propertyType = this.registerStringField("propertyType",
                                                          (model,
                                                           value) => model.property = UtilsService.getEnumValue(Models.NormalizationEngineValueControllerPropertyType, value),
                                                          (model) => model.property || Models.NormalizationEngineValueControllerPropertyType.Name);

        this.appendDummyInput()
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.NormalizationEngineValueControllerPropertyType, false)), field_propertyType);

        this.appendConnectedBlock(this.block_a)
            .appendField("of controller");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.NormalizationEngineOperatorUnaryGetControllerProperty): void
    {
        model.property = Models.NormalizationEngineValueControllerPropertyType.Name;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext     : "NormalizationRules",
              blockName        : "normalization__get_controller_points",
              model            : Models.NormalizationEngineOperatorUnaryGetControllerPoints,
              outputType       : Models.EngineValueList,
              outputElementType: Models.NormalizationEngineValuePoint
          })
export class GetControllerPointsBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetControllerPoints>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.NormalizationEngineValueController);

        this.appendConnectedBlock(this.block_a)
            .appendField("get points of");

        this.setInputsInline(true);
    }
}

@BlockDef({
              blockContext: "NormalizationRules",
              blockName   : "normalization__get_point_property",
              model       : Models.NormalizationEngineOperatorUnaryGetPointProperty,
              outputType  : Models.EngineValuePrimitiveString
          })
export class GetPointPropertyBlock extends Base.UnaryOperatorBlock<Models.NormalizationEngineOperatorUnaryGetPointProperty>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.NormalizationEngineValuePoint);

        let field_propertyType = this.registerStringField("propertyType",
                                                          (model,
                                                           value) => model.property = UtilsService.getEnumValue(Models.NormalizationEngineValuePointPropertyType, value),
                                                          (model) => model.property || Models.NormalizationEngineValuePointPropertyType.Name);

        this.appendDummyInput()
            .appendField("get")
            .appendField(new Blockly.FieldDropdown(this.getDropdownFromEnum(Models.NormalizationEngineValuePointPropertyType, false)), field_propertyType);

        this.appendConnectedBlock(this.block_a)
            .appendField("of point");

        this.setInputsInline(true);
    }

    protected initForToolbox(model: Models.NormalizationEngineOperatorUnaryGetPointProperty): void
    {
        model.property = Models.NormalizationEngineValuePointPropertyType.Name;
        super.initForToolbox(model);
    }
}
