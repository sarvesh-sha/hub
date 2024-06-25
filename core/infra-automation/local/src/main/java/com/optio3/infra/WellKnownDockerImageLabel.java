/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.infra.waypoint.FirmwareHelper;
import com.optio3.util.Base64EncodedValue;
import com.optio3.util.Exceptions;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unchecked")
public enum WellKnownDockerImageLabel
{
    BuildId("Optio3_BuildId")
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    Architecture("Optio3_Architecture")
            {
                @Override
                public DockerImageArchitecture parseValue(String value)
                {
                    DockerImageArchitecture arch = DockerImageArchitecture.parse(value);
                    if (arch == null)
                    {
                        throw Exceptions.newRuntimeException("Unrecognized image architecture: %s", value);
                    }

                    return arch;
                }
            },
    Service("Optio3_TargetService") // Used to tag an image with its purpose
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    ServiceFeatures("Optio3_TargetServiceFeatures") // Used to distinguish different versions of a service
            {
                @Override
                public Set<String> parseValue(String value)
                {
                    return Sets.newHashSet(StringUtils.split(value, ','));
                }
            },
    DatabaseName("Optio3_DbName") // The name of the database
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    DatabaseSchema("Optio3_DbSchema") // The version of the database schema
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    ConfigTemplate("Optio3_ConfigTemplate")
            {
                @Override
                public Base64EncodedValue parseValue(String value)
                {
                    return new Base64EncodedValue(value);
                }
            },
    DeploymentContextId("Optio3_Deployment_ContextId") // Used to tag a running container with its context ID
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    DeploymentInstanceId("Optio3_Deployment_InstanceId") // Used to tag a running container with its ID
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    DeploymentPurpose("Optio3_Deployment_Purpose") // Used to tag a running container with its purpose
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            },
    Other(null)
            {
                @Override
                public String parseValue(String value)
                {
                    return value;
                }
            };

    private final String name;

    WellKnownDockerImageLabel(String name)
    {
        this.name = name;
    }

    public static WellKnownDockerImageLabel parse(String name)
    {
        for (WellKnownDockerImageLabel l : values())
        {
            if (l.name == null || l.name.equals(name))
            {
                return l;
            }
        }

        return null;
    }

    public String getName()
    {
        return name;
    }

    protected abstract <T> T parseValue(String value);

    public <T> Optional<T> findLabel(Map<String, String> labels)
    {
        String value = getValue(labels);
        if (StringUtils.isBlank(value))
        {
            return Optional.empty();
        }

        return Optional.of(parseValue(value));
    }

    public <T> T getLabel(Map<String, String> labels)
    {
        Optional<T> opt = findLabel(labels);
        return opt.orElse(null);
    }

    public <T> void setValue(Map<String, String> labels,
                             T value)
    {
        labels.put(name, value.toString());
    }

    public String getValue(Map<String, String> labels)
    {
        return labels != null ? labels.get(name) : null;
    }
}
