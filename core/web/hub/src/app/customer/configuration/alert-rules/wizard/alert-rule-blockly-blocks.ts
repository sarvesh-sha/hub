import * as Alerts from "app/customer/engines/alerts/alerts";
import * as Assets from "app/customer/engines/alerts/assets";
import * as ControlPoints from "app/customer/engines/alerts/control-points";
import * as Delivery from "app/customer/engines/alerts/delivery-options";
import * as TravelLog from "app/customer/engines/alerts/travel-log";

import * as Common from "app/customer/engines/shared/common";
import * as DateTime from "app/customer/engines/shared/datetime";
import * as Functions from "app/customer/engines/shared/function";
import * as Inputs from "app/customer/engines/shared/input";
import * as Lists from "app/customer/engines/shared/lists";
import * as Logic from "app/customer/engines/shared/logic";
import * as Maths from "app/customer/engines/shared/math";
import * as Statements from "app/customer/engines/shared/statements";
import * as Strings from "app/customer/engines/shared/strings";
import * as Variables from "app/customer/engines/shared/variables";

import {BlocklyWorkspaceBlocks} from "app/customer/engines/shared/workspace-data";
import {Lookup} from "framework/services/utils.service";
import {ToolboxCategory} from "framework/ui/blockly/toolbox-category";

export const AlertRuleBlocklyBlocks: BlocklyWorkspaceBlocks = {
    getNamespaces(): string[]
    {
        return [
            "Shared",
            "AlertRules"
        ];
    },

    getToolboxCategories(): Lookup<ToolboxCategory>
    {
        return {
            "Control"                : {
                color      : 120,
                definitions: [
                    Statements.NopCommentBlock,
                    Statements.NopCommentSectionBlock,
                    Statements.ThreadBlock,
                    Statements.StatementThreadEndBlock,
                    Statements.StatementRepeatWhileBlock,
                    Lists.StatementBreakBlock,
                    Lists.StatementContinueBlock
                ]
            },
            "Input"                  : {
                color      : 111,
                definitions: [
                    Inputs.InputParameterNumberBlock,
                    ControlPoints.InputParameterControlPointBlock,
                    ControlPoints.InputParameterControlPointsSelectionBlock,
                    Inputs.InputParameterDateTimeBlock,
                    Inputs.InputParameterDurationBlock,
                    Inputs.InputParameterStringBlock,
                    Delivery.InputParameterDeliveryOptionsBlock,
                    Inputs.InputParameterBooleanBlock
                ]
            },
            "Alerts"                 : {
                color      : 30,
                definitions: [
                    Alerts.CreateAlertBlock,
                    Alerts.GetAlertBlock,
                    Alerts.LiteralAlertSeverityBlock,
                    Alerts.LiteralAlertStatusBlock,
                    Alerts.HasAlertChangedBlock,
                    Alerts.GetAlertStatusBlock,
                    Alerts.GetAlertSeverityBlock,
                    Alerts.SetAlertTextBlock,
                    Alerts.SetAlertTimestampBlock,
                    Alerts.SetAlertDescriptionBlock,
                    Alerts.SetAlertSeverityBlock,
                    Alerts.SetAlertSetStatusBlock,
                    Alerts.NewEmailBlock,
                    Alerts.SetEmailSubjectBlock,
                    Alerts.AddEmailLineBlock,
                    Alerts.NewSmsBlock,
                    Alerts.SetSmsSenderBlock,
                    Alerts.AddSmsLineBlock,
                    Alerts.NewTicketBlock,
                    Alerts.CommitActionBlock
                ]
            },
            "Math"                   : {
                color      : 250,
                definitions: [
                    Maths.BinaryExpressionArithmeticBlock,
                    Maths.BinaryExpressionArithmeticPercentageBlock,
                    Maths.LiteralNumberBlock
                ]
            },
            "Assets"                 : {
                color      : 40,
                definitions: [
                    Assets.LiteralAssetQueryEquipmentClassBlock,
                    Assets.LiteralAssetQueryPointClassBlock,
                    Assets.LiteralAssetQueryTagBlock,
                    Assets.AssetQueryAndBlock,
                    Assets.AssetQueryOrBlock,
                    Assets.AssetQueryNotBlock,
                    Assets.AssetQueryExecBlock,
                    Assets.AssetQueryRelationsBlock,
                    Assets.AssetQueryRelations2Block,
                    Assets.AssetQueryRelationsSingleBlock,
                    Assets.AssetQueryRelations2SingleBlock,
                    Assets.AssetAsControlPointBlock,
                    Assets.AssetAsDeviceBlock,
                    Assets.AssetAsLogicalBlock,
                    Assets.AssetGetLocationBlock,
                    Assets.LocationGetNameBlock,
                    Assets.AssetGetNameBlock,
                    Assets.AlertGraphNodeBlock,
                    Assets.AlertGraphNodesBlock
                ]
            },
            "Control Points"         : {
                color      : 12,
                definitions: [
                    ControlPoints.LiteralControlPointsSelectionBlock,
                    ControlPoints.LiteralControlPointBlock,
                    ControlPoints.ControlPointPresentValueBlock,
                    ControlPoints.ControlPointGetSampleBlock,
                    ControlPoints.ControlPointGetSampleRangeBlock,
                    ControlPoints.ControlPointGetNewSamplesBlock,
                    ControlPoints.SampleGetPropertyBlock,
                    ControlPoints.SampleGetTimeBlock,
                    ControlPoints.ControlPointGetSampleAggregateBlock,
                    ControlPoints.ControlPointGetPropertyBlock,
                    ControlPoints.ControlPointSetPropertyBlock,
                    Common.LiteralEngineeringUnitsBlock
                ]
            },
            "Control Points Metadata": {
                color      : 12,
                definitions: [
                    ControlPoints.ControlPointSetMetadataBlock,
                    ControlPoints.ControlPointGetMetadataNumberBlock,
                    ControlPoints.ControlPointGetMetadataStringBlock,
                    ControlPoints.ControlPointGetMetadataTimestampBlock
                ]
            },
            "Travel Log"             : {
                color      : 12,
                definitions: [
                    TravelLog.TravelLogCoordinatesBlock,
                    TravelLog.TravelLogGetNewEntriesBlock,
                    TravelLog.TravelEntryInsideFenceBlock,
                    TravelLog.TravelEntryGetTimeBlock
                ]
            },
            "Delivery Options"       : {
                color      : 80,
                definitions: [
                    Delivery.LiteralDeliveryOptionsBlock,
                    Delivery.GetEmailDeliveryOptionsFromLocationBlock,
                    Delivery.GetSmsDeliveryOptionsFromLocationBlock,
                    Delivery.BinaryExpressionDeliveryOptionsBlock
                ]
            },
            "Strings"                : {
                color      : 156,
                definitions: [
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
                    Strings.LookupTableReplaceBlock
                ]
            },
            "Lists"                  : {
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
            "Logic"                  : {
                color      : 190,
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
            "Time"                   : {
                color      : 180,
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
                    DateTime.DateTimeRangeBlock,
                    DateTime.LiteralWeeklyScheduleBlock,
                    DateTime.WeeklyScheduleSetTimeZoneBlock,
                    DateTime.WeeklyScheduleIsIncludedBlock,
                    ControlPoints.GetTimeZoneFromLocationBlock
                ]
            },
            "Variables"              : {
                color      : 80,
                custom     : Variables.VariablesCategory,
                definitions: null
            },
            "Functions"              : {
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
