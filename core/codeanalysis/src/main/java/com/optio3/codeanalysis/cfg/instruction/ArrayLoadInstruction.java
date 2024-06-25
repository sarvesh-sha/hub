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

public final class ArrayLoadInstruction extends AbstractArrayInstruction
{
    public static int getOpcodeFor(Type arrayType) throws
                                                   AnalyzerException
    {
        if (arrayType == null || arrayType == TypeResolver.PlaceholderTypeForArray)
        {
            return Opcodes.AALOAD;
        }

        if (TypeResolver.isArray(arrayType))
        {
            Type elementType = TypeResolver.getElementType(arrayType);

            switch (elementType.getSort())
            {
                case Type.BOOLEAN:
                    return Opcodes.BALOAD;

                case Type.CHAR:
                    return Opcodes.CALOAD;

                case Type.BYTE:
                    return Opcodes.BALOAD;

                case Type.SHORT:
                    return Opcodes.SALOAD;

                case Type.INT:
                    return Opcodes.IALOAD;

                case Type.LONG:
                    return Opcodes.LALOAD;

                case Type.FLOAT:
                    return Opcodes.FALOAD;

                case Type.DOUBLE:
                    return Opcodes.DALOAD;

                case Type.OBJECT:
                case Type.ARRAY:
                    return Opcodes.AALOAD;
            }
        }

        throw TypeResolver.reportProblem("Invalid type for ArrayLocal instruction: %s", arrayType);
    }

    public static Type getTypeForOpcode(int opcode)
    {
        switch (opcode)
        {
            case Opcodes.IALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfInteger;

            case Opcodes.LALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfLong;

            case Opcodes.FALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfFloat;

            case Opcodes.DALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfDouble;

            case Opcodes.AALOAD:
                return TypeResolver.PlaceholderTypeForArray;

            case Opcodes.BALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfByte;

            case Opcodes.CALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfChar;

            case Opcodes.SALOAD:
                return TypeResolver.TypeForPrimitiveArrayOfShort;

            default:
                return null;
        }
    }

    //--//

    public ArrayLoadInstruction(Type arrayType) throws
                                                AnalyzerException
    {
        super(getOpcodeFor(arrayType), arrayType);
    }

    private ArrayLoadInstruction(ArrayLoadInstruction source,
                                 ControlFlowGraph.Cloner cloner) throws
                                                                 AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ArrayLoadInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 2; // The Array and the index.
    }

    @Override
    public int pushSize()
    {
        return TypeResolver.getElementType(arrayType)
                           .getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, Type.INT_TYPE); // Index
        FrameValue array = pop(typeResolver, output, TypeResolver.PlaceholderTypeForArray); // Array

        push(output, TypeResolver.getElementType(array.getType()), this);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitArrayLoad(this, arrayType);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" ");
        sb.append(arrayType.getClassName());
    }
}
