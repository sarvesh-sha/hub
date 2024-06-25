/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.ProxyFactory;
import com.optio3.cloud.client.builder.api.CustomerCommunicationsApi;
import com.optio3.cloud.client.builder.model.CrashReport;
import com.optio3.cloud.client.builder.model.DeviceDetails;
import com.optio3.cloud.client.builder.model.EmailMessage;
import com.optio3.cloud.client.builder.model.EmailRecipient;
import com.optio3.cloud.client.builder.model.TextMessage;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForNotification extends AbstractHubActivityHandler
{
    private static final int c_maxRetry = 10 * 60; // Minutes

    //--//

    public EmailMessage  emailContext;
    public boolean       emailDone;
    public TextMessage   smsContext;
    public boolean       smsDone;
    public DeviceDetails deviceDetails;
    public boolean       deviceDetailsDone;
    public CrashReport   crashReport;
    public boolean       crashReportDone;

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
                                                        EmailMessage emailContext,
                                                        TextMessage smsContext,
                                                        DeviceDetails deviceDetails,
                                                        CrashReport crashReport) throws
                                                                                 Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForNotification.class, (t) ->
        {
            t.initializeTimeout(c_maxRetry, TimeUnit.MINUTES);

            t.emailContext  = emailContext;
            t.smsContext    = smsContext;
            t.deviceDetails = deviceDetails;
            t.crashReport   = crashReport;
        });
    }

    //--//

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

        if (deviceDetails != null)
        {
            return String.format("Forwarding details for new device: %s / %s", deviceDetails.productId, deviceDetails.hostId);
        }

        if (crashReport != null)
        {
            return String.format("Crash report at %s", crashReport.page);
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
        HubConfiguration cfg = appConfig;

        CustomerCommunicationsApi communicatorApi = getCommunicator(cfg);
        if (communicatorApi != null)
        {
            if (!emailDone && emailContext != null)
            {
                communicatorApi.sendEmail(cfg.communicatorId, cfg.communicatorAccessKey, emailContext);
                emailDone = true;
            }

            if (!smsDone && smsContext != null)
            {
                communicatorApi.sendText(cfg.communicatorId, cfg.communicatorAccessKey, smsContext);
                smsDone = true;
            }

            if (!deviceDetailsDone && deviceDetails != null)
            {
                communicatorApi.registerDevice(cfg.communicatorId, cfg.communicatorAccessKey, deviceDetails);
                deviceDetailsDone = true;
            }

            if (!crashReportDone && crashReport != null)
            {
                communicatorApi.reportCrash(cfg.communicatorId, cfg.communicatorAccessKey, crashReport);
                crashReportDone = true;
            }
        }

        return markAsCompleted();
    }

    //--//

    private CustomerCommunicationsApi getCommunicator(HubConfiguration cfg)
    {
        if (cfg.communicatorConnectionUrl != null && cfg.communicatorId != null && cfg.communicatorAccessKey != null)
        {
            ProxyFactory proxyFactory = new ProxyFactory();

            return proxyFactory.createProxy(cfg.communicatorConnectionUrl + "/api/v1", CustomerCommunicationsApi.class);
        }

        return null;
    }
}
