import {AlertRuleBlocklyWorkspaceData, AlertRuleConfigurationValues} from "app/customer/configuration/alert-rules/wizard/alert-rule-blockly-workspace-data";

import * as Base from "app/customer/engines/shared/base";
import {AppBlocklyWorkspaceComponent} from "app/customer/engines/shared/workspace.component";

import * as Models from "app/services/proxy/model/models";

import * as Blockly from "blockly";

import {UtilsService} from "framework/services/utils.service";
import {BlockDef} from "framework/ui/blockly/block";

interface ModelWithRelationship
{
    relation: Models.AssetRelationship;

    fromChild: boolean;
}

function setRelationship(model: ModelWithRelationship,
                         value: string)
{
    if (value.startsWith("!"))
    {
        value           = value.substring(1);
        model.fromChild = true;
    }
    else
    {
        model.fromChild = false;
    }

    model.relation = UtilsService.getEnumValue(Models.AssetRelationship, value);
}

function getRelationship(relation: Models.AssetRelationship,
                         fromChild: boolean): string
{
    return (fromChild ? "!" : "") + relation;
}

function getRelationships(): string[][]
{
    let names = UtilsService.getEnumNames(Models.AssetRelationship);

    names.sort((a,
                b) => UtilsService.compareStrings(a, b, true));

    let result = [];

    for (let name of names)
    {
        result.push([
                        name,
                        getRelationship(UtilsService.getEnumValue(Models.AssetRelationship, name), false)
                    ]);
    }

    for (let name of names)
    {
        result.push([
                        `${name} from child`,
                        getRelationship(UtilsService.getEnumValue(Models.AssetRelationship, name), true)
                    ]);
    }

    return result;
}

//--//

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_graph_node",
              model       : Models.AlertEngineOperatorUnaryAssetGraphNode,
              outputType  : Models.AlertEngineValueAsset
          })
export class AlertGraphNodeBlock extends Base.ExpressionBlock<Models.AlertEngineOperatorUnaryAssetGraphNode>
{
    protected initFields(): void
    {
        super.initFields();

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        function getFunctions(): string[][]
        {
            return data.resolveNodes();
        }

        let field_nodeId = this.registerStringField("nodeId",
                                                    (model,
                                                     value) => model.nodeId = value,
                                                    (model) => model.nodeId);

        this.appendDummyInput()
            .appendField("asset from graph node")
            .appendField(new Blockly.FieldDropdown(getFunctions), field_nodeId);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "asset_graph_nodes",
              model            : Models.AlertEngineOperatorUnaryAssetGraphNodes,
              outputType       : Models.AlertEngineValueAssets,
              outputElementType: Models.AlertEngineValueAsset
          })
