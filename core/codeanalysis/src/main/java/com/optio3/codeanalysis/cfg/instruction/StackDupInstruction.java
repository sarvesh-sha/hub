/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.util.Printer;

public final class StackDupInstruction extends StackInstruction
{
    public static int getOpcodeFor(int slots,
                                   int depth) throws
                                              AnalyzerException
    {
        if (slots == 1 && depth == 0)
        {
            return Opcodes.DUP;
        }

        if (slots == 1 && depth == 1)
        {
            return Opcodes.DUP_X1;
        }

        if (slots == 1 && depth == 2)
        {
            return Opcodes.DUP_X2;
        }

        if (slots == 2 && depth == 0)
        {
            return Opcodes.DUP2;
        }

        if (slots == 2 && depth == 1)
        {
            return Opcodes.DUP2_X1;
        }

        if (slots == 2 && depth == 2)
        {
            return Opcodes.DUP2_X2;
        }

        throw TypeResolver.reportProblem("Illegal StackDupInstruction input: slots=%d depth=%d", slots, depth);
    }

    //--//

    public final int slots;

    public final int depth;

    public StackDupInstruction() throws
                                 AnalyzerException
    {
        this(1, 0);
    }

    public StackDupInstruction(int slots,
                               int depth) throws
                                          AnalyzerException
    {
        super(getOpcodeFor(slots, depth));

        this.slots = slots;
        this.depth = depth;
    }

    private StackDupInstruction(StackDupInstruction source,
                                ControlFlowGraph.Cloner cloner) throws
                                                                AnalyzerException
    {
        super(source, cloner);

        slots = source.slots;
        depth = source.depth;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new StackDupInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return slots + depth;
    }

    @Override
    public int pushSize()
    {
        return 2 * slots + depth;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        //
        // dup     :                         value  ->                                  value, value     IF value  == size-1.
        // dup_x1  :                 value2, value1 ->                         value1, value2, value1    IF value1 == size-1, value2 == size-1.
        // dup_x2  :         value3, value2, value1 ->                 value1, value3, value2, value1    IF value1 == size-1, value2 == size-1, value3 == size-1.
        // dup_x2  :                 value2, value1 ->                         value1, value2, value1    IF value1 == size-1, value2 == size-2.
        // dup2    :                 value2, value1 ->                 value2, value1, value2, value1    IF value1 == size-1, value2 == size-1.
        // dup2    :                         value  ->                                  value, value     IF value  == size-2.
        // dup2_x1 :         value3, value2, value1 ->         value2, value1, value3, value2, value1    IF value1 == size-1, value2 == size-1, value3 == size-1.
        // dup2_x1 :                 value2, value1 ->                         value1, value2, value1    IF value1 == size-2, value2 == size-1.
        // dup2_x2 : value4, value3, value2, value1 -> value2, value1, value4, value3, value2, value1    IF value1 == size-1, value2 == size-1, value3 == size-1, value4 == size-1
        // dup2_x2 :         value3, value2, value1 ->                 value1, value3, value2, value1    IF value1 == size-2, value2 == size-1, value3 == size-1
        // dup2_x2 :         value3, value2, value1 ->         value2, value1, value3, value2, value1    IF value1 == size-1, value2 == size-1, value3 == size-2
        // dup2_x2 :                 value2, value1 ->                         value1, value2, value1    IF value1 == size-2, value2 == size-2.
        //        

        FrameState output = new FrameState(input);

        List<FrameValue> topStack    = Lists.newArrayList();
        List<FrameValue> bottomStack = Lists.newArrayList();

        int poppedSlots = 0;
        while (poppedSlots < slots)
        {
            FrameValue v = pop(output, slots - poppedSlots);
            poppedSlots += v.getSize();
            topStack.add(0, v);
        }

        int poppedDepth = 0;
        while (poppedDepth < depth)
        {
            FrameValue v = pop(output, depth - poppedDepth);
            poppedDepth += v.getSize();
            bottomStack.add(0, v);
        }

        for (FrameValue v : topStack)
            push(output, v);

        for (FrameValue v : bottomStack)
            push(output, v);

        for (FrameValue v : topStack)
            push(output, v);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitStackDup(this, slots, depth);
    }

    //--//

    private FrameValue pop(FrameState sf,
                           int maxSlots) throws
                                         AnalyzerException
    {
        FrameValue v = pop(sf);

        int size = v.getSize();
        if (size == 0 || size > maxSlots)
        {
            throw prepareStackException();
        }

        return v;
    }

    private AnalyzerException prepareStackException()
    {
        return TypeResolver.reportProblem("Illegal use of %s", Printer.OPCODES[opcode]);
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
        json.newField("depth", depth);
    }
}
