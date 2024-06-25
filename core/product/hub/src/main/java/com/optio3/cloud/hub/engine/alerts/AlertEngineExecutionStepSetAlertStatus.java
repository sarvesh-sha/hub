/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.alert.AlertStatus;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("AlertEngineExecutionStepSetAlertStatus")
public class AlertEngineExecutionStepSetAlertStatus extends AlertEngineExecutionStep
{
    public TypedRecordIdentity<AlertRecord> record;
    public AlertStatus                      status;
    public String                           statusText;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx)
    {
        AlertRecord rec_alert = accessAlert(ctx, record);
        if (rec_alert != null)
        {
            ctx.logger.debug("Evaluation updated alert status %s", this);

            SessionHolder sessionHolder = ctx.ensureSession();
            rec_alert.updateStatus(sessionHolder.createHelper(AlertHistoryRecord.class), timestamp, status, statusText);
        }
    }
}
