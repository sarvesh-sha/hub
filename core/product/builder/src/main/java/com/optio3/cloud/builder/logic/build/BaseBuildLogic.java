/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.build;

import javax.ws.rs.NotFoundException;

import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;

public abstract class BaseBuildLogic
{
    protected final BuilderConfiguration m_config;
    protected final HostRecord           m_targetHost;

    protected BaseBuildLogic(BuilderConfiguration config,
                             HostRecord targetHost)
    {
        m_config = config;
        m_targetHost = targetHost;
    }

    protected <T> T getProxy(Class<T> clz)
    {
        return m_config.hostRemoter.createRemotableProxy(m_targetHost, clz);
    }

    public UserInfo getCredentialForHost(String host,
                                         RoleType role) throws
                                                        NotFoundException
    {
        // Because we'll send credentials through an RPC channel, we need to get the effective one, otherwise we won't have a password.
        return m_config.getCredentialForHost(host, true, role);
    }
}
