/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.BatchToken;
import com.optio3.cloud.persistence.RecordLocator;

public abstract class BaseDeployTaskCreation extends BaseDeployTask
{
    public final List<RecordLocator<DeploymentTaskRecord>> loc_tasksToStop = Lists.newArrayList();
    public       RecordLocator<RegistryTaggedImageRecord>  loc_image;
    public       boolean                                   configureThroughEnvVar;
    public       Map<String, String>                       containerLabels;
    public       String                                    containerId;
    public       BatchToken                                batchToken;
    public       int                                       batchOffset;
    public       int                                       nextCheckin;

    protected CompletableFuture<Void> terminateOldTasks() throws
                                                          Exception
    {
        DeployLogicForAgent agentLogic      = getLogicForAgent();
        String              hostDisplayName = getHostDisplayName();

        for (RecordLocator<DeploymentTaskRecord> loc_task : loc_tasksToStop)
        {
            String containerId = withLocatorReadonlyOrNull(loc_task, (sessionHolder, rec_task) -> rec_task != null ? rec_task.getDockerId() : null);

            if (containerId != null)
            {
                loggerInstance.info("Removing Container '%s' on host '%s'", containerId, hostDisplayName);

                await(removeContainerAndVolumes(agentLogic, containerId));

                withLocatorOrNull(loc_task, (sessionHolder, rec_task) ->
                {
                    if (rec_task != null)
                    {
                        rec_task.setDockerId(null);
                    }
                });

                loggerInstance.info("Removed Container '%s' from host '%s'", containerId, hostDisplayName);
            }
        }

        return wrapAsync(null);
    }
}
