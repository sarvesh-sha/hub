/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.persistence.jobs.output;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.cloud.annotation.Optio3TableInfo;
import com.optio3.cloud.builder.model.jobs.output.RegistryImage;
import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentTaskRecord;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.MetadataField;
import com.optio3.cloud.persistence.ModelMapperTarget;
import com.optio3.cloud.persistence.QueryHelperWithCommonFields;
import com.optio3.cloud.persistence.RawQueryHelper;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordWithCommonFields_;
import com.optio3.cloud.persistence.RecordWithMetadata;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.util.CollectionUtils;
import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "REGISTRY_IMAGE")
@Optio3TableInfo(externalId = "RegistryImage", model = RegistryImage.class, metamodel = RegistryImageRecord_.class, metadata = RegistryImageRecord.WellKnownMetadata.class)
public class RegistryImageRecord extends RecordWithMetadata implements ModelMapperTarget<RegistryImage, RegistryImageRecord_>
{
    public static class WellKnownMetadata implements Optio3TableInfo.IMetadataDigest
    {
        public static final MetadataField<Map<String, String>> labels = new MetadataField<>("labels", MetadataField.TypeRef_mapOfStrings, Maps::newHashMap);
    }

    //--//

    private static final Pattern s_buildIdPattern = Pattern.compile("_(\\d\\d\\d\\d)(\\d\\d)(\\d\\d)_(\\d\\d)(\\d\\d)$");

    //--//

    @NaturalId
    @Column(name = "image_sha", nullable = false)
    private String imageSha;

    @Enumerated(EnumType.STRING)
    @Column(name = "architecture", nullable = false)
    private DockerImageArchitecture architecture;

    @Column(name = "build_time")
    private ZonedDateTime buildTime;

    @Column(name = "target_service")
    @Enumerated(EnumType.STRING)
    private DeploymentRole targetService;

    //--//

    /**
     * List of all the tags using this image.
     */
    @OneToMany(mappedBy = "image", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<RegistryTaggedImageRecord> referencingTags;

    //--//

    /**
     * List of all the tasks using this image.
     */
    @OneToMany(mappedBy = "imageReference", fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.EXTRA)
    private List<DeploymentTaskRecord> referencingTasks;

    //--//

    public RegistryImageRecord()
    {
    }

    public static RegistryImageRecord newInstance(String imageSha,
                                                  Map<String, String> labels,
                                                  DockerImageArchitecture arch)
    {
        RegistryImageRecord res = new RegistryImageRecord();
        res.imageSha     = imageSha;
        res.architecture = arch;

        if (labels != null)
        {
            res.updateLabels(labels);
        }

        res.refreshBuildTime();
        res.refreshTargetService();

        return res;
    }

    //--//

    public String getImageSha()
    {
        return imageSha;
    }

    public DockerImageArchitecture getArchitecture()
    {
        return architecture;
    }

    public Map<String, String> getLabels()
    {
        return getMetadata(WellKnownMetadata.labels);
    }

    public void updateLabels(Map<String, String> labels)
    {
        putMetadata(WellKnownMetadata.labels, labels);
    }

    //--//

    public List<RegistryTaggedImageRecord> getReferencingTags()
    {
        return CollectionUtils.asEmptyCollectionIfNull(referencingTags);
    }

    public List<DeploymentTaskRecord> getReferencingTasks()
    {
        return CollectionUtils.asEmptyCollectionIfNull(referencingTasks);
    }

    public boolean hasActiveReferencingTasks()
    {
        for (DeploymentTaskRecord rec_task : getReferencingTasks())
        {
            DeploymentHostRecord rec_host = rec_task.getDeployment();
            if (rec_host == null)
            {
                continue;
            }

            if (rec_host.getOperationalStatus()
                        .canIgnoreOldTasks())
            {
                continue;
            }

            return true;
        }

        return false;
    }

    public ZonedDateTime getBuildTime()
    {
        return buildTime;
    }

    private void refreshBuildTime()
    {
        Optional<String> buildIdOpt = findLabel(WellKnownDockerImageLabel.BuildId);
        if (buildIdOpt.isPresent())
        {
            buildTime = parseBuildTime(buildIdOpt.get());
        }
        else
        {
            buildTime = null;
        }
    }

    public static ZonedDateTime parseBuildTime(String buildId)
    {
        Matcher matcher = s_buildIdPattern.matcher(buildId);
        if (matcher.find())
        {
            int year   = Integer.parseInt(matcher.group(1));
            int month  = Integer.parseInt(matcher.group(2));
            int day    = Integer.parseInt(matcher.group(3));
            int hour   = Integer.parseInt(matcher.group(4));
            int minute = Integer.parseInt(matcher.group(5));

            return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.systemDefault());
        }

        return null;
    }

