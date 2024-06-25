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

import java.net.URL;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

import com.optio3.cloud.annotation.Optio3RecurringProcessor;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.logic.BackgroundActivityScheduler;
import com.optio3.cloud.logic.RecurringActivityHandler;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Logger;
import com.optio3.serialization.Reflection;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

@Optio3RecurringProcessor
public class RecurringCertificateCheck extends RecurringActivityHandler
{
    public static final Logger LoggerInstance = BackgroundActivityScheduler.LoggerInstance.createSubLogger(RecurringCertificateCheck.class);

    enum ConfigVariable implements IConfigVariable
    {
        CustomerSysId("CUSTOMER_SYSID"),
        Customer("CUSTOMER"),
        ServiceSysId("SERVICE_SYSID"),
        Service("SERVICE"),
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

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator          = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_goodCertificate = s_configValidator.newTemplate(RecurringAgentCheck.class,
                                                                                                                              "emails/certificate/good_certificate.txt",
                                                                                                                              "${",
                                                                                                                              "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_badCertificate  = s_configValidator.newTemplate(RecurringAgentCheck.class,
                                                                                                                              "emails/certificate/bad_certificate.txt",
                                                                                                                              "${",
                                                                                                                              "}");

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

        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadDeployments = true;
        settings.loadServices    = true;

        DeploymentGlobalDescriptor globalDescriptor = sessionProvider.computeInReadOnlySession((sessionHolder) -> DeploymentGlobalDescriptor.get(sessionHolder, settings));

        for (CustomerService svc : globalDescriptor.services.values())
        {
            boolean shouldCheck = false;

            shouldCheck |= svc.hasAnyTasksForRole(DeploymentStatus.Ready, DeploymentRole.hub, null, true);
            shouldCheck |= svc.hasAnyTasksForRole(DeploymentStatus.Ready, DeploymentRole.reporter, null, true);
            shouldCheck |= svc.hasAnyTasksForRole(DeploymentStatus.Ready, DeploymentRole.database, null, true);

            if (!shouldCheck)
            {
                // No running tasks, skip...
                continue;
            }

            processService(sessionProvider, svc);

            // Yield processor.
            await(sleep(1, TimeUnit.MILLISECONDS));

            if (wasComputationCancelled())
            {
                break;
            }
        }

        return wrapAsync(TimeUtils.future(8, TimeUnit.HOURS));
    }

    @Override
    public void shutdown()
    {
        // Nothing to do.
    }

    //--//

    private void processService(SessionProvider sessionProvider,
                                CustomerService svc) throws
                                                     Exception
    {
        try (SessionHolder sessionHolder = sessionProvider.newSessionWithTransaction())
        {
            CustomerServiceRecord rec_svc = sessionHolder.getEntity(CustomerServiceRecord.class, svc.sysId);
            if (rec_svc.getCurrentActivityIfNotDone() != null)
            {
                // Service is busy, we'll try again later.
                return;
            }

            String url = rec_svc.getUrl();
            if (url == null)
            {
                return;
            }

            Boolean valid = checkCertificate(new URL(url));
            if (valid != null)
            {
                if (valid)
                {
                    rec_svc.putMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailure, null);

                    if (rec_svc.getMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailureNotified))
                    {
                        rec_svc.putMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailureNotified, null);

                        sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Info, "Certificate validation success", s_template_goodCertificate);
                    }
                }
                else
                {
                    ZonedDateTime failureCheck = rec_svc.getMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailure);
                    if (!TimeUtils.wasUpdatedRecently(failureCheck, 2, TimeUnit.DAYS))
                    {
                        rec_svc.putMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailure, TimeUtils.now());
                        rec_svc.putMetadata(CustomerServiceRecord.WellKnownMetadata.certificateFailureNotified, true);

                        sendEmail(sessionHolder, rec_svc, BuilderApplication.EmailFlavor.Warning, "Certificate validation failed", s_template_badCertificate);
                    }
                }

                sessionHolder.commit();
            }
        }
    }

    private Boolean checkCertificate(URL site)
    {
        LoggerInstance.debug("Trying to talk to %s...", site);

        try
        {
            HttpsURLConnection conn = (HttpsURLConnection) site.openConnection();
            conn.connect();
            Certificate[] certs = conn.getServerCertificates();
            for (Certificate cert : certs)
            {
                X509Certificate x = Reflection.as(cert, X509Certificate.class);
                if (x != null)
                {
                    try
                    {
                        ZonedDateTime dateTime = TimeUtils.now()
                                                          .plus(30, ChronoUnit.DAYS);

                        x.checkValidity(Date.from(dateTime.toInstant()));
                    }
                    catch (CertificateExpiredException | CertificateNotYetValidException e)
                    {
                        LoggerInstance.debug("Failed validation with %s", e);
                        return false;
                    }
                }
            }

            return true;
        }
        catch (Throwable e)
        {
            for (Throwable ePtr = e; ePtr != null; ePtr = ePtr.getCause())
            {
                CertificateException e2 = Reflection.as(ePtr, CertificateException.class);
                if (e2 != null)
                {
                    LoggerInstance.debug("Failed validation with %s", e2);
                    return false;
                }
            }

            // If it's not a certificate issue, ignore failure.
            LoggerInstance.debug("Failed validation with %s", e);
            return null;
        }
    }

    private void sendEmail(SessionHolder sessionHolder,
                           CustomerServiceRecord rec_svc,
                           BuilderApplication.EmailFlavor flavor,
                           String emailSubject,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        CustomerRecord rec_cust = rec_svc.getCustomer();

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        parameters.setValue(ConfigVariable.CustomerSysId, rec_cust.getSysId());
        parameters.setValue(ConfigVariable.Customer, rec_cust.getName());
        parameters.setValue(ConfigVariable.ServiceSysId, rec_svc.getSysId());
        parameters.setValue(ConfigVariable.Service, rec_svc.getName());
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        BuilderApplication app = sessionHolder.getServiceNonNull(BuilderApplication.class);
        app.sendEmailNotification(flavor, String.format("%s - %s", rec_svc.getName(), emailSubject), parameters);
    }
}
