/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.async.instrument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.DependencyAnalyzer;
import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.GenericType.FormalParameter;
import com.optio3.codeanalysis.GenericType.TypeArgument;
import com.optio3.codeanalysis.GenericTypeInfo;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.ControlFlowGraphMapper;
import com.optio3.codeanalysis.cfg.InstructionReader;
import com.optio3.codeanalysis.cfg.TypeVisitor;
import com.optio3.codeanalysis.cfg.instruction.NewObjectInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.codeanalysis.logging.ColumnarLogAdapter;
import com.optio3.codeanalysis.logging.LogVisitor;
import com.optio3.codeanalysis.logging.TextifierWithOrderedLabels;
import com.optio3.codeanalysis.logging.WellKnownContexts;
import com.optio3.serialization.Reflection;
import com.optio3.test.async.instrument.DumpByteCode.MyClassLoader;
import com.optio3.test.common.AutoRetryOnFailure;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.BufferUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.util.ASMifier;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

public class CodeAnalysisTest extends Optio3Test
{
    public static boolean dumpWithTextifier = !false;
    public static boolean dumpWithASMifier  = false;

    public static boolean useUnderline          = false;
    public static boolean dumpByteCodeOnFailure = false;
    public static boolean verboseDifferentDump  = false;

    private boolean m_verbose;

    public static class ToInstrument
    {
        public static class Inner
        {
            public int value;
        }

        public void init()
        {
            System.out.println("test");
        }

        public int test(int i,
                        int j)
        {
            try
            {
                return i + j;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return -1;
            }
        }
    }

    //--//

    @Before
    public void setVerboseLevel()
    {
        m_verbose = failedOnFirstRun();
    }

