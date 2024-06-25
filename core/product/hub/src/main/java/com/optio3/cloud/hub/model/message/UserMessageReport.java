/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.message;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.persistence.message.UserMessageRecord;
import com.optio3.cloud.hub.persistence.message.UserMessageReportRecord;
import com.optio3.cloud.hub.persistence.report.ReportRecord;
import com.optio3.cloud.model.TypedRecordIdentity;

@JsonTypeName("UserMessageReport")
public class UserMessageReport extends UserMessage
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportRecord> report;

    //--//

    @Override
    public UserMessageRecord newRecord()
    {
        return new UserMessageReportRecord();
    }
}
