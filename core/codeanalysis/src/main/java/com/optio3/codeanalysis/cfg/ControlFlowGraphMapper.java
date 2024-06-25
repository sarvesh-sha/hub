/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Map;

import com.google.common.collect.Maps;

public class ControlFlowGraphMapper
{
    public final ControlFlowGraph sourceCfg;
    public final ControlFlowGraph targetCfg;

    private final Map<BasicBlock, BasicBlock>                   m_basicBlocks         = Maps.newHashMap();
    private final Map<AbstractInstruction, AbstractInstruction> m_instructions        = Maps.newHashMap();
    private final Map<TryCatchHandler, TryCatchHandler>         m_exceptions          = Maps.newHashMap();
    private final Map<LocalVariable, LocalVariable>             m_variables           = Maps.newHashMap();
    private final Map<SourceCodeVariable, SourceCodeVariable>   m_sourceCodeVariables = Maps.newHashMap();

    ControlFlowGraphMapper(ControlFlowGraph sourceCfg,
                           ControlFlowGraph targetCfg)
    {
        this.sourceCfg = sourceCfg;
        this.targetCfg = targetCfg;
    }

    //--//

    public BasicBlock get(BasicBlock bb)
    {
        if (bb == null)
        {
            return null;
        }

        return m_basicBlocks.get(bb);
    }

    void put(BasicBlock bb,
             BasicBlock bbCloned)
    {
        m_basicBlocks.put(bb, bbCloned);
    }

    public AbstractInstruction get(AbstractInstruction insn)
    {
        if (insn == null)
        {
            return null;
        }

        return m_instructions.get(insn);
    }

    public <T extends AbstractInstruction> T get(Class<T> clz,
                                                 T insn)
    {
        return clz.cast(get(insn));
    }

    void put(AbstractInstruction insn,
             AbstractInstruction insnCloned)
    {
        m_instructions.put(insn, insnCloned);
    }

    public TryCatchHandler get(TryCatchHandler tch)
    {
        if (tch == null)
        {
            return null;
        }

        return m_exceptions.get(tch);
    }

    void put(TryCatchHandler tch,
             TryCatchHandler tchCloned)
    {
        m_exceptions.put(tch, tchCloned);
    }

    public SourceCodeVariable get(SourceCodeVariable scv)
    {
        if (scv == null)
        {
            return null;
        }

        return m_sourceCodeVariables.get(scv);
    }

    void put(SourceCodeVariable scv,
             SourceCodeVariable scvCloned)
    {
        m_sourceCodeVariables.put(scv, scvCloned);
    }

    public LocalVariable get(LocalVariable var)
    {
        if (var == null)
        {
            return null;
        }

        return m_variables.get(var);
    }

    void put(LocalVariable var,
             LocalVariable varCloned)
    {
        m_variables.put(var, varCloned);
    }
}
