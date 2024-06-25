/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.bookkeeping;

import java.io.IOException;
import java.io.InputStream;

import com.optio3.cloud.builder.logic.RepositoryLogic;
import com.optio3.cloud.builder.model.jobs.input.RepositoryRefresh;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.logic.IBackgroundActivityProgress;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;

public class TaskForRepositoryRefresh extends BaseBookKeepingTask implements IBackgroundActivityProgress<RepositoryRefresh>
{
    public RepositoryRefresh results;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder) throws
                                                                                     Exception
    {
        return BaseBookKeepingTask.scheduleActivity(sessionHolder, TaskForRepositoryRefresh.class, (t) ->
        {
            // Nothing to configure.
        });
    }

    //--//

    @Override
    public RepositoryRefresh fetchProgress(SessionHolder sessionHolder,
                                           boolean detailed)
    {
        return results != null ? results : new RepositoryRefresh();
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
        return "Refresh branches and commits from GitHub";
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
        RepositoryLogic logic = new RepositoryLogic(sessionHolder);
        results = logic.refreshAll();
        markAsCompleted();
    }
}
