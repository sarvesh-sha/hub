/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.worker;

import static java.util.Objects.requireNonNull;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.worker.MappedDockerVolume;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.SessionHolder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "MAPPED_DOCKER_VOLUME")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "MappedDockerVolume", model = MappedDockerVolume.class, metamodel = MappedDockerVolumeRecord_.class)
public class MappedDockerVolumeRecord extends RecordWithCommonFields implements ModelMapperTarget<MappedDockerVolume, MappedDockerVolumeRecord_>
{
    /**
     * Bound to this container.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getOwningContainer")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_container", nullable = false, foreignKey = @ForeignKey(name = "OWNING_CONTAINER__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DockerContainerRecord owningContainer;

    //--//

    /**
     * Path for the files inside the container.
     */
    @Column(name = "path", nullable = false)
    private String path;

    /**
     * If set, the files come from a local directory.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getDirectory")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "directory", foreignKey = @ForeignKey(name = "DIRECTORY__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private ManagedDirectoryRecord directory;

    /**
     * If set, the files come from a volume.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getVolume")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "volume", foreignKey = @ForeignKey(name = "VOLUME__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DockerVolumeRecord volume;

    //--//

    public MappedDockerVolumeRecord()
    {
    }

    public static MappedDockerVolumeRecord newInstance(DockerContainerRecord container)
    {
        requireNonNull(container);

        MappedDockerVolumeRecord res = new MappedDockerVolumeRecord();
        res.owningContainer = container;
        return res;
    }

    //--//

    public DockerContainerRecord getOwningContainer()
    {
        return owningContainer;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public ManagedDirectoryRecord getDirectory()
    {
        return directory;
    }

    public void setDirectory(ManagedDirectoryRecord directory)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.directory != directory)
        {
            this.directory = directory;
        }
    }

    public DockerVolumeRecord getVolume()
    {
        return volume;
    }

    public void setVolume(DockerVolumeRecord volume)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.volume != volume)
        {
            this.volume = volume;
        }
    }

    //--//

    public void deleteResources(SessionHolder sessionHolder)
    {
        if (directory != null)
        {
            directory.getMappedIn()
                     .remove(this);
            directory = null;
        }

        if (volume != null)
        {
            volume.getMappedAs()
                  .remove(this);
            volume = null;
        }

        if (owningContainer != null)
        {
            owningContainer.getMappedTo()
                           .remove(this);
            owningContainer = null;
        }

        sessionHolder.deleteEntity(this);
    }
}
