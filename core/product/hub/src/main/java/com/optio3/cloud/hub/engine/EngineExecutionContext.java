/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine;

import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.engine.core.block.EngineExpressionFunctionCall;
import com.optio3.cloud.hub.engine.core.block.EngineExpressionMemoize;
import com.optio3.cloud.hub.engine.core.block.EngineProcedureDeclaration;
import com.optio3.cloud.hub.engine.core.block.EngineStatementProcedureCall;
import com.optio3.cloud.hub.logic.location.LocationsEngine;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.logic.tags.TagsEngine;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.ILogger;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class EngineExecutionContext<D extends EngineDefinitionDetails, S extends EngineExecutionStep>
{
    @FunctionalInterface
    public interface LogEntry
    {
        void recordLogEntry(EngineExecutionStack stack,
                            String text);
    }

    public final ILogger                   logger;
    public final SessionProvider           sessionProvider;
    public final EngineExecutionProgram<D> program;

    public ZonedDateTime thresholdTimestamp;

    protected ZonedDateTime limitTimestamp(ZonedDateTime timestamp)
    {
        return TimeUtils.min(timestamp, thresholdTimestamp);
    }

    public Map<String, EngineValue> variables = Maps.newHashMap();

    public Map<EngineExpressionMemoize, EngineValue> memoizer = Maps.newHashMap();

    public EngineExecutionStack     topOfStack;

    public boolean traceExecution;
    public List<S> steps = Lists.newArrayList();

    private LogEntry m_logCallback;

    private final Map<String, Pattern> m_lookupRegexCaseSensitive   = Maps.newHashMap();
    private final Map<String, Pattern> m_lookupRegexCaseInsensitive = Maps.newHashMap();

    public final DeliveryOptions.Resolver deliveryOptionsResolver;

    private SamplesCache             m_samplesCache;
    private TagsEngine.Snapshot      m_tagsSnapshot;
    private LocationsEngine.Snapshot m_locationsSnapshot;

    //--//

    protected EngineExecutionContext(ILogger logger,
                                     SessionProvider sessionProvider,
                                     EngineExecutionProgram<D> program)
    {
        this.logger          = logger != null ? logger : HubApplication.LoggerInstance;
        this.sessionProvider = sessionProvider;
        this.program         = program;

        deliveryOptionsResolver = new DeliveryOptions.Resolver(sessionProvider);
    }

    //--//

    public boolean hasThread()
    {
        return program.mainThread != null;
    }

    public void reset(ZonedDateTime when)
    {
        variables.clear();
        steps.clear();

        thresholdTimestamp = when != null ? when : TimeUtils.now();
        topOfStack         = null;

        if (program.mainThread != null)
        {
            pushBlock(program.mainThread);
        }
    }

    public boolean evaluate(int maxSteps,
                            LogEntry callback)
    {
        try
        {
            m_logCallback = callback;

            while (topOfStack != null)
            {
                if (maxSteps-- <= 0)
                {
                    return false;
                }

                EngineBlock block = topOfStack.block;

                try
                {
                    block.advanceExecution(this, topOfStack);
                }
                catch (final Throwable t)
                {
                    addFailureStep(block, t);
                    break;
                }
            }

            return true;
        }
        finally
        {
            m_logCallback = null;
        }
    }

    private void addFailureStep(EngineBlock block,
                                Throwable t)
    {
        String message;

        EngineExecutionStep step = newStep();
        step.failureDetailed = t;

        while (StringUtils.isEmpty(message = t.getMessage()))
        {
            Throwable t2 = t.getCause();
            if (t2 != null)
            {
                t = t2;
                continue;
            }

            message = t.getClass()
                       .getSimpleName();
            break;
        }

        step.failure = String.format("%s : %s", block, message);
    }

    //--//

    private EngineProcedureDeclaration resolveFunction(String functionId)
    {
        EngineProcedureDeclaration func = program.functionLookup.get(functionId);
        if (func == null)
        {
            throw Exceptions.newRuntimeException("Evaluation Error: no function with id '%s'", functionId);
        }

        return func;
    }

    public void pushCall(EngineExpressionFunctionCall block)
    {
        topOfStack.expectedResult = block.resultType;

        if (block.resolvedFunction == null)
        {
            block.resolvedFunction = resolveFunction(block.functionId);
        }

        pushCall(block.resolvedFunction, block.functionId);
    }

    public void pushCall(EngineStatementProcedureCall block)
    {
        if (block.resolvedFunction == null)
        {
            block.resolvedFunction = resolveFunction(block.functionId);
        }

        pushCall(block.resolvedFunction, block.functionId);
    }

    private void pushCall(EngineProcedureDeclaration func,
                          String functionId)
    {
        pushBlock(func);
        topOfStack.functionId = functionId;
    }

    public void pushBlock(EngineBlock block)
    {
        if (block == null)
        {
            throw topOfStack.unexpected("No required input block for %s", topOfStack.block);
        }

        EngineExpression<?> expr = Reflection.as(block, EngineExpression.class);
        if (expr != null)
        {
            topOfStack.expectedResult = expr.resultType;
        }

        EngineExecutionStack newStack = new EngineExecutionStack();

        newStack.parent = topOfStack;
        newStack.block  = block;
        topOfStack      = newStack;

        if (traceExecution)
        {
            EngineExecutionStep step = newStep();
            step.enteringBlockId = topOfStack.block.id;
        }
    }

    public void popBlock()
    {
        popBlockInner();

        if (topOfStack != null && topOfStack.expectedResult != null)
        {
            throw Exceptions.newRuntimeException("Evaluation Error: Block '%s' : expecting expression of type '%s', got nothing", topOfStack.block.id, getTypeName(topOfStack.expectedResult));
        }
    }

    public void popBlock(EngineValue value)
    {
        popBlockInner();

        if (topOfStack == null)
        {
            throw Exceptions.newRuntimeException("Evaluation Error: no parent stack");
        }

        if (topOfStack.expectedResult == null)
        {
            throw Exceptions.newRuntimeException("Evaluation Error: Block '%s' : unexpected expression of type '%s'", topOfStack.block.id, getValueTypeName(value));
        }

        if (value != null && !topOfStack.expectedResult.isInstance(value))
        {
            throw Exceptions.newRuntimeException("Evaluation Error: Block '%s' : expecting expression of type '%s', got expression of type '%s'",
                                                 topOfStack.block.id,
                                                 getTypeName(topOfStack.expectedResult),
                                                 getValueTypeName(value));
        }

        topOfStack.expectedResult = null;

        if (topOfStack.childResults == null)
        {
            topOfStack.childResults = new LinkedList<>();
        }

        topOfStack.childResults.add(value);
    }

    private void popBlockInner()
    {
        if (topOfStack == null)
        {
            throw Exceptions.newRuntimeException("Evaluation Error: no top of stack");
        }

        if (traceExecution)
        {
            EngineExecutionStep step = newStep();
            step.leavingBlockId = topOfStack.block.id;
        }

        topOfStack = topOfStack.parent;
    }

    public String getValueTypeName(Object value)
    {
        return getTypeName(value != null ? value.getClass() : null);
    }

    public String getTypeName(Class<?> clz)
    {
        if (clz == null)
        {
            clz = Object.class;
        }

        return clz.getSimpleName();
    }

    public void popFunction()
    {
        popNonFunction();

        popBlock();
    }

    public void backToLoop(boolean shouldBreak)
    {
        while (topOfStack != null)
        {
            if (topOfStack.block.handleLoopRequest(this, topOfStack, shouldBreak))
            {
                break;
            }
        }
    }

    public void popFunction(EngineValue value)
    {
        popNonFunction();

        popBlock(value);
    }

    private void popNonFunction()
    {
        while (topOfStack != null && topOfStack.functionId == null)
        {
            popBlockInner();
        }
    }

    //--//

    public void assignVariable(EngineVariableReference variable,
                               EngineValue value)
    {
        if (traceExecution)
        {
            EngineExecutionStep step = newStep();
            step.assignment       = new EngineExecutionAssignment();
            step.assignment.name  = variable.name;
            step.assignment.value = value;
        }

        String                   name = variable.name;
        Map<String, EngineValue> map  = variables;

        // Handle local variables.
        for (EngineExecutionStack stack = topOfStack; stack != null; stack = stack.parent)
        {
            Map<String, EngineValue> localMap = stack.localVariablesSetter;
            if (localMap != null && localMap.containsKey(name))
            {
                map = localMap;
                break;
            }
        }

        map.put(name, value);
    }

    public EngineValue getVariable(EngineVariableReference variable)
    {
        String                   name = variable.name;
        Map<String, EngineValue> map  = variables;

        // Handle local variables.
        for (EngineExecutionStack stack = topOfStack; stack != null; stack = stack.parent)
        {
            Map<String, EngineValue> localMap = stack.localVariablesGetter;
            if (localMap != null && localMap.containsKey(name))
            {
                map = localMap;
                break;
            }
        }

        return map.get(name);
    }

    //--//

    public void pushStep(S step)
    {
        steps.add(step);
        step.sequenceNumber = steps.size();
    }

    public <T extends S> T findStep(Class<T> clz,
                                    Predicate<T> callback)
    {
        for (int i = steps.size(); i-- > 0; )
        {
            S step = steps.get(i);
            if (clz.isInstance(step))
            {
                T res = clz.cast(step);

                if (callback == null || callback.test(res))
                {
                    return res;
                }
            }
        }

        return null;
    }

    private S newStep()
    {
        S step = allocateStep();
        pushStep(step);
        return step;
    }

    protected abstract S allocateStep();

    //--//

    public SamplesCache getSamplesCache()
    {
        if (m_samplesCache == null)
        {
            m_samplesCache = sessionProvider.getServiceNonNull(SamplesCache.class);
        }

        return m_samplesCache;
    }

    public TagsEngine.Snapshot getTagsEngineSnapshot()
    {
        if (m_tagsSnapshot == null)
        {
            m_tagsSnapshot = sessionProvider.getServiceNonNull(TagsEngine.class)
                                            .acquireSnapshot(false);
        }

        return m_tagsSnapshot;
    }

    public LocationsEngine.Snapshot getLocationsEngineSnapshot()
    {
        if (m_locationsSnapshot == null)
        {
            m_locationsSnapshot = sessionProvider.getServiceNonNull(LocationsEngine.class)
                                                 .acquireSnapshot(false);
        }

        return m_locationsSnapshot;
    }

    //--//

    public Pattern compileRegex(String regex,
                                boolean caseSensitive)
    {
        Map<String, Pattern> map = caseSensitive ? m_lookupRegexCaseSensitive : m_lookupRegexCaseInsensitive;

        Pattern pattern = map.get(regex);
        if (pattern == null)
        {
            pattern = Pattern.compile(regex, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE);
            map.put(regex, pattern);
        }

        return pattern;
    }

    public String format(EngineExecutionStack stack,
                         String format,
                         LinkedList<EngineValue> arguments)
    {

        StringBuilder       sb        = new StringBuilder();
        Map<String, String> modifiers = Maps.newHashMap();

        int pos = 0;
        while (pos < format.length())
        {
            int modifierStart = StringUtils.indexOf(format, "${", pos);
            if (modifierStart < 0)
            {
                sb.append(format, pos, format.length());
                break;
            }

            int modifierEnd = StringUtils.indexOf(format, "}", pos);
            if (modifierEnd < 0)
            {
                pos = modifierStart + 2;
                continue;
            }

            sb.append(format, pos, modifierStart);
            pos = modifierEnd + 1;

            String   modifier = format.substring(modifierStart + 2, modifierEnd);
            String[] parts    = StringUtils.split(modifier, '|');
            int      index    = Integer.parseInt(parts[0]);

            EngineValue argument = CollectionUtils.getNthElement(arguments, index);
            if (argument != null)
            {
                modifiers.clear();
                for (int i = 1; i < parts.length; i++)
                {
                    String part = parts[i];
                    int    sep  = part.indexOf('=');
                    if (sep > 0)
                    {
                        modifiers.put(part.substring(0, sep), part.substring(sep + 1));
                    }
                }

                String formattedValue = argument.format(this, stack, modifiers);
                if (formattedValue != null)
                {
                    sb.append(formattedValue);
                }
            }
        }

        return sb.toString();
    }

    public final boolean isLogEnabled()
    {
        return m_logCallback != null;
    }

    public final void recordLogEntry(EngineExecutionStack stack,
                                     String line)
    {
        if (m_logCallback != null)
        {
            m_logCallback.recordLogEntry(stack, line);
        }
    }
}