export class AlertGraphNodesBlock extends Base.ExpressionBlock<Models.AlertEngineOperatorUnaryAssetGraphNodes>
{
    protected initFields(): void
    {
        super.initFields();

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        function getFunctions(): string[][]
        {
            return data.resolveNodes();
        }

        let field_nodeId = this.registerStringField("nodeId",
                                                    (model,
                                                     value) => model.nodeId = value,
                                                    (model) => model.nodeId);

        this.appendDummyInput()
            .appendField("assets from graph node")
            .appendField(new Blockly.FieldDropdown(getFunctions), field_nodeId);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_equipment_class_literal",
              model       : Models.AlertEngineLiteralAssetQueryEquipmentClass,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class LiteralAssetQueryEquipmentClassBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralAssetQueryEquipmentClass>
{
    protected initFields(): void
    {
        super.initFields();

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_equipmentClass = this.registerStringField("equipmentClass",
                                                            (model,
                                                             value) => model.equipmentClass = value,
                                                            (model) => model.equipmentClass);

        this.appendDummyInput()
            .appendField("with equipment class")
            .appendField(AlertRuleConfigurationValues.toDropdown(data.configValues.equipmentClasses), field_equipmentClass);

    }

    protected initForToolbox(model: Models.AlertEngineLiteralAssetQueryEquipmentClass): void
    {
        model.equipmentClass = null;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_point_class_literal",
              model       : Models.AlertEngineLiteralAssetQueryPointClass,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class LiteralAssetQueryPointClassBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralAssetQueryPointClass>
{
    protected initFields(): void
    {
        super.initFields();

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_pointClass = this.registerStringField("pointClass",
                                                        (model,
                                                         value) => model.pointClass = value,
                                                        (model) => model.pointClass);

        this.appendDummyInput()
            .appendField("with point class")
            .appendField(AlertRuleConfigurationValues.toDropdown(data.configValues.pointClasses), field_pointClass);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralAssetQueryPointClass): void
    {
        model.pointClass = null;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_tag_literal",
              model       : Models.AlertEngineLiteralAssetQueryTag,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class LiteralAssetQueryTagBlock extends Base.ExpressionBlock<Models.AlertEngineLiteralAssetQueryTag>
{
    protected initFields(): void
    {
        super.initFields();

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_tag = this.registerStringField("tag",
                                                 (model,
                                                  value) => model.tag = value,
                                                 (model) => model.tag);

        this.appendDummyInput()
            .appendField("with tag")
            .appendField(AlertRuleConfigurationValues.toDropdown(data.configValues.tags), field_tag);
    }

    protected initForToolbox(model: Models.AlertEngineLiteralAssetQueryTag): void
    {
        model.tag = null;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_not",
              model       : Models.AlertEngineOperatorUnaryAssetQueryNot,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class AssetQueryNotBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAssetQueryNot>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAssetQueryCondition);

        this.appendConnectedBlock(this.block_a)
            .appendField("not");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_and",
              model       : Models.AlertEngineOperatorBinaryAssetQueryAnd,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class AssetQueryAndBlock extends Base.BinaryOperatorBlock<Models.AlertEngineOperatorBinaryAssetQueryAnd>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAssetQueryCondition, Models.AlertEngineValueAssetQueryCondition);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("and");

        this.appendConnectedBlock(this.block_b);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_or",
              model       : Models.AlertEngineOperatorBinaryAssetQueryOr,
              outputType  : Models.AlertEngineValueAssetQueryCondition
          })
export class AssetQueryOrBlock extends Base.BinaryOperatorBlock<Models.AlertEngineOperatorBinaryAssetQueryOr>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAssetQueryCondition, Models.AlertEngineValueAssetQueryCondition);

        this.appendConnectedBlock(this.block_a);

        this.appendDummyInput()
            .appendField("or");

        this.appendConnectedBlock(this.block_b);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "asset_query",
              model            : Models.AlertEngineOperatorUnaryAssetQueryExec,
              outputType       : Models.AlertEngineValueAssets,
              outputElementType: Models.AlertEngineValueAsset
          })
export class AssetQueryExecBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAssetQueryExec>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAssetQueryCondition);

        this.appendConnectedBlock(this.block_a)
            .appendField("assets that satisfy query");
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "asset_query_relations",
              model            : Models.AlertEngineOperatorUnaryAssetQueryRelation,
              outputType       : Models.AlertEngineValueAssets,
              outputElementType: Models.AlertEngineValueAsset
          })
export class AssetQueryRelationsBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAssetQueryRelation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_relation = this.registerStringField("relation",
                                                      (model,
                                                       value) => setRelationship(model, value),
                                                      (model) => getRelationship(model.relation, model.fromChild));

        this.appendConnectedBlock(this.block_a)
            .appendField("assets that have relation")
            .appendField(new Blockly.FieldDropdown(getRelationships()), field_relation)
            .appendField("with asset");
    }

    protected initForToolbox(model: Models.AlertEngineOperatorBinaryAssetQueryRelation)
    {
        model.relation = Models.AssetRelationship.controls;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext     : "AlertRules",
              blockName        : "asset_query_relations2",
              model            : Models.AlertEngineOperatorBinaryAssetQueryRelation,
              outputType       : Models.AlertEngineValueAssets,
              outputElementType: Models.AlertEngineValueAsset
          })
export class AssetQueryRelations2Block extends Base.BinaryOperatorBlock<Models.AlertEngineOperatorBinaryAssetQueryRelation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset, Models.AlertEngineValueAssetQueryCondition);

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_relation = this.registerStringField("relation",
                                                      (model,
                                                       value) => setRelationship(model, value),
                                                      (model) => getRelationship(model.relation, model.fromChild));

        this.appendConnectedBlock(this.block_a)
            .appendField("assets that have relation")
            .appendField(new Blockly.FieldDropdown(getRelationships()), field_relation)
            .appendField("with asset");

        this.appendDummyInput()
            .appendField("and satisfy query");

        this.appendConnectedBlock(this.block_b);
    }

    protected initForToolbox(model: Models.AlertEngineOperatorBinaryAssetQueryRelation)
    {
        model.relation = Models.AssetRelationship.controls;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_relations_single",
              model       : Models.AlertEngineOperatorUnaryAssetQueryRelationSingle,
              outputType  : Models.AlertEngineValueAsset
          })
export class AssetQueryRelationsSingleBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAssetQueryRelationSingle>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_relation = this.registerStringField("relation",
                                                      (model,
                                                       value) => setRelationship(model, value),
                                                      (model) => getRelationship(model.relation, model.fromChild));

        this.appendConnectedBlock(this.block_a)
            .appendField("single asset that has relation")
            .appendField(new Blockly.FieldDropdown(getRelationships()), field_relation)
            .appendField("with asset");
    }

    protected initForToolbox(model: Models.AlertEngineOperatorBinaryAssetQueryRelation)
    {
        model.relation = Models.AssetRelationship.controls;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_query_relations2_single",
              model       : Models.AlertEngineOperatorBinaryAssetQueryRelationSingle,
              outputType  : Models.AlertEngineValueAsset
          })
export class AssetQueryRelations2SingleBlock extends Base.BinaryOperatorBlock<Models.AlertEngineOperatorBinaryAssetQueryRelation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset, Models.AlertEngineValueAssetQueryCondition);

        let component = this.getComponent<AppBlocklyWorkspaceComponent>();
        let data      = <AlertRuleBlocklyWorkspaceData>component.data;

        let field_relation = this.registerStringField("relation",
                                                      (model,
                                                       value) => setRelationship(model, value),
                                                      (model) => getRelationship(model.relation, model.fromChild));

        this.appendConnectedBlock(this.block_a)
            .appendField("single asset that has relation")
            .appendField(new Blockly.FieldDropdown(getRelationships()), field_relation)
            .appendField("with asset");

        this.appendDummyInput()
            .appendField("and satisfies query");

        this.appendConnectedBlock(this.block_b);
    }

    protected initForToolbox(model: Models.AlertEngineOperatorBinaryAssetQueryRelation)
    {
        model.relation = Models.AssetRelationship.controls;
        super.initForToolbox(model);
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_as_controlpoint",
              model       : Models.AlertEngineOperatorUnaryAsControlPoint,
              outputType  : Models.AlertEngineValueControlPoint
          })
export class AssetAsControlPointBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAsControlPoint>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        this.appendConnectedBlock(this.block_a)
            .appendField("as control point");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_as_device",
              model       : Models.AlertEngineOperatorUnaryAsDevice,
              outputType  : Models.AlertEngineValueDevice
          })
export class AssetAsDeviceBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAsDevice>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        this.appendConnectedBlock(this.block_a)
            .appendField("as device");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_as_logical",
              model       : Models.AlertEngineOperatorUnaryAsGroup,
              outputType  : Models.AlertEngineValueLocation
          })
export class AssetAsLogicalBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAsGroup>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        this.appendConnectedBlock(this.block_a)
            .appendField("as logical asset");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_get_location",
              model       : Models.AlertEngineOperatorUnaryAssetGetLocation,
              outputType  : Models.AlertEngineValueLocation
          })
export class AssetGetLocationBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryAssetGetLocation>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        this.appendConnectedBlock(this.block_a)
            .appendField("get location of");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "location_get_name",
              model       : Models.AlertEngineOperatorUnaryLocationGetName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class LocationGetNameBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryLocationGetName>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueLocation);

        this.appendConnectedBlock(this.block_a)
            .appendField("get name of location");
    }
}

@BlockDef({
              blockContext: "AlertRules",
              blockName   : "asset_get_name",
              model       : Models.AlertEngineOperatorUnaryAssetGetName,
              outputType  : Models.EngineValuePrimitiveString
          })
export class AssetGetNameBlock extends Base.UnaryOperatorBlock<Models.AlertEngineOperatorUnaryLocationGetName>
{
    protected initFields(): void
    {
        super.initOperatorFields(Models.AlertEngineValueAsset);

        this.appendConnectedBlock(this.block_a)
            .appendField("get name of asset");
    }
}
