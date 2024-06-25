/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.alert;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.event.EventFilterRequest;
import com.optio3.cloud.hub.persistence.alert.AlertDefinitionRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

@JsonTypeName("AlertFilterRequest")
public class AlertFilterRequest extends EventFilterRequest
{
    public List<AlertStatus>                              alertStatusIDs;
    public List<AlertType>                                alertTypeIDs;
    public List<AlertSeverity>                            alertSeverityIDs;
    public TypedRecordIdentityList<AlertDefinitionRecord> alertRules;

    //--//

    public boolean hasStatus()
    {
        return hasItems(alertStatusIDs);
    }

    public boolean hasTypes()
    {
        return hasItems(alertTypeIDs);
    }

    public boolean hasSeverities()
    {
        return hasItems(alertSeverityIDs);
    }

    public boolean hasRules()
    {
        return hasItems(alertRules);
    }
}
