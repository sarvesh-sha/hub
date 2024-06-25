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

public final class BinaryOperationInstruction extends AbstractTypedInstruction
{
    public BinaryOperationInstruction(int opcode) throws
                                                  AnalyzerException
    {
        super(opcode, decodeType(opcode));
    }

    private BinaryOperationInstruction(BinaryOperationInstruction source,
                                       ControlFlowGraph.Cloner cloner) throws
                                                                       AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new BinaryOperationInstruction(this, cloner);
    }

    //--//

    public static Type decodeType(int opcode) throws
                                              AnalyzerException
    {
        switch (opcode)
        {
            case Opcodes.IADD:
            case Opcodes.ISUB:
            case Opcodes.IMUL:
            case Opcodes.IDIV:
            case Opcodes.IREM:

            case Opcodes.ISHL:
            case Opcodes.ISHR:
            case Opcodes.IUSHR:
            case Opcodes.IAND:
            case Opcodes.IOR:
            case Opcodes.IXOR:
                return Type.INT_TYPE;

            case Opcodes.LADD:
            case Opcodes.LSUB:
            case Opcodes.LMUL:
            case Opcodes.LDIV:
            case Opcodes.LREM:

            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
            case Opcodes.LAND:
            case Opcodes.LOR:
            case Opcodes.LXOR:

            case Opcodes.LCMP:
                return Type.LONG_TYPE;

            case Opcodes.FADD:
            case Opcodes.FSUB:
            case Opcodes.FMUL:
            case Opcodes.FDIV:
            case Opcodes.FREM:

            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                return Type.FLOAT_TYPE;

            case Opcodes.DADD:
            case Opcodes.DSUB:
            case Opcodes.DMUL:
            case Opcodes.DDIV:
            case Opcodes.DREM:

            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return Type.DOUBLE_TYPE;

            default:
                throw TypeResolver.reportProblem("Not a binary operation opcode: 0x%02x", opcode & 0xFF);
        }
    }

    private Type typeRVal()
    {
        switch (opcode)
        {
            case Opcodes.LSHL:
            case Opcodes.LSHR:
            case Opcodes.LUSHR:
                return Type.INT_TYPE;

            default:
                return type;
        }
    }

    //--//

    @Override
    public int popSize()
    {
        return type.getSize() + typeRVal().getSize();
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

        pop(typeResolver, output, typeRVal()); // value2
        pop(typeResolver, output, type); // value1

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
