/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.workflow;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.hub.persistence.config.UserRecord;
import com.optio3.cloud.hub.persistence.workflow.WorkflowRecord;
import com.optio3.cloud.model.BasePaginatedResponse;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.SortCriteria;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.serialization.Reflection;
import com.optio3.util.CollectionUtils;

public class WorkflowOverrides
{
    public Set<String> workflowIds = Sets.newHashSet();

    public Map<String, String> deviceNames     = Maps.newHashMap();
    public Map<String, String> deviceLocations = Maps.newHashMap();

    public Map<String, String>  pointNames           = Maps.newHashMap();
    public Map<String, String>  pointClasses         = Maps.newHashMap();
    public Map<String, String>  pointParents         = Maps.newHashMap();
    public Map<String, Integer> pointSamplingPeriods = Maps.newHashMap();
    public Map<String, Boolean> pointSampling        = Maps.newHashMap();

    public Map<String, String> equipmentNames     = Maps.newHashMap();
    public Map<String, String> equipmentClasses   = Maps.newHashMap();
    public Map<String, String> equipmentParents   = Maps.newHashMap();
    public Map<String, String> equipmentLocations = Maps.newHashMap();
    public Map<String, String> equipmentMerge     = Maps.newHashMap();

    public Set<String> removedEquipment = Sets.newHashSet();
    public Set<String> createdEquipment = Sets.newHashSet();

    public static WorkflowOverrides load(SessionHolder sessionHolder)
    {
        WorkflowOverrides            overrides = new WorkflowOverrides();
        RecordHelper<WorkflowRecord> helper    = sessionHolder.createHelper(WorkflowRecord.class);

        SortCriteria sort = new SortCriteria();
        sort.ascending = true;
        sort.column    = "createdOn";

        WorkflowFilterRequest filters = new WorkflowFilterRequest();
        filters.sortBy = Lists.newArrayList(sort);

        BasePaginatedResponse<RecordIdentity> workflowIds = WorkflowRecord.filter(helper, filters);
        List<WorkflowRecord>                  workflows   = WorkflowRecord.getBatch(helper, CollectionUtils.transformToList(workflowIds.results, id -> id.sysId));

        for (WorkflowRecord rec_workflow : workflows)
        {
            overrides.workflowIds.add(rec_workflow.getSysId());

            switch (rec_workflow.getStatus())
            {
                case Disabled:
                case Disabling:
                    continue;
            }

            IWorkflowHandlerForOverrides handler = Reflection.as(rec_workflow.getDetails(), IWorkflowHandlerForOverrides.class);
            if (handler != null)
            {
                handler.getOverride(overrides);
            }
        }

        return overrides;
    }

    public void resolveWorkflows(SessionHolder sessionHolder,
                                 RecordLocator<UserRecord> loc_user)
    {
        RecordHelper<WorkflowRecord> helper   = sessionHolder.createHelper(WorkflowRecord.class);
        UserRecord                   rec_user = sessionHolder.fromLocatorOrNull(loc_user);

        for (String sysId : workflowIds)
        {
            WorkflowRecord rec_workflow = helper.get(sysId);
            rec_workflow.markAsProcessed(sessionHolder, rec_user != null ? rec_user : rec_workflow.getCreatedBy());
        }
    }
}
