import * as Metrics from "app/customer/engines/metrics/metrics";
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

export const MetricBlocklyBlocks: BlocklyWorkspaceBlocks = {
    getNamespaces(): string[]
    {
        return [
            "Shared",
            "Metrics"
        ];
    },

    getToolboxCategories(): Lookup<ToolboxCategory>
    {
        return {
            "Control"  : {
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
            "Metrics"  : {
                color      : 30,
                definitions: [
                    Metrics.InputParameterScalarBlock,
                    Metrics.InputParameterSeriesBlock,
                    Metrics.InputParameterSeriesWithTimeOffsetBlock,
                    Metrics.MetricsEngineStatementSetOutputToScalarBlock,
                    Metrics.MetricsEngineStatementSetOutputToSeriesBlock,
                    Metrics.MetricsEngineStatementSetOutputToSeriesWithNameBlock,
                    Metrics.MetricsEngineOperatorUnaryAsSeriesBlock,
                    Metrics.MetricsEngineCreateVector3Block,
                    Metrics.MetricsEngineOperatorBinaryBistableBlock,
                    Metrics.MetricsEngineCreateEnumeratedSeriesBlock,
                    Metrics.MetricsEngineCreateMultiStableSeriesBlock,
                    Metrics.MetricsEngineOperatorUnarySelectValueBlock
                ]
            },
            "Math"     : {
                color      : 250,
                definitions: [
                    Metrics.MetricsEngineOperatorBinaryBlock,
                    Metrics.MetricsEngineOperatorBinaryLogicBlock,
                    Metrics.MetricsEngineOperatorUnaryNotBlock,
                    Metrics.MetricsEngineOperatorVectorBinaryAddBlock,
                    Metrics.MetricsEngineOperatorVectorBinarySubtractBlock,
                    Metrics.MetricsThresholdBlock,
                    Metrics.MetricsThresholdCountBlock,
                    Metrics.MetricsThresholdDurationBlock,
                    Metrics.MetricsThresholdPartialDurationBlock,
                    Metrics.MetricsThresholdRangeBlock,
                    Metrics.MetricsThresholdEnumBlock,
                    Metrics.MetricsFilterBlock,
                    Metrics.MetricsFilterInsideScheduleBlock,
                    Metrics.MetricsFilterOutsideScheduleBlock,
                    Maths.BinaryExpressionArithmeticBlock,
                    Maths.BinaryExpressionArithmeticPercentageBlock,
                    Metrics.LiteralScalarBlock,
                    Maths.LiteralNumberBlock,
                    Metrics.MetricsEngineOperatorGpsDistanceBlock,
                    Metrics.MetricsEngineOperatorGpsSunElevationBlock
                ]
            },
            "Strings"  : {
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
                    Strings.RegexIsMatchBlock,
                    Strings.RegexMatchCaseSensitiveBlock,
                    Strings.RegexGetGroupBlock,
                    Strings.RegexReplaceBlock,
                    Strings.LiteralRegexReplaceTableBlock,
                    Strings.RegexTableReplaceBlock
                ]
            },
            "Lists"    : {
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
            "Logic"    : {
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
            "Time"     : {
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
                    DateTime.WeeklyScheduleIsIncludedBlock
                ]
            },
            "Variables": {
                color      : 80,
                custom     : Variables.VariablesCategory,
                definitions: null
            },
            "Functions": {
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
