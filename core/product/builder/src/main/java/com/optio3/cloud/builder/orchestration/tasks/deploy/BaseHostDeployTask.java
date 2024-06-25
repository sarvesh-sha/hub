/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;

public abstract class BaseHostDeployTask extends BaseDeployTask implements BackgroundActivityHandler.ICleanupOnFailure,
                                                                           BackgroundActivityHandler.IPostProcess
{
    public static class ActivityWithHost
    {
        public RecordLocator<BackgroundActivityRecord> activity;
        public RecordLocked<DeploymentHostRecord>      lock_host;
    }

    @Override
    public void postProcess(Throwable t) throws
                                         Exception
    {
        lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
        {
            final DeploymentHostRecord rec_host = lock_host.get();

            rec_host.conditionallyChangeStatus(DeploymentStatus.Cancelling, DeploymentStatus.Cancelled);

            if (rec_host.getStatus() == DeploymentStatus.Cancelled)
            {
                markAsFailed(new CancellationException());
            }
        });
    }

    @Override
    public void cleanupOnFailure(Throwable t) throws
                                              Exception
    {
        lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
        {
            final DeploymentHostRecord rec_host = lock_host.get();

            rec_host.conditionallyChangeStatus(DeploymentStatus.Booting, DeploymentStatus.BootFailed);
            rec_host.conditionallyChangeStatus(DeploymentStatus.Cancelling, DeploymentStatus.Cancelled);
        });
    }

    //--//

    protected boolean setHostStatus(DeploymentStatus status) throws
                                                             Exception
    {
        return lockedWithLocatorOrNull(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
        {
            if (lock_host == null)
            {
                return false;
            }

            final DeploymentHostRecord rec_host = lock_host.get();

            switch (rec_host.getStatus())
            {
                case Cancelling:
                case Cancelled:
                    return false;

                default:
                    if (status == DeploymentStatus.Ready)
                    {
                        rec_host.setOperationalStatus(DeploymentOperationalStatus.operational);
                    }

                    rec_host.setStatus(status);
                    return true;
            }
        });
    }
}
