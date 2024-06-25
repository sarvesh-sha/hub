/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class InvokeInstruction extends AbstractMethodInstruction
{
    public final Type owner;

    public final boolean isInterface;

    public InvokeInstruction(int opcode,
                             MethodAnalyzer ma,
                             boolean isInterface)
    {
        this(opcode, ma.genericMethodInfo, isInterface);
    }

    public InvokeInstruction(int opcode,
                             GenericMethodInfo gmi,
                             boolean isInterface)
    {
        this(opcode,
             gmi.getDeclaringGenericType()
                .asType(),
             gmi.getName(),
             gmi.getSignature(),
             isInterface);
    }

    public InvokeInstruction(int opcode,
                             Type owner,
                             String name,
                             GenericType.MethodDescriptor signature,
                             boolean isInterface)
    {
        super(opcode, name, signature, opcode == Opcodes.INVOKESTATIC);

        this.owner = owner;
        this.isInterface = isInterface;
    }

    private InvokeInstruction(InvokeInstruction source,
                              ControlFlowGraph.Cloner cloner) throws
                                                              AnalyzerException
    {
        super(source, cloner);

        owner = source.owner;
        isInterface = source.isInterface;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new InvokeInstruction(this, cloner);
    }

    //--/

    @Override
    public boolean isCallTo(GenericMethodInfo gmi)
    {
        if (!super.isCallTo(gmi))
        {
            return false;
        }

        if (!owner.equals(gmi.getDeclaringGenericType()
                             .asType()))
        {
            return false;
        }

        return true;
    }

    //--//

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        Type[] argumentTypes = getArgumentTypes();
        for (int i = argumentTypes.length; i-- > 0; )
        {
            Type       arg = argumentTypes[i];
            FrameValue v   = pop(typeResolver, output, arg); // value for Nth argument
            if (v.isUninitialized())
            {
                throw TypeResolver.reportProblem("Detected uninitialized value getting used in a method invocation");
            }
        }

        if (!isStatic)
        {
            FrameValue v          = pop(typeResolver, output, owner); // objectref
            boolean    isInitCall = (opcode == Opcodes.INVOKESPECIAL && name.equals("<init>"));

            if (v.isUninitialized())
            {
                if (isInitCall)
                {
                    FrameValue vInit = FrameValue.create(v.getType(), this);

                    output.replaceLocalValue(v, vInit);
                    output.replaceStackValue(v, vInit);
                }
                else
                {
                    throw TypeResolver.reportProblem("Detected uninitialized value getting used in a method invocation");
                }
            }
            else
            {
                if (isInitCall)
                {
                    throw TypeResolver.reportProblem("Detected initialized value getting passed to an <init> method");
                }
            }
        }

        Type returnType = getReturnType();
        if (returnType.getSize() != 0)
        {
            push(output, returnType, this); // result
        }

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
        super.accept(visitor);

        visitor.visitType(owner);
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitInvoke(this, opcode, owner, name, signature, isInterface);
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
        sb.append(" (");
        Type[] argumentTypes = getArgumentTypes();
        for (int i = 0; i < argumentTypes.length; i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }

            sb.append(argumentTypes[i].getClassName());
        }
        sb.append(") => ");
        sb.append(getReturnType().getClassName());
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("owner", owner);
        json.newField("isInterface", isInterface);
    }
}
