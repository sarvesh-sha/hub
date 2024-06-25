/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class LoadConstantInstruction extends AbstractTypedInstruction
{
    public static int getOpcodeFor(Object value)
    {
        if (value == null)
        {
            return Opcodes.ACONST_NULL;
        }

        if (value instanceof Integer)
        {
            int val = (int) value;

            if (val >= -1 && val <= 5)
            {
                return Opcodes.ICONST_0 + val;
            }

            if (val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE)
            {
                return Opcodes.BIPUSH;
            }

            if (val >= Short.MIN_VALUE && val <= Short.MAX_VALUE)
            {
                return Opcodes.SIPUSH;
            }
        }

        if (value instanceof Long)
        {
            long val = (long) value;

            if (val == 0)
            {
                return Opcodes.LCONST_0;
            }

            if (val == 1)
            {
                return Opcodes.LCONST_1;
            }
        }

        if (value instanceof Float)
        {
            float val = (float) value;

            if (val == 0)
            {
                return Opcodes.FCONST_0;
            }

            if (val == 1)
            {
                return Opcodes.FCONST_1;
            }

            if (val == 2)
            {
                return Opcodes.FCONST_2;
            }
        }

        if (value instanceof Double)
        {
            double val = (double) value;

            if (val == 0)
            {
                return Opcodes.DCONST_0;
            }

            if (val == 1)
            {
                return Opcodes.DCONST_1;
            }
        }

        return Opcodes.LDC;
    }

    public static Type getTypeFor(Object value) throws
                                                AnalyzerException
    {
        if (value == null)
        {
            return TypeResolver.TypeForNull;
        }

        if (value instanceof Integer)
        {
            return Type.INT_TYPE;
        }

        if (value instanceof Float)
        {
            return Type.FLOAT_TYPE;
        }

        if (value instanceof Long)
        {
            return Type.LONG_TYPE;
        }

        if (value instanceof Double)
        {
            return Type.DOUBLE_TYPE;
        }

        if (value instanceof String)
        {
            return TypeResolver.TypeForString;
        }

        if (value instanceof Type)
        {
            Type cst2 = (Type) value;

            int sort = cst2.getSort();
            if (sort == Type.OBJECT || sort == Type.ARRAY)
            {
                return TypeResolver.TypeForClass;
            }

            if (sort == Type.METHOD)
            {
                return TypeResolver.TypeForMethod;
            }
        }

        if (value instanceof Handle)
        {
            return TypeResolver.TypeForMethodHandle;
        }

        throw TypeResolver.reportProblem("Illegal LDC constant '%s'", value);
    }

    //--//

    public final Object value;

    public LoadConstantInstruction(Object value) throws
                                                 AnalyzerException
    {
        super(getOpcodeFor(value), getTypeFor(value));

        this.value = value;
    }

    private LoadConstantInstruction(LoadConstantInstruction source,
                                    ControlFlowGraph.Cloner cloner) throws
                                                                    AnalyzerException
    {
        super(source, cloner);

        value = source.value;
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new LoadConstantInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return 0;
    }

    @Override
    public int pushSize()
    {
        return type.getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input)
    {
        FrameState output = new FrameState(input);

        push(output, type, this); // constant

        return output;
    }

    @Override
    public void accept(TypeVisitor visitor)
    {
        super.accept(visitor);

        if (value instanceof Type)
        {
            visitor.visitType((Type) value);
        }
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitLoadConstant(this, value);
    }

    //--//

    @Override
    public void toString(StringBuilder sb)
    {
        Object val = value;

        if (val instanceof String)
        {
            String str = (String) val;

            str = str.replace("\n", "\\n");
            str = str.replace("\r", "\\r");
            str = str.replace("\t", "\\t");
            val = '"' + str + '"';
        }

        toStringOpcode(sb);
        sb.append(" ");
        sb.append(val);
        sb.append(" {");
        sb.append(type.getClassName());
        sb.append("}");
    }

    @Override
    public void toString(JsonBuilder json)
    {
        super.toString(json);

        json.newField("value", value);
    }
}
