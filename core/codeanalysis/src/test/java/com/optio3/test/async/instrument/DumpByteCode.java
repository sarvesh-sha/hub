/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.async.instrument;

import java.io.PrintWriter;

import com.optio3.codeanalysis.logging.TextifierWithOrderedLabels;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class DumpByteCode
{
    public static boolean asCode = !false;

    public static class MyClassLoader extends ClassLoader
    {
        private final boolean m_verbose;
        private       String  m_target;
        private       byte[]  m_byteCode;

        public MyClassLoader(boolean verbose)
        {
            m_verbose = verbose;
        }

        @Override
        protected Class<?> loadClass(String name,
                                     boolean resolve) throws
                                                      ClassNotFoundException
        {
            if (name.equals(m_target))
            {
                if (m_byteCode != null)
                {
                    return defineClass(name, m_byteCode, 0, m_byteCode.length);
                }

                ClassReader cr;
                try
                {
                    cr = new ClassReader(name);
                    ClassNode cn = new ClassNode(Opcodes.ASM7);
                    cr.accept(cn, 0);

                    ClassWriter  cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES);
                    ClassVisitor cv;

                    if (m_verbose)
                    {
                        cv = new TraceClassVisitor(cw, asCode ? new ASMifier() : new TextifierWithOrderedLabels(cn), new PrintWriter(System.out));
                    }
                    else
                    {
                        cv = cw;
                    }

                    for (MethodNode method : cn.methods)
                    {
                        log("Method: %s %s", method.name, method.desc);
                        if (method.name.equals("test"))
                        {
                            Analyzer<BasicValue> a = new Analyzer<>(new BasicInterpreter());
                            a.analyze(name, method);
                            Frame<BasicValue>[] frames = a.getFrames();

                            AbstractInsnNode ptr = method.instructions.getFirst();

                            while (ptr != null)
                            {
                                if (ptr.getOpcode() == Opcodes.ILOAD)
                                {
                                    InsnList il = new InsnList();
                                    il.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
                                    il.add(new LdcInsnNode("test for " + name));
                                    il.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));

                                    method.instructions.insertBefore(ptr, il);
                                    break;
                                }

                                ptr = ptr.getNext();
                            }
                            Analyzer<BasicValue> a2 = new Analyzer<>(new BasicInterpreter());
                            a2.analyze(name, method);
                            Frame<BasicValue>[] frames2 = a2.getFrames();
                            log("Before: %d", frames.length);

                            for (int i = 0; i < frames.length; i++)
                            {
                                log("%d = %s", i, frames[i]);
                            }

                            log("After: %d", frames2.length);
                            for (int i = 0; i < frames2.length; i++)
                            {
                                log("%d = %s", i, frames2[i]);
                            }
                        }
                    }

                    cn.accept(cv);
                    byte[] buf = cw.toByteArray();
                    return defineClass(name, buf, 0, buf.length);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }

            return super.loadClass(name, resolve);
        }

        private void log(String format,
                         Object... args)
        {
            if (m_verbose)
            {
                System.out.println(String.format(format, args));
            }
        }

        public Class<?> reload(Class<?> clz) throws
                                             ClassNotFoundException
        {
            m_target = clz.getName();

            return loadClass(m_target);
        }

        public Class<?> reload(Class<?> clz,
                               byte[] byteCode) throws
                                                ClassNotFoundException
        {
            m_target = clz.getName();
            m_byteCode = byteCode;

            return loadClass(m_target);
        }
    }

    public static MyClassLoader installClassLoader(boolean verbose)
    {
        return new MyClassLoader(verbose);
    }
}
//
//public class DumperTest_ToInstrumentDump implements Opcodes
//{
//
//    public static byte[] dump() throws Exception
//    {
//
//        ClassWriter cw = new ClassWriter(0);
//        FieldVisitor fv;
//        MethodVisitor mv;
//        AnnotationVisitor av0;
//
//        cw.visit(52, ACC_PUBLIC + ACC_SUPER, "com/optio3/test/async/instrument/DumperTest$ToInstrument", null, "java/lang/Object", null);
//
//        cw.visitSource("DumperTest.java", null);
//
//        cw.visitInnerClass("com/optio3/test/async/instrument/DumperTest$ToInstrument", "com/optio3/test/async/instrument/DumperTest", "ToInstrument", ACC_PUBLIC + ACC_STATIC);
//
//        cw.visitInnerClass("com/optio3/test/async/instrument/DumperTest$ToInstrument$Inner", "com/optio3/test/async/instrument/DumperTest$ToInstrument", "Inner", ACC_PUBLIC + ACC_STATIC);
//
//        {
//            mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
//            mv.visitCode();
//            Label l0 = new Label();
//            mv.visitLabel(l0);
//            mv.visitLineNumber(17, l0);
//            mv.visitVarInsn(ALOAD, 0);
//            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
//            mv.visitInsn(RETURN);
//            Label l1 = new Label();
//            mv.visitLabel(l1);
//            mv.visitLocalVariable("this", "Lcom/optio3/test/async/instrument/DumperTest$ToInstrument;", null, l0, l1, 0);
//            mv.visitMaxs(1, 1);
//            mv.visitEnd();
//        }
//        {
//            mv = cw.visitMethod(ACC_PUBLIC, "init", "()V", null, null);
//            mv.visitCode();
//            Label l0 = new Label();
//            mv.visitLabel(l0);
//            mv.visitLineNumber(26, l0);
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            mv.visitLdcInsn("test");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//            Label l1 = new Label();
//            mv.visitLabel(l1);
//            mv.visitLineNumber(27, l1);
//            mv.visitInsn(RETURN);
//            Label l2 = new Label();
//            mv.visitLabel(l2);
//            mv.visitLocalVariable("this", "Lcom/optio3/test/async/instrument/DumperTest$ToInstrument;", null, l0, l2, 0);
//            mv.visitMaxs(2, 1);
//            mv.visitEnd();
//        }
//        {
//            mv = cw.visitMethod(ACC_PUBLIC, "test", "(II)I", null, null);
//            mv.visitCode();
//            Label l0 = new Label();
//            Label l1 = new Label();
//            Label l2 = new Label();
//            mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
//            mv.visitLabel(l0);
//            mv.visitLineNumber(33, l0);
//            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            mv.visitLdcInsn("test for com.optio3.test.async.instrument.DumperTest$ToInstrument");
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//            mv.visitVarInsn(ILOAD, 1);
//            mv.visitVarInsn(ILOAD, 2);
//            mv.visitInsn(IADD);
//            mv.visitLabel(l1);
//            mv.visitInsn(IRETURN);
//            mv.visitLabel(l2);
//            mv.visitLineNumber(35, l2);
//            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]
//            {
//                "java/lang/Exception"
//            });
//            mv.visitVarInsn(ASTORE, 3);
//            Label l3 = new Label();
//            mv.visitLabel(l3);
//            mv.visitLineNumber(37, l3);
//            mv.visitVarInsn(ALOAD, 3);
//            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "()V", false);
//            Label l4 = new Label();
//            mv.visitLabel(l4);
//            mv.visitLineNumber(38, l4);
//            mv.visitInsn(ICONST_M1);
//            mv.visitInsn(IRETURN);
//            Label l5 = new Label();
//            mv.visitLabel(l5);
//            mv.visitLocalVariable("this", "Lcom/optio3/test/async/instrument/DumperTest$ToInstrument;", null, l0, l5, 0);
//            mv.visitLocalVariable("i", "I", null, l0, l5, 1);
//            mv.visitLocalVariable("j", "I", null, l0, l5, 2);
//            mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, l3, l5, 3);
//            mv.visitMaxs(2, 4);
//            mv.visitEnd();
//        }
//        cw.visitEnd();
//
//        return cw.toByteArray();
//    }
//}
