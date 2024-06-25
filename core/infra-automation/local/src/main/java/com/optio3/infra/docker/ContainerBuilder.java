/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.infra.docker;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration.MappingConfig;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration.PortConfig;
import com.optio3.infra.docker.model.EmptyObject;
import com.optio3.infra.docker.model.EndpointSettings;
import com.optio3.infra.docker.model.ExtendedConfig;
import com.optio3.infra.docker.model.HostConfig;
import com.optio3.infra.docker.model.NetworkingConfig;
import com.optio3.infra.docker.model.PortBinding;
import com.optio3.infra.docker.model.PortMap;
import com.optio3.infra.docker.model.RestartPolicy;
import com.optio3.infra.docker.model.RestartPolicyMode;
import com.optio3.infra.docker.model.Volume;
import com.optio3.text.CommandLineTokenizer;
import com.optio3.util.IdGenerator;
import org.apache.commons.lang3.StringUtils;

public class ContainerBuilder
{
    private static final String DOCKER_SOCKET = "/var/run/docker.sock";

    //--//

    private ExtendedConfig m_createConfig;

    //--//

    public void loadFrom(ContainerConfiguration config) throws
                                                        Exception
    {
        if (config.privileged)
        {
            allowPrivilegedAccess();
        }

        if (config.allowAccessToDockerDaemon)
        {
            allowAccessToDockerDaemon();
        }

        if (config.restartAlways)
        {
            RestartPolicy restartPolicy = new RestartPolicy();
            restartPolicy.name = RestartPolicyMode.ALWAYS;

            ensureHostConfig().restartPolicy = restartPolicy;
        }

        setImage(config.image);
        setEntrypoint(config.entrypointOverride);
        setCommand(config.commandLine);

        if (config.networkMode != null)
        {
            ensureHostConfig().networkMode = config.networkMode;

            if (StringUtils.isNotEmpty(config.networkAlias))
            {
                useNetwork(config.networkMode, config.networkAlias);
            }
        }

        if (config.workingDir != null)
        {
            setWorkingDir(config.workingDir);
        }

        if (config.labels != null)
        {
            for (String key : config.labels.keySet())
                addLabel(key, config.labels.get(key));
        }

        if (config.environmentVariables != null)
        {
            for (String key : config.environmentVariables.keySet())
                addEnvironmentVariable(key, config.environmentVariables.get(key));
        }

        if (config.bindings != null)
        {
            for (MappingConfig binding : config.bindings)
            {
                if (binding.volume != null)
                {
                    addBind(binding.volume, binding.guestDir);
                }

                if (binding.hostDir != null)
                {
                    addBind(binding.hostDir, binding.guestDir);
                }
            }
        }

        if (config.ports != null)
        {
            for (PortConfig port : config.ports)
                addPort(port.hostPort, port.containerPort, port.useUDP);
        }
    }

    public ExtendedConfig build()
    {
        return ensureMainConfig();
    }

    //--//

    public ContainerBuilder allowPrivilegedAccess()
    {
        ensureHostConfig().privileged = true;

        return this;
    }

    public ContainerBuilder allowAccessToDockerDaemon()
    {
        // Bind the docker socket to the container.
        addBindRaw(DOCKER_SOCKET, DOCKER_SOCKET);

        // And expose an environment variable to let the container find itself.
        addEnvironmentVariable(DockerHelper.SELFID, IdGenerator.newGuid());

        return this;
    }

    public ContainerBuilder allowAccessToHostNetwork()
    {
        ensureHostConfig().networkMode = "host";

        return this;
    }

    public ContainerBuilder setImage(String image)
    {
        requireNonNull(image);

        ensureMainConfig().image = image;

        return this;
    }

    public ContainerBuilder setWorkingDir(Path workingDir)
    {
        requireNonNull(workingDir);

        ensureMainConfig().workingDir = getPathAsString(workingDir);

        return this;
    }

