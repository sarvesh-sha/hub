/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlockVisitor;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;

public abstract class AbstractFlowControlInstruction extends AbstractInstruction
{
    protected AbstractFlowControlInstruction(int opcode)
    {
        super(opcode);
    }

    protected AbstractFlowControlInstruction(AbstractFlowControlInstruction source,
                                             ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);
    }

    //--//

    public abstract void accept(BasicBlockVisitor bbv);
}
