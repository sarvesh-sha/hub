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

public abstract class AbstractArrayInstruction extends AbstractInstruction
{
    public final Type arrayType;

    protected AbstractArrayInstruction(int opcode,
                                       Type arrayType)
    {
        super(opcode);

        this.arrayType = arrayType;
    }

    protected AbstractArrayInstruction(AbstractArrayInstruction source,
                                       ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        arrayType = source.arrayType;
    }

    //--//

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(arrayType);
    }

    //--//

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("arrayType", arrayType);
    }
}
