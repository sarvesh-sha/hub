/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.io.IOException;
import java.io.InputStream;

import com.optio3.cloud.builder.logic.RegistryLogic;
import com.optio3.cloud.builder.model.jobs.output.RegistryRefresh;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;

public class TaskForRegistryRefresh extends BaseBookKeepingTask implements IBackgroundActivityProgress<RegistryRefresh>
{
    public RegistryRefresh results;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder) throws
                                                                                     Exception
    {
        return BaseBookKeepingTask.scheduleActivity(sessionHolder, TaskForRegistryRefresh.class, (t) ->
        {
            // Nothing to configure.
        });
    }

    //--//

    @Override
    public RegistryRefresh fetchProgress(SessionHolder sessionHolder,
                                         boolean detailed)
    {
        return results != null ? results : new RegistryRefresh();
    }

    @Override
    public void generateStream() throws
                                 IOException
    {
        // Nothing to do.
    }

    @Override
    public InputStream streamContents() throws
                                        IOException
    {
        return null;
    }

    //--//

    @Override
    public void configureContext()
    {
        // Nothing to do.
    }

    @Override
    public String getTitle()
    {
        return "Refresh list of Docker Images from the Registry";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(needsSession = true, autoRetry = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        loggerInstance.info("Starting to list Registry catalog...");
        RegistryLogic logic = new RegistryLogic(sessionHolder);
        results = logic.refreshImagesFromRegistry();
        loggerInstance.info("Done listing Registry catalog, found %s images", results.images);

        markAsCompleted();
    }
}
