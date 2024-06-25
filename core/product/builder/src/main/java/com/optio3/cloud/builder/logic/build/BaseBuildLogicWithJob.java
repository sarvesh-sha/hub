/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic.build;

import static java.util.Objects.requireNonNull;

import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.worker.HostBoundResource;
import com.optio3.cloud.builder.persistence.worker.HostRecord;
import com.optio3.util.Exceptions;

public abstract class BaseBuildLogicWithJob extends BaseBuildLogic
{
    protected final JobRecord m_job;

    protected BaseBuildLogicWithJob(BuilderConfiguration config,
                                    HostRecord targetHost,
                                    JobRecord job)
    {
        super(config, targetHost);

        m_job = job;
    }

    //--//

    protected void ensureAcquired(HostBoundResource target)
    {
        requireNonNull(target);

        final JobRecord ownerJob = target.getAcquiredBy();
        if (ownerJob != m_job)
        {
            throw Exceptions.newRuntimeException("Resource %s (associated with %s) not acquired by Job %s",
                                                 target.getResourceDisplayName(),
                                                 JobRecord.getDisplayName(ownerJob),
                                                 JobRecord.getDisplayName(m_job));
        }
    }
}
