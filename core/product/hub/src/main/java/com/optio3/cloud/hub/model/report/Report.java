/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.optio3.cloud.annotation.Optio3MapAsReadOnly;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.model.TypedRecordIdentity;

public class Report extends BaseModel
{
    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionRecord> reportDefinition;

    @Optio3MapAsReadOnly
    public TypedRecordIdentity<ReportDefinitionVersionRecord> reportDefinitionVersion;

    public ZonedDateTime rangeStart;

    public ZonedDateTime rangeEnd;

    @Optio3MapAsReadOnly
    public ReportStatus status;

    @Optio3MapAsReadOnly
    public ReportReason reason;

    @Optio3MapAsReadOnly
    public int size;

    public static String getDownloadUrl(String sysId,
                                        String name,
                                        ZonedDateTime createdOn,
                                        HubConfiguration cfg)
    {
        String encodedName = "report";
        try
        {
            encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.name());
        }
        catch (UnsupportedEncodingException ex)
        {
        }

        String formattedTime = createdOn.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        return String.format("%s/api/v1/reports/item/%s/%s__%s.pdf", cfg.cloudConnectionUrl, sysId, encodedName, formattedTime);
    }
}
