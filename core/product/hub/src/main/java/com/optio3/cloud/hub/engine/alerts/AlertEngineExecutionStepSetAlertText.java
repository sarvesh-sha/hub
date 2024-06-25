/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.alert.AlertHistoryRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("AlertEngineExecutionStepSetAlertText")
public class AlertEngineExecutionStepSetAlertText extends AlertEngineExecutionStep
{
    public TypedRecordIdentity<AlertRecord> record;
    public String                           text;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx)
    {
        AlertRecord rec_alert = accessAlert(ctx, record);
        if (rec_alert != null && text != null)
        {
            SessionHolder sessionHolder = ctx.ensureSession();
            rec_alert.updateLastHistoryText(sessionHolder.createHelper(AlertHistoryRecord.class), text);
        }
    }
}
