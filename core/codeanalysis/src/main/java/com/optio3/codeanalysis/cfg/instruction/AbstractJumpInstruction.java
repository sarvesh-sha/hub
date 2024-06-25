/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class AbstractJumpInstruction extends AbstractFlowControlInstruction
{
    public final BasicBlock defaultTarget;

    protected AbstractJumpInstruction(int opcode,
                                      BasicBlock defaultTarget)
    {
        super(opcode);

        this.defaultTarget = defaultTarget;
    }

    protected AbstractJumpInstruction(AbstractJumpInstruction source,
                                      ControlFlowGraph.Cloner cloner) throws
                                                                      AnalyzerException
    {
        super(source, cloner);

        defaultTarget = cloner.clone(source.defaultTarget);
    }

    @Override
    public void accept(BasicBlockVisitor bbv)
    {
        bbv.visitTarget(defaultTarget);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("defaultTarget", defaultTarget.getSequenceNumber());
    }
}
