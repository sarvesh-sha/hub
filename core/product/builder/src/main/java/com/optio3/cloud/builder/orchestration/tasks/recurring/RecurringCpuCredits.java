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
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgentOnHost;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.deploy.CommonDeployer;
import com.optio3.logging.Logger;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringCpuCredits extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringCpuCredits.class);

    enum ConfigVariable implements IConfigVariable
    {
        CustomerSysId("CUSTOMER_SYSID"),
        Customer("CUSTOMER"),
        ServiceSysId("SERVICE_SYSID"),
        Service("SERVICE"),
        Host("HOST"),
        CreditsBalance("CREDITS_BALANCE"),
        CreditsConsumed("CREDITS_CONSUMED"),
        CpuLoad("CPU_LOAD"),
        Timestamp("TIMESTAMP");

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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator        = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_creditsNormal = s_configValidator.newTemplate(RecurringCpuCredits.class, "emails/host/credits_normal.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_creditsLow    = s_configValidator.newTemplate(RecurringCpuCredits.class, "emails/host/credits_low.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_cpuHigh       = s_configValidator.newTemplate(RecurringCpuCredits.class, "emails/host/cpu_high.txt", "${", "}");

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
            // When running in a Developer environment, disable credit checks.
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
                sessionProvider.callWithSessionWithAutoCommit((sessionHolder) -> processService(sessionHolder, svc));

                // Yield processor.
                await(sleep(1, TimeUnit.MILLISECONDS));

                if (wasComputationCancelled())
                {
                    break;
                }
            }
        }

        return wrapAsync(TimeUtils.future(2, TimeUnit.HOURS));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private void processService(SessionHolder sessionHolder,
                                CustomerService svc)
    {
        CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, svc.sysId);

        Duration interval = Duration.of(1, ChronoUnit.HOURS);

        for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, null))
        {
            for (DeploymentRole role : host.roles)
            {
                if (role.cloudBased && host.ensureInstanceType().deployerClass != null)
                {
                    DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, host.sysId);

                    ZonedDateTime rangeEnd = TimeUtils.now()
                                                      .truncatedTo(ChronoUnit.HOURS);

                    ZonedDateTime rangeStart = rangeEnd.minus(24, ChronoUnit.HOURS);

                    try
                    {
                        DeployLogicForAgentOnHost logic = new DeployLogicForAgentOnHost(sessionHolder, rec_host, rec_svc, null);

                        var samples = logic.getMetrics(rangeStart, rangeEnd, interval);
                        var metric  = CollectionUtils.lastElement(samples);
                        if (metric != null)
                        {
                            boolean lowCredits   = metric.creditsRemaining < 200;
                            int     highCpuHours = 0;
                            for (CommonDeployer.Metrics sample : samples)
                            {
                                if (sample.cpuLoad > 70)
                                {
                                    highCpuHours++;
                                }
                            }

                            var metadata = rec_host.getMetadata();

                            if (lowCredits)
                            {
                                if (DeploymentHostRecord.WellKnownMetadata.warningLowCredits.get(metadata) == null)
                                {
                                    sendEmail(sessionHolder,
                                              rec_svc,
                                              rec_host,
                                              metric.creditsRemaining - metric.creditsRemainingSurplus,
                                              metric.creditsConsumed,
                                              metric.cpuLoad,
                                              BuilderApplication.EmailFlavor.Alert,
                                              "Low Cloud Credits detected",
                                              s_template_creditsLow);

                                    DeploymentHostRecord.WellKnownMetadata.warningLowCredits.put(metadata, TimeUtils.now());
                                }
                            }
                            else if (highCpuHours > 4)
                            {
                                if (DeploymentHostRecord.WellKnownMetadata.warningHighCpu.get(metadata) == null)
                                {
                                    sendEmail(sessionHolder,
                                              rec_svc,
                                              rec_host,
                                              metric.creditsRemaining - metric.creditsRemainingSurplus,
                                              metric.creditsConsumed,
                                              metric.cpuLoad,
                                              BuilderApplication.EmailFlavor.Alert,
                                              "High CPU usage detected",
                                              s_template_cpuHigh);

                                    DeploymentHostRecord.WellKnownMetadata.warningHighCpu.put(metadata, TimeUtils.now());
                                }
                            }
                            else
                            {
                                if (DeploymentHostRecord.WellKnownMetadata.warningLowCredits.get(metadata) != null || DeploymentHostRecord.WellKnownMetadata.warningHighCpu.get(metadata) != null)
                                {
                                    sendEmail(sessionHolder,
                                              rec_svc,
                                              rec_host,
                                              metric.creditsRemaining - metric.creditsRemainingSurplus,
                                              metric.creditsConsumed,
                                              metric.cpuLoad,
                                              BuilderApplication.EmailFlavor.Info,
                                              "Cloud Credits and CPU usage back to normal",
                                              s_template_creditsNormal);

                                    DeploymentHostRecord.WellKnownMetadata.warningLowCredits.remove(metadata);
                                    DeploymentHostRecord.WellKnownMetadata.warningHighCpu.remove(metadata);
                                }
                            }

                            rec_host.setMetadata(metadata);
                        }
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.warn("Failed to check metrics for host '%s': %s", rec_host.getDisplayName(), t);
                    }
                }
            }
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           CustomerServiceRecord rec_svc,
                           DeploymentHostRecord rec_host,
                           double creditsBalance,
                           double creditsConsumed,
                           double cpuLoad,
                           BuilderApplication.EmailFlavor flavor,
                           String emailSubject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        BuilderApplication app = sessionHolder.getServiceNonNull(BuilderApplication.class);

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        CustomerRecord rec_cust = rec_svc.getCustomer();

        parameters.setValue(ConfigVariable.CustomerSysId, rec_cust.getSysId());
        parameters.setValue(ConfigVariable.Customer, rec_cust.getName());
        parameters.setValue(ConfigVariable.ServiceSysId, rec_svc.getSysId());
        parameters.setValue(ConfigVariable.Service, rec_svc.getName());
        parameters.setValue(ConfigVariable.Host, rec_host.getDisplayName());
        parameters.setValue(ConfigVariable.CreditsBalance, String.format("%f", Math.floor(creditsBalance)));
        parameters.setValue(ConfigVariable.CreditsConsumed, String.format("%.3f", creditsConsumed));
        parameters.setValue(ConfigVariable.CpuLoad, String.format("%.1f%%", cpuLoad));
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        app.sendEmailNotification(flavor, String.format("%s - %s", rec_svc.getName(), emailSubject), parameters);
    }
}
