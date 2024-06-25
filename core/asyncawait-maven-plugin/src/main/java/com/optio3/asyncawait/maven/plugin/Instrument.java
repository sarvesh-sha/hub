/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "instrument", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = false, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class Instrument extends SharedMojo
{
    /**
     * Location of the file.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * If set to <code>true</code> it will bypass instrumenting the unit tests
     * entirely.
     */
    @Parameter(property = "optio3.instrument.skip")
    private boolean dontInstrument;

    @Override
    public void execute() throws
                          MojoExecutionException
    {
        if (dontInstrument)
        {
            getLog().info("process-classes instrumentation disabled for this project");
            return;
        }

        super.execute();
    }

    @Override
    protected String getType()
    {
        return "main-classes";
    }

    /**
     * Return the main classes directory
     */
    @Override
    protected File getClassesDirectory()
    {
        return classesDirectory;
    }

    @Override
    protected ClassLoader createClassLoader()
    {
        return createClassLoader(generateClassPath(true));
    }
}
