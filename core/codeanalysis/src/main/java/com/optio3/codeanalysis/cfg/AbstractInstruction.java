/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.AbstractFlowControlInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableStoreInstruction;
import com.optio3.util.CollectionUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.TypeAnnotationNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.util.Printer;

public abstract class AbstractInstruction
{
    public final int opcode;

    /**
     * The runtime visible type annotations of this instruction. This field is
     * only used for real instructions (i.e. not for labels, frames, or line
     * number nodes). This list is a list of {@link TypeAnnotationNode} objects.
     * May be <tt>null</tt>.
     *
     * @associates org.objectweb.asm.tree.TypeAnnotationNode
     * @label visible
     */
    public List<TypeAnnotationNode> visibleTypeAnnotations;

    /**
     * The runtime invisible type annotations of this instruction. This field is
     * only used for real instructions (i.e. not for labels, frames, or line
     * number nodes). This list is a list of {@link TypeAnnotationNode} objects.
     * May be <tt>null</tt>.
     *
     * @associates org.objectweb.asm.tree.TypeAnnotationNode
     * @label invisible
     */
    public List<TypeAnnotationNode> invisibleTypeAnnotations;

    public SourceCodeInformation sourceCode;

    private BasicBlock m_basicBlock;
    private FrameState m_frameState;

    protected AbstractInstruction(int opcode)
    {
        this.opcode = opcode;
    }

    protected AbstractInstruction(AbstractInstruction source,
                                  ControlFlowGraph.Cloner cloner)
    {
        opcode = source.opcode;

        visibleTypeAnnotations = AbstractInstruction.cloneAnnotations(source.visibleTypeAnnotations);
        invisibleTypeAnnotations = AbstractInstruction.cloneAnnotations(source.invisibleTypeAnnotations);

        sourceCode = source.sourceCode;
    }

    public abstract AbstractInstruction clone(ControlFlowGraph.Cloner cloner) throws
                                                                              AnalyzerException;

    //--//

    public static List<TypeAnnotationNode> cloneAnnotations(List<TypeAnnotationNode> srcList)
    {
        if (srcList == null)
        {
            return null;
        }

        List<TypeAnnotationNode> dstList = Lists.newArrayList();
        for (TypeAnnotationNode src : srcList)
        {
            TypeAnnotationNode ann = new TypeAnnotationNode(src.typeRef, src.typePath, src.desc);
            src.accept(ann);
            dstList.add(ann);
        }
        return dstList;
    }

    public static AbstractInstruction[] toArray(Collection<AbstractInstruction> coll)
    {
        AbstractInstruction[] res = new AbstractInstruction[CollectionUtils.size(coll)];

        if (coll != null)
        {
            coll.toArray(res);
        }

        return res;
    }

    public static <T extends AbstractInstruction> CheckedPredicate<AbstractInstruction> createFilter(Class<T> clz,
                                                                                                     CheckedPredicate<T> visitor)
    {
        return (insn) ->
        {
            if (!clz.isInstance(insn))
            {
                return true;
            }

            return visitor.test(clz.cast(insn));
        };
    }

    //--//

    public BasicBlock getBasicBlock()
    {
        return m_basicBlock;
    }

    public void setBasicBlock(BasicBlock basicBlock)
    {
        if (basicBlock == m_basicBlock)
        {
            return;
        }

        //
        // Make sure we don't get moved from basic block to basic block incorrectly.
        //
        if (basicBlock != null)
        {
            m_basicBlock = basicBlock;
            Preconditions.checkState(basicBlock.getInstructionIndex(this) != -1);
        }
        else
        {
            Preconditions.checkState(m_basicBlock.getInstructionIndex(this) == -1);
            m_basicBlock = null;
        }
    }

    public FrameState getFrameState()
    {
        return m_frameState;
    }

    public void setFrameState(FrameState fs)
    {
        m_frameState = fs;
        m_basicBlock.invalidateFrameState();
    }

    public void substitute(AbstractInstruction insn) throws
                                                     AnalyzerException
    {
        Preconditions.checkNotNull(m_basicBlock);
        m_basicBlock.substituteInstruction(this, insn);
    }

    public void remove() throws
                         AnalyzerException
    {
        if (m_basicBlock != null)
        {
            m_basicBlock.removeInstruction(this);
        }

        Preconditions.checkState(m_basicBlock == null);
    }

    public void addBefore(AbstractInstruction... insns) throws
                                                        AnalyzerException
    {
        Preconditions.checkNotNull(m_basicBlock);
        m_basicBlock.addInstructionsBefore(this, insns);
    }

    public void addAfter(AbstractInstruction... insns) throws
                                                       AnalyzerException
    {
        Preconditions.checkNotNull(m_basicBlock);
        m_basicBlock.addInstructionsAfter(this, insns);
    }

    public BasicBlock splitBefore() throws
                                    AnalyzerException
    {
        return m_basicBlock.splitAt(this);
    }

    public BasicBlock splitAfter() throws
                                   AnalyzerException
    {
        return m_basicBlock.splitAt(getNext());
    }

