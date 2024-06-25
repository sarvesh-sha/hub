/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;

import com.optio3.asyncawait.CompileTime;
import com.optio3.asyncawait.converter.AsyncTransformer;
import com.optio3.util.FileSystem;

/**
 * Class called when a java agent is attached to the jvm in runtime.
 */
public class Agent
{
    public static String TRACE_PACKAGE = null;

    /*
     * From
     * https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/instrument
     * /Instrumentation.html
     *
     * Agent-Class
     *
     * If an implementation supports a mechanism to start agents sometime after
     * the VM has started then this attribute specifies the agent class. That
     * is, the class containing the agentmain method. This attribute is
     * required, if it is not present the agent will not be started. Note: this
     * is a class name, not a file name or path.
     */
    public static void agentmain(String agentArgs,
                                 Instrumentation inst)
    {
        AsyncClassFileTransformer transformer = new AsyncClassFileTransformer();
        inst.addTransformer(transformer, true);

        ClassLoaderInterceptor interceptor = new ClassLoaderInterceptor(inst, transformer);
        interceptor.execute();

        long runStart = System.nanoTime();

        int  checked   = 0;
        long checkTime = 0;

        int  transformed     = 0;
        long retransformTime = 0;

        for (Class<?> clazz : inst.getAllLoadedClasses())
        {
            if (TRACE_PACKAGE != null)
            {
                String name = clazz.getName();
                if (name.startsWith(TRACE_PACKAGE))
                {
                    System.out.printf("agentmain: %s%n", name);
                }
            }

            if (inst.isModifiableClass(clazz))
            {
                checked++;

                try
                {
                    long             checkStart = System.nanoTime();
                    AsyncTransformer checker    = new AsyncTransformer(null, clazz, null);
                    boolean          transform  = checker.shouldTransform();
                    long             checkEnd   = System.nanoTime();
                    checkTime += (checkEnd - checkStart);

                    if (transform)
                    {
                        transformed++;
                        long retransformStart = System.nanoTime();
                        inst.retransformClasses(clazz);
                        long retransformEnd = System.nanoTime();
                        retransformTime += (retransformEnd - retransformStart);
                    }
                }
                catch (Exception | Error e)
                {
                    System.err.printf("Failed to transform class %s due to exception %s%n", clazz.getName(), e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        long runEnd  = System.nanoTime();
        long runTime = (runEnd - runStart);

        System.err.printf("Applied AsyncTransformer to code in %dmsec: %d classes checked in %d, %d transformed in %d%n",
                          toMSec(runTime),
                          checked,
                          toMSec(checkTime),
                          transformed,
                          toMSec(retransformTime));
        transformer.markRunning();
    }

    private static long toMSec(long runTime)
    {
        return (runTime + 500 * 1000) / (1000 * 1000);
    }

    /*
     * From
     * https://docs.oracle.com/javase/8/docs/api/index.html?java/lang/instrument
     * /Instrumentation.html
     *
     * Premain-Class
     *
     * When an agent is specified at JVM launch time this attribute specifies
     * the agent class. That is, the class containing the premain method. When
     * an agent is specified at JVM launch time this attribute is required. If
     * the attribute is not present the JVM will abort. Note: this is a class
     * name, not a file name or path.
     */
    public static void premain(String agentArgs,
                               Instrumentation inst)
    {
        AsyncClassFileTransformer transformer = new AsyncClassFileTransformer();
        inst.addTransformer(transformer, true);

        ClassLoaderInterceptor interceptor = new ClassLoaderInterceptor(inst, transformer);
        interceptor.execute();

        transformer.markRunning();
    }

    //--//

    public static void loadAtRuntime()
    {
        try
        {
            //
            // We use an embedded JAR file to carry the manifest for the Agent.
            // This way we don't have to worry about how our code got bundled with the application.
            //
            URL resource = CompileTime.class.getResource("/asyncawait-bootstrap.jar");

            //
            // Simple case, it's bundled as a plain file.
            //
            if ("file".equals(resource.getProtocol()))
            {
                File jarFile = new File(resource.toURI());
                AgentLoader.loadAgent(jarFile.getPath(), null);
                return;
            }

            //
            // A bit more complicated, it's bundled as a jar, so extract our JAR to a temporary file.
            //
            try (FileSystem.TmpFileHolder tempJar = FileSystem.createTempFile("javaagent", ".jar"))
            {
                try (InputStream input = resource.openStream())
                {
                    try (FileOutputStream output = new FileOutputStream(tempJar.get()))
                    {
                        byte[] buffer = new byte[1024];

                        while (true)
                        {
                            int len = input.read(buffer);
                            if (len < 0)
                            {
                                break;
                            }

                            output.write(buffer, 0, len);
                        }
                    }

                    AgentLoader.loadAgent(tempJar.getAbsolutePath(), null);
                }
            }
        }
        catch (Throwable e)
        {
            throw new RuntimeException("Error attaching java agent", e);
        }
    }
}
