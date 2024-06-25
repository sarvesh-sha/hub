/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.persistence;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.logging.Severity;
import com.optio3.util.TimeUtils;

public class LogHolder implements AutoCloseable
{
    private static final int MAX_BLOCK_SIZE = 100;

    private final LogHandler<?, ?> m_handler;
    private final StringBuilder    m_pendingLine;
    private final List<LogEntry>   m_pendingEntries = Lists.newArrayList();

    private boolean m_lastCharWasCR;

    LogHolder(LogHandler<?, ?> handler)
    {
        m_handler     = handler;
        m_pendingLine = new StringBuilder();
    }

    @Override
    public void close()
    {
        flushLine(true);

        flushBlock(false);
    }

    public CompletableFuture<Void> addLineAsync(int fd,
                                                ZonedDateTime timestamp,
                                                String host,
                                                String thread,
                                                String selector,
                                                Severity level,
                                                String line)
    {
        addLineSync(fd, timestamp, host, thread, selector, level, line);

        return AsyncRuntime.NullResult;
    }

    public void addLineSync(int fd,
                            ZonedDateTime timestamp,
                            String host,
                            String thread,
                            String selector,
                            Severity level,
                            String line)
    {
        var entry = new LogEntry();
        entry.fd        = fd;
        entry.timestamp = timestamp != null ? timestamp : TimeUtils.now();
        entry.host      = host;
        entry.thread    = thread;
        entry.selector  = selector;
        entry.level     = level;
        entry.line      = line;
        m_pendingEntries.add(entry);

        flushBlock(true);
    }

    public CompletableFuture<Void> addTextAsync(String text)
    {
        addTextSync(text);

        return AsyncRuntime.NullResult;
    }

    public void addTextSync(String text)
    {
        //
        // Split into lines.
        //
        int lastSplit = 0;

        for (int pos = 0; pos < text.length(); pos++)
        {
            char c = text.charAt(pos);

            switch (c)
            {
                case '\r':
                case '\n':
                    if (!m_lastCharWasCR)
                    {
                        appendToLine(text, lastSplit, pos);
                        flushLine(false);
                    }

                    lastSplit = pos + 1;
                    break;
            }

            m_lastCharWasCR = (c == '\r');
        }

        appendToLine(text, lastSplit, text.length());
    }

    //--//

    private void appendToLine(String text,
                              int begin,
                              int end)
    {
        if (begin < end)
        {
            m_pendingLine.append(text, begin, end);
        }
    }

    private void flushLine(boolean ifNotEmpty)
    {
        if (ifNotEmpty && m_pendingLine.length() == 0)
        {
            return;
        }

        m_pendingLine.append("\n");
        String line = m_pendingLine.toString();
        m_pendingLine.setLength(0);

        //--//

        var entry = new LogEntry();
        entry.fd        = 1;
        entry.timestamp = TimeUtils.now();
        entry.line      = line;
        m_pendingEntries.add(entry);

        flushBlock(true);
    }

    private void flushBlock(boolean ifNeeded)
    {
        if (ifNeeded && m_pendingEntries.size() < MAX_BLOCK_SIZE)
        {
            return;
        }

        if (!m_pendingEntries.isEmpty())
        {
            try
            {
                m_handler.saveBlock(m_pendingEntries);
            }
            catch (Throwable ex)
            {
                // Discard everything.
            }

            m_pendingEntries.clear();
        }
    }
}
