/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import com.google.common.collect.Maps;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableIncrementInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableStoreInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.WellKnownContexts;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.analysis.AnalyzerException;

class FrameAnalyzer
{
    abstract class BasePendingFrame
    {
        final BasePendingFrame             parent;
        final int                          depth;
        final LinkedList<BasePendingFrame> pendingChildren = new LinkedList<>();

        CodeAnalysisLogger.IndentResource indentResource;
        BasicBlock                        bb;
        FrameState                        nextState;

        protected BasePendingFrame(BasePendingFrame parent)
        {
            this.parent = parent;
            this.depth  = parent != null ? parent.depth + 1 : 1;
        }

        void queueFrame(BasicBlock bb,
                        FrameState next)
        {
            var obj = new PendingRegularFrame(this);
            obj.bb        = bb;
            obj.nextState = next;
            pendingChildren.add(obj);
        }

        void queueExceptionFrame(TryCatchHandler tbh,
                                 FrameState commonExceptionFrame)
        {
            var obj = new PendingExceptionFrame(this);
            obj.tbh                  = tbh;
            obj.commonExceptionFrame = commonExceptionFrame;
            pendingChildren.add(obj);
        }

        abstract void enter();

        abstract void leave();
    }

    class PendingRegularFrame extends BasePendingFrame
    {
        PendingRegularFrame(BasePendingFrame parent)
        {
            super(parent);
        }

        @Override
        void enter()
        {
            indentResource = m_logger.indent("##");
            m_logger.debug(WellKnownContexts.FrameComputation, ">>");
        }

        @Override
        void leave()
        {
            m_logger.debug(WellKnownContexts.FrameComputation, "<<");
            indentResource.close();
        }
    }

    class PendingExceptionFrame extends BasePendingFrame
    {
        TryCatchHandler tbh;
        FrameState      commonExceptionFrame;

        PendingExceptionFrame(BasePendingFrame parent)
        {
            super(parent);
        }

        @Override
        void enter()
        {
            bb        = tbh.handler;
            nextState = new FrameState(commonExceptionFrame);

            Type type = tbh.type;

            m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Exception handler %s at %d", parent.bb.getSequenceNumber(), type, bb.getSequenceNumber());
            nextState.clearStack();
            nextState.push(FrameValue.createWithUnknownOrigin(type != null ? type : TypeResolver.TypeForThrowable));

            indentResource = m_logger.indent("EX##");
            m_logger.debug(WellKnownContexts.FrameComputation, ">>");
        }

        @Override
        void leave()
        {
            m_logger.debug(WellKnownContexts.FrameComputation, "<<");
            indentResource.close();
        }
    }

    private final ControlFlowGraph            m_cfg;
    private final CodeAnalysisLogger          m_logger;
    private final int                         m_maxFrameComputationDepth;
    private final Map<BasicBlock, FrameState> m_blockToFrame = Maps.newHashMap();

    FrameAnalyzer(ControlFlowGraph cfg,
                  CodeAnalysisLogger logger,
                  int maxFrameComputationDepth)
    {
        m_cfg                      = cfg;
        m_logger                   = logger;
        m_maxFrameComputationDepth = maxFrameComputationDepth;
    }

    void computeStackFrames() throws
                              AnalyzerException
    {
        FrameState current = prepareInitialFrame();

        m_logger.debug(WellKnownContexts.FrameComputation, "Initial frame state for method '%s'", m_cfg.getContext());
        current.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG, "   ");

        BasePendingFrame stack = new PendingRegularFrame(null);
        stack.queueFrame(m_cfg.getEntrypoint(), current);

        while (stack != null)
        {
            if (stack.depth > m_maxFrameComputationDepth)
            {
                throw TypeResolver.reportProblem("Too deep!");
            }

            if (!stack.pendingChildren.isEmpty())
            {
                stack = stack.pendingChildren.getFirst();
                continue;
            }

            if (stack.indentResource == null)
            {
                if (stack.parent == null)
                {
                    return;
                }

                stack.enter();

                BasicBlock bb        = stack.bb;
                FrameState nextState = stack.nextState;

                FrameState previousState = m_blockToFrame.get(bb);
                if (previousState != null)
                {
                    if (previousState.getStackDepth() != nextState.getStackDepth())
                    {
                        m_logger.error(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Stack frames from multiple paths have different stack depth!", bb.getSequenceNumber());
                        previousState.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "PREVIOUS: ");
                        nextState.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "NEXT    : ");

                        throw TypeResolver.reportProblem("Stack frames from multiple paths have different stack depth!");
                    }

                    try (CodeAnalysisLogger.IndentResource res = m_logger.indent("--"))
                    {
                        nextState = processMerge(bb, nextState, previousState);
                        if (nextState == null)
                        {
                            continue;
                        }
                    }
                }
                else
                {
                    m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Propagating new frame", bb.getSequenceNumber());
                    m_logger.debug(WellKnownContexts.FrameComputation, "");
                }

                nextState.freeze();
                m_blockToFrame.put(bb, nextState);

                m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Processing frame", bb.getSequenceNumber());
                nextState.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG, "NEW STATE: ");
                m_logger.debug(WellKnownContexts.FrameComputation, "");

