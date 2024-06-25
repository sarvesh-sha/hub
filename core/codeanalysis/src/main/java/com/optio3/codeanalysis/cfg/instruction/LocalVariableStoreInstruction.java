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

public final class LocalVariableStoreInstruction extends LocalVariableInstruction
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
                return Opcodes.ISTORE;

            case Type.LONG:
                return Opcodes.LSTORE;

            case Type.FLOAT:
                return Opcodes.FSTORE;

            case Type.DOUBLE:
                return Opcodes.DSTORE;

            case Type.OBJECT:
            case Type.ARRAY:
                return Opcodes.ASTORE;

            default:
                throw TypeResolver.reportProblem("Invalid type for LOAD local variable: %s", localVar.type);
        }
    }

    //--//

    public LocalVariableStoreInstruction(LocalVariable localVar) throws
                                                                 AnalyzerException
    {
        super(getOpcodeFor(localVar), localVar);
    }

    private LocalVariableStoreInstruction(LocalVariableStoreInstruction source,
                                          ControlFlowGraph.Cloner cloner) throws
                                                                          AnalyzerException
    {
        super(source, cloner);
    }

    @Override
    public AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                     AnalyzerException
    {
        return new LocalVariableStoreInstruction(this, cloner);
    }

    //--//

    @Override
    public int popSize()
    {
        return localVariable.type.getSize();
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

        FrameValue v = pop(typeResolver, output, localVariable.type); // local Variable
        if (v.isUninitialized())
        {
            throw TypeResolver.reportProblem("Detected uninitialized value ('%s') getting stored in a local variable '%s'", v, this);
        }

        setVariable(output, localVariable, v);

        return output;
    }

    @Override
    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitLocalVarStore(this, localVariable);
    }
}
