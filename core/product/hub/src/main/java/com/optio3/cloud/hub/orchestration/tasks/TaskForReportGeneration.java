/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.NewCookie;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.optio3.cloud.ProxyFactory;
import com.optio3.cloud.authentication.WellKnownRole;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.hub.model.DeliveryOptions;
import com.optio3.cloud.hub.model.report.ReportReason;
import com.optio3.cloud.hub.model.report.ReportSchedulingOptions;
import com.optio3.cloud.hub.model.report.ReportStatus;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageReportRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.NetworkHelper;
import com.optio3.util.BoxingUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

public class TaskForReportGeneration extends AbstractHubActivityHandler implements BackgroundActivityHandler.ICleanupOnFailureWithSession
{
    enum State
    {
        QueueGeneration,
        WaitForReportGeneration,
        NotifyUser,
    }

    enum ConfigVariable implements IConfigVariable
    {
        SiteUrl("SITE_URL"),
        ReportUrl("REPORT_URL"),
        ReportName("REPORT_NAME"),
        ReportId("REPORT_ID"),
        ReportDefinitionId("REPORT_DEFINITION_ID"),
        ReportTime("REPORT_TIME"),
        ReportUser("REPORT_USER"),
        ReportEmail("REPORT_EMAIL");

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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_reportGenerated = s_configValidator.newTemplate(TaskForReportGeneration.class,
                                                                                                                              "emails/reports/report_generated.txt",
                                                                                                                              "${",
                                                                                                                              "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_reportFailed    = s_configValidator.newTemplate(TaskForReportGeneration.class,
                                                                                                                              "emails/reports/report_failed.txt",
                                                                                                                              "${",
                                                                                                                              "}");

    //--//

    public RecordLocator<ReportRecord>                     loc_report;
    public com.optio3.client.reporter.model.ReportIdentity identity;
    public ZonedDateTime                                   waitTimeout;
    public String                                          reportTitle;
    public ReportReason                                    reason;
    public int                                             generationRetries;
    public ZonedDateTime                                   scheduledTime;

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder,
                                                        ReportRecord rec_report,
                                                        ZonedDateTime scheduledTime) throws
                                                                                     Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForReportGeneration.class, (newHandler) ->
        {
            ReportDefinitionRecord rec_def = rec_report.getReportDefinition();

            newHandler.reportTitle       = String.format("Generate report '%s' (%s)", rec_def.getTitle(), rec_report.getSysId());
            newHandler.reason            = rec_report.getReason();
            newHandler.loc_report        = sessionHolder.createLocator(rec_report);
            newHandler.scheduledTime     = scheduledTime;
            newHandler.generationRetries = 5;
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return reportTitle;
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_report;
    }

    @BackgroundActivityMethod(stateClass = State.class, initial = true)
    public CompletableFuture<Void> state_QueueGeneration() throws
                                                           Exception
    {
        app.suspendResultStagingSpooler(5, TimeUnit.MINUTES);

        var createRequest = new com.optio3.client.reporter.model.ReportCreateRequest();
        createRequest.reportToken        = loc_report.getIdRaw();
        createRequest.baseUrl            = getBaseUrl();
        createRequest.maxWaitTimeSeconds = 5 * 60;
        createRequest.landscape          = false;

        ProxyFactory                              proxyFactory = new ProxyFactory();
        com.optio3.client.reporter.api.ReportsApi reportsApi   = proxyFactory.createProxy(appConfig.reporterConnectionUrl, com.optio3.client.reporter.api.ReportsApi.class);

        // Cleanup report definition before queueing generation
        withLocator(loc_report, (sessionHolder, rec_report) ->
        {
            ReportDefinitionVersionRecord rec_def = rec_report.getReportDefinitionVersion();

            rec_def.cleanUp(sessionHolder);

            rec_report.setStatus(ReportStatus.Queued);

            createRequest.sessionToken = createSessionToken(rec_def);
        });

        withLocatorReadonly(loc_report, (sessionHolder, reportRecord) ->
        {
            var rec_version = reportRecord.getReportDefinitionVersion();
            var config      = rec_version.getDetails().reportConfiguration;
            createRequest.reportTime = getReportTime(rec_version);
            createRequest.pdfFormat  = config.pdfFormat;
            createRequest.landscape  = config.landscape;
        });

        try
        {
            identity = reportsApi.create(createRequest);
        }
        catch (Throwable t)
        {
            loggerInstance.warn("%s Failed to start\n%s", reportTitle, t);

            withLocator(loc_report, (sessionHolder, rec_report) ->
            {
                rec_report.setStatus(ReportStatus.Failed);
            });

            return continueAtState(State.NotifyUser);
        }

        waitTimeout = null;

        return continueAtState(State.WaitForReportGeneration);
    }

