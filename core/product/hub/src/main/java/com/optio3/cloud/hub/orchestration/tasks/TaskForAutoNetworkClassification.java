/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.hub.persistence.asset.NetworkAssetRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;

public class TaskForAutoNetworkClassification extends AbstractHubActivityHandler
{
    public RecordLocator<NetworkAssetRecord> loc_network;

    //--//

    public static BackgroundActivityRecord scheduleTaskIfNotRunning(SessionHolder sessionHolder,
                                                                    NetworkAssetRecord rec_network) throws
                                                                                                    Exception
    {
        var lst = BackgroundActivityRecord.findHandlers(sessionHolder, false, true, TaskForAutoNetworkClassification.class, sessionHolder.createLocator(rec_network));

        BackgroundActivityRecord rec_activity = sessionHolder.fromIdentityOrNull(CollectionUtils.firstElement(lst));
        if (rec_activity == null)
        {
            //
            // Start task with a delay, to make sure we pick up as many changes as possible.
            //
            rec_activity = scheduleActivity(sessionHolder, 2, ChronoUnit.SECONDS, TaskForAutoNetworkClassification.class, (t) ->
            {
                t.loc_network = sessionHolder.createLocator(rec_network);
            });
        }
        else
        {
            rec_activity.transitionToActive(TimeUtils.future(2, TimeUnit.SECONDS));
        }

        return rec_activity;
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Reclassify network with well-known equipment/point classes";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return loc_network;
    }

    @BackgroundActivityMethod(initial = true)
    public void process() throws
                          Exception
    {
        InstanceConfiguration cfg = getServiceNonNull(InstanceConfiguration.class);
        cfg.executeClassification(getSessionProvider(), loc_network);

        markAsCompleted();
    }
}