/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.alerts.value.AlertEngineValueAlert;
import com.optio3.cloud.hub.model.alert.AlertEventLevel;
import com.optio3.cloud.hub.model.alert.AlertEventType;
import com.optio3.cloud.hub.model.alert.AlertSeverity;
import com.optio3.cloud.hub.model.alert.AlertType;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.SessionHolder;

@JsonTypeName("AlertEngineExecutionStepCreateAlert")
public class AlertEngineExecutionStepCreateAlert extends AlertEngineExecutionStep
{
    public TypedRecordIdentity<DeviceElementRecord> controlPoint;
    public TypedRecordIdentity<AlertRecord>         record;

    public AlertEventLevel level;
    public AlertType       type;
    public AlertSeverity   severity;

    @Override
    public void commit(AlertEngineExecutionContext ctx) throws
                                                        Exception
    {
        ctx.logger.debug("Evaluation created alert %s", this);

        if (record == null)
        {
            ctx.logger.error("Invalid AlertEngineValueAlert state: no record!");
            return;
        }

        if (controlPoint == null)
        {
            ctx.logger.error("Invalid AlertEngineValueAlert state: no device element!");
            return;
        }

        DeviceElementRecord rec_deviceElement = ctx.loadRecordIdentity(controlPoint);
        if (rec_deviceElement == null)
        {
            ctx.logger.error("Cannot create alert, target element '%s' missing", controlPoint.sysId);
            return;
        }

        if (type == null)
        {
            type = AlertType.DEVICE_FAILURE;
        }

        if (severity == null)
        {
            severity = AlertSeverity.NORMAL;
        }

        if (timestamp == null)
        {
            AlertEngineValueAlert alert = ctx.alertHolder.getAlert(controlPoint, type);
            timestamp = alert != null ? alert.timestamp : null;
        }

        SessionHolder                sessionHolder = ctx.ensureSession();
        AlertDefinitionRecord        rec_def       = ctx.getAlertDefinitionRecord();
        AlertDefinitionVersionRecord rec_version   = ctx.getAlertDefinitionVersionRecord();
        AlertRecord                  rec_alert     = rec_deviceElement.prepareNewAlert(sessionHolder, rec_version, timestamp, type, AlertEventLevel.failure);

        rec_alert.setSeverity(severity);
        rec_alert.setDescription(rec_def.getTitle());

        //
        // We create a fake sysId in the record value for the alert during evaluation.
        // Use it one on the real record.
        //
        rec_alert.setSysId(record.sysId);

        sessionHolder.persistEntity(rec_alert);

        rec_alert.addHistoryEntry(sessionHolder, timestamp, AlertEventLevel.failure, AlertEventType.created, "%s", rec_def.getTitle());
    }
}
