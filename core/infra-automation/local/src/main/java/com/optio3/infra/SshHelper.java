/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Logger;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;
import com.optio3.concurrency.Executors;
import com.optio3.infra.directory.SshKey;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;
import com.optio3.util.function.ConsumerWithException;

public class SshHelper implements AutoCloseable
{
    private final JSch    m_ssh;
    private final Session m_session;

    private ChannelShell      m_shellChannel;
    private PipedOutputStream m_output;
    private PipedInputStream  m_input;

    public SshHelper(SshKey key,
                     String host,
                     String user) throws
                                  Exception
    {
        this(key.getPrivateKey(), key.getPublicKey(), key.passphrase.getBytes(), host, user);
    }

    public SshHelper(byte[] privateKey,
                     byte[] publicKey,
                     byte[] passphrase,
                     String host,
                     String user) throws
                                  Exception
    {
        m_ssh = new JSch();

//        enableVerboseLogging();

        m_ssh.addIdentity(null, privateKey, publicKey, passphrase);

        m_session = m_ssh.getSession(user, host);
        m_session.setServerAliveCountMax(360);
        m_session.setServerAliveInterval(10000);

        HostKeyRepository hostkeyRepository = new HostKeyRepository()
        {
            Map<String, HostKey> knownHosts = Maps.newHashMap();

            @Override
            public int check(String host,
                             byte[] key)
            {
                HostKey hk = knownHosts.get(host);
                if (hk == null)
                {
                    try
                    {
                        hk = new HostKey(host, key);
                    }
                    catch (JSchException e)
                    {
                    }

                    knownHosts.put(host, hk);
                }

                return HostKeyRepository.OK;
            }

            @Override
            public void add(HostKey hostkey,
                            UserInfo ui)
            {
            }

            @Override
            public void remove(String host,
                               String type)
            {
            }

            @Override
            public void remove(String host,
                               String type,
                               byte[] key)
            {
            }

            @Override
            public String getKnownHostsRepositoryID()
            {
                throw new RuntimeException("Not implemented");
            }

            @Override
            public HostKey[] getHostKey()
            {
                Collection<HostKey> list = knownHosts.values();

                return toArray(list);
            }

            @Override
            public HostKey[] getHostKey(String host,
                                        String type)
            {
                List<HostKey> list = Lists.newArrayList();

                HostKey hk = knownHosts.get(host);
                if (hk != null)
                {
                    list.add(hk);
                }

                return toArray(list);
            }

            private HostKey[] toArray(Collection<HostKey> val)
            {

                HostKey[] res = new HostKey[val.size()];
                val.toArray(res);

                return res;
            }
        };

        m_session.setHostKeyRepository(hostkeyRepository);
        m_session.connect();
    }

    @Override
    public void close()
    {
        closeShell();

        m_session.disconnect();
    }

    public void enableVerboseLogging()
    {
        JSch.setLogger(new Logger()
        {
            @Override
            public boolean isEnabled(int level)
            {
                return true;
            }

            @Override
            public void log(int level,
                            String message)
            {
                System.out.println(message);
            }
        });
    }

    public int exec(String commandLine,
                    int timeout,
                    TimeUnit unit,
                    ConsumerWithException<String> stdout,
                    ConsumerWithException<String> stderr) throws
                                                          Exception
    {
        ChannelExec channel = (ChannelExec) m_session.openChannel("exec");
        channel.setCommand(commandLine);

        channel.setInputStream(null);

        try (LineConverter stdoutConv = new LineConverter(stdout, channel.getInputStream());
             LineConverter stderrConv = new LineConverter(stderr, channel.getExtInputStream()))
        {
            channel.connect();

            try
            {
                MonotonousTime deadline = TimeUtils.computeTimeoutExpiration(timeout, unit);

                while (!channel.isEOF() && !TimeUtils.isTimeoutExpired(deadline))
                {
                    Executors.safeSleep(100);
                    stdoutConv.pump();
                    stderrConv.pump();
                }

                return channel.getExitStatus();
            }
            finally
            {
                channel.disconnect();
            }
        }
    }

    public void openShell() throws
                            Exception
    {
        ChannelShell channel = (ChannelShell) m_session.openChannel("shell");

        PipedOutputStream outputRes = new PipedOutputStream();
        PipedInputStream  input     = new PipedInputStream(outputRes, 32 * 1024); // Large output buffer, otherwise the Pipe will stall for 1 second
        channel.setInputStream(input);

        PipedOutputStream output   = new PipedOutputStream();
        PipedInputStream  inputRes = new PipedInputStream(output, 32 * 1024); // Large input buffer, otherwise the Pipe will stall for 1 second.
        channel.setOutputStream(output);

        channel.connect();

        synchronized (m_session)
        {
            closeShell();
            m_shellChannel = channel;
            m_input = inputRes;
            m_output = outputRes;
        }
    }

    public void closeShell()
    {
        synchronized (m_session)
        {
            if (m_shellChannel != null)
            {
                m_shellChannel.disconnect();
                m_shellChannel = null;
            }

            if (m_input != null)
            {
                try
                {
                    m_input.close();
                }
                catch (IOException e)
                {
                    // Ignore exception on shutdown
                }

                m_input = null;
            }

            if (m_output != null)
            {
                try
                {
                    m_output.close();
                }
                catch (IOException e)
                {
                    // Ignore exception on shutdown
                }

                m_output = null;
            }
        }
    }

    public PipedOutputStream getOutputStream()
    {
        return m_output;
    }

    public PipedInputStream getInputStream()
    {
        return m_input;
    }

    //--//

    static class LineConverter implements AutoCloseable
    {
        private final ConsumerWithException<String> m_callback;
        private final InputStream                   m_stream;
        private final StringBuilder                 m_sb;

        public LineConverter(ConsumerWithException<String> callback,
                             InputStream stream)
        {
            m_callback = callback;
            m_stream = stream;
            m_sb = new StringBuilder();
        }

        void pump() throws
                    Exception
        {
            int len;

            while ((len = m_stream.available()) > 0)
            {
                byte[] buf = new byte[len];
                m_stream.read(buf);

                for (int i = 0; i < len; i++)
                {
                    char c = (char) buf[i];
                    m_sb.append(c);

                    if (c == '\n')
                    {
                        flush();
                    }
                }
            }
        }

        private void flush() throws
                             Exception
        {
            if (m_callback != null)
            {
                m_callback.accept(m_sb.toString());
            }

            m_sb.setLength(0);
        }

        @Override
        public void close() throws
                            Exception
        {
            pump();
            flush();
        }
    }
}
