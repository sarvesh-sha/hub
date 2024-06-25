/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.logic.alerts.AlertExecutionSpooler;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageAlertRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

@JsonTypeName("AlertEngineValueEmail")
public class AlertEngineValueEmail extends AlertEngineValueAction
{
    enum ConfigVariable implements IConfigVariable
    {
        SiteUrl("SITE_URL"),
        AlertSysId("ALERT_SYSID"),
        AlertBody("ALERT_BODY"),
        Timestamp("TIME");

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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_opened   = s_configValidator.newTemplate(AlertExecutionSpooler.class, "emails/alerts/opened.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_resolved = s_configValidator.newTemplate(AlertExecutionSpooler.class, "emails/alerts/resolved.txt", "${", "}");

    //--//

    public String       subject;
    public List<String> body;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx,
                       AlertRecord rec_alert)
    {
        switch (alert.status)
        {
            case resolved:
            case closed:
                sendEmail(ctx, rec_alert, s_template_resolved);
                break;

            default:
                sendEmail(ctx, rec_alert, s_template_opened);
                break;
        }
    }

    //--//

    private void sendEmail(AlertEngineExecutionContext ctx,
                           AlertRecord rec_alert,
                           ConfigVariables.Template<ConfigVariable> emailTemplate)
    {
        HubApplication   app = ctx.sessionProvider.getServiceNonNull(HubApplication.class);
        HubConfiguration cfg = ctx.sessionProvider.getServiceNonNull(HubConfiguration.class);

        ConfigVariables<ConfigVariable> parameters = emailTemplate.allocate();

        parameters.setValue(ConfigVariable.SiteUrl, cfg.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.AlertSysId, alert.record.sysId);
        parameters.setValue(ConfigVariable.AlertBody, String.join("\n", CollectionUtils.asEmptyCollectionIfNull(body)));
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        List<UserRecord> users = ctx.deliveryOptionsResolver.collectUsers(ctx.ensureSession(), deliveryOptions.resolvedUsers);
        for (UserRecord rec_user : users)
        {
            send(ctx, rec_alert, app, parameters, rec_user);
        }
    }

    private void send(AlertEngineExecutionContext ctx,
                      AlertRecord rec_alert,
                      HubApplication app,
                      ConfigVariables<ConfigVariable> parameters,
                      UserRecord rec_user)
    {
        SessionHolder sessionHolder = ctx.ensureSession();

        app.sendEmailNotification(sessionHolder, false, rec_user.getEmailAddress(), BoxingUtils.get(subject, "Alert notification"), false, parameters);

        UserMessageAlertRecord rec_message = UserMessageAlertRecord.newInstance(rec_user, rec_alert);
        rec_message.setBody(String.join("\n", CollectionUtils.asEmptyCollectionIfNull(body)));
        rec_message.setSubject(subject);
        rec_message.persist(sessionHolder);
    }
}
