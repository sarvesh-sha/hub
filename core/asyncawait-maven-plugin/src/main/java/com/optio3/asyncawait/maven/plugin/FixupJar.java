/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.maven.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import com.optio3.archive.ZipWalker;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "fixupjar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class FixupJar extends AbstractMojo
{
    /**
     * Directory containing the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.directory}", property = "outputDirectory", required = true)
    private File outputDirectory;

    /**
     * Name of the generated JAR.
     */
    @Parameter(defaultValue = "${project.build.finalName}", property = "finalName", readonly = true)
    private String finalName;

    /**
     * Prints each file being instrumented
     */
    @Parameter
    private boolean verbose = false;

    @Override
    public void execute() throws
                          MojoExecutionException
    {
        File jar = new File(outputDirectory, finalName + ".jar");

        if (jar.exists())
        {
            File jarBackup = new File(outputDirectory, finalName + ".jar.bak");
            if (jarBackup.exists())
            {
                jarBackup.delete();
            }

            if (!jar.renameTo(jarBackup))
            {
                throw new MojoExecutionException("Failed to rename JAR for backup");
            }

            try (InputStream input = new FileInputStream(jarBackup))
            {
                List<ZipWalker.ArchiveEntry> entries = ZipWalker.load(input);

                //
                // JAR files require the first entry to be the manifest.
                // Sort files keeping that in mind.
                //
                final String manifest = "meta-inf/manifest.mf";

                entries.sort((left, right) ->
                             {
                                 String leftName  = left.getName();
                                 String rightName = right.getName();

                                 boolean leftIsManifest  = StringUtils.equalsIgnoreCase(manifest, leftName);
                                 boolean rightIsManifest = StringUtils.equalsIgnoreCase(manifest, rightName);

                                 if (leftIsManifest)
                                 {
                                     return -1;
                                 }

                                 if (rightIsManifest)
                                 {
                                     return 1;
                                 }

                                 return leftName.compareTo(rightName);
                             });

                // Pick a constant time, so it won't affect the hash.
                ZonedDateTime dt = ZonedDateTime.of(2018, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());

                for (ZipWalker.ArchiveEntry inMemoryEntry : entries)
                {
                    inMemoryEntry.setTime(dt);
                }

                try (OutputStream output = new FileOutputStream(jar))
                {
                    ZipWalker.save(output, 9, entries);

                    getLog().info(String.format("Updated JAR %s.jar to have timestamp %s", finalName, dt));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new MojoExecutionException(String.format("Failed to fixup JAR '%s'", finalName), e);
            }

            jarBackup.delete();
        }
    }
}
