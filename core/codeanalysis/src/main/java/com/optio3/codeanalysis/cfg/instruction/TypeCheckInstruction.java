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
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class TypeCheckInstruction extends AbstractTypedInstruction
{
    public TypeCheckInstruction(int opcode,
                                Type type)
    {
        super(opcode, type);
    }

    private TypeCheckInstruction(TypeCheckInstruction source,
                                 ControlFlowGraph.Cloner cloner) throws
                                                                 AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new TypeCheckInstruction(this, cloner);
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
        return 1;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        FrameValue v = pop(typeResolver, output, TypeResolver.TypeForObject); // Objectref

        if (opcode == Opcodes.INSTANCEOF)
        {
            push(output, Type.INT_TYPE, this);
        }
        else
        {
            push(output, type, v);
        }

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitTypeInsn(this, opcode, type);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" {");
        sb.append(type.getClassName());
        sb.append("}");
    }
}