    //--//

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testSignatures() throws
                                 AnalyzerException
    {
        GenericType gt1 = roundTripSignature("(ILcom/optio3/asyncawait/AbstractStateMachine$Callback<Ljava/lang/Integer;>;)V");
        Type        t1  = gt1.asRawType();
        assertEquals("(ILcom/optio3/asyncawait/AbstractStateMachine$Callback;)V", t1.toString());

        GenericType gt2 = roundTripSignature("Lcom/optio3/asyncawait/AbstractStateMachine$Callback<Ljava/lang/Integer;>;");
        Type        t2  = gt2.asRawType();
        assertEquals("com/optio3/asyncawait/AbstractStateMachine$Callback", t2.getInternalName());

        GenericType gt3 = roundTripSignature(
                "<T:Lcom/optio3/protocol/bacnet/model/pdu/request/ConfirmedServiceRequest;U:Lcom/optio3/protocol/bacnet/model/pdu/request/ConfirmedServiceResponse<TT;>;>Ljava/util/concurrent/CompletableFuture<TU;>;Lcom/optio3/protocol/bacnet/IApplicationPduListener;");
        Type t3 = gt3.asRawType();
        assertEquals("java/util/concurrent/CompletableFuture", t3.getInternalName());

        GenericType gt4 = roundTripSignature("<V:Ljava/lang/Object;>(Ljava/util/concurrent/CompletableFuture<TV;>;Ljava/util/function/Consumer<TV;>;Ljava/lang/Runnable;)V");
        Type        t4  = gt4.asRawType();
        assertEquals("(Ljava/util/concurrent/CompletableFuture;Ljava/util/function/Consumer;Ljava/lang/Runnable;)V", t4.toString());

        GenericType gt5 = roundTripSignature(
                "<V:Lcom/optio3/test/async/instrument/SampleTestCode;>Lcom/optio3/test/async/instrument/SampleTestCode$SomeGenericType<TV;Ljava/lang/Integer;>.SomeNestedType<Ljava/util/ArrayList<Ljava/lang/String;>;>;");
        Type t5 = gt5.asRawType();
        assertEquals("Lcom/optio3/test/async/instrument/SampleTestCode$SomeGenericType$SomeNestedType;", t5.toString());

        //--//

        TypeResolver typeResolver = new TypeResolver(null, null);

        GenericTypeInfo       tiNestedType1 = typeResolver.getGenericTypeInfo(SampleTestCode.SomeGenericType.AnotherLayer.SomeNestedType.class);
        List<FormalParameter> fps1          = tiNestedType1.getSignature().formalParameters;
        assertEquals(1, fps1.size());
        GenericType.FormalParameter fp1 = fps1.get(0);
        assertNull(fp1.classBound);
        assertEquals("Ljava/util/List;",
                     fp1.asRawType()
                        .toString());

        GenericTypeInfo       tiNestedType2 = typeResolver.getGenericTypeInfo(SampleTestCode.SomeGenericType.AnotherLayer.InstanceOfNestedType.class);
        List<FormalParameter> fps2          = tiNestedType2.getSignature().formalParameters;
        assertEquals(1, fps2.size());
        GenericType.FormalParameter fp2 = fps2.get(0);
        assertEquals("Lcom/optio3/test/async/instrument/SampleTestCode;",
                     fp2.classBound.asRawType()
                                   .toString());

        GenericType.TypeReference tr2  = tiNestedType2.getSuperclass();
        List<TypeArgument>        tas2 = tr2.typeArguments;
        assertEquals(1, tas2.size());
        TypeArgument ta2 = tas2.get(0);
        assertEquals("Ljava/util/ArrayList<Ljava/lang/String;>;", ta2.bound.toString());

        GenericType arg1 = typeResolver.parseGenericTypeReference(Type.getType(SampleTestCode.class)
                                                                      .getDescriptor(), null);
        GenericType arg2 = typeResolver.parseGenericTypeReference(Type.getType(String.class)
                                                                      .getDescriptor(), null);
        GenericType arg3 = typeResolver.parseGenericTypeReference("Ljava/util/List<*>;", null);

        GenericTypeInfo           tiNestedType3 = typeResolver.getGenericTypeInfo(SampleTestCode.SomeGenericType.AnotherLayer.InstanceOfNestedType2.class);
        GenericType.TypeReference ref1          = tiNestedType1.createReferenceForSubclass(arg1, arg2, arg3);
        assertEquals(
                "Lcom/optio3/test/async/instrument/SampleTestCode$SomeGenericType<Lcom/optio3/test/async/instrument/SampleTestCode;Ljava/lang/String;>.AnotherLayer.SomeNestedType<Ljava/util/List<*>;>;",
                ref1.toString());
        assertEquals(tiNestedType3.getSuperclass(), ref1);
    }

