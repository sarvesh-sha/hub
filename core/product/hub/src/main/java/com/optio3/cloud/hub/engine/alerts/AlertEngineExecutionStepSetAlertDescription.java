/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("AlertEngineExecutionStepSetAlertDescription")
public class AlertEngineExecutionStepSetAlertDescription extends AlertEngineExecutionStep
{
    public TypedRecordIdentity<AlertRecord> record;
    public String                           description;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx)
    {
        AlertRecord rec_alert = accessAlert(ctx, record);
        if (rec_alert != null)
        {
            rec_alert.setExtendedDescription(description);
        }
    }
}
