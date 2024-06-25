/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment.delayedOps;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.optio3.cloud.builder.orchestration.tasks.deploy.BaseDeployTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForGatewayCreation;
import com.optio3.cloud.builder.orchestration.tasks.deploy.TaskForReporterCreation;
import com.optio3.cloud.builder.persistence.deployment.DelayedOperation;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.Exceptions;

@JsonTypeName("DelayedTaskCreation")
public class DelayedTaskCreation extends DelayedOperation implements DelayedOperation.IPreprocessState
{
    public DeploymentRole                           role;
    public RecordLocator<RegistryTaggedImageRecord> loc_image;
    public RecordLocator<?>                         loc_context;

    public final List<RecordLocator<DeploymentTaskRecord>> loc_tasksToStop = Lists.newArrayList();
    public       boolean                                   ignoreOfflineDelay;

    //--//

    public static boolean queue(RecordLocked<DeploymentHostRecord> lock_target,
                                DeploymentRole role,
                                RegistryTaggedImageRecord rec_taggedImage,
                                RecordLocator<?> loc_context) throws
                                                              Exception
    {
        SessionHolder sessionHolder = lock_target.getSessionHolder();

        DelayedTaskCreation op = new DelayedTaskCreation();
        op.role        = role;
        op.loc_image   = sessionHolder.createLocator(rec_taggedImage);
        op.loc_context = loc_context;

        return DeploymentHostRecord.queueUnique(lock_target, op);
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        DelayedTaskCreation that = Reflection.as(o, DelayedTaskCreation.class);
        if (that != null)
        {
            return role == that.role && Objects.equal(loc_image, that.loc_image);
        }

        return false;
    }

    @Override
    public boolean mightRequireImagePull()
    {
        return true;
    }

    @Override
    public boolean validate(SessionHolder sessionHolder)
    {
        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocatorOrNull(loc_image);
        return rec_taggedImage != null;
    }

    @Override
    public String getSummary(SessionHolder sessionHolder)
    {
        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
        return String.format("Create task '%s' with image '%s'", role, rec_taggedImage.getTag());
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

        DeploymentHostRecord targetHost = lock_targetHost.get();

        if (!ignoreOfflineDelay && !loc_tasksToStop.isEmpty())
        {
            nextAction = DelayedTaskTermination.shouldDrain(loggerInstance, sessionHolder, targetHost);
            if (nextAction != null)
            {
                return nextAction;
            }
        }

        loggerInstance.info("Queueing delayed task creation for host '%s'", targetHost.getDisplayName());

        RegistryTaggedImageRecord rec_taggedImage = sessionHolder.fromLocator(loc_image);
        RegistryImageRecord       rec_targetImage = rec_taggedImage.getImage();

        List<DeploymentTaskRecord> tasks = targetHost.getTasks();
        for (DeploymentTaskRecord task : tasks)
        {
            if (task.getImageReference() == rec_targetImage && task.getDockerId() != null)
            {
                loggerInstance.warn("Task running image '%s' already present on host '%s', exiting...", rec_taggedImage.getTag(), targetHost.getDisplayName());
                return new NextAction.Done();
            }
        }

        BaseDeployTask.ActivityWithTask activityWithTask;

        switch (role)
        {
            case gateway:
            {
                activityWithTask = TaskForGatewayCreation.scheduleTask(lock_targetHost, loc_tasksToStop, rec_taggedImage);
                break;
            }

            case reporter:
            {
                activityWithTask = TaskForReporterCreation.scheduleTask(lock_targetHost, loc_tasksToStop, rec_taggedImage);
                break;
            }

            default:
                throw Exceptions.newRuntimeException("Unsupported role for DelayedTaskCreation: %s", role);
        }

        return new NextAction.WaitForActivity(sessionHolder.fromLocator(activityWithTask.activity));
    }

    //--//

    @Override
    public void preprocessState(SessionHolder sessionHolder,
                                List<DelayedOperation> ops)
    {
        //
        // Delete all other task creations for the same role.
        //
        for (DelayedTaskCreation oldTask : removeOperations(ops, DelayedTaskCreation.class, (op) -> op.role == role))
        {
            loc_tasksToStop.addAll(oldTask.loc_tasksToStop);
            ignoreOfflineDelay |= oldTask.ignoreOfflineDelay;

            // Delete all other downloads for the same image.
            removeOperations(ops, DelayedImagePull.class, (op) -> op.loc_image.equals(oldTask.loc_image));
        }

        //
        // Merge task terminations.
        //
        List<DelayedTaskTermination> tasksToStop = removeOperations(ops, DelayedTaskTermination.class, (op) ->
        {
            DeploymentTaskRecord rec_task = sessionHolder.fromLocatorOrNull(op.loc_task);
            return rec_task != null && rec_task.getRole() == role;
        });

        for (DelayedTaskTermination taskToStop : tasksToStop)
        {
            loc_tasksToStop.add(taskToStop.loc_task);
            ignoreOfflineDelay |= taskToStop.ignoreOfflineDelay;
        }
    }
}
