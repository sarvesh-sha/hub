/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Arrays;

import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.LocalVariable;
import com.optio3.codeanalysis.cfg.instruction.ArrayLengthInstruction;
import com.optio3.codeanalysis.cfg.instruction.BinaryOperationInstruction;
import com.optio3.codeanalysis.cfg.instruction.FieldGetInstruction;
import com.optio3.codeanalysis.cfg.instruction.InvokeInstruction;
import com.optio3.codeanalysis.cfg.instruction.LoadConstantInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableStoreInstruction;
import com.optio3.codeanalysis.cfg.instruction.TypeCheckInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnaryConditionalJumpInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnconditionalJumpInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

// @formatter:off
/**
 * 
 * As we transform classes, we need to add new methods or fields.
 * But the {@link java.lang.instrument.Instrumentation} API does not allow changing the schema of an already loaded class.
 * <br>
 * To do that, we need to intercept the calls to the {@link ClassLoader#defineClass(String, byte[], int, int)} method.
 * <p>
 * Problem is, the JVM really doesn't like to have foreign code (not coming from the java.* space) injected into the class loader. 
 * <p>
 * The solution is tricky: instead of injecting calls to code outside java.* packages, we abuse something we can change: {@link System#in}.
 * <p>
 * We set {@link System#in} to an instance of one of our classes, which also implements {@link java.lang.instrument.ClassFileTransformer}.
 * <p>
 * Then we instrument the {@link ClassLoader} to fetch {@link System#in}, cast it to {@link java.lang.instrument.ClassFileTransformer},
 * and finally call our code.
 * <br>
 * Now we are good to go (... and take a shower, I feel a bit dirty...).
 */
// @formatter:on
public class ClassLoaderInterceptor implements ClassFileTransformer
{
    class FakeInputStream extends InputStream implements ClassFileTransformer
    {
        InputStream in;

        public FakeInputStream(InputStream in)
        {
            this.in = in;
        }

        //--//

        // Forward InputStream calls to the original object.

        @Override
        public int read() throws
                          IOException
        {
            return in.read();
        }

        @Override
        public int read(byte[] b,
                        int off,
                        int len) throws
                                 IOException
        {
            return in.read(b, off, len);
        }

        @Override
        public void close() throws
                            IOException
        {
            in.close();
        }

        @Override
        public int available() throws
                               IOException
        {
            return in.available();
        }

        @Override
        public int read(byte[] b) throws
                                  IOException
        {
            return in.read(b);
        }

        @Override
        public long skip(long n) throws
                                 IOException
        {
            return in.skip(n);
        }

        @Override
        public boolean markSupported()
        {
            return in.markSupported();
        }

        @Override
        public synchronized void mark(int readlimit)
        {
            in.mark(readlimit);
        }

        @Override
        public synchronized void reset() throws
                                         IOException
        {
            in.reset();
        }

