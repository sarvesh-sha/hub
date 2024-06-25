/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.maven.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.DependencyAnalyzer;
import com.optio3.codeanalysis.GenericTypeInfo;
import com.optio3.codeanalysis.TypeResolver;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "analyze", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresProject = false, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class Analyze extends SharedMojo
{
    /**
     * If set to <code>true</code> it will bypass instrumenting the unit tests
     * entirely.
     */
    @Parameter(property = "optio3.instrument.skip")
    private boolean dontInstrument;

    /**
     * If set to <code>true</code> it will bypass instrumenting the unit tests
     * entirely.
     */
    @Parameter(property = "optio3.analyze.onlyjars")
    private boolean onlyJARs;

    /**
     * If set to <code>true</code> it will describe the dependencies of each class
     * entirely.
     */
    @Parameter(property = "optio3.analyze.verbose")
    private boolean includeEachClass;

    @Override
    public void execute() throws
                          MojoExecutionException
    {
        if (dontInstrument)
        {
            getLog().info("process-classes instrumentation disabled for this project");
            return;
        }

        File contentDirectory = getClassesDirectory();
        if (!contentDirectory.exists())
        {
            getLog().warn(getType() + " directory is empty! " + contentDirectory);
            return;
        }

        try
        {
            List<String> classes = Lists.newArrayList();
            getFiles(contentDirectory, classes);

            classes.sort(String::compareTo);

            ClassLoader ourClassLoader = createClassLoader();
            ClassLoader loader = new ClassLoader()
            {
                @Override
                public URL getResource(String name)
                {
                    if (classes.contains(name))
                    {
                        try
                        {
                            String path = "file:" + contentDirectory.getAbsolutePath() + File.separator + name;
                            URL    res  = new URL(path);
                            return res;
                        }
                        catch (MalformedURLException e)
                        {
                            e.printStackTrace();
                        }
                    }

                    return ourClassLoader.getResource(name);
                }
            };

            Multimap<File, String> reverseLookup = HashMultimap.create();
            boolean                firstFailure  = true;

            for (String classFile : classes)
            {
                getLog().debug(String.format("Analyzing file %s...", classFile));

                TypeResolver.Loader itf = (internalClassName) -> loader.getResource(internalClassName + ".class");

                int    pos       = classFile.lastIndexOf(".class");
                String className = classFile.substring(0, pos);

                try
                {
                    CodeAnalysisLogger logger       = CodeAnalysisLogger.createCallbackLogger(CodeAnalysisLogger.Level.WARN, System.out::println);
                    TypeResolver       typeResolver = new TypeResolver(logger, itf);

                    final TypeResolver.ClassLocator cl = typeResolver.getClassReader(className);
                    ClassAnalyzer                   ca = new ClassAnalyzer(typeResolver, cl);

                    DependencyAnalyzer              da      = new DependencyAnalyzer(ca);
                    Multimap<File, GenericTypeInfo> summary = da.generateSummary();

                    for (File file : summary.keySet())
                    {
                        reverseLookup.put(file, className);
                    }

                    if (includeEachClass)
                    {
                        getLog().info(String.format("Class '%s' depends on:", className));
                        for (File file : summary.keySet())
                        {
                            getLog().info(String.format("    %s", file));
                        }
                        getLog().info("----------------------------------------------------------------------------");
                    }
                }
                catch (Exception e)
                {
                    if (firstFailure)
                    {
                        firstFailure = false;
                        getLog().info(String.format("Encountered exception '%s' while trying to analyze %s...", e, className));
                        getLog().info(e);
                    }
                    else
                    {
                        getLog().debug(String.format("Encountered exception '%s' while trying to analyze %s...", e, className));
                        getLog().debug(e);
                    }
                }
            }

            final List<File> files = Lists.newArrayList(reverseLookup.keySet());
            files.sort(File::compareTo);
            for (File file : files)
            {
                if (!verbose && StringUtils.equals(file.getName(), "rt.jar"))
                {
                    continue;
                }

                if (onlyJARs && file.isDirectory())
                {
                    continue;
                }

                getLog().info(String.format("Project '%s' needed by:", file));
                ArrayList<String> lst = Lists.newArrayList(reverseLookup.get(file));
                lst.sort(String::compareTo);
                for (String s : lst)
                {
                    getLog().info(String.format("    %s", s));
                }
                getLog().info("----------------------------------------------------------------------------");
            }
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error assembling analysis", e);
        }
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
