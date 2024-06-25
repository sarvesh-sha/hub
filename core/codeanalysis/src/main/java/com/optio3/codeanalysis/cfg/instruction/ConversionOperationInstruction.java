/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class ConversionOperationInstruction extends AbstractInstruction
{
    public final Type inputType;
    public final Type outputType;

    public ConversionOperationInstruction(int opcode) throws
                                                      AnalyzerException
    {
        super(opcode);

        this.inputType = decodeInput(opcode);
        this.outputType = decodeOutput(opcode);
    }

    private ConversionOperationInstruction(ConversionOperationInstruction source,
                                           ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        inputType = source.inputType;
        outputType = source.outputType;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ConversionOperationInstruction(this, cloner);
    }

    //--//

    public static Type decodeInput(int opcode) throws
                                               AnalyzerException
    {
        switch (opcode)
        {
            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                return Type.INT_TYPE;

            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
                return Type.LONG_TYPE;

            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
                return Type.FLOAT_TYPE;

            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
                return Type.DOUBLE_TYPE;

            default:
                throw TypeResolver.reportProblem("Not a conversion opcode: 0x%02x", opcode & 0xFF);
        }
    }

    public static Type decodeOutput(int opcode) throws
                                                AnalyzerException
    {
        switch (opcode)
        {
            case Opcodes.I2B:
                return Type.BYTE_TYPE;

            case Opcodes.I2C:
                return Type.CHAR_TYPE;

            case Opcodes.I2S:
                return Type.SHORT_TYPE;

            case Opcodes.L2I:
            case Opcodes.F2I:
            case Opcodes.D2I:
                return Type.INT_TYPE;

            case Opcodes.I2L:
            case Opcodes.F2L:
            case Opcodes.D2L:
                return Type.LONG_TYPE;

            case Opcodes.I2F:
            case Opcodes.L2F:
            case Opcodes.D2F:
                return Type.FLOAT_TYPE;

            case Opcodes.I2D:
            case Opcodes.L2D:
            case Opcodes.F2D:
                return Type.DOUBLE_TYPE;

            default:
                throw TypeResolver.reportProblem("Not a conversion opcode: 0x%02x", opcode & 0xFF);
        }
    }

    @Override
    public int popSize()
    {
        return inputType.getSize();
    }

    @Override
    public int pushSize()
    {
        return outputType.getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, inputType); // value

        push(output, outputType, this); // result

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(inputType);
        visitor.visitType(outputType);
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
        sb.append(inputType.getClassName());
        sb.append("} => {");
        sb.append(outputType.getClassName());
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("inputType", inputType);
        json.newField("outputType", outputType);
    }
}
