/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.LoadConstantInstruction;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class InstructionWriter
{
    public final MethodNode   method;
    public final TypeResolver typeResolver;

    private final Map<SourceCodeVariable, LocalVariableNode> m_activeVariables;
    private final Map<AbstractInstruction, LabelNode>        m_labelsMap;
    private final Set<LabelNode>                             m_labelsEmitted;
    private       SourceCodeInformation                      m_lastLine;
    private       BasicBlock                                 m_lastBasicBlock;
    private       LabelNode                                  m_labelAfterCode;

    protected InstructionWriter(MethodNode method,
                                TypeResolver typeResolver)
    {
        this.method = method;
        this.typeResolver = typeResolver;

        m_activeVariables = Maps.newHashMap();
        m_labelsMap = Maps.newHashMap();
        m_labelsEmitted = Sets.newHashSet();
    }

    //--//

    public void visitBasicBlocksStart()
    {
        method.localVariables = null;
        method.instructions.clear();
        method.tryCatchBlocks.clear();

        m_labelsMap.clear();
        m_labelsEmitted.clear();
        m_lastLine = null;
        m_labelAfterCode = null;
    }

    public void visitBasicBlock(BasicBlock bb) throws
                                               AnalyzerException
    {
        LabelNode label = resolveLabel(bb);
        visitLabel(label);

        for (AbstractInstruction insn : bb.getInstructions())
        {
            processLocalVariableScopes(bb, insn);

            visitInstruction(insn);
        }

        m_lastBasicBlock = bb;
    }

    public void visitBasicBlocksEnd()
    {
        // Peephole optimization removing GOTO's to the next instruction.
        AbstractInsnNode insn = method.instructions.getFirst();
        while (insn != null)
        {
            AbstractInsnNode insnNext = insn.getNext();

            if (insn.getOpcode() == Opcodes.GOTO)
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                if (insn2.label == insnNext)
                {
                    method.instructions.remove(insn);
                }
            }

            insn = insnNext;
        }

        //
        // Flush the pending local variable definitions.
        //
        flushLocalVariableScopes();
    }

    public void visitInstruction(AbstractInstruction insn) throws
                                                           AnalyzerException
    {
        //
        // Emit a line number whenever we switch from one line to another.
        //
        if (insn.sourceCode != null)
        {
            if (m_lastLine != insn.sourceCode)
            {
                m_lastLine = insn.sourceCode;

                LabelNode label = resolveLabel(insn);
                visitLabel(label);
                visitInsn(null, new LineNumberNode(m_lastLine.lineNumber, label));
            }
        }

        //
        // Emit the instruction.
        //
        insn.accept(this);
    }

    public void visitExceptionHandler(TryCatchHandler tch)
    {
        List<BasicBlock> covered = tch.getSortedCoveredBasicBlocks();
        BasicBlock       start   = null;
        BasicBlock       end     = null;

        for (BasicBlock bb : covered)
        {
            if (end != null)
            {
                if (end.getSequenceNumber() + 1 == bb.getSequenceNumber())
                {
                    //
                    // Adjacent blocks, proceed.
                    //
                    end = bb;
                    continue;
                }

                //
                // Disjoint blocks, flush an entry in the TryCatch table.
                //
                flushExceptionHandler(tch, start, end);

                start = null;
            }

            if (start == null)
            {
                start = bb;
            }

            end = bb;
        }

        flushExceptionHandler(tch, start, end);
    }

    private void flushExceptionHandler(TryCatchHandler tch,
                                       BasicBlock start,
                                       BasicBlock end)
    {
        if (start != null)
        {
            LabelNode handlerNode = resolveLabel(tch.handler);
            LabelNode startNode   = resolveLabel(start);
            LabelNode endNode     = resolveLabelAfter(end);

            TryCatchBlockNode tcb = new TryCatchBlockNode(startNode, endNode, handlerNode, tch.type != null ? tch.type.getInternalName() : null);
            method.tryCatchBlocks.add(tcb);
        }
    }

    //--//

    public void visitOpcode(AbstractInstruction source,
                            int opcode)
    {
        visitInsn(source, new InsnNode(opcode));
    }

    public void visitLoadConstant(AbstractInstruction source,
                                  Object value) throws
                                                AnalyzerException
    {
        int opcode = LoadConstantInstruction.getOpcodeFor(value);
        switch (opcode)
        {
            case Opcodes.LDC:
            {
                LdcInsnNode insn = new LdcInsnNode(Opcodes.LDC);
                insn.cst = value;
                visitInsn(source, insn);
                return;
            }

            case Opcodes.BIPUSH:
                visitInsn(source, new IntInsnNode(Opcodes.BIPUSH, (int) value));
                return;

            case Opcodes.SIPUSH:
                visitInsn(source, new IntInsnNode(Opcodes.SIPUSH, (int) value));
                return;

            default:
                visitOpcode(source, opcode);
                return;
        }
    }

    public void visitLocalVarLoad(AbstractInstruction source,
                                  LocalVariable v) throws
                                                   AnalyzerException
    {
        int opcode;

        switch (v.type.getSort())
        {
            //@formatter:off
            case Type.BOOLEAN: opcode = Opcodes.ILOAD; break; 
            case Type.BYTE   : opcode = Opcodes.ILOAD; break; 
            case Type.CHAR   : opcode = Opcodes.ILOAD; break; 
            case Type.SHORT  : opcode = Opcodes.ILOAD; break; 
            case Type.INT    : opcode = Opcodes.ILOAD; break; 
            case Type.LONG   : opcode = Opcodes.LLOAD; break; 
            case Type.FLOAT  : opcode = Opcodes.FLOAD; break; 
            case Type.DOUBLE : opcode = Opcodes.DLOAD; break; 
            case Type.OBJECT : opcode = Opcodes.ALOAD; break;
            case Type.ARRAY  : opcode = Opcodes.ALOAD; break;
            //@formatter:on

            default:
                throw TypeResolver.reportProblem("Unexpected type '%s'", v.type);
        }

        visitInsn(source, new VarInsnNode(opcode, v.getIndex()));
    }

    public void visitLocalVarStore(AbstractInstruction source,
                                   LocalVariable v) throws
                                                    AnalyzerException
    {
        int opcode;

        switch (v.type.getSort())
        {
            //@formatter:off
            case Type.BOOLEAN: opcode = Opcodes.ISTORE; break; 
            case Type.BYTE   : opcode = Opcodes.ISTORE; break; 
            case Type.CHAR   : opcode = Opcodes.ISTORE; break; 
            case Type.SHORT  : opcode = Opcodes.ISTORE; break; 
            case Type.INT    : opcode = Opcodes.ISTORE; break; 
            case Type.LONG   : opcode = Opcodes.LSTORE; break; 
            case Type.FLOAT  : opcode = Opcodes.FSTORE; break; 
            case Type.DOUBLE : opcode = Opcodes.DSTORE; break; 
            case Type.OBJECT : opcode = Opcodes.ASTORE; break;
            case Type.ARRAY  : opcode = Opcodes.ASTORE; break;
            //@formatter:on

            default:
                throw TypeResolver.reportProblem("Unexpected type '%s'", v.type);
        }

        visitInsn(source, new VarInsnNode(opcode, v.getIndex()));
    }

    public void visitLocalVarIncrement(AbstractInstruction source,
                                       LocalVariable v,
                                       int increment)
    {
        visitInsn(source, new IincInsnNode(v.getIndex(), increment));
    }

    public void visitArrayLoad(AbstractInstruction source,
                               Type type) throws
                                          AnalyzerException
    {
        int opcode;

        if (TypeResolver.PlaceholderTypeForArray.equals(type))
        {
            opcode = Opcodes.AALOAD;
        }
        else if (TypeResolver.isArray(type))
        {
            switch (TypeResolver.getElementType(type)
                                .getSort())
            {
                //@formatter:off
                case Type.INT   : opcode = Opcodes.IALOAD; break; 
                case Type.LONG  : opcode = Opcodes.LALOAD; break; 
                case Type.FLOAT : opcode = Opcodes.FALOAD; break; 
                case Type.DOUBLE: opcode = Opcodes.DALOAD; break; 
                case Type.BYTE  : opcode = Opcodes.BALOAD; break;
                case Type.CHAR  : opcode = Opcodes.CALOAD; break;
                case Type.SHORT : opcode = Opcodes.SALOAD; break;
                //@formatter:on

                default:
                    throw TypeResolver.reportProblem("Unexpected type '%s'", type);
            }
        }
        else
        {
            throw TypeResolver.reportProblem("Unexpected type '%s'", type);
        }

        visitOpcode(source, opcode);
    }

    public void visitArrayStore(AbstractInstruction source,
                                Type type) throws
                                           AnalyzerException
    {
        int opcode;

        if (TypeResolver.PlaceholderTypeForArray.equals(type))
        {
            opcode = Opcodes.AASTORE;
        }
        else if (TypeResolver.isArray(type))
        {
            Type elementType = TypeResolver.getElementType(type);
            switch (elementType.getSort())
            {
                //@formatter:off
                case Type.INT   : opcode = Opcodes.IASTORE; break; 
                case Type.LONG  : opcode = Opcodes.LASTORE; break; 
                case Type.FLOAT : opcode = Opcodes.FASTORE; break; 
                case Type.DOUBLE: opcode = Opcodes.DASTORE; break; 
                case Type.BYTE  : opcode = Opcodes.BASTORE; break;
                case Type.CHAR  : opcode = Opcodes.CASTORE; break;
                case Type.SHORT : opcode = Opcodes.SASTORE; break;
                //@formatter:on

                default:
                    throw TypeResolver.reportProblem("Unexpected type '%s'", type);
            }
        }
        else
        {
            throw TypeResolver.reportProblem("Unexpected type '%s'", type);
        }

        visitOpcode(source, opcode);
    }

    public void visitArrayNew(AbstractInstruction source,
                              Type type) throws
                                         AnalyzerException
    {
        Preconditions.checkState(TypeResolver.isArray(type));

        Type elementType = TypeResolver.getElementType(type);
        if (TypeResolver.isObject(elementType))
        {
            visitTypeInsn(source, Opcodes.ANEWARRAY, elementType);
        }
        else
        {
            int operand;

            switch (elementType.getSort())
            {
                //@formatter:off
                case Type.BOOLEAN: operand = Opcodes.T_BOOLEAN; break;
                case Type.CHAR   : operand = Opcodes.T_CHAR   ; break;
                case Type.BYTE   : operand = Opcodes.T_BYTE   ; break;
                case Type.SHORT  : operand = Opcodes.T_SHORT  ; break;
                case Type.INT    : operand = Opcodes.T_INT    ; break;
                case Type.FLOAT  : operand = Opcodes.T_FLOAT  ; break;
                case Type.DOUBLE : operand = Opcodes.T_DOUBLE ; break;
                case Type.LONG   : operand = Opcodes.T_LONG   ; break;
                
                default: throw TypeResolver.reportProblem("Unexpected type '%s'", type);
                //@formatter:on
            }

            visitInsn(source, new IntInsnNode(Opcodes.NEWARRAY, operand));
        }
    }

    public void visitArrayNew(AbstractInstruction source,
                              Type type,
                              int dims)
    {
        visitInsn(source, new MultiANewArrayInsnNode(type.getInternalName(), dims));
    }

    public void visitField(AbstractInstruction source,
                           int opcode,
                           Type owner,
                           String name,
                           Type fieldType,
                           boolean isStatic)
    {
        visitInsn(source, new FieldInsnNode(opcode, owner.getInternalName(), name, fieldType.getDescriptor()));
    }

    public void visitSwitch(AbstractInstruction source,
                            int opcode,
                            Map<Integer, BasicBlock> switchLookup,
                            BasicBlock dflt) throws
                                             AnalyzerException
    {
        Integer[] keys = switchLookup.keySet()
                                     .stream()
                                     .sorted((o1, o2) -> o1.compareTo(o2))
                                     .toArray((s) -> new Integer[s]);

        int[]       keysUnboxed = new int[keys.length];
        LabelNode[] labels      = new LabelNode[keys.length];
        for (int i = 0; i < keys.length; i++)
        {
            Integer key = keys[i];
            keysUnboxed[i] = key;
            labels[i] = resolveLabel(switchLookup.get(key));
        }
        LabelNode dfltNode = resolveLabel(dflt);

        if (opcode == Opcodes.TABLESWITCH)
        {
            int min = keysUnboxed[0];
            int max = keysUnboxed[keys.length - 1];

            for (int i = min; i <= max; i++)
            {
                if (!switchLookup.containsKey(i))
                {
                    throw TypeResolver.reportProblem("Missing target for key '%d' in TABLESWITCH (range: %d..%d)", i, min, max);
                }
            }

            visitInsn(source, new TableSwitchInsnNode(min, max, dfltNode, labels));
        }
        else if (opcode == Opcodes.LOOKUPSWITCH)
        {
            visitInsn(source, new LookupSwitchInsnNode(dfltNode, keysUnboxed, labels));
        }
        else
        {
            throw TypeResolver.reportProblem("Unexpected opcode '0x%02d'", opcode);
        }
    }

    public void visitStackPop(AbstractInstruction source,
                              int slots) throws
                                         AnalyzerException
    {
        if (slots == 1)
        {
            visitOpcode(source, Opcodes.POP);
        }
        else if (slots == 2)
        {
            visitOpcode(source, Opcodes.POP2);
        }
        else
        {
            throw TypeResolver.reportProblem("Unexpected number of slots to pop: %d", slots);
        }
    }

    public void visitStackDup(AbstractInstruction source,
                              int slots,
                              int depth)
    {
        if (slots == 1 && depth == 0)
        {
            visitOpcode(source, Opcodes.DUP);
        }
        else if (slots == 1 && depth == 1)
        {
            visitOpcode(source, Opcodes.DUP_X1);
        }
        else if (slots == 1 && depth == 2)
        {
            visitOpcode(source, Opcodes.DUP_X2);
        }
        else if (slots == 2 && depth == 0)
        {
            visitOpcode(source, Opcodes.DUP2);
        }
        else if (slots == 2 && depth == 1)
        {
            visitOpcode(source, Opcodes.DUP2_X1);
        }
        else if (slots == 2 && depth == 2)
        {
            visitOpcode(source, Opcodes.DUP2_X2);
        }
    }

    public void visitJump(AbstractInstruction source,
                          int opcode,
                          BasicBlock target)
    {
        visitInsn(source, new JumpInsnNode(opcode, resolveLabel(target)));
    }

    public void visitTypeInsn(AbstractInstruction source,
                              int opcode,
                              Type type)
    {
        visitInsn(source, new TypeInsnNode(opcode, type.getInternalName()));
    }

    public void visitInvoke(AbstractInstruction source,
                            int opcode,
                            Type owner,
                            String name,
                            GenericType.MethodDescriptor signature,
                            boolean isInterface)
    {
        String methodDesc = signature.asRawType()
                                     .getDescriptor();
        visitInsn(source, new MethodInsnNode(opcode, owner.getInternalName(), name, methodDesc, isInterface));
    }

    public void visitInvokeDynamic(AbstractInstruction source,
                                   String name,
                                   GenericType.MethodDescriptor signature,
                                   Handle bsm,
                                   Object... bsmArgs)
    {
        String methodDesc = signature.asRawType()
                                     .getDescriptor();
        visitInsn(source, new InvokeDynamicInsnNode(name, methodDesc, bsm, bsmArgs));
    }

    //--//

    private void processLocalVariableScopes(BasicBlock bb,
                                            AbstractInstruction insn)
    {
        List<SourceCodeVariable> listOfActiveSourceCodeVariables = bb.getActiveSourceCodeVariablesAt(bb.getInstructionIndex(insn));

        for (SourceCodeVariable scv : listOfActiveSourceCodeVariables)
        {
            if (!m_activeVariables.containsKey(scv))
            {
                //
                // Found a new local variable scope.
                //
                LabelNode start = resolveLabel(insn);
                visitLabel(start);

                String desc = scv.getType()
                                 .getDescriptor();
                String sig = scv.signature.toString();

                if (sig.equals(desc))
                {
                    sig = null;
                }

                LocalVariableNode newLocalVar = new LocalVariableNode(scv.name, desc, sig, start, null, scv.getIndex());
                m_activeVariables.put(scv, newLocalVar);

                if (method.localVariables == null)
                {
                    method.localVariables = Lists.newArrayList();
                }

                method.localVariables.add(newLocalVar);
            }
        }

        Set<SourceCodeVariable> setOfPendingSourceCodeVariables = Sets.newHashSet(m_activeVariables.keySet());
        for (SourceCodeVariable scv : setOfPendingSourceCodeVariables)
        {
            if (!listOfActiveSourceCodeVariables.contains(scv))
            {
                //
                // Found the end of a local variable scope.
                //
                LabelNode end = resolveLabel(insn);
                visitLabel(end);

                LocalVariableNode localVar = m_activeVariables.remove(scv);
                localVar.end = end;
            }
        }
    }

    private void flushLocalVariableScopes()
    {
        LabelNode end = resolveLabelAfter(m_lastBasicBlock);

        for (LocalVariableNode localVar : m_activeVariables.values())
        {
            localVar.end = end;
        }
    }

    //--//

    private void visitLabel(LabelNode node)
    {
        if (m_labelsEmitted.add(node))
        {
            visitInsn(null, node);
        }
    }

    private LabelNode resolveLabel(BasicBlock bb)
    {
        return resolveLabel(bb.getFirstInstruction());
    }

    private LabelNode resolveLabelAfter(BasicBlock bb)
    {
        BasicBlock bbNext = bb.getNextBasicBlock();
        if (bbNext != null)
        {
            return resolveLabel(bbNext);
        }

        if (m_labelAfterCode == null)
        {
            m_labelAfterCode = new LabelNode();
            visitLabel(m_labelAfterCode);
        }

        return m_labelAfterCode;
    }

    private LabelNode resolveLabel(AbstractInstruction insn)
    {
        return m_labelsMap.computeIfAbsent(insn, k -> new LabelNode());
    }

    private void visitInsn(AbstractInstruction source,
                           AbstractInsnNode insn)
    {
        if (source != null)
        {
            insn.visibleTypeAnnotations = AbstractInstruction.cloneAnnotations(source.visibleTypeAnnotations);
            insn.invisibleTypeAnnotations = AbstractInstruction.cloneAnnotations(source.invisibleTypeAnnotations);
        }

        method.instructions.add(insn);
    }
}
