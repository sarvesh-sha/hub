/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.maven.plugin;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "instrument-test", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, requiresProject = false, threadSafe = true, executionStrategy = "always",
      requiresDependencyResolution = ResolutionScope.TEST)
public class InstrumentTest extends SharedMojo
{
    /**
     * If set to <code>true</code> it will bypass instrumenting the unit tests
     * entirely.
     */
    @Parameter(property = "maven.test.skip")
    private boolean skip;

    /**
     * If set to <code>true</code> it will bypass instrumenting the unit tests
     * entirely.
     */
    @Parameter(property = "optio3.instrument.test.skip")
    private boolean dontInstrument;

    /**
     * Directory containing the test classes files that should be instrumented
     */
    @Parameter(defaultValue = "${project.build.testOutputDirectory}", required = true)
    private File testClassesDirectory;

    @Override
    public void execute() throws
                          MojoExecutionException
    {
        if (dontInstrument)
        {
            getLog().info("process-test-classes instrumentation disabled for this project");
            return;
        }

        if (skip)
        {
            getLog().info("Skipping the instrumentation of the test classes");
            return;
        }

        super.execute();
    }

    @Override
    protected String getType()
    {
        return "test-classes";
    }

    /**
     * Return the test classes directory
     */
    @Override
    protected File getClassesDirectory()
    {
        return testClassesDirectory;
    }

    @Override
    protected ClassLoader createClassLoader()
    {
        List<File> paths = generateClassPath(false);

        paths.add(testClassesDirectory);

        return createClassLoader(paths);
    }
}
