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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class ArrayNewMultiInstruction extends AbstractArrayInstruction
{
    public final int dimensions;

    public ArrayNewMultiInstruction(Type arrayType,
                                    int dimensions)
    {
        super(Opcodes.MULTIANEWARRAY, arrayType);

        this.dimensions = dimensions;
    }

    private ArrayNewMultiInstruction(ArrayNewMultiInstruction source,
                                     ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        super(source, cloner);

        dimensions = source.dimensions;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ArrayNewMultiInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return dimensions;
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

        for (int i = 0; i < dimensions; i++)
        {
            pop(typeResolver, output, Type.INT_TYPE); // Size for Nth dimension
        }

        push(output, arrayType, this);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitArrayNew(this, arrayType, dimensions);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" ");
        sb.append(arrayType.getClassName());
        sb.append(" ");
        sb.append(dimensions);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("dimensions", dimensions);
    }
}
