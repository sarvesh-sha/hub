/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.FieldAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class FieldGetInstruction extends AbstractFieldInstruction
{
    public FieldGetInstruction(FieldAnalyzer fa)
    {
        this(fa.declaringClass.genericTypeInfo.asType(),
             fa.getName(),
             fa.getType()
               .asRawType(),
             fa.isStatic());
    }

    public FieldGetInstruction(Type owner,
                               String name,
                               Type fieldType,
                               boolean isStatic)
    {
        super(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner, name, fieldType, isStatic);
    }

    private FieldGetInstruction(FieldGetInstruction source,
                                ControlFlowGraph.Cloner cloner) throws
                                                                AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new FieldGetInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return isStatic ? 0 : 1;
    }

    @Override
    public int pushSize()
    {
        return type.getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        if (!isStatic)
        {
            pop(typeResolver, output, owner); // objectref
        }

        push(output, type, this); // result

        return output;
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" ");
        sb.append(owner.getClassName());
        sb.append(".");
        sb.append(name);
    }
}
