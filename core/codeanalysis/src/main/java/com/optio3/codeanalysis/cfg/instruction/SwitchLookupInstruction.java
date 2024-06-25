/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class SwitchLookupInstruction extends AbstractSwitchInstruction
{
    public SwitchLookupInstruction(BasicBlock defaultTarget)
    {
        super(Opcodes.LOOKUPSWITCH, defaultTarget);
    }

    private SwitchLookupInstruction(SwitchLookupInstruction source,
                                    ControlFlowGraph.Cloner cloner) throws
                                                                    AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new SwitchLookupInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 1;
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" => ");
        sb.append(defaultTarget.getSequenceNumber());
        sb.append(" {");

        boolean first = true;
        for (Integer key : switchLookup.keySet())
        {
            if (!first)
            {
                sb.append(", ");
            }

            sb.append(key);
            sb.append(" -> ");
            sb.append(switchLookup.get(key)
                                  .getSequenceNumber());

            first = false;
        }
        sb.append("}");
    }
}
