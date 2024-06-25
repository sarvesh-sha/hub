/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Sets;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.communication.CrashReport;
import com.optio3.cloud.builder.model.communication.EmailMessage;
import com.optio3.cloud.builder.model.communication.EmailRecipient;
import com.optio3.cloud.builder.model.communication.TextMessage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.AwsHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.logging.Logger;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForNotification extends BaseBookKeepingTask
{
    public static final  Logger LoggerInstance    = new Logger(TaskForNotification.class);
    private static final int    c_rescheduleDelay = 5; // Minutes
    private static final int    c_maxRetry        = 10 * 60; // Minutes

    //--//

    public RecordLocator<CustomerServiceRecord> loc_svc;
    public EmailMessage                         emailContext;
    public boolean                              emailContextDone;
    public TextMessage                          smsContext;
    public boolean                              smsContextDone;
    public CrashReport                          crashReport;
    public boolean                              crashReportDone;

    //--//

    public static EmailRecipient newRecipient(String address,
                                              String name)
    {
        EmailRecipient res = new EmailRecipient();
        res.address = address;
        res.name    = name;
        return res;
    }

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        CustomerServiceRecord rec_svc,
                                                        EmailMessage emailContext,
                                                        TextMessage smsContext,
                                                        CrashReport crashReport) throws
                                                                                 Exception
    {
        if (rec_svc != null)
        {
            var ver = rec_svc.getVertical();
            if (emailContext != null)
            {
                ver.fixupEmail(emailContext);
            }

            if (smsContext != null)
            {
                ver.fixupText(smsContext);
            }
        }

        return scheduleActivity(sessionHolder, TaskForNotification.class, (t) ->
        {
            t.initializeTimeout(c_maxRetry, TimeUnit.MINUTES);

            t.loc_svc      = sessionHolder.createLocator(rec_svc);
            t.emailContext = emailContext;
            t.smsContext   = smsContext;
            t.crashReport  = crashReport;
        });
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = CustomerServiceRecord.buildContextualLogger(loggerInstance, loc_svc);
    }

    @Override
    public String getTitle()
    {
        if (emailContext != null)
        {
            return String.format("Email to '%s' about '%s'", StringUtils.join(CollectionUtils.transformToList(emailContext.to, r -> r.address), ", "), emailContext.subject);
        }

        if (smsContext != null)
        {
            return String.format("SMS to '%s' about '%s'", StringUtils.join(smsContext.phoneNumbers, ", "), smsContext.text);
        }

        if (crashReport != null)
        {
            return String.format("Crash report at %s%s", crashReport.site, crashReport.page);
        }

        return "Unknown notification";
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
        BuilderConfiguration cfg = appConfig;

        if (!cfg.developerSettings.disableEmails)
        {
            if (emailContext != null && !emailContextDone)
            {
                try (AwsHelper aws = AwsHelper.buildCachedWithDirectoryLookup(cfg.credentials, WellKnownSites.optio3DomainName(), null))
                {
                    for (EmailRecipient emailRecipient : emailContext.to)
                    {
                        if (StringUtils.isBlank(emailRecipient.address))
                        {
                            continue;
                        }

                        if (emailRecipient.address.endsWith("@demo.optio3.com"))
                        {
                            // Skip demo accounts
                            continue;
                        }

                        aws.sendTextEmail(emailContext.from.address, emailContext.subject, emailContext.text, emailRecipient.address);
                    }
                }

                emailContextDone = true;
            }

            if (smsContext != null && !smsContextDone)
            {
                try (AwsHelper aws = AwsHelper.buildCachedWithDirectoryLookup(cfg.credentials, WellKnownSites.optio3DomainName(), null))
                {
                    for (String phoneNumber : smsContext.phoneNumbers)
                    {
                        if (StringUtils.isBlank(phoneNumber))
                        {
                            continue;
                        }

                        if (!phoneNumber.startsWith("+"))
                        {
                            phoneNumber = "+1-" + phoneNumber;
                        }

                        aws.sendTextMessage(smsContext.senderId, smsContext.text, phoneNumber);
                    }
                }

                smsContextDone = true;
            }

            if (crashReport != null && !crashReportDone)
            {
                withLocator(loc_svc, (sessionHolder, rec_svc) ->
                {
                    for (var role : rec_svc.getRoleImages())
                    {
                        if (role.role == DeploymentRole.hub)
                        {
                            RegistryTaggedImageRecord rec_image = sessionHolder.fromIdentityOrNull(role.image);
                            if (rec_image != null)
                            {
                                JobRecord rec_job = rec_image.getOwningJob();
                                if (rec_job != null)
                                {
                                    rec_job.sendCrashReport(getSessionProvider(), crashReport);
                                    break;
                                }
                            }
                        }
                    }
                });

                crashReportDone = true;
            }
        }

        return markAsCompleted();
    }
}
