/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.AbstractFlowControlInstruction;
import com.optio3.codeanalysis.cfg.instruction.LineNumberInstruction;
import com.optio3.codeanalysis.cfg.instruction.MetadataInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnconditionalJumpInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.WellKnownContexts;
import com.optio3.util.Exceptions;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

class ControlFlowGraphImporter
{
    private static class InstructionSlotStatus
    {
        AbstractInsnNode            instruction;
        BasicBlock                  basicBlock;
        Map<Integer, LocalVariable> variables;
        SourceCodeInformation       lineNumber;
        boolean                     splitAtInstruction;
    }

    private final GenericMethodInfo m_methodInfo;
    private final MethodNode        m_methodNode;

    private final ControlFlowGraph   m_graph;
    private final CodeAnalysisLogger m_logger;

    private final InstructionSlotStatus[] m_slots;

    private final SourceCodeVariable[] m_sourceCodeVariables;

    private final InsnList m_insnList;

    ControlFlowGraphImporter(ControlFlowGraph graph,
                             MethodAnalyzer methodAnalyzer,
                             CodeAnalysisLogger logger,
                             boolean dontImportCode)
    {
        Preconditions.checkNotNull(methodAnalyzer);
        Preconditions.checkNotNull(logger);

        m_methodInfo = methodAnalyzer.genericMethodInfo;
        m_methodNode = methodAnalyzer.getMethodNode();

        m_graph = graph;
        m_logger = logger;

        if (dontImportCode)
        {
            m_insnList = null;
            m_slots = null;
        }
        else
        {
            m_insnList = m_methodNode.instructions;
            int numInsns = m_insnList.size();
            m_slots = new InstructionSlotStatus[numInsns];

            for (int i = 0; i < numInsns; i++)
            {
                InstructionSlotStatus slot = new InstructionSlotStatus();
                slot.instruction = m_insnList.get(i);
                m_slots[i] = slot;
            }
        }

        m_sourceCodeVariables = new SourceCodeVariable[getLocalVariables().size()];
    }

    private List<LocalVariableNode> getLocalVariables()
    {
        List<LocalVariableNode> localVars = m_methodNode.localVariables;
        if (localVars == null)
        {
            localVars = Collections.emptyList();
        }

        return localVars;
    }

    void process() throws
                   AnalyzerException
    {
        importVariables();

        if (m_slots != null)
        {
            importInstructions();

            if (m_logger.isEnabled(WellKnownContexts.BasicBlocks, CodeAnalysisLogger.Level.DEBUG))
            {
                m_logger.debug(WellKnownContexts.BasicBlocks, "Basic blocks after import for '%s':", m_methodInfo);

                for (BasicBlock b : m_graph.getBasicBlocks())
                    m_logger.debug(WellKnownContexts.BasicBlocks, "%s", b);

                m_logger.debug(WellKnownContexts.BasicBlocks, "");
            }

            linkExceptions();
        }
    }

    //--//

    private void importVariables() throws
                                   AnalyzerException
    {
        GenericType.MethodDescriptor sig = m_methodInfo.getSignature();

        List<LocalVariableNode> localVars    = getLocalVariables();
        int                     numLocalVars = localVars.size();
        for (int i = 0; i < numLocalVars; i++)
        {
            LocalVariableNode localVar = localVars.get(i);
            int               start    = indexOf(localVar.start);
            int               end      = indexOf(localVar.end);

            String        varSig  = localVar.signature != null ? localVar.signature : localVar.desc;
            GenericType   varType = m_methodInfo.typeResolver.parseGenericTypeReference(varSig, sig);
            LocalVariable newVar  = m_graph.ensureLocalVariableAtIndex(localVar.index, varType.asRawType());

            m_sourceCodeVariables[i] = newVar.createSourceCodeVariable(localVar.name, varType);

            if (m_slots != null)
            {
                Integer index = newVar.getIndex();
                for (int offset = start; offset < end; offset++)
                {
                    Map<Integer, LocalVariable> map = ensureVariableMap(offset);

                    LocalVariable oldVar = map.get(index);
                    if (oldVar != null)
                    {
                        throw TypeResolver.reportProblem("Encountered two variables assigned to the same index '%d' at offset %d: '%s' and '%s'", index, offset, newVar, oldVar);
                    }

                    map.put(index, newVar);
                }
            }
        }
    }

