/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.Region;
import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Cascade;
import com.optio3.cloud.annotation.Optio3Cascade.Flavor;
import com.optio3.cloud.annotation.Optio3ControlNotifications;
import com.optio3.cloud.annotation.Optio3ControlNotifications.Notify;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.BuilderApplication;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.HostRemoter;
import com.optio3.cloud.builder.model.communication.CrashReport;
import com.optio3.cloud.builder.model.deployment.DeploymentGlobalDescriptor;
import com.optio3.cloud.builder.model.deployment.DeploymentHost;
import com.optio3.cloud.builder.model.deployment.DeploymentTask;
import com.optio3.cloud.builder.model.jobs.Job;
import com.optio3.cloud.builder.model.jobs.JobFilterRequest;
import com.optio3.cloud.builder.model.jobs.JobStatus;
import com.optio3.cloud.builder.model.jobs.JobUsage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryTaggedImageUsage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.config.UserRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryCommitRecord;
import com.optio3.cloud.builder.persistence.jobs.input.RepositoryRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerContainerRecord;
import com.optio3.cloud.builder.persistence.worker.DockerTemporaryImageRecord;
import com.optio3.cloud.builder.persistence.worker.DockerVolumeRecord;
import com.optio3.cloud.builder.persistence.worker.ManagedDirectoryRecord;
import com.optio3.cloud.builder.persistence.worker.RecordWithResources;
import com.optio3.cloud.builder.persistence.worker.RepositoryCheckoutRecord;
import com.optio3.cloud.builder.persistence.worker.TrackedRecordWithResources;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.concurrency.Executors;
import com.optio3.infra.AzureHelper;
import com.optio3.infra.WellKnownSites;
import com.optio3.service.IServiceProvider;
import com.optio3.util.CollectionUtils;
import com.optio3.util.ConfigVariables;
import com.optio3.util.IConfigVariable;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.LazyToOne;
import org.hibernate.annotations.LazyToOneOption;

@Entity
@Table(name = "JOB", indexes = { @Index(columnList = "status"), @Index(columnList = "id_prefix") })
@Optio3TableInfo(externalId = "Job", model = Job.class, metamodel = JobRecord_.class)
public class JobRecord extends RecordWithResources implements ModelMapperTarget<Job, JobRecord_>
{
    private static final ArrayList<JobStatus> c_completedJobs = Lists.newArrayList(JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.CANCELLED);

    public enum ConfigVariable implements IConfigVariable
    {
        Job("JOB"),
        JobId("JOB_ID"),
        Branch("BRANCH"),
        Commit("COMMIT"),
        Url("URL"),
        Timestamp("TIME"),
        CrashSite("CRASH_SITE"),
        CrashPage("CRASH_PAGE"),
        CrashUser("CRASH_USER"),
        CrashStack("CRASH_STACK");

        private final String m_variable;

        ConfigVariable(String variable)
        {
            m_variable = variable;
        }

        public String getVariable()
        {
            return m_variable;
        }
    }

    private static final ConfigVariables.Validator<ConfigVariable> s_configValidator         = new ConfigVariables.Validator<>(ConfigVariable.values());
    private static final ConfigVariables.Template<ConfigVariable>  s_template_buildCompleted = s_configValidator.newTemplate(JobRecord.class, "emails/build_completed.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_buildFailed    = s_configValidator.newTemplate(JobRecord.class, "emails/build_failed.txt", "${", "}");
    private static final ConfigVariables.Template<ConfigVariable>  s_template_crashReport    = s_configValidator.newTemplate(JobRecord.class, "emails/crash_report.txt", "${", "}");

    //--//

    @Optio3ControlNotifications(reason = "Ignore changes", direct = Notify.NEVER, reverse = Notify.NEVER)
    @Optio3Cascade(mode = Flavor.DELETE, getter = "getDefinition")
    @ManyToOne(fetch = FetchType.LAZY)
    @LazyToOne(LazyToOneOption.PROXY)
    @JoinColumn(name = "definition", foreignKey = @ForeignKey(name = "DEFINITION__FK"))
    // Lazy fields have to be loaded before setting them again, or Hibernate will miss the update if you try to null the field.
    private JobDefinitionRecord definition;

    @Column(name = "name", nullable = false)
    private String name;

    // It's supposed to be a @NaturalId, but Hibernate doesn't allow them in subclasses.
    @Column(name = "id_prefix", nullable = false)
    private String idPrefix;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "branch")
    private String branch;

