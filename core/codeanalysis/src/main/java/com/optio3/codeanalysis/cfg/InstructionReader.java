/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.codeanalysis.cfg;

import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.instruction.ArrayLengthInstruction;
import com.optio3.codeanalysis.cfg.instruction.ArrayLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.ArrayNewInstruction;
import com.optio3.codeanalysis.cfg.instruction.ArrayNewMultiInstruction;
import com.optio3.codeanalysis.cfg.instruction.ArrayStoreInstruction;
import com.optio3.codeanalysis.cfg.instruction.BinaryConditionalJumpInstruction;
import com.optio3.codeanalysis.cfg.instruction.BinaryOperationInstruction;
import com.optio3.codeanalysis.cfg.instruction.ComparisonOperationInstruction;
import com.optio3.codeanalysis.cfg.instruction.ConversionOperationInstruction;
import com.optio3.codeanalysis.cfg.instruction.FieldGetInstruction;
import com.optio3.codeanalysis.cfg.instruction.FieldPutInstruction;
import com.optio3.codeanalysis.cfg.instruction.FrameInstruction;
import com.optio3.codeanalysis.cfg.instruction.InvokeDynamicInstruction;
import com.optio3.codeanalysis.cfg.instruction.InvokeInstruction;
import com.optio3.codeanalysis.cfg.instruction.LabelInstruction;
import com.optio3.codeanalysis.cfg.instruction.LineNumberInstruction;
import com.optio3.codeanalysis.cfg.instruction.LoadConstantInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableIncrementInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableStoreInstruction;
import com.optio3.codeanalysis.cfg.instruction.MonitorInstruction;
import com.optio3.codeanalysis.cfg.instruction.NewObjectInstruction;
import com.optio3.codeanalysis.cfg.instruction.NopInstruction;
import com.optio3.codeanalysis.cfg.instruction.ReturnInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackDupInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackPopInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackSwapInstruction;
import com.optio3.codeanalysis.cfg.instruction.SwitchLookupInstruction;
import com.optio3.codeanalysis.cfg.instruction.SwitchTableInstruction;
import com.optio3.codeanalysis.cfg.instruction.ThrowInstruction;
import com.optio3.codeanalysis.cfg.instruction.TypeCheckInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnaryConditionalJumpInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnaryOperationInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnconditionalJumpInstruction;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class InstructionReader
{
    private final TypeResolver                 m_typeResolver;
    private final GenericType.MethodDescriptor m_context;
    private final Type                         m_returnValue;

    protected InstructionReader(TypeResolver typeResolver,
                                GenericType.MethodDescriptor context)
    {
        m_typeResolver = typeResolver;
        m_context = context;

        m_returnValue = context.getRawReturnType();
    }

    public AbstractInstruction visitInsn(final AbstractInsnNode insn) throws
                                                                      AnalyzerException
    {
        AbstractInstruction res = visitInner(insn);

        res.visibleTypeAnnotations = AbstractInstruction.cloneAnnotations(insn.visibleTypeAnnotations);
        res.invisibleTypeAnnotations = AbstractInstruction.cloneAnnotations(insn.invisibleTypeAnnotations);

        return res;
    }

    private AbstractInstruction visitInner(final AbstractInsnNode insn) throws
                                                                        AnalyzerException
    {
        int opcode = insn.getOpcode();
        switch (opcode)
        {
            case Opcodes.NOP:
                return new NopInstruction();

            case Opcodes.ACONST_NULL:
                return new LoadConstantInstruction(null);

            case Opcodes.ICONST_M1:
            case Opcodes.ICONST_0:
            case Opcodes.ICONST_1:
            case Opcodes.ICONST_2:
            case Opcodes.ICONST_3:
            case Opcodes.ICONST_4:
            case Opcodes.ICONST_5:
                return new LoadConstantInstruction(opcode - Opcodes.ICONST_0);

            case Opcodes.LCONST_0:
            case Opcodes.LCONST_1:
                return new LoadConstantInstruction((long) (opcode - Opcodes.LCONST_0));

            case Opcodes.FCONST_0:
            case Opcodes.FCONST_1:
            case Opcodes.FCONST_2:
                return new LoadConstantInstruction((float) (opcode - Opcodes.FCONST_0));

            case Opcodes.DCONST_0:
            case Opcodes.DCONST_1:
                return new LoadConstantInstruction((double) (opcode - Opcodes.DCONST_0));

            case Opcodes.BIPUSH:
            case Opcodes.SIPUSH:
            {
                IntInsnNode insn2 = (IntInsnNode) insn;

                return new LoadConstantInstruction(insn2.operand);
            }

            case Opcodes.LDC:
            {
                LdcInsnNode insn2 = (LdcInsnNode) insn;

                return new LoadConstantInstruction(insn2.cst);
            }

            case Opcodes.ILOAD:
            case Opcodes.LLOAD:
            case Opcodes.FLOAD:
            case Opcodes.DLOAD:
            case Opcodes.ALOAD:
            {
                VarInsnNode insn2 = (VarInsnNode) insn;
                Type        type;

                switch (opcode)
                {
                    case Opcodes.ILOAD:
                        type = Type.INT_TYPE;
                        break;

                    case Opcodes.LLOAD:
                        type = Type.LONG_TYPE;
                        break;

                    case Opcodes.FLOAD:
                        type = Type.FLOAT_TYPE;
                        break;

                    case Opcodes.DLOAD:
                        type = Type.DOUBLE_TYPE;
                        break;

                    case Opcodes.ALOAD:
                        type = TypeResolver.TypeForObject; // We need to analyze the flow to know the type.
                        break;

                    default:
                        throw TypeResolver.reportProblem(insn, "INTERNAL ERROR");
                }

                return new LocalVariableLoadInstruction(resolveVariable(insn2, insn2.var, type, false));
            }

            case Opcodes.IALOAD:
            case Opcodes.LALOAD:
            case Opcodes.FALOAD:
            case Opcodes.DALOAD:
            case Opcodes.AALOAD:
            case Opcodes.BALOAD:
            case Opcodes.CALOAD:
            case Opcodes.SALOAD:
            {
                Type arrayType = ArrayLoadInstruction.getTypeForOpcode(opcode);
                if (arrayType == null)
                {
                    throw TypeResolver.reportProblem(insn, "INTERNAL ERROR");
                }

                return new ArrayLoadInstruction(arrayType);
            }

            case Opcodes.ISTORE:
            case Opcodes.LSTORE:
            case Opcodes.FSTORE:
            case Opcodes.DSTORE:
            case Opcodes.ASTORE:
            {
                VarInsnNode insn2 = (VarInsnNode) insn;
                Type        type;

                switch (opcode)
                {
                    case Opcodes.ISTORE:
                        type = Type.INT_TYPE;
                        break;

                    case Opcodes.LSTORE:
                        type = Type.LONG_TYPE;
                        break;

                    case Opcodes.FSTORE:
                        type = Type.FLOAT_TYPE;
                        break;

                    case Opcodes.DSTORE:
                        type = Type.DOUBLE_TYPE;
                        break;

                    case Opcodes.ASTORE:
                        type = TypeResolver.TypeForObject; // We need to analyze the flow to know the type.
                        break;

                    default:
                        throw TypeResolver.reportProblem(insn, "INTERNAL ERROR");
                }

                return new LocalVariableStoreInstruction(resolveVariable(insn2, insn2.var, type, true));
            }

            case Opcodes.IASTORE:
            case Opcodes.LASTORE:
            case Opcodes.FASTORE:
            case Opcodes.DASTORE:
            case Opcodes.AASTORE:
            case Opcodes.BASTORE:
            case Opcodes.CASTORE:
            case Opcodes.SASTORE:
            {
                Type arrayType = ArrayStoreInstruction.getTypeForOpcode(opcode);
                if (arrayType == null)
                {
                    throw TypeResolver.reportProblem(insn, "INTERNAL ERROR");
                }

                return new ArrayStoreInstruction(arrayType);
            }

            case Opcodes.POP:
                return new StackPopInstruction(1);

            case Opcodes.POP2:
                return new StackPopInstruction(2);

            case Opcodes.DUP:
                return new StackDupInstruction(1, 0);

            case Opcodes.DUP_X1:
                return new StackDupInstruction(1, 1);

            case Opcodes.DUP_X2:
                return new StackDupInstruction(1, 2);

            case Opcodes.DUP2:
                return new StackDupInstruction(2, 0);

            case Opcodes.DUP2_X1:
                return new StackDupInstruction(2, 1);

            case Opcodes.DUP2_X2:
                return new StackDupInstruction(2, 2);

            case Opcodes.SWAP:
                return new StackSwapInstruction();

            case Opcodes.IADD:
            case Opcodes.LADD:
            case Opcodes.FADD:
            case Opcodes.DADD:
            case Opcodes.ISUB:
            case Opcodes.LSUB:
            case Opcodes.FSUB:
            case Opcodes.DSUB:
            case Opcodes.IMUL:
            case Opcodes.LMUL:
            case Opcodes.FMUL:
            case Opcodes.DMUL:
            case Opcodes.IDIV:
            case Opcodes.LDIV:
            case Opcodes.FDIV:
            case Opcodes.DDIV:
            case Opcodes.IREM:
            case Opcodes.LREM:
            case Opcodes.FREM:
            case Opcodes.DREM:
            case Opcodes.ISHL:
            case Opcodes.LSHL:
            case Opcodes.ISHR:
            case Opcodes.LSHR:
            case Opcodes.IUSHR:
            case Opcodes.LUSHR:
            case Opcodes.IAND:
            case Opcodes.LAND:
            case Opcodes.IOR:
            case Opcodes.LOR:
            case Opcodes.IXOR:
            case Opcodes.LXOR:
                return new BinaryOperationInstruction(opcode);

            case Opcodes.LCMP:
            case Opcodes.FCMPL:
            case Opcodes.FCMPG:
            case Opcodes.DCMPL:
            case Opcodes.DCMPG:
                return new ComparisonOperationInstruction(opcode);

            case Opcodes.INEG:
            case Opcodes.LNEG:
            case Opcodes.FNEG:
            case Opcodes.DNEG:
                return new UnaryOperationInstruction(opcode);

            case Opcodes.IINC:
            {
                IincInsnNode insn2 = (IincInsnNode) insn;

                return new LocalVariableIncrementInstruction(resolveVariable(insn2, insn2.var, Type.INT_TYPE, false), insn2.incr);
            }

            case Opcodes.I2L:
            case Opcodes.I2F:
            case Opcodes.I2D:
            case Opcodes.L2I:
            case Opcodes.L2F:
            case Opcodes.L2D:
            case Opcodes.F2I:
            case Opcodes.F2L:
            case Opcodes.F2D:
            case Opcodes.D2I:
            case Opcodes.D2L:
            case Opcodes.D2F:
            case Opcodes.I2B:
            case Opcodes.I2C:
            case Opcodes.I2S:
                return new ConversionOperationInstruction(opcode);

            case Opcodes.IFEQ:
            case Opcodes.IFNE:
            case Opcodes.IFLT:
            case Opcodes.IFGE:
            case Opcodes.IFGT:
            case Opcodes.IFLE:
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                BasicBlock defaultTarget = resolveFallThrough(insn2);
                BasicBlock successTarget = resolveLabel(insn2.label);
                return new UnaryConditionalJumpInstruction(opcode, Type.INT_TYPE, defaultTarget, successTarget);
            }

            case Opcodes.IF_ICMPEQ:
            case Opcodes.IF_ICMPNE:
            case Opcodes.IF_ICMPLT:
            case Opcodes.IF_ICMPGE:
            case Opcodes.IF_ICMPGT:
            case Opcodes.IF_ICMPLE:
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                BasicBlock defaultTarget = resolveFallThrough(insn2);
                BasicBlock successTarget = resolveLabel(insn2.label);
                return new BinaryConditionalJumpInstruction(opcode, Type.INT_TYPE, defaultTarget, successTarget);
            }

            case Opcodes.IF_ACMPEQ:
            case Opcodes.IF_ACMPNE:
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                BasicBlock defaultTarget = resolveFallThrough(insn2);
                BasicBlock successTarget = resolveLabel(insn2.label);
                return new BinaryConditionalJumpInstruction(opcode, TypeResolver.TypeForObject, defaultTarget, successTarget);
            }

            case Opcodes.IFNULL:
            case Opcodes.IFNONNULL:
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                BasicBlock defaultTarget = resolveFallThrough(insn2);
                BasicBlock successTarget = resolveLabel(insn2.label);
                return new UnaryConditionalJumpInstruction(opcode, TypeResolver.TypeForObject, defaultTarget, successTarget);
            }

            case Opcodes.GOTO:
            {
                JumpInsnNode insn2 = (JumpInsnNode) insn;

                // Need to break basic blocks on the next instruction.
                @SuppressWarnings("unused") BasicBlock nonReachableTarget = resolveFallThrough(insn2);

                return new UnconditionalJumpInstruction(resolveLabel(insn2.label));
            }

            case Opcodes.JSR:
                throw TypeResolver.reportProblem(insn, "JSR not supported!");

            case Opcodes.RET:
                throw TypeResolver.reportProblem(insn, "RET not supported!");

            case Opcodes.TABLESWITCH:
            {
                TableSwitchInsnNode insn2 = (TableSwitchInsnNode) insn;

                SwitchTableInstruction newInsn = new SwitchTableInstruction(resolveLabel(insn2.dflt));

                for (int i = 0, key = insn2.min; key <= insn2.max; i++, key++)
                {
                    LabelNode  value  = insn2.labels.get(i);
                    BasicBlock target = resolveLabel(value);
                    newInsn.switchLookup.put(key, target);
                }

                return newInsn;
            }

            case Opcodes.LOOKUPSWITCH:
            {
                LookupSwitchInsnNode insn2 = (LookupSwitchInsnNode) insn;

                SwitchLookupInstruction newInsn = new SwitchLookupInstruction(resolveLabel(insn2.dflt));

                for (int i = 0; i < insn2.keys.size(); i++)
                {
                    Integer    key    = insn2.keys.get(i);
                    LabelNode  value  = insn2.labels.get(i);
                    BasicBlock target = resolveLabel(value);
                    newInsn.switchLookup.put(key, target);
                }

                return newInsn;
            }

            case Opcodes.IRETURN:
            case Opcodes.LRETURN:
            case Opcodes.FRETURN:
            case Opcodes.DRETURN:
            case Opcodes.ARETURN:
            case Opcodes.RETURN:
            {
                if ((opcode == Opcodes.RETURN) != (m_returnValue == Type.VOID_TYPE))
                {
                    throw TypeResolver.reportProblem(insn, "Incompatible return type");
                }

                return new ReturnInstruction(opcode, m_returnValue);
            }

            case Opcodes.GETSTATIC:
            case Opcodes.GETFIELD:
            case Opcodes.PUTSTATIC:
            case Opcodes.PUTFIELD:
            {
                FieldInsnNode insn2 = (FieldInsnNode) insn;

                Type owner     = Type.getObjectType(insn2.owner);
                Type fieldType = Type.getType(insn2.desc);

                switch (opcode)
                {
                    case Opcodes.GETSTATIC:
                        return new FieldGetInstruction(owner, insn2.name, fieldType, true);

                    case Opcodes.GETFIELD:
                        return new FieldGetInstruction(owner, insn2.name, fieldType, false);

                    case Opcodes.PUTSTATIC:
                        return new FieldPutInstruction(owner, insn2.name, fieldType, true);

                    case Opcodes.PUTFIELD:
                        return new FieldPutInstruction(owner, insn2.name, fieldType, false);

                    default:
                        throw TypeResolver.reportProblem(insn, "INTERNAL ERROR");
                }
            }

            case Opcodes.INVOKEVIRTUAL:
            case Opcodes.INVOKESPECIAL:
            case Opcodes.INVOKESTATIC:
            case Opcodes.INVOKEINTERFACE:
            {
                MethodInsnNode insn2 = (MethodInsnNode) insn;

                Type owner = Type.getObjectType(insn2.owner);

                GenericType.MethodDescriptor signature = m_typeResolver.parseGenericMethodSignature(insn2.desc, m_context);

                return new InvokeInstruction(opcode, owner, insn2.name, signature, insn2.itf);
            }

            case Opcodes.INVOKEDYNAMIC:
            {
                InvokeDynamicInsnNode insn2 = (InvokeDynamicInsnNode) insn;

                GenericType.MethodDescriptor signature = m_typeResolver.parseGenericMethodSignature(insn2.desc, m_context);

                return new InvokeDynamicInstruction(insn2.name, signature, insn2.bsm, insn2.bsmArgs);
            }

            case Opcodes.NEW:
            {
                TypeInsnNode insn2 = (TypeInsnNode) insn;

                return new NewObjectInstruction(Type.getObjectType(insn2.desc));
            }

            case Opcodes.NEWARRAY:
            {
                IntInsnNode insn2 = (IntInsnNode) insn;
                Type        type;

                switch (insn2.operand)
                {
                    case Opcodes.T_BOOLEAN:
                        type = TypeResolver.TypeForPrimitiveArrayOfBoolean;
                        break;

                    case Opcodes.T_CHAR:
                        type = TypeResolver.TypeForPrimitiveArrayOfChar;
                        break;

                    case Opcodes.T_BYTE:
                        type = TypeResolver.TypeForPrimitiveArrayOfByte;
                        break;

                    case Opcodes.T_SHORT:
                        type = TypeResolver.TypeForPrimitiveArrayOfShort;
                        break;

                    case Opcodes.T_INT:
                        type = TypeResolver.TypeForPrimitiveArrayOfInteger;
                        break;

                    case Opcodes.T_FLOAT:
                        type = TypeResolver.TypeForPrimitiveArrayOfFloat;
                        break;

                    case Opcodes.T_DOUBLE:
                        type = TypeResolver.TypeForPrimitiveArrayOfDouble;
                        break;

                    case Opcodes.T_LONG:
                        type = TypeResolver.TypeForPrimitiveArrayOfLong;
                        break;

                    default:
                        throw TypeResolver.reportProblem(insn, "Invalid array type");
                }

                return new ArrayNewInstruction(opcode, type);
            }

            case Opcodes.ANEWARRAY:
            {
                TypeInsnNode insn2 = (TypeInsnNode) insn;

                Type elementType = Type.getObjectType(insn2.desc);
                return new ArrayNewInstruction(opcode, Type.getType("[" + elementType));
            }

            case Opcodes.ARRAYLENGTH:
                return new ArrayLengthInstruction();

            case Opcodes.ATHROW:
                return new ThrowInstruction();

            case Opcodes.CHECKCAST:
            case Opcodes.INSTANCEOF:
            {
                TypeInsnNode insn2 = (TypeInsnNode) insn;

                Type type = Type.getObjectType(insn2.desc);

                return new TypeCheckInstruction(opcode, type);
            }

            case Opcodes.MONITORENTER:
            case Opcodes.MONITOREXIT:
                return new MonitorInstruction(opcode);

            case Opcodes.MULTIANEWARRAY:
            {
                MultiANewArrayInsnNode insn2 = (MultiANewArrayInsnNode) insn;

                Type arrayType = Type.getObjectType(insn2.desc);
                return new ArrayNewMultiInstruction(arrayType, insn2.dims);
            }

            default:
            {
                if (insn instanceof FrameNode)
                {
                    FrameNode insn2 = (FrameNode) insn;

                    return new FrameInstruction(insn2);
                }

                if (insn instanceof LineNumberNode)
                {
                    LineNumberNode insn2 = (LineNumberNode) insn;

                    return new LineNumberInstruction(insn2.start, insn2.line);
                }

                if (insn instanceof LabelNode)
                {
                    LabelNode insn2 = (LabelNode) insn;

                    return new LabelInstruction(insn2);
                }

                throw TypeResolver.reportProblem(insn, "Illegal opcode 0x%02x", insn.getOpcode());
            }
        }
    }

    protected abstract BasicBlock resolveLabel(LabelNode label) throws
                                                                AnalyzerException;

    protected abstract BasicBlock resolveFallThrough(JumpInsnNode jump) throws
                                                                        AnalyzerException;

    protected abstract LocalVariable resolveVariable(AbstractInsnNode insn,
                                                     int localIndex,
                                                     Type type,
                                                     boolean afterInstruction) throws
                                                                               AnalyzerException;
}
