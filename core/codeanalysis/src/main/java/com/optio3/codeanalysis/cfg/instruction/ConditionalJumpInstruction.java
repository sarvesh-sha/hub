/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class ConditionalJumpInstruction extends AbstractJumpInstruction
{
    public final Type       operandType;
    public final BasicBlock successTarget;

    public ConditionalJumpInstruction(int opcode,
                                      Type operandType,
                                      BasicBlock defaultTarget,
                                      BasicBlock successTarget)
    {
        super(opcode, defaultTarget);

        this.operandType = operandType;
        this.successTarget = successTarget;
    }

    protected ConditionalJumpInstruction(ConditionalJumpInstruction source,
                                         ControlFlowGraph.Cloner cloner) throws
                                                                         AnalyzerException
    {
        super(source, cloner);

        operandType = source.operandType;
        successTarget = cloner.clone(source.successTarget);
    }

    //--//

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(operandType);
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitJump(this, opcode, successTarget);
        visitor.visitJump(this, Opcodes.GOTO, defaultTarget);
    }

    @Override
    public void accept(BasicBlockVisitor bbv)
    {
        super.accept(bbv);

        bbv.visitTarget(successTarget);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("successTarget", successTarget.getSequenceNumber());
        json.newField("operandType", operandType);
    }
}
