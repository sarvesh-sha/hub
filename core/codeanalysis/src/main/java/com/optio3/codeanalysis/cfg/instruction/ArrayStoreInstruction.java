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

public final class ArrayStoreInstruction extends AbstractArrayInstruction
{
    public static int getOpcodeFor(Type arrayType) throws
                                                   AnalyzerException
    {
        if (arrayType == null || arrayType == TypeResolver.PlaceholderTypeForArray)
        {
            return Opcodes.AASTORE;
        }

        if (TypeResolver.isArray(arrayType))
        {
            Type elementType = TypeResolver.getElementType(arrayType);

            switch (elementType.getSort())
            {
                case Type.BOOLEAN:
                    return Opcodes.BASTORE;

                case Type.CHAR:
                    return Opcodes.CASTORE;

                case Type.BYTE:
                    return Opcodes.BASTORE;

                case Type.SHORT:
                    return Opcodes.SASTORE;

                case Type.INT:
                    return Opcodes.IASTORE;

                case Type.LONG:
                    return Opcodes.LASTORE;

                case Type.FLOAT:
                    return Opcodes.FASTORE;

                case Type.DOUBLE:
                    return Opcodes.DASTORE;

                case Type.OBJECT:
                case Type.ARRAY:
                    return Opcodes.AASTORE;
            }
        }

        throw TypeResolver.reportProblem("Invalid type for ArrayLocal instruction: %s", arrayType);
    }

    public static Type getTypeForOpcode(int opcode)
    {
        switch (opcode)
        {
            case Opcodes.IASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfInteger;

            case Opcodes.LASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfLong;

            case Opcodes.FASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfFloat;

            case Opcodes.DASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfDouble;

            case Opcodes.AASTORE:
                return TypeResolver.PlaceholderTypeForArray;

            case Opcodes.BASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfByte;

            case Opcodes.CASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfChar;

            case Opcodes.SASTORE:
                return TypeResolver.TypeForPrimitiveArrayOfShort;

            default:
                return null;
        }
    }

    //--//

    public ArrayStoreInstruction(Type arrayType) throws
                                                 AnalyzerException
    {
        super(getOpcodeFor(arrayType), arrayType);
    }

    private ArrayStoreInstruction(ArrayStoreInstruction source,
                                  ControlFlowGraph.Cloner cloner) throws
                                                                  AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ArrayStoreInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 2 + TypeResolver.getElementType(arrayType)
                               .getSize(); // The Array, the index, and the value.
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

        FrameValue v = pop(output); // Value
        pop(typeResolver, output, Type.INT_TYPE); // Index
        FrameValue array = pop(typeResolver, output, TypeResolver.PlaceholderTypeForArray); // Array

        Type expectedType = TypeResolver.getElementType(array.getType());
        Type actualType   = v.getType();
        if (!typeResolver.canCastTo(expectedType, actualType))
        {
            throw TypeResolver.reportProblem("Can't store a value of type '%s' into an array of '%s'", actualType, expectedType);
        }

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitArrayStore(this, arrayType);
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