    public AbstractInstruction getPrevious()
    {
        int index = m_basicBlock.getInstructionIndex(this) - 1;
        return m_basicBlock.getInstructionAt(index);
    }

    public AbstractInstruction getNext()
    {
        int index = m_basicBlock.getInstructionIndex(this) + 1;
        return m_basicBlock.getInstructionAt(index);
    }

    public AbstractFlowControlInstruction asFlowInstruction()
    {
        if (this instanceof AbstractFlowControlInstruction)
        {
            return (AbstractFlowControlInstruction) this;
        }

        return null;
    }

    //--//

    public Set<AbstractInstruction> findVariableUses(LocalVariable localVar)
    {
        Set<AbstractInstruction> uses    = Sets.newHashSet();
        Set<AbstractInstruction> visited = Sets.newHashSet();

        findVariableUses(uses, visited, localVar, this);

        return uses;
    }

    private void findVariableUses(Set<AbstractInstruction> uses,
                                  Set<AbstractInstruction> visited,
                                  LocalVariable localVar,
                                  AbstractInstruction insn)
    {
        if (insn != null && visited.add(insn))
        {
            for (TryCatchHandler tch : insn.getBasicBlock()
                                           .getExceptionHandlers()
                                           .values())
            {
                findVariableUses(uses, visited, localVar, tch.handler.getFirstInstruction());
            }

            AbstractFlowControlInstruction flow = insn.asFlowInstruction();
            if (flow != null)
            {
                BasicBlockVisitor visitor = new BasicBlockVisitor()
                {
                    @Override
                    public void visitTarget(BasicBlock target)
                    {
                        findVariableUses(uses, visited, localVar, target.getFirstInstruction());
                    }
                };

                flow.accept(visitor);
            }
            else
            {
                if (insn instanceof LocalVariableInstruction)
                {
                    LocalVariableInstruction insn2 = (LocalVariableInstruction) insn;

                    if (Objects.equals(insn2.localVariable.getIndex(), localVar.getIndex()))
                    {
                        if (insn2 instanceof LocalVariableStoreInstruction)
                        {
                            // Instruction kills variable, terminate search.
                            return;
                        }

                        if (insn2 instanceof LocalVariableLoadInstruction)
                        {
                            uses.add(insn2);
                        }
                    }
                }

                findVariableUses(uses, visited, localVar, insn.getNext());
            }
        }
    }

    public abstract int popSize();

    public abstract int pushSize();

    public abstract FrameState processStack(TypeResolver typeResolver,
                                            FrameState input) throws
                                                              AnalyzerException;

    public abstract void accept(TypeVisitor visitor);

    public abstract void accept(InstructionWriter visitor) throws
                                                           AnalyzerException;

    //--//

    protected FrameValue pop(FrameState fs)
    {
        FrameValue val = fs.pop();

        return val;
    }

    protected FrameValue pop(TypeResolver typeResolver,
                             FrameState fs,
                             Type expectedType) throws
                                                AnalyzerException
    {
        FrameValue val = pop(fs);

        Type actualType = val.getType();
        if (!typeResolver.canCastTo(expectedType, actualType))
        {
            throw TypeResolver.reportProblem("Can't cast %s %s != %s [%s]", val.isUninitialized() ? "UNINIT" : "", actualType, expectedType, Printer.OPCODES[opcode]);
        }

        return val;
    }

    protected void push(FrameState fs,
                        FrameValue v)
    {
        fs.push(v);
    }

    protected void push(FrameState fs,
                        Type type,
                        FrameValue forwardedFrom)
    {
        fs.push(type, forwardedFrom);
    }

    protected void push(FrameState fs,
                        Type type,
                        AbstractInstruction generator)
    {
        fs.push(type, generator);
    }

    protected FrameValue getVariable(FrameState fs,
                                     LocalVariable localVariable) throws
                                                                  AnalyzerException
    {
        FrameValue res = fs.getVariable(localVariable);
        if (res == null)
        {
            throw TypeResolver.reportProblem("Unexpected access to variable '%s'", localVariable);
        }

        return res;
    }

    protected void setVariable(FrameState fs,
                               LocalVariable localVariable,
                               FrameValue v)
    {
        fs.setVariable(localVariable, v);
    }

    //--//

    @Override
    public String toString()
    {
        JsonBuilder json = new JsonBuilder();

        toString(json);
        json.close();

        return json.toString();
    }

    public abstract void toString(StringBuilder sb);

    protected void toStringOpcode(StringBuilder sb)
    {
        if (opcode >= 0 && opcode < Printer.OPCODES.length)
        {
            sb.append(Printer.OPCODES[opcode]);
        }
    }

    public void toString(JsonBuilder json)
    {
        if (opcode >= 0 && opcode < Printer.OPCODES.length)
        {
            json.newField("opcode", Printer.OPCODES[opcode]);
        }

        if (sourceCode != null)
        {
            json.newField("lineNumber", sourceCode.lineNumber);
        }
    }
}
