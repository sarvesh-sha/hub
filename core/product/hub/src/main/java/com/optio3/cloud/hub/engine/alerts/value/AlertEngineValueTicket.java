/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.alerts.value;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.engine.alerts.AlertEngineExecutionContext;
import com.optio3.cloud.hub.persistence.alert.AlertRecord;

@JsonTypeName("AlertEngineValueTicket")
public class AlertEngineValueTicket extends AlertEngineValueAction
{
    public String       subject;
    public List<String> body;

    //--//

    @Override
    public void commit(AlertEngineExecutionContext ctx,
                       AlertRecord rec_alert)
    {
        // TODO: create ticket
    }
}
