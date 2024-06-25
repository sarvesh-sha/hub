/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.dropwizard.Configuration;

public abstract class AbstractConfiguration extends Configuration
{
    public final JsonWebSocketDnsHints dnsHints = new JsonWebSocketDnsHints();

    public String scratchDirectory;

    private boolean m_runningUnitTests;

    @JsonIgnore
    public boolean isRunningUnitTests()
    {
        return m_runningUnitTests;
    }

    public void markAsUnitTest()
    {
        m_runningUnitTests = true;
    }
}

