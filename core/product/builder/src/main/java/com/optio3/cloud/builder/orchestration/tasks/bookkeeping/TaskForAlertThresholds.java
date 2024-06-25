/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import org.apache.commons.lang3.StringUtils;

public class TaskForAlertThresholds extends BaseBookKeepingTask
{
    public RecordLocator<CustomerServiceRecord> targetService;
    public RecordLocator<DeploymentHostRecord>  targetHost;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        CustomerServiceRecord rec_svc,
                                                        DeploymentHostRecord rec_host) throws
                                                                                       Exception
    {
        return BaseBookKeepingTask.scheduleActivity(sessionHolder, TaskForAlertThresholds.class, (t) ->
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
        return "Set alert thresholds";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return targetHost;
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        List<CustomerService.AlertThresholds> alertSettings = withLocatorReadonly(targetService, (sessionHolder, rec_svc) ->
        {
            return rec_svc.getAlertThresholds();
        });

        if (!alertSettings.isEmpty())
        {
            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadDeployments = true;
            settings.loadServices    = true;

            DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(getSessionProvider(), settings);

            DeployLogicForHub logic = DeployLogicForHub.fromLocator(getSessionProvider(), targetService);
            logic.login(false);

            CustomerService svc = globalDescriptor.getService(targetService);

            for (CustomerService.AlertThresholds settingsForRole : alertSettings)
            {
                for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, settingsForRole.role))
                {
                    if (targetHost == null || StringUtils.equals(targetHost.getIdRaw(), host.sysId))
                    {
                        var loc = new RecordLocator<>(DeploymentHostRecord.class, host.sysId);
                        withLocatorOrNull(loc, (sessionHolder, rec_host) ->
                        {
                            if (rec_host != null)
                            {
                                String failure = logic.changeThresholds(rec_host, settingsForRole.warningThreshold, settingsForRole.alertThreshold);
                                if (failure != null)
                                {
                                    loggerInstance.error("Host '%s': %s", host.displayName, failure);
                                }
                            }
                        });
                    }
                }
            }
        }

        return markAsCompleted();
    }
}