    private Map<Integer, LocalVariable> ensureVariableMap(int offset)
    {
        InstructionSlotStatus slot = m_slots[offset];
        if (slot.variables == null)
        {
            slot.variables = Maps.newHashMap();
        }

        return slot.variables;
    }

    //--//

    private void importInstructions() throws
                                      AnalyzerException
    {
        //
        // To prepare, we collect all the split points.
        //

        m_slots[0].splitAtInstruction = true; // Always create a basic block for the entrypoint.

        InstructionReader visitorForLabelCollection = new InstructionReader(m_methodInfo.typeResolver, m_methodInfo.getSignature())
        {
            @Override
            protected BasicBlock resolveLabel(LabelNode label)
            {
                addToSplit(label);

                return null;
            }

            @Override
            protected BasicBlock resolveFallThrough(JumpInsnNode jump)
            {
                addToSplit(jump.getNext());

                return null;
            }

            @Override
            protected LocalVariable resolveVariable(AbstractInsnNode insn,
                                                    int localIndex,
                                                    Type type,
                                                    boolean afterInstruction)
            {
                int index = indexOf(insn);
                if (afterInstruction)
                {
                    index++;
                }

                Map<Integer, LocalVariable> map = ensureVariableMap(index);

                return map.computeIfAbsent(localIndex, i -> m_graph.ensureLocalVariableAtIndex(i, type));
            }
        };

        for (InstructionSlotStatus slot : m_slots)
        {
            AbstractInsnNode    insn    = slot.instruction;
            AbstractInstruction newInsn = visitorForLabelCollection.visitInsn(insn);

            if (newInsn instanceof LineNumberInstruction)
            {
                //
                // Assign line numbers to instruction offsets.
                //
                LineNumberInstruction newInsn2 = (LineNumberInstruction) newInsn;

                int offset2 = indexOf(newInsn2.start);
                m_slots[offset2].lineNumber = new SourceCodeInformation(newInsn2.lineNumber);
            }
        }

        for (TryCatchBlockNode tcb : m_methodNode.tryCatchBlocks)
        {
            addToSplit(tcb.handler);
            addToSplit(tcb.start);
            addToSplit(tcb.end);
        }

        //
        // With the list of split points, allocate all the basic blocks.
        //
        {
            BasicBlock block = null;
            for (int offset = 0; offset < m_slots.length; offset++)
            {
                InstructionSlotStatus slot = m_slots[offset];

                if (slot.splitAtInstruction)
                {
                    block = m_graph.newBasicBlock();

                    m_logger.trace(WellKnownContexts.InstructionImport, "traverse %d : NEW block %d", offset, block.getSequenceNumber());
                }

                slot.basicBlock = block;
            }
        }

        //
        // Map the instructions to basic blocks.
        //

        InstructionReader visitorForInstructionConversion = new InstructionReader(m_methodInfo.typeResolver, m_methodInfo.getSignature())
        {
            @Override
            protected BasicBlock resolveLabel(LabelNode label) throws
                                                               AnalyzerException
            {
                int index = indexOf(label);
                if (index < 0)
                {
                    throw TypeResolver.reportProblem(label, "Unexpected label");
                }

                return m_slots[index].basicBlock;
            }

            @Override
            protected BasicBlock resolveFallThrough(JumpInsnNode jump) throws
                                                                       AnalyzerException
            {
                if (jump.getNext() == null)
                {
                    // No instruction after this one.
                    return null;
                }

                int index = indexOf(jump.getNext());
                if (index < 0)
                {
                    throw TypeResolver.reportProblem(jump, "Unexpected jump");
                }

                return m_slots[index].basicBlock;
            }

            @Override
            protected LocalVariable resolveVariable(AbstractInsnNode insn,
                                                    int localIndex,
                                                    Type type,
                                                    boolean afterInstruction) throws
                                                                              AnalyzerException
            {
                int index = indexOf(insn);
                if (afterInstruction)
                {
                    index++;
                }

                Map<Integer, LocalVariable> map = ensureVariableMap(index);

                LocalVariable localVar = map.get(localIndex);
                if (localVar == null)
                {
                    throw TypeResolver.reportProblem(insn, "Unexpected local variable");
                }

                Type varType = localVar.type;

                if (!m_methodInfo.typeResolver.canCastTo(type, varType))
                {
                    throw TypeResolver.reportProblem(insn, "Unexpected type for local variable: actual %s <-> %s (requested)", varType, type);
                }

                return localVar;
            }
        };

        BasicBlock lastBasicBlock               = null;
        int        offsetOfLastAddedInstruction = -1;

        SourceCodeInformation lineNumber = null;
        for (int offset = 0; offset < m_slots.length; offset++)
        {
            InstructionSlotStatus slot  = m_slots[offset];
            AbstractInsnNode      insn  = slot.instruction;
            BasicBlock            block = resolveBasicBlock(insn);

            if (slot.lineNumber != null)
            {
                lineNumber = slot.lineNumber;
            }

            m_logger.trace(WellKnownContexts.InstructionImport, "traverse %d : PROCESSING block %d", offset, block.getSequenceNumber());

            AbstractInstruction newInsn = visitorForInstructionConversion.visitInsn(insn);
            if (newInsn instanceof MetadataInstruction)
            {
                continue;
            }

            m_logger.trace(WellKnownContexts.InstructionImport, "traverse %d : CONVERTED  block %d: %s", offset, block.getSequenceNumber(), newInsn);

            if (lastBasicBlock != null && lastBasicBlock != block)
            {
                //
                // We move from one basic block to another,
                // find all the SourceCodeVariables still alive at the end of the previous block,
                // and add them to the new block, with a rangeStart = -1.
                //
                block.copySourceCodeVariableRanges(lastBasicBlock);
            }

            processVariablesRemovedInRange(offsetOfLastAddedInstruction, offset, block);
            processVariablesAddedInRange(offsetOfLastAddedInstruction, offset, block, lastBasicBlock == null);

            addInstruction(block, newInsn, lineNumber);

            AbstractFlowControlInstruction fc = newInsn.asFlowInstruction();
            if (fc == null)
            {
                //
                // If the next instruction is in a different basic block,
                // emit an Unconditional Jump.
                //
                AbstractInsnNode nextInsn = insn.getNext();

                BasicBlock block2 = resolveBasicBlock(nextInsn);
                if (block != block2)
                {
                    UnconditionalJumpInstruction jump = new UnconditionalJumpInstruction(block2);

                    addInstruction(block, jump, lineNumber);
                }
            }

            lastBasicBlock = block;
            offsetOfLastAddedInstruction = offset;
        }

        m_graph.removeEmptyBasicBlocks();
    }

