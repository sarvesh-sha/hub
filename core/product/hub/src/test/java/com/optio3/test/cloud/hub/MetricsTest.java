/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.engine.EngineBlock;
import com.optio3.cloud.hub.engine.EngineExecutionProgram;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineTab;
import com.optio3.cloud.hub.engine.core.block.EngineLiteralList;
import com.optio3.cloud.hub.engine.core.block.EngineLiteralString;
import com.optio3.cloud.hub.engine.core.block.EngineLiteralWeeklySchedule;
import com.optio3.cloud.hub.engine.core.block.EngineThread;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetailsForUserProgram;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionStep;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineCreateEnumeratedSeries;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineCreateMultiStableSeries;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineInputParameterScalar;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineInputParameterSeries;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorAggregate;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorBinary;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorBinaryBistable;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorFilterInsideSchedule;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorFilterOutsideSchedule;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorThreshold;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorThresholdCount;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorThresholdDuration;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorThresholdEnum;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineOperatorUnarySelectValue;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineStatementSetOutputToScalar;
import com.optio3.cloud.hub.engine.metrics.block.MetricsEngineStatementSetOutputToSeries;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueScalar;
import com.optio3.cloud.hub.engine.metrics.value.MetricsEngineValueSeries;
import com.optio3.cloud.hub.model.metrics.MetricsDefinition;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionImportExport;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionVersion;
import com.optio3.cloud.hub.model.schedule.DailySchedule;
import com.optio3.cloud.hub.model.schedule.DailyScheduleWithDayOfWeek;
import com.optio3.cloud.hub.model.schedule.RecurringWeeklySchedule;
import com.optio3.cloud.hub.model.schedule.RelativeTimeRange;
import com.optio3.cloud.hub.model.shared.program.CommonEngineAggregateOperation;
import com.optio3.cloud.hub.model.shared.program.CommonEngineArithmeticOperation;
import com.optio3.cloud.hub.model.shared.program.CommonEngineCompareOperation;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesPropertyResponse;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;
import com.optio3.protocol.model.EngineeringUnits;
import com.optio3.protocol.model.EngineeringUnitsFactors;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.IdGenerator;
import com.optio3.util.TimeUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;

public class MetricsTest extends Optio3Test
{
    static class MetricsEngineExecutionContextTest extends MetricsEngineExecutionContext
    {
        final Map<String, MetricsEngineValueSeries> parametersForSeries = Maps.newHashMap();

        public MetricsEngineExecutionContextTest(ILogger logger,
                                                 SessionProvider sessionProvider,
                                                 EngineExecutionProgram<MetricsDefinitionDetails> program)
        {
            super(logger, sessionProvider, program);
        }

        @Override
        public MetricsEngineValueSeries getSeries(String nodeId,
                                                  int timeShift,
                                                  ChronoUnit timeShiftUnit)
        {
            return parametersForSeries.get(nodeId);
        }
    }

    //--//

    static ZoneId zoneId = ZoneId.of("America/Los_Angeles");

    // Monday
    static ZonedDateTime sharedStart = ZonedDateTime.of(2020, 11, 2, 0, 0, 0, 0, zoneId);

