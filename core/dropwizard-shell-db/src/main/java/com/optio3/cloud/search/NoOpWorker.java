/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.search;

import java.util.Properties;

import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.Worker;
import org.hibernate.search.spi.WorkerBuildContext;

public class NoOpWorker implements Worker
{
    public NoOpWorker()
    {
    }

    @Override
    public void performWork(Work work,
                            TransactionContext transactionContext)
    {
    }

    @Override
    public void initialize(Properties props,
                           WorkerBuildContext context,
                           QueueingProcessor queueingProcessor)
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public void flushWorks(TransactionContext transactionContext)
    {
    }
}
