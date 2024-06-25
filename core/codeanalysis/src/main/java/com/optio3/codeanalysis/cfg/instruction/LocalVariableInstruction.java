/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.LocalVariable;
import com.optio3.codeanalysis.cfg.TypeVisitor;

public abstract class LocalVariableInstruction extends AbstractInstruction
{
    public final LocalVariable localVariable;

    protected LocalVariableInstruction(int opcode,
                                       LocalVariable localVar)
    {
        super(opcode);

        this.localVariable = localVar;
    }

    protected LocalVariableInstruction(LocalVariableInstruction source,
                                       ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        localVariable = cloner.clone(source.localVariable);
    }

    //--//

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(localVariable.type);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" ");
        sb.append(localVariable);
        sb.append(" {");
        sb.append(localVariable.type.getClassName());
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("localVariable", localVariable);
    }
}
