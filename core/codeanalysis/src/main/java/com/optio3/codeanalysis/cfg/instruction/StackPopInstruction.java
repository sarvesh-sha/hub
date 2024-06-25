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
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class StackPopInstruction extends StackInstruction
{
    public static int getOpcodeFor(int slots) throws
                                              AnalyzerException
    {
        if (slots == 1)
        {
            return Opcodes.POP;
        }

        if (slots == 2)
        {
            return Opcodes.POP2;
        }

        throw TypeResolver.reportProblem("Illegal StackPopInstruction input: slots=%d", slots);
    }

    //--//
    public final int slots;

    public StackPopInstruction() throws
                                 AnalyzerException
    {
        this(1);
    }

    public StackPopInstruction(int slots) throws
                                          AnalyzerException
    {
        super(getOpcodeFor(slots));

        this.slots = slots;
    }

    private StackPopInstruction(StackPopInstruction source,
                                ControlFlowGraph.Cloner cloner) throws
                                                                AnalyzerException
    {
        super(source, cloner);

        slots = source.slots;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new StackPopInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return slots;
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

        int poppedSlots = 0;
        while (poppedSlots < slots)
        {
            FrameValue v = pop(output);

            int size = v.getSize();
            if (size != 1 && size != 2)
            {
                throw TypeResolver.reportProblem("Illegal use of POP in this frame: %s", input);
            }

            poppedSlots += size;
        }

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitStackPop(this, slots);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("slots", slots);
    }
}