    //--//

    private void processVariablesAddedInRange(int rangeStart,
                                              int rangeEnd,
                                              BasicBlock block,
                                              boolean firstBasicBlock)
    {
        for (SourceCodeVariable scv : collectVariablesAddedInRange(rangeStart, rangeEnd))
        {
            int offset = block.getNumberOfInstructions();
            if (offset == 0 && firstBasicBlock)
            {
                offset = -1;
            }

            block.addSourceCodeVariable(scv, offset);
        }
    }

    private void processVariablesRemovedInRange(int rangeStart,
                                                int rangeEnd,
                                                BasicBlock block)
    {
        for (SourceCodeVariable scv : collectVariablesRemovedInRange(rangeStart, rangeEnd))
        {
            block.removeSourceCodeVariable(scv, block.getNumberOfInstructions());
        }
    }

    private Collection<SourceCodeVariable> collectVariablesAddedInRange(int rangeStart,
                                                                        int rangeEnd)
    {
        return filterVariables((localVar) ->
                               {
                                   int varStart = indexOf(localVar.start);

                                   if (rangeStart <= varStart && varStart < rangeEnd)
                                   {
                                       //
                                       // Start of definition is in the range.
                                       // Still defined at the end of the range?
                                       //
                                       int varEnd = indexOf(localVar.end);
                                       if (rangeEnd < varEnd)
                                       {
                                           return true;
                                       }
                                   }

                                   return false;
                               });
    }

    private Collection<SourceCodeVariable> collectVariablesRemovedInRange(int rangeStart,
                                                                          int rangeEnd)
    {
        return filterVariables((localVar) ->
                               {
                                   int varEnd = indexOf(localVar.end);

                                   if (rangeStart <= varEnd && varEnd < rangeEnd)
                                   {
                                       //
                                       // End of definition is in the range.
                                       // Still defined at the end of the range?
                                       //
                                       int varStart = indexOf(localVar.start);
                                       if (varStart < rangeStart)
                                       {
                                           return true;
                                       }
                                   }

                                   return false;
                               });
    }