    @BackgroundActivityMethod(stateClass = State.class)
    public CompletableFuture<Void> state_WaitForReportGeneration() throws
                                                                   Exception
    {
        app.suspendResultStagingSpooler(5, TimeUnit.MINUTES);

        boolean failed;

        try
        {
            ProxyFactory proxyFactory = new ProxyFactory();
            var          reportsApi   = proxyFactory.createProxy(appConfig.reporterConnectionUrl, com.optio3.client.reporter.api.ReportsApi.class);

            var status = reportsApi.getStatus(identity.reportId);

            switch (BoxingUtils.get(status.status, com.optio3.client.reporter.model.ReportStatus.UNKNOWN))
            {
                case QUEUED:
                case PROCESSING:
                    return rescheduleDelayed(1, TimeUnit.SECONDS);

                case SUCCESS:
                {
                    var    report        = reportsApi.download(identity.reportId);
                    byte[] base64Decoded = DatatypeConverter.parseBase64Binary(report.bytes);

                    withLocator(loc_report, (sessionHolder, rec_report) ->
                    {
                        rec_report.setBytes(base64Decoded);
                        rec_report.setStatus(ReportStatus.Finished);
                    });

                    failed = false;
                }
                break;

                default:
                    failed = true;
                    break;
            }
        }
        catch (NotFoundException ex)
        {
            // Reporter lost track of our ID, start over
            failed = true;
        }
        catch (Throwable t)
        {
            if (waitTimeout == null)
            {
                waitTimeout = TimeUtils.future(10, TimeUnit.MINUTES);
            }

            if (!TimeUtils.isTimeoutExpired(waitTimeout))
            {
                return rescheduleDelayed(30, TimeUnit.SECONDS);
            }

            failed = true;
        }

        if (failed)
        {
            if (reason != ReportReason.OnDemand)
            {
                if (generationRetries-- > 0)
                {
                    loggerInstance.warn("%s failed (%d retries left)", reportTitle, generationRetries);
                    return continueAtState(State.QueueGeneration);
                }
            }

            withLocator(loc_report, (sessionHolder, rec_report) ->
            {
                rec_report.setStatus(ReportStatus.Failed);
            });
        }

        return continueAtState(State.NotifyUser);
    }

    @BackgroundActivityMethod(stateClass = State.class, needsSession = true)
    public void state_NotifyUser(SessionHolder sessionHolder) throws
                                                              Exception
    {
        ReportRecord           reportRecord   = getReportRecord(sessionHolder);
        ReportDefinitionRecord rec_definition = reportRecord.getReportDefinition();

        // Don't send notifications/emails for report previews or OnDemand reports
        if (reason != ReportReason.OnDemand && rec_definition.getAutoDelete() == null)
        {
            sendNotifications(sessionHolder, reportRecord, rec_definition);
        }

        markAsCompleted();
    }

    private ReportRecord getReportRecord(SessionHolder sessionHolder)
    {
        if (loc_report != null)
        {
            return sessionHolder.fromLocatorOrNull(loc_report);
        }
        else
        {
            throw new IllegalStateException("Unable to find report record.");
        }
    }

    @JsonIgnore
    private String getBaseUrl() throws
                                IOException
    {
        String url = appConfig.cloudConnectionUrl;

        if (appConfig.localReportConnectionUrl != null)
        {
            url = appConfig.localReportConnectionUrl;
        }

        if (url.contains("localhost"))
        {
            for (NetworkHelper.InterfaceAddressDetails itfDetails : NetworkHelper.listNetworkAddresses(false, false, false, false, null))
            {
                url = url.replace("localhost", itfDetails.localAddress.getHostAddress());
                break;
            }
        }
        return url;
    }

