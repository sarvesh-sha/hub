/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.config;

import com.optio3.cloud.hub.persistence.alert.AlertDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.config.SystemPreferenceRecord;
import com.optio3.cloud.hub.persistence.config.UserPreferenceRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.normalization.NormalizationRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;

public class UsageFilterResponse
{
    public       int                                           userPreferenceHits;
    public final TypedRecordIdentityList<UserPreferenceRecord> userPreferenceItems = new TypedRecordIdentityList<>();

    public       int                                             systemPreferenceHits;
    public final TypedRecordIdentityList<SystemPreferenceRecord> systemPreferenceItems = new TypedRecordIdentityList<>();

    public       int                                                       dashboardHits;
    public final TypedRecordIdentityList<DashboardDefinitionVersionRecord> dashboardItems = new TypedRecordIdentityList<>();

    public       int                                                   alertDefinitionVersionHits;
    public final TypedRecordIdentityList<AlertDefinitionVersionRecord> alertDefinitionVersionItems = new TypedRecordIdentityList<>();

    public       int                                                     metricsDefinitionVersionHits;
    public final TypedRecordIdentityList<MetricsDefinitionVersionRecord> metricsDefinitionVersionItems = new TypedRecordIdentityList<>();

    public       int                                          normalizationVersionHits;
    public final TypedRecordIdentityList<NormalizationRecord> normalizationVersionItems = new TypedRecordIdentityList<>();

    public       int                                                    reportDefinitionVersionHits;
    public final TypedRecordIdentityList<ReportDefinitionVersionRecord> reportDefinitionVersionItems = new TypedRecordIdentityList<>();

    public       int                                     workflowHits;
    public final TypedRecordIdentityList<WorkflowRecord> workflowItems = new TypedRecordIdentityList<>();
}
