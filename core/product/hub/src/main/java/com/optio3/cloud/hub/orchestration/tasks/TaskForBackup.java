/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.orchestration.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;

import com.optio3.archive.TarBuilder;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.orchestration.AbstractHubActivityHandler;
import com.optio3.cloud.hub.persistence.BackgroundActivityRecord;
import com.optio3.cloud.logic.BackgroundActivityHandler;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.util.FileSystem;
import com.optio3.util.TimeUtils;
import org.hibernate.query.Query;

public class TaskForBackup extends AbstractHubActivityHandler implements BackgroundActivityHandler.ICleanupOnFailure
{
    public String file;

    //--//

    public static BackgroundActivityRecord scheduleTask(SessionHolder sessionHolder) throws
                                                                                     Exception
    {
        return scheduleActivity(sessionHolder, 0, null, TaskForBackup.class, (newHandler) ->
        {
            HubConfiguration cfg = sessionHolder.getServiceNonNull(HubConfiguration.class);
            FileSystem.createDirectory(Paths.get(cfg.backupLocation));
            newHandler.file = String.format("%s/%s.tar.gz", cfg.backupLocation, DEFAULT_TIMESTAMP.format(TimeUtils.now()));
        });
    }

    //--//

    @Override
    public String getTitle()
    {
        return "Live Database Backup";
    }

    @Override
    public RecordLocator<? extends RecordWithCommonFields> getContext()
    {
        return null;
    }

    @BackgroundActivityMethod(needsSession = true)
    public void process(SessionHolder sessionHolder) throws
                                                     Exception
    {
        try (FileSystem.TmpFileHolder backupAsZip = FileSystem.createTempFile("backup", "zip"))
        {
            Query query = sessionHolder.createNativeQuery("BACKUP TO :file")
                                       .setParameter("file", backupAsZip.getAbsolutePath());

            query.executeUpdate();

            try (FileOutputStream stream = new FileOutputStream(file))
            {
                try (TarBuilder builder = new TarBuilder(stream, true))
                {
                    builder.copyFromZip(backupAsZip.get());
                }
            }
        }

        markAsCompleted();
    }

    @Override
    public void cleanupOnFailure(Throwable t)
    {
        new File(file).delete();
    }
}
