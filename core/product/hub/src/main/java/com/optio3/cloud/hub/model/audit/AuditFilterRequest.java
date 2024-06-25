/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.audit;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.event.EventFilterRequest;

@JsonTypeName("AuditFilterRequest")
public class AuditFilterRequest extends EventFilterRequest
{
    public List<AuditType> auditTypeIDs;

    //--//

    public boolean hasTypes()
    {
        return hasItems(auditTypeIDs);
    }
}
