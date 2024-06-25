/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.cloud.hub;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.HubDataDefinition;
import com.optio3.cloud.hub.engine.EngineExecutionAssignment;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionStep;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveBoolean;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveNumber;
import com.optio3.cloud.hub.engine.core.value.EngineValuePrimitiveString;
import com.optio3.cloud.hub.model.alert.AlertDefinitionImportExport;
import com.optio3.cloud.hub.model.alert.AlertDefinitionVersion;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.ObjectMappers;
import com.optio3.test.common.Optio3Test;
import com.optio3.test.dropwizard.TestApplicationWithDbRule;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Test;

public class AlertsTest extends Optio3Test
{
    @ClassRule
    public static final TestApplicationWithDbRule<HubApplication, HubConfiguration> applicationRule = new TestApplicationWithDbRule<>(HubApplication.class, "hub-test.yml", (configuration) ->
    {
        configuration.data = new HubDataDefinition[] { new HubDataDefinition("demodata/defaultUsers.json", false, false) };
    }, null);

    @Test
    public void testMath() throws
                           Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/math1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 32.0);
    }

    @Test
    public void testSimpleFunction() throws
                                     Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/function1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 115);
    }

    @Test
    public void testSimpleFunctionNoArguments() throws
                                                Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/functionNoArguments.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 5);
    }

    @Test
    public void testRecursiveFunction() throws
                                        Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/Fibonacci.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 13);
    }

    @Test
    public void testWhileLoop() throws
                                Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/whileLoop1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 0);
    }

    @Test
    public void testLogicIfElse() throws
                                  Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/logic1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 3);
    }

    @Test
    public void testFailure() throws
                              Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/failure1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertFailure(steps);
    }

    @Test
    public void testList() throws
                           Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/list1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 15);
    }

    @Test
    public void testForEachBreakContinue() throws
                                           Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/loop1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 5);
    }

    @Test
    public void testWhileBreakContinue() throws
                                         Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/loop2.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "result", 6);
    }

    @Test
    public void testAlertStatusComparison() throws
                                            Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/alertStatus1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "activeEqualsActive", true);
        assertAssignment(steps, "activeNotEqualsActive", false);
        assertAssignment(steps, "activeNotEqualsMuted", true);
        assertAssignment(steps, "activeLessThanClosed", true);
    }

    @Test
    public void testAlertSeverityComparison() throws
                                              Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/alertSeverity1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "normalEqualsNormal", true);
        assertAssignment(steps, "normalNotEqualsNormal", false);
        assertAssignment(steps, "normalLessThanEqualNormal", true);
        assertAssignment(steps, "lowLessThanSignificant", true);
        assertAssignment(steps, "significantGreaterThanLow", true);
    }

    @Test
    public void testRegex1() throws
                             Exception
    {
        AlertDefinitionVersionRecord   version = loadAlert("alerts/regex1.json");
        List<AlertEngineExecutionStep> steps   = executeAlert(version);
        assertAssignment(steps, "t1", "123");
        assertAssignment(steps, "t2", "adad FOO adad");
    }

    //--//

    private void assertAssignment(List<AlertEngineExecutionStep> steps,
                                  String name,
                                  double value)
    {
        EngineExecutionAssignment actual = getAssignment(steps, name);

        EngineValuePrimitiveNumber actualValue = assertCast(EngineValuePrimitiveNumber.class, actual.value);
        if (value != actualValue.asNumber())
        {
            dumpSteps(steps);
            assertEquals(value, actualValue.asNumber(), 0);
        }
    }

    private void assertAssignment(List<AlertEngineExecutionStep> steps,
                                  String name,
                                  String value)
    {
        EngineExecutionAssignment actual = getAssignment(steps, name);

        EngineValuePrimitiveString actualValue = assertCast(EngineValuePrimitiveString.class, actual.value);
        if (!StringUtils.equals(value, actualValue.value))
        {
            dumpSteps(steps);
            assertEquals(value, actualValue.value);
        }
    }

    private void assertAssignment(List<AlertEngineExecutionStep> steps,
                                  String name,
                                  boolean value)
    {
        EngineExecutionAssignment actual = getAssignment(steps, name);

        EngineValuePrimitiveBoolean actualValue = assertCast(EngineValuePrimitiveBoolean.class, actual.value);
        if (value != actualValue.value)
        {
            dumpSteps(steps);
            assertEquals(value, actualValue.value);
        }
    }

    private EngineExecutionAssignment getAssignment(List<AlertEngineExecutionStep> steps,
                                                    String name)
    {
        List<EngineExecutionAssignment> assignments = CollectionUtils.transformToListNoNulls(steps, (step) ->
        {
            if (step.assignment != null && name.equals(step.assignment.name))
            {
                return step.assignment;
            }

            return null;
        });

        EngineExecutionAssignment actual = CollectionUtils.lastElement(assignments);

        assertNotNull(actual);

        return actual;
    }

    private void assertFailure(List<AlertEngineExecutionStep> steps)
    {
        List<AlertEngineExecutionStep> failureSteps = CollectionUtils.transformToListNoNulls(steps, (step) ->
        {
            if (step.failure != null)
            {
                return step;
            }

            return null;
        });

        assertTrue(failureSteps.size() > 0);
    }

    private List<AlertEngineExecutionStep> executeAlert(AlertDefinitionVersionRecord rec)
    {
        // Benchmark performance.
        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            try (AlertEngineExecutionContext ctx = new AlertEngineExecutionContext(null, sessionHolder, rec, null))
            {
                ctx.traceExecution = false;

                final int loops = 10000;
                Stopwatch sw2   = Stopwatch.createStarted();
                for (int i = 0; i < loops; i++)
                {
                    ctx.reset(null);

                    ctx.evaluate(1000, (stack, line) -> System.out.printf("Log: %s\n", line));
                }
                sw2.stop();

                System.out.printf("%s: %d : %,d nsec per evaluation\n",
                                  rec.getDefinition()
                                     .getTitle(),
                                  loops,
                                  sw2.elapsed(TimeUnit.NANOSECONDS) / loops);
            }
        }

        try (SessionHolder sessionHolder = applicationRule.openSessionWithoutTransaction())
        {
            try (AlertEngineExecutionContext ctx = new AlertEngineExecutionContext(null, sessionHolder, rec, null))
            {
                ctx.traceExecution = true;
                ctx.reset(null);
                ctx.evaluate(1000, (stack, line) -> System.out.printf("Log: %s\n", line));
                return ctx.steps;
            }
        }
    }

    private AlertDefinitionVersionRecord loadAlert(String path) throws
                                                                Exception
    {
        String alertRaw = loadResourceAsText(path, true);
        ObjectMapper mapper = applicationRule.getApplication()
                                             .getServiceNonNull(ObjectMapper.class);

        AlertDefinitionImportExport importExport = mapper.readValue(alertRaw, AlertDefinitionImportExport.class);

        AlertDefinitionRecord        rec_definition;
        AlertDefinitionVersionRecord rec_version;

        try (SessionHolder sessionHolder = applicationRule.openSessionWithTransaction())
        {
            RecordHelper<AlertDefinitionRecord>        definitionHelper = sessionHolder.createHelper(AlertDefinitionRecord.class);
            RecordHelper<AlertDefinitionVersionRecord> versionHelper    = sessionHolder.createHelper(AlertDefinitionVersionRecord.class);

            rec_definition = AlertDefinitionRecord.newInstance(importExport.definition.purpose);
            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, importExport.definition, rec_definition);

            definitionHelper.persist(rec_definition);

            AlertDefinitionVersion version = new AlertDefinitionVersion();
            version.details = new ModelSanitizerContext.Simple(sessionHolder).processTyped(importExport.details);

            rec_version = AlertDefinitionVersionRecord.newInstance(versionHelper, version, rec_definition, null, null);

            sessionHolder.commit();
        }

        return rec_version;
    }

    private void dumpSteps(List<AlertEngineExecutionStep> steps)
    {
        System.out.println(ObjectMappers.prettyPrintAsJson(steps));
    }
}
