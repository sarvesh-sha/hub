/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import org.objectweb.asm.tree.LabelNode;

public final class LineNumberInstruction extends MetadataInstruction
{
    public final LabelNode start;

    public final int lineNumber;

    public LineNumberInstruction(LabelNode start,
                                 int lineNumber)
    {
        this.start = start;
        this.lineNumber = lineNumber;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner)
    {
        return null; // Not supported.
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        sb.append(start);
        sb.append(" ");
        sb.append(lineNumber);
    }
}
