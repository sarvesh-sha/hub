/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.asyncwait.maven.plugin;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class MyMojoTest// extends AbstractMojoTestCase
{
    @Rule
    public MojoRule rule = new MojoRule()
    {
        @Override
        protected void before()
        {
        }

        @Override
        protected void after()
        {
        }
    };

    //
    // This test should be manually enabled after building the sample-project.
    //
    // First do "mvn install -Dmaven.test.skip=true" for the plugin project.
    // Then "mvn compile" and "mvn test-compile" for the sample project.
    // Finally, you are ready to test.
    //
    @Test
    @Ignore
    public void instrumentSampleProject() throws
                                          Exception
    {
        File baseDir = new File("sample-project");
        rule.executeMojo(baseDir, "instrument");
        rule.executeMojo(baseDir, "instrument-test");
    }
}
