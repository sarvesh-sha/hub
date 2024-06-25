/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.logic;

import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.optio3.cloud.builder.BuilderConfiguration;
import com.optio3.cloud.builder.model.jobs.output.RegistryRefresh;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.db.Optio3DbRateLimiter;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.model.RecordIdentity;
import com.optio3.cloud.model.ValidationResult;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.infra.WellKnownSites;
import com.optio3.infra.directory.RoleType;
import com.optio3.infra.directory.UserInfo;
import com.optio3.infra.docker.DockerHelper;
import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.logging.Logger;
import com.optio3.service.IServiceProvider;
import org.apache.commons.lang3.StringUtils;

public final class RegistryLogic
{
    public static final Logger LoggerInstance = new Logger(RegistryLogic.class);

    private final IServiceProvider m_serviceProvider;

    public RegistryLogic(IServiceProvider serviceProvider)
    {
        m_serviceProvider = serviceProvider;
    }

    public RegistryRefresh refreshImagesFromRegistry() throws
                                                       Exception
    {
        RegistryRefresh      status = new RegistryRefresh();
        BuilderConfiguration cfg    = m_serviceProvider.getServiceNonNull(BuilderConfiguration.class);

        List<DockerHelper.RegistryImage> results;

        //
        // The Nexus server has two Docker registries, one for pushing and one for pulling.
        // The one for pulling also connect with Docker Hub.
        // Here we don't care about the images on Docker Hub, we don't manage them.
        // Instead, we get the catalog from the Push registry and change the tags to be for the Pull registry.
        //
        {
            UserInfo user = cfg.getCredentialForHostOrNull(WellKnownSites.dockerRegistry(), true, RoleType.Publisher);
            if (user != null)
            {
                try (DockerHelper helper = new DockerHelper(user))
                {
                    // Fetch from Push registry.
                    results = helper.analyzeRegistry(WellKnownSites.dockerRegistryAddress(true), true);
                }

                for (DockerHelper.RegistryImage result : results)
                {
                    // Change tags to the Pull registry.
                    result.tag = new DockerImageIdentifier(WellKnownSites.makeDockerImageTagForPull(result.tag));
                }
            }
            else
            {
                user = cfg.getCredentialForHost(WellKnownSites.dockerRegistry(), true, RoleType.Subscriber);

                try (DockerHelper helper = new DockerHelper(user))
                {
                    // Fetch from Push registry.
                    results = helper.analyzeRegistry(WellKnownSites.dockerRegistryAddress(false), true);
                }
            }
        }

        results.sort(Comparator.comparing(x -> x.tag.fullName));

        LoggerInstance.info("Registry contains %d images:", results.size());
        for (DockerHelper.RegistryImage result : results)
        {
            LoggerInstance.info("   %s", result.tag);
        }
        LoggerInstance.info("");

        List<String> deletedImages = Lists.newArrayList();
        List<String> deletedTags   = Lists.newArrayList();

        try (SessionHolder holder = SessionHolder.createWithNewSessionWithTransaction(m_serviceProvider, null, Optio3DbRateLimiter.Normal))
        {
            RecordHelper<RegistryImageRecord>       helperImage       = holder.createHelper(RegistryImageRecord.class);
            RecordHelper<RegistryTaggedImageRecord> helperTaggedImage = holder.createHelper(RegistryTaggedImageRecord.class);

            //
            // Prevent other changes to the image registry.
            //
            helperImage.lockTableUntilEndOfTransaction(20, TimeUnit.SECONDS);

            for (RecordIdentity ri : RegistryImageRecord.list(helperImage))
            {
                RegistryImageRecord rec_image = helperImage.get(ri.sysId);

                if (findImageBySha(results, rec_image) == null)
                {
                    LoggerInstance.info("Discovered a deleted image: %s", rec_image.getImageSha());
                    deletedImages.add(ri.sysId);
                }
                else
                {
                    for (RegistryTaggedImageRecord rec_taggedImage : rec_image.getReferencingTags())
                    {
                        DockerHelper.RegistryImage image = findImageByTag(results, rec_taggedImage);
                        if (image == null)
                        {
                            LoggerInstance.info("Discovered a deleted tag: %s", rec_taggedImage.getTag());
                            deletedTags.add(rec_taggedImage.getSysId());
                        }
                        else if (!StringUtils.equals(rec_image.getImageSha(), image.imageSha))
                        {
                            LoggerInstance.info("Discovered a replaced tag: %s (was: %s, now: %s)", rec_taggedImage.getTag(), rec_image.getImageSha(), image.imageSha);
                            deletedTags.add(rec_taggedImage.getSysId());
                        }
                    }
                }
            }

            for (DockerHelper.RegistryImage result : results)
            {
                status.images++;

                RegistryImageRecord rec_image = RegistryImageRecord.findBySha(helperImage, result.imageSha);
                if (rec_image == null)
                {
                    LoggerInstance.info("Recovered image information from Docker Registry: %s", result.imageSha);
                    rec_image = RegistryImageRecord.newInstance(result.imageSha, result.labels, result.architecture);

                    ZonedDateTime buildTime = rec_image.getBuildTime();
                    if (buildTime != null)
                    {
                        rec_image.setCreatedOn(buildTime);
                        rec_image.setUpdatedOn(buildTime);
                    }

                    helperImage.persist(rec_image);
                    helperImage.flush();

                    status.imagesAdded++;
                }

                String tag = result.tag.getFullName();

                if (rec_image.findExistingTag(tag) == null)
                {
                    LoggerInstance.info("Recovered image tag from Docker Registry: %s (%s)", tag, result.imageSha);
                    RegistryTaggedImageRecord rec_newTaggedImage = RegistryTaggedImageRecord.newInstance(null, rec_image, tag);

                    ZonedDateTime buildTime = rec_newTaggedImage.getBuildTime();
                    if (buildTime != null)
                    {
                        rec_newTaggedImage.setCreatedOn(buildTime);
                        rec_newTaggedImage.setUpdatedOn(buildTime);
                    }

                    helperTaggedImage.persist(rec_newTaggedImage);
                    helperTaggedImage.flush();

                    status.tagsAdded++;
                }
            }

            holder.commit();
        }

        for (String deletedTaggedImage : deletedTags)
        {
            try (ValidationResultsHolder validation = new ValidationResultsHolder(new SessionProvider(m_serviceProvider, null, Optio3DbRateLimiter.Normal), null, false))
            {
                RecordHelper<RegistryTaggedImageRecord> helperTaggedImage = validation.sessionHolder.createHelper(RegistryTaggedImageRecord.class);
                RegistryTaggedImageRecord               rec               = helperTaggedImage.get(deletedTaggedImage);

                rec.remove(validation, helperTaggedImage);

                status.tagsRemoved++;
            }
            catch (InvalidStateException e1)
            {
                LoggerInstance.error("Failed to remove record for deleted tag '%s': %s", deletedTaggedImage, e1.getMessage());
                dumpInvalidState(e1);
            }
            catch (Throwable e)
            {
                LoggerInstance.error("Failed to remove record for deleted tag '%s': %s", deletedTaggedImage, e);
            }
        }

        for (String deletedImage : deletedImages)
        {
            try (ValidationResultsHolder validation = new ValidationResultsHolder(new SessionProvider(m_serviceProvider, null, Optio3DbRateLimiter.Normal), null, false))
            {
                RecordHelper<RegistryImageRecord> helperImage = validation.sessionHolder.createHelper(RegistryImageRecord.class);
                RegistryImageRecord               rec         = helperImage.get(deletedImage);
                List<RegistryTaggedImageRecord>   tags        = rec.getReferencingTags();
                int                               tagsRemoved = tags.size();

                rec.remove(validation, helperImage);

                status.imagesRemoved++;
                status.tagsRemoved += tagsRemoved;
            }
            catch (InvalidStateException e1)
            {
                LoggerInstance.error("Failed to remove record for deleted tag '%s': %s", deletedImage, e1.getMessage());
                dumpInvalidState(e1);
            }
            catch (Throwable e)
            {
                LoggerInstance.error("Failed to remove record for deleted image '%s': %s", deletedImage, e);
            }
        }

        return status;
    }

    private void dumpInvalidState(InvalidStateException e1)
    {
        if (e1.validationErrors != null)
        {
            for (ValidationResult entry : e1.validationErrors.entries)
            {
                LoggerInstance.error("   %s - %s", entry.field, entry.reason);
            }
        }
    }

    private DockerHelper.RegistryImage findImageBySha(List<DockerHelper.RegistryImage> results,
                                                      RegistryImageRecord rec)
    {
        String sha = rec.getImageSha();

        for (DockerHelper.RegistryImage result : results)
        {
            if (result.imageSha.equals(sha))
            {
                return result;
            }
        }

        return null;
    }

    private DockerHelper.RegistryImage findImageByTag(List<DockerHelper.RegistryImage> results,
                                                      RegistryTaggedImageRecord rec)
    {
        String tag = rec.getTag();

        for (DockerHelper.RegistryImage result : results)
        {
            if (result.tag.getFullName()
                          .equals(tag))
            {
                return result;
            }
        }

        return null;
    }
}
