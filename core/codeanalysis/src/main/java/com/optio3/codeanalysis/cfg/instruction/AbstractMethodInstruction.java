/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Type;

public abstract class AbstractMethodInstruction extends AbstractInstruction
{
    public final String name;

    public final GenericType.MethodDescriptor signature;

    public final boolean isStatic;

    protected AbstractMethodInstruction(int opcode,
                                        String name,
                                        GenericType.MethodDescriptor signature,
                                        boolean isStatic)
    {
        super(opcode);

        this.name = name;
        this.signature = signature;
        this.isStatic = isStatic;
    }

    protected AbstractMethodInstruction(AbstractMethodInstruction source,
                                        ControlFlowGraph.Cloner cloner)
    {
        super(source, cloner);

        name = source.name;
        signature = source.signature;
        isStatic = source.isStatic;
    }

    //--//

    public boolean isCallTo(GenericMethodInfo gmi)
    {
        if (!name.equals(gmi.getName()))
        {
            return false;
        }

        if (isStatic != gmi.isStatic())
        {
            return false;
        }

        if (!signature.equalsRawTypes(gmi.getSignature()))
        {
            return false;
        }

        return true;
    }

    //--//

    @Override
    public int popSize()
    {
        int size = isStatic ? 0 : 1;

        for (Type arg : getArgumentTypes())
        {
            size += arg.getSize();
        }

        return size;
    }

    @Override
    public int pushSize()
    {
        return getReturnType().getSize();
    }

    public Type getReturnType()
    {
        return signature.getRawReturnType();
    }

    public Type[] getArgumentTypes()
    {
        return signature.getRawParameterTypes();
    }

    //--//

    @Override
    public void accept(TypeVisitor visitor)
    {
        visitor.visitType(getReturnType());

        for (Type t : getArgumentTypes())
            visitor.visitType(t);
    }

    //--//

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("name", name);
        json.newField("isStatic", isStatic);
        json.newField("returnType", getReturnType());

        Type[] argumentTypes = getArgumentTypes();
        if (argumentTypes.length > 0)
        {
            try (JsonBuilder sub = json.newArray("argumentTypes"))
            {
                for (Type arg : argumentTypes)
                    sub.newRawValue(arg);
            }
        }
    }
}
