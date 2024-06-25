/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.concurrency.Executors;
import com.optio3.infra.SshHelper;
import com.optio3.text.CommandLineTokenizer;

public class ShellSession
{
    private static final byte[] s_empty = new byte[0];

    private final int                timeout;
    private final TimeUnit           unit;
    private       ScheduledFuture<?> m_timeoutPromise;

    private SshHelper m_sshHelper;

    private Process m_proc;

    //--//

    public ShellSession(String commandLine,
                        int timeout,
                        TimeUnit unit) throws
                                       IOException
    {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command(CommandLineTokenizer.translate(commandLine));

        m_proc = builder.start();

        this.timeout = timeout;
        this.unit = unit;
        resetTimeout();
    }

    public ShellSession(String server,
                        String user,
                        byte[] privateKey,
                        byte[] publicKey,
                        byte[] passphrase,
                        int timeout,
                        TimeUnit unit) throws
                                       Exception
    {
        m_sshHelper = new SshHelper(privateKey, publicKey, passphrase, server, user);
        m_sshHelper.openShell();

        this.timeout = timeout;
        this.unit = unit;
        resetTimeout();
    }

    public void stop()
    {
        clearTimeout();

        if (m_proc != null)
        {
            m_proc.destroyForcibly();
            m_proc = null;
        }

        if (m_sshHelper != null)
        {
            m_sshHelper.close();
            m_sshHelper = null;
        }
    }

    public void writeToStdin(byte[] buffer) throws
                                            IOException
    {
        resetTimeout();

        OutputStream stream;

        if (m_proc != null)
        {
            stream = m_proc.getOutputStream();
        }
        else if (m_sshHelper != null)
        {
            stream = m_sshHelper.getOutputStream();
        }
        else
        {
            return;
        }

        stream.write(buffer);
        stream.flush();
    }

    public byte[] readFromStdout(int max) throws
                                          Exception
    {
        resetTimeout();

        if (m_proc != null)
        {
            return read(m_proc.getInputStream(), max);
        }

        if (m_sshHelper != null)
        {
            return read(m_sshHelper.getInputStream(), max);
        }

        return s_empty;
    }

    public byte[] readFromStderr(int max) throws
                                          Exception
    {
        resetTimeout();

        if (m_proc != null)
        {
            return read(m_proc.getErrorStream(), max);
        }

        return s_empty;
    }

    public int getExitCode()
    {
        if (m_sshHelper != null)
        {
            return -1;
        }

        if (m_proc == null)
        {
            return -3;
        }

        if (m_proc.isAlive())
        {
            return -1;
        }

        return m_proc.exitValue();
    }

    //--//

    private void clearTimeout()
    {
        if (m_timeoutPromise != null)
        {
            m_timeoutPromise.cancel(false);
            m_timeoutPromise = null;
        }
    }

    private void resetTimeout()
    {
        clearTimeout();

        m_timeoutPromise = Executors.scheduleOnDefaultPool(this::stop, timeout, unit);
    }

    private byte[] read(InputStream stream,
                        int max) throws
                                 IOException
    {
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        int                   available;
        byte[]                buf = null;

        while (max > 0 && (available = stream.available()) > 0)
        {
            if (buf == null)
            {
                buf = new byte[1024];
            }

            int len = Math.min(available, max);

            int read = stream.read(buf, 0, Math.min(len, buf.length));
            if (read < 0)
            {
                break;
            }

            res.write(buf, 0, read);
            max -= read;
        }

        return res.toByteArray();
    }
}