        //--//

        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer)
        {
            try
            {
                byte[] result = m_transformer.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
                if (result != null)
                {
                    return result;
                }
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }

            return classfileBuffer;
        }
    }

    //--//

    public static boolean Verbose = false;

    private final Instrumentation           m_inst;
    private final AsyncClassFileTransformer m_transformer;

    public ClassLoaderInterceptor(Instrumentation inst,
                                  AsyncClassFileTransformer transformer)
    {
        m_inst = inst;
        m_transformer = transformer;
    }

    public void execute()
    {
        try
        {
            m_inst.addTransformer(this, true);
            m_inst.retransformClasses(ClassLoader.class);
            m_inst.removeTransformer(this);
        }
        catch (UnmodifiableClassException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer)
    {
        if ("java/lang/ClassLoader".equals(className))
        {
            try
            {
                return instrumentClassLoader();
            }
            catch (IOException | AnalyzerException | SecurityException | NoSuchMethodException e)
            {
                e.printStackTrace();
            }
        }

        return null;
    }

    private byte[] instrumentClassLoader() throws
                                           AnalyzerException,
                                           IOException,
                                           NoSuchMethodException,
                                           SecurityException
    {
        TypeResolver.Loader loader = new TypeResolver.Loader()
        {
            @Override
            public URL locateResource(String internalClassName) throws
                                                                IOException
            {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                return cl.getResource(internalClassName + ".class");
            }
        };

        TypeResolver typeResolver = new TypeResolver(null, loader);

        TypeResolver.ClassLocator cl = typeResolver.getClassReader(Type.getInternalName(ClassLoader.class));
        ClassAnalyzer             ca = new ClassAnalyzer(typeResolver, cl);

        //
        // protected final Class<?> ClassLoader.defineClass(String name, byte[] b, int off, int len, ProtectionDomain protectionDomain)
        //
        Type[] defineClassArgumentTypes = toType(String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
        Type   defineClassReturnType    = Type.getType(Class.class);
        Type   classLoaderType          = Type.getType(ClassLoader.class);

        Type           md = Type.getMethodType(defineClassReturnType, defineClassArgumentTypes);
        MethodAnalyzer ma = ca.findMethod("defineClass", md);
        if (ma == null)
        {
            System.err.println("INTERNAL ERROR: Failed to find ClassLoader.defineClass method");
            return null;
        }

        ControlFlowGraph cfg = ma.parseControlFlowGraph(null);

        LocalVariable varThis = cfg.getLocalVariableAtIndex(0, classLoaderType);
        LocalVariable varName = cfg.getLocalVariableAtIndex(1, defineClassArgumentTypes[0]);
        LocalVariable varB    = cfg.getLocalVariableAtIndex(2, defineClassArgumentTypes[1]);
        LocalVariable varOff  = cfg.getLocalVariableAtIndex(3, defineClassArgumentTypes[2]);
        LocalVariable varLen  = cfg.getLocalVariableAtIndex(4, defineClassArgumentTypes[3]);
        LocalVariable varPD   = cfg.getLocalVariableAtIndex(5, defineClassArgumentTypes[4]);

        BasicBlock originalCode = cfg.getBasicBlockAt(0);
        BasicBlock entry        = originalCode.insertNewBasicBlockBefore(false);
        BasicBlock copyBuffer   = entry.insertNewBasicBlockAfter(false);
        BasicBlock redirect     = copyBuffer.insertNewBasicBlockAfter(false);

        {
            entry.addInstructions(new AbstractInstruction[] { new LocalVariableLoadInstruction(varOff),
                                                              new UnaryConditionalJumpInstruction(Opcodes.IFEQ, TypeResolver.TypeForObject, copyBuffer, redirect) });
        }

        {
            //
            // public static byte[] Arrays.copyOfRange(byte[] original, int from, int to)
            //
            Method            m   = Arrays.class.getDeclaredMethod("copyOfRange", byte[].class, int.class, int.class);
            GenericMethodInfo mdi = typeResolver.getGenericMethodInfo(m);

            copyBuffer.addInstructions(new AbstractInstruction[] { new LocalVariableLoadInstruction(varB),
                                                                   // original
                                                                   new LocalVariableLoadInstruction(varOff),
                                                                   // from
                                                                   new LocalVariableLoadInstruction(varLen),
                                                                   new LocalVariableLoadInstruction(varOff),
                                                                   new BinaryOperationInstruction(Opcodes.IADD),
                                                                   // to = len + off

                                                                   new InvokeInstruction(Opcodes.INVOKESTATIC, mdi, false),
                                                                   new LocalVariableStoreInstruction(varB),
                                                                   new LoadConstantInstruction(0),
                                                                   new LocalVariableStoreInstruction(varOff),
                                                                   new UnconditionalJumpInstruction(redirect) });
        }

        {
            //
            // public void ClassFileTransformer.transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
            //
            Method            m   = ClassFileTransformer.class.getDeclaredMethod("transform", ClassLoader.class, String.class, Class.class, ProtectionDomain.class, byte[].class);
            GenericMethodInfo mdi = typeResolver.getGenericMethodInfo(m);

            redirect.addInstructions(new AbstractInstruction[] { new FieldGetInstruction(Type.getType(System.class), "in", Type.getType(InputStream.class), true),
                                                                 new TypeCheckInstruction(Opcodes.CHECKCAST, Type.getType(ClassFileTransformer.class)),
                                                                 new LocalVariableLoadInstruction(varThis),
                                                                 // loader
                                                                 new LocalVariableLoadInstruction(varName),
                                                                 // className
                                                                 new LoadConstantInstruction(null),
                                                                 // classBeingRedefined
                                                                 new LocalVariableLoadInstruction(varPD),
                                                                 // protectionDomain
                                                                 new LocalVariableLoadInstruction(varB),
                                                                 // classfileBuffer
                                                                 new InvokeInstruction(Opcodes.INVOKEINTERFACE, mdi, true),
                                                                 new LocalVariableStoreInstruction(varB),
                                                                 new LocalVariableLoadInstruction(varB),
                                                                 new ArrayLengthInstruction(),
                                                                 new LocalVariableStoreInstruction(varLen),

                                                                 new UnconditionalJumpInstruction(originalCode), });
        }

        ma.accept(cfg);

        if (Verbose)
        {
            ControlFlowGraph.Dumper dumper = cfg.new Dumper();
            for (String line : dumper.execute())
                System.err.println(line);
        }

        byte[] bytecode = ca.encode();

        if (Verbose)
        {
            ClassReader cr2 = new ClassReader(bytecode);
            ClassNode   cn2 = new ClassNode(Opcodes.ASM7);
            cr2.accept(cn2, 0);

            CodeAnalysisLogger.printClass(cn2, null, System.err);
        }

        System.setIn(new FakeInputStream(System.in));

        return bytecode;
    }

    private Type[] toType(Class<?>... classes)
    {
        Type[] types = new Type[classes.length];
        for (int i = 0; i < types.length; i++)
            types[i] = Type.getType(classes[i]);

        return types;
    }
}
