/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class ThrowInstruction extends AbstractFlowControlInstruction
{
    public ThrowInstruction()
    {
        super(Opcodes.ATHROW);
    }

    private ThrowInstruction(ThrowInstruction source,
                             ControlFlowGraph.Cloner cloner) throws
                                                             AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ThrowInstruction(this, cloner);
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

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, TypeResolver.TypeForThrowable); // Exception

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitOpcode(this, Opcodes.ATHROW);
    }

    @Override
    public void accept(BasicBlockVisitor bbv)
    {
        // No target to visit.
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
    }
}
