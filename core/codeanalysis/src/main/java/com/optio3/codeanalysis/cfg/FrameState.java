/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collections;
import java.util.EmptyStackException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger.Level;
import org.objectweb.asm.Type;

public class FrameState
{
    private final Map<Integer, FrameValue> m_localVars;

    private final List<FrameValue> m_stackVars;

    private boolean m_frozen;

    //--//

    public FrameState()
    {
        m_localVars = Maps.newHashMap();
        m_stackVars = Lists.newArrayList();
    }

    public FrameState(FrameState source)
    {
        m_localVars = Maps.newHashMap(source.m_localVars);
        m_stackVars = Lists.newArrayList(source.m_stackVars);
    }

    //--//

    public void freeze()
    {
        m_frozen = true;
    }

    public LocalVariable getLocalAsVariable(Integer index)
    {
        FrameValue fv = getLocal(index);
        return new LocalVariable(index, fv.getType());
    }

    public Set<Integer> getLocals()
    {
        return m_localVars.keySet();
    }

    public FrameValue getLocal(Integer index)
    {
        return m_localVars.get(index);
    }

    public void setLocal(Integer index,
                         FrameValue fv)
    {
        ensureNotFrozen();

        m_localVars.put(index, fv);
    }

    public void removeLocal(Integer index)
    {
        ensureNotFrozen();

        m_localVars.remove(index);
    }

    //--//

    public void clearStack()
    {
        ensureNotFrozen();

        m_stackVars.clear();
    }

    public int getStackSlots()
    {
        return m_stackVars.size();
    }

    public List<FrameValue> getStackVars()
    {
        return Collections.unmodifiableList(m_stackVars);
    }

    public FrameValue getStackVar(int index)
    {
        return m_stackVars.get(index);
    }

    public int getStackDepth()
    {
        int depth = 0;
        for (FrameValue v : m_stackVars)
        {
            depth += v.getType()
                      .getSize();
        }
        return depth;
    }

    public FrameValue pop()
    {
        ensureNotFrozen();

        int top = m_stackVars.size() - 1;
        if (top < 0)
        {
            throw new EmptyStackException();
        }

        return m_stackVars.remove(top);
    }

    public void push(FrameValue v)
    {
        ensureNotFrozen();

        m_stackVars.add(v.coerceToInt());
    }

    public void push(Type type,
                     FrameValue forwardedFrom)
    {
        push(FrameValue.create(type, forwardedFrom));
    }

    public void push(Type type,
                     AbstractInstruction insn)
    {
        push(FrameValue.create(type, insn));
    }

    public void replaceLocalValue(FrameValue oldValue,
                                  FrameValue newValue)
    {
        ensureNotFrozen();

        for (Entry<Integer, FrameValue> en : m_localVars.entrySet())
        {
            if (en.getValue() == oldValue)
            {
                en.setValue(newValue);
            }
        }
    }

    public void replaceStackValue(FrameValue oldValue,
                                  FrameValue newValue)
    {
        ensureNotFrozen();

        for (int i = 0; i < m_stackVars.size(); i++)
        {
            if (m_stackVars.get(i) == oldValue)
            {
                m_stackVars.set(i, newValue);
            }
        }
    }

    //--//

    public void setVariable(LocalVariable localVariable,
                            FrameValue v)
    {
        setLocal(localVariable.getIndex(), v);
    }

    public FrameValue getVariable(LocalVariable localVariable)
    {
        return getLocal(localVariable.getIndex());
    }

    //--//

    public void log(CodeAnalysisLogger logger,
                    String context,
                    CodeAnalysisLogger.Level level,
                    String prefix)
    {
        if (!logger.isEnabled(context, level))
        {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prefix);

        List<Integer> list = Lists.newArrayList(m_localVars.keySet());
        if (!list.isEmpty())
        {
            list.sort(Integer::compareTo);
            int maxIndex = list.get(list.size() - 1);

            sb.append(" LOCALS:");
            for (int index = 0; index <= maxIndex; index++)
            {
                FrameValue v = m_localVars.get(index);

                FrameValue.dumpType(sb, v);
            }

            int column = sb.length();
            column = (column + 10);
            column -= column % 10;
            while (sb.length() < column)
            {
                sb.append(' ');
            }

            sb.append(" STACK:");
            for (FrameValue v : m_stackVars)
            {
                FrameValue.dumpType(sb, v);
            }
        }

        logger.log(context, level, sb.toString());
    }

    //--//

    private void ensureNotFrozen()
    {
        if (m_frozen)
        {
            throw new RuntimeException("Frame frozen");
        }
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        CodeAnalysisLogger logger = CodeAnalysisLogger.createCallbackLogger(Level.TRACE, (s) -> sb.append(s));
        log(logger, null, CodeAnalysisLogger.Level.TRACE, "");
        return sb.toString();
    }
}
