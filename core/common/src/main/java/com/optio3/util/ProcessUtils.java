/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;

public class ProcessUtils
{
    public static byte[] execAndCaptureOutput(File redirectStdin,
                                              long timeout,
                                              TimeUnit unit,
                                              String... commandLine) throws
                                                                     IOException
    {
        InputStream stream = exec(redirectStdin, timeout, unit, commandLine);
        return IOUtils.toByteArray(stream);
    }

    public static List<String> execAndCaptureOutputAsLines(File redirectStdin,
                                                           long timeout,
                                                           TimeUnit unit,
                                                           String... commandLine) throws
                                                                                  IOException
    {
        InputStream stream = exec(redirectStdin, timeout, unit, commandLine);
        return IOUtils.readLines(stream, Charset.defaultCharset());
    }

    public static InputStream exec(File redirectStdin,
                                   long timeout,
                                   TimeUnit unit,
                                   String... commandLine) throws
                                                          IOException
    {
        ProcessBuilder pb = new ProcessBuilder(commandLine);

        if (redirectStdin != null)
        {
            pb.redirectInput(redirectStdin);
        }

        Process p = pb.start();

        boolean exited;

        try
        {
            exited = p.waitFor(timeout, unit);
        }
        catch (InterruptedException e)
        {
            exited = false;
        }

        if (!exited)
        {
            p.destroyForcibly();
            throw Exceptions.newGenericException(IOException.class, "Timeout executing '%s'", String.join(" ", commandLine));
        }

        int exitCode = p.exitValue();
        if (exitCode != 0)
        {
            String error = IOUtils.toString(p.getErrorStream(), Charset.defaultCharset());
            throw Exceptions.newGenericException(IOException.class, "Execution of '%s' failed with exit code: %d (stderr = '%s')", String.join(" ", commandLine), exitCode, error);
        }

        return p.getInputStream();
    }
}
