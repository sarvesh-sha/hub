/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class UnaryConditionalJumpInstruction extends ConditionalJumpInstruction
{
    public UnaryConditionalJumpInstruction(int opcode,
                                           Type type,
                                           BasicBlock defaultTarget,
                                           BasicBlock successTarget)
    {
        super(opcode, type, defaultTarget, successTarget);
    }

    private UnaryConditionalJumpInstruction(UnaryConditionalJumpInstruction source,
                                            ControlFlowGraph.Cloner cloner) throws
                                                                            AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new UnaryConditionalJumpInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return operandType.getSize();
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, operandType); // condition

        return output;
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" => TRUE:");
        sb.append(successTarget.getSequenceNumber());
        sb.append(" => FALSE:");
        sb.append(defaultTarget.getSequenceNumber());
    }
}
