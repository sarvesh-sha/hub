/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.recurring;

import static com.optio3.asyncawait.CompileTime.await;
import static com.optio3.asyncawait.CompileTime.sleep;
import static com.optio3.asyncawait.CompileTime.wasComputationCancelled;
import static com.optio3.asyncawait.CompileTime.wrapAsync;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForHub;
import com.optio3.cloud.builder.model.admin.HubHeapAndThreads;
import com.optio3.cloud.builder.model.admin.HubUniqueStackTrace;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentInstance;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskRestartSingle;
import com.optio3.cloud.client.hub.api.AdminTasksApi;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringHub extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringHub.class);

    enum ConfigVariable implements IConfigVariable
    {
        CustomerSysId("CUSTOMER_SYSID"),
        Customer("CUSTOMER"),
        ServiceSysId("SERVICE_SYSID"),
        Service("SERVICE"),
        Timestamp("TIMESTAMP"),
        Context("CONTEXT");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator       = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_gotHeartbeat = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/hub/got_heartbeat.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_noHeartbeat  = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/hub/no_heartbeat.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_heapNormal   = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/hub/heap_normal.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_heapWarning  = s_configValidator.newTemplate(RecurringAgentCheck.class, "emails/hub/heap_warning.txt", "${", "}");

    //--//

    @Override
    public Duration startupDelay()
    {
        return null;
    }

    @Override
    public CompletableFuture<ZonedDateTime> process(SessionProvider sessionProvider) throws
                                                                                     Exception
    {
        BuilderConfiguration cfg = sessionProvider.getServiceNonNull(BuilderConfiguration.class);
        if (cfg.developerSettings.developerMode)
        {
            //
            // When running in a Developer environment, disable hub checks.
            //
        }
        else
        {
            DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
            settings.loadDeployments = true;
            settings.loadServices    = true;

            DeploymentGlobalDescriptor globalDescriptor = sessionProvider.computeInReadOnlySession((sessionHolder) -> DeploymentGlobalDescriptor.get(sessionHolder, settings));

            for (CustomerService svc : globalDescriptor.services.values())
            {
                if (svc.operationalStatus != DeploymentOperationalStatus.operational)
                {
                    // Only care about production instances.
                    continue;
                }

                List<DeploymentTask> tasks = svc.findTasksForRole(DeploymentStatus.Ready, DeploymentRole.hub, null, true);
                if (tasks.isEmpty())
                {
                    // No running tasks for Hub, skip...
                    continue;
                }

                await(processService(sessionProvider, svc, globalDescriptor));

                // Yield processor.
                await(sleep(1, TimeUnit.MILLISECONDS));

                if (wasComputationCancelled())
                {
                    break;
                }
            }
        }

        return wrapAsync(TimeUtils.future(15, TimeUnit.MINUTES));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private CompletableFuture<Void> processService(SessionProvider sessionProvider,
                                                   CustomerService svc,
                                                   DeploymentGlobalDescriptor globalDescriptor) throws
                                                                                                Exception
    {
        DeployLogicForHub logic = sessionProvider.computeInReadOnlySession(sessionHolder ->
                                                                           {
                                                                               CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, svc.sysId);
                                                                               if (rec_svc.getCurrentActivityIfNotDone() != null)
                                                                               {
                                                                                   // Service is busy, we'll try again later.
                                                                                   return null;
                                                                               }

                                                                               return DeployLogicForHub.fromRecord(sessionHolder, rec_svc);
                                                                           });

        if (logic != null)
        {
            LoggerInstance.debug("Trying to talk to Hub for '%s'...", logic.service_name);

            List<String> tasksToRestart = Lists.newArrayList();

            try
            {
                logic.login(false);

                LoggerInstance.debug("Logged in for '%s'...", logic.service_name);

                com.optio3.cloud.client.hub.api.UsersApi userProxy = logic.createHubProxy(com.optio3.cloud.client.hub.api.UsersApi.class);
                userProxy.getAll();

                //--//

                com.optio3.cloud.client.hub.api.AdminTasksApi adminProxy = logic.createHubProxy(com.optio3.cloud.client.hub.api.AdminTasksApi.class);

                //--//

                HubHeapAndThreads heapAndThreads = analyzeHeapStatus(adminProxy);
                if (heapAndThreads != null)
                {
                    heapAndThreads.heapWarning = heapAndThreads.isFreeMemoryBelowThreshold(75, 400_000_000);

                    List<HubHeapAndThreads> lst = CustomerServiceRecord.WellKnownMetadata.hubHeapStatus.get(logic.service_metadata);
                    lst.removeIf((item) -> !TimeUtils.wasUpdatedRecently(item.timestamp, 24, TimeUnit.HOURS));
                    HubHeapAndThreads heapAndThreadsPrevious = CollectionUtils.lastElement(lst);
                    lst.add(heapAndThreads);

                    CustomerServiceRecord.WellKnownMetadata.hubHeapStatus.put(logic.service_metadata, lst);

                    if (heapAndThreads.heapWarning)
                    {
                        if (heapAndThreadsPrevious != null && !heapAndThreadsPrevious.heapWarning)
                        {
                            sendEmail(logic, BuilderApplication.EmailFlavor.Warning, "Hub Heap warning", heapAndThreads.dump(), s_template_heapWarning);
                        }
                    }
                    else
                    {
                        if (heapAndThreadsPrevious != null && heapAndThreadsPrevious.heapWarning)
                        {
                            sendEmail(logic, BuilderApplication.EmailFlavor.Info, "Hub Heap back to normal", null, s_template_heapNormal);
                        }
                    }
                }

                //--//

                Map<String, CustomerServiceRecord.RestartHistory> taskRestartHistory = CustomerServiceRecord.WellKnownMetadata.taskRestartHistory.get(logic.service_metadata);

                com.optio3.cloud.client.hub.api.GatewaysApi gatewayProxy = logic.createHubProxy(com.optio3.cloud.client.hub.api.GatewaysApi.class);

                for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, DeploymentRole.gateway))
                {
                    com.optio3.cloud.client.hub.model.GatewayAsset asset = gatewayProxy.lookup(host.hostId);
                    if (asset != null)
                    {
                        //
                        // Azure Edge don't run an agent, so use the Hub's last updated timestamp as heartbeat.
                        //
                        DeploymentHostStatusDescriptor desc = new DeploymentHostStatusDescriptor(host, false);
                        if (desc.instanceType == DeploymentInstance.AZURE_EDGE)
                        {
                            sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                                          {
                                                                              DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, host.sysId);
                                                                              rec_host.setLastHeartbeat(asset.lastUpdatedDate);
                                                                          });
                        }

                        if (TimeUtils.wasUpdatedRecently(host.lastHeartbeat, 30, TimeUnit.MINUTES))
                        {
                            //
                            // Host is online, see if a task is stuck.
                            //

                            if (!host.delayedOperations && !TimeUtils.wasUpdatedRecently(asset.lastUpdatedDate, 3, TimeUnit.HOURS))
                            {
                                //
                                // Found a task that is not talking to the Hub, even if the agent on the same host is talking to the Builder.
                                // Queue a restart.
                                //
                                List<DeploymentTask> tasks = host.getTasksForRole(DeploymentStatus.Ready, DeploymentRole.gateway, null, true);
                                if (tasks.size() == 1)
                                {
                                    DeploymentTask task = tasks.get(0);

                                    CustomerServiceRecord.RestartHistory history = taskRestartHistory.get(host.hostId);
                                    if (history == null)
                                    {
                                        history             = new CustomerServiceRecord.RestartHistory();
                                        history.lastAttempt = TimeUtils.now(); // Start monitoring from now.
                                        taskRestartHistory.put(host.hostId, history);
                                    }

                                    if (history.shouldProceed())
                                    {
                                        history.lastAttempt = TimeUtils.now();

                                        tasksToRestart.add(task.sysId);
                                    }
                                }
                            }
                            else
                            {
                                taskRestartHistory.remove(host.hostId);
                            }
                        }
                        else if (!TimeUtils.wasUpdatedRecently(host.lastHeartbeat, 1, TimeUnit.HOURS))
                        {
                            //
                            // Host is offline, reset restart history.
                            //

                            taskRestartHistory.remove(host.hostId);
                        }
                    }
                }

                taskRestartHistory.values()
                                  .removeIf(CustomerServiceRecord.RestartHistory::shouldRemoveStale);

                CustomerServiceRecord.WellKnownMetadata.taskRestartHistory.put(logic.service_metadata, taskRestartHistory);

                //--//

                CustomerServiceRecord.WellKnownMetadata.hubFailure.remove(logic.service_metadata);

                if (CustomerServiceRecord.WellKnownMetadata.hubFailureNotified.get(logic.service_metadata))
                {
                    CustomerServiceRecord.WellKnownMetadata.hubFailureNotified.remove(logic.service_metadata);

                    sendEmail(logic, BuilderApplication.EmailFlavor.Info, "Hub request success", null, s_template_gotHeartbeat);
                }

                LoggerInstance.debug("Talked to Hub for '%s'", logic.service_name);
            }
            catch (Throwable t)
            {
                ZonedDateTime failureTimestamp = CustomerServiceRecord.WellKnownMetadata.hubFailure.get(logic.service_metadata);
                if (failureTimestamp == null)
                {
                    LoggerInstance.info("Failed to talk to Hub for '%s': %s", logic.service_name, t);

                    failureTimestamp = TimeUtils.now();
                    CustomerServiceRecord.WellKnownMetadata.hubFailure.put(logic.service_metadata, failureTimestamp);
                }
                else
                {
                    LoggerInstance.debug("Failed to talk to Hub for '%s': %s", logic.service_name, t);
                }

                if (!TimeUtils.wasUpdatedRecently(failureTimestamp, 45, TimeUnit.MINUTES))
                {
                    CustomerServiceRecord.WellKnownMetadata.hubFailureNotified.put(logic.service_metadata, true);
                    sendEmail(logic, BuilderApplication.EmailFlavor.Warning, "Hub request failure", null, s_template_noHeartbeat);
                }
            }

            sessionProvider.callWithSessionWithAutoCommit(sessionHolder ->
                                                          {
                                                              CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, svc.sysId);
                                                              rec_svc.setMetadata(logic.service_metadata);
                                                          });

            for (String sysId_task : tasksToRestart)
            {
                try (SessionHolder subSessionHolder = sessionProvider.newSessionWithTransaction())
                {
                    DeploymentTaskRecord rec_task = subSessionHolder.getEntityOrNull(DeploymentTaskRecord.class, sysId_task);
                    if (rec_task != null)
                    {
                        DeploymentHostRecord rec_host = rec_task.getDeployment();

                        if (rec_host.getOperationalStatus() == DeploymentOperationalStatus.operational)
                        {
                            RecordLocked<DeploymentHostRecord> lock_target = subSessionHolder.optimisticallyUpgradeToLocked(rec_host, 2, TimeUnit.MINUTES);

                            if (DelayedTaskRestartSingle.queue(lock_target, rec_task, true))
                            {
                                subSessionHolder.commit();

                                LoggerInstance.warn("Detected task '%s' on host '%s' not talking to Hub, restarting...", rec_task.getName(), rec_host.getDisplayName());
                            }
                        }
                    }
                }
                catch (Throwable t)
                {
                    // Ignore failures, we'll retry on the next checkin.
                }
            }
        }

        return AsyncRuntime.NullResult;
    }

    private HubHeapAndThreads analyzeHeapStatus(AdminTasksApi adminProxy)
    {
        try
        {
            HubUniqueStackTrace currentStack = null;

            var res = new HubHeapAndThreads();
            res.timestamp = TimeUtils.now();

            String threadsDump = adminProxy.dumpThreads();
            for (String line : StringUtils.split(threadsDump, '\n'))
            {
                String v = matchStart(line, "Memory, Max  :");
                if (v != null)
                {
                    res.memoryMax = parseSize(v);
                    continue;
                }

                v = matchStart(line, "Memory, Total:");
                if (v != null)
                {
                    res.memoryTotal = parseSize(v);
                    continue;
                }

                v = matchStart(line, "Memory, Free :");
                if (v != null)
                {
                    res.memoryFree = parseSize(v);
                    continue;
                }

                v = matchStart(line, "Memory, Used :");
                if (v != null)
                {
                    res.memoryUsed = parseSize(v);
                    continue;
                }

                if (line.startsWith("Found "))
                {
                    currentStack = new HubUniqueStackTrace();
                    res.uniqueStackTraces.add(currentStack);
                    continue;
                }

                if (currentStack != null)
                {
                    v = line.trim();
                    if (v.length() == 0)
                    {
                        currentStack = null;
                        continue;
                    }

                    if (v.startsWith("'"))
                    {
                        currentStack.threads.add(v);
                        continue;
                    }

                    if (v.startsWith("("))
                    {
                        currentStack.frames.add(v);
                        continue;
                    }
                }
            }

            int numTraces = res.uniqueStackTraces.size();
            if (numTraces > 1)
            {
                // The last trace is for the thread generating the dump, skip.
                res.uniqueStackTraces.remove(numTraces - 1);
            }

            res.uniqueStackTraces.remove(currentStack);

            return res;
        }
        catch (Throwable t)
        {
            return null;
        }
    }

    private String matchStart(String line,
                              String search)
    {
        if (line.startsWith(search))
        {
            return line.substring(search.length());
        }

        return null;
    }

    private long parseSize(String v)
    {
        return Long.parseLong(StringUtils.replace(v, ",", "")
                                         .trim());
    }

    private void sendEmail(DeployLogicForHub logic,
                           BuilderApplication.EmailFlavor flavor,
                           String emailSubject,
                           String context,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        parameters.setValue(ConfigVariable.CustomerSysId, logic.loc_customer.getIdRaw());
        parameters.setValue(ConfigVariable.Customer, logic.customer_name);
        parameters.setValue(ConfigVariable.ServiceSysId, logic.loc_service.getIdRaw());
        parameters.setValue(ConfigVariable.Service, logic.service_name);
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());
        parameters.setValue(ConfigVariable.Context, context);

        logic.app.sendEmailNotification(flavor, String.format("%s - %s", logic.service_name, emailSubject), parameters);
    }
}
