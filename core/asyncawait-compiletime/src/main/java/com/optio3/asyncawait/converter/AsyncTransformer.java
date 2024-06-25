/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.converter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.AbstractAsyncComputation;
import com.optio3.asyncawait.AbstractContinuationState;
import com.optio3.asyncawait.AsyncBackground;
import com.optio3.asyncawait.AsyncDelay;
import com.optio3.asyncawait.AsyncExecutor;
import com.optio3.asyncawait.AsyncOrigin;
import com.optio3.asyncawait.AsyncPreventBlocking;
import com.optio3.asyncawait.CompileTime;
import com.optio3.asyncawait.ContinuationState;
import com.optio3.codeanalysis.ClassAccess;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.ConstantPoolAnalyzer;
import com.optio3.codeanalysis.ConstantPoolAnalyzer.Entry;
import com.optio3.codeanalysis.FieldAccess;
import com.optio3.codeanalysis.FieldAnalyzer;
import com.optio3.codeanalysis.GenericMethodInfo;
import com.optio3.codeanalysis.GenericType;
import com.optio3.codeanalysis.GenericType.MethodDescriptor;
import com.optio3.codeanalysis.GenericTypeInfo;
import com.optio3.codeanalysis.MethodAccess;
import com.optio3.codeanalysis.MethodAnalyzer;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.cfg.AbstractInstruction;
import com.optio3.codeanalysis.cfg.BasicBlock;
import com.optio3.codeanalysis.cfg.ControlFlowGraph;
import com.optio3.codeanalysis.cfg.ControlFlowGraphMapper;
import com.optio3.codeanalysis.cfg.FrameState;
import com.optio3.codeanalysis.cfg.FrameValue;
import com.optio3.codeanalysis.cfg.LocalVariable;
import com.optio3.codeanalysis.cfg.SourceCodeInformation;
import com.optio3.codeanalysis.cfg.TryCatchHandler;
import com.optio3.codeanalysis.cfg.instruction.FieldGetInstruction;
import com.optio3.codeanalysis.cfg.instruction.FieldPutInstruction;
import com.optio3.codeanalysis.cfg.instruction.InvokeInstruction;
import com.optio3.codeanalysis.cfg.instruction.LoadConstantInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableLoadInstruction;
import com.optio3.codeanalysis.cfg.instruction.LocalVariableStoreInstruction;
import com.optio3.codeanalysis.cfg.instruction.MonitorInstruction;
import com.optio3.codeanalysis.cfg.instruction.NewObjectInstruction;
import com.optio3.codeanalysis.cfg.instruction.ReturnInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackDupInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackPopInstruction;
import com.optio3.codeanalysis.cfg.instruction.StackSwapInstruction;
import com.optio3.codeanalysis.cfg.instruction.SwitchLookupInstruction;
import com.optio3.codeanalysis.cfg.instruction.ThrowInstruction;
import com.optio3.codeanalysis.cfg.instruction.TypeCheckInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnaryConditionalJumpInstruction;
import com.optio3.codeanalysis.cfg.instruction.UnconditionalJumpInstruction;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.util.TraceClassVisitor;

public class AsyncTransformer
{
    public static final String LoggingContext = "AsyncTransformer";

    //--//

    public static final Method CompletableFuture_isDone;
    public static final Method CompletableFuture_isCancelled;
    public static final Method CompletableFuture_get;
    public static final Method CompletableFuture_complete;
    public static final Method CompletableFuture_completeExceptionally;
    public static final Method CompletableFuture_completedFuture;

    public static final Method CompileTime_bootstrap;
    public static final Method CompileTime_wasComputationCancelled;
    public static final Method CompileTime_await;
    public static final Method CompileTime_awaitTimeout;
    public static final Method CompileTime_awaitNoUnwrap;
    public static final Method CompileTime_awaitNoUnwrapTimeout;
    public static final Method CompileTime_sleep;
    public static final Method CompileTime_wrapAsync;

    @SuppressWarnings("rawtypes")
    public static final Constructor<AbstractAsyncComputation> AsyncComputation_init;
    public static final Method                                AsyncComputation_start;
    public static final Method                                AsyncComputation_startDelayed;
    public static final Method                                AsyncComputation_sleep;
    public static final Method                                AsyncComputation_advanceInner;
    public static final Method                                AsyncComputation_forwardFuture;
    public static final Method                                AsyncComputation_invalidState;

    public static final Constructor<ContinuationState.Foreground> ContinuationState_init_foreground;
    public static final Constructor<ContinuationState.Background> ContinuationState_init_background;
    public static final Method                                    ContinuationState_queue;
    public static final Method                                    ContinuationState_queueTimeout;
    public static final Method                                    ContinuationState_getFuture;
    public static final Method                                    ContinuationState_getAndUnwrapException;

    public static final Method AsyncOrigin_type;
    public static final Method AsyncOrigin_method;
    public static final Method AsyncOrigin_signature;

    //--//

    private static final SearchTypeReference   s_instrumentTypeSearch;
    private static final SearchMethodReference s_instrumentMethodSearch;

    private static final Type s_typeForAsyncBackground      = Type.getType(AsyncBackground.class);
    private static final Type s_typeForAsyncDelay           = Type.getType(AsyncDelay.class);
    private static final Type s_typeForAsyncExecutor        = Type.getType(AsyncExecutor.class);
    private static final Type s_typeForAsyncPreventBlocking = Type.getType(AsyncPreventBlocking.class);
    private static final Type s_typeForTimeUnit             = Type.getType(TimeUnit.class);

    private static final Type s_typeForThreadPoolExecutor = Type.getType(ThreadPoolExecutor.class);
    private static final Type s_typeForScheduledExecutor  = Type.getType(ScheduledExecutorService.class);

    //--//

    private final ClassLoader                m_loader;
    private final CodeAnalysisLogger         m_logger;
    private final TypeResolver               m_typeResolver;
    private final String                     m_targetClassName;
    private final Map<String, ClassAnalyzer> m_output = Maps.newHashMap();

    private GenericTypeInfo m_typeForCompletableFuture;
    private GenericTypeInfo m_typeForAsyncComputation;
    private GenericTypeInfo m_typeForAbstractContinuationState;
    private GenericTypeInfo m_typeForContinuationStateForeground;
    private GenericTypeInfo m_typeForContinuationStateBackground;

    private GenericMethodInfo m_methodForBootstrap;
    private GenericMethodInfo m_methodForWasComputationCancelled;
    private GenericMethodInfo m_methodForAwaitNoUnwrap;
    private GenericMethodInfo m_methodForAwaitNoUnwrapTimeout;
    private GenericMethodInfo m_methodForAwait;
    private GenericMethodInfo m_methodForAwaitTimeout;
    private GenericMethodInfo m_methodForSleep;
    private GenericMethodInfo m_methodForWrapAsync;
    private GenericMethodInfo m_methodForCompletedFuture;

    private ClassAnalyzer m_classToTransform;

    //--//

    //
    // We have to manually construct the class, because lambdas don't mix well with Transformation Agent operations (deadlocks on shared data structures...)  
    //

    static class SearchTypeReference implements Function<ConstantPoolAnalyzer.Entry, ConstantPoolAnalyzer.Entry>
    {
        private final String m_searchTarget;

        SearchTypeReference(String target)
        {
            m_searchTarget = target;
        }

        @Override
        public Entry apply(Entry v)
        {
            if (m_searchTarget.equals(v.className))
            {
                return v;
            }

            if (v.tag == ConstantPoolAnalyzer.CONSTANT_Utf8)
            {
                //
                // Type names could hide in annotations, which are tracked as method_info.
                // We don't want to parse method_infos, so we also look for any string.
                //
                if (m_searchTarget.equals(v.value))
                {
                    return v;
                }
            }

            return null;
        }
    }

    static class SearchMethodReference extends SearchTypeReference
    {
        private final Set<String> m_names;

        SearchMethodReference(String target)
        {
            super(target);

            m_names = Sets.newHashSet();

            for (Method m : CompileTime.class.getDeclaredMethods())
            {
                KnownMethod anno = m.getAnnotation(KnownMethod.class);
                if (anno != null)
                {
                    m_names.add(m.getName());
                }
            }
        }

        @Override
        public Entry apply(Entry v)
        {
            v = super.apply(v);
            if (v != null)
            {
                if (m_names.contains(v.memberName))
                {
                    return v;
                }
            }

            return null;
        }
    }

