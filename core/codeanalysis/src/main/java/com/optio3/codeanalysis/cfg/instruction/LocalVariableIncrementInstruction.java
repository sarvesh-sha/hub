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
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.LocalVariable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class LocalVariableIncrementInstruction extends LocalVariableInstruction
{
    public final int increment;

    public LocalVariableIncrementInstruction(LocalVariable localVar,
                                             int increment)
    {
        super(Opcodes.IINC, localVar);

        this.increment = increment;
    }

    private LocalVariableIncrementInstruction(LocalVariableIncrementInstruction source,
                                              ControlFlowGraph.Cloner cloner) throws
                                                                              AnalyzerException
    {
        super(source, cloner);

        increment = source.increment;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new LocalVariableIncrementInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 0;
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input)
    {
        FrameState output = new FrameState(input);

        FrameValue v = FrameValue.create(localVariable.type, this);
        setVariable(output, localVariable, v);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitLocalVarIncrement(this, localVariable, increment);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        super.toString(sb);
        sb.append(" ");
        sb.append(increment);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("increment", increment);
    }
}
