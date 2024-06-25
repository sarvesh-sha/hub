/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAction;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;

@JsonTypeName("AlertEngineExecutionStepCommitAction")
public class AlertEngineExecutionStepCommitAction extends AlertEngineExecutionStep
{
    public AlertEngineValueAction details;

    @Override
    public void commit(AlertEngineExecutionContext ctx)
    {
        AlertEngineValueAlert alert = details.alert;
        if (alert != null)
        {
            ctx.logger.debug("Evaluation created action %s", details);

            AlertRecord rec_alert = accessAlert(ctx, alert.record);

            details.commit(ctx, rec_alert);
        }
    }
}
