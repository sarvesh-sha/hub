/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.optio3.concurrency.Executors;
import com.optio3.serialization.ObjectMappers;

public class NgrokHelper implements AutoCloseable
{
    private final String m_port;

    private Process m_proc;

    private String m_remoteEndpoint;
    private String m_localEndpoint;

    public NgrokHelper(String port)
    {
        m_port = port;
    }

    @Override
    public void close()
    {
        if (m_proc != null)
        {
            m_proc.destroyForcibly();
        }
    }

    public String start() throws
                          Exception
    {
        if (extractInfo(1))
        {
            if (String.format("localhost:%s", m_port)
                      .equals(m_localEndpoint))
            {
                // Already started, nothing to do.
                return m_remoteEndpoint;
            }

            // Already running but on a different port, restart.
            klllAllInstances();
        }

        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/usr/local/bin/ngrok", "tcp", m_port);

        m_proc = builder.start();

        extractInfo(10);
        return m_remoteEndpoint;
    }

    public static void klllAllInstances() throws
                                          Exception
    {
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("/usr/bin/killall", "ngrok");
        builder.start()
               .waitFor(1, TimeUnit.SECONDS);
    }

    //--//

    private boolean extractInfo(int retries)
    {
        for (int i = 0; i < retries; i++)
        {
            Executors.safeSleep(100);

            if (extractInfoInner())
            {
                return true;
            }
        }

        return false;
    }

    private boolean extractInfoInner()
    {
        try (InputStream stream = new URL("http://127.0.0.1:4040/api/tunnels").openStream())
        {
            JsonNode tree = ObjectMappers.SkipNulls.readTree(stream);

            ArrayNode nodeTunnels = (ArrayNode) tree.get("tunnels");
            if (nodeTunnels == null)
            {
                return false;
            }

            for (Iterator<JsonNode> it = nodeTunnels.elements(); it.hasNext(); )
            {
                JsonNode node = it.next();

                JsonNode nodePublicUrl = node.get("public_url");
                if (nodePublicUrl == null)
                {
                    continue;
                }

                JsonNode nodeConfig = node.get("config");
                if (nodeConfig == null)
                {
                    continue;
                }

                JsonNode nodeAddr = nodeConfig.get("addr");
                if (nodeAddr == null)
                {
                    continue;
                }

                m_remoteEndpoint = nodePublicUrl.asText();
                m_localEndpoint = nodeAddr.asText();
                return true;
            }
        }
        catch (Throwable t)
        {
            // Ignore failure...
        }

        return false;
    }
}
