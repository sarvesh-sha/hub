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
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.util.TimeUtils;

public class ImagePullSession
{
    private final DeployerApplication m_app;
    private final String              m_image;

    private final List<ShellOutput>          m_output = Lists.newArrayList();
    private       CompletableFuture<Boolean> m_done;
    private       String                     m_imageSha;
    private       boolean                    m_shutdown;

    public ImagePullSession(DeployerApplication app,
                            String image)
    {
        m_app = app;
        m_image = image;
    }

    public void start() throws
                        Exception
    {
        ImagePullSessionWorker worker = new ImagePullSessionWorker(m_app, m_image, () -> m_shutdown, this::addLine, this::setImageSha);
        m_done = worker.execute();
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

    public boolean isDone()
    {
        return m_done != null && m_done.isDone();
    }

    public String getImage()
    {
        return m_image;
    }

    public String getImageSha()
    {
        return isDone() ? m_imageSha : null;
    }

    private void setImageSha(String sha)
    {
        m_imageSha = sha;
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
