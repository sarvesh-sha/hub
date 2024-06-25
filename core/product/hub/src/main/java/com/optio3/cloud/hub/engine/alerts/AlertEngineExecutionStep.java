/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.EngineExecutionStep;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.SessionHolder;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineExecutionStepCommitAction.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepCreateAlert.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertDescription.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertSeverity.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertStatus.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepSetAlertText.class),
                @JsonSubTypes.Type(value = AlertEngineExecutionStepSetControlPointValue.class) })
@JsonTypeName("AlertEngineExecutionStep")
public class AlertEngineExecutionStep extends EngineExecutionStep
{
    public ZonedDateTime timestamp;

    public void commit(AlertEngineExecutionContext ctx) throws
                                                        Exception
    {
    }

    protected AlertRecord accessAlert(AlertEngineExecutionContext ctx,
                                      TypedRecordIdentity<AlertRecord> record)
    {
        if (record == null)
        {
            ctx.logger.error("Invalid AlertEngineValueAlert state: no record!");
            return null;
        }

        SessionHolder sessionHolder = ctx.ensureSession();
        AlertRecord   rec_alert     = sessionHolder.getEntityOrNull(AlertRecord.class, record.sysId);
        if (rec_alert == null)
        {
            ctx.logger.error("Cannot access alert '%s', doesn't exist...", record.sysId);
        }

        return rec_alert;
    }
}
