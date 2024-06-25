/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class AbstractFieldInstruction extends AbstractTypedInstruction
{
    public final Type owner;

    public final String name;

    public final boolean isStatic;

    public AbstractFieldInstruction(int opcode,
                                    Type owner,
                                    String name,
                                    Type fieldType,
                                    boolean isStatic)
    {
        super(opcode, fieldType);

        this.owner = owner;
        this.name = name;
        this.isStatic = isStatic;
    }

    protected AbstractFieldInstruction(AbstractFieldInstruction source,
                                       ControlFlowGraph.Cloner cloner) throws
                                                                       AnalyzerException
    {
        super(source, cloner);

        owner = source.owner;
        name = source.name;
        isStatic = source.isStatic;
    }

    //--//

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitField(this, opcode, owner, name, type, isStatic);
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("owner", owner);
        json.newField("name", name);
        json.newField("isStatic", isStatic);
    }
}
