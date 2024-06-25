/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra;

import com.optio3.infra.docker.DockerImageIdentifier;

public class WellKnownSites
{
    private static int DOCKER_REGISTRY_PORT_FOR_PULL = 5000;
    private static int DOCKER_REGISTRY_PORT_FOR_PUSH = 5001;

    private static boolean s_useLocalhost = false;

    public static void useLocalhostForNexus()
    {
        s_useLocalhost = true;
    }

    //--//

    public static String optio3DomainName()
    {
        return "optio3.io";
    }

    public static String digineousDomainName()
    {
        return "digineous.com";
    }

    public static String ldapServer()
    {
        return "ldap.dev." + optio3DomainName();
    }

    public static String builderServer()
    {
        return "builder.dev." + optio3DomainName();
    }

    //--//

    public static String dockerRegistry()
    {
        return dockerRegistry(s_useLocalhost);
    }

    public static String dockerRegistry(boolean useLocalhost)
    {
        return (useLocalhost ? "localhost.dev." : "repo.dev.") + optio3DomainName();
    }

    public static String dockerRegistryAddress(boolean forPush)
    {
        return String.format("%s:%d", dockerRegistry(), forPush ? DOCKER_REGISTRY_PORT_FOR_PUSH : DOCKER_REGISTRY_PORT_FOR_PULL);
    }

    public static String makeDockerImageTagForPush(String imageTag)
    {
        return makeDockerImageTagForPush(new DockerImageIdentifier(imageTag));
    }

    public static String makeDockerImageTagForPush(DockerImageIdentifier img)
    {
        DockerImageIdentifier imgClone = new DockerImageIdentifier(img);
        imgClone.registryHost = dockerRegistry();
        imgClone.registryPort = DOCKER_REGISTRY_PORT_FOR_PUSH;

        return imgClone.getFullName();
    }

    public static String makeDockerImageTagForPull(String imageTag)
    {
        return makeDockerImageTagForPull(new DockerImageIdentifier(imageTag));
    }

    public static String makeDockerImageTagForPull(DockerImageIdentifier img)
    {
        DockerImageIdentifier imgClone = new DockerImageIdentifier(img);
        imgClone.registryHost = dockerRegistry();
        imgClone.registryPort = DOCKER_REGISTRY_PORT_FOR_PULL;

        return imgClone.getFullName();
    }

    //--//

    public static String nexusRepository(String repo)
    {
        if (s_useLocalhost)
        {
            return String.format("https://localhost.dev.%s:1443/repository/%s", optio3DomainName(), repo);
        }
        else
        {
            return String.format("https://repo.dev.%s/repository/%s", optio3DomainName(), repo);
        }
    }
}
