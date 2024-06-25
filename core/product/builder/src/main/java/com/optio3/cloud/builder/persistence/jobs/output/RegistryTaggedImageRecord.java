/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs.output;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.annotation.Optio3UpgradeValue;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.customer.Customer;
import com.optio3.cloud.builder.model.customer.CustomerService;
import com.optio3.cloud.builder.model.customer.CustomerServiceBackup;
import com.optio3.cloud.builder.model.customer.RoleAndArchitectureWithImage;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentHostStatusDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImageUsage;
import com.optio3.cloud.builder.model.jobs.output.ReleaseStatusReport;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceBackupRecord;
import com.optio3.cloud.builder.persistence.customer.CustomerServiceRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.builder.persistence.jobs.JobRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "REGISTRY_TAGGED_IMAGE")
@DynamicUpdate // Due to HHH-11506
@Optio3TableInfo(externalId = "RegistryTaggedImage", model = RegistryTaggedImage.class, metamodel = RegistryTaggedImageRecord_.class)
public class RegistryTaggedImageRecord extends RecordWithCommonFields implements ModelMapperTarget<RegistryTaggedImage, RegistryTaggedImageRecord_>
{
    /**
     * Bound to this job.
     */
    @Optio3ControlNotifications(reason = "Notify changes", direct = Notify.ALWAYS, reverse = Notify.ALWAYS, getter = "getOwningJob")
    @Optio3Cascade(mode = Optio3Cascade.Flavor.CLEAR, getter = "getOwningJob", setter = "setOwningJob")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "owning_job", foreignKey = @ForeignKey(name = "REGISTRYTAGGEDIMAGE__OWNING_JOB__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobRecord owningJob;

    /**
     * Pointing to this image.
     */
    @Optio3ControlNotifications(reason = "Notify image when tag changes", direct = Notify.ON_ASSOCIATION_CHANGES, reverse = Notify.NEVER, getter = "getImage")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "image", nullable = false, foreignKey = @ForeignKey(name = "IMAGE__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private RegistryImageRecord image;

    @Column(name = "tag", nullable = false)
    private String tag;

    @Optio3UpgradeValue("None")
    @Enumerated(EnumType.STRING)
    @Column(name = "release_status", nullable = false)
    private RegistryImageReleaseStatus releaseStatus;

    //--//

    public RegistryTaggedImageRecord()
    {
    }

    public static RegistryTaggedImageRecord newInstance(JobRecord owningJob,
                                                        RegistryImageRecord image,
                                                        String tag)
    {
        RegistryTaggedImageRecord res = new RegistryTaggedImageRecord();
        res.owningJob     = owningJob;
        res.image         = image;
        res.tag           = tag;
        res.releaseStatus = RegistryImageReleaseStatus.None;
        return res;
    }

    //--//

    public JobRecord getOwningJob()
    {
        return owningJob;
    }

    public void setOwningJob(JobRecord owningJob)
    {
        if (owningJob != null)
        {
            throw Exceptions.newGenericException(InvalidStateException.class, "INTERNAL ERROR: cannot change job owning image %s", getSysId());
        }

        this.owningJob = null;
    }

    public RegistryImageRecord getImage()
    {
        return image;
    }

    public void setImage(RegistryImageRecord image)
    {
        if (this.image != image)
        {
            this.image = image;
        }
    }

    public String getImageSha()
    {
        return image != null ? image.getImageSha() : null;
    }

    public String getTag()
    {
        return tag;
    }

    public RegistryImageReleaseStatus getReleaseStatus()
    {
        return releaseStatus;
    }

    public ZonedDateTime getBuildTime()
    {
        ZonedDateTime buildTime = RegistryImageRecord.parseBuildTime(tag);
        if (buildTime == null)
        {
            buildTime = image.getBuildTime();
        }

        return buildTime;
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<RegistryTaggedImageRecord, RegistryTaggedImage>> applyFilters,
                                    Consumer<RegistryTaggedImage> callback)
    {
        RawQueryHelper<RegistryTaggedImageRecord, RegistryTaggedImage> qh = new RawQueryHelper<>(sessionHolder, RegistryTaggedImageRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addReference(RegistryTaggedImageRecord_.owningJob, JobRecord.class, (obj, val) -> obj.owningJob = val);
        qh.addReference(RegistryTaggedImageRecord_.image, RegistryImageRecord.class, (obj, val) -> obj.image = val);
        qh.addString(RegistryTaggedImageRecord_.tag, (obj, val) -> obj.tag = val);
        qh.addEnum(RegistryTaggedImageRecord_.releaseStatus, RegistryImageReleaseStatus.class, (obj, val) -> obj.releaseStatus = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(RegistryTaggedImage::new, callback);
    }

    public static TypedRecordIdentityList<RegistryTaggedImageRecord> list(RecordHelper<RegistryTaggedImageRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, RegistryTaggedImageRecord_.tag, true);
        });
    }

    public static RegistryTaggedImageRecord findMatch(SessionHolder sessionHolder,
                                                      DeploymentRole role,
                                                      DockerImageArchitecture architecture,
                                                      RegistryImageReleaseStatus status)
    {
        TypedRecordIdentity<RegistryTaggedImageRecord> ri = ReleaseStatusReport.findCompatible(reportReleaseStatus(sessionHolder, status), role, architecture);
        return sessionHolder.fromIdentityOrNull(ri);
    }

    public static List<RegistryTaggedImageRecord> getBatch(RecordHelper<RegistryTaggedImageRecord> helper,
                                                           List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    //--//

    public <T> Optional<T> findLabel(WellKnownDockerImageLabel label)
    {
        return image == null ? Optional.empty() : image.findLabel(label);
    }

    public <T> T findLabelOrDefault(WellKnownDockerImageLabel label,
                                    T defaultValue)
    {
        Optional<T> opt = findLabel(label);
        return opt.orElse(defaultValue);
    }

    public DeploymentRole getTargetService()
    {
        return image == null ? null : image.getTargetService();
    }

    public DockerImageArchitecture getArchitecture()
    {
        return image == null ? null : image.getArchitecture();
    }

    //--//

    public RegistryTaggedImageUsage getUsage(DeploymentGlobalDescriptor globalDescriptor)
    {
        RegistryTaggedImageUsage usage = new RegistryTaggedImageUsage();

        RegistryTaggedImage taggedImage = globalDescriptor.taggedImages.get(getSysId());
        if (taggedImage != null)
        {
            RegistryImage image = taggedImage.rawImage;
            if (image != null)
            {
                usage.image = image;
                usage.tag   = taggedImage.tag;
                usage.isRC  = taggedImage.releaseStatus == RegistryImageReleaseStatus.ReleaseCandidate;
                usage.isRTM = taggedImage.releaseStatus == RegistryImageReleaseStatus.Release;

                for (CustomerService service : globalDescriptor.imageToServices.get(image.sysId))
                {
                    usage.services.add(service.sysId);
                    ensureService(globalDescriptor, usage, service);
                }

                for (CustomerServiceBackup backup : globalDescriptor.imageToBackups.get(image.sysId))
                {
                    usage.backups.add(backup.sysId);
                    ensureBackup(globalDescriptor, usage, backup);
                }

                for (DeploymentTask task : globalDescriptor.imageToTasks.get(image.sysId))
                {
                    usage.tasks.add(task.sysId);
                    usage.lookupTask.put(task.sysId, task);

                    DeploymentHost host = globalDescriptor.hosts.get(task.deployment.sysId);
                    if (host != null)
                    {
                        usage.lookupHost.put(host.sysId, new DeploymentHostStatusDescriptor(host, false));
                    }
                }
            }
        }

        return usage;
    }

    private void ensureBackup(DeploymentGlobalDescriptor globalDescriptor,
                              RegistryTaggedImageUsage usage,
                              CustomerServiceBackup backup)
    {
        usage.lookupBackup.put(backup.sysId, backup);

        CustomerService service = globalDescriptor.getService(backup.customerService);
        if (service != null)
        {
            ensureService(globalDescriptor, usage, service);
        }
    }

    private void ensureService(DeploymentGlobalDescriptor globalDescriptor,
                               RegistryTaggedImageUsage usage,
                               CustomerService service)
    {
        usage.lookupService.put(service.sysId, service);

        Customer customer = globalDescriptor.customers.get(service.customer.sysId);
        if (customer != null)
        {
            usage.lookupCustomer.put(customer.sysId, customer);
        }
    }

    //--//

    public void mark(SessionHolder sessionHolder,
                     RecordHelper<RegistryTaggedImageRecord> helper,
                     RegistryImageReleaseStatus status) throws
                                                        Exception
    {
        DeploymentRole          role = getTargetService();
        DockerImageArchitecture arch = getArchitecture();

        if (role != null && arch != null)
        {
            //
            // Only one image per role can have a certain release status.
            //
            for (RegistryTaggedImageRecord rec_taggedImage : helper.listAll())
            {
                if (rec_taggedImage.getTargetService() == role && rec_taggedImage.getArchitecture() == arch && rec_taggedImage.releaseStatus == status)
                {
                    rec_taggedImage.releaseStatus = RegistryImageReleaseStatus.None;
                }
            }
        }

        releaseStatus = status;
    }

    public static Set<ReleaseStatusReport> reportReleaseStatus(SessionHolder sessionHolder,
                                                               RegistryImageReleaseStatus status)
    {
        DeploymentGlobalDescriptor.Settings settings = new DeploymentGlobalDescriptor.Settings();
        settings.loadImages = true;

        DeploymentGlobalDescriptor globalDescriptor = DeploymentGlobalDescriptor.get(sessionHolder, settings);

        Set<ReleaseStatusReport> results = Sets.newHashSet();

        for (RegistryTaggedImage taggedImage : globalDescriptor.taggedImages.values())
        {
            if (taggedImage.releaseStatus == status)
            {
                RegistryImage image = taggedImage.rawImage;
                if (image != null && image.targetService != null)
                {
                    ReleaseStatusReport res = new ReleaseStatusReport();
                    res.role         = image.targetService;
                    res.architecture = image.architecture;
                    res.image        = RecordIdentity.newTypedInstance(RegistryTaggedImageRecord.class, taggedImage.sysId);

                    results.add(res);
                }
            }
        }

        return results;
    }

    //--//

    public List<CustomerService> findServices(SessionHolder sessionHolder)
    {
        List<CustomerService> lst = Lists.newArrayList();

        CustomerServiceRecord.streamAllRaw(sessionHolder, null, (raw) ->
        {
            for (RoleAndArchitectureWithImage item : CustomerServiceRecord.WellKnownMetadata.role_images.get(raw.decodeMetadata()))
            {
                if (StringUtils.equals(item.image.sysId, getSysId()))
                {
                    lst.add(raw);
                    break;
                }
            }
        });

        return lst;
    }

    public List<CustomerServiceBackup> findBackups(SessionHolder sessionHolder)
    {
        List<CustomerServiceBackup> lst = Lists.newArrayList();

        CustomerServiceBackupRecord.streamAllRaw(sessionHolder, null, (raw) ->
        {
            for (RoleAndArchitectureWithImage item : CustomerServiceBackupRecord.WellKnownMetadata.role_images.get(raw.decodeMetadata()))
            {
                if (StringUtils.equals(item.image.sysId, getSysId()))
                {
                    lst.add(raw);
                    break;
                }
            }
        });

        return lst;
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (image.hasActiveReferencingTasks())
        {
            validation.addFailure("referencingTasks", "Tag '%s' is used by tasks", tag);
        }

        switch (releaseStatus)
        {
            case Release:
                validation.addFailure("build", "Tag '%s' is marked as Release", tag);
                break;

            case ReleaseCandidate:
                validation.addFailure("build", "Tag '%s' is marked as Release Candidate", tag);
                break;
        }

        if (!findServices(validation.sessionHolder).isEmpty())
        {
            validation.addFailure("services", "Tag '%s' is used by services", tag);
        }

        if (!findBackups(validation.sessionHolder).isEmpty())
        {
            validation.addFailure("backups", "Tag '%s' is used by backups", tag);
        }

        if (owningJob != null && !validation.isDeletePending(owningJob))
        {
            validation.addFailure("owningJob", "Tag '%s' is used by job '%s'", tag, owningJob.getName());
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<RegistryTaggedImageRecord> helper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            BuilderConfiguration cfg = validation.getServiceNonNull(BuilderConfiguration.class);

            UserInfo user = cfg.getCredentialForHost(WellKnownSites.dockerRegistry(), true, RoleType.Publisher);

            DockerImageIdentifier imageTag = new DockerImageIdentifier(getTag());

            try (DockerHelper dockerHelper = new DockerHelper(user))
            {
                dockerHelper.deleteManifest(WellKnownSites.dockerRegistryAddress(true), imageTag);
            }
            catch (Throwable t)
            {
                // Ignore registry failures.
            }

            helper.delete(this);
        }
    }
}