    //--//

    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    public void testAdd() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Plus;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 2, 3, 4));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 5, 3, 5, 7, 9));

        MetricsEngineExecutionContext ctx2 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 10, 2, 3, 4, 5));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 2, 3, 4));
        });
        assertSeries(ctx2, newSeries(sharedStart, EngineeringUnits.meters, 5, 3, 4.5, 6, 7.5, 8, 9));

        MetricsEngineExecutionContext ctx3 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.millimeters, 5, 2000, 3000, 4000, 5000));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.centimeters, 5, 100, 200, 300, 400));
        });
        assertSeries(ctx3, newSeries(sharedStart, EngineeringUnits.meters, 5, 3, 5, 7, 9));
    }

    @Test
    public void testSub() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Minus;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 3, 5, 7));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 0, -1, -2));
    }

    @Test
    public void testSubScalar() throws
                                Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 2;
                                                                     b.units = EngineeringUnits.millimeters.getConversionFactors();

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Minus;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 5, 2 - 0.002, 3 - 0.002, 4 - 0.002, 5 - 0.002));
    }

    @Test
    public void testSubScalar2() throws
                                 Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 2;
                                                                     b.units = EngineeringUnits.centimeters.getConversionFactors();

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Minus;
                                                                     op.a         = EngineExpression.cast(b);
                                                                     op.b         = EngineExpression.cast(a);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 5, 0.02 - 2, 0.02 - 3, 0.02 - 4, 0.02 - 5));
    }

    @Test
    public void testMul() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Multiply;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 3, 5, 7));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.square_meters, 5, 2, 9, 20, 35));
    }

    @Test
    public void testDiv() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Divide;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 1, 3, 5, 7));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.no_units, 5, 2f / 1, 3f / 3, 4f / 5, 5f / 7));
    }

    @Test
    public void testDivScalar() throws
                                Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 2;
                                                                     b.units = EngineeringUnits.hours.getConversionFactors();

                                                                     MetricsEngineOperatorBinary op = new MetricsEngineOperatorBinary();
                                                                     op.operation = CommonEngineArithmeticOperation.Divide;
                                                                     op.a         = EngineExpression.cast(a);
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters_per_second, 5, 1.0 / 3600, 1.5 / 3600, 2.0 / 3600, 2.5 / 3600));
    }

    @Test
    public void testThreshold() throws
                                Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 400;
                                                                     b.units = EngineeringUnits.centimeters.getConversionFactors();

                                                                     MetricsEngineOperatorThreshold op = new MetricsEngineOperatorThreshold();
                                                                     op.operation = CommonEngineCompareOperation.GreaterThanOrEqual;
                                                                     op.a         = a;
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.activeInactive, 5, 0, 0, 1, 1));
    }

    @Test
    public void testThresholdEnum() throws
                                    Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     EngineLiteralString enum1 = new EngineLiteralString();
                                                                     enum1.value = "v2";

                                                                     EngineLiteralList b = new EngineLiteralList();
                                                                     b.value = Lists.newArrayList(enum1);

                                                                     MetricsEngineOperatorThresholdEnum op = new MetricsEngineOperatorThresholdEnum();
                                                                     op.a = a;
                                                                     op.b = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, 5, "v2", "v1", "v3", "v4", "v2", "v1"));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.activeInactive, 5, 1, 0, 0, 0, 1, 0));
    }

    @Test
    public void testThresholdEnumDuration() throws
                                            Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 0.0;
                                                                     b.units = EngineeringUnits.no_units.getConversionFactors();

                                                                     EngineLiteralString enum1 = new EngineLiteralString();
                                                                     enum1.value = "v2";

                                                                     EngineLiteralList c = new EngineLiteralList();
                                                                     c.value = Lists.newArrayList(enum1);

                                                                     MetricsEngineOperatorThresholdEnum op = new MetricsEngineOperatorThresholdEnum();
                                                                     op.a = a;
                                                                     op.b = EngineExpression.cast(c);

                                                                     MetricsEngineOperatorThresholdDuration op2 = new MetricsEngineOperatorThresholdDuration();
                                                                     op2.operation = CommonEngineCompareOperation.GreaterThan;
                                                                     op2.a         = op;
                                                                     op2.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op2;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, 5, "v2", "v1", "v3", "v4", "v2", "v1"));
        });
        assertLastScalar(ctx1, newScalar(EngineeringUnits.seconds, 10));
    }

    @Test
    public void testThresholdCount() throws
                                     Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 300;
                                                                     b.units = EngineeringUnits.centimeters.getConversionFactors();

                                                                     MetricsEngineOperatorThresholdCount op = new MetricsEngineOperatorThresholdCount();
                                                                     op.operation = CommonEngineCompareOperation.GreaterThanOrEqual;
                                                                     op.a         = a;
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5, 1, 5, 2, 4));
        });
        assertLastScalar(ctx1, newScalar(EngineeringUnits.counts, 3));
    }

    @Test
    public void testThresholdDuration() throws
                                        Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterScalar b = new MetricsEngineInputParameterScalar();
                                                                     b.value = 300;
                                                                     b.units = EngineeringUnits.centimeters.getConversionFactors();

                                                                     MetricsEngineOperatorThresholdDuration op = new MetricsEngineOperatorThresholdDuration();
                                                                     op.operation = CommonEngineCompareOperation.GreaterThan;
                                                                     op.a         = a;
                                                                     op.b         = EngineExpression.cast(b);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5, 1, 5, 2, 4, 4));
        });
        assertLastScalar(ctx1, newScalar(EngineeringUnits.seconds, 20));
    }

    @Test
    public void testMin() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineOperatorAggregate op = new MetricsEngineOperatorAggregate();
                                                                     op.operation = CommonEngineAggregateOperation.Min;
                                                                     op.a         = a;

                                                                     MetricsEngineStatementSetOutputToScalar output = new MetricsEngineStatementSetOutputToScalar();
                                                                     output.scalar = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertScalar(ctx1, newScalar(EngineeringUnits.meters, 2));
    }

    @Test
    public void testMax() throws
                          Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineOperatorAggregate op = new MetricsEngineOperatorAggregate();
                                                                     op.operation = CommonEngineAggregateOperation.Max;
                                                                     op.a         = a;

                                                                     MetricsEngineStatementSetOutputToScalar output = new MetricsEngineStatementSetOutputToScalar();
                                                                     output.scalar = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertScalar(ctx1, newScalar(EngineeringUnits.meters, 5));
    }

    @Test
    public void testMean() throws
                           Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineOperatorAggregate op = new MetricsEngineOperatorAggregate();
                                                                     op.operation = CommonEngineAggregateOperation.Mean;
                                                                     op.a         = a;

                                                                     MetricsEngineStatementSetOutputToScalar output = new MetricsEngineStatementSetOutputToScalar();
                                                                     output.scalar = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 2, 3, 4, 5));
        });
        assertScalar(ctx1, newScalar(EngineeringUnits.meters, 3.5));
    }

    //--//

    @Test
    public void testFilterWithinSchedule() throws
                                           Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

                                                                     RelativeTimeRange relativeRange = new RelativeTimeRange();
                                                                     relativeRange.offsetSeconds   = 1 * 3600;
                                                                     relativeRange.durationSeconds = 1 * 3600;

                                                                     DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
                                                                     day.dayOfWeek     = DayOfWeek.MONDAY;
                                                                     day.dailySchedule = new DailySchedule();
                                                                     day.dailySchedule.ranges.add(relativeRange);

                                                                     schedule.days.add(day);

                                                                     EngineLiteralWeeklySchedule b = new EngineLiteralWeeklySchedule();
                                                                     b.value = schedule;

                                                                     MetricsEngineOperatorFilterInsideSchedule op = new MetricsEngineOperatorFilterInsideSchedule();
                                                                     op.a = a;
                                                                     op.b = b;

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.maxInterpolationGap = Double.MAX_VALUE;
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 86400, 0, 86400));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 1000, Double.NaN, Double.NaN, 3600, 7200, Double.NaN, Double.NaN));
    }

    @Test
    public void testFilterOutsideSchedule() throws
                                            Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     RecurringWeeklySchedule schedule = new RecurringWeeklySchedule();

                                                                     RelativeTimeRange relativeRange = new RelativeTimeRange();
                                                                     relativeRange.offsetSeconds   = 1 * 3600;
                                                                     relativeRange.durationSeconds = 1 * 3600;

                                                                     DailyScheduleWithDayOfWeek day = new DailyScheduleWithDayOfWeek();
                                                                     day.dayOfWeek     = DayOfWeek.MONDAY;
                                                                     day.dailySchedule = new DailySchedule();
                                                                     day.dailySchedule.ranges.add(relativeRange);

                                                                     schedule.days.add(day);

                                                                     EngineLiteralWeeklySchedule b = new EngineLiteralWeeklySchedule();
                                                                     b.value = schedule;

                                                                     MetricsEngineOperatorFilterOutsideSchedule op = new MetricsEngineOperatorFilterOutsideSchedule();
                                                                     op.a = a;
                                                                     op.b = b;

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.maxInterpolationGap = Double.MAX_VALUE;
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 86400, 0, 86400));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.meters, 1000, 0, 3600, Double.NaN, Double.NaN, 7200, 86400));
    }

    //--//

    @Test
    public void testCreateEnumeratedSeries() throws
                                             Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     var op1 = new MetricsEngineOperatorUnarySelectValue();
                                                                     op1.identifier = "a";
                                                                     op1.a          = a;

                                                                     var op2 = new MetricsEngineOperatorUnarySelectValue();
                                                                     op2.identifier = "b";
                                                                     op2.a          = b;

                                                                     var op = new MetricsEngineCreateEnumeratedSeries();
                                                                     op.value = Lists.newArrayList(op1, op2);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 1, 0, 0, 0));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 0, 0, 1, 0));
        });
        assertSeries(ctx1, newSeries(sharedStart, 5, null, "a", null, "b", null));
    }

    @Test
    public void testCreateMultistableSeries() throws
                                              Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     var op1 = new MetricsEngineOperatorUnarySelectValue();
                                                                     op1.identifier = "a";
                                                                     op1.a          = a;

                                                                     var op2 = new MetricsEngineOperatorUnarySelectValue();
                                                                     op2.identifier = "b";
                                                                     op2.a          = b;

                                                                     var op = new MetricsEngineCreateMultiStableSeries();
                                                                     op.value = Lists.newArrayList(op1, op2);

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 1, 0, 0, 0));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 0, 0, 1, 0));
        });
        assertSeries(ctx1, newSeries(sharedStart, 5, null, "a", "a", "b", "b"));
    }

    @Test
    public void testCreateBistableSeries() throws
                                           Exception
    {
        MetricsDefinitionVersionRecord version = generateMetrics((thread) ->
                                                                 {
                                                                     MetricsEngineInputParameterSeries a = new MetricsEngineInputParameterSeries();
                                                                     a.nodeId = "a";

                                                                     MetricsEngineInputParameterSeries b = new MetricsEngineInputParameterSeries();
                                                                     b.nodeId = "b";

                                                                     var op = new MetricsEngineOperatorBinaryBistable();
                                                                     op.a = a;
                                                                     op.b = b;

                                                                     MetricsEngineStatementSetOutputToSeries output = new MetricsEngineStatementSetOutputToSeries();
                                                                     output.series = op;

                                                                     thread.statements.add(output);
                                                                 });

        MetricsEngineExecutionContext ctx1 = executeMetrics(version, (ctx) ->
        {
            ctx.parametersForSeries.put("a", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 1, 0, 0, 0));
            ctx.parametersForSeries.put("b", newSeries(sharedStart, EngineeringUnits.meters, 5, 0, 0, 0, 1, 0));
        });
        assertSeries(ctx1, newSeries(sharedStart, EngineeringUnits.activeInactive, 5, Double.NaN, 1, 1, 0, 0));
    }

    //--//

    private MetricsEngineValueScalar newScalar(EngineeringUnits units,
                                               double value)
    {
        MetricsEngineValueScalar res = new MetricsEngineValueScalar();
        res.value = value;
        res.units = units.getConversionFactors();

        return res;
    }

    private MetricsEngineValueSeries newSeries(ZonedDateTime startTime,
                                               EngineeringUnits units,
                                               int timeStep,
                                               double... values)
    {
        TimeSeriesPropertyResponse samples = new TimeSeriesPropertyResponse();
        samples.values       = values;
        samples.expectedType = Double.class;
        samples.timestamps   = new double[values.length];
        samples.timeZone     = startTime.getZone();

        double baseTime = startTime.toEpochSecond();

        for (int i = 0; i < values.length; i++)
        {
            samples.timestamps[i] = baseTime + i * timeStep;
        }

        samples.setUnits(units);

        return new MetricsEngineValueSeries(samples);
    }

    private MetricsEngineValueSeries newSeries(ZonedDateTime startTime,
                                               int timeStep,
                                               String... values)
    {
        double[]                   valuesIndexes   = new double[values.length];
        final List<String>         enumLookup      = Lists.newArrayList();
        final Map<String, Integer> indexAssignment = Maps.newHashMap();

        Function<String, Integer> assignId = (value) ->
        {
            int index = indexAssignment.size();
            enumLookup.add(value);
            return index;
        };

        for (int i = 0; i < values.length; i++)
        {
            String value = values[i];

            valuesIndexes[i] = value != null ? indexAssignment.computeIfAbsent(value, assignId) : Double.NaN;
        }

        MetricsEngineValueSeries series = newSeries(startTime, EngineeringUnits.enumerated, timeStep, valuesIndexes);
        enumLookup.toArray(series.values.enumLookup = new String[enumLookup.size()]);

        return series;
    }

    //--//

    private void assertScalar(MetricsEngineExecutionContext ctx,
                              MetricsEngineValueScalar expected)
    {
        MetricsEngineValueScalar actualValue = ctx.outputForScalar;
        assertNotNull(actualValue);

        assertTrue(expected.units.isEquivalent(actualValue.units));
        assertEquals(expected.value, actualValue.convert(expected.units).value, 0.001);
    }

    private void assertLastScalar(MetricsEngineExecutionContext ctx,
                                  MetricsEngineValueScalar expected)
    {
        MetricsEngineValueSeries actualValues = ctx.outputForSeries;
        assertNotNull(actualValues);

        double[]                 samples     = actualValues.values.values;
        MetricsEngineValueScalar actualValue = new MetricsEngineValueScalar();
        actualValue.units = actualValues.getUnitsFactors();
        actualValue.value = samples[samples.length - 1];

        assertTrue(expected.units.isEquivalent(actualValue.units));
        assertEquals(expected.value, actualValue.convert(expected.units).value, 0.001);
    }

    private void assertSeries(MetricsEngineExecutionContext ctx,
                              MetricsEngineValueSeries expected)
    {
        MetricsEngineValueSeries actualValue = ctx.outputForSeries;
        assertNotNull(actualValue);

        EngineeringUnitsFactors expectedUnits = expected.getUnitsFactors();
        assertTrue(expectedUnits.isEquivalent(actualValue.getUnitsFactors()));
        Assert.assertArrayEquals(expected.values.values, actualValue.convert(expectedUnits).values.values, 0.001);
    }

    private MetricsEngineExecutionContextTest executeMetrics(MetricsDefinitionVersionRecord rec,
                                                             Consumer<MetricsEngineExecutionContextTest> callback)
    {
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            EngineExecutionProgram<MetricsDefinitionDetails> program = rec.prepareProgram(sessionHolder);
            MetricsEngineExecutionContextTest                ctx     = new MetricsEngineExecutionContextTest(null, sessionHolder.getSessionProvider(), program);

            ctx.traceExecution = true;
            ctx.reset(null);
            callback.accept(ctx);

            Double start = null;
            Double end   = null;

            for (MetricsEngineValueSeries value : ctx.parametersForSeries.values())
            {
                for (double timestamp : value.values.timestamps)
                {
                    if (start == null || start > timestamp)
                    {
                        start = timestamp;
                    }

                    if (end == null || end < timestamp)
                    {
                        end = timestamp;
                    }
                }
            }

            ctx.rangeStart = TimeUtils.fromSecondsToUtcTime((long) (double) start);
            ctx.rangeEnd   = TimeUtils.fromSecondsToUtcTime((long) (double) end);

            ctx.evaluate(1000, (stack, line) -> System.out.printf("Log: %s\n", line));
            return ctx;
        }
    }

    private MetricsDefinitionVersionRecord loadMetrics(String path) throws
                                                                    Exception
    {
        return generateMetrics(() ->
                               {
                                   String metricsRaw = loadResourceAsText(path, true);
                                   ObjectMapper mapper = applicationRule.getApplication()
                                                                        .getServiceNonNull(ObjectMapper.class);

                                   return mapper.readValue(metricsRaw, MetricsDefinitionImportExport.class);
                               });
    }

    private MetricsDefinitionVersionRecord generateMetrics(Consumer<EngineThread> callback) throws
                                                                                            Exception
    {
        return generateMetrics(() ->
                               {
                                   MetricsDefinitionImportExport ie = new MetricsDefinitionImportExport();
                                   ie.definition             = new MetricsDefinition();
                                   ie.definition.title       = "nodeId";
                                   ie.definition.description = "description";
                                   ie.details                = new MetricsDefinitionDetailsForUserProgram();

                                   EngineThread thread = new EngineThread();
                                   thread.statements = Lists.newArrayList();

                                   List<EngineBlock> block = Lists.newArrayList();
                                   block.add(thread);

                                   EngineTab tab = new EngineTab();
                                   tab.name = "main";
                                   tab.blockChains.add(block);
                                   ie.details.tabs.add(tab);

                                   callback.accept(thread);
                                   return ie;
                               });
    }

    private MetricsDefinitionVersionRecord generateMetrics(Callable<MetricsDefinitionImportExport> callback) throws
                                                                                                             Exception
    {
        MetricsDefinitionImportExport importExport = callback.call();

        MetricsDefinitionRecord        rec_definition;
        MetricsDefinitionVersionRecord rec_version;

        try (SessionHolder sessionHolder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<MetricsDefinitionRecord>        definitionHelper = sessionHolder.createHelper(MetricsDefinitionRecord.class);
            RecordHelper<MetricsDefinitionVersionRecord> versionHelper    = sessionHolder.createHelper(MetricsDefinitionVersionRecord.class);

            rec_definition = MetricsDefinitionRecord.newInstance(IdGenerator.newGuid());
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, importExport.definition, rec_definition);

            definitionHelper.persist(rec_definition);

            MetricsDefinitionVersion version = new MetricsDefinitionVersion();
            version.details = new ModelSanitizerContext.Simple(sessionHolder).processTyped(importExport.details);

            rec_version = MetricsDefinitionVersionRecord.newInstance(versionHelper, version, rec_definition, null, null);

            sessionHolder.commit();
        }

        return rec_version;
    }

    private void dumpSteps(List<MetricsEngineExecutionStep> steps)
    {
        System.out.println(ObjectMappers.prettyPrintAsJson(steps));
    }
}
