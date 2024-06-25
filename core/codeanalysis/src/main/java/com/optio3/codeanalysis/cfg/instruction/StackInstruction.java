/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph.Cloner;
import com.optio3.codeanalysis.cfg.TypeVisitor;

public abstract class StackInstruction extends AbstractInstruction
{
    protected StackInstruction(int opcode)
    {
        super(opcode);
    }

    protected StackInstruction(StackInstruction source,
                               Cloner cloner)
    {
        super(source, cloner);
    }

    //--//

    @Override
    public final void accept(TypeVisitor visitor)
    {
    }
}
