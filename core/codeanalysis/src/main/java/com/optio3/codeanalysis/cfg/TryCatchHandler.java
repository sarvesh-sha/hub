/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.objectweb.asm.Type;

public class TryCatchHandler
{
    public final BasicBlock handler;

    public final Type type;

    private final Set<BasicBlock> m_covered = Sets.newHashSet();

    private TryCatchHandler m_outer;

    public TryCatchHandler(BasicBlock handler,
                           Type type)
    {
        this.handler = handler;
        this.type = type;
    }

    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(type);

        if (m_outer != null)
        {
            m_outer.accept(visitor);
        }
    }

    //--//

    public void addToCoveredSet(BasicBlock basicBlock)
    {
        m_covered.add(basicBlock);

        if (m_outer != null)
        {
            m_outer.addToCoveredSet(basicBlock);
        }
    }

    public void removeFromCoveredSet(BasicBlock basicBlock)
    {
        m_covered.remove(basicBlock);

        if (m_outer != null)
        {
            m_outer.removeFromCoveredSet(basicBlock);
        }
    }

    public Set<BasicBlock> getCoveredBasicBlocks()
    {
        return Collections.unmodifiableSet(m_covered);
    }

    public List<BasicBlock> getSortedCoveredBasicBlocks()
    {
        List<BasicBlock> list = Lists.newArrayList(m_covered);
        list.sort((o1, o2) -> o1.getSequenceNumber() - o2.getSequenceNumber());
        return list;
    }

    public TryCatchHandler getOuter()
    {
        return m_outer;
    }

    void setOuter(TryCatchHandler outer)
    {
        m_outer = outer;
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    public void toString(StringBuilder sb)
    {
        sb.append(String.format("EX[%d](%s", handler.getSequenceNumber(), type != null ? type.getClassName() : "<finally>"));

        if (!m_covered.isEmpty())
        {
            List<BasicBlock> list = Lists.newArrayList(m_covered);
            list.sort((o1, o2) -> o1.getSequenceNumber() - o2.getSequenceNumber());

            boolean first = true;
            for (BasicBlock bb : list)
            {
                if (first)
                {
                    sb.append(" for ");
                }
                else
                {
                    sb.append(", ");
                }

                first = false;
                sb.append(String.format("%d", bb.getSequenceNumber()));
            }
        }

        sb.append(")");
    }
}
