/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.optio3.cloud.hub.engine.EngineStatement;

@JsonSubTypes({ @JsonSubTypes.Type(value = AlertEngineStatementAddEmailLine.class),
                @JsonSubTypes.Type(value = AlertEngineStatementAddSmsLine.class),
                @JsonSubTypes.Type(value = AlertEngineStatementCommitAction.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetAlertDescription.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetAlertSeverity.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetAlertStatus.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetAlertText.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetAlertTimestamp.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetControlPointMetadata.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetEmailSubject.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetProperty.class),
                @JsonSubTypes.Type(value = AlertEngineStatementSetSmsSender.class) })
public abstract class EngineStatementFromAlerts extends EngineStatement
{
}
