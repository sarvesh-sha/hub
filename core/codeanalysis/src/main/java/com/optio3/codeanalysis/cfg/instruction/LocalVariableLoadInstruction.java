/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg.instruction;

import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.LocalVariable;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public final class LocalVariableLoadInstruction extends LocalVariableInstruction
{
    public static int getOpcodeFor(LocalVariable localVar) throws
                                                           AnalyzerException
    {
        switch (localVar.type.getSort())
        {
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                return Opcodes.ILOAD;

            case Type.LONG:
                return Opcodes.LLOAD;

            case Type.FLOAT:
                return Opcodes.FLOAD;

            case Type.DOUBLE:
                return Opcodes.DLOAD;

            case Type.OBJECT:
            case Type.ARRAY:
                return Opcodes.ALOAD;

            default:
                throw TypeResolver.reportProblem("Invalid type for LOAD local variable: %s", localVar.type);
        }
    }

    //--//

    public LocalVariableLoadInstruction(LocalVariable localVar) throws
                                                                AnalyzerException
    {
        super(getOpcodeFor(localVar), localVar);
    }

    private LocalVariableLoadInstruction(LocalVariableLoadInstruction source,
                                         ControlFlowGraph.Cloner cloner) throws
                                                                         AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new LocalVariableLoadInstruction(this, cloner);
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
        return localVariable.type.getSize();
    }

    @Override
    public FrameState processStack(TypeResolver typeResolver,
                                   FrameState input) throws
                                                     AnalyzerException
    {
        FrameState output = new FrameState(input);

        FrameValue v = getVariable(output, localVariable);
        push(output, v); // local Variable

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitLocalVarLoad(this, localVariable);
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
}