    private String createSessionToken(ReportDefinitionVersionRecord rec_def)
    {
        CookiePrincipal principal;

        UserRecord rec_user = rec_def.getDefinition()
                                     .getUser();

        if (rec_user != null)
        {
            principal = app.buildPrincipal(rec_user.getEmailAddress());
        }
        else
        {
            // Generate a fake principal for report engine to use.
            principal = app.buildPrincipal("reporter@local");
            principal.setEmbeddedRolesEx(WellKnownRole.Machine);
        }

        NewCookie sessionToken = app.generateCookie(principal);

        return sessionToken.getValue();
    }

    private void sendNotifications(SessionHolder sessionHolder,
                                   ReportRecord rec_report,
                                   ReportDefinitionRecord rec_definition)
    {
        UserRecord                    rec_user    = rec_definition.getUser();
        ReportDefinitionVersionRecord rec_version = rec_report.getReportDefinitionVersion();
        ReportSchedulingOptions       schedule    = rec_version.getDetails().schedule;
        boolean                       success     = true;

        ConfigVariables<ConfigVariable> parameters;

        switch (rec_report.getStatus())
        {
            case Finished:
                parameters = s_template_reportGenerated.allocate();
                break;

            default:
                success = false;
                parameters = s_template_reportFailed.allocate();
                break;
        }

        parameters.setValue(ConfigVariable.SiteUrl, appConfig.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.ReportUrl, rec_report.generateDownloadUrl(rec_definition.getTitle(), rec_report.getCreatedOn(), appConfig));
        parameters.setValue(ConfigVariable.ReportName, rec_definition.getTitle());
        parameters.setValue(ConfigVariable.ReportUser, String.format("%s %s", rec_user.getFirstName(), rec_user.getLastName()));
        parameters.setValue(ConfigVariable.ReportEmail, rec_user.getEmailAddress());
        parameters.setValue(ConfigVariable.ReportId, rec_report.getSysId());
        parameters.setValue(ConfigVariable.ReportDefinitionId, rec_definition.getSysId());

        parameters.setValue(ConfigVariable.ReportTime, getReportTime(rec_version));

        if (schedule != null && schedule.deliveryOptions != null)
        {
            DeliveryOptions.Resolver resolver      = new DeliveryOptions.Resolver(sessionHolder.getSessionProvider());
            Set<String>              resolvedUsers = resolver.resolve(schedule.deliveryOptions);
            List<UserRecord>         users         = resolver.collectUsers(sessionHolder, resolvedUsers);

            for (UserRecord user : users)
            {
                sendUserMessage(sessionHolder, user, rec_report, rec_definition, parameters, success);
            }
        }
        else
        {
            sendUserMessage(sessionHolder, rec_user, rec_report, rec_definition, parameters, success);
        }
    }

    private void sendUserMessage(SessionHolder sessionHolder,
                                 UserRecord rec_user,
                                 ReportRecord rec_report,
                                 ReportDefinitionRecord rec_definition,
                                 ConfigVariables<ConfigVariable> parameters,
                                 boolean success)
    {
        if (rec_user == null)
        {
            return;
        }

        String title   = rec_definition.getTitle();
        String subject = success ? String.format("%s - Report Ready", title) : String.format("%s - Report Failed", title);
        String body    = success ? String.format("Report '%s' was created.", title) : String.format("Failed to generate report '%s'", title);

        UserMessageReportRecord rec_message = UserMessageReportRecord.newInstance(rec_user, rec_report);
        rec_message.setSubject(subject);
        rec_message.setBody(body);
        rec_message.persist(sessionHolder);

        app.sendEmailNotification(sessionHolder, false, rec_user.getEmailAddress(), subject, false, parameters);
    }

    private String getReportTime(ReportDefinitionVersionRecord rec_version)
    {
        ZonedDateTime           reportTime = scheduledTime;
        ReportSchedulingOptions schedule   = rec_version.getDetails().schedule;

        if (schedule != null && schedule.schedule != null)
        {
            reportTime = reportTime.withZoneSameInstant(ZoneId.of(schedule.schedule.zoneDesired));
        }

        return reportTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG));
    }

    @Override
    public void cleanupOnFailure(SessionHolder sessionHolder,
                                 Throwable t) throws
                                              Exception
    {
        ReportRecord reportRecord = getReportRecord(sessionHolder);
        if (reportRecord != null && reportRecord.getStatus() != com.optio3.cloud.hub.model.report.ReportStatus.Finished)
        {
            reportRecord.setStatus(com.optio3.cloud.hub.model.report.ReportStatus.Failed);
        }
    }
}
