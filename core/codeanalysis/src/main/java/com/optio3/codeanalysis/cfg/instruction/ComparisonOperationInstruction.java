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

public final class ComparisonOperationInstruction extends AbstractInstruction
{
    public final Type inputType;

    public ComparisonOperationInstruction(int opcode) throws
                                                      AnalyzerException
    {
        super(opcode);

        this.inputType = decodeInput(opcode);
    }

    private ComparisonOperationInstruction(ComparisonOperationInstruction source,
                                           ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        inputType = source.inputType;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ComparisonOperationInstruction(this, cloner);
    }

    //--//

    public static Type decodeInput(int opcode) throws
                                               AnalyzerException
    {
        switch (opcode)
        {
            case Opcodes.LCMP:
                return Type.LONG_TYPE;

            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
                return Type.FLOAT_TYPE;

            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return Type.DOUBLE_TYPE;

            default:
                throw TypeResolver.reportProblem("Not a conversion opcode: 0x%02x", opcode & 0xFF);
        }
    }

    //--//

    @Override
    public int popSize()
    {
        return inputType.getSize() * 2;
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

        pop(typeResolver, output, inputType); // value2
        pop(typeResolver, output, inputType); // value1

        push(output, Type.INT_TYPE, this); // result

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(inputType);
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
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("inputType", inputType.toString());
    }
}
