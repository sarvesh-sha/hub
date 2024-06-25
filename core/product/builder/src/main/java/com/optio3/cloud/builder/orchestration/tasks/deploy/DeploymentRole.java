/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.orchestration.tasks.deploy;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;

public enum DeploymentRole
{
    // Lowercase for backward compatibility...
    // @formatter:off
    test           (false, false, 0),
    builder        (true , false, 1),
    deployer       (false, true , 2),
    database       (true , false, 3),
    database_backup(true , false, 4),
    hub            (true , false, 5),
    gateway        (false, true , 6),
    prober         (false, true , 7), // Retired, but the position is still valid in the DB.
    waypoint       (false, true , 8),
    tracker        (false, false, 9),
    reporter       (true , false, 10),
    provisioner    (false, true , 11),
    bridge_BACnetP2(false, true , 12); // Retired, but the position is still valid in the DB.
    // @formatter:on

    private static final ConcurrentMap<Long, Set<DeploymentRole>> s_lookup = Maps.newConcurrentMap();

    public final  boolean cloudBased;
    public final  boolean asyncImagePull;
    private final long    m_mask;

    DeploymentRole(boolean cloudBased,
                   boolean asyncImagePull,
                   int maskPosition)
    {
        this.cloudBased     = cloudBased;
        this.asyncImagePull = asyncImagePull;

        m_mask = 1L << maskPosition;
    }

    public static DeploymentRole parse(String value)
    {
        for (DeploymentRole v : values())
        {
            if (StringUtils.equals(v.name(), value))
            {
                return v;
            }
        }

        return null;
    }

    public static Set<DeploymentRole> mapFrom(long roleIds)
    {
        Set<DeploymentRole> res = s_lookup.get(roleIds);
        if (res == null)
        {
            res = Sets.newHashSet();

            for (DeploymentRole role : DeploymentRole.values())
            {
                if (role.isActive(roleIds))
                {
                    res.add(role);
                }
            }

            res = Collections.unmodifiableSet(res);
            s_lookup.put(roleIds, res);
        }

        return res;
    }

    public static long mapTo(Collection<DeploymentRole> roles)
    {
        long roleIds = 0;

        if (roles != null)
        {
            for (DeploymentRole role : roles)
            {
                roleIds |= role.m_mask;
            }
        }

        return roleIds;
    }

    public boolean isActive(long bitField)
    {
        return (bitField & m_mask) != 0;
    }

    public long add(long bitField)
    {
        return bitField | m_mask;
    }

    public long remove(long bitField)
    {
        return bitField & ~m_mask;
    }
}
