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
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class UnconditionalJumpInstruction extends AbstractJumpInstruction
{
    public UnconditionalJumpInstruction(BasicBlock target)
    {
        super(Opcodes.GOTO, target);
    }

    private UnconditionalJumpInstruction(UnconditionalJumpInstruction source,
                                         ControlFlowGraph.Cloner cloner) throws
                                                                         AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new UnconditionalJumpInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 0;
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input)
    {
        return input;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitJump(this, opcode, defaultTarget);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" => ");
        sb.append(defaultTarget.getSequenceNumber());
    }
}