    private GenericType roundTripSignature(String sig)
    {
        GenericType gt = GenericType.parse(sig, null);
        assertEquals(sig, gt.toString());
        return gt;
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testInstrumentingCode() throws
                                        Exception
    {
        MyClassLoader loader = DumpByteCode.installClassLoader(m_verbose);

        Class<?> clz2 = loader.reload(ToInstrument.class);
        log("%s", clz2 == ToInstrument.class);
        Object obj = Reflection.newInstance(clz2);
        Method m = obj.getClass()
                      .getMethod("test", Integer.TYPE, Integer.TYPE);
        Object res = m.invoke(obj, 1, 2);
        log("%d", res);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void testInsertingVariable() throws
                                        Exception
    {
        MyClassLoader loader = DumpByteCode.installClassLoader(m_verbose);

        Class<?> clz = SampleTestCode.ToTestVariableInsertion.class;
        try
        {
            boolean oldVerbose = m_verbose;
            m_verbose = false;
            insertVariableWorker(clz, 2);
            m_verbose = oldVerbose;

            fail("This should have failed");
        }
        catch (AnalyzerException e)
        {
            assertTrue(e.getMessage()
                        .contains("Unexpected access to variable '4 (I)'"));
        }

        ClassAnalyzer ca = insertVariableWorker(clz, 3);

        Class<?> clz2 = loader.reload(clz, ca.encode());
        Object   obj  = Reflection.newInstance(clz2);
        Method m = obj.getClass()
                      .getMethod("test", Integer.TYPE, Integer.TYPE);
        Object res = m.invoke(obj, 1, 10);
        logln("%d", res);
    }

    private ClassAnalyzer insertVariableWorker(Class<?> clz,
                                               int index) throws
                                                          IOException,
                                                          AnalyzerException
    {
        TypeResolver              typeResolver = new TypeResolver(null, null);
        TypeResolver.ClassLocator cl           = typeResolver.getClassReader(clz);
        ClassAnalyzer             ca           = new ClassAnalyzer(typeResolver, cl);

        if (m_verbose)
        {
            logln("");
            logln("testInsertingVariable: BEFORE");
            CodeAnalysisLogger.printClass(ca.getClassNode(), null, System.out);
            logln("");
        }

        Type             type = Type.getType("(II)I");
        MethodAnalyzer   ma   = ca.findMethod("test", type);
        ControlFlowGraph cfg  = ma.parseControlFlowGraph(null);
        cfg.insertLocalVariableAtIndex(index, Type.DOUBLE_TYPE);
        ma.accept(cfg);

        ClassAnalyzer    ca2  = new ClassAnalyzer(null, new TypeResolver.ClassLocator(ca.encode()));
        MethodAnalyzer   ma2  = ca2.findMethod("test");
        ControlFlowGraph cfg2 = ma2.parseControlFlowGraph(null);
        cfg2.refreshFrameState();

        if (m_verbose)
        {
            logln("testInsertingVariable: AFTER");
            ClassAnalyzer ca3 = new ClassAnalyzer(null, new TypeResolver.ClassLocator(ca.encode()));
            CodeAnalysisLogger.printClass(ca3.getClassNode(), null, System.out);
            logln("");
        }

        return ca;
    }

    @Ignore("Only enable to check a new Java release")
    @Test
    public void checkForJSR() throws
                              Exception
    {
        Reflections reflections = new Reflections(null, new SubTypesScanner(false));

        int typesChecked   = 0;
        int methodsChecked = 0;
        int jsrFound       = 0;
        int retFound       = 0;

        for (String type : reflections.getAllTypes())
        {
            typesChecked++;
            //            logln("Checking type %s...", type);

            ClassNode classNode = new ClassNode(Opcodes.ASM7);

            ClassReader cr = new ClassReader(type);
            cr.accept(classNode, 0);

            for (MethodNode method : classNode.methods)
            {
                methodsChecked++;

                for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext())
                {
                    if (insn.getOpcode() == Opcodes.JSR)
                    {
                        logln("%s : %s : %s = JSR", type, method.name, method.desc);
                        jsrFound++;
                        break;
                    }

                    if (insn.getOpcode() == Opcodes.RET)
                    {
                        logln("%s : %s : %s = RET", type, method.name, method.desc);
                        retFound++;
                        break;
                    }
                }
            }
        }

        logln("Scanned %d types, %d methods. Found %d uses of JSR, %d uses of RET", typesChecked, methodsChecked, jsrFound, retFound);

        assertEquals(0, jsrFound);
        assertEquals(0, retFound);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void analyzeDumperTest() throws
                                    IOException,
                                    AnalyzerException
    {
        analyze(CodeAnalysisTest.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void analyzeSampleTestCode() throws
                                        IOException,
                                        AnalyzerException
    {
        analyze(SampleTestCode.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void analyzeGenericType() throws
                                     IOException,
                                     AnalyzerException
    {
        analyze(SampleTestCode.SomeGenericType.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void analyzeGenericNestedType() throws
                                           IOException,
                                           AnalyzerException
    {
        ClassAnalyzer ca = analyze(SampleTestCode.SomeGenericType.AnotherLayer.SomeNestedType.class);

        Method[] methods = SampleTestCode.SomeGenericType.AnotherLayer.SomeNestedType.class.getDeclaredMethods();
        for (Method m : methods)
        {
            GenericMethodInfo gmi = ca.genericTypeInfo.typeResolver.getGenericMethodInfo(m);
            assertNotNull(gmi);
            GenericMethodInfo gmi2 = ca.genericTypeInfo.get(m);
            assertNotNull(gmi2);

            assertEquals(gmi, gmi2);
        }
    }

    private ClassAnalyzer analyze(Class<?> clz) throws
                                                IOException,
                                                AnalyzerException
    {
        TypeResolver              typeResolver = new TypeResolver(null, null);
        TypeResolver.ClassLocator cl           = typeResolver.getClassReader(clz);
        ClassAnalyzer             ca           = new ClassAnalyzer(typeResolver, cl);

        ClassNode cn = ca.getClassNode();

        if (failedOnFirstRun())
        {
            dumpClassNode(cn);
        }

        for (MethodAnalyzer ma : ca.methods)
        {
            MethodNode mn = ma.getMethodNode();

            logln("");
            logln("##################################################################################################################################################################");
            logln("##################################################################################################################################################################");
            logln("##################################################################################################################################################################");
            logln("");

            logln("%s %s", ma.getName(), ma.getSignature());

            TraceMethodVisitor visitor1 = new TraceMethodVisitor(new TextifierWithOrderedLabels(mn));

            for (TryCatchBlockNode block : mn.tryCatchBlocks)
            {
                dumpTryCatchBlock(block, visitor1);
            }

            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());

            Frame<BasicValue>[] frames = analyzer.analyze(cn.name, mn);

            for (int i = 0; i < frames.length; i++)
            {
                AbstractInsnNode  insn  = mn.instructions.get(i);
                Frame<BasicValue> frame = frames[i];

                if (frame != null)
                {
                    log("%-3d : locals=%-2d, stack=%-2d             ", i, frame.getLocals(), frame.getStackSize());
                    for (int j = 0; j < frame.getLocals(); j++)
                    {
                        BasicValue v = frame.getLocal(j);
                        log(" %s", v);
                    }
                    log("  S=");
                    for (int j = 0; j < frame.getStackSize(); j++)
                    {
                        BasicValue v = frame.getStack(j);
                        log(" %s", v);
                    }

                    indentTo(0, 40);
                }

                dumpInstruction(visitor1, insn, -1);
            }
            logln("");

            CodeAnalysisLogger logger = new CodeAnalysisLogger()
            {
                @Override
                protected void logInner(String context,
                                        Level level,
                                        String msg)
                {
                    CodeAnalysisLogger.consoleLogger.log(context, level, msg);
                }

                @Override
                public boolean isEnabled(String context,
                                         Level level)
                {
                    if (level.ordinal() <= CodeAnalysisLogger.Level.WARN.ordinal())
                    {
                        return true;
                    }

                    if (!WellKnownContexts.FrameComputation.equals(context))
                    {
                        return false;
                    }

                    if (!m_verbose)
                    {
                        return false;
                    }

                    if (level.ordinal() >= CodeAnalysisLogger.Level.DEBUG.ordinal())
                    {
                        return true;
                    }

                    return false;
                }
            };

            ControlFlowGraph cfg = ma.parseControlFlowGraph(logger);

            logln("FIRST PASS:");
            cfg.refreshFrameState();
            ControlFlowGraph.Dumper dumperFirstPass = cfg.new Dumper();
            for (String line : dumperFirstPass.execute())
                logln("%s", line);

            //
            // For code coverage.
            //
            {
                cfg.createClone();
            }

            //
            // Break basic blocks at each NEW instruction.
            //
            int sequenceNumber = 0;
            while (true)
            {
                BasicBlock bb = cfg.getBasicBlockAt(sequenceNumber++);
                if (bb == null)
                {
                    break;
                }

                for (AbstractInstruction insn : bb.getInstructions())
                {
                    //
                    // For code coverage.
                    //
                    {
                        StringBuilder sb = new StringBuilder();
                        insn.toString(sb);
                        insn.toString();
                    }

                    if (insn instanceof NewObjectInstruction)
                    {
                        if (bb.splitAt(insn) != null)
                        {
                            break;
                        }
                    }
                }
            }

            logln("SECOND PASS:");
            cfg.refreshFrameState();
            ControlFlowGraph.Dumper dumperSecondPass = cfg.new Dumper();
            for (String line : dumperSecondPass.execute())
                logln("%s", line);

            //--//

            Set<Type> seenTypes = Sets.newHashSet();
            TypeVisitor typeVisitor = new TypeVisitor()
            {

                @Override
                public void visitType(Type type)
                {
                    if (type != null)
                    {
                        seenTypes.add(type);
                    }
                }
            };

            cfg.accept(typeVisitor);

            logln("List of types:");
            seenTypes.stream()
                     .map((t) -> t.getClassName())
                     .sorted()
                     .forEach((s) -> logln("   %s", s));
            logln("");

            //--//

            ma.accept(cfg);

            visitor1 = new TraceMethodVisitor(new TextifierWithOrderedLabels(mn));

            for (TryCatchBlockNode block : mn.tryCatchBlocks)
            {
                dumpTryCatchBlock(block, visitor1);
            }

            for (int i = 0; i < mn.instructions.size(); i++)
            {
                AbstractInsnNode insn = mn.instructions.get(i);
                dumpInstruction(visitor1, insn, -1);
            }
        }

        return ca;
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void roundTripDumperTest() throws
                                      IOException,
                                      AnalyzerException,
                                      InterruptedException
    {
        roundTrip(CodeAnalysisTest.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void roundTripSampleTestCode() throws
                                          IOException,
                                          AnalyzerException,
                                          InterruptedException
    {
        roundTrip(SampleTestCode.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void roundTripControlFlowGraph() throws
                                            IOException,
                                            AnalyzerException,
                                            InterruptedException
    {
        roundTrip(ControlFlowGraph.class);
    }

    @Test
    @AutoRetryOnFailure(retries = 1, reason = "First run is quiet, retry is verbose")
    public void roundTripInstructionReader() throws
                                             IOException,
                                             AnalyzerException,
                                             InterruptedException
    {
        roundTrip(InstructionReader.class);
    }

    private void roundTrip(Class<?> clz) throws
                                         IOException,
                                         AnalyzerException,
                                         InterruptedException
    {
        byte[] originalByteCode = readClass(clz);

        ClassAnalyzer ca                = new ClassAnalyzer(null, new TypeResolver.ClassLocator(originalByteCode));
        ClassNode     cn                = ca.getClassNode();
        byte[]        originalByteCode2 = reencode(ca);

        if (m_verbose)
        {
            logln("");
            logln("##################################################################################################################################################################");
            logln("##################################################################################################################################################################");
            logln("##################################################################################################################################################################");
            logln("");
            logln("BEFORE:");
            logln("");

            dumpClassNode(cn);
        }

        for (MethodAnalyzer ma : ca.methods)
        {
            if (ma.hasCode())
            {
                ControlFlowGraph cfg = ma.parseControlFlowGraph(null);

                cfg.refreshFrameState();

                int sequenceNumber = 0;
                while (true)
                {
                    BasicBlock bb = cfg.getBasicBlockAt(sequenceNumber++);
                    if (bb == null)
                    {
                        break;
                    }

                    for (AbstractInstruction insn : bb.getInstructions())
                    {
                        //
                        // For code coverage.
                        //
                        {
                            StringBuilder sb = new StringBuilder();
                            insn.toString(sb);
                            insn.toString();
                        }

                        if (insn instanceof NewObjectInstruction)
                        {
                            if (bb.splitAt(insn) != null)
                            {
                                break;
                            }
                        }
                    }
                }

                logln("Emitting %s...", ma.getName());
                ControlFlowGraphMapper mapper = cfg.createClone();
                ma.accept(mapper.targetCfg);
            }
        }

        if (m_verbose)
        {
            logln("");
            logln("MIDDLE");
            logln("");

            dumpClassNode(cn);
        }

        byte[] emittedByteCode = ca.encode();

        ClassAnalyzer caRoundTrip = new ClassAnalyzer(null, new TypeResolver.ClassLocator(emittedByteCode));
        ClassNode     cnRoundTrip = ca.getClassNode();

        if (m_verbose)
        {
            logln("");
            logln("AFTER:");
            logln("");

            dumpClassNode(cnRoundTrip);
        }

        for (MethodAnalyzer ma : caRoundTrip.methods)
        {
            if (ma.hasCode())
            {
                ControlFlowGraph cfg = ma.parseControlFlowGraph(null);

                cfg.refreshFrameState();
            }
        }

        byte[] emittedByteCode2 = reencode(emittedByteCode);

        assertEquals("Bytecode different in length", originalByteCode2.length, emittedByteCode2.length);
        boolean success = true;
        for (int i = 0; i < originalByteCode2.length; i++)
        {
            if (originalByteCode2[i] != emittedByteCode2[i])
            {
                success = false;

                if (!verboseDifferentDump)
                {
                    break;
                }

                logln(" DIFFERENCE AT %04x: %02x != %02x", i, originalByteCode2[i] & 0xFF, emittedByteCode2[i] & 0xFF);
            }
        }
        logln("");

        if (!success)
        {
            List<String> original = dumpToList(originalByteCode2);
            List<String> emitted  = dumpToList(originalByteCode2);

            if (verboseDifferentDump)
            {
                success = original.size() == emitted.size();
            }
            else
            {
                assertEquals(original.size(), emitted.size());
                success = true;
            }

            for (int i = 0; i < original.size() && i < emitted.size(); i++)
            {
                String originalLine = original.get(i);
                String emittedLine  = emitted.get(i);

                if (!originalLine.equals(emittedLine))
                {
                    logln(" DIFFERENCE AT line %d:");
                    logln(" ORIGINAL : %s", originalLine);
                    logln(" EMITTED  : %s", emittedLine);
                    success = false;
                }
            }
        }

        if (!success)
        {
            if (verboseDifferentDump)
            {
                logln("Original: ");
                dumpContext(originalByteCode2);
                logln("Emitted: ");
                dumpContext(emittedByteCode2);
            }

            if (dumpByteCodeOnFailure)
            {
                try (FileOutputStream output = new FileOutputStream("good-dump.class"))
                {
                    output.write(originalByteCode2);
                }

                try (FileOutputStream output = new FileOutputStream("bad-dump.class"))
                {
                    output.write(emittedByteCode2);
                }
            }
        }
        assertTrue(success);
    }

    private static byte[] reencode(byte[] byteCode) throws
                                                    IOException,
                                                    AnalyzerException
    {
        ClassAnalyzer ca = new ClassAnalyzer(null, new TypeResolver.ClassLocator(byteCode));
        return reencode(ca);
    }

    private static byte[] reencode(ClassAnalyzer ca) throws
                                                     IOException,
                                                     AnalyzerException
    {
        //
        // The JAVA compiler emits the variable in source code order, not in definition order.
        // It can cause small differences in the re-emitted byte code, because we emit variables in definition order.
        // Let's just sort the variables in a predictable manner.
        //
        for (MethodAnalyzer ma : ca.methods)
        {
            MethodNode mn = ma.getMethodNode();
            if (mn.localVariables == null)
            {
                continue;
            }

            mn.localVariables.sort((l1, l2) ->
                                   {
                                       int l1Start = mn.instructions.indexOf(l1.start);
                                       int l2Start = mn.instructions.indexOf(l2.start);

                                       if (l1Start == l2Start)
                                       {
                                           l1Start = mn.localVariables.indexOf(l1);
                                           l2Start = mn.localVariables.indexOf(l2);
                                       }
                                       return l1Start - l2Start;
                                   });
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        ca.getClassNode()
          .accept(cw);
        return cw.toByteArray();
    }

    private static List<String> dumpToList(byte[] byteCode) throws
                                                            IOException,
                                                            AnalyzerException
    {
        ClassAnalyzer ca = new ClassAnalyzer(null, new TypeResolver.ClassLocator(byteCode));
        return dumpToList(ca.getClassNode());
    }

    private static List<String> dumpToList(ClassNode cn)
    {
        List<String> list = Lists.newArrayList();

        Printer p = new ASMifier();
        cn.accept(new TraceClassVisitor(null, p, null));

        expandList(p.text, (t) -> list.add(t));

        return list;
    }

    private void dumpContext(byte[] buffer)
    {
        BufferUtils.convertToHex(buffer, 0, buffer.length, 32, true, this::log);
    }

    private static byte[] readClass(final Class<?> clz) throws
                                                        IOException
    {
        try (InputStream stream = ClassLoader.getSystemResourceAsStream(clz.getName()
                                                                           .replace('.', '/') + ".class"))
        {
            byte[]                tempBuf      = new byte[1024];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(stream.available());

            while (true)
            {
                int n = stream.read(tempBuf);
                if (n == -1)
                {
                    break;
                }

                outputStream.write(tempBuf, 0, n);
            }

            return outputStream.toByteArray();
        }
    }

    private void dumpClassNode(ClassNode cn)
    {
        if (dumpWithTextifier)
        {
            CodeAnalysisLogger.printClass(cn, new TextifierWithOrderedLabels(cn), System.out);
        }

        if (dumpWithASMifier)
        {
            CodeAnalysisLogger.printClass(cn, new ASMifier(), System.out);
        }
    }

    private void dumpMethodNode(MethodNode mn)
    {

        if (dumpWithTextifier)
        {
            CodeAnalysisLogger.printMethod(mn, new TextifierWithOrderedLabels(mn), System.out);
        }

        if (dumpWithASMifier)
        {
            CodeAnalysisLogger.printMethod(mn, new ASMifier(), System.out);
        }
    }

    private void dumpTryCatchBlock(TryCatchBlockNode block,
                                   TraceMethodVisitor visitor)
    {
        block.accept(visitor);
        logMultiLine(flushPrinter(visitor));
    }

    private void dumpInstruction(TraceMethodVisitor visitor,
                                 AbstractInsnNode insn,
                                 int lineNumber)
    {
        if (lineNumber > 0)
        {
            log("[Line: %-4d]", lineNumber);
        }
        else
        {
            log("            ", lineNumber);
        }

        insn.accept(visitor);
        logMultiLine(flushPrinter(visitor));
    }

    //--//

    private String flushPrinter(TraceMethodVisitor visitor)
    {
        StringBuilder sb = new StringBuilder();

        expandList(visitor.p.text, (t) -> sb.append(t));
        visitor.p.text.clear();
        return sb.toString();
    }

    private static void expandList(final List<?> l,
                                   Consumer<String> callback)
    {
        for (Object o : l)
        {
            if (o instanceof List)
            {
                expandList((List<?>) o, callback);
            }
            else
            {
                callback.accept(o.toString());
            }
        }
    }

    //--//

    private ColumnarLogAdapter m_outputFormatter = new ColumnarLogAdapter(new LogVisitor()
    {
        @Override
        public void accept(String line)
        {
            if (m_verbose)
            {
                System.out.println(line);
            }
        }
    });

    public int getCurrentLogColumn()
    {
        return m_outputFormatter.getCurrentLogColumn();
    }

    public void logToColumn(int column)
    {
        m_outputFormatter.logToColumn(column);
    }

    public void indentTo(int minimum,
                         int incrementQuantum)
    {
        m_outputFormatter.indentTo(minimum, incrementQuantum);
    }

    public void logMultiLine(String txt)
    {
        m_outputFormatter.logMultiLine(txt);
    }

    public void log(String fmt,
                    Object... args)
    {
        m_outputFormatter.log(fmt, args);
    }

    public void logln(String fmt,
                      Object... args)
    {
        m_outputFormatter.logln(fmt, args);
    }

    public void logln()
    {
        m_outputFormatter.logln();
    }
}
