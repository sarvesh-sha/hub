/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.google.common.base.Preconditions;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class ReturnInstruction extends AbstractFlowControlInstruction
{
    public final Type returnType;

    public ReturnInstruction()
    {
        this(Opcodes.RETURN, Type.VOID_TYPE);
    }

    public ReturnInstruction(int opcode,
                             Type returnType)
    {
        super(opcode);

        Preconditions.checkNotNull(returnType);

        this.returnType = returnType;
    }

    private ReturnInstruction(ReturnInstruction source,
                              ControlFlowGraph.Cloner cloner) throws
                                                              AnalyzerException
    {
        super(source, cloner);

        returnType = source.returnType;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ReturnInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return returnType.getSize();
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

        if (returnType.getSize() != 0)
        {
            pop(typeResolver, output, returnType);
        }

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(returnType);
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitOpcode(this, opcode);
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
        sb.append(" {");
        sb.append(returnType.getClassName());
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("returnType", returnType);
    }
}
