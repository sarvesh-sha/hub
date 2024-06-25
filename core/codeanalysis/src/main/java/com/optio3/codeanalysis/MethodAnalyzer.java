/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis;

import com.optio3.codeanalysis.GenericType.MethodDescriptor;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.InstructionWriter;
import com.optio3.codeanalysis.cfg.LocalVariable;
import com.optio3.codeanalysis.cfg.instruction.InvokeInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.ReturnInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class MethodAnalyzer
{
    public final ClassAnalyzer       declaringClass;
    public final GenericMethodInfo   genericMethodInfo;
    public final AnnotationsAnalyzer annotations;

    private final MethodNode m_methodNode;
    private       boolean    m_modified;

    MethodAnalyzer(ClassAnalyzer ca,
                   GenericMethodInfo gmi,
                   MethodNode mn,
                   AnnotationsAnalyzer annotations)
    {
        this.declaringClass = ca;
        this.genericMethodInfo = gmi;
        this.annotations = annotations;

        m_methodNode = mn;
    }

    public void accept(ControlFlowGraph cfg) throws
                                             AnalyzerException
    {
        if (cfg.getContext() != genericMethodInfo)
        {
            throw TypeResolver.reportProblem("Incompatible ControlFlow graph, expecting '%s', got '%s'", genericMethodInfo, cfg.getContext());
        }

        InstructionWriter iv = new InstructionWriter(m_methodNode, genericMethodInfo.typeResolver)
        {

        };

        cfg.accept(iv);

        setModified();
    }

    //--//

    public MethodNode getMethodNode()
    {
        return m_methodNode;
    }

    public boolean hasCode()
    {
        return genericMethodInfo.hasCode();
    }

    public boolean isStatic()
    {
        return genericMethodInfo.isStatic();
    }

    public boolean isConstructor()
    {
        return genericMethodInfo.isConstructor();
    }

    public String getName()
    {
        return genericMethodInfo.getName();
    }

    public MethodDescriptor getSignature()
    {
        return genericMethodInfo.getSignature();
    }

    public boolean wasModified()
    {
        return m_modified;
    }

    void setModified()
    {
        m_modified = true;

        declaringClass.setModified();
    }

    //--//

    public void addModifiers(MethodAccess... modifiers)
    {
        genericMethodInfo.addModifiers(modifiers);

        flushModifiers();
    }

    public void removeModifiers(MethodAccess... modifiers)
    {
        genericMethodInfo.removeModifiers(modifiers);

        flushModifiers();
    }

    private void flushModifiers()
    {
        m_methodNode.access = MethodAccess.toValue(genericMethodInfo.getModifiers());

        setModified();
    }

    //--//

    public MethodDescriptor parseInvokeSignature(String desc)
    {
        return genericMethodInfo.typeResolver.parseGenericMethodSignature(desc, getSignature());
    }

    //--//

    public ControlFlowGraph parseControlFlowGraph(CodeAnalysisLogger logger) throws
                                                                             AnalyzerException
    {
        return createControlFlowGraphInner(logger, false);
    }

    public ControlFlowGraph createControlFlowGraph(CodeAnalysisLogger logger) throws
                                                                              AnalyzerException
    {
        return createControlFlowGraphInner(logger, true);
    }

    private ControlFlowGraph createControlFlowGraphInner(CodeAnalysisLogger logger,
                                                         boolean dontImportCode) throws
                                                                                 AnalyzerException
    {
        return new ControlFlowGraph(this, logger, dontImportCode);
    }

    //--//

    public ControlFlowGraph emitCodeToCallSuperConstructor(CodeAnalysisLogger logger) throws
                                                                                      AnalyzerException
    {
        if (!isConstructor())
        {
            throw TypeResolver.reportProblem("'%s' is not a constructor", this);
        }

        ControlFlowGraph cfg = createControlFlowGraph(logger);

        BasicBlock bb = cfg.newBasicBlock();

        loadLocal(bb, cfg.getThisVar());
        for (LocalVariable arg : cfg.getArgumentVars())
            loadLocal(bb, arg);

        Type superType = declaringClass.genericTypeInfo.getSuperclass()
                                                       .asRawType();
        GenericTypeInfo   superGenericType = declaringClass.genericTypeInfo.typeResolver.getGenericTypeInfo(superType);
        GenericMethodInfo m                = superGenericType.findMethod(getName(), getSignature(), true);

        bb.addInstructions(new AbstractInstruction[] { new InvokeInstruction(Opcodes.INVOKESPECIAL, m, false), new ReturnInstruction(Opcodes.RETURN, Type.VOID_TYPE), });

        return cfg;
    }

    private void loadLocal(BasicBlock bb,
                           LocalVariable var) throws
                                              AnalyzerException
    {
        bb.addInstruction(new LocalVariableLoadInstruction(var));
    }

    //--//

    @Override
    public String toString()
    {
        return String.format("%s.%s %s", declaringClass.getInternalName(), getName(), getSignature());
    }
}
