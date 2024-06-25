/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.docker;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public class DockerImageIdentifier
{
    public final String fullName;

    public String  registryHost;
    public Integer registryPort;

    public String account;
    public String name;
    public String tag;

    @JsonCreator
    public DockerImageIdentifier(String image)
    {
        fullName = image;

        String  regHost = null;
        Integer regPort = null;

        int hostPos = image.indexOf('/');
        if (hostPos >= 0)
        {
            String hostPart = image.substring(0, hostPos);

            boolean hasDomainName = hostPart.indexOf('.') >= 0;
            int     portPos       = hostPart.indexOf(':');

            // Either it's "registry.domain.com" or "somelocalhost:port".
            if (hasDomainName || portPos > 0)
            {
                if (portPos > 0)
                {
                    regPort = Integer.valueOf(hostPart.substring(portPos + 1));
                    regHost = hostPart.substring(0, portPos);
                }
                else
                {
                    regHost = hostPart;
                }

                image = image.substring(hostPos + 1);
            }
        }

        registryHost = regHost;
        registryPort = regPort;

        int accountPos = image.indexOf('/');
        if (accountPos >= 0)
        {
            account = image.substring(0, accountPos);

            image = image.substring(accountPos + 1);
        }
        else
        {
            account = null;
        }

        int tagPos = image.indexOf(':');
        if (tagPos >= 0)
        {
            name = image.substring(0, tagPos);
            tag = image.substring(tagPos + 1);
        }
        else
        {
            name = image;
            tag = null;
        }
    }

    public DockerImageIdentifier(DockerImageIdentifier img)
    {
        fullName = img.fullName;
        registryHost = img.registryHost;
        registryPort = img.registryPort;

        account = img.account;
        name = img.name;
        tag = img.tag;
    }

    //--//

    public String getFullName()
    {
        StringBuilder sb = new StringBuilder();

        if (registryHost != null)
        {
            appendRegistryAddress(sb);

            sb.append("/");
        }

        appendAccountAndName(sb);

        appendTag(sb);

        return sb.toString();
    }

    public String getRepositoryName()
    {
        StringBuilder sb = new StringBuilder();

        if (registryHost != null)
        {
            appendRegistryAddress(sb);

            sb.append("/");
        }

        appendAccountAndName(sb);

        return sb.toString();
    }

    public String getRegistryAddress()
    {
        if (registryHost == null)
        {
            return null;
        }

        StringBuilder sb = new StringBuilder();

        appendRegistryAddress(sb);

        return sb.toString();
    }

    public String getAccountAndName()
    {
        StringBuilder sb = new StringBuilder();

        appendAccountAndName(sb);

        return sb.toString();
    }

    //--//

    private void appendRegistryAddress(StringBuilder sb)
    {
        sb.append(registryHost);
        if (registryPort != null)
        {
            sb.append(":");
            sb.append(registryPort);
        }
    }

    private void appendAccountAndName(StringBuilder sb)
    {
        if (account != null)
        {
            sb.append(account);
            sb.append("/");
        }

        sb.append(name);
    }

    private void appendTag(StringBuilder sb)
    {
        if (tag != null)
        {
            sb.append(":");
            sb.append(tag);
        }
    }

    @Override
    @JsonValue
    public String toString()
    {
        return fullName;
    }
}
