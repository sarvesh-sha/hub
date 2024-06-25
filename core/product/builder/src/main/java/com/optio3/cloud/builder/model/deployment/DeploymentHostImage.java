/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

import com.optio3.infra.docker.DockerImageIdentifier;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public class DeploymentHostImage
{
    public static final String BOOTSTRAP                     = "bootstrap";
    public static final String DEPLOYER_BOOTSTRAP_ARM        = "repo.dev.optio3.io:5000/optio3-deployer-armv7:" + BOOTSTRAP;
    public static final String DEPLOYER_BOOTSTRAP_ARM_LEGACY = "repo.dev.optio3.io:5000/optio3-deployer-armhf:" + BOOTSTRAP;
    public static final String WAYPOINT_BOOTSTRAP_ARM        = "repo.dev.optio3.io:5000/optio3-waypoint-armv7:" + BOOTSTRAP;
    public static final String PROVISIONER_BOOTSTRAP_ARM     = "repo.dev.optio3.io:5000/optio3-provisioner-armv7:" + BOOTSTRAP;

    public String        id;
    public String        tag;
    public long          size;
    public ZonedDateTime created;
    public ZonedDateTime lastRefreshed;
    public ZonedDateTime lastUsed;

    public boolean isStale(int amount,
                           TimeUnit unit)
    {
        return !TimeUtils.wasUpdatedRecently(lastRefreshed, amount, unit);
    }

    public static boolean isBoostrap(String tag)
    {
        if (StringUtils.isBlank(tag))
        {
            return false;
        }

        DockerImageIdentifier id = new DockerImageIdentifier(tag);
        return StringUtils.equals(id.tag, BOOTSTRAP);
    }

    public static String mapToLegacyVersion(String tag)
    {
        if (StringUtils.equals(tag, DEPLOYER_BOOTSTRAP_ARM))
        {
            return DEPLOYER_BOOTSTRAP_ARM_LEGACY;
        }

        return null;
    }

    public static String mapFromLegacyVersion(String tag)
    {
        if (StringUtils.equals(tag, DEPLOYER_BOOTSTRAP_ARM_LEGACY))
        {
            return DEPLOYER_BOOTSTRAP_ARM;
        }

        return null;
    }
}
