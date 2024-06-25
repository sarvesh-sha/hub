/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra.various;

import java.io.IOException;

import com.optio3.infra.SshHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.SshKey;
import com.optio3.test.infra.Optio3InfraTest;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class SshTest extends Optio3InfraTest
{
    @Before
    public void setup() throws
                        Exception
    {
        ensureCredentials(true, false);
    }

    @Ignore("Manually enable to test, since it requires access to AWS")
    @Test
    public void listFilesWithJSch() throws
                                    Exception
    {
        SshKey key = credDir.findFirstSshKey("amazon.com", "ec2-user");

        try (SshHelper helper = new SshHelper(key, WellKnownSites.dockerRegistry(), key.user))
        {
            helper.openShell();

            Thread worker = new Thread()
            {
                @Override
                public void run()
                {
                    try
                    {
                        IOUtils.copy(helper.getInputStream(), System.out);
                    }
                    catch (IOException e)
                    {
                    }
                }
            };

            worker.start();
            try
            {
                helper.getOutputStream()
                      .write("ls -al\n".getBytes());
                Thread.sleep(5000);
            }
            finally
            {
                worker.interrupt();
            }
        }
    }
}
