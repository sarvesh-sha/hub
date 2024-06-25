/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.deployment;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Maps;
import com.optio3.asyncawait.AsyncRuntime;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.builder.model.deployment.DeploymentOperationalStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentStatus;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskRestartSingle;
import com.optio3.cloud.builder.persistence.deployment.delayedOps.DelayedTaskTermination;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.proxy.DeployerDockerApi;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogHandler;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithHeartbeat;
import com.optio3.cloud.persistence.RecordWithHeartbeat_;
import com.optio3.cloud.persistence.RecordWithMetadata_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.service.IServiceProvider;
import com.optio3.util.BoxingUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "DEPLOYMENT_TASK")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "DeploymentTask", model = DeploymentTask.class, metamodel = DeploymentTaskRecord_.class, metadata = DeploymentTaskRecord.WellKnownMetadata.class)
public class DeploymentTaskRecord extends RecordWithHeartbeat implements ModelMapperTarget<DeploymentTask, DeploymentTaskRecord_>,
                                                                         LogHandler.ILogHost<DeploymentTaskLogRecord>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final TypeReference<Map<String, EmbeddedMountpoint>> s_typeRef_Mounts = new TypeReference<>()
        {
        };

        public static final MetadataField<Map<String, String>>             labels         = new MetadataField<>("labels", MetadataField.TypeRef_mapOfStrings, Maps::newHashMap);
        public static final MetadataField<Map<String, EmbeddedMountpoint>> mounts         = new MetadataField<>("mounts", s_typeRef_Mounts, Maps::newHashMap);
        public static final MetadataField<Integer>                         restartCount   = new MetadataField<>("restartCount", Integer.class);
        public static final MetadataField<ZonedDateTime>                   restartWarning = new MetadataField<>("restartWarning", ZonedDateTime.class);
    }

    //--//

    /**
     * The deployment this task is controlled by.
     */
    @Optio3ControlNotifications(reason = "Only notify host of task's changes", direct = Notify.ALWAYS, reverse = Notify.NEVER, getter = "getDeployment")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "deployment", nullable = false, foreignKey = @ForeignKey(name = "TASK__DEPLOYMENT__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private DeploymentHostRecord deployment;

    //--//

    @Column(name = "docker_id")
    private String dockerId;

    @Optio3UpgradeValue("Initialized")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeploymentStatus status;

    @Column(name = "purpose")
    @Enumerated(EnumType.STRING)
    private DeploymentRole purpose;

    @Column(name = "image", nullable = false)
    private String image;

    @Optio3ControlNotifications(reason = "Don't notify image", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getImageReference")
    @Optio3Cascade(mode = Flavor.CLEAR, getter = "getImageReference", setter = "setImageReference")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "image_reference", foreignKey = @ForeignKey(name = "IMAGE_REFERENCE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RegistryImageRecord imageReference;

    @Column(name = "name")
    private String name;

    //--//

    @Column(name = "last_output")
    private ZonedDateTime lastOutput;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset;

    @Lob
    @Column(name = "log_ranges")
    private byte[] logRanges;

    //--//

    public DeploymentTaskRecord()
    {
    }

    public static DeploymentTaskRecord newInstance(DeploymentHostRecord deployer)
    {
        requireNonNull(deployer);

        DeploymentTaskRecord res = new DeploymentTaskRecord();
        res.deployment = deployer;
        res.status     = DeploymentStatus.Initialized;
        return res;
    }

    //--//

    public ZonedDateTime getLastOutput()
    {
        return lastOutput;
    }

    public int getLastOffset()
    {
        return lastOffset;
    }

    @Override
    public byte[] getLogRanges()
    {
        return logRanges;
    }

    @Override
    public void setLogRanges(byte[] logRanges,
                             ZonedDateTime lastOutput,
                             int lastOffset)
    {
        if (!Arrays.equals(this.logRanges, logRanges))
        {
            this.logRanges  = logRanges;
            this.lastOutput = lastOutput;
            this.lastOffset = lastOffset;
        }
    }

    @Override
    public void refineLogQuery(LogHandler.JoinHelper<?, DeploymentTaskLogRecord> jh)
    {
        jh.addWhereClauseWithEqual(jh.rootLog, DeploymentTaskLogRecord_.owningTask, this);
    }

    @Override
    public DeploymentTaskLogRecord allocateNewLogInstance()
    {
        return DeploymentTaskLogRecord.newInstance(this);
    }

    public static LogHandler<DeploymentTaskRecord, DeploymentTaskLogRecord> allocateLogHandler(RecordLocked<DeploymentTaskRecord> lock)
    {
        return new LogHandler<>(lock, DeploymentTaskLogRecord.class);
    }

    public static LogHandler<DeploymentTaskRecord, DeploymentTaskLogRecord> allocateLogHandler(SessionHolder sessionHolder,
                                                                                               DeploymentTaskRecord rec)
    {
        return new LogHandler<>(sessionHolder, rec, DeploymentTaskLogRecord.class);
    }

    //--//

    public DeploymentHostRecord getDeployment()
    {
        return deployment;
    }

    public String getDockerId()
    {
        return dockerId;
    }

    public void setDockerId(String dockerId)
    {
        if (!StringUtils.equals(this.dockerId, dockerId))
        {
            this.dockerId = dockerId;
        }
    }

    public DeploymentStatus getStatus()
    {
        return status;
    }

    public void setStatus(DeploymentStatus status)
    {
        this.status = status;
    }

    public boolean conditionallyChangeStatus(DeploymentStatus expectedStatus,
                                             DeploymentStatus status)
    {
        if (this.status == expectedStatus)
        {
            this.status = status;
            return true;
        }

        return false;
    }

    public DeploymentRole getPurpose()
    {
        return purpose;
    }

    private void refreshPurpose(Map<String, String> labels)
    {
        String purposeText = WellKnownDockerImageLabel.DeploymentPurpose.getValue(labels);

        if (StringUtils.isEmpty(purposeText))
        {
            purposeText = WellKnownDockerImageLabel.Service.getValue(labels);
        }

        purpose = DeploymentRole.parse(purposeText);
    }

    public String getImage()
    {
        return image;
    }

    public void setImage(String image)
    {
        this.image = image;
    }

    public RegistryImageRecord getImageReference()
    {
        return imageReference;
    }

    public void setImageReference(RegistryImageRecord imageRef)
    {
        // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
        if (this.imageReference != imageRef)
        {
            this.imageReference = imageRef;
        }
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Map<String, String> getLabels()
    {
        return getMetadata(WellKnownMetadata.labels);
    }

    public void updateLabels(Map<String, String> labels)
    {
        putMetadata(WellKnownMetadata.labels, labels);

        refreshPurpose(labels);
    }

    public Map<String, EmbeddedMountpoint> getMounts()
    {
        return getMetadata(WellKnownMetadata.mounts);
    }

    public void updateMounts(Map<String, EmbeddedMountpoint> mounts)
    {
        putMetadata(WellKnownMetadata.mounts, mounts);
    }

    public int getRestartCount()
    {
        return BoxingUtils.get(getMetadata(WellKnownMetadata.restartCount), 0);
    }

    public void setRestartCount(int restartCount)
    {
        putMetadata(WellKnownMetadata.restartCount, restartCount);
    }

    //--//

    public DeploymentRole getRole()
    {
        if (imageReference != null)
        {
            return imageReference.getTargetService();
        }

        return getPurpose();
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<DeploymentTaskRecord, DeploymentTask>> applyFilters,
                                    Consumer<DeploymentTask> callback)
    {
        RawQueryHelper<DeploymentTaskRecord, DeploymentTask> qh = new RawQueryHelper<>(sessionHolder, DeploymentTaskRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addObject(RecordWithMetadata_.metadataCompressed, byte[].class, (obj, val) -> obj.metadataCompressed = val);
        qh.addDate(RecordWithHeartbeat_.lastHeartbeat, (obj, val) -> obj.lastHeartbeat = val);

        qh.addReference(DeploymentTaskRecord_.deployment, DeploymentHostRecord.class, (obj, val) -> obj.deployment = val);
        qh.addString(DeploymentTaskRecord_.dockerId, (obj, val) -> obj.dockerId = val);
        qh.addEnum(DeploymentTaskRecord_.status, DeploymentStatus.class, (obj, val) -> obj.status = val);
        qh.addString(DeploymentTaskRecord_.image, (obj, val) -> obj.image = val);
        qh.addReference(DeploymentTaskRecord_.imageReference, RegistryImageRecord.class, (obj, val) -> obj.imageReference = val);
        qh.addString(DeploymentTaskRecord_.name, (obj, val) -> obj.name = val);
        qh.addEnum(DeploymentTaskRecord_.purpose, DeploymentRole.class, (obj, val) -> obj.purpose = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(DeploymentTask::new, callback);
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (!gotHeartbeatRecently(10, TimeUnit.MINUTES))
        {
            // The task hasn't been updated in ten minutes, it's okay to remove it.
            return;
        }

        DeploymentHostRecord rec_host = getDeployment();
        if (rec_host != null)
        {
            if (rec_host.getOperationalStatus() == DeploymentOperationalStatus.retired)
            {
                // Host has been retired, we can delete all the tasks.
                return;
            }
        }

        switch (status)
        {
            case Ready:
                validation.addFailure("running", "Task '%s' is running", getDockerId());
                break;
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<DeploymentTaskRecord> taskHelper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            taskHelper.delete(this);
        }
    }

    //--//

    public void scheduleRestart(ValidationResultsHolder validation) throws
                                                                    Exception
    {
        String dockerId = getDockerId();
        if (dockerId != null)
        {
            DeploymentHostRecord rec_host = getDeployment();
            if (rec_host == null)
            {
                validation.addFailure("deployment", "Task '%s' doesn't have an associated host", dockerId);
            }
            else
            {
                DeploymentAgentRecord rec_agent = rec_host.findAgentByDockerId(dockerId);
                if (rec_agent != null && rec_agent.isActive())
                {
                    validation.addFailure("active", "Task '%s' is the active agent", dockerId);
                }
            }

            if (validation.canProceed())
            {
                RecordLocked<DeploymentHostRecord> lock_target = validation.sessionHolder.optimisticallyUpgradeToLocked(rec_host, 2, TimeUnit.MINUTES);

                DelayedTaskRestartSingle.queue(lock_target, this, true);
            }
        }
    }

    public void scheduleTerminate(ValidationResultsHolder validation) throws
                                                                      Exception
    {
        String dockerId = getDockerId();
        if (dockerId != null)
        {
            DeploymentHostRecord rec_host = getDeployment();
            if (rec_host == null)
            {
                validation.addFailure("deployment", "Task '%s' doesn't have an associated host", dockerId);
            }
            else
            {
                DeploymentAgentRecord rec_agent = rec_host.findAgentByDockerId(dockerId);
                if (rec_agent != null && rec_agent.isActive())
                {
                    validation.addFailure("active", "Task '%s' is the active agent", dockerId);
                }
            }

            if (validation.canProceed())
            {
                DelayedTaskTermination.queue(validation.sessionHolder, this, false);
            }
        }
    }

    //--//

    public RegistryTaggedImageRecord findTaggedImage(RecordHelper<RegistryImageRecord> helper,
                                                     String matchTag)
    {
        RegistryImageRecord rec_image = imageReference;

        if (rec_image == null)
        {
            rec_image = RegistryImageRecord.findBySha(helper, image);
        }

        if (rec_image != null)
        {
            for (RegistryTaggedImageRecord rec_taggedImage : rec_image.getReferencingTags())
            {
                if (matchTag == null || StringUtils.equals(rec_taggedImage.getTag(), matchTag))
                {
                    return rec_taggedImage;
                }
            }
        }

        return null;
    }

    //--//

    public CompletableFuture<DeployerDockerApi> getDockerProxy(IServiceProvider serviceProvider)
    {
        DeploymentHostRecord rec_host = getDeployment();
        if (rec_host == null)
        {
            return AsyncRuntime.asNull();
        }

        return rec_host.getProxyOrNull(serviceProvider, DeployerDockerApi.class, 100);
    }

    //--//

    @Override
    public DeploymentTask toModelOverride(SessionHolder sessionHolder,
                                          ModelMapperPolicy policy,
                                          DeploymentTask model)
    {
        if (model.imageReference == null)
        {
            RegistryImageRecord rec_image = RegistryImageRecord.findBySha(sessionHolder.createHelper(RegistryImageRecord.class), model.image);
            if (rec_image != null)
            {
                model.imageReference = TypedRecordIdentity.newTypedInstance(rec_image);
            }
        }

        return model;
    }
}
