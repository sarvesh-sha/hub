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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgentOnHost;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.util.BoxingUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

@Optio3RecurringProcessor
public class RecurringHost extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringHost.class);

    enum ConfigVariable implements IConfigVariable
    {
        CustomerSysId("CUSTOMER_SYSID"),
        Customer("CUSTOMER"),
        ServiceSysId("SERVICE_SYSID"),
        Service("SERVICE"),
        Host("HOST"),
        OldIp("OLD_IP"),
        NewIp("NEW_IP"),
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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator   = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_ipChange = s_configValidator.newTemplate(RecurringHost.class, "emails/host/ip_change.txt", "${", "}");

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

            sessionProvider.callWithSessionWithAutoCommit((sessionHolder) -> processService(sessionHolder, svc));

            // Yield processor.
            await(sleep(1, TimeUnit.MILLISECONDS));

            if (wasComputationCancelled())
            {
                break;
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

    private void processService(SessionHolder sessionHolder,
                                CustomerService svc)
    {
        CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, svc.sysId);

        for (DeploymentHost host : svc.findHostsForRole(DeploymentStatus.Ready, null))
        {
            for (DeploymentRole role : host.roles)
            {
                if (role.cloudBased && host.ensureInstanceType().deployerClass != null)
                {
                    DeploymentHostRecord rec_host = sessionHolder.getEntity(DeploymentHostRecord.class, host.sysId);

                    try
                    {
                        DeployLogicForAgentOnHost logic      = new DeployLogicForAgentOnHost(sessionHolder, rec_host, rec_svc, null);
                        String                    ipActual   = logic.getPublicIp();
                        String                    ipExpected = rec_host.getInstanceIp();

                        if (!StringUtils.equals(ipActual, ipExpected))
                        {
                            LoggerInstance.warn("Detected IP change for host '%s': %s -> %s", rec_host.getDisplayName(), ipExpected, ipActual);

                            BuilderConfiguration cfg = sessionHolder.getServiceNonNull(BuilderConfiguration.class);
                            if (cfg.developerSettings.developerMode)
                            {
                                //
                                // When running in a Developer environment, disable DNS updates.
                                //
                            }
                            else
                            {
                                logic.updateDns(ipExpected, ipActual);

                                sendEmail(sessionHolder, rec_svc, rec_host, ipExpected, ipActual, BuilderApplication.EmailFlavor.Alert, "Public IP change detected", s_template_ipChange);
                            }
                        }
                    }
                    catch (Throwable t)
                    {
                        LoggerInstance.warn("Failed to check IP change for host '%s': %s", rec_host.getDisplayName(), t.getMessage());
                    }
                }
            }
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           CustomerServiceRecord rec_svc,
                           DeploymentHostRecord rec_host,
                           String oldIp,
                           String newIp,
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
        parameters.setValue(ConfigVariable.OldIp, BoxingUtils.get(oldIp, "<none>"));
        parameters.setValue(ConfigVariable.NewIp, BoxingUtils.get(newIp, "<none>"));
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        app.sendEmailNotification(flavor, String.format("%s - %s", rec_svc.getName(), emailSubject), parameters);
    }
}
