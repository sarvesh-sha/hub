/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.deployment.DeploymentHostFile;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedFileTransfer;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.RecordLocator;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DEPLOYMENT_HOST_FILE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentHostFile", model = DeploymentHostFile.class, metamodel = DeploymentHostFileRecord_.class)
public class DeploymentHostFileRecord extends RecordWithCommonFields implements ModelMapperTarget<DeploymentHostFile, DeploymentHostFileRecord_>
{
    /**
     * The deployment this file is controlled by.
     */
    @Optio3ControlNotifications(reason = "Only notify host of file's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getDeployment")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getDeployment")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "deployment", nullable = false, foreignKey = @ForeignKey(name = "FILE__DEPLOYMENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostRecord deployment;

    /**
     * The task this file is controlled by.
     */
    @Optio3ControlNotifications(reason = "Only notify task of file's changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getTask", setter = "setTask")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "task", foreignKey = @ForeignKey(name = "FILE__TASK__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentTaskRecord task;

    //--//

    @Column(name = "path")
    private String path;

    @Column(name = "task_name")
    private String taskName;

    @Column(name = "downloaded_on")
    private ZonedDateTime downloadedOn;

    @Column(name = "uploaded_on")
    private ZonedDateTime uploadedOn;

    @OneToMany(mappedBy = "owningFile", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sequenceNumber")
    private List<DeploymentHostFileChunkRecord> chunks;

    //--//

    public DeploymentHostFileRecord()
    {
    }

    public static DeploymentHostFileRecord newInstance(DeploymentHostRecord host,
                                                       String path,
                                                       DeploymentTaskRecord task)
    {
        requireNonNull(host);

        DeploymentHostFileRecord res = new DeploymentHostFileRecord();
        res.deployment = host;
        res.path       = path;

        res.setTask(task);

        return res;
    }

    //--//

    public DeploymentHostRecord getDeployment()
    {
        return deployment;
    }

    public DeploymentTaskRecord getTask()
    {
        return task;
    }

    public void setTask(DeploymentTaskRecord task)
    {
        this.task = task;

        if (task != null)
        {
            taskName = String.format("%s (%s)", task.getName(), task.getDockerId());
        }
    }

    public String getTaskName()
    {
        return taskName;
    }

    public String getPath()
    {
        return path;
    }

    public long getLength()
    {
        if (CollectionUtils.isEmpty(chunks))
        {
            return -1;
        }

        long total = 0;

        for (DeploymentHostFileChunkRecord chunk : chunks)
        {
            total += chunk.getLength();
        }

        return total;
    }

    public ZonedDateTime getDownloadedOn()
    {
        return downloadedOn;
    }

    public void setDownloadedOn(ZonedDateTime downloadedOn)
    {
        this.downloadedOn = downloadedOn;
    }

    public ZonedDateTime getUploadedOn()
    {
        return uploadedOn;
    }

    public void setUploadedOn(ZonedDateTime uploadedOn)
    {
        this.uploadedOn = uploadedOn;
    }

    //--//

    public void start(SessionHolder sessionHolder,
                      boolean upload) throws
                                      Exception
    {
        DelayedFileTransfer.queue(sessionHolder, this, upload);
    }

    //--//

    public List<DeploymentHostFileChunkRecord> getChunks()
    {
        return CollectionUtils.asEmptyCollectionIfNull(chunks);
    }

    public void deleteContents(SessionHolder sessionHolder)
    {
        for (DeploymentHostFileChunkRecord rec_chunk : Lists.newArrayList(getChunks()))
        {
            sessionHolder.deleteEntity(rec_chunk);
        }
    }

    public static OutputStream writeAsStream(SessionProvider sessionProvider,
                                             RecordLocator<DeploymentHostFileRecord> loc_file)
    {
        return new OutputStream()
        {
            private final byte[] m_buffer = new byte[DeploymentHostFileChunkRecord.c_chunkSize];
            private int m_offset;
            private int m_sequenceNumber;

            @Override
            public void write(byte[] b,
                              int off,
                              int len) throws
                                       IOException
            {
                while (len > 0)
                {
                    int chunk = Math.min(len, m_buffer.length - m_offset);

                    System.arraycopy(b, off, m_buffer, m_offset, chunk);

                    off += chunk;
                    len -= chunk;
                    m_offset += chunk;

                    flushIfNeeded();
                }
            }

            @Override
            public void flush() throws
                                IOException
            {
                // Noop, we only flush at the end.
            }

            @Override
            public void close() throws
                                IOException
            {
                reallyFlush();
            }

            @Override
            public void write(int b) throws
                                     IOException
            {
                m_buffer[m_offset++] = (byte) b;

                flushIfNeeded();
            }

            private void flushIfNeeded() throws
                                         IOException
            {
                if (m_offset == m_buffer.length)
                {
                    reallyFlush();
                }
            }

            private void reallyFlush() throws
                                       IOException
            {
                if (m_offset > 0)
                {
                    try (SessionHolder subSessionHolder = sessionProvider.newSessionWithTransaction())
                    {
                        DeploymentHostFileRecord rec_file = subSessionHolder.fromLocatorOrNull(loc_file);
                        if (rec_file != null)
                        {
                            DeploymentHostFileChunkRecord rec_chunk = DeploymentHostFileChunkRecord.newInstance(rec_file, m_sequenceNumber++);
                            rec_chunk.setContents(Arrays.copyOf(m_buffer, m_offset));

                            subSessionHolder.persistEntity(rec_chunk);
                            subSessionHolder.commit();
                        }
                    }

                    m_offset = 0;
                }
            }
        };
    }

    public InputStream readAsStream(SessionProvider sessionProvider)
    {
        List<String> chunks = CollectionUtils.transformToList(this.chunks, RecordWithCommonFields::getSysId);

        return new InputStream()
        {
            private byte[] m_buffer;
            private int m_offset;
            private int m_sequenceNumber;

            @Override
            public int read(byte[] b,
                            int off,
                            int len)
            {
                int read = 0;

                while (len > 0 && isAvailable())
                {
                    int chunk = Math.min(len, m_buffer.length - m_offset);
                    System.arraycopy(m_buffer, m_offset, b, off, chunk);

                    off += chunk;
                    len -= chunk;
                    read += chunk;
                    m_offset += chunk;
                }

                return read > 0 ? read : -1;
            }

            @Override
            public long skip(long n)
            {
                long skipped = 0;

                while (n > 0 && isAvailable())
                {
                    long chunk = Math.min(n, m_buffer.length - m_offset);
                    m_offset += chunk;
                    n -= chunk;
                    skipped += chunk;
                }

                return skipped;
            }

            @Override
            public void close() throws
                                IOException
            {
                m_sequenceNumber = -2; // An invalid value.
                m_buffer         = null;
            }

            @Override
            public synchronized void reset()
            {
                m_buffer         = null;
                m_offset         = 0;
                m_sequenceNumber = 0;
            }

            @Override
            public int read() throws
                              IOException
            {
                return isAvailable() ? m_buffer[m_offset++] & 0xFF : -1;
            }

            private boolean isAvailable()
            {
                if (m_buffer == null || m_offset >= m_buffer.length)
                {
                    m_buffer = null;
                    m_offset = 0;

                    String sysId = CollectionUtils.getNthElement(chunks, m_sequenceNumber);
                    if (sysId != null)
                    {
                        try (SessionHolder subSessionHolder = sessionProvider.newReadOnlySession())
                        {
                            DeploymentHostFileChunkRecord rec_chunk = subSessionHolder.getEntity(DeploymentHostFileChunkRecord.class, sysId);
                            m_buffer = rec_chunk.getContents();
                            m_sequenceNumber++;
                        }
                    }
                }

                return m_buffer != null;
            }
        };
    }
}