    private Collection<SourceCodeVariable> filterVariables(Predicate<LocalVariableNode> filter)
    {
        List<SourceCodeVariable> list = Collections.emptyList();

        List<LocalVariableNode> localVars = getLocalVariables();
        for (int i = 0; i < localVars.size(); i++)
        {
            if (filter.test(localVars.get(i)))
            {
                if (list.isEmpty())
                {
                    list = Lists.newArrayList();
                }

                list.add(m_sourceCodeVariables[i]);
            }
        }

        return list;
    }

    //--//

    private void addInstruction(BasicBlock block,
                                AbstractInstruction insn,
                                SourceCodeInformation lineNumber) throws
                                                                  AnalyzerException
    {
        insn.sourceCode = lineNumber;

        block.addInstruction(insn);
    }

    private void addToSplit(AbstractInsnNode insn)
    {
        if (insn != null)
        {
            m_slots[indexOf(insn)].splitAtInstruction = true;
        }
    }

    private BasicBlock resolveBasicBlock(AbstractInsnNode insn) throws
                                                                AnalyzerException
    {
        InstructionSlotStatus slot   = m_slots[indexOf(insn)];
        BasicBlock            target = slot.basicBlock;
        if (target == null)
        {
            throw TypeResolver.reportProblem("INTERNAL ERROR: Target of jump is not part of the Control Flow Graph!");
        }

        return target;
    }

    private int indexOf(AbstractInsnNode insn)
    {
        return m_insnList.indexOf(insn);
    }

    //--//

    private void linkExceptions() throws
                                  AnalyzerException
    {
        List<TryCatchBlockNode> tcbs = Lists.newArrayList(m_methodNode.tryCatchBlocks);

        removeOverlappingRanges(tcbs);

        //
        // Make sure that the various exception ranges are properly contained, no partial overlap!
        //
        Comparator<TryCatchBlockNode> func = (tcbOuter, tcbInner) ->
        {
            int diff = compareRanges(tcbOuter, tcbInner);
            switch (diff)
            {
                case -2:
                case 2:
                    int startOuter = indexOf(tcbOuter.start);
                    int endOuter = indexOf(tcbOuter.end);

                    int startInner = indexOf(tcbInner.start);
                    int endInner = indexOf(tcbInner.end);

                    //
                    // Unfortunately, we can't throw an AnalyzerException from a lambda for a Comparator.
                    // We'll take it outside and re-throw it with the proper type.
                    //
                    m_logger.error(WellKnownContexts.ExceptionSorting, "Found overlapping trycatch block : [%d-%d] [%d-%d]", startOuter, endOuter, startInner, endInner);

                    throw Exceptions.newRuntimeException("Found overlapping trycatch block : [%d-%d] [%d-%d]", startOuter, endOuter, startInner, endInner);

                default:
                    return -diff; // We want to sort from the outer to the inner.
            }
        };

        if (m_logger.isEnabled(WellKnownContexts.ExceptionSorting, CodeAnalysisLogger.Level.DEBUG))
        {
            for (TryCatchBlockNode tcb : tcbs)
            {
                int startOuter = indexOf(tcb.start);
                int endOuter   = indexOf(tcb.end);

                m_logger.debug(WellKnownContexts.ExceptionSorting, "BEFORE SORTED [%d-%d] %s", startOuter, endOuter, tcb.type);
            }
        }

        try
        {
            tcbs.sort(func);
        }
        catch (RuntimeException ex)
        {
            throw TypeResolver.reportProblem(ex.getMessage());
        }

        if (m_logger.isEnabled(WellKnownContexts.ExceptionSorting, CodeAnalysisLogger.Level.DEBUG))
        {
            for (TryCatchBlockNode tcb : tcbs)
            {
                int startOuter = indexOf(tcb.start);
                int endOuter   = indexOf(tcb.end);

                m_logger.debug(WellKnownContexts.ExceptionSorting, "SORTED [%d-%d] %s", startOuter, endOuter, tcb.type);
            }
        }

        //
        // Then associate the Exception Type and Handler to each basic block.
        //
        for (TryCatchBlockNode tcb : tcbs)
        {
            BasicBlock handlerBlock = resolveBasicBlock(tcb.handler);
            Type       type         = (tcb.type == null) ? null : Type.getObjectType(tcb.type);

            TryCatchHandler tch = new TryCatchHandler(handlerBlock, type);

            BasicBlock       lastBlockCovered = null;
            AbstractInsnNode insn             = tcb.start;
            while (insn != tcb.end)
            {
                BasicBlock insnBlock = resolveBasicBlock(insn);
                if (lastBlockCovered != insnBlock)
                {
                    lastBlockCovered = insnBlock;

                    TryCatchHandler oldTch = insnBlock.getExceptionHandler(type);
                    if (oldTch != null)
                    {
                        if (tch.getOuter() != null && tch.getOuter() != oldTch)
                        {
                            throw TypeResolver.reportProblem("Unexpected layering of try/catch exception blocks");
                        }

                        tch.setOuter(oldTch);
                    }

                    insnBlock.setExceptionHandler(tch);
                }

                insn = insn.getNext();
                if (insn == null)
                {
                    throw TypeResolver.reportProblem("Unexpected non-traversed node");
                }
            }
        }
    }

