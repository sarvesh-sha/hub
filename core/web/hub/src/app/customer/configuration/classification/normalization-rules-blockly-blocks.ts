import * as Normalization from "app/customer/engines/normalization/normalization";
import * as Vector from "app/customer/engines/normalization/vector";
import * as Common from "app/customer/engines/shared/common";
import * as DateTime from "app/customer/engines/shared/datetime";
import * as Functions from "app/customer/engines/shared/function";
import * as Lists from "app/customer/engines/shared/lists";
import * as Logic from "app/customer/engines/shared/logic";
import * as Maths from "app/customer/engines/shared/math";
import * as Statements from "app/customer/engines/shared/statements";
import * as Strings from "app/customer/engines/shared/strings";
import * as Variables from "app/customer/engines/shared/variables";

import {BlocklyWorkspaceBlocks} from "app/customer/engines/shared/workspace-data";
import {Lookup} from "framework/services/utils.service";
import {ToolboxCategory} from "framework/ui/blockly/toolbox-category";

export const NormalizationRulesBlocklyBlocks: BlocklyWorkspaceBlocks = {
    getNamespaces(): string[]
    {
        return [
            "Shared",
            "NormalizationRules"
        ];
    },

    getToolboxCategories(): Lookup<ToolboxCategory>
    {
        return {
            "Control"       : {
                color      : 23,
                definitions: [
                    Statements.NopCommentBlock,
                    Statements.NopCommentSectionBlock,
                    Statements.ThreadBlock,
                    Statements.StatementThreadEndBlock,
                    Statements.StatementRepeatWhileBlock,
                    Lists.StatementBreakBlock,
                    Lists.StatementContinueBlock,
                    Common.MemoizeBlock
                ]
            },
            "Point"         : {
                color      : 213,
                definitions: [
                    Normalization.GetInputValueBlock,
                    Normalization.GetControlPointNameBlock,
                    Normalization.GetControlPointNameRawBlock,
                    Normalization.GetControlPointOverrideNameBlock,
                    Normalization.GetControlPointDescriptionBlock,
                    Normalization.GetControlPointIdentifierBlock,
                    Normalization.GetControlPointLocationBlock,
                    Normalization.GetControlPointUnitsBlock,
                    Normalization.GetControlPointUnitsStringBlock,
                    Common.LiteralEngineeringUnitsBlock,
                    Common.CompareEngineeringUnitsBlock,
                    Normalization.GetDashboardNameBlock,
                    Normalization.GetDashboardEquipmentNameBlock,
                    Normalization.GetImportedStructureBlock,
                    Normalization.SetOutputValueBlock,
                    Normalization.GetControllerPointsBlock,
                    Normalization.GetPointPropertyBlock
                ]
            },
            "Controller"    : {
                color      : 61,
                definitions: [
                    Normalization.GetControllerNameBlock,
                    Normalization.GetControllerBackupNameBlock,
                    Normalization.GetControllerDescriptionBlock,
                    Normalization.GetControllerIdentifierBlock,
                    Normalization.GetControllerLocationBlock,
                    Normalization.GetControllerModelBlock,
                    Normalization.GetControllerVendorBlock,
                    Normalization.GetControllerInSubnetBlock,
                    Normalization.GetControllersBlock,
                    Normalization.GetControllerPropertyBlock
                ]
            },
            "Equipment"     : {
                color      : 183,
                definitions: [
                    Normalization.PushEquipmentAndClassBlock,
                    Normalization.PushEquipmentExpressionBlock,
                    Normalization.PushEquipmentTableExpressionBlock,
                    Normalization.GetEquipmentBlock,
                    Normalization.GetEquipmentClassBlock,
                    Normalization.GetEquipmentClassIdBlock,
                    Normalization.GetEquipmentClassNameBlock,
                    Normalization.GetEquipmentsBlock,
                    Normalization.CreateEquipmentBlock,
                    Normalization.CreateChildEquipmentBlock,
                    Normalization.ClearEquipmentBlock,
                    Normalization.SetEquipmentClassTableBlock,
                    Normalization.LiteralEquipmentClassBlock
                ]
            },
            "Location"      : {
                color      : 79,
                definitions: [
                    Normalization.PushLocationBlock,
                    Normalization.PushEquipmentLocationBlock,
                    Normalization.GetLocationBlock
                ]
            },
            "Metadata"      : {
                color      : 100,
                definitions: [
                    Normalization.SetMetadataBlock,
                    Normalization.GetMetadataStringBlock,
                    Normalization.GetMetadataNumberBlock
                ]
            },
            "Classification": {
                color      : 131,
                definitions: [
                    Normalization.GetControlPointClassBlock,
                    Normalization.GetPointClassNameBlock,
                    Normalization.GetPointClassDescriptionBlock,
                    Normalization.SetPointClassBlock,
                    Normalization.SetPointClassTableBlock,
                    Normalization.SetSamplingPeriodBlock,
                    Normalization.SetPointClassFromTermScoringBlock,
                    Normalization.SetEngineeringUnitsBlock
                ]
            },
            "Vector"        : {
                color      : 145,
                definitions: [
                    Vector.LiteralDocumentSetBlock,
                    Vector.FilterDocumentsBlock,
                    Vector.ScoreTopDocumentBlock,
                    Vector.ScoreDocumentsBlock,
                    Vector.GetDocumentScoreBlock,
                    Vector.GetJaccardIndexBlock,
                    Vector.SetPointClassFromDocumentBlock
                ]
            },
            "Tags"          : {
                color      : 190,
                definitions: [
                    Normalization.TagsSetBlock,
                    Normalization.TagsGetBlock,
                    Lists.LiteralStringSetBlock,
                    Lists.StringSetOperationBlock,
                    Normalization.TokenizeStringBlock
                ]
            },
            "Math"          : {
                color      : 250,
                definitions: [
                    Maths.BinaryExpressionArithmeticBlock,
                    Maths.BinaryExpressionArithmeticPercentageBlock,
                    Maths.LiteralNumberBlock
                ]
            },
            "Strings"       : {
                color      : 156,
                definitions: [
                    Normalization.NormalizeTextBlock,
                    Strings.LiteralStringBlock,
                    Strings.FormatTextBlock,
                    Strings.LogTextBlock,
                    Strings.StringConcatBlock,
                    Strings.StringStartsWithBlock,
                    Strings.StringEndsWithBlock,
                    Strings.StringIndexOfBlock,
                    Strings.StringSplitBlock,
                    Strings.StringToLowerCaseBlock,
                    Strings.StringToUpperCaseBlock,
                    Strings.StringToNumberBlock,
                    Strings.RegexMatchBlock,
                    Strings.RegexMatchCaseSensitiveBlock,
                    Strings.RegexGetGroupBlock,
                    Strings.RegexReplaceBlock,
                    Strings.RegexIsMatchBlock,
                    Strings.LiteralRegexReplaceTableBlock,
                    Strings.RegexTableReplaceBlock,
                    Strings.LiteralLookupTableBlock,
                    Strings.LookupTableLookupBlock,
                    Strings.LookupTableFilterBlock,
                    Strings.LookupTableReplaceBlock,
                    Strings.LookupTablePutBlock
                ]
            },
            "Lists"         : {
                color      : 200,
                definitions: [
                    Lists.StatementForEachBlock,
                    Lists.ListAsListBlock,
                    Lists.ListLengthBlock,
                    Lists.ListLiteralBlock,
                    Lists.StatementBreakBlock,
                    Lists.StatementContinueBlock,
                    Lists.ListJoinBlock,
                    Lists.ListGetBlock,
                    Lists.ListContainsBlock
                ]
            },
            "Logic"         : {
                color      : 10,
                custom     : Logic.LogicCategory,
                definitions: [
                    Logic.LogicCompareBlock,
                    Logic.ApproximateEqualityBlock,
                    Logic.RangeCheckBlock,
                    Logic.LogicOperationBlock,
                    Logic.LogicNotBlock,
                    Logic.StatementLogicIfBlock,
                    Logic.IsEmptyBlock,
                    Logic.IsNotEmptyBlock,
                    Logic.LogicIsValidNumberBlock,
                    Logic.LogicIsNotValidNumberBlock,
                    Logic.LogicIsNotNullBlock,
                    Logic.LogicIsNullBlock,
                    Logic.LiteralNullBlock,
                    Logic.LiteralBooleanBlock
                ]
            },
            "Time"          : {
                color      : 170,
                definitions: [
                    DateTime.LiteralDateTimeBlock,
                    DateTime.CurrentDateTimeBlock,
                    DateTime.LiteralTimeZoneBlock,
                    DateTime.DateTimeSetTimeZoneBlock,
                    DateTime.DateTimeRangeFromCurrentTimeBlock,
                    DateTime.DateTimeRangeFromTimeBlock,
                    DateTime.DateTimeModifyBlock,
                    DateTime.DateTimeGetFieldBlock,
                    DateTime.DurationLiteralBlock,
                    DateTime.DateTimeRangeBlock
                ]
            },
            "Variables"     : {
                color      : 80,
                custom     : Variables.VariablesCategory,
                definitions: null
            },
            "Functions"     : {
                color      : 225,
                definitions: [
                    Functions.ProcedureBlock,
                    Functions.FunctionReturnBlock,
                    Functions.FunctionReturnValueBlock,
                    Functions.StatementProcedureCallBlock,
                    Functions.ExpressionFunctionCallBlock
                ]
            }
        };
    }
};
