/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.github.model.event;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public enum EventType
{
    issues(IssueEvent.class),
    issue_comment(IssueCommentEvent.class),
    create(CreateEvent.class),
    push(PushEvent.class),
    pull_request(PullRequestEvent.class);

    private final Class<? extends CommonEvent> m_clz;

    EventType(Class<? extends CommonEvent> clz)
    {
        m_clz = clz;
    }

    public static EventType parse(String value)
    {
        for (EventType t : values())
        {
            if (t.name()
                 .equals(value))
            {
                return t;
            }
        }

        return null;
    }

    public CommonEvent decode(ObjectMapper mapper,
                              byte[] payload) throws
                                              JsonParseException,
                                              JsonMappingException,
                                              IOException
    {
        return mapper.readValue(payload, m_clz);
    }
}
