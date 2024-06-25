/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Type;

public abstract class AbstractTypedInstruction extends AbstractInstruction
{
    public final Type type;

    protected AbstractTypedInstruction(int opcode,
                                       Type type)
    {
        super(opcode);

        this.type = type;
    }

    protected AbstractTypedInstruction(AbstractTypedInstruction source,
                                       ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        type = source.type;
    }

    //--//

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(type);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("type", type);
    }
}
