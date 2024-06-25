/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.logic.alerts.AlertExecutionSpooler;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import com.optio3.util.TimeUtils;

@JsonTypeName("AlertEngineValueSms")
public class AlertEngineValueSms extends AlertEngineValueAction
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
    private static final ConfigVariables.Template<ConfigVariable>  s_template_opened   = s_configValidator.newTemplate(AlertExecutionSpooler.class, "texts/alerts/opened.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_resolved = s_configValidator.newTemplate(AlertExecutionSpooler.class, "texts/alerts/resolved.txt", "${", "}");

    //--//

    public String       sender;
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
                sendText(ctx, rec_alert, s_template_resolved);
                break;

            default:
                sendText(ctx, rec_alert, s_template_opened);
                break;
        }
    }

    //--//

    private void sendText(AlertEngineExecutionContext ctx,
                          AlertRecord rec_alert,
                          ConfigVariables.Template<ConfigVariable> textTemplate)
    {
        HubApplication   app = ctx.sessionProvider.getServiceNonNull(HubApplication.class);
        HubConfiguration cfg = ctx.sessionProvider.getServiceNonNull(HubConfiguration.class);

        ConfigVariables<ConfigVariable> parameters = textTemplate.allocate();

        parameters.setValue(ConfigVariable.SiteUrl, cfg.cloudConnectionUrl);
        parameters.setValue(ConfigVariable.AlertSysId, alert.record.sysId);
        parameters.setValue(ConfigVariable.AlertBody, String.join("\n", CollectionUtils.asEmptyCollectionIfNull(body)));
        parameters.setValue(ConfigVariable.Timestamp, TimeUtils.now());

        SessionHolder sessionHolder = ctx.ensureSession();

        List<UserRecord> users = ctx.deliveryOptionsResolver.collectUsers(sessionHolder, deliveryOptions.resolvedUsers);
        for (UserRecord rec_user : users)
        {
            app.sendTextNotification(sessionHolder, false, sender, rec_user.getPhoneNumber(), parameters);
        }
    }
}