    @Column(name = "commit")
    private String commit;

    @Column(name = "triggered_by")
    private String triggeredBy;

    //--//

    /**
     * List of all the steps in this job.
     */
    @OneToMany(mappedBy = "owningJob", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<JobSourceRecord> sources;

    /**
     * List of all the steps in this job.
     */
    @OneToMany(mappedBy = "owningJob", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    @OrderBy("sys_created_on")
    private List<JobStepRecord> steps;

    /**
     * List of all the images pushed to the Docker Registry.
     */
    @OneToMany(mappedBy = "owningJob", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RegistryTaggedImageRecord> generatedImages;

    /**
     * List of all the resources acquired by this job.
     */
    @OneToMany(mappedBy = "acquiredBy", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<TrackedRecordWithResources> acquiredResources;

    //--//

    public JobRecord()
    {
    }

    public static JobRecord newInstance(JobDefinitionRecord jobDef,
                                        String branch,
                                        String commit,
                                        UserRecord user)
    {
        requireNonNull(jobDef);

        JobRecord res = new JobRecord();
        res.definition = jobDef;
        res.status     = JobStatus.CREATED;
        res.branch     = branch;
        res.commit     = commit;

        if (user != null)
        {
            res.triggeredBy = user.getEmailAddress();
        }

        return res;
    }

    //--//

    public JobDefinitionRecord getDefinition()
    {
        return definition;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getIdPrefix()
    {
        return idPrefix;
    }

    public void setIdPrefix(String idPrefix)
    {
        this.idPrefix = idPrefix;
    }

    public JobStatus getStatus()
    {
        return status;
    }

    public void setStatus(JobStatus status)
    {
        this.status = status;
    }

    public boolean conditionallyChangeStatus(JobStatus expectedStatus,
                                             JobStatus status)
    {
        if (this.status == expectedStatus)
        {
            this.status = status;
            return true;
        }

        return false;
    }

    public String getBranch()
    {
        return branch;
    }

    public String getCommit()
    {
        return commit;
    }

    public String getTriggeredBy()
    {
        return triggeredBy;
    }

    //--//

    public List<JobSourceRecord> getSources()
    {
        return CollectionUtils.asEmptyCollectionIfNull(sources);
    }

    public List<JobStepRecord> getSteps()
    {
        return CollectionUtils.asEmptyCollectionIfNull(steps);
    }

    //--//

    public void sendEmail(IServiceProvider serviceProvider)
    {
        BuilderApplication app = serviceProvider.getServiceNonNull(BuilderApplication.class);

        if (triggeredBy != null)
        {
            ConfigVariables<ConfigVariable> parameters;
            String                          subject;

            switch (status)
            {
                case COMPLETED:
                    subject = "Build completed";
                    parameters = s_template_buildCompleted.allocate();
                    break;

                default:
                case FAILED:
                    subject = "Build failed";
                    parameters = s_template_buildFailed.allocate();
                    break;
            }

            parameters.setValue(ConfigVariable.Job, name);
            parameters.setValue(ConfigVariable.JobId, idPrefix);
            parameters.setValue(ConfigVariable.Branch, branch);
            parameters.setValue(ConfigVariable.Commit, commit);
            parameters.setValue(ConfigVariable.Url, "https://" + WellKnownSites.builderServer() + "/#/jobs/item/" + getSysId());
            parameters.setValue(ConfigVariable.Timestamp, getCreatedOn());

            app.sendEmailNotification(triggeredBy, subject, parameters);
        }
    }

    public void sendCrashReport(IServiceProvider serviceProvider,
                                CrashReport crashReport)
    {
        BuilderApplication   app = serviceProvider.getServiceNonNull(BuilderApplication.class);
        BuilderConfiguration cfg = serviceProvider.getServiceNonNull(BuilderConfiguration.class);

        ConfigVariables<ConfigVariable> parameters = s_template_crashReport.allocate();

        parameters.setValue(ConfigVariable.Job, name);
        parameters.setValue(ConfigVariable.JobId, idPrefix);
        parameters.setValue(ConfigVariable.Branch, branch);
        parameters.setValue(ConfigVariable.Commit, commit);
        parameters.setValue(ConfigVariable.Url, "https://" + WellKnownSites.builderServer() + "/#/jobs/item/" + getSysId());
        parameters.setValue(ConfigVariable.Timestamp, crashReport.timestamp);
        parameters.setValue(ConfigVariable.CrashSite, crashReport.site);
        parameters.setValue(ConfigVariable.CrashPage, crashReport.page);
        parameters.setValue(ConfigVariable.CrashUser, crashReport.user);
        parameters.setValue(ConfigVariable.CrashStack, crashReport.stack);

        app.sendEmailNotification(cfg.emailForWarnings, "Crash Report", parameters);
    }

    public List<TrackedRecordWithResources> getAcquiredResources()
    {
        return CollectionUtils.asEmptyCollectionIfNull(acquiredResources);
    }

    public <T extends TrackedRecordWithResources> List<T> getAcquiredResources(Class<T> clz)
    {
        List<T> list = Lists.newArrayList();

        for (TrackedRecordWithResources resource : getAcquiredResources())
        {
            T typedResource = SessionHolder.asEntityOfClassOrNull(resource, clz);
            if (typedResource != null)
            {
                list.add(typedResource);
            }
        }

        return list;
    }

    public List<DockerContainerRecord> getAcquiredContainers()
    {
        return getAcquiredResources(DockerContainerRecord.class);
    }

    public List<DockerVolumeRecord> getAcquiredVolumes()
    {
        return getAcquiredResources(DockerVolumeRecord.class);
    }

    public List<DockerTemporaryImageRecord> getAcquiredTemporaryImages()
    {
        return getAcquiredResources(DockerTemporaryImageRecord.class);
    }

    public List<ManagedDirectoryRecord> getAcquiredDirectories()
    {
        return getAcquiredResources(ManagedDirectoryRecord.class);
    }

    public List<RepositoryCheckoutRecord> getAcquiredCheckouts()
    {
        return getAcquiredResources(RepositoryCheckoutRecord.class);
    }

    //--//

    public List<RegistryTaggedImageRecord> getGeneratedImages()
    {
        return CollectionUtils.asEmptyCollectionIfNull(generatedImages);
    }

    public JobUsage getUsage(SessionHolder sessionHolder,
                             DeploymentGlobalDescriptor globalDescriptor)
    {
        JobUsage jobUsage = new JobUsage();
        jobUsage.name      = getName();
        jobUsage.createdOn = getCreatedOn();

        for (JobSourceRecord source : getSources())
        {
            String commit = source.getCommit();
            if (commit != null)
            {
                RepositoryRecord repo = source.getRepo();
                if (repo != null)
                {
                    final RecordHelper<RepositoryCommitRecord> helper = sessionHolder.createHelper(RepositoryCommitRecord.class);

                    RepositoryCommitRecord rec_commit = repo.findCommitByHash(helper, commit);
                    if (rec_commit != null)
                    {
                        jobUsage.descShort = jobUsage.desc = rec_commit.getMessage();

                        if (jobUsage.descShort.length() > 50)
                        {
                            jobUsage.descShort = jobUsage.descShort.substring(0, 50) + "...";
                        }
                    }
                }
            }
        }

        for (RegistryTaggedImageRecord rec_taggedImage : getGeneratedImages())
        {
            RegistryTaggedImageUsage usage = rec_taggedImage.getUsage(globalDescriptor);
            if (usage.isInUse(false))
            {
                jobUsage.addImage(usage);

                RegistryImage  image = globalDescriptor.images.get(usage.image.sysId);
                DeploymentRole role  = image.targetService;
                if (role != null)
                {
                    for (DeploymentTask task : globalDescriptor.imageToTasks.get(usage.image.sysId))
                    {
                        DeploymentHost host = globalDescriptor.getHost(task.deployment);
                        if (host != null)
                        {
                            jobUsage.addHostInRole(role, host.sysId);

                            if (host.customerService != null && host.hasRole(role))
                            {
                                jobUsage.addServiceInRole(role, host.customerService.sysId);
                            }
                        }
                    }
                }
            }
        }

        return jobUsage;
    }

    //--//

    public static String getDisplayName(JobRecord job)
    {
        if (job == null)
        {
            return "<no job>";
        }

        final String sysId = job.getSysId();
        final String name  = job.getName();
        return name != null ? String.format("%s [%s]", sysId, name) : sysId;
    }

    public static TypedRecordIdentityList<JobRecord> list(RecordHelper<JobRecord> helper,
                                                          JobFilterRequest filters)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, RecordWithCommonFields_.createdOn, false);

            if (filters != null)
            {
                if (filters.executing)
                {
                    jh.addWhereClauseNotIn(jh.root, JobRecord_.status, c_completedJobs);
                }

                jh.filterTimestampsCoveredByTargetRange(jh.root, RecordWithCommonFields_.createdOn, filters.after, filters.before);
            }
        });
    }

    public static List<JobRecord> getBatch(RecordHelper<JobRecord> helper,
                                           List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static JobRecord findByPrefix(RecordHelper<JobRecord> helper,
                                         String prefix)
    {
        TypedRecordIdentity<JobRecord> ri = QueryHelperWithCommonFields.single(helper, (jh) ->
        {
            jh.addWhereClauseWithEqual(jh.root, JobRecord_.idPrefix, prefix);
        });

        return ri != null ? helper.get(ri.sysId) : null;
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        validation.markAsPendingDelete(this);

        for (RegistryTaggedImageRecord recImage : getGeneratedImages())
        {
            recImage.checkRemoveConditions(validation);
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<JobRecord> helper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            JobDefinitionRecord rec_def = getDefinition();
            if (rec_def != null)
            {
                for (JobDefinitionStepRecord step : rec_def.getSteps())
                {
                    if (step.requiresCdn())
                    {
                        // Remove in background, to avoid blocking caller.
                        Executors.getDefaultThreadPool()
                                 .execute(() -> callWithCdnHelper(validation, AzureHelper.CdnHelper::deleteContents));
                        break;
                    }
                }
            }

            helper.delete(this);
        }
    }

    public void callWithCdnHelper(IServiceProvider serviceProvider,
                                  BiConsumer<AzureHelper.CdnHelper, String> callback)
    {
        Executors.callWithAutoRetry(3, 100, () ->
        {
            BuilderConfiguration cfg = serviceProvider.getServiceNonNull(BuilderConfiguration.class);

            if (cfg.credentials != null)
            {
                try (AzureHelper azure = AzureHelper.buildCachedWithDirectoryLookup(cfg.credentials, WellKnownSites.optio3DomainName(), AzureEnvironment.AZURE, Region.US_WEST))
                {
                    AzureHelper.CdnHelper cdnHelper     = azure.getCdnHelper();
                    String                containerName = cdnHelper.normalizeContainerName(getIdPrefix());

                    callback.accept(cdnHelper, containerName);
                }
            }
        });
    }

    //--//

    public void releaseResources(HostRemoter remoter,
                                 SessionHolder sessionHolder,
                                 boolean force) throws
                                                Exception
    {
        releaseChildResources(remoter, sessionHolder, getAcquiredResources());
    }

    private <T extends TrackedRecordWithResources> void releaseChildResources(HostRemoter remoter,
                                                                              SessionHolder sessionHolder,
                                                                              List<T> list) throws
                                                                                            Exception
    {
        for (T child : Lists.newArrayList(list))
        {
            child.release(remoter, sessionHolder);
        }
    }

    @Override
    protected void checkConditionsForFreeingResourcesInner(ValidationResultsHolder validation)
    {
        checkConditionsForFreeingResources(getSteps(), validation);
    }

    @Override
    protected void freeResourcesInner(HostRemoter remoter,
                                      ValidationResultsHolder validation) throws
                                                                          Exception
    {
        freeChildResources(remoter, validation, getSteps());
    }

    @Override
    protected List<? extends RecordWithResources> deleteRecursivelyInner(HostRemoter remoter,
                                                                         ValidationResultsHolder validation)
    {
        // Nothing to do.
        return null;
    }
}