    private int compareRanges(TryCatchBlockNode tcbA,
                              TryCatchBlockNode tcbB)
    {
        int startA = indexOf(tcbA.start);
        int endA   = indexOf(tcbA.end);

        int startB = indexOf(tcbB.start);
        int endB   = indexOf(tcbB.end);

        if (endA <= startB)
        {
            // Non-overlapping, A is to the left of B.
            m_logger.debug(WellKnownContexts.ExceptionSorting, "EX [%d-%d] [%d-%d] Non-overlapping, A is to the left of B.", startA, endA, startB, endB);
            return -1;
        }

        if (startA >= endB)
        {
            // Non-overlapping, outer is to the right of inner.
            m_logger.debug(WellKnownContexts.ExceptionSorting, "EX [%d-%d] [%d-%d] Non-overlapping, A is to the right of B.", startA, endA, startB, endB);
            return 1;
        }

        if (startA == startB)
        {
            if (endA > endB)
            {
                // A is covering B, it should follow in the list of exceptions.
                return 1;
            }
            else if (endA < endB)
            {
                // B is covering A, it should follow in the list of exceptions.
                return -1;
            }
            else
            {
                return 0;
            }
        }

        if (startA < startB)
        {
            // A's start is to the left of B's start.

            if (endA >= endB)
            {
                // A is covering B, it should follow in the list of exceptions.
                m_logger.debug(WellKnownContexts.ExceptionSorting, "EX [%d-%d] [%d-%d] Overlapping, A is covering B.", startA, endA, startB, endB);
                return 1;
            }

            return 2; // Ack, overlap, A is to the left of B.
        }
        else
        {
            // A's start is to the right of B's start.

            if (endA <= endB)
            {
                // A is covered by B, it should follow in the list of exceptions.
                m_logger.debug(WellKnownContexts.ExceptionSorting, "EX [%d-%d] [%d-%d] Overlapping, A is covered by B.", startA, endA, startB, endB);
                return -1;
            }

            return -2; // Ack, overlap, A is to the right of B.
        }
    }

    private void removeOverlappingRanges(List<TryCatchBlockNode> tcbs)
    {
        for (int outer = 0; outer < tcbs.size(); outer++)
        {
            TryCatchBlockNode tcbOuter = tcbs.get(outer);

            for (int inner = outer + 1; inner < tcbs.size(); inner++)
            {
                TryCatchBlockNode tcbInner = tcbs.get(inner);

                int diff = compareRanges(tcbOuter, tcbInner);

                switch (diff)
                {
                    case 2: // Ack, overlap, outer is to the left of inner.
                        // Since the exception table is evaluated in entry order, truncate the inner entry at the outer's end, so it doesn't overlap with the outer one.
                        tcbInner = new TryCatchBlockNode(tcbOuter.end, tcbInner.end, tcbInner.handler, tcbInner.type);
                        tcbs.set(inner, tcbInner);
                        break;

                    case -2: // Ack, overlap, outer is to the right of inner.
                        // Since the exception table is evaluated in entry order, truncate the inner entry at the ouer's start, so it doesn't overlap with the outer one.
                        tcbInner = new TryCatchBlockNode(tcbInner.start, tcbOuter.start, tcbInner.handler, tcbInner.type);
                        tcbs.set(inner, tcbInner);
                        break;
                }
            }
        }
    }
}
