/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedAgentBatteryConfiguration;
import com.optio3.cloud.client.deployer.model.DeployerShutdownConfiguration;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import org.apache.commons.lang3.StringUtils;

public class TaskForServiceBatteryThresholds extends BaseBookKeepingTask
{
    public RecordLocator<CustomerServiceRecord> targetService;
    public RecordLocator<DeploymentHostRecord>  targetHost;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        CustomerServiceRecord rec_svc,
                                                        DeploymentHostRecord rec_host) throws
                                                                                       Exception
    {
        return BaseBookKeepingTask.scheduleActivity(sessionHolder, TaskForServiceBatteryThresholds.class, (t) ->
        {
            t.targetService = sessionHolder.createLocator(rec_svc);
            t.targetHost    = sessionHolder.createLocator(rec_host);
        });
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = CustomerServiceRecord.buildContextualLogger(loggerInstance, targetService);
    }

    @Override
    public String getTitle()
    {
        return "Set battery thresholds";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployerShutdownConfiguration shutdownSettings = withLocatorReadonly(targetService, (sessionHolder, rec_svc) ->
        {
            return rec_svc.getBatteryThresholds();
        });

        if (shutdownSettings != null)
        {
            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadDeployments = true;
            settings.loadServices    = true;

            DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(getSessionProvider(), settings);

            CustomerService svc = globalDescriptor.getService(targetService);

            for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, DeploymentRole.gateway))
            {
                if (targetHost == null || StringUtils.equals(targetHost.getIdRaw(), host.sysId))
                {
                    var loc = new RecordLocator<>(DeploymentHostRecord.class, host.sysId);
                    lockedWithLocatorOrNull(loc, 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
                    {
                        if (lock_host != null)
                        {
                            DeploymentHostRecord rec_host = lock_host.get();
                            rec_host.setBatteryThresholds(sessionHolder, shutdownSettings);

                            DelayedAgentBatteryConfiguration.queue(lock_host);
                        }
                    });
                }
            }
        }

        return markAsCompleted();
    }
}
