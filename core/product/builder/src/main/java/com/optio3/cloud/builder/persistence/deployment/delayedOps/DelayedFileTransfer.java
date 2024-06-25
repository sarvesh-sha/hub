/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForHostFileTransfer;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostFileRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;

@JsonTypeName("DelayedFileTransfer")
public class DelayedFileTransfer extends DelayedOperation
{
    public RecordLocator<DeploymentHostFileRecord> loc_file;
    public boolean                                 upload;

    //--//

    public static boolean queue(SessionHolder sessionHolder,
                                DeploymentHostFileRecord rec_file,
                                boolean upload) throws
                                                Exception
    {
        DeploymentHostRecord               rec_target  = rec_file.getDeployment();
        RecordLocked<DeploymentHostRecord> lock_target = sessionHolder.optimisticallyUpgradeToLocked(rec_target, 2, TimeUnit.MINUTES);

        DelayedFileTransfer op = new DelayedFileTransfer();
        op.loc_file = sessionHolder.createLocator(rec_file);
        op.upload   = upload;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedFileTransfer that = Reflection.as(o, DelayedFileTransfer.class);
        if (that != null)
        {
            return Objects.equal(loc_file, that.loc_file) && upload == that.upload;
        }

        return false;
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return false;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        DeploymentHostFileRecord rec_file = sessionHolder.fromLocatorOrNull(loc_file);
        return rec_file != null;
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        DeploymentHostFileRecord rec_file = sessionHolder.fromLocator(loc_file);
        return String.format("File %s for '%s'", getFlavor(), rec_file.getPath());
    }

    @Override
    public NextAction process() throws
                                Exception
    {
        NextAction nextAction = shouldSleep(Math.min(retries, 30));
        if (nextAction != null)
        {
            return nextAction;
        }

        DeploymentHostFileRecord rec_file = sessionHolder.fromLocator(loc_file);

        DeploymentHostRecord targetHost = lock_targetHost.get();
        loggerInstance.info("Queueing delayed '%s' %s for host '%s'", rec_file.getPath(), getFlavor(), targetHost.getDisplayName());

        return new NextAction.WaitForActivity(TaskForHostFileTransfer.scheduleTask(sessionHolder, rec_file, upload));
    }

    private String getFlavor()
    {
        return upload ? "upload" : "download";
    }
}
