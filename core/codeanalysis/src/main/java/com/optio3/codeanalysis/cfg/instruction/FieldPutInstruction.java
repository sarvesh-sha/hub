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
import com.optio3.codeanalysis.cfg.FrameValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class FieldPutInstruction extends AbstractFieldInstruction
{
    public FieldPutInstruction(FieldAnalyzer fa)
    {
        this(fa.declaringClass.genericTypeInfo.asType(),
             fa.getName(),
             fa.getType()
               .asRawType(),
             fa.isStatic());
    }

    public FieldPutInstruction(Type owner,
                               String name,
                               Type fieldType,
                               boolean isStatic)
    {
        super(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, owner, name, fieldType, isStatic);
    }

    private FieldPutInstruction(FieldPutInstruction source,
                                ControlFlowGraph.Cloner cloner) throws
                                                                AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new FieldPutInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return type.getSize() + (isStatic ? 0 : 1);
    }

    @Override
    public int pushSize()
    {
        return 0;
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        FrameValue v = pop(typeResolver, output, type); // value
        if (v.isUninitialized())
        {
            throw TypeResolver.reportProblem("Detected uninitialized value ('%s') getting stored in a field '%s'", v.getType(), this);
        }

        if (!isStatic)
        {
            pop(typeResolver, output, owner); // objectref
        }

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
