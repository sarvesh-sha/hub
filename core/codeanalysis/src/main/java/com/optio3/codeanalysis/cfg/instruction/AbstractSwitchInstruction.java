/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class AbstractSwitchInstruction extends AbstractJumpInstruction
{
    public final Map<Integer, BasicBlock> switchLookup = Maps.newHashMap();

    protected AbstractSwitchInstruction(int opcode,
                                        BasicBlock defaultTarget)
    {
        super(opcode, defaultTarget);
    }

    protected AbstractSwitchInstruction(AbstractSwitchInstruction source,
                                        ControlFlowGraph.Cloner cloner) throws
                                                                        AnalyzerException
    {
        super(source, cloner);

        for (Integer index : source.switchLookup.keySet())
        {
            BasicBlock cloned = cloner.clone(source.switchLookup.get(index));
            switchLookup.put(index, cloned);
        }
    }

    //--//

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        pop(typeResolver, output, Type.INT_TYPE); // key

        return output;
    }

    @Override
    public final void accept(TypeVisitor visitor)
    {
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitSwitch(this, opcode, switchLookup, defaultTarget);
    }

    //--//

    @Override
    public void accept(BasicBlockVisitor bbv)
    {
        super.accept(bbv);

        for (BasicBlock to : switchLookup.values())
            bbv.visitTarget(to);
    }

    //--//

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        if (!switchLookup.isEmpty())
        {
            try (JsonBuilder sub = json.newArray("switchLookup"))
            {
                for (Integer key : switchLookup.keySet())
                {
                    try (JsonBuilder sub2 = sub.newObject(null))
                    {
                        sub2.newField("key", key);
                        sub2.newField("target",
                                      switchLookup.get(key)
                                                  .getSequenceNumber());
                    }
                }
            }
        }
    }
}