                Collection<TryCatchHandler> exceptionHandlers = bb.getExceptionHandlers()
                                                                  .values();

                //
                // The frame state for the exception handlers is the frame state at the start of the basic block,
                // minus all the "killed" local variables.
                //
                FrameState commonExceptionFrame = exceptionHandlers.isEmpty() ? null : new FrameState(nextState);

                nextState = processInstructions(bb, nextState, commonExceptionFrame);

                nextState.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG, "NEXT    : ");

                if (m_logger.isEnabled(WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG))
                {
                    for (BasicBlock succ : bb.getSuccessors())
                    {
                        m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Forwarding processing to BASICBLOCK(%d)", bb.getSequenceNumber(), succ.getSequenceNumber());
                    }
                }

                for (BasicBlock succ : bb.getSuccessors())
                {
//            System.out.printf("%d BASICBLOCK(%d): %d\n", m_depth, bb.getSequenceNumber(), succ.getSequenceNumber());
                    stack.queueFrame(succ, nextState);
                }

                if (commonExceptionFrame != null)
                {
                    for (TryCatchHandler tbh : exceptionHandlers)
                    {
                        stack.queueExceptionFrame(tbh, commonExceptionFrame);
                    }
                }
            }
            else
            {
                stack.leave();

                for (AbstractInstruction insn : stack.bb.getInstructions())
                {
                    if (insn.getFrameState() == null)
                    {
                        m_logger.error(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Instruction without stack frame! [%s]", stack.bb.getSequenceNumber(), insn);

                        throw TypeResolver.reportProblem("Instruction without stack frame!");
                    }
                }

                BasePendingFrame parent = stack.parent;
                parent.pendingChildren.remove(stack);

                stack = parent;
            }
        }
    }

    private FrameState prepareInitialFrame()
    {
        FrameState current = new FrameState();

        LocalVariable thisVar = m_cfg.getThisVar();
        if (thisVar != null)
        {
            FrameValue v;

            if (m_cfg.getContext()
                     .isConstructor())
            {
                v = FrameValue.createUninitialized(thisVar.type);
            }
            else
            {
                v = FrameValue.createWithUnknownOrigin(thisVar.type);
            }

            current.setVariable(thisVar, v);
        }

        for (LocalVariable argVar : m_cfg.getArgumentVars())
            current.setVariable(argVar, FrameValue.createWithUnknownOrigin(argVar.type));

        return current;
    }

    private FrameState processMerge(final BasicBlock bb,
                                    final FrameState next,
                                    final FrameState previous) throws
                                                               AnalyzerException
    {
        m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Processing merge", bb.getSequenceNumber());
        previous.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG, "PREVIOUS: ");
        next.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.DEBUG, "NEXT    : ");

        //
        // Detected that there was already a frame associated with the basic block.
        // We need to perform a merge of the state, to get the types in common from all paths.
        //
        FrameState merged   = new FrameState(next);
        boolean    modified = false;

        for (Integer indexNext : next.getLocals())
        {
            FrameValue previousValue = previous.getLocal(indexNext);
            if (previousValue != null)
            {
                //
                // Case 1) "in both".
                //
                FrameValue nextValue = merged.getLocal(indexNext);

                if (previousValue.equals(nextValue))
                {
                    continue;
                }

                m_logger.debug(WellKnownContexts.FrameComputation, "Checking local %d:", indexNext);
                m_logger.debug(WellKnownContexts.FrameComputation, "    Previous: %s", previousValue);
                m_logger.debug(WellKnownContexts.FrameComputation, "    Next    : %s", nextValue);

                FrameValue mergedValue = nextValue.merge(m_cfg.getContext().typeResolver, previousValue);
                if (mergedValue == null)
                {
                    m_logger.debug(WellKnownContexts.FrameComputation, " ==> REMOVED, since the variable's types are not compatible from the two frames", indexNext);

                    // We don't set the 'modified' flag, because we are bringing 'merged' toward 'previous'.
                    merged.removeLocal(indexNext);
                    continue;
                }

                // Always use the merged value, since it's the one with the union sets of generators.
                if (nextValue != mergedValue)
                {
                    merged.setLocal(indexNext, mergedValue);
                }

                if (previousValue.equals(mergedValue))
                {
                    m_logger.debug(WellKnownContexts.FrameComputation, " ==> SAME");
                    continue;
                }

                // Found a common type, use that for the variable slot.
                modified = true;

                m_logger.debug(WellKnownContexts.FrameComputation, " ==> MERGED: %s", mergedValue);
            }
            else
            {
                //
                // Case 2) "in next but not in previous".
                //

                m_logger.debug(WellKnownContexts.FrameComputation, "Removed local %d since it's not present in both frames", indexNext);

                //
                // No need to set the 'modified' flag, since we are removing it from the 'next' frame.
                // In doing so, the new 'next' frame is closer to 'previous' one.
                //
                merged.removeLocal(indexNext);
            }
        }

        for (Integer indexPrevious : previous.getLocals())
        {
            if (next.getLocal(indexPrevious) == null)
            {
                //
                // Case 3) "in previous but not in next".
                //

                m_logger.debug(WellKnownContexts.FrameComputation, "Removed local %d since it was present only on the previous version of the frame", indexPrevious);

                modified = true;
            }
        }

        for (int i = 0; i < previous.getStackSlots(); i++)
        {
            FrameValue previousValue = previous.getStackVar(i);
            FrameValue nextValue     = merged.getStackVar(i);

            if (previousValue.equals(nextValue))
            {
                continue;
            }

            m_logger.debug(WellKnownContexts.FrameComputation, "Checking stack %d:", i);
            m_logger.debug(WellKnownContexts.FrameComputation, "    Previous: %s", previousValue);
            m_logger.debug(WellKnownContexts.FrameComputation, "    Next    : %s", nextValue);

            FrameValue mergedValue = nextValue.merge(m_cfg.getContext().typeResolver, previousValue);
            if (mergedValue == null)
            {
                m_logger.error(WellKnownContexts.FrameComputation, "Incompatible types to merge: %s <=> %s", previousValue, nextValue);
                previous.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "PREVIOUS: ");
                next.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "NEXT    : ");

                throw TypeResolver.reportProblem("Incompatible types to merge: %s <=> %s", previousValue, nextValue);
            }

            // Always use the merged value, since it's the one with the union sets of generators.
            if (nextValue != mergedValue)
            {
                merged.replaceStackValue(nextValue, mergedValue);
            }

            if (previousValue.equals(mergedValue))
            {
                m_logger.debug(WellKnownContexts.FrameComputation, " ==> SAME");
                continue;
            }

            // Found a common type, use that for the stack slot.
            modified = true;

            m_logger.debug(WellKnownContexts.FrameComputation, " ==> MERGED: %s", mergedValue);
        }

        if (!modified)
        {
            //
            // Nothing to do, frames are identical.
            //

            m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): No need to propagate merged frames", bb.getSequenceNumber());
            m_logger.debug(WellKnownContexts.FrameComputation, "");
            return null;
        }

        m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Propagating merged frames", bb.getSequenceNumber());
        m_logger.debug(WellKnownContexts.FrameComputation, "");

        return merged;
    }

    private FrameState processInstructions(BasicBlock bb,
                                           FrameState next,
                                           FrameState commonExceptionFrame) throws
                                                                            AnalyzerException
    {
        for (AbstractInstruction insn : bb.getInstructions())
        {
            next.freeze();
            insn.setFrameState(next);

            next.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.TRACE, "NEXT    : ");
            m_logger.trace(WellKnownContexts.FrameComputation, "Processing instruction %s", insn);

            int        expectedDepth = next.getStackDepth() - insn.popSize() + insn.pushSize();
            FrameState after         = insn.processStack(m_cfg.getContext().typeResolver, next);
            if (after.getStackDepth() != expectedDepth)
            {
                m_logger.error(WellKnownContexts.FrameComputation,
                               "INTERNAL ERROR: instruction '%s' did not update the stack appropriately: expected=%d, actual=%d",
                               insn,
                               expectedDepth,
                               after.getStackDepth());
                next.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "BEFORE  : ");
                after.log(m_logger, WellKnownContexts.FrameComputation, CodeAnalysisLogger.Level.ERROR, "AFTER   : ");

                throw TypeResolver.reportProblem("INTERNAL ERROR: instruction '%s' did not update the stack appropriately: expected=%d, actual=%d", insn, expectedDepth, after.getStackDepth());
            }
            next = after;

            if (commonExceptionFrame != null)
            {
                //
                // Look for "killed" local variables.
                //
                if (insn instanceof LocalVariableStoreInstruction || insn instanceof LocalVariableIncrementInstruction)
                {
                    LocalVariableInstruction insn2 = (LocalVariableInstruction) insn;

                    Integer    index       = insn2.localVariable.getIndex();
                    FrameValue originalVal = commonExceptionFrame.getLocal(index);
                    if (originalVal != null)
                    {
                        //
                        // The variable was available at the top of the basic block.
                        // Was it changed to something of a different type?
                        //
                        FrameValue modifiedVal = next.getLocal(index);
                        FrameValue mergedVal   = originalVal.merge(m_cfg.getContext().typeResolver, modifiedVal);
                        if (mergedVal == null)
                        {
                            //
                            // Nothing in common, kill the variable.
                            //
                            commonExceptionFrame.removeLocal(index);

                            m_logger.debug(WellKnownContexts.FrameComputation, "BASICBLOCK(%d): Killed variable %s for handlers, due to %s", bb.getSequenceNumber(), insn2.localVariable, insn);
                            m_logger.debug(WellKnownContexts.FrameComputation, "");
                        }
                        else if (!originalVal.equals(mergedVal))
                        {
                            //
                            // Found a common superclass, update the frame.
                            //
                            commonExceptionFrame.setLocal(index, mergedVal);
                        }
                    }
                }
            }
        }

        return next;
    }
}