    public ContainerBuilder setEntrypoint(List<String> entrypoint)
    {
        if (entrypoint != null)
        {
            ensureMainConfig().entrypoint = Lists.newArrayList(entrypoint);
        }

        return this;
    }

    public ContainerBuilder setCommandLine(String line)
    {
        ensureMainConfig().cmd = CommandLineTokenizer.translate(line);

        return this;
    }

    public ContainerBuilder setCommand(List<String> args)
    {
        if (args != null)
        {
            ensureMainConfig().cmd = Lists.newArrayList(args);
        }

        return this;
    }

    public void addLabel(String key,
                         String value)
    {
        ensureMainConfig().labels.put(key, value);
    }

    public void addEnvironmentVariable(String name,
                                       String value)
    {
        ensureMainConfig().env.add(name + "=" + value);
    }

    //--//

    public ContainerBuilder addBind(Path hostDir,
                                    Path guestDir)
    {
        requireNonNull(hostDir);
        requireNonNull(guestDir);

        String host  = getPathAsString(hostDir);
        String guest = getPathAsString(guestDir);

        String actualHostPath = handleEncodingHostPath(host, guest);

        return addBindRaw(actualHostPath, guest);
    }

    public ContainerBuilder addBind(String volume,
                                    Path guestDir) throws
                                                   Exception
    {
        requireNonNull(volume);
        requireNonNull(guestDir);

        String guest = getPathAsString(guestDir);

        try (DockerHelper helper = new DockerHelper(null))
        {
            Volume state = helper.inspectVolume(volume);
            handleEncodingHostPath(state.mountpoint, guest);
        }

        return addBindRaw(volume, guest);
    }

    //--//

    public ContainerBuilder addPort(int hostPort,
                                    int containerPort,
                                    boolean useUDP)
    {
        PortBinding pb = new PortBinding();
        pb.hostIp = "0.0.0.0";
        pb.hostPort = String.valueOf(hostPort);

        PortMap pm = new PortMap();
        pm.add(pb);

        String key = String.valueOf(containerPort);
        if (useUDP)
        {
            key += "/udp";
        }
        else
        {
            key += "/tcp";
        }

        ensureMainConfig().exposedPorts.put(key, new EmptyObject());
        ensureHostConfig().portBindings.put(key, pm);

        return this;
    }

    //--//

    public ContainerBuilder useNetwork(String network,
                                       String... aliases)
    {
        EndpointSettings ep = ensureNetwork(network);

        for (String alias : aliases)
            ep.aliases.add(alias);

        return this;
    }

    //--//

    private ExtendedConfig ensureMainConfig()
    {
        if (m_createConfig == null)
        {
            m_createConfig = new ExtendedConfig();
        }

        return m_createConfig;
    }

    private HostConfig ensureHostConfig()
    {
        ExtendedConfig config = ensureMainConfig();

        if (config.hostConfig == null)
        {
            config.hostConfig = new HostConfig();
        }

        return config.hostConfig;
    }

    private NetworkingConfig ensureNetworkConfig()
    {
        ExtendedConfig config = ensureMainConfig();

        if (config.networkingConfig == null)
        {
            config.networkingConfig = new NetworkingConfig();
        }

        return config.networkingConfig;
    }

    private EndpointSettings ensureNetwork(String network)
    {
        NetworkingConfig cfg = ensureNetworkConfig();
        return cfg.endpointsConfig.computeIfAbsent(network, k -> new EndpointSettings());
    }

    public static String getPathAsString(Path path)
    {
        return path != null ? path.toAbsolutePath()
                                  .toString() : null;
    }

    private String handleEncodingHostPath(String leftSide,
                                          String rightSide)
    {
        String envName        = DockerHelper.encodeGuestBinding(rightSide);
        String actualHostPath = DockerHelper.mapBindingToHost(leftSide);
        addEnvironmentVariable(envName, actualHostPath);

        return actualHostPath;
    }

    private ContainerBuilder addBindRaw(String leftSide,
                                        String rightSide)
    {
        ensureHostConfig().binds.add(leftSide + ":" + rightSide);

        return this;
    }
}
