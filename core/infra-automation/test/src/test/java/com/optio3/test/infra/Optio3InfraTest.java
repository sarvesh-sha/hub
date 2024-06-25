/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.test.infra;

import java.io.File;

import ch.qos.logback.classic.Logger;
import com.optio3.infra.directory.CredentialDirectory;
import com.optio3.test.common.Optio3Test;
import com.optio3.util.Exceptions;
import io.dropwizard.logging.LoggingUtil;

public abstract class Optio3InfraTest extends Optio3Test
{
    protected CredentialDirectory credDir;

    protected void ensureCredentials(boolean useFat,
                                     boolean failIfMissing) throws
                                                            Exception
    {
        disableSlf4jLogger();

        String root   = System.getenv("HOME") + "/git/infra/identity/";
        File   master = new File(root + (useFat ? "masterFat.key" : "master.key"));
        if (!master.exists())
        {
            if (failIfMissing)
            {
                throw Exceptions.newRuntimeException("No %s file", master);
            }

            return;
        }

        credDir = CredentialDirectory.load(master);
    }

    private void disableSlf4jLogger()
    {
        final Logger root = LoggingUtil.getLoggerContext()
                                       .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAndStopAllAppenders();
    }
}
