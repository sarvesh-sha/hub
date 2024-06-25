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
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.GenericTypeInfo;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger.Level;
import com.optio3.codeanalysis.logging.LogToStringVisitor;
import com.optio3.util.CollectionUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class ControlFlowGraph
{
    public static class Cloner
    {
        private final ControlFlowGraphMapper m_mapper;

        Cloner(ControlFlowGraphMapper mapper)
        {
            m_mapper = mapper;
        }

        public BasicBlock clone(BasicBlock bb) throws
                                               AnalyzerException
        {
            if (bb == null)
            {
                return null;
            }

            BasicBlock bbCloned = m_mapper.get(bb);
            if (bbCloned == null)
            {
                throw TypeResolver.reportProblem("INTERNAL ERROR: unexpected uncloned basic block: %s", bb);
            }

            return bbCloned;
        }

        public AbstractInstruction clone(AbstractInstruction insn) throws
                                                                   AnalyzerException
        {
            if (insn == null)
            {
                return null;
            }

            AbstractInstruction insnCloned = m_mapper.get(insn);
            if (insnCloned == null)
            {
                insnCloned = insn.clone(this);
                m_mapper.put(insn, insnCloned);
            }

            return insnCloned;
        }

        public TryCatchHandler clone(TryCatchHandler tch) throws
                                                          AnalyzerException
        {
            if (tch == null)
            {
                return null;
            }

            TryCatchHandler tchCloned = m_mapper.get(tch);
            if (tchCloned == null)
            {
                tchCloned = new TryCatchHandler(clone(tch.handler), tch.type);
                tchCloned.setOuter(clone(tch.getOuter()));

                m_mapper.put(tch, tchCloned);
            }

            return tchCloned;
        }

        public SourceCodeVariable clone(SourceCodeVariable scv)
        {
            if (scv == null)
            {
                return null;
            }

            SourceCodeVariable scvCloned = m_mapper.get(scv);
            if (scvCloned == null)
            {
                LocalVariable varCloned = m_mapper.targetCfg.ensureLocalVariableAtIndex(scv.getIndex(), scv.getType());
                scvCloned = varCloned.createSourceCodeVariable(scv.name, scv.signature);

                m_mapper.put(scv, scvCloned);
            }

            return scvCloned;
        }

        public LocalVariable clone(LocalVariable var)
        {
            if (var == null)
            {
                return null;
            }

            LocalVariable varCloned = m_mapper.get(var);
            if (varCloned == null)
            {
                varCloned = m_mapper.targetCfg.ensureLocalVariableAtIndex(var.getIndex(), var.type);

                m_mapper.put(var, varCloned);
            }

            return varCloned;
        }

        public LocalVariable[] clone(LocalVariable[] vars)
        {
            if (vars == null)
            {
                return null;
            }

            LocalVariable[] varsCloned = new LocalVariable[vars.length];
            for (int i = 0; i < vars.length; i++)
            {
                varsCloned[i] = clone(vars[i]);
            }

            return varsCloned;
        }
    }

    private       GenericMethodInfo  m_methodInfo;
    private final CodeAnalysisLogger m_logger;

    private final List<BasicBlock>                 m_basicBlocks    = Lists.newArrayList();
    private final Multimap<Integer, LocalVariable> m_localVariables = HashMultimap.create();

    private LocalVariable   m_thisVar;
    private LocalVariable[] m_argumentsVar;

    private boolean               m_frameStateComputed;
    private boolean               m_modified;
    private SourceCodeInformation m_defaultSourceCode;

    public ControlFlowGraph(MethodAnalyzer method,
                            CodeAnalysisLogger logger,
                            boolean dontImportCode) throws
                                                    AnalyzerException
    {
        if (logger == null)
        {
            logger = CodeAnalysisLogger.nullLogger;
        }

        m_methodInfo = method.genericMethodInfo;
        m_logger     = logger;

        setContext(method.genericMethodInfo);

        ControlFlowGraphImporter importer = new ControlFlowGraphImporter(this, method, logger, dontImportCode);
        importer.process();

        m_modified = false;
    }

    private ControlFlowGraph(ControlFlowGraph source)
    {
        m_methodInfo = source.m_methodInfo;
        m_logger     = source.m_logger;
    }

    public ControlFlowGraphMapper createClone() throws
                                                AnalyzerException
    {
        ControlFlowGraph newCfg = new ControlFlowGraph(this);

        ControlFlowGraphMapper mapper = new ControlFlowGraphMapper(this, newCfg);
        Cloner                 cloner = new Cloner(mapper);

        //
        // First copy all the local variables.
        //
        for (Integer index : m_localVariables.keySet())
        {
            for (LocalVariable localVar : m_localVariables.get(index))
                cloner.clone(localVar);
        }

        newCfg.m_thisVar      = cloner.clone(m_thisVar);
        newCfg.m_argumentsVar = cloner.clone(m_argumentsVar);

        //
        // Then create the empty basic blocks.
        //
        for (BasicBlock bb : m_basicBlocks)
        {
            BasicBlock bbCloned = newCfg.newBasicBlock();
            mapper.put(bb, bbCloned);
        }

        //
        // Finally copy all the basic blocks and their instructions.
        //
        for (BasicBlock bb : m_basicBlocks)
        {
            BasicBlock bbCloned = cloner.clone(bb);
            bbCloned.clone(bb, cloner);
        }

        return mapper;
    }

    //--//

    public void accept(TypeVisitor visitor)
    {
        for (BasicBlock bb : m_basicBlocks)
            bb.accept(visitor);
    }

    public void accept(InstructionWriter visitor) throws
                                                  AnalyzerException
    {
        visitor.visitBasicBlocksStart();

        for (BasicBlock bb : m_basicBlocks)
        {
            visitor.visitBasicBlock(bb);
        }

        visitor.visitBasicBlocksEnd();

        List<TryCatchHandler> handlers = collectExceptionHandlersInNestingOrder(visitor);
        for (TryCatchHandler tch : handlers)
            visitor.visitExceptionHandler(tch);
    }

    //--//

    private List<TryCatchHandler> collectExceptionHandlersInNestingOrder(InstructionWriter visitor) throws
                                                                                                    AnalyzerException
    {
        List<TryCatchHandler> handlers = Lists.newArrayList(collectExceptionHandlers());

        //
        // Skip unused handlers.
        //
        handlers.removeIf((tch) -> tch.getCoveredBasicBlocks()
                                      .isEmpty());

        try
        {
            handlers.sort((left, right) ->
                          {
                              try
                              {
                                  Set<BasicBlock> leftCovered  = left.getCoveredBasicBlocks();
                                  Set<BasicBlock> rightCovered = right.getCoveredBasicBlocks();

                                  boolean leftInRight = rightCovered.containsAll(leftCovered);
                                  boolean rightInLeft = leftCovered.containsAll(rightCovered);

                                  if (leftInRight && rightInLeft)
                                  {
                                      //
                                      // The two handlers cover the same basic blocks.
                                      // Sort based on type hierarchy.
                                      //

                                      if (left.type == null)
                                      {
                                          // A finally block comes last, so put left *after* right.
                                          return 1;
                                      }

                                      if (right.type == null)
                                      {
                                          // A finally block comes last, so put left *before* right.
                                          return -1;
                                      }

                                      if (visitor.typeResolver.canCastTo(left.type, right.type))
                                      {
                                          // Left's exception is more generic than right's, put left *after* right.
                                          return 1;
                                      }

                                      if (visitor.typeResolver.canCastTo(right.type, left.type))
                                      {
                                          // Right's exception is more generic than left's, put left *before* right.
                                          return -1;
                                      }
                                  }
                                  else if (!leftInRight && rightInLeft)
                                  {
                                      // Right is nested inside left, put left *after* right.
                                      return 1;
                                  }
                                  else if (leftInRight && !rightInLeft)
                                  {
                                      // Left is nested inside right, put left *before* right.
                                      return -1;
                                  }
                                  else
                                  {
                                      //
                                      // Make sure the two handlers are truly disjoint.
                                      //
                                      SetView<BasicBlock> intersection = Sets.intersection(leftCovered, rightCovered);
                                      Preconditions.checkArgument(intersection.isEmpty());
                                  }

                                  List<BasicBlock> listLeft   = left.getSortedCoveredBasicBlocks();
                                  List<BasicBlock> listRight  = right.getSortedCoveredBasicBlocks();
                                  BasicBlock       firstLeft  = listLeft.get(0);
                                  BasicBlock       firstRight = listRight.get(0);

                                  //
                                  // Everything else the same, sort based on the sequence number of the first basic block in each set.
                                  //
                                  int diff = Integer.compare(firstLeft.getSequenceNumber(), firstRight.getSequenceNumber());
                                  if (diff == 0)
                                  {
                                      //
                                      // Same first basic block, sort based on the sequence number of the basic block for the handlers.
                                      //
                                      diff = Integer.compare(left.handler.getSequenceNumber(), right.handler.getSequenceNumber());
                                  }

                                  return diff;
                              }
                              catch (AnalyzerException e)
                              {
                                  // Wrap checked exception into runtime, due to lambda.
                                  throw new RuntimeException(e);
                              }
                          });

            return handlers;
        }
        catch (RuntimeException e)
        {
            if (e.getCause() instanceof AnalyzerException)
            {
                // Unwrap checked exception.
                AnalyzerException e2 = (AnalyzerException) e.getCause();
                throw e2;
            }

            throw e;
        }
    }

    //--//

    public LocalVariable getThisVar()
    {
        return m_thisVar;
    }

    public LocalVariable[] getArgumentVars()
    {
        return m_argumentsVar;
    }

    public GenericMethodInfo getContext()
    {
        return m_methodInfo;
    }

    public void setContext(GenericMethodInfo methodInfo)
    {
        m_methodInfo = methodInfo;
        setContextInner();
    }

    public void setDefaultSourceCode(SourceCodeInformation sci)
    {
        m_defaultSourceCode = sci;
    }

    public SourceCodeInformation getDefaultSourceCode()
    {
        return m_defaultSourceCode;
    }

    private void setContextInner()
    {
        Type[] argumentsType = m_methodInfo.getSignature()
                                           .getRawParameterTypes();
        m_argumentsVar = new LocalVariable[argumentsType.length];

        int local = 0;
        if (m_methodInfo.isStatic())
        {
            m_thisVar = null;
        }
        else
        {
            Type t = m_methodInfo.getDeclaringGenericType()
                                 .asType();
            m_thisVar = ensureLocalVariableAtIndex(local, t);
            local += t.getSize();
        }

        for (int i = 0; i < argumentsType.length; ++i)
        {
            Type t = argumentsType[i];
            m_argumentsVar[i] = ensureLocalVariableAtIndex(local, t);
            local += t.getSize();
        }
    }

    public LocalVariable getLocalVariableAtIndex(Integer index,
                                                 Type type)
    {
        Collection<LocalVariable> vars = m_localVariables.get(index);
        for (LocalVariable localVar : vars)
        {
            if (localVar.type.equals(type))
            {
                return localVar;
            }
        }

        return null;
    }

    public LocalVariable ensureLocalVariableAtIndex(Integer index,
                                                    Type type)
    {
        LocalVariable oldVar = getLocalVariableAtIndex(index, type);
        if (oldVar != null)
        {
            return oldVar;
        }

        flagAsModified();

        LocalVariable newVar = new LocalVariable(index, type);
        m_localVariables.put(index, newVar);
        return newVar;
    }

    public LocalVariable newLocalVariable(ClassAnalyzer ca)
    {
        return newLocalVariable(ca.genericTypeInfo);
    }

    public LocalVariable newLocalVariable(GenericTypeInfo gti)
    {
        return newLocalVariable(gti.asType());
    }

    public LocalVariable newLocalVariable(Type type)
    {
        int max = 0;

        for (LocalVariable var : m_localVariables.values())
            max = Math.max(max, var.getIndex() + var.type.getSize());

        return ensureLocalVariableAtIndex(max, type);
    }

    public void remapLocalVariablesInRange(Integer index,
                                           int width) throws
                                                      AnalyzerException
    {
        for (LocalVariable var : m_localVariables.get(index - 1))
        {
            if (var.type.getSize() == 2)
            {
                throw TypeResolver.reportProblem("Can't insert a variable at slot '%d', because another variable overlaps it: %s", index, var);
            }
        }

        Multimap<Integer, LocalVariable> remappedLocalVariables = HashMultimap.create();
        for (Integer indexOld : m_localVariables.keys())
        {
            Collection<LocalVariable> values = m_localVariables.get(indexOld);
            if (indexOld < index)
            {
                remappedLocalVariables.putAll(indexOld, values);
            }
            else
            {
                Integer indexNew = indexOld + width;

                for (LocalVariable var : values)
                    var.setIndex(indexNew);

                remappedLocalVariables.putAll(indexNew, values);
            }
        }
        m_localVariables.clear();
        m_localVariables.putAll(remappedLocalVariables);

        flagAsModified();
    }

    public LocalVariable insertLocalVariableAtIndex(Integer index,
                                                    Type type) throws
                                                               AnalyzerException
    {
        remapLocalVariablesInRange(index, type.getSize());

        return ensureLocalVariableAtIndex(index, type);
    }

    public GenericType getVariableGenericTypeIfAvailable(LocalVariable var,
                                                         boolean createIfNotFound)
    {
        for (BasicBlock bb : m_basicBlocks)
        {
            GenericType gt = bb.extractGenericType(var);
            if (gt != null)
            {
                return gt;
            }
        }

        if (createIfNotFound)
        {
            return m_methodInfo.typeResolver.getGenericTypeReference(var.type);
        }

        return null;
    }

    public String getVariableNameIfAvailable(LocalVariable var)
    {
        for (BasicBlock bb : m_basicBlocks)
        {
            String name = bb.extractName(var);
            if (name != null)
            {
                return name;
            }
        }

        return null;
    }

    //--//

    public void resetCode()
    {
        m_basicBlocks.clear();
        m_localVariables.clear();

        setContextInner();
    }

    void removeEmptyBasicBlocks()
    {
        for (int i = 0; i < m_basicBlocks.size(); i++)
        {
            BasicBlock bb = m_basicBlocks.get(i);
            if (bb.getNumberOfInstructions() == 0)
            {
                Preconditions.checkState(bb.getSuccessors()
                                           .isEmpty());
                Preconditions.checkState(bb.getPredecessors()
                                           .isEmpty());

                m_basicBlocks.remove(i--);
            }
        }
    }

    public BasicBlock newBasicBlock()
    {
        return newBasicBlock(null, false, false);
    }

    BasicBlock newBasicBlockBefore(BasicBlock target,
                                   boolean inheritExceptionHandlers)
    {
        return newBasicBlock(target, inheritExceptionHandlers, false);
    }

    BasicBlock newBasicBlockAfter(BasicBlock target,
                                  boolean inheritExceptionHandlers)
    {
        return newBasicBlock(target, inheritExceptionHandlers, true);
    }

    private BasicBlock newBasicBlock(BasicBlock target,
                                     boolean inheritExceptionHandlers,
                                     boolean insertAfter)
    {
        flagAsModified();

        BasicBlock newBlock = new BasicBlock(this);

        if (inheritExceptionHandlers)
        {
            for (TryCatchHandler tch : target.getExceptionHandlersInner()
                                             .values())
                newBlock.setExceptionHandler(tch);
        }

        if (target != null)
        {
            int index = getIndexOf(target);
            m_basicBlocks.add(insertAfter ? index + 1 : index, newBlock);
        }
        else
        {
            m_basicBlocks.add(newBlock);
        }

        flagAsModified();

        return newBlock;
    }

    int getIndexOf(BasicBlock basicBlock)
    {
        return m_basicBlocks.indexOf(basicBlock);
    }

    public int getNumberOfBasicBlocks()
    {
        return m_basicBlocks.size();
    }

    public BasicBlock getEntrypoint()
    {
        return getBasicBlockAt(0);
    }

    public BasicBlock getBasicBlockAt(int sequenceNumber)
    {
        return CollectionUtils.getNthElement(m_basicBlocks, sequenceNumber);
    }

    public Collection<BasicBlock> getBasicBlocks()
    {
        return Collections.unmodifiableList(m_basicBlocks);
    }

    public boolean visitInstructions(CheckedPredicate<AbstractInstruction> visitor) throws
                                                                                    AnalyzerException,
                                                                                    IOException
    {
        for (BasicBlock bb : BasicBlock.toArray(m_basicBlocks))
        {
            if (!bb.visitInstructions(visitor))
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

    public Set<TryCatchHandler> collectExceptionHandlers()
    {
        Set<TryCatchHandler> res = Sets.newHashSet();

        for (BasicBlock bb : m_basicBlocks)
        {
            for (TryCatchHandler tch : bb.getExceptionHandlers()
                                         .values())
                res.add(tch);
        }

        return res;
    }

    //--//

    public boolean wasModified()
    {
        return m_modified;
    }

    public void refreshFrameState() throws
                                    AnalyzerException
    {
        if (!m_frameStateComputed)
        {
            int           size = m_basicBlocks.size() + 10;
            FrameAnalyzer fa   = new FrameAnalyzer(this, m_logger, size * size);
            fa.computeStackFrames();

            m_frameStateComputed = true;
        }
    }

    void invalidateFrameState()
    {
        m_frameStateComputed = false;
    }

    void flagAsModified()
    {
        m_modified = true;

        invalidateFrameState();
    }

    //--//

    public class Dumper extends LogToStringVisitor
    {
        private final CodeAnalysisLogger m_logger;
        private final Level              m_level;

        public Dumper()
        {
            m_level  = CodeAnalysisLogger.Level.INFO;
            m_logger = CodeAnalysisLogger.createCallbackLogger(m_level, (s) -> log(s));
        }

        public List<String> execute()
        {
            clear();

            Set<TryCatchHandler> tchs          = collectExceptionHandlers();
            Set<BasicBlock>      handlerBlocks = Sets.newHashSet();
            for (TryCatchHandler tch : tchs)
            {
                logln("  %s", tch);
                handlerBlocks.add(tch.handler);
            }

            List<BasicBlock> workList = traverse(getEntrypoint());
            for (BasicBlock basicBlock : workList)
            {
                logln("");
                logln("Basicblock %d has %d instructions%s", basicBlock.getSequenceNumber(), basicBlock.getNumberOfInstructions(), handlerBlocks.contains(basicBlock) ? " EXCEPTION" : "");
                if (!basicBlock.getPredecessors()
                               .isEmpty())
                {
                    log("    << ");

                    for (BasicBlock pred : basicBlock.getPredecessors())
                        log(" %d ", pred.getSequenceNumber());

                    logln("");
                }

                for (SourceCodeVariableRange scvRange : basicBlock.getSourceCodeVariableRanges())
                {
                    if (scvRange.rangeStart < 0 && scvRange.rangeEnd < 0)
                    {
                        logln(" Local Variable: %s <defined everywhere>", scvRange.target);
                    }
                    else if (scvRange.rangeStart < 0)
                    {
                        logln(" Local Variable: %s <defined from start to %d>", scvRange.target, scvRange.rangeEnd);
                    }
                    else if (scvRange.rangeEnd < 0)
                    {
                        logln(" Local Variable: %s <defined from %d to end>", scvRange.target, scvRange.rangeStart);
                    }
                    else
                    {
                        logln(" Local Variable: %s <defined from %d to %d>", scvRange.target, scvRange.rangeStart, scvRange.rangeEnd);
                    }
                }

                for (AbstractInstruction insn : basicBlock.getInstructions())
                {
                    dumpInstruction(insn);
                    logln("");
                }

                if (!basicBlock.getSuccessors()
                               .isEmpty())
                {
                    log("    >> ");

                    for (BasicBlock succ : basicBlock.getSuccessors())
                        log(" %d ", succ.getSequenceNumber());

                    logln("");
                }

                if (!basicBlock.getExceptionHandlers()
                               .isEmpty())
                {
                    log("  EX>> ");

                    for (Entry<Type, TryCatchHandler> en : basicBlock.getExceptionHandlers()
                                                                     .entrySet())
                    {
                        Type            type = en.getKey();
                        TryCatchHandler tch  = en.getValue();
                        log(" %d(%s)", tch.handler.getSequenceNumber(), type == null ? "<finally>" : type.getClassName());
                    }

                    logln("");
                }
            }

            return getResults();
        }

        private List<BasicBlock> traverse(BasicBlock basicBlock)
        {
            Set<BasicBlock>  visited  = Sets.newHashSet();
            List<BasicBlock> workList = Lists.newArrayList();

            workList.add(basicBlock);

            while (!workList.isEmpty())
            {
                basicBlock = workList.remove(0);

                if (!visited.add(basicBlock))
                {
                    continue;
                }

                workList.addAll(basicBlock.getSuccessors());

                for (TryCatchHandler tch : basicBlock.getExceptionHandlers()
                                                     .values())
                    workList.add(tch.handler);
            }

            workList.addAll(visited);
            workList.sort((o1, o2) -> o1.getSequenceNumber() - o2.getSequenceNumber());

            return workList;
        }

        private void dumpInstruction(AbstractInstruction insn)
        {
            if (insn.sourceCode != null)
            {
                log("[Line: %-4d]", insn.sourceCode.lineNumber);
            }
            else
            {
                log("            ");
            }

            StringBuilder sb = new StringBuilder();
            insn.toString(sb);
            log(" %s", sb);

            if (insn.getFrameState() != null)
            {
                indentTo(160, 40);
                insn.getFrameState()
                    .log(m_logger, null, m_level, "");
            }
        }
    }
}
