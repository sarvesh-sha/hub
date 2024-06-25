/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import static com.optio3.asyncawait.CompileTime.await;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.optio3.cloud.builder.logic.deploy.DeployLogicForAgent;
import com.optio3.cloud.builder.model.deployment.DeploymentHostImage;
import com.optio3.cloud.builder.model.jobs.output.RegistryImageReleaseStatus;
import com.optio3.cloud.builder.persistence.BackgroundActivityRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentAgentRecord;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryImageRecord;
import com.optio3.cloud.builder.persistence.jobs.output.RegistryTaggedImageRecord;
import com.optio3.cloud.client.deployer.model.ContainerStatus;
import com.optio3.cloud.client.deployer.model.ImageStatus;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.exception.InvalidStateException;
import com.optio3.cloud.logic.BackgroundActivityMethod;
import com.optio3.cloud.persistence.MetadataMap;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.infra.WellKnownDockerImageLabel;
import com.optio3.logging.Severity;
import com.optio3.serialization.ObjectMappers;
import com.optio3.util.CollectionUtils;
import com.optio3.util.Exceptions;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class TaskForImagesPruning extends BaseTaskForAgent
{
    public int daysToKeep = 30;

    public static class ImageClassification
    {
        public ImageStatus target;
        public String      service;
        public boolean     isMarked;
        public boolean     isBootstrap;
        public boolean     isCandidate;
    }

    public static BackgroundActivityRecord scheduleTask(RecordLocked<DeploymentHostRecord> lock_targetHost,
                                                        int daysToKeep) throws
                                                                        Exception
    {
        Exceptions.requireNotNull(lock_targetHost, InvalidArgumentException.class, "No host provided");

        DeploymentHostRecord  targetHost      = lock_targetHost.get();
        String                hostId          = targetHost.getHostId();
        DeploymentAgentRecord rec_agentActive = targetHost.findActiveAgent();
        Exceptions.requireNotNull(rec_agentActive, InvalidStateException.class, "No active agent on host '%s'", hostId);

        return BaseDeployTask.scheduleActivity(lock_targetHost, null, TaskForImagesPruning.class, (t) ->
        {
            t.daysToKeep = daysToKeep;
        });
    }

    //--//

    @Override
    public void configureContext()
    {
        loggerInstance = DeploymentHostRecord.buildContextualLogger(loggerInstance, getTargetHostLocator());
    }

    @Override
    public String getTitle()
    {
        return String.format("Prune old and unused Docker Images on host '%s', %d days threshold", getHostDisplayName(), daysToKeep);
    }

    @BackgroundActivityMethod(autoRetry = true)
    public CompletableFuture<Void> process() throws
                                             Exception
    {
        DeployLogicForAgent     agentLogic = getLogicForAgent();
        final List<ImageStatus> images     = await(agentLogic.listImages(false));
        if (images != null)
        {
            Set<String> imagesWithReleaseStatus = Sets.newHashSet();

            callInReadOnlySession(sessionHolder ->
                                  {
                                      RecordHelper<RegistryTaggedImageRecord> helper = sessionHolder.createHelper(RegistryTaggedImageRecord.class);

                                      for (RegistryTaggedImageRecord rec_taggedImage : helper.listAll())
                                      {
                                          if (rec_taggedImage.getReleaseStatus() != RegistryImageReleaseStatus.None)
                                          {
                                              RegistryImageRecord rec_image = rec_taggedImage.getImage();

                                              imagesWithReleaseStatus.add(rec_image.getImageSha());
                                          }
                                      }
                                  });

            //--//

            MetadataMap metadata = withLocatorReadonly(getTargetHostLocator(), (sessionHolder, rec_host) ->
            {
                return rec_host.getMetadata();
            });

            DeploymentHostRecord.AgentStatistics stats = DeploymentHostRecord.WellKnownMetadata.imagesHistory.get(metadata);

            if (loggerInstance.isEnabled(Severity.Debug))
            {
                if (stats == null)
                {
                    loggerInstance.debug("Host %s: no history", getHostDisplayName());

                    for (String key : metadata.keySet())
                    {
                        loggerInstance.debug("  Metadata: %s = %s", key, ObjectMappers.prettyPrintAsJson(metadata.getObject(key, JsonNode.class)));
                    }
                }
                else
                {
                    stats.imageTimestamp.forEach((k, v) -> loggerInstance.debug("Host %s: history: %s seen on %s", getHostDisplayName(), k, v));
                }
            }

            if (stats == null)
            {
                stats = new DeploymentHostRecord.AgentStatistics();
            }

            stats.pruneDeletedImages(images);

            List<ImageClassification> classifiedImages = CollectionUtils.transformToList(images, (img) -> classify(imagesWithReleaseStatus, img));

            if (CollectionUtils.findFirst(classifiedImages, (ic) -> ic.isCandidate) != null) // At least one candidate, check running tasks.
            {
                ZonedDateTime now       = TimeUtils.now();
                ZonedDateTime threshold = now.minus(daysToKeep, ChronoUnit.DAYS);

                for (ContainerStatus container : await(agentLogic.listContainers()))
                {
                    loggerInstance.debug("Host %s: mark image in use: %s", getHostDisplayName(), container.image);
                    stats.markAsInUse(now, container.image);

                    for (ImageClassification ic : classifiedImages)
                    {
                        if (StringUtils.equals(ic.target.id, container.image))
                        {
                            // In use, not a candidate.
                            ic.isCandidate = false;
                        }
                    }
                }

                Map<String, List<ImageClassification>> lookupByService = Maps.newHashMap();

                for (ImageClassification ic : classifiedImages)
                {
                    if (ic.service != null)
                    {
                        List<ImageClassification> lst = lookupByService.computeIfAbsent(ic.service, (k) -> Lists.newArrayList());
                        lst.add(ic);
                    }
                }

                final DeploymentHostRecord.AgentStatistics finalStats = stats;

                for (String service : lookupByService.keySet())
                {
                    List<ImageClassification> lst = lookupByService.get(service);

                    lst.sort((a, b) ->
                             {
                                 // Put candidates after non-candidates.
                                 if (a.isCandidate != b.isCandidate)
                                 {
                                     return a.isCandidate ? 1 : -1;
                                 }

                                 var tA = finalStats.imageTimestamp.get(a.target.id);
                                 var tB = finalStats.imageTimestamp.get(b.target.id);

                                 return TimeUtils.compare(tB, tA); // Sort from most to least recently used.
                             });

                    Set<String> kept = Sets.newHashSet();

                    for (ImageClassification ic : lst)
                    {
                        ImageStatus imageStatus = ic.target;
                        boolean     remove;

                        if (!ic.isCandidate)
                        {
                            // This image should be kept.
                            remove = false;
                        }
                        else if (kept.contains(imageStatus.id))
                        {
                            // Already processed, keep.
                            remove = false;
                        }
                        else if (stats.isStale(now, threshold, imageStatus.id))
                        {
                            // Keep only if it's the only version we have.
                            remove = !kept.isEmpty();
                        }
                        else
                        {
                            // Only keep two versions at most.
                            remove = kept.size() >= 2;
                        }

                        if (remove)
                        {
                            loggerInstance.info("Host %s: removing stale image %s (tags: %s)", getHostDisplayName(), imageStatus.id, imageStatus.repoTags);
                            for (String repoTag : imageStatus.repoTags)
                            {
                                await(agentLogic.removeImage(repoTag, false));
                            }
                            await(agentLogic.removeImage(imageStatus.id, false));
                        }
                        else
                        {
                            loggerInstance.debug("Host %s: keeping image %s (tags: %s), last use on %s", getHostDisplayName(), imageStatus.id, imageStatus.repoTags, stats.imageTimestamp);
                            kept.add(imageStatus.id);
                        }
                    }
                }
            }

            if (loggerInstance.isEnabled(Severity.Debug))
            {
                loggerInstance.debug("Host %s: new history:", getHostDisplayName());
                stats.imageTimestamp.forEach((k, v) -> loggerInstance.debug("Host %s: new history: %s seen on %s", getHostDisplayName(), k, v));
            }

            DeploymentHostRecord.WellKnownMetadata.imagesHistory.put(metadata, stats);

            lockedWithLocator(getTargetHostLocator(), 2, TimeUnit.MINUTES, (sessionHolder, lock_host) ->
            {
                DeploymentHostRecord rec_host = lock_host.get();
                rec_host.setMetadata(metadata);
            });
        }

        return markAsCompleted();
    }

    private ImageClassification classify(Set<String> imagesWithReleaseStatus,
                                         ImageStatus image)
    {
        ImageClassification ic = new ImageClassification();
        ic.target      = image;
        ic.isCandidate = true;

        if (loggerInstance.isEnabled(Severity.Debug))
        {
            loggerInstance.debug("Host %s: Checking image: %s", getHostDisplayName(), ObjectMappers.prettyPrintAsJson(image));
        }

        ic.service = image.labels != null ? image.labels.get(WellKnownDockerImageLabel.Service.getName()) : null;
        if (ic.service == null)
        {
            loggerInstance.debug("Host %s: Skipping image '%s', no metadata", getHostDisplayName(), image.id);
            ic.isCandidate = false;
        }

        ic.isMarked = imagesWithReleaseStatus.contains(image.id);

        if (ic.isMarked)
        {
            // Don't remove images with a Release status.
            loggerInstance.debug("Host %s: Skipping image '%s', it has Release Status", getHostDisplayName(), image.id);
            ic.isCandidate = false;
        }

        if (image.repoTags != null)
        {
            for (String repoTag : image.repoTags)
            {
                if (StringUtils.equals(repoTag, "<none>:<none>"))
                {
                    // Untagged image, ready to be purged.
                    continue;
                }

                if (DeploymentHostImage.isBoostrap(repoTag))
                {
                    // Don't remove bootstrap images.
                    loggerInstance.debug("Host %s: Skipping image '%s', marked as Bootstrap", getHostDisplayName(), image.id);
                    ic.isBootstrap = true;
                    ic.isCandidate = false;
                }
            }
        }

        return ic;
    }
}
