/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.bootstrap;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.asyncawait.converter.AsyncTransformer;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import com.optio3.concurrency.Executors;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public class AsyncClassFileTransformer implements ClassFileTransformer
{
    public static String TRACE_PACKAGE = null;

    public static CodeAnalysisLogger.Level VerbosityLevel          = CodeAnalysisLogger.Level.OFF;
    public static CodeAnalysisLogger.Level VerbosityLevelOnFailure = CodeAnalysisLogger.Level.DEBUG;

    //
    // We need a place that is shared among all class loaders.
    // Since 'System' is initialized early in the execution,
    // we use a property to communicate across class loaders. 
    //
    private static String OPTIO3_ASYNC_TRANSFORMER_RUNNING = "OPTIO3_ASYNC_TRANSFORMER_RUNNING";

    private static final Set<String> s_classesToIgnore = Sets.newConcurrentHashSet();

    private static final AsyncTransformer s_transformer;

    private static class TransformedByteCode
    {
        private static final Method s_findLoadedClassMethod;
        private static final Method s_defineClassMethod;

        static
        {
            try
            {
                s_findLoadedClassMethod = ClassLoader.class.getDeclaredMethod("findLoadedClass", String.class);
                s_findLoadedClassMethod.setAccessible(true);

                s_defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
                s_defineClassMethod.setAccessible(true);
            }
            catch (NoSuchMethodException | SecurityException e)
            {
                throw new RuntimeException("Unable to access 'ClassLoader.defineClass' method", e);
            }
        }

        //--//

        final String className;
        final byte[] byteCode;

        Class<?> loadedClass;

        TransformedByteCode(String className,
                            byte[] byteCode)
        {
            this.className = className;
            this.byteCode = byteCode;
        }

        public void ensureDefined(ClassLoader loader)
        {
            if (byteCode != null && loadedClass == null)
            {
                try
                {
                    loadedClass = (Class<?>) s_defineClassMethod.invoke(loader, className, byteCode, 0, byteCode.length);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e)
                {
                    throw new RuntimeException(String.format("Unable to load code for synthetic class '%s'", className), e);
                }
            }
        }
    }

    private static class ClassLoaderTracker
    {
        private static final List<ClassLoaderTracker> s_loaders = Lists.newArrayList();

        private final SoftReference<ClassLoader> m_loader;

        private final Map<String, TransformedByteCode> m_results = Maps.newHashMap();

        private ClassLoaderTracker(ClassLoader classLoader)
        {
            m_loader = new SoftReference<>(classLoader);
        }

        static ClassLoaderTracker getTracker(ClassLoader loader)
        {
            synchronized (s_loaders)
            {
                for (ClassLoaderTracker tracker : s_loaders)
                {
                    if (tracker.m_loader.get() == loader)
                    {
                        return tracker;
                    }
                }

                ClassLoaderTracker newTracker = new ClassLoaderTracker(loader);
                s_loaders.add(newTracker);
                return newTracker;
            }
        }

        synchronized TransformedByteCode get(String internalName)
        {
            return m_results.get(internalName);
        }

        synchronized TransformedByteCode put(String internalName,
                                             String className,
                                             byte[] byteCode)
        {
            TransformedByteCode res = get(internalName);
            if (res == null)
            {
                res = new TransformedByteCode(className, byteCode);
                m_results.put(internalName, res);
            }

            return res;
        }
    }

    static
    {
        //
        // Make sure we load all the important classes before starting the agent.
        //
        s_transformer = new AsyncTransformer(null, AsyncClassFileTransformer.class, null);
        s_transformer.shouldTransform();
    }

    public AsyncClassFileTransformer()
    {
    }

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer)
    {
        if (className == null)
        {
            return null;
        }

        String internalName = AsyncTransformer.getInternalName(className);

        if (s_classesToIgnore.contains(internalName))
        {
            return null;
        }

        ClassLoaderTracker tracker = ClassLoaderTracker.getTracker(loader);

        TransformedByteCode previousResult = tracker.get(internalName);
        if (previousResult != null)
        {
            return previousResult.byteCode;
        }

        Map<String, ClassAnalyzer> outputs;

        try
        {
            CodeAnalysisLogger logger = CodeAnalysisLogger.createCallbackLogger(VerbosityLevel, (s) -> System.err.println(s));
            outputs = transfomInner(logger, loader, internalName);
            if (outputs == null)
            {
                return null;
            }
        }
        catch (Exception e)
        {
            if (VerbosityLevel == VerbosityLevelOnFailure)
            {
                return null;
            }

            System.err.println(String.format("Encountered exception '%s' while trying to transform %s...", e, internalName));
            e.printStackTrace(System.err);

            //
            // Retry with debug logging enabled.
            //
            CodeAnalysisLogger logger = CodeAnalysisLogger.createCallbackLogger(VerbosityLevelOnFailure, (s) -> System.err.println(s));
            try
            {
                outputs = transfomInner(logger, loader, internalName);
                if (outputs == null)
                {
                    return null;
                }
            }
            catch (AnalyzerException | IOException e1)
            {
                System.err.println(String.format("Encountered exception '%s' while trying to transform %s...", e, internalName));
                e.printStackTrace(System.err);

                return null;
            }
        }

        try
        {
            //
            // Save results in the pending cache.
            //
            for (String name : outputs.keySet())
            {
                ClassAnalyzer ca = outputs.get(name);

                tracker.put(name, name.replace('/', '.'), ca.encode());
            }

            //
            // If the transform generated new classes, forcibly load them (they don't exist in the system, the loader won't find them).
            //
            byte[] result = null;

            for (String name : outputs.keySet())
            {
                TransformedByteCode res = tracker.get(name);

                //
                // Don't load the target of this transform call.
                // Just return it from the call.
                //
                if (name.equals(internalName))
                {
                    if (res.loadedClass == null)
                    {
                        res.loadedClass = classBeingRedefined;
                    }

                    result = res.byteCode;
                    continue;
                }

                res.ensureDefined(loader);
            }

            return result;
        }
        catch (Exception ex)
        {
            System.err.println(String.format("Encountered exception '%s' while trying to transform %s...", ex, internalName));
            ex.printStackTrace(System.err);

            return null;
        }
    }

    private Map<String, ClassAnalyzer> transfomInner(CodeAnalysisLogger logger,
                                                     ClassLoader loader,
                                                     String internalName) throws
                                                                          AnalyzerException,
                                                                          IOException
    {
        if (TRACE_PACKAGE != null)
        {
            if (internalName.startsWith(TRACE_PACKAGE))
            {
                System.out.printf("transfomInner: %s%n", internalName);
            }
        }

        AsyncTransformer worker = new AsyncTransformer(loader, internalName, logger);
        if (!worker.shouldTransform())
        {
            return null;
        }

        return worker.transform();
    }

    //--//

    public static void ignore(Class<?> clz)
    {
        s_classesToIgnore.add(AsyncTransformer.getInternalName(clz.getName()));
    }

    public void markRunning()
    {
        System.setProperty(OPTIO3_ASYNC_TRANSFORMER_RUNNING, "true");
    }

    public static boolean isRunning()
    {
        return "true".equals(System.getProperty(OPTIO3_ASYNC_TRANSFORMER_RUNNING));
    }

    public static void ensureLoaded()
    {
        if (!isRunning())
        {
            try
            {
                Agent.loadAtRuntime();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error attaching java agent for async/await", e);
            }

            int countDown = 10000;

            while (countDown > 0)
            {
                if (isRunning())
                {
                    break;
                }

                Executors.safeSleep(1);

                countDown--;
            }

            if (countDown == 0)
            {
                throw new RuntimeException("Failed to attach java agent for async/await");
            }
        }
    }
}
