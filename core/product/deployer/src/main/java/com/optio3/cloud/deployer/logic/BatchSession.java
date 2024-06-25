/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.logic;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.ShellOutput;
import com.optio3.cloud.client.deployer.model.batch.DockerBatch;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.util.TimeUtils;

public class BatchSession
{
    private final DeployerApplication m_app;
    private final List<DockerBatch>   m_list;

    private final List<ShellOutput>                               m_output = Lists.newArrayList();
    private       CompletableFuture<List<DockerBatch.BaseResult>> m_done;
    private       boolean                                         m_shutdown;

    public BatchSession(DeployerApplication app,
                        List<DockerBatch> list)
    {
        m_app = app;
        m_list = list;
    }

    public void start() throws
                        Exception
    {
        if (m_done == null)
        {
            BatchSessionWorker worker = new BatchSessionWorker(m_app, m_list, () -> m_shutdown, this::addLine);
            m_done = worker.execute();
        }
    }

    public void stop()
    {
        m_shutdown = true;

        if (m_done != null)
        {
            m_done.completeExceptionally(new TimeoutException());
        }
    }

    public List<ShellOutput> getOutput()
    {
        synchronized (m_output)
        {
            return Lists.newArrayList(m_output);
        }
    }

    public CompletableFuture<List<DockerBatch.BaseResult>> getDone()
    {
        return m_done;
    }

    //--//

    private void addLine(String line)
    {
        ShellOutput so = new ShellOutput();
        so.timestamp = TimeUtils.now();
        so.payload = line;

        synchronized (m_output)
        {
            m_output.add(so);
        }
    }
}