    static
    {
        Type backgroundType = Type.getType(AsyncBackground.class);
        Type compileType    = Type.getType(CompileTime.class);

        s_instrumentTypeSearch = new SearchTypeReference(backgroundType.getDescriptor());
        s_instrumentMethodSearch = new SearchMethodReference(compileType.getInternalName());

        try
        {
            // @formatter:off
            CompletableFuture_isDone                = getMethod(CompletableFuture.class, "isDone"                                );
            CompletableFuture_isCancelled           = getMethod(CompletableFuture.class, "isCancelled"                           );
            CompletableFuture_get                   = getMethod(CompletableFuture.class, "get"                                   );
            CompletableFuture_complete              = getMethod(CompletableFuture.class, "complete"             , Object.class   );
            CompletableFuture_completeExceptionally = getMethod(CompletableFuture.class, "completeExceptionally", Throwable.class);
            CompletableFuture_completedFuture       = getMethod(CompletableFuture.class, "completedFuture"      , Object.class   );

            CompileTime_bootstrap               = getMethod(CompileTime.class, KnownMethodId.CompileTime_bootstrap                                                                   );
            CompileTime_wasComputationCancelled = getMethod(CompileTime.class, KnownMethodId.CompileTime_wasComputationCancelled                                                     );
            CompileTime_await                   = getMethod(CompileTime.class, KnownMethodId.CompileTime_await                  , CompletableFuture.class                            );
            CompileTime_awaitTimeout            = getMethod(CompileTime.class, KnownMethodId.CompileTime_awaitTimeout           , CompletableFuture.class, long.class, TimeUnit.class);
            CompileTime_awaitNoUnwrap           = getMethod(CompileTime.class, KnownMethodId.CompileTime_awaitNoUnwrap          , CompletableFuture.class                            );
            CompileTime_awaitNoUnwrapTimeout    = getMethod(CompileTime.class, KnownMethodId.CompileTime_awaitNoUnwrapTimeout   , CompletableFuture.class, long.class, TimeUnit.class);
            CompileTime_sleep                   = getMethod(CompileTime.class, KnownMethodId.CompileTime_sleep                                           , long.class, TimeUnit.class);
            CompileTime_wrapAsync               = getMethod(CompileTime.class, KnownMethodId.CompileTime_wrapAsync              , Object.class                                       );

            AsyncComputation_init          = getConstructor(AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_init         , ThreadPoolExecutor.class, ScheduledExecutorService.class                                 );
            AsyncComputation_start         = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_start                                                                                                   );
            AsyncComputation_startDelayed  = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_startDelayed , long.class              , TimeUnit.class                                                 );
            AsyncComputation_sleep         = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_sleep        , long.class              , TimeUnit.class                 , AbstractAsyncComputation.class);
            AsyncComputation_advanceInner  = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_advanceInner , int.class               , AbstractContinuationState.class                                );
            AsyncComputation_forwardFuture = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_forwardFuture, CompletableFuture.class                                                                  );
            AsyncComputation_invalidState  = getMethod     (AbstractAsyncComputation.class, KnownMethodId.AsyncComputation_invalidState , int.class                                                                                );

            ContinuationState_init_foreground       = getConstructor(ContinuationState.Foreground.class, KnownMethodId.ContinuationState_init_foreground      , AbstractAsyncComputation.class, int.class     , CompletableFuture.class);
            ContinuationState_init_background       = getConstructor(ContinuationState.Background.class, KnownMethodId.ContinuationState_init_background      , AbstractAsyncComputation.class, int.class     , CompletableFuture.class);
            ContinuationState_queue                 = getMethod     (ContinuationState.class           , KnownMethodId.ContinuationState_queue                                                                                         );
            ContinuationState_queueTimeout          = getMethod     (ContinuationState.class           , KnownMethodId.ContinuationState_queueTimeout         , long.class                    , TimeUnit.class                         );
            ContinuationState_getFuture             = getMethod     (ContinuationState.class           , KnownMethodId.ContinuationState_getFuture                                                                                     );
            ContinuationState_getAndUnwrapException = getMethod     (ContinuationState.class           , KnownMethodId.ContinuationState_getAndUnwrapException, CompletableFuture.class                                                );

            AsyncOrigin_type      = getMethod(AsyncOrigin.class, KnownMethodId.AsyncOrigin_type     );
            AsyncOrigin_method    = getMethod(AsyncOrigin.class, KnownMethodId.AsyncOrigin_method   );
            AsyncOrigin_signature = getMethod(AsyncOrigin.class, KnownMethodId.AsyncOrigin_signature);
            // @formatter:on
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    private static String findMethodName(Class<?> clz,
                                         KnownMethodId id)
    {
        for (Method m : clz.getDeclaredMethods())
        {
            KnownMethod anno = m.getAnnotation(KnownMethod.class);

            if (anno != null && anno.value() == id)
            {
                return m.getName();
            }
        }

        throw Exceptions.newRuntimeException("No method is annotated with KnownMethod %s on %s", id, clz.getSimpleName());
    }

    private static Method getMethod(Class<?> clz,
                                    KnownMethodId id,
                                    Class<?>... parameterTypes)
    {
        return getMethod(clz, findMethodName(clz, id), parameterTypes);
    }

    private static Method getMethod(Class<?> clz,
                                    String name,
                                    Class<?>... parameterTypes)
    {
        try
        {
            return clz.getDeclaredMethod(name, parameterTypes);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Can't find method '");
            sb.append(name);
            sb.append("' with parameters (");
            String prefix = "";
            for (Class<?> param : parameterTypes)
            {
                sb.append(prefix);
                prefix = ", ";
                sb.append(param.getSimpleName());
            }
            sb.append(")");
            throw new RuntimeException(sb.toString(), e);
        }
    }

    private static <T> Constructor<T> getConstructor(Class<T> clz,
                                                     KnownMethodId id,
                                                     Class<?>... parameterTypes)
    {
        Constructor<T> c = getConstructor(clz, parameterTypes);

        KnownMethod anno = c.getAnnotation(KnownMethod.class);

        if (anno != null && anno.value() == id)
        {
            return c;
        }

        throw Exceptions.newRuntimeException("No constructor is annotated with KnownMethod %s on %s", id, clz.getSimpleName());
    }

    private static <T> Constructor<T> getConstructor(Class<T> clz,
                                                     Class<?>... parameterTypes)
    {
        try
        {
            return clz.getDeclaredConstructor(parameterTypes);
        }
        catch (NoSuchMethodException | SecurityException e)
        {
            StringBuilder sb = new StringBuilder();
            sb.append("Can't find constructor with parameters (");
            String prefix = "";
            for (Class<?> param : parameterTypes)
            {
                sb.append(prefix);
                prefix = ", ";
                sb.append(param.getSimpleName());
            }
            sb.append(")");
            throw new RuntimeException(sb.toString(), e);
        }
    }

    public AsyncTransformer(ClassLoader loader,
                            Class<?> clz,
                            CodeAnalysisLogger logger)
    {
        this(loader, getInternalName(clz), logger);
    }

    public AsyncTransformer(ClassLoader loader,
                            String internalName,
                            CodeAnalysisLogger logger)
    {
        if (loader == null)
        {
            loader = ClassLoader.getSystemClassLoader();
        }

        if (logger == null)
        {
            logger = CodeAnalysisLogger.createCallbackLogger(CodeAnalysisLogger.Level.WARN, System.out::println);
        }

        m_loader = loader;
        m_logger = logger;

        TypeResolver.Loader itf = (internalClassName) -> m_loader.getResource(internalClassName + ".class");

        m_typeResolver = new TypeResolver(m_logger, itf);
        m_targetClassName = internalName;
    }

    public static String getInternalName(Class<?> clz)
    {
        return getInternalName(clz.getName());
    }

    public static String getInternalName(String className)
    {
        return className.replace('.', '/');
    }

    public boolean shouldTransform()
    {
        m_logger.trace("shouldTransform: %s", m_targetClassName);

        //
        // Skip System classes.
        //
        {
            if (m_targetClassName.startsWith("java/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("javax/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("sun/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/sun/"))
            {
                return false;
            }
        }

        //
        // Skip well-known dependencies.
        //
        {
            if (m_targetClassName.startsWith("org/apache/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/google/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/microsoft/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/azure/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/amazonaws/"))
            {
                return false;
            }
        }

        //
        // Don't try and instrument the instrumenter...
        //
        {
            if (m_targetClassName.startsWith("org/objectweb/asm/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("org/apache/commons/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/optio3/asyncawait/converter/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/optio3/asyncawait/bootstrap/"))
            {
                return false;
            }

            if (m_targetClassName.startsWith("com/optio3/codeanalysis/"))
            {
                return false;
            }
        }

        try
        {
            TypeResolver.ClassLocator cl = m_typeResolver.getClassReader(m_targetClassName);
            if (cl != null)
            {
                //
                // To reduce the overhead of the agent, just do a quick pass on the constant pool, looking for references to key methods.
                //
                ConstantPoolAnalyzer.Entry found;

                found = ConstantPoolAnalyzer.searchClassNames(cl.classReader, s_instrumentTypeSearch);
                if (found != null)
                {
                    return true;
                }

                found = ConstantPoolAnalyzer.searchMethodReferences(cl.classReader, s_instrumentMethodSearch);
                if (found != null)
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            System.err.printf("Encountered an error trying to parse class '%s': %s%n", m_targetClassName, e);
            e.printStackTrace(System.err);
        }

        return false;
    }

    //--//

    public Map<String, ClassAnalyzer> transform() throws
                                                  AnalyzerException,
                                                  IOException
    {
        final TypeResolver.ClassLocator cl = m_typeResolver.getClassReader(m_targetClassName);
        m_classToTransform = new ClassAnalyzer(m_typeResolver, cl);

        if (m_logger.isEnabled(LoggingContext, CodeAnalysisLogger.Level.DEBUG))
        {
            TraceClassVisitor visitor = CodeAnalysisLogger.printClass(m_classToTransform.getClassNode(), null, null);
            m_logger.log(LoggingContext, CodeAnalysisLogger.Level.DEBUG, visitor);

            m_logger.debug(LoggingContext, "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
        }

        //--//

        m_typeForCompletableFuture = m_typeResolver.getGenericTypeInfo(CompletableFuture.class);
        m_typeForAsyncComputation = m_typeResolver.getGenericTypeInfo(AbstractAsyncComputation.class);
        m_typeForAbstractContinuationState = m_typeResolver.getGenericTypeInfo(AbstractContinuationState.class);
        m_typeForContinuationStateForeground = m_typeResolver.getGenericTypeInfo(ContinuationState.Foreground.class);
        m_typeForContinuationStateBackground = m_typeResolver.getGenericTypeInfo(ContinuationState.Background.class);

        m_methodForBootstrap = m_typeResolver.getGenericMethodInfo(CompileTime_bootstrap);
        m_methodForWasComputationCancelled = m_typeResolver.getGenericMethodInfo(CompileTime_wasComputationCancelled);

        m_methodForAwait = m_typeResolver.getGenericMethodInfo(CompileTime_await);
        m_methodForAwaitTimeout = m_typeResolver.getGenericMethodInfo(CompileTime_awaitTimeout);
        m_methodForSleep = m_typeResolver.getGenericMethodInfo(CompileTime_sleep);
        m_methodForWrapAsync = m_typeResolver.getGenericMethodInfo(CompileTime_wrapAsync);
        m_methodForCompletedFuture = m_typeResolver.getGenericMethodInfo(CompletableFuture_completedFuture);

        m_methodForAwaitNoUnwrap = m_typeResolver.getGenericMethodInfo(CompileTime_awaitNoUnwrap);
        m_methodForAwaitNoUnwrapTimeout = m_typeResolver.getGenericMethodInfo(CompileTime_awaitNoUnwrapTimeout);

        //--//

        m_output.put(m_classToTransform.getInternalName(), m_classToTransform);

        List<MethodAnalyzer> methods = Lists.newArrayList(m_classToTransform.methods);
        for (MethodAnalyzer ma : methods)
        {
            if (ma.hasCode())
            {
                MethodState ms = new MethodState(ma);

                ms.process();
            }
        }

        if (!m_classToTransform.wasModified())
        {
            m_output.remove(m_classToTransform.getInternalName());
        }

        if (m_logger.isEnabled(LoggingContext, CodeAnalysisLogger.Level.DEBUG))
        {
            for (String classOutput : m_output.keySet())
            {
                m_logger.debug(LoggingContext, ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
                m_logger.debug(LoggingContext, "OUTPUT for %s", classOutput);
                m_logger.debug(LoggingContext, "");

                ClassAnalyzer ca2 = m_output.get(classOutput);
                ClassReader   cr2 = new ClassReader(ca2.encode());
                ClassNode     cn2 = new ClassNode(Opcodes.ASM7);
                cr2.accept(cn2, 0);

                TraceClassVisitor visitor = CodeAnalysisLogger.printClass(cn2, null, null);
                m_logger.log(LoggingContext, CodeAnalysisLogger.Level.DEBUG, visitor);
            }
        }

        return m_output;
    }

    //--//

    static class InstructionToContinuation<T extends AbstractInstruction>
    {
        T                           insn;
        ContinuationStateDescriptor cs;

        InstructionToContinuation(T insn)
        {
            this.insn = insn;
            this.cs = new ContinuationStateDescriptor();
        }
    }

    class MethodState
    {
        class LogicState
        {
            private final ControlFlowGraphMapper m_mapper;
            private final ControlFlowGraph       m_cfg;

            private final LocalVariable m_stateMachineVar;
            private final LocalVariable m_stateIdVar;
            private final LocalVariable m_baseContinuationStateVar;
            private final LocalVariable m_continuationStateVar;
            private final LocalVariable m_futureVar;

            private final GenericMethodInfo m_future_isDone;
            private final GenericMethodInfo m_future_isCancelled;
            private final GenericMethodInfo m_future_get;
            private final GenericMethodInfo m_ac_sleep;
            private final GenericMethodInfo m_cs_queue;
            private final GenericMethodInfo m_cs_queueTimeout;
            private final GenericMethodInfo m_cs_getFuture;
            private final GenericMethodInfo m_cs_getAndUnwrapException;

            private LocalVariable m_timeoutVar;
            private LocalVariable m_unitVar;

            LogicState() throws
                         AnalyzerException
            {
                m_mapper = m_originalMethodCfg.createClone();

                ControlFlowGraph cfg = m_mapper.targetCfg;
                m_cfg = cfg;

                // Make room for the parameters of the logic method: StateMachine, int, RestorePoint.
                cfg.remapLocalVariablesInRange(0, 3);
                cfg.setContext(m_methodForStateMachineLogic.genericMethodInfo);

                LocalVariable[] args = cfg.getArgumentVars();
                m_stateMachineVar = args[0];
                m_stateIdVar = args[1];
                m_baseContinuationStateVar = args[2];

                m_continuationStateVar = cfg.newLocalVariable(m_classForContinuationState);
                m_futureVar = m_cfg.newLocalVariable(m_typeForCompletableFuture);

                //--//

                m_future_isDone = m_typeResolver.getGenericMethodInfo(CompletableFuture_isDone);
                m_future_isCancelled = m_typeResolver.getGenericMethodInfo(CompletableFuture_isCancelled);
                m_future_get = m_typeResolver.getGenericMethodInfo(CompletableFuture_get);

                m_ac_sleep = m_typeResolver.getGenericMethodInfo(AsyncComputation_sleep);

                m_cs_queue = m_typeResolver.getGenericMethodInfo(ContinuationState_queue);
                m_cs_queueTimeout = m_typeResolver.getGenericMethodInfo(ContinuationState_queueTimeout);
                m_cs_getFuture = m_typeResolver.getGenericMethodInfo(ContinuationState_getFuture);
                m_cs_getAndUnwrapException = m_typeResolver.getGenericMethodInfo(ContinuationState_getAndUnwrapException);
            }

            void forwardAllCancellationChecks() throws
                                                AnalyzerException
            {
                for (InvokeInstruction insn : m_computationCancelledChecks)
                {
                    InvokeInstruction insnCloned = m_mapper.get(InvokeInstruction.class, insn);
                    m_cfg.setDefaultSourceCode(insnCloned.sourceCode);

                    insnCloned.addBefore(new LocalVariableLoadInstruction(m_stateMachineVar));
                    insnCloned.substitute(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_future_isCancelled, false));
                }
            }

            void forwardAllReturns() throws
                                     AnalyzerException
            {
                GenericMethodInfo forwardFuture = m_typeResolver.getGenericMethodInfo(AsyncComputation_forwardFuture);

                for (InstructionToContinuation<ReturnInstruction> pair : m_returns)
                {
                    ReturnInstruction ret = pair.insn;

                    ReturnInstruction retCloned = m_mapper.get(ReturnInstruction.class, ret);
                    m_cfg.setDefaultSourceCode(retCloned.sourceCode);

                    retCloned.addBefore(new LocalVariableLoadInstruction(m_stateMachineVar));
                    retCloned.addBefore(new StackSwapInstruction());
                    retCloned.addBefore(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, forwardFuture, false));
                    retCloned.substitute(new ReturnInstruction());
                }
            }

            void forwardAllSleeps() throws
                                    AnalyzerException
            {
                for (InvokeInstruction insn : m_sleeps)
                {
                    InvokeInstruction insnCloned = m_mapper.get(InvokeInstruction.class, insn);
                    m_cfg.setDefaultSourceCode(insnCloned.sourceCode);

                    insnCloned.addBefore(new LocalVariableLoadInstruction(m_stateMachineVar));
                    insnCloned.substitute(new InvokeInstruction(Opcodes.INVOKESTATIC, m_ac_sleep, false));
                }
            }

            void generateSaveAndRestorePoints() throws
                                                AnalyzerException
            {
                int stateId = 1;

                for (InstructionToContinuation<InvokeInstruction> pair : m_awaits)
                {
                    InvokeInstruction           call = pair.insn;
                    ContinuationStateDescriptor cs   = pair.cs;

                    cs.stateId = stateId++;

                    InvokeInstruction callCloned = m_mapper.get(InvokeInstruction.class, call);
                    m_cfg.setDefaultSourceCode(callCloned.sourceCode);

                    //
                    // As starting point, we have a basic block with a call to CompileTime.await():
                    //
                    //   BBx
                    //   {
                    //      <code before await>
                    //      InvokeStatic CompileTime.await(....);
                    //      <code after await>
                    //   }
                    //
                    // We want to have this structure after some transformations:
                    //
                    //   BB1 = beforeCall
                    //   {
                    //      <code before await>
                    //      <save timeout, if present>
                    //      <check to see if the future is done>
                    //      <if done, goto restartPoint>
                    //      GOTO savePoint
                    //   }
                    // 
                    //   BB2 = savePoint
                    //   {
                    //      <create new RestorePoint>
                    //      <spill stack>
                    //      <spill locals>
                    //      <enqueue continuation>
                    //   }
                    // 
                    //   BB3 = restorePoint
                    //   {
                    //      <restore locals>
                    //      <restore stack>
                    //      GOTO restartPoint
                    //   }
                    //
                    //   BB4 = restartPoint
                    //   {
                    //      InvokeVirtual CompletableFuture.get();
                    //      <code after await>
                    //   }
                    //
                    // Step 1 : split at callCloned, to create BB1 and BB2.
                    // Step 2 : add BB2 after BB1. 
                    // Step 3 : add BB3 after BB2. 
                    //
                    BasicBlock beforeCall = callCloned.getBasicBlock();

                    cs.restartPoint = callCloned.splitBefore();

                    // Inherit exception handlers, since we might throw trying to enqueue the continuation.
                    cs.savePoint = beforeCall.insertNewBasicBlockAfter(true);

                    cs.restorePoint = cs.savePoint.insertNewBasicBlockAfter(false);

                    //
                    // With the new structure in place, we can move things around.
                    //

                    if (cs.hasTimeout)
                    {
                        beforeCall.addInstruction(new LocalVariableStoreInstruction(ensureVariableForUnit()));
                        beforeCall.addInstruction(new LocalVariableStoreInstruction(ensureVariableForTimeout()));
                    }

                    //
                    // Now we have the Future at the top of the stack.
                    //

                    {
                        //
                        // Start populating the restore path with code to reload the locals.
                        //
                        fetchLocalsFromStateMachine(cs, cs.restorePoint);

                        //
                        // Create a new basic block, we might need to jump in the middle of the restore path when a constructor is involved.
                        //
                        cs.restartPointFromRP = cs.restorePoint.insertNewBasicBlockAfter(false);
                        cs.restorePoint.setControlFlow(new UnconditionalJumpInstruction(cs.restartPointFromRP));

                        //
                        // Populate the restore path with code to reload the stack.
                        //
                        fetchStacksFromRestorePoint(cs, cs.restartPointFromRP);

                        //
                        // Jump back to the original code.
                        //
                        {
                            BasicBlock bb = cs.restartPointFromRP;

                            bb.addInstruction(new LocalVariableLoadInstruction(m_continuationStateVar));
                            bb.addInstruction(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_cs_getFuture, false));

                            bb.setControlFlow(new UnconditionalJumpInstruction(cs.restartPoint));
                        }
                    }

                    if (cs.hasAnyUninitializedValuesOnStack())
                    {
                        //
                        // When there are uninitialized values on the stack, we can't simply jump to the exit code and resume from the restart code,
                        // the JVM will throw a VerifyError (it doesn't support merging frames with different generators for uninit values).
                        //
                        // So we have to spill all the stack to the RestorePoint, check for isDone, then resume from the RestorePoint.
                        //

                        beforeCall.addInstruction(new LocalVariableStoreInstruction(m_futureVar));

                        spillStackToRestorePoint(cs, beforeCall);

                        beforeCall.addInstruction(new LocalVariableLoadInstruction(m_futureVar));
                        beforeCall.addInstruction(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_future_isDone, false));

                        beforeCall.setControlFlow(new UnaryConditionalJumpInstruction(Opcodes.IFNE, Type.INT_TYPE, cs.savePoint, cs.restartPointFromRP));

                        spillLocalsToStateMachine(cs, cs.savePoint);

                        enqueueContinuation(cs, cs.savePoint);
                    }
                    else
                    {

                        //
                        // Let's DUP and call the CompletableFuture.isDone() method.
                        //
                        beforeCall.addInstruction(new StackDupInstruction());
                        beforeCall.addInstruction(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_future_isDone, false));

                        beforeCall.setControlFlow(new UnaryConditionalJumpInstruction(Opcodes.IFNE, Type.INT_TYPE, cs.savePoint, cs.restartPoint));

                        //
                        // Populate the save path:
                        //
                        //  * save the future on the stack,
                        //  * spill the stack,
                        //  * spill the locals,
                        //  * enqueue continuation.
                        //

                        cs.savePoint.addInstruction(new LocalVariableStoreInstruction(m_futureVar));

                        spillStackToRestorePoint(cs, cs.savePoint);

                        spillLocalsToStateMachine(cs, cs.savePoint);

                        enqueueContinuation(cs, cs.savePoint);
                    }

                    //
                    // Change the call from Instrument.await to CompletableFuture.get() or ContinuationState.getAndUnwrapException(future);
                    //
                    if (cs.dontUnwrapException)
                    {
                        callCloned.substitute(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_future_get, false));
                    }
                    else
                    {
                        callCloned.substitute(new InvokeInstruction(Opcodes.INVOKESTATIC, m_cs_getAndUnwrapException, false));
                    }
                }
            }

            void addSwitchTable() throws
                                  AnalyzerException
            {
                BasicBlock entry = m_cfg.getBasicBlockAt(0);
                m_cfg.setDefaultSourceCode(searchFirstSourceCode(entry));

                for (AbstractInstruction insn : entry.getInstructions())
                {
                    if (insn.sourceCode != null)
                    {
                        m_cfg.setDefaultSourceCode(insn.sourceCode);
                        break;
                    }
                }

                BasicBlock bb       = entry.insertNewBasicBlockBefore(false);
                BasicBlock bbError  = bb.insertNewBasicBlockAfter(false);
                BasicBlock bbState0 = bbError.insertNewBasicBlockAfter(false);

                //
                // Cast the argument to our subclass of RestorePoint.
                //
                bb.addInstructions(new LocalVariableLoadInstruction(m_baseContinuationStateVar),
                                   new TypeCheckInstruction(Opcodes.CHECKCAST, m_classForContinuationState.genericTypeInfo.asType()),
                                   new LocalVariableStoreInstruction(m_continuationStateVar));

                //
                // Emit state dispatcher.
                //
                {
                    SwitchLookupInstruction lookup = new SwitchLookupInstruction(bbError);

                    for (InstructionToContinuation<InvokeInstruction> pair : m_awaits)
                    {
                        InvokeInstruction           call = pair.insn;
                        ContinuationStateDescriptor cs   = pair.cs;

                        lookup.switchLookup.put(cs.stateId, cs.restorePoint);
                    }

                    lookup.switchLookup.put(0, bbState0);

                    bb.addInstructions(new LocalVariableLoadInstruction(m_stateIdVar), lookup);
                }

                //
                // Emit the error basic block.
                //
                {
                    GenericMethodInfo invalidState = m_typeResolver.getGenericMethodInfo(AsyncComputation_invalidState);

                    bbError.addInstructions(new LocalVariableLoadInstruction(m_stateMachineVar),
                                            new LocalVariableLoadInstruction(m_stateIdVar),
                                            new InvokeInstruction(Opcodes.INVOKEVIRTUAL, invalidState, false),
                                            new ThrowInstruction());
                }

                //
                // Emit the restore point for the initial state.
                //
                {
                    fetchLocalsFromStateMachine(m_entryPointState, bbState0);

                    bbState0.setControlFlow(new UnconditionalJumpInstruction(entry));
                }
            }

            void accept() throws
                          AnalyzerException
            {
                dump(CodeAnalysisLogger.Level.DEBUG, "AfterTransformation", m_cfg);
                m_cfg.refreshFrameState();
                dump(CodeAnalysisLogger.Level.TRACE, "AfterTransformationWithFrames", m_cfg);
                m_methodForStateMachineLogic.accept(m_cfg);
            }

            //--//

            private void spillLocalsToStateMachine(ContinuationStateDescriptor cs,
                                                   BasicBlock bb) throws
                                                                  AnalyzerException
            {
                //
                // Spill all the locals to the fields of the StateMachine object.
                //
                for (LocalVariable localVar : cs.localsSorted)
                {
                    LocalVariable                              clonedVar = m_mapper.get(localVar);
                    ContinuationStateDescriptor.AnnotatedField af        = cs.locals.get(localVar);

                    //
                    // No field for the local means it's a known NULL value. No need to spill it.
                    //
                    if (af.storageField != null)
                    {
                        bb.addInstruction(new LocalVariableLoadInstruction(m_stateMachineVar));
                        bb.addInstruction(new LocalVariableLoadInstruction(clonedVar));
                        bb.addInstruction(new FieldPutInstruction(af.storageField));
                    }
                }
            }

            private void fetchLocalsFromStateMachine(ContinuationStateDescriptor cs,
                                                     BasicBlock bb) throws
                                                                    AnalyzerException
            {
                //
                // Fetch all the locals from the fields of the StateMachine object.
                //

                for (LocalVariable localVar : cs.localsSorted)
                {
                    LocalVariable                              clonedVar = m_mapper.get(localVar);
                    ContinuationStateDescriptor.AnnotatedField af        = cs.locals.get(localVar);

                    if (af.storageField == null)
                    {
                        //
                        // No field for the local means it's a known NULL value.
                        //
                        bb.addInstruction(new LoadConstantInstruction(null));
                    }
                    else
                    {
                        bb.addInstruction(new LocalVariableLoadInstruction(m_stateMachineVar));
                        bb.addInstruction(new FieldGetInstruction(af.storageField));
                    }

                    bb.addInstruction(new LocalVariableStoreInstruction(clonedVar));
                }
            }

            //--//

            private void spillStackToRestorePoint(ContinuationStateDescriptor cs,
                                                  BasicBlock bb) throws
                                                                 AnalyzerException
            {
                //
                // Allocate new RestorePoint object.
                //
                bb.addInstructions(new NewObjectInstruction(m_classForContinuationState.genericTypeInfo.asType()),
                                   new StackDupInstruction(),
                                   new LocalVariableLoadInstruction(m_stateMachineVar),
                                   new LoadConstantInstruction(cs.stateId),
                                   new LocalVariableLoadInstruction(m_futureVar),
                                   new InvokeInstruction(Opcodes.INVOKESPECIAL, m_methodForContinuationState_init, false),
                                   new LocalVariableStoreInstruction(m_continuationStateVar));

                //
                // Spill all the stack to the fields of the RestorePoint object.
                //
                Set<AbstractInstruction> toRemove = null;

                for (int i = cs.stack.size(); i-- > 0; )
                {
                    ContinuationStateDescriptor.AnnotatedField stack = cs.stack.get(i);

                    if (stack.frameValue.isUninitialized())
                    {
                        //
                        // Peephole optimization to remove the dead allocations on the save path,
                        // instead of popping the values from the stack. 
                        //
                        boolean canRemoveNewInstruction = false;

                        if (stack.frameValue.hasSingleGenerator())
                        {
                            AbstractInstruction insn = stack.frameValue.getSingleGenerator();
                            if (insn instanceof NewObjectInstruction && insn.getNext() instanceof StackDupInstruction)
                            {
                                canRemoveNewInstruction = true;

                                if (toRemove == null)
                                {
                                    toRemove = Sets.newHashSet();
                                }

                                toRemove.add(m_mapper.get(insn));
                                toRemove.add(m_mapper.get(insn.getNext()));
                            }
                        }

                        if (!canRemoveNewInstruction)
                        {
                            bb.addInstruction(new StackPopInstruction());
                        }
                    }
                    else if (stack.storageField == null)
                    {
                        //
                        // No field for the stack means it's a known NULL value. No need to spill it.
                        //
                        bb.addInstruction(new StackPopInstruction());
                    }
                    else if (stack.frameValue.getSize() == 2)
                    {
                        // We can't use a SWAP instruction, because the two values are of different sizes. So we have to use DUP_X2 and POP.
                        bb.addInstructions(new LocalVariableLoadInstruction(m_continuationStateVar),
                                           new StackDupInstruction(1, 2),
                                           new StackPopInstruction(),
                                           new FieldPutInstruction(stack.storageField));
                    }
                    else
                    {
                        bb.addInstructions(new LocalVariableLoadInstruction(m_continuationStateVar), new StackSwapInstruction(), new FieldPutInstruction(stack.storageField));
                    }
                }

                if (toRemove != null)
                {
                    for (AbstractInstruction insn : toRemove)
                        insn.remove();
                }
            }

            private void fetchStacksFromRestorePoint(ContinuationStateDescriptor cs,
                                                     BasicBlock bb) throws
                                                                    AnalyzerException
            {
                //
                // Fetch all the stack to the fields on the RestorePoint object.
                //
                NewObjectInstruction lastOrigin = null;

                for (int i = 0; i < cs.stack.size(); i++)
                {
                    ContinuationStateDescriptor.AnnotatedField stack = cs.stack.get(i);

                    if (stack.frameValue.isUninitialized())
                    {
                        NewObjectInstruction origin = (NewObjectInstruction) stack.frameValue.getSingleGenerator();

                        if (lastOrigin == origin)
                        {
                            bb.addInstruction(new StackDupInstruction());
                            lastOrigin = null;
                        }
                        else
                        {
                            bb.addInstruction(new NewObjectInstruction(origin.type));
                            lastOrigin = origin;
                        }
                    }
                    else if (stack.storageField == null)
                    {
                        //
                        // No field for the stack means it's a known NULL value.
                        //
                        bb.addInstruction(new LoadConstantInstruction(null));
                    }
                    else
                    {
                        bb.addInstruction(new LocalVariableLoadInstruction(m_continuationStateVar));
                        bb.addInstruction(new FieldGetInstruction(stack.storageField));
                    }
                }
            }

            //--//

            private void enqueueContinuation(ContinuationStateDescriptor cs,
                                             BasicBlock bb) throws
                                                            AnalyzerException
            {
                bb.addInstruction(new LocalVariableLoadInstruction(m_continuationStateVar));

                if (cs.hasTimeout)
                {
                    bb.addInstructions(new LocalVariableLoadInstruction(ensureVariableForTimeout()),
                                       new LocalVariableLoadInstruction(ensureVariableForUnit()),
                                       new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_cs_queueTimeout, false));
                }
                else
                {
                    bb.addInstruction(new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m_cs_queue, false));
                }

                bb.setControlFlow(new ReturnInstruction());
            }

            //--//

            private LocalVariable ensureVariableForTimeout()
            {
                if (m_timeoutVar == null)
                {
                    //
                    // public static <T> T await(CompletableFuture<T> t, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
                    //
                    // 'timeout' is the second parameter.
                    //
                    Type[] paramTypes = m_methodForAwaitNoUnwrapTimeout.getSignature()
                                                                       .getRawParameterTypes();
                    m_timeoutVar = m_cfg.newLocalVariable(paramTypes[1]);
                }

                return m_timeoutVar;
            }

            private LocalVariable ensureVariableForUnit()
            {
                if (m_unitVar == null)
                {
                    //
                    // public static <T> T await(CompletableFuture<T> t, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
                    //
                    // 'unit' is the second parameter.
                    //
                    Type[] paramTypes = m_methodForAwaitNoUnwrapTimeout.getSignature()
                                                                       .getRawParameterTypes();
                    m_unitVar = m_cfg.newLocalVariable(paramTypes[2]);
                }

                return m_unitVar;
            }
        }

        private final MethodAnalyzer   m_originalMethod;
        private final ControlFlowGraph m_originalMethodCfg;

        private ContinuationStateDescriptor m_entryPointState;

        private final List<InstructionToContinuation<ReturnInstruction>> m_returns                    = Lists.newArrayList();
        private final List<InstructionToContinuation<InvokeInstruction>> m_awaits                     = Lists.newArrayList();
        private final List<InvokeInstruction>                            m_sleeps                     = Lists.newArrayList();
        private final Set<InvokeInstruction>                             m_computationCancelledChecks = Sets.newHashSet();
        private       GenericType                                        m_outputType;

        private final Map<LocalVariable, FieldAnalyzer> m_localsToVars = Maps.newHashMap();

        private MethodAnalyzer m_methodForStateMachineLogic;

        private ClassAnalyzer  m_classForAsyncComputation;
        private MethodAnalyzer m_methodForAsyncComputation_init;
        private MethodAnalyzer m_methodForAsyncComputation_advanceInner;

        private ClassAnalyzer  m_classForContinuationState;
        private MethodAnalyzer m_methodForContinuationState_init;

        private boolean m_executeInBackground;
        private Integer m_argumentForThreadPoolExecutor;
        private Integer m_argumentForScheduledExecutor;
        private Integer m_argumentForDelay;
        private Integer m_argumentForDelayUnits;
        private boolean m_mustProcess;

        MethodState(MethodAnalyzer ma) throws
                                       AnalyzerException
        {
            m_originalMethod = ma;
            m_originalMethodCfg = m_originalMethod.parseControlFlowGraph(m_logger);
        }

        void process() throws
                       AnalyzerException,
                       IOException
        {
            m_logger.info(LoggingContext, "Processing %s...", m_originalMethod);

            collectSplitPoints();

            processAnnotations();

            if (m_mustProcess)
            {
                m_originalMethodCfg.refreshFrameState();

                analyzeReturnType();

                createClasses();

                generateStubForAdvanceInner();

                //
                // Move original code to StateMachine class.
                //
                LogicState logicState = new LogicState();
                logicState.forwardAllCancellationChecks();
                logicState.forwardAllReturns();
                logicState.forwardAllSleeps();
                logicState.generateSaveAndRestorePoints();
                logicState.addSwitchTable();
                logicState.accept();

                insertNewCodeInOriginalMethod();
            }
        }

        private void processAnnotations() throws
                                          AnalyzerException
        {
            processMethodAnnotations();
            processParameterAnnotations();
        }

        private void processMethodAnnotations()
        {
            List<AnnotationNode> anList = m_originalMethod.getMethodNode().visibleAnnotations;
            if (anList == null)
            {
                return;
            }

            for (AnnotationNode an : anList)
            {
                GenericType.TypeDeclaration gti = getAnnotationType(an);

                if (gti.equals(s_typeForAsyncBackground))
                {
                    m_executeInBackground = true;
                    m_mustProcess = true;
                }
            }
        }

        private GenericType.TypeDeclaration getAnnotationType(AnnotationNode an)
        {
            return m_typeResolver.parseGenericTypeDeclaration(an.desc, m_originalMethod.getSignature());
        }

        private void processParameterAnnotations() throws
                                                   AnalyzerException
        {
            List<AnnotationNode>[] args = m_originalMethod.getMethodNode().visibleParameterAnnotations;
            if (args == null)
            {
                return;
            }

            Type[] argTypes = m_originalMethod.getSignature()
                                              .getRawParameterTypes();
            for (int argNum = 0; argNum < args.length; argNum++)
            {
                List<AnnotationNode> anList = args[argNum];
                if (anList == null)
                {
                    continue;
                }

                Type argType = argTypes[argNum];

                for (AnnotationNode an : anList)
                {
                    GenericType.TypeDeclaration gti = getAnnotationType(an);

                    if (gti.equals(s_typeForAsyncDelay))
                    {
                        if (Type.LONG_TYPE.equals(argType))
                        {
                            m_argumentForDelay = argNum;
                            continue;
                        }

                        if (m_typeResolver.canCastTo(s_typeForTimeUnit, argType))
                        {
                            m_argumentForDelayUnits = argNum;
                            continue;
                        }

                        throw TypeResolver.reportProblem("Annotation %s applied to parameter of incompatible type: %s", s_typeForAsyncDelay.getClassName(), argType);
                    }

                    if (gti.equals(s_typeForAsyncExecutor))
                    {
                        if (m_typeResolver.canCastTo(s_typeForThreadPoolExecutor, argType))
                        {
                            m_argumentForThreadPoolExecutor = argNum;
                            continue;
                        }

                        if (m_typeResolver.canCastTo(s_typeForScheduledExecutor, argType))
                        {
                            m_argumentForScheduledExecutor = argNum;
                            continue;
                        }

                        throw TypeResolver.reportProblem("Annotation %s applied to parameter of incompatible type: %s", s_typeForAsyncExecutor.getClassName(), argType);
                    }
                }
            }
        }

        private void analyzeReturnType() throws
                                         AnalyzerException
        {
            GenericType returnType = m_originalMethod.getSignature().returnType;

            if (m_originalMethod.getSignature()
                                .returnsVoid())
            {
                throw TypeResolver.reportProblem("Async method '%s' does not return any value", m_originalMethod);
            }

            if (!m_typeResolver.canCastTo(m_typeForCompletableFuture, returnType))
            {
                throw TypeResolver.reportProblem("Async method '%s' does not return a CompletableFuture<T>: %s", m_originalMethod, returnType);
            }

            GenericType.TypeReference returnType2 = (GenericType.TypeReference) returnType;

            //
            // Sometimes the Java compiler generates lambdas with raw signatures, instead of adding the generic declarations.
            // In that case, we don't have a Bound argument. Just default back to Object, which is what we have at runtime anyways...
            // 
            m_outputType = returnType2.getBoundArgumentOrDefault(0);
        }

        private void collectSplitPoints() throws
                                          AnalyzerException,
                                          IOException
        {
            m_mustProcess = false;

            List<InvokeInstruction> toDelete  = Lists.newArrayList();
            List<InvokeInstruction> toReplace = Lists.newArrayList();

            m_originalMethodCfg.visitInstructions(InvokeInstruction.class, (insn) ->
            {
                if (insn.isCallTo(m_methodForBootstrap))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.bootstrap at %s -> %s", insn, insn.getBasicBlock());

                    toDelete.add(insn);
                }

                if (insn.isCallTo(m_methodForWrapAsync))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.wrapAsync at %s -> %s", insn, insn.getBasicBlock());

                    toReplace.add(insn);
                }

                if (insn.isCallTo(m_methodForWasComputationCancelled))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.wasComputationCancelled at %s -> %s", insn, insn.getBasicBlock());

                    m_computationCancelledChecks.add(insn);
                    m_mustProcess = true;
                }

                //--//

                if (insn.isCallTo(m_methodForAwait))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.await at %s -> %s", insn, insn.getBasicBlock());

                    InstructionToContinuation<InvokeInstruction> pair = new InstructionToContinuation<>(insn);
                    m_awaits.add(pair);
                    m_mustProcess = true;
                }

                if (insn.isCallTo(m_methodForAwaitTimeout))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.await with timeout at %s -> %s", insn, insn.getBasicBlock());

                    InstructionToContinuation<InvokeInstruction> pair = new InstructionToContinuation<>(insn);
                    pair.cs.hasTimeout = true;
                    m_awaits.add(pair);
                    m_mustProcess = true;
                }

                //--//

                if (insn.isCallTo(m_methodForAwaitNoUnwrap))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.awaitNoUnwrapException at %s -> %s", insn, insn.getBasicBlock());

                    InstructionToContinuation<InvokeInstruction> pair = new InstructionToContinuation<>(insn);
                    pair.cs.dontUnwrapException = true;
                    m_awaits.add(pair);
                    m_mustProcess = true;
                }

                if (insn.isCallTo(m_methodForAwaitNoUnwrapTimeout))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.awaitNoUnwrapException with timeout at %s -> %s", insn, insn.getBasicBlock());

                    InstructionToContinuation<InvokeInstruction> pair = new InstructionToContinuation<>(insn);
                    pair.cs.dontUnwrapException = true;
                    pair.cs.hasTimeout = true;
                    m_awaits.add(pair);
                    m_mustProcess = true;
                }

                //--//

                if (insn.isCallTo(m_methodForSleep))
                {
                    m_logger.trace(LoggingContext, "Found CompileTime.sleep at %s -> %s", insn, insn.getBasicBlock());

                    m_sleeps.add(insn);
                    m_mustProcess = true;
                }

                return true;
            });

            m_originalMethodCfg.visitInstructions(ReturnInstruction.class, (insn) ->
            {
                m_logger.trace(LoggingContext, "Found return at %s -> %s", insn, insn.getBasicBlock());

                InstructionToContinuation<ReturnInstruction> pair = new InstructionToContinuation<>(insn);
                m_returns.add(pair);

                return true;
            });

            boolean modified = false;

            for (InvokeInstruction insn : toDelete)
            {
                insn.remove();
                modified = true;
            }

            for (InvokeInstruction insn : toReplace)
            {
                InvokeInstruction insnNew = new InvokeInstruction(insn.opcode, m_methodForCompletedFuture, false);
                insn.substitute(insnNew);

                modified = true;
            }

            if (modified && !m_mustProcess)
            {
                //
                // There are no async call in this method, but it still refers to the CompileTime methods.
                // Make sure to flush the modified CFG back to the method, so it will be persisted.
                //
                m_originalMethod.accept(m_originalMethodCfg);
            }
        }

        private void createClasses() throws
                                     AnalyzerException
        {
            dump(CodeAnalysisLogger.Level.DEBUG, "BeforeTransformation", m_originalMethodCfg);

            m_classForAsyncComputation = newNestedType("sm__" + m_originalMethod.getName(), m_typeForAsyncComputation, m_outputType);

            GenericTypeInfo superType = m_executeInBackground ? m_typeForContinuationStateBackground : m_typeForContinuationStateForeground;
            m_classForContinuationState = newNestedType("rp__" + m_originalMethod.getName(), superType);

            AnnotationVisitor annoVisitor = m_classForAsyncComputation.getClassNode()
                                                                      .visitAnnotation(Type.getType(AsyncOrigin.class)
                                                                                           .getDescriptor(), true);
            annoVisitor.visit(AsyncOrigin_type.getName(), m_originalMethod.declaringClass.genericTypeInfo.asType());
            annoVisitor.visit(AsyncOrigin_method.getName(), m_originalMethod.getName());
            annoVisitor.visit(AsyncOrigin_signature.getName(),
                              m_originalMethod.getSignature()
                                              .asInternalName());

            //
            // Map source-level variables to fields.
            //
            {
                mapLocalVariable(m_originalMethodCfg.getThisVar(), "local$", "this");

                LocalVariable[] args = m_originalMethodCfg.getArgumentVars();
                for (int i = 0; i < args.length; i++)
                {
                    LocalVariable arg  = args[i];
                    String        name = m_originalMethodCfg.getVariableNameIfAvailable(arg);
                    if (name == null)
                    {
                        name = "arg" + i;
                    }

                    mapLocalVariable(arg, "local$", name);
                }
            }

            //
            // Map locals to fields for the entrypoint.
            //
            {
                AbstractInstruction entryPoint = m_originalMethodCfg.getEntrypoint()
                                                                    .getFirstInstruction();
                FrameState fs = entryPoint.getFrameState();

                m_entryPointState = new ContinuationStateDescriptor();

                for (Integer index : fs.getLocals())
                {
                    LocalVariable localVar = m_originalMethodCfg.ensureLocalVariableAtIndex(index,
                                                                                            fs.getLocal(index)
                                                                                              .getType());

                    FieldAnalyzer fa = m_localsToVars.get(localVar);
                    if (fa == null)
                    {
                        throw TypeResolver.reportProblem("Unexpected missing variable for '%s'", localVar);
                    }

                    m_entryPointState.addLocal(localVar, fa, null);
                }
                m_entryPointState.sortLocals();
            }

            //
            // Record which stack values map to return values.
            //
            for (InstructionToContinuation<ReturnInstruction> pair : m_returns)
            {
                ReturnInstruction           ret = pair.insn;
                ContinuationStateDescriptor cs  = pair.cs;

                cs.frame = new FrameState(ret.getFrameState());

                cs.future = cs.frame.pop();
            }

            //
            // At each continuation point, map local variables to fields of the state machine and stack entries to fields of the restore point.
            //
            for (InstructionToContinuation<InvokeInstruction> pair : m_awaits)
            {
                InvokeInstruction           call = pair.insn;
                ContinuationStateDescriptor cs   = pair.cs;

                cs.frame = new FrameState(call.getFrameState());

                for (TryCatchHandler tch : call.getBasicBlock()
                                               .getExceptionHandlers()
                                               .values())
                {
                    for (BasicBlock bb : tch.handler.getLinearChainOfBlocks())
                    {
                        for (AbstractInstruction insn : bb.getInstructions())
                        {
                            InvokeInstruction insnInvoke = Reflection.as(insn, InvokeInstruction.class);
                            if (insnInvoke != null && insnInvoke.name.equals("close"))
                            {
                                if (m_typeResolver.canCastTo(s_typeForAsyncPreventBlocking, insnInvoke.owner))
                                {
                                    throw TypeResolver.reportProblem("Detected a blocking call in method '%s' holding a non-blocking closable resource in basic block '%s'",
                                                                     m_originalMethod,
                                                                     call.getBasicBlock());
                                }
                            }

                            MonitorInstruction insnMonitor = Reflection.as(insn, MonitorInstruction.class);
                            if (insnMonitor != null && insnMonitor.opcode == Opcodes.MONITOREXIT)
                            {
                                throw TypeResolver.reportProblem("Detected a blocking call in method '%s' holding a lock in basic block '%s'", m_originalMethod, call.getBasicBlock());
                            }
                        }
                    }
                }

                if (cs.hasTimeout)
                {
                    cs.timeUnits = cs.frame.pop();
                    cs.timeout = cs.frame.pop();
                }

                cs.future = cs.frame.pop();

                for (Integer index : cs.frame.getLocals())
                {
                    FrameValue    v        = cs.frame.getLocal(index);
                    LocalVariable localVar = m_originalMethodCfg.ensureLocalVariableAtIndex(index, v.getType());

                    // Is the variable still alive?
                    Set<AbstractInstruction> uses = call.findVariableUses(localVar);
                    if (uses.isEmpty())
                    {
                        m_logger.debug(LoggingContext, "Not saving %s because it's dead", localVar);
                        continue;
                    }

                    if (v.isNull())
                    {
                        //
                        // When we see a NULL value on the frame, we need to handle it specially.
                        // Don't create a field for it, we'll reload a NULL on restart.
                        //
                        cs.addLocal(localVar, null, v);
                    }
                    else
                    {
                        FieldAnalyzer fa = mapLocalVariable(localVar, "local_noname$", null);
                        cs.addLocal(localVar, fa, v);
                    }
                }
                cs.sortLocals();

                Map<Type, Integer> numbering = Maps.newHashMap();

                for (FrameValue fv : cs.frame.getStackVars())
                {
                    if (fv.isUninitialized())
                    {
                        if (!fv.hasSingleGenerator())
                        {
                            throw TypeResolver.reportProblem("INTERNAL ERROR: unexpected stack slot with multiple possible generators for value at %s", call);
                        }

                        cs.addStack(null, fv);
                    }
                    else if (fv.isNull())
                    {
                        cs.addStack(null, fv);
                    }
                    else
                    {
                        Type type = fv.getType();

                        FieldAnalyzer fa = mapStackSlot(numbering, type);
                        cs.addStack(fa, fv);
                    }
                }
            }

            //
            // New Synthetic method in the original class, to host the state machine logic.
            //
            {
                //
                // public void <method>$logic(AsyncComputation, int, AbstractContinuationState)
                //
                String desc = Type.getMethodDescriptor(Type.VOID_TYPE, m_classForAsyncComputation.genericTypeInfo.asType(), Type.INT_TYPE, m_typeForAbstractContinuationState.asType());

                GenericType.MethodDescriptor md                       = m_typeResolver.parseGenericMethodSignature(desc, null);
                String                       nameForStateMachineLogic = findUniqueName(m_classToTransform, m_originalMethod.getName(), md);

                m_methodForStateMachineLogic = m_classToTransform.addMethod(nameForStateMachineLogic, md, MethodAccess.Public, MethodAccess.Static, MethodAccess.Synthetic);
            }

            {
                GenericMethodInfo m = m_typeResolver.getGenericConstructorInfo(AsyncComputation_init);
                m_methodForAsyncComputation_init = m_classForAsyncComputation.overrideMethod(m, MethodAccess.Public, MethodAccess.Synthetic);
                ControlFlowGraph cfgConstructor = m_methodForAsyncComputation_init.emitCodeToCallSuperConstructor(null);
                for (AbstractInstruction insn : cfgConstructor.getBasicBlockAt(0)
                                                              .getInstructions())
                {
                    insn.sourceCode = searchFirstSourceCode(m_originalMethodCfg.getBasicBlockAt(0));
                }
                dump(CodeAnalysisLogger.Level.DEBUG, "m_methodForStateMachine_init", cfgConstructor);
                m_methodForAsyncComputation_init.accept(cfgConstructor);

                m = m_typeResolver.getGenericMethodInfo(AsyncComputation_advanceInner);
                m_methodForAsyncComputation_advanceInner = m_classForAsyncComputation.overrideMethod(m, MethodAccess.Public, MethodAccess.Synthetic);
            }

            {
                GenericMethodInfo m = m_typeResolver.getGenericConstructorInfo(m_executeInBackground ? ContinuationState_init_background : ContinuationState_init_foreground);
                m_methodForContinuationState_init = m_classForContinuationState.overrideMethod(m, MethodAccess.Public, MethodAccess.Synthetic);

                ControlFlowGraph cfgConstructor = m_methodForContinuationState_init.emitCodeToCallSuperConstructor(null);
                for (AbstractInstruction insn : cfgConstructor.getBasicBlockAt(0)
                                                              .getInstructions())
                {
                    insn.sourceCode = searchFirstSourceCode(m_originalMethodCfg.getBasicBlockAt(0));
                }
                dump(CodeAnalysisLogger.Level.DEBUG, "m_methodForRestorePoint_init", cfgConstructor);
                m_methodForContinuationState_init.accept(cfgConstructor);
            }

            //--//

            for (FieldAnalyzer fa : m_classForAsyncComputation.fields)
                m_logger.trace(LoggingContext, "%s", fa);

            for (FieldAnalyzer fa : m_classForContinuationState.fields)
                m_logger.trace(LoggingContext, "%s", fa);
        }

        private void generateStubForAdvanceInner() throws
                                                   AnalyzerException
        {
            ControlFlowGraph cfg = m_methodForAsyncComputation_advanceInner.createControlFlowGraph(m_logger);
            cfg.setDefaultSourceCode(searchFirstSourceCode(m_originalMethodCfg.getBasicBlockAt(0)));

            BasicBlock bb = cfg.newBasicBlock();

            bb.addInstruction(new LocalVariableLoadInstruction(cfg.getThisVar()));

            for (LocalVariable arg : cfg.getArgumentVars())
                bb.addInstruction(new LocalVariableLoadInstruction(arg));

            bb.addInstructions(new InvokeInstruction(Opcodes.INVOKESTATIC, m_methodForStateMachineLogic, false), new ReturnInstruction(Opcodes.RETURN, Type.VOID_TYPE));

            dump(CodeAnalysisLogger.Level.DEBUG, "m_methodForStateMachine_advanceInner", cfg);
            m_methodForAsyncComputation_advanceInner.accept(cfg);
        }

        private void insertNewCodeInOriginalMethod() throws
                                                     AnalyzerException
        {
            ControlFlowGraph cfg = m_originalMethodCfg;
            cfg.setDefaultSourceCode(searchFirstSourceCode(cfg.getBasicBlockAt(0)));

            cfg.resetCode();

            Type          stateMachineType = m_classForAsyncComputation.genericTypeInfo.asType();
            LocalVariable stateMachine     = cfg.newLocalVariable(stateMachineType);

            BasicBlock bb = cfg.newBasicBlock();

            bb.addInstructions(new NewObjectInstruction(stateMachineType),
                               new StackDupInstruction(),
                               fetchArgumentOrDefault(cfg, m_argumentForThreadPoolExecutor, null),
                               fetchArgumentOrDefault(cfg, m_argumentForScheduledExecutor, null),
                               new InvokeInstruction(Opcodes.INVOKESPECIAL, m_methodForAsyncComputation_init, false),
                               new LocalVariableStoreInstruction(stateMachine));

            if (!m_originalMethod.isStatic())
            {
                storeLocalIntoField(bb, stateMachine, cfg.getThisVar(), null);
            }

            for (LocalVariable arg : cfg.getArgumentVars())
                storeLocalIntoField(bb, stateMachine, arg, null);

            if (m_executeInBackground)
            {
                GenericMethodInfo m = m_typeResolver.getGenericMethodInfo(AsyncComputation_startDelayed);

                bb.addInstructions(new LocalVariableLoadInstruction(stateMachine),
                                   fetchArgumentOrDefault(cfg, m_argumentForDelay, (long) 0),
                                   fetchArgumentOrDefault(cfg, m_argumentForDelayUnits, null),
                                   new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m, false),
                                   new LocalVariableLoadInstruction(stateMachine),
                                   new ReturnInstruction(Opcodes.ARETURN, stateMachineType));
            }
            else
            {
                GenericMethodInfo m = m_typeResolver.getGenericMethodInfo(AsyncComputation_start);

                bb.addInstructions(new LocalVariableLoadInstruction(stateMachine),
                                   new InvokeInstruction(Opcodes.INVOKEVIRTUAL, m, false),
                                   new LocalVariableLoadInstruction(stateMachine),
                                   new ReturnInstruction(Opcodes.ARETURN, stateMachineType));
            }

            dump(CodeAnalysisLogger.Level.DEBUG, "m_originalMethod", cfg);
            m_originalMethod.accept(cfg);
        }

        private AbstractInstruction fetchArgumentOrDefault(ControlFlowGraph cfg,
                                                           Integer index,
                                                           Object defaultValue) throws
                                                                                AnalyzerException
        {
            if (index != null)
            {
                return new LocalVariableLoadInstruction(cfg.getArgumentVars()[index]);
            }

            return new LoadConstantInstruction(defaultValue);
        }

        private void storeLocalIntoField(BasicBlock bb,
                                         LocalVariable stateMachine,
                                         LocalVariable var,
                                         ControlFlowGraphMapper mapper) throws
                                                                        AnalyzerException
        {
            FieldAnalyzer fa = m_localsToVars.get(var);

            if (mapper != null)
            {
                var = mapper.get(var);
            }

            bb.addInstructions(new LocalVariableLoadInstruction(stateMachine), new LocalVariableLoadInstruction(var), new FieldPutInstruction(fa));
        }

        private SourceCodeInformation searchFirstSourceCode(BasicBlock entry)
        {

            for (AbstractInstruction insn : entry.getInstructions())
            {
                if (insn.sourceCode != null)
                {
                    return insn.sourceCode;
                }
            }
            return null;
        }

        //--//

        private String findUniqueName(ClassAnalyzer ca,
                                      String name,
                                      MethodDescriptor md)
        {
            NameSequencer seq = new NameSequencer(name, "$logic");
            while (true)
            {
                String fullName = seq.nextValue();

                if (ca.findMethod(fullName, md) == null)
                {
                    return fullName;
                }
            }
        }

        private FieldAnalyzer mapLocalVariable(LocalVariable localVar,
                                               String namePrefix,
                                               String name) throws
                                                            AnalyzerException
        {
            if (localVar == null)
            {
                return null;
            }

            FieldAnalyzer fa = m_localsToVars.get(localVar);
            if (fa == null)
            {
                GenericType gt = m_originalMethodCfg.getVariableGenericTypeIfAvailable(localVar, true);

                NameSequencer seq = new NameSequencer(namePrefix, name);
                while (true)
                {
                    String fullName = seq.nextValue();

                    if (m_classForAsyncComputation.genericTypeInfo.findField(fullName, true) == null)
                    {
                        EnumSet<FieldAccess> access = EnumSet.of(FieldAccess.Synthetic);
                        fa = m_classForAsyncComputation.addField(access, fullName, gt);
                        m_localsToVars.put(localVar, fa);
                        break;
                    }
                }
            }

            return fa;
        }

        private FieldAnalyzer mapStackSlot(Map<Type, Integer> numbering,
                                           Type type) throws
                                                      AnalyzerException
        {
            Integer index = numbering.get(type);
            if (index == null)
            {
                index = 0;
            }

            numbering.put(type, index + 1);

            NameSequencer seq = new NameSequencer("stack$", String.valueOf(index));
            while (true)
            {
                String fullName = seq.nextValue();

                FieldAnalyzer fa = m_classForContinuationState.findField(fullName);
                if (fa != null)
                {
                    if (!fa.getType()
                           .equals(type))
                    {
                        // Same name, but different type, keep looking.
                        continue;
                    }

                    return fa;
                }

                EnumSet<FieldAccess> access = EnumSet.of(FieldAccess.Synthetic);
                GenericType          gt     = m_typeResolver.getGenericTypeReference(type);
                fa = m_classForContinuationState.addField(access, fullName, gt);
                return fa;
            }
        }
    }

    //--//

    private ClassAnalyzer newNestedType(String innerNamePrefix,
                                        GenericTypeInfo superType,
                                        GenericType... args) throws
                                                             AnalyzerException
    {
        NameSequencer seq = new NameSequencer(innerNamePrefix, null);
        while (true)
        {
            String innerName = seq.nextValue();
            String name      = m_classToTransform.getInternalName() + "$" + innerName;

            if (!m_output.containsKey(name))
            {
                EnumSet<ClassAccess>      access     = EnumSet.of(ClassAccess.Synthetic, ClassAccess.Super, ClassAccess.Final);
                GenericType.TypeReference superclass = superType.createReferenceForSubclass(args);
                ClassAnalyzer             ca         = m_classToTransform.addNestedType(access, innerName, superclass);
                m_output.put(name, ca);
                return ca;
            }
        }
    }

    private void dump(CodeAnalysisLogger.Level level,
                      String prefix,
                      ControlFlowGraph cfg)
    {
        if (m_logger.isEnabled(LoggingContext, level))
        {
            ControlFlowGraph.Dumper dumper = cfg.new Dumper();
            for (String line : dumper.execute())
                m_logger.log(LoggingContext, level, "%s %s", prefix, line);
        }
    }
}
