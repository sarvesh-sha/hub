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
import com.optio3.codeanalysis.cfg.InstructionWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class UnaryOperationInstruction extends AbstractTypedInstruction
{
    public UnaryOperationInstruction(int opcode) throws
                                                 AnalyzerException
    {
        super(opcode, decodeType(opcode));
    }

    private UnaryOperationInstruction(UnaryOperationInstruction source,
                                      ControlFlowGraph.Cloner cloner) throws
                                                                      AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new UnaryOperationInstruction(this, cloner);
    }

    //--//

    public static Type decodeType(int opcode) throws
                                              AnalyzerException
    {
        switch (opcode)
        {
            case Opcodes.INEG:
                return Type.INT_TYPE;

            case Opcodes.LNEG:
                return Type.LONG_TYPE;

            case Opcodes.FNEG:
                return Type.FLOAT_TYPE;

            case Opcodes.DNEG:
                return Type.DOUBLE_TYPE;

            default:
                throw TypeResolver.reportProblem("Not a unary operation opcode: 0x%02x", opcode & 0xFF);
        }
    }

    //--//

    @Override
    public int popSize()
    {
        return type.getSize();
    }

    @Override
    public int pushSize()
    {
        return type.getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, type); // value

        push(output, type, this); // result

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitOpcode(this, opcode);
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
