/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class InvokeDynamicInstruction extends AbstractMethodInstruction
{
    public final Handle bsm;

    public final Object[] bsmArgs;

    public InvokeDynamicInstruction(String name,
                                    GenericType.MethodDescriptor signature,
                                    Handle bsm,
                                    Object[] bsmArgs)
    {
        super(Opcodes.INVOKEDYNAMIC, name, signature, true);

        this.bsm = bsm;
        this.bsmArgs = bsmArgs;
    }

    private InvokeDynamicInstruction(InvokeDynamicInstruction source,
                                     ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        super(source, cloner);

        bsm = source.bsm;
        bsmArgs = source.bsmArgs;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new InvokeDynamicInstruction(this, cloner);
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
            Type arg = argumentTypes[i];
            pop(typeResolver, output, arg); // value for Nth argument
        }

        Type returnType = getReturnType();
        if (returnType.getSize() != 0)
        {
            push(output, returnType, this); // result
        }

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor)
    {
        visitor.visitInvokeDynamic(this, name, signature, bsm, bsmArgs);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        toStringOpcode(sb);
        sb.append(" ");
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
        sb.append(" ");
        sb.append(bsm);
        sb.append("{");
        for (int i = 0; i < bsmArgs.length; i++)
        {
            if (i != 0)
            {
                sb.append(", ");
            }

            sb.append(bsmArgs[i]);
        }
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("bsm", bsm);

        if (bsmArgs != null)
        {
            try (JsonBuilder sub = json.newArray("bsmArgs"))
            {
                for (Object arg : bsmArgs)
                    sub.newRawValue(arg);
            }
        }
    }
}