    //--//

    public DeploymentRole getTargetService()
    {
        return targetService;
    }

    private void refreshTargetService()
    {
        Optional<String> purposeOpt = findLabel(WellKnownDockerImageLabel.Service);
        if (purposeOpt.isPresent())
        {
            String purposeText = purposeOpt.get();
            targetService = DeploymentRole.parse(purposeText);
        }
        else
        {
            targetService = null;
        }
    }

    //--//

    public static void streamAllRaw(SessionHolder sessionHolder,
                                    Consumer<RawQueryHelper<RegistryImageRecord, RegistryImage>> applyFilters,
                                    Consumer<RegistryImage> callback)
    {
        RawQueryHelper<RegistryImageRecord, RegistryImage> qh = new RawQueryHelper<>(sessionHolder, RegistryImageRecord.class);

        qh.addString(RecordWithCommonFields_.sysId, (obj, val) -> obj.sysId = val);
        qh.addDate(RecordWithCommonFields_.createdOn, (obj, val) -> obj.createdOn = val);
        qh.addDate(RecordWithCommonFields_.updatedOn, (obj, val) -> obj.updatedOn = val);

        qh.addString(RegistryImageRecord_.imageSha, (obj, val) -> obj.imageSha = val);
        qh.addEnum(RegistryImageRecord_.architecture, DockerImageArchitecture.class, (obj, val) -> obj.architecture = val);
        qh.addDate(RegistryImageRecord_.buildTime, (obj, val) -> obj.buildTime = val);
        qh.addEnum(RegistryImageRecord_.targetService, DeploymentRole.class, (obj, val) -> obj.targetService = val);

        if (applyFilters != null)
        {
            applyFilters.accept(qh);
        }

        qh.stream(RegistryImage::new, callback);
    }

    public static TypedRecordIdentityList<RegistryImageRecord> list(RecordHelper<RegistryImageRecord> helper)
    {
        return QueryHelperWithCommonFields.list(helper, (jh) ->
        {
            jh.addOrderBy(jh.root, RegistryImageRecord_.imageSha, true);
        });
    }

    public static List<RegistryImageRecord> getBatch(RecordHelper<RegistryImageRecord> helper,
                                                     List<String> ids)
    {
        return QueryHelperWithCommonFields.getBatch(helper, ids);
    }

    public static RegistryImageRecord findBySha(RecordHelper<RegistryImageRecord> helper,
                                                String imageSha)
    {
        return helper.byNaturalId()
                     .using(RegistryImageRecord_.imageSha.getName(), imageSha)
                     .load();
    }

    //--//

    public RegistryTaggedImageRecord findExistingTag(String tag)
    {
        for (RegistryTaggedImageRecord rec_taggedImage : getReferencingTags())
        {
            if (rec_taggedImage.getTag()
                               .equals(tag))
            {
                return rec_taggedImage;
            }
        }

        return null;
    }

    public <T> Optional<T> findLabel(WellKnownDockerImageLabel label)
    {
        return label.findLabel(getLabels());
    }

    public <T> T getLabel(WellKnownDockerImageLabel label)
    {
        return label.getLabel(getLabels());
    }

    //--//

    public void checkRemoveConditions(ValidationResultsHolder validation)
    {
        if (hasActiveReferencingTasks())
        {
            validation.addFailure("referencingTasks", "Image '%s' is used by tasks", getImageSha());
        }

        for (RegistryTaggedImageRecord rec_subTaggedImage : getReferencingTags())
        {
            rec_subTaggedImage.checkRemoveConditions(validation);
        }
    }

    public void remove(ValidationResultsHolder validation,
                       RecordHelper<RegistryImageRecord> helper)
    {
        checkRemoveConditions(validation);

        if (validation.canProceed())
        {
            for (RegistryTaggedImageRecord rec_subTaggedImage : Lists.newArrayList(getReferencingTags()))
            {
                rec_subTaggedImage.remove(validation, helper.wrapFor(RegistryTaggedImageRecord.class));
            }

            helper.delete(this);
        }
    }
}
