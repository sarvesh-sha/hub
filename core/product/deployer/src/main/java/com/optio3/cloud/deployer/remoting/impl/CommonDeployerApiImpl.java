/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.deployer.remoting.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import com.google.common.collect.Maps;
import com.optio3.cloud.deployer.DeployerApplication;
import com.optio3.cloud.deployer.DeployerConfiguration;
import com.optio3.concurrency.Executors;
import com.optio3.logging.Logger;
import com.optio3.util.Exceptions;
import com.optio3.util.MonotonousTime;
import com.optio3.util.TimeUtils;

public abstract class CommonDeployerApiImpl
{
    public static final Logger LoggerInstance = new Logger(CommonDeployerApiImpl.class, true);

    public static class MonitoredFile
    {
        public final String path;
        public final File   target;

        MonotonousTime   expiration;
        RandomAccessFile handleForRead;
        RandomAccessFile handleForWrite;

        public MonitoredFile(String path)
        {
            this.path   = path;
            this.target = new File(path);
        }

        void markInUse()
        {
            expiration = TimeUtils.computeTimeoutExpiration(12, TimeUnit.HOURS);
        }

        public boolean delete()
        {
            try
            {
                closeHandles();

                if (!target.isFile())
                {
                    return false;
                }

                return target.delete();
            }
            catch (Throwable t)
            {
                // Ignore failures.
                LoggerInstance.error("Failed to delete stale file %s, due to %s", path, t);
                return false;
            }
        }

        public synchronized void closeHandles() throws
                                                IOException
        {
            if (handleForRead != null)
            {
                handleForRead.close();
                handleForRead = null;
            }

            if (handleForWrite != null)
            {
                handleForWrite.close();
                handleForWrite = null;
            }
        }

        public synchronized int read(long filePosition,
                                     byte[] buffer) throws
                                                    IOException
        {
            RandomAccessFile file = handleForWrite;

            if (file == null)
            {
                if (handleForRead == null)
                {
                    handleForRead = new RandomAccessFile(target, "r");
                }

                file = handleForRead;
            }

            file.seek(filePosition);
            return file.read(buffer);
        }

        public synchronized void write(long filePosition,
                                       byte[] buffer,
                                       int offset,
                                       int length) throws
                                                   IOException
        {
            if (handleForWrite == null)
            {
                handleForWrite = new RandomAccessFile(target, "rw");
            }

            handleForWrite.seek(filePosition);
            handleForWrite.write(buffer, offset, length);
        }
    }

    private static final Map<String, MonitoredFile> s_files = Maps.newHashMap();
    private static       ScheduledFuture<?>         s_purger;

    //--//

    @Inject
    private DeployerApplication m_app;

    protected DeployerApplication getApplication()
    {
        return m_app;
    }

    protected DeployerConfiguration getConfiguration()
    {
        return m_app.getServiceNonNull(DeployerConfiguration.class);
    }

    protected String normalizeRoot() throws
                                     IOException
    {
        DeployerConfiguration cfg = getConfiguration();
        return getCanonicalPath(cfg.getAgentFilesRoot());
    }

    protected MonitoredFile normalizePath(String path) throws
                                                       IOException
    {
        DeployerConfiguration cfg = getConfiguration();

        Path root   = cfg.getAgentFilesRoot();
        Path target = root.resolve(path);

        String root2   = getCanonicalPath(root);
        String target2 = getCanonicalPath(target);

        if (target2.startsWith(root2))
        {
            synchronized (s_files)
            {
                MonitoredFile desc = s_files.get(target2);
                if (desc == null)
                {
                    LoggerInstance.debug("New tracked file %s", target2);

                    desc = new MonitoredFile(target2);
                    s_files.put(target2, desc);
                }

                desc.markInUse();

                ensureScheduled();

                return desc;
            }
        }

        throw Exceptions.newRuntimeException("Invalid path: %s", path);
    }

    private void ensureScheduled()
    {
        if (s_purger == null)
        {
            s_purger = Executors.scheduleOnDefaultPool(this::processStaleFiles, 1, TimeUnit.HOURS);
        }
    }

    private void processStaleFiles()
    {
        synchronized (s_files)
        {
            s_purger = null;

            Iterator<MonitoredFile> it = s_files.values()
                                                .iterator();
            while (it.hasNext())
            {
                MonitoredFile desc = it.next();

                LoggerInstance.debugVerbose("Checking file %s (expires at %s)", desc.target, desc.expiration);

                if (!desc.target.exists())
                {
                    LoggerInstance.debugVerbose("File %s already deleted...", desc.target);

                    desc.delete();

                    it.remove();
                }
                else if (TimeUtils.isTimeoutExpired(desc.expiration))
                {
                    LoggerInstance.debug("Deleting stale file %s", desc.target);

                    desc.delete();

                    it.remove();
                }
            }

            if (!s_files.isEmpty())
            {
                ensureScheduled();
            }
            else
            {
                LoggerInstance.debugVerbose("No more files to track, existing...");
            }
        }
    }

    private static String getCanonicalPath(Path path) throws
                                                      IOException
    {
        return path.toFile()
                   .getCanonicalPath();
    }
}
