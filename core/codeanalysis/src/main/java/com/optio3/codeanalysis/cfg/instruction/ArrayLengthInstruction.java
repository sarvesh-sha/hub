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

public final class ArrayLengthInstruction extends AbstractArrayInstruction
{
    public ArrayLengthInstruction()
    {
        super(Opcodes.ARRAYLENGTH, TypeResolver.PlaceholderTypeForArray);
    }

    private ArrayLengthInstruction(ArrayLengthInstruction source,
                                   ControlFlowGraph.Cloner cloner) throws
                                                                   AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new ArrayLengthInstruction(this, cloner);
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
                                   FrameState input)
    {
        FrameState output = new FrameState(input);

        pop(output);
        push(output, Type.INT_TYPE, this);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitOpcode(this, Opcodes.ARRAYLENGTH);
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
