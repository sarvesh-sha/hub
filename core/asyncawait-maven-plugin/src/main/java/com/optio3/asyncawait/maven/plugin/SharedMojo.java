/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.maven.plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.converter.AsyncTransformer;
import com.optio3.codeanalysis.ClassAnalyzer;
import com.optio3.codeanalysis.logging.CodeAnalysisLogger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class SharedMojo extends AbstractMojo
{
    /**
     * Directory where the instrumented files will be written. If not set the
     * files will be overwritten with the new data.
     */
    @Parameter(required = false)
    private File outputDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
    protected File classesDirectory;

    @Parameter(defaultValue = "${project.build.directory}/asyncwait.timestamp.", required = false)
    protected File lastRunTimestamp;

    /**
     * Prints each file being instrumented
     */
    @Parameter
    protected boolean verbose = false;

    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;

    /**
     * Instruments the files.
     */
    @Override
    public void execute() throws
                          MojoExecutionException
    {
        File dir = getClassesDirectory();
        if (dir == null || !dir.exists() || dir.list().length < 1)
        {
            getLog().info("Skipping instrumentation of the " + getType());
        }
        else
        {
            instrumentFiles();
        }
    }

    public boolean isVerbose()
    {
        return verbose;
    }

    /**
     * Instrument the files that require instrumentation.
     */
    public void instrumentFiles() throws
                                  MojoExecutionException
    {
        try
        {
            File contentDirectory = getClassesDirectory();
            if (!contentDirectory.exists())
            {
                getLog().warn(getType() + " directory is empty! " + contentDirectory);
                return;
            }

            List<String> classes          = Lists.newArrayList();
            long         lastModified     = getFiles(contentDirectory, classes);
            File         lastRunTimestamp = this.lastRunTimestamp;
            if (lastRunTimestamp != null)
            {
                lastRunTimestamp = new File(lastRunTimestamp.getAbsolutePath() + getType());
            }

            if (lastRunTimestamp != null && lastRunTimestamp.exists() && lastRunTimestamp.lastModified() >= lastModified)
            {
                getLog().info("Optio3 Async still up-to-date...");
                return;
            }

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

            int instrumentedCount = 0;
            for (String classFile : classes)
            {
                getLog().info(String.format("Instrumenting file %s...", classFile));

                int    pos       = classFile.lastIndexOf(".class");
                String className = classFile.substring(0, pos);

                Map<String, ClassAnalyzer> results;

                try
                {
                    results = transfomInner(null, loader, className);
                }
                catch (Exception e)
                {
                    getLog().error(String.format("Encountered exception '%s' while trying to transform %s...", e, className));
                    getLog().error(e);

                    //
                    // Retry with debug logging enabled.
                    //
                    CodeAnalysisLogger logger = CodeAnalysisLogger.createCallbackLogger(CodeAnalysisLogger.Level.DEBUG, (s) -> getLog().error(s));
                    results = transfomInner(logger, loader, className);
                }

                if (results != null)
                {
                    for (String outputName : results.keySet())
                    {
                        ClassAnalyzer ca = results.get(outputName);

                        getLog().info(String.format("Instrumented %s", outputName));

                        byte[] bytecode = ca.encode();
                        File   f        = new File(contentDirectory, outputName + ".class");
                        try (FileOutputStream stream = new FileOutputStream(f))
                        {
                            stream.write(bytecode);
                        }
                    }
                    instrumentedCount++;
                }
            }

            if (lastRunTimestamp != null)
            {
                try (OutputStream stream = new FileOutputStream(lastRunTimestamp))
                {
                    String contents = "Optio3 Async " + getType() + " instrumented: " + instrumentedCount;
                    stream.write(contents.getBytes());
                }
            }

            getLog().info("Optio3 Async " + getType() + " instrumented: " + instrumentedCount);
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Error assembling instrumenting", e);
        }
    }

    private Map<String, ClassAnalyzer> transfomInner(CodeAnalysisLogger logger,
                                                     ClassLoader loader,
                                                     String internalName) throws
                                                                          AnalyzerException,
                                                                          IOException
    {
        AsyncTransformer worker = new AsyncTransformer(loader, internalName, logger);
        if (!worker.shouldTransform())
        {
            return null;
        }

        return worker.transform();
    }

    protected long getFiles(final File contentDirectory,
                            List<String> results) throws
                                                  MojoExecutionException
    {
        if (!contentDirectory.isDirectory())
        {
            throw new MojoExecutionException(contentDirectory.getAbsolutePath() + " is not a directory.");
        }

        return collect(results, contentDirectory, null);
    }

    private static long collect(List<String> results,
                                File contentDirectory,
                                String prefix)
    {
        long lastModified = Long.MIN_VALUE;

        for (File file : contentDirectory.listFiles())
        {
            String relativePath = prefix != null ? (prefix + File.separator + file.getName()) : file.getName();

            if (file.isDirectory())
            {
                long lastModifiedChild = collect(results, file, relativePath);
                lastModified = Math.max(lastModified, lastModifiedChild);
            }
            else if (file.getName()
                         .endsWith(".class"))
            {
                lastModified = Math.max(lastModified, file.lastModified());
                results.add(relativePath);
            }
        }

        return lastModified;
    }

    //--//

    protected List<File> generateClassPath(boolean skipTest)
    {
        List<File> classpath = Lists.newArrayList();

        classpath.add(classesDirectory);

        Set<Artifact> classpathArtifacts = project.getArtifacts();

        for (Artifact artifact : classpathArtifacts)
        {
            if (artifact.getArtifactHandler()
                        .isAddedToClasspath())
            {
                if (skipTest && "test".equalsIgnoreCase(artifact.getScope()))
                {
                    continue;
                }

                File file = artifact.getFile();
                if (file != null)
                {
                    classpath.add(file);
                }
            }
        }

        return classpath;
    }

    protected ClassLoader createClassLoader(List<File> paths)
    {
        try
        {
            URL[] urls = new URL[paths.size()];
            for (int i = 0; i < urls.length; i++)
                urls[i] = paths.get(i)
                               .toURI()
                               .toURL();

            return new URLClassLoader(urls);
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException(e);
        }
    }

    //-//

    /**
     * Return the specific output directory to instrument.
     */
    protected abstract File getClassesDirectory();

    protected abstract String getType();

    protected abstract ClassLoader createClassLoader();
}
