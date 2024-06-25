/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class StackSwapInstruction extends StackInstruction
{
    public StackSwapInstruction()
    {
        super(Opcodes.SWAP);
    }

    private StackSwapInstruction(StackSwapInstruction source,
                                 ControlFlowGraph.Cloner cloner) throws
                                                                 AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new StackSwapInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 2;
    }

    @Override
    public int pushSize()
    {
        return 2;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        FrameValue value2 = pop(output);
        FrameValue value1 = pop(output);

        if (value1.getSize() != 1 || value2.getSize() != 1)
        {
            throw TypeResolver.reportProblem("Illegal use of SWAP in this frame: %s", input);
        }

        push(output, value2);
        push(output, value1);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitOpcode(this, Opcodes.SWAP);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
    }
}
