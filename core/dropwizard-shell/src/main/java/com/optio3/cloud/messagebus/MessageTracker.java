/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.messagebus;

import java.util.Set;

import com.google.common.collect.Sets;

class MessageTracker
{
    private final String[]    m_messages;
    private final Set<String> m_seen;
    private       int         m_cursor;

    MessageTracker(int size)
    {
        m_messages = new String[size];
        m_seen = Sets.newHashSetWithExpectedSize(size);
    }

    synchronized boolean track(String messageId)
    {
        if (!m_seen.add(messageId))
        {
            return false;
        }

        int cursor = m_cursor;

        String oldMessageId = m_messages[cursor];
        if (oldMessageId != null)
        {
            m_seen.remove(oldMessageId);
        }

        m_messages[cursor] = messageId;

        if (++cursor >= m_messages.length)
        {
            cursor = 0;
        }

        m_cursor = cursor;
        return true;
    }
}
