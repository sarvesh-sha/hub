/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static java.util.Objects.requireNonNull;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.model.BaseModel;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DEPLOYMENT_HOST_FILE_CHUNK")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentHostFileChunk", model = BaseModel.class, metamodel = DeploymentHostFileChunkRecord_.class)
public class DeploymentHostFileChunkRecord extends RecordWithCommonFields
{
    public static final int c_chunkSize = 6 * 1024 * 1024;

    /**
     * The file this chunk belongs to.
     */
    @Optio3ControlNotifications(reason = "Report changes to file", direct = Optio3ControlNotifications.Notify.ALWAYS, reverse = Optio3ControlNotifications.Notify.NEVER, getter = "getOwningFile")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.DELETE, getter = "getOwningFile")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_file", nullable = false, foreignKey = @ForeignKey(name = "DEPLOYMENT_HOST_FILE_CHUNK__FILE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostFileRecord owningFile;

    //--//

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "length", nullable = false)
    private int length;

    @Lob
    @Column(name = "contents", length = c_chunkSize)
    @Basic(fetch = FetchType.LAZY)
    private byte[] contents;

    //--//

    public DeploymentHostFileChunkRecord()
    {
    }

    public static DeploymentHostFileChunkRecord newInstance(DeploymentHostFileRecord file,
                                                            int sequenceNumber)
    {
        requireNonNull(file);

        DeploymentHostFileChunkRecord res = new DeploymentHostFileChunkRecord();
        res.owningFile     = file;
        res.sequenceNumber = sequenceNumber;
        return res;
    }

    //--//

    public DeploymentHostFileRecord getOwningFile()
    {
        return owningFile;
    }

    public int getSequenceNumber()
    {
        return sequenceNumber;
    }

    public byte[] getContents()
    {
        return contents;
    }

    public void setContents(byte[] contents)
    {
        this.contents = contents;
        this.length   = contents.length;
    }

    public int getLength()
    {
        return length;
    }
}
