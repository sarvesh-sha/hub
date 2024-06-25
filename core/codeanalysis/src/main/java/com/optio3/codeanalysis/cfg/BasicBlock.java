/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.JsonBuilder;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.AbstractFlowControlInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnconditionalJumpInstruction;
import com.optio3.util.CollectionUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class BasicBlock
{
    public final ControlFlowGraph graph;

    private List<AbstractInstruction> m_instructions = Lists.newArrayList();

    private List<BasicBlock> m_predecessors;
    private List<BasicBlock> m_successors;

    private Map<Type, TryCatchHandler> m_exceptionHandlers;

    private List<SourceCodeVariableRange> m_sourceCodeVariableRanges;

    //--//

    BasicBlock(ControlFlowGraph graph)
    {
        this.graph = graph;
    }

    void clone(BasicBlock source,
               ControlFlowGraph.Cloner cloner) throws
                                               AnalyzerException
    {
        //
        // Clone all the instructions.
        //
        for (AbstractInstruction insn : source.getInstructions())
        {
            AbstractInstruction insnCloned = cloner.clone(insn);
            addInstruction(insnCloned);
        }

        //
        // Clone all the debug infos.
        //
        {
            List<SourceCodeVariableRange> list       = ensureSourceCodeVariableRanges();
            List<SourceCodeVariableRange> listSource = source.ensureSourceCodeVariableRanges();

            for (SourceCodeVariableRange scvRange : listSource)
            {
                SourceCodeVariable scvCloned = cloner.clone(scvRange.target);
                list.add(new SourceCodeVariableRange(scvCloned, scvRange.rangeStart, scvRange.rangeEnd));
            }
        }

        //
        // Clone all the exception handlers.
        //
        for (Type t : source.getExceptionHandlers()
                            .keySet())
        {
            TryCatchHandler tch = source.getExceptionHandler(t);

            setExceptionHandler(cloner.clone(tch));
        }
    }

    public static BasicBlock[] toArray(Collection<BasicBlock> coll)
    {
        BasicBlock[] res = new BasicBlock[CollectionUtils.size(coll)];

        if (coll != null)
        {
            coll.toArray(res);
        }

        return res;
    }

    //--//

    public void accept(TypeVisitor visitor)
    {
        for (AbstractInstruction insn : m_instructions)
            insn.accept(visitor);

        for (TryCatchHandler tch : getExceptionHandlersInner().values())
            tch.accept(visitor);
    }

    //--//

    public int getSequenceNumber()
    {
        return graph.getIndexOf(this);
    }

    public BasicBlock getPreviousBasicBlock()
    {
        int offset = getSequenceNumber() - 1;

        return graph.getBasicBlockAt(offset);
    }

    public BasicBlock getNextBasicBlock()
    {
        int offset = getSequenceNumber() + 1;

        return graph.getBasicBlockAt(offset);
    }

    public Collection<BasicBlock> getPredecessors()
    {
        return Collections.unmodifiableList(getPredecessorsInner());
    }

    private List<BasicBlock> getPredecessorsInner()
    {
        return CollectionUtils.asEmptyCollectionIfNull(m_predecessors);
    }

    private List<BasicBlock> ensurePredecessors()
    {
        if (m_predecessors == null)
        {
            m_predecessors = Lists.newArrayList();
        }

        return m_predecessors;
    }

    public Collection<BasicBlock> getSuccessors()
    {
        return Collections.unmodifiableList(getSuccessorsInner());
    }

    private List<BasicBlock> getSuccessorsInner()
    {
        return CollectionUtils.asEmptyCollectionIfNull(m_successors);
    }

    private List<BasicBlock> ensureSuccessors()
    {
        if (m_successors == null)
        {
            m_successors = Lists.newArrayList();
        }

        return m_successors;
    }

    public List<BasicBlock> getLinearChainOfBlocks()
    {
        List<BasicBlock> res    = Lists.newArrayList();
        BasicBlock       target = this;
        while (true)
        {
            res.add(target);

            if (CollectionUtils.size(target.m_successors) != 1)
            {
                break;
            }

            BasicBlock next = target.m_successors.get(0);

            if (next.m_predecessors.size() != 1)
            {
                break;
            }

            if (res.contains(next))
            {
                // Avoid loops.
                break;
            }

            target = next;
        }

        return res;
    }

    //--//

    public Collection<AbstractInstruction> getInstructions()
    {
        return Collections.unmodifiableList(m_instructions);
    }

    public int getNumberOfInstructions()
    {
        return m_instructions.size();
    }

    public int getInstructionIndex(AbstractInstruction insn)
    {
        Preconditions.checkState(insn.getBasicBlock() == this);

        return m_instructions.indexOf(insn);
    }

    public AbstractInstruction getInstructionAt(int index)
    {
        return (index >= 0 && index < getNumberOfInstructions()) ? getInstructionInner(index) : null;
    }

    private AbstractInstruction getInstructionInner(int index)
    {
        return m_instructions.get(index);
    }

    //--//

    public void addInstructions(AbstractInstruction... insnArray) throws
                                                                  AnalyzerException
    {
        for (AbstractInstruction insn : insnArray)
            addInstruction(insn);
    }

    public void addInstruction(AbstractInstruction insn) throws
                                                         AnalyzerException
    {
        int index = getNumberOfInstructions();
        if (index > 0)
        {
            AbstractInstruction lastInsn   = getInstructionInner(index - 1);
            boolean             lastIsFlow = (lastInsn.asFlowInstruction() != null);

            if (lastIsFlow)
            {
                AbstractFlowControlInstruction insnAsFlow = insn.asFlowInstruction();

                if (insnAsFlow == null)
                {
                    //
                    // If there's already a control flow instruction, add this instruction before it.
                    //
                    index--;
                }
            }
        }

        addInstructionInner(index, insn);
    }

    void addInstructionInner(int index,
                             AbstractInstruction insn) throws
                                                       AnalyzerException
    {
        Preconditions.checkState(insn.getBasicBlock() == null);

        boolean insnIsFlow = insn.asFlowInstruction() != null;

        int size = getNumberOfInstructions();
        if (size > 0)
        {
            //
            // Make sure Control Flow instructions are inserted in the right way:
            //
            // - Only one per block.
            // - Always at the end.
            //
            if (index == size)
            {
                AbstractInstruction lastInsn       = getInstructionInner(index - 1);
                boolean             lastInsnIsFlow = lastInsn.asFlowInstruction() != null;

                if (lastInsnIsFlow)
                {
                    if (insnIsFlow)
                    {
                        throw TypeResolver.reportProblem("Only one Control Flow instruction per basic block: %s", this);
                    }

                    throw TypeResolver.reportProblem("Can't add a normal instruction after control flow one in basic block: %s", this);
                }
            }
            else
            {
                if (insnIsFlow)
                {
                    throw TypeResolver.reportProblem("Can't add Control Flow instruction in the middle of basic block: %s", this);
                }
            }
        }

        if (insn.sourceCode == null)
        {
            insn.sourceCode = graph.getDefaultSourceCode();
        }

        // First add to the list.
        m_instructions.add(index, insn);
        // Then set the basic block info.
        insn.setBasicBlock(this);

        //
        // Automatically link basic blocks if needed.
        //
        AbstractFlowControlInstruction fc = insn.asFlowInstruction();
        if (fc != null)
        {
            BasicBlockVisitor visitorForLinkingBasicBlocks = new BasicBlockVisitor()
            {
                @Override
                public void visitTarget(BasicBlock target)
                {
                    addSuccessor(target);
                }
            };

            fc.accept(visitorForLinkingBasicBlocks);
        }

        graph.flagAsModified();
    }

    //--//

    void substituteInstruction(AbstractInstruction insnOld,
                               AbstractInstruction insnNew) throws
                                                            AnalyzerException
    {
        int index = getInstructionIndex(insnOld);

        removeInstructionInner(insnOld);
        addInstructionInner(index, insnNew);
    }

    //--//

    void addInstructionsBefore(AbstractInstruction insn,
                               AbstractInstruction... insns) throws
                                                             AnalyzerException
    {
        int index = getInstructionIndex(insn);

        addInstructionsInner(index, insns);
    }

    void addInstructionsAfter(AbstractInstruction insn,
                              AbstractInstruction... insns) throws
                                                            AnalyzerException
    {
        if (insn.asFlowInstruction() != null)
        {
            throw TypeResolver.reportProblem("Cannot add instructions after flow control '%s' ", insn);
        }

        int index = getInstructionIndex(insn) + 1;

        addInstructionsInner(index, insns);
    }

    void addInstructionsInner(int index,
                              AbstractInstruction... insns) throws
                                                            AnalyzerException
    {
        for (AbstractInstruction insnNew : insns)
        {
            Preconditions.checkState(insnNew.getBasicBlock() == null);

            if (insnNew.asFlowInstruction() != null)
            {
                throw TypeResolver.reportProblem("Cannot insert flow control instruction '%s'", insnNew);
            }

            if (insnNew.sourceCode == null)
            {
                insnNew.sourceCode = graph.getDefaultSourceCode();
            }
        }

        // First add to the list.
        m_instructions.addAll(index, Lists.newArrayList(insns));
        // Then set the basic block info.
        for (AbstractInstruction insnNew : insns)
            insnNew.setBasicBlock(this);

        graph.flagAsModified();
    }

    //--//

    void removeInstruction(AbstractInstruction insn) throws
                                                     AnalyzerException
    {
        if (insn.asFlowInstruction() != null)
        {
            throw TypeResolver.reportProblem("Cannot remove Control Flow instruction from basic block: %s", this);
        }

        removeInstructionInner(insn);
    }

    void removeInstructionInner(AbstractInstruction insn)
    {
        int index = getInstructionIndex(insn);

        // First remove from list.
        m_instructions.remove(index);
        // Then reset the basic block info.
        insn.setBasicBlock(null);

        //
        // Automatically unlink basic blocks if needed.
        //
        AbstractFlowControlInstruction fc = insn.asFlowInstruction();
        if (fc != null)
        {
            BasicBlockVisitor visitorForLinkingBasicBlocks = new BasicBlockVisitor()
            {
                @Override
                public void visitTarget(BasicBlock target)
                {
                    removeSuccessor(target);
                }
            };

            fc.accept(visitorForLinkingBasicBlocks);
        }

        graph.flagAsModified();
    }

    public AbstractInstruction getFirstInstruction()
    {
        Preconditions.checkState(getNumberOfInstructions() > 0);
        return getInstructionInner(0);
    }

    public AbstractInstruction getLastInstruction()
    {
        int size = getNumberOfInstructions();
        Preconditions.checkState(size > 0);
        return getInstructionInner(size - 1);
    }

    public boolean visitInstructions(CheckedPredicate<AbstractInstruction> visitor) throws
                                                                                    AnalyzerException,
                                                                                    IOException
    {
        for (AbstractInstruction insn : AbstractInstruction.toArray(m_instructions))
        {
            if (!visitor.test(insn))
            {
                return false;
            }
        }

        return true;
    }

    public <T extends AbstractInstruction> boolean visitInstructions(Class<T> clz,
                                                                     CheckedPredicate<T> visitor) throws
                                                                                                  AnalyzerException,
                                                                                                  IOException
    {
        return visitInstructions(AbstractInstruction.createFilter(clz, visitor));
    }

    //--//

    public Map<Type, TryCatchHandler> getExceptionHandlers()
    {
        return Collections.unmodifiableMap(getExceptionHandlersInner());
    }

    Map<Type, TryCatchHandler> getExceptionHandlersInner()
    {
        return CollectionUtils.asEmptyCollectionIfNull(m_exceptionHandlers);
    }

    public TryCatchHandler getExceptionHandler(Type type)
    {
        return getExceptionHandlersInner().get(type);
    }

    public void setExceptionHandler(TryCatchHandler tch)
    {
        if (m_exceptionHandlers == null)
        {
            m_exceptionHandlers = Maps.newHashMap();
        }

        m_exceptionHandlers.put(tch.type, tch);

        tch.addToCoveredSet(this);

        graph.flagAsModified();
    }

    //--//

    void copySourceCodeVariableRanges(BasicBlock predecessor)
    {
        List<SourceCodeVariableRange> list     = ensureSourceCodeVariableRanges();
        List<SourceCodeVariableRange> listPred = predecessor.ensureSourceCodeVariableRanges();

        for (SourceCodeVariableRange scvRange : listPred)
        {
            if (scvRange.rangeEnd == -1)
            {
                list.add(new SourceCodeVariableRange(scvRange.target, -1, -1));
            }
        }
    }

    public List<SourceCodeVariable> getActiveSourceCodeVariablesAt(int offset)
    {
        List<SourceCodeVariable> list = Lists.newArrayList();

        for (SourceCodeVariableRange scvRange : getSourceCodeVariableRangesInner())
        {
            if (scvRange.isAliveAt(offset))
            {
                list.add(scvRange.target);
            }
        }

        return list;
    }

    public void addSourceCodeVariable(SourceCodeVariable scv,
                                      int offset)
    {
        List<SourceCodeVariableRange> list            = ensureSourceCodeVariableRanges();
        int                           effectiveOffset = (offset < 0) ? 0 : offset;

        for (SourceCodeVariableRange scvRange : list)
        {
            if (scvRange.target == scv && scvRange.isStartOfRangeLessThanOrEqualTo(effectiveOffset))
            {
                if (scvRange.isEndOfRangeGreaterThanOrEqualTo(effectiveOffset))
                {
                    // Variable already alive, extend range.
                    scvRange.rangeEnd = -1;
                    return;
                }
            }
        }

        list.add(new SourceCodeVariableRange(scv, offset, -1));
    }

    public void removeSourceCodeVariable(SourceCodeVariable scv,
                                         int offset)
    {
        List<SourceCodeVariableRange> list            = ensureSourceCodeVariableRanges();
        int                           effectiveOffset = (offset < 0) ? getNumberOfInstructions() : offset;

        for (SourceCodeVariableRange scvRange : list)
        {
            if (scvRange.target == scv && scvRange.isStartOfRangeLessThanOrEqualTo(effectiveOffset))
            {
                if (scvRange.isEndOfRangeGreaterThanOrEqualTo(effectiveOffset))
                {
                    // Variable was alive alive, truncate range.
                    if (effectiveOffset > 0)
                    {
                        scvRange.rangeEnd = offset;
                    }
                    else
                    {
                        list.remove(scvRange);
                    }
                    return;
                }
            }
        }
    }

    public Collection<SourceCodeVariableRange> getSourceCodeVariableRanges()
    {
        return Collections.unmodifiableList(getSourceCodeVariableRangesInner());
    }

    private List<SourceCodeVariableRange> getSourceCodeVariableRangesInner()
    {
        return CollectionUtils.asEmptyCollectionIfNull(m_sourceCodeVariableRanges);
    }

    private List<SourceCodeVariableRange> ensureSourceCodeVariableRanges()
    {
        if (m_sourceCodeVariableRanges == null)
        {
            m_sourceCodeVariableRanges = Lists.newArrayList();
        }

        return m_sourceCodeVariableRanges;
    }

    public GenericType extractGenericType(LocalVariable var)
    {
        List<SourceCodeVariableRange> list = ensureSourceCodeVariableRanges();

        for (SourceCodeVariableRange scvRange : list)
        {
            SourceCodeVariable scv = scvRange.target;

            if (scv.matches(var))
            {
                return scv.signature;
            }
        }

        return null;
    }

    public String extractName(LocalVariable var)
    {
        List<SourceCodeVariableRange> list = ensureSourceCodeVariableRanges();

        for (SourceCodeVariableRange scvRange : list)
        {
            SourceCodeVariable scv = scvRange.target;

            if (scv.matches(var))
            {
                return scv.name;
            }
        }

        return null;
    }

    //--//

    public void setControlFlow(AbstractFlowControlInstruction flow) throws
                                                                    AnalyzerException
    {
        int size = getNumberOfInstructions();
        if (size > 0)
        {
            AbstractInstruction insn = getInstructionInner(size - 1);
            if (insn.asFlowInstruction() != null)
            {
                insn.substitute(flow);
                return;
            }
        }

        addInstruction(flow);
    }

    public BasicBlock insertNewBasicBlockBefore(boolean inheritExceptionHandlers)
    {
        return graph.newBasicBlockBefore(this, inheritExceptionHandlers);
    }

    public BasicBlock insertNewBasicBlockAfter(boolean inheritExceptionHandlers)
    {
        return graph.newBasicBlockAfter(this, inheritExceptionHandlers);
    }

    public BasicBlock splitAt(AbstractInstruction insn) throws
                                                        AnalyzerException
    {
        int offset = getInstructionIndex(insn);

        int size = getNumberOfInstructions();
        if (offset == 0 || offset == size)
        {
            // nothing to do.
            return null;
        }

        Preconditions.checkArgument(offset > 0 && offset < size);

        BasicBlock newBlock = insertNewBasicBlockAfter(true);

        //
        // Split instructions between the two blocks.
        //
        {
            List<AbstractInstruction> leftOfSplit  = Lists.newArrayList(m_instructions.subList(0, offset));
            List<AbstractInstruction> rightOfSplit = Lists.newArrayList(m_instructions.subList(offset, size));

            m_instructions = leftOfSplit;
            newBlock.m_instructions = rightOfSplit;

            for (AbstractInstruction insntoMode : rightOfSplit)
            {
                //
                // Since we are removing instructions, the next one to move is always at 'offset'.
                //

                // First remove from old basic block.
                insntoMode.setBasicBlock(null);

                // Then add to new basic block.
                insntoMode.setBasicBlock(newBlock);
            }
        }

        //
        // Re-organize debug info.
        //
        {
            List<SourceCodeVariableRange> list = getSourceCodeVariableRangesInner();

            for (int i = 0; i < list.size(); i++)
            {
                SourceCodeVariableRange scvRange = list.get(i);
                int                     rangeStart;
                int                     rangeEnd = scvRange.rangeEnd;

                if (scvRange.isStartOfRangeLessThan(offset))
                {
                    // Range starts to the left of the split point.
                    // Either a split or a move.

                    if (scvRange.isStartOfRangeEqualTo(offset))
                    {
                        //
                        // Move to the new block.
                        //
                        list.remove(i--);

                        rangeStart = 0;
                    }
                    else if (scvRange.isEndOfRangeGreaterThanOrEqualTo(offset))
                    {
                        // A split.
                        scvRange.rangeEnd = -1;
                        rangeStart = -1;
                    }
                    else
                    {
                        // Range is to the left of the split, nothing to do.
                        continue;
                    }
                }
                else
                {
                    //
                    // Move to the new block.
                    //
                    list.remove(i--);

                    rangeStart = scvRange.rangeStart - offset;
                }

                newBlock.addSourceCodeVariable(scvRange.target, rangeStart);
                if (rangeEnd > 0)
                {
                    newBlock.removeSourceCodeVariable(scvRange.target, rangeEnd - offset);
                }
            }
        }

        //
        // Remove all the successors and re-link them to the new block.
        //
        {
            List<BasicBlock> oldSuccessors = Lists.newArrayList(getSuccessorsInner());

            for (BasicBlock successor : oldSuccessors)
                this.removeSuccessor(successor);

            for (BasicBlock successor : oldSuccessors)
                newBlock.addSuccessor(successor);
        }

        //
        // Finally add a goto to the next block.
        //
        {
            UnconditionalJumpInstruction jump = new UnconditionalJumpInstruction(newBlock);

            jump.sourceCode = newBlock.getFirstInstruction().sourceCode;

            addInstruction(jump);
        }

        //--//

        return newBlock;
    }

    void addSuccessor(BasicBlock target)
    {
        Preconditions.checkState(graph == target.graph);

        CollectionUtils.addIfMissingAndNotNull(ensureSuccessors(), target);
        CollectionUtils.addIfMissingAndNotNull(target.ensurePredecessors(), this);

        graph.flagAsModified();
    }

    void removeSuccessor(BasicBlock target)
    {
        Preconditions.checkState(graph == target.graph);

        ensureSuccessors().remove(target);
        target.ensurePredecessors()
              .remove(this);

        graph.flagAsModified();
    }

    public void invalidateFrameState()
    {
        graph.flagAsModified();
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

    public void toString(JsonBuilder json)
    {
        json.newField("num", getSequenceNumber());

        try (JsonBuilder sub = json.newArray("instructions"))
        {
            for (AbstractInstruction slot : m_instructions)
            {
                try (JsonBuilder entry = sub.newObject(null))
                {
                    slot.toString(entry);
                }
            }
        }

        emitCollection(json, getSuccessorsInner(), "successors");
        emitCollection(json, getPredecessorsInner(), "predecessors");
    }

    private void emitCollection(JsonBuilder json,
                                List<BasicBlock> list,
                                String text)
    {
        if (!list.isEmpty())
        {
            try (JsonBuilder sub = json.newArray(text))
            {
                for (BasicBlock block : list)
                    sub.newRawValue(block.getSequenceNumber());
            }
        }
    }
}
