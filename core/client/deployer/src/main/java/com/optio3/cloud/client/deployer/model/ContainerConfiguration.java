/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.optio3.text.CommandLineTokenizer;

public class ContainerConfiguration
{
    public static class MappingConfig
    {
        public Path   hostDir;
        public String volume;

        public Path guestDir;
    }

    public static class PortConfig
    {
        public int     hostPort;
        public int     containerPort;
        public boolean useUDP;
    }

    //--//

    public Map<String, String> labels = Maps.newHashMap();

    public String image;

    public List<String> entrypointOverride;

    public List<String> commandLine;

    public Path workingDir;

    public Map<String, String> environmentVariables = Maps.newHashMap();

    //--//

    public boolean privileged;

    public boolean allowAccessToDockerDaemon;

    public boolean restartAlways;

    //--//

    public List<MappingConfig> bindings = Lists.newArrayList();

    //--//

    public String networkMode;
    public String networkAlias;

    public List<PortConfig> ports = Lists.newArrayList();

    //--//

    public void overrideEntrypoint(String entrypoint)
    {
        if (entrypoint != null)
        {
            entrypointOverride = CommandLineTokenizer.translate(entrypoint);
        }
    }

    public void overrideCommandLine(String line)
    {
        if (line != null)
        {
            commandLine = CommandLineTokenizer.translate(line);
        }
    }

    public void overrideCommands(String... args)
    {
        commandLine = Lists.newArrayList(args);
    }

    public void addBind(Path hostDir,
                        Path guestDir)
    {
        requireNonNull(hostDir);
        requireNonNull(guestDir);

        MappingConfig mc = new MappingConfig();
        mc.hostDir = hostDir;
        mc.guestDir = guestDir;
        bindings.add(mc);
    }

    public void addBind(String volume,
                        Path guestDir)
    {
        requireNonNull(volume);
        requireNonNull(guestDir);

        MappingConfig mc = new MappingConfig();
        mc.volume = volume;
        mc.guestDir = guestDir;
        bindings.add(mc);
    }

    public Path mapFromGuestToHost(Path dir)
    {
        for (MappingConfig binding : bindings)
        {
            if (dir.startsWith(binding.guestDir))
            {
                dir = binding.guestDir.relativize(dir);
                return binding.hostDir.resolve(dir);
            }
        }

        return null;
    }

    //--//

    public void addPort(int hostPort,
                        int containerPort,
                        boolean useUDP)
    {
        PortConfig pc = new PortConfig();
        pc.hostPort = hostPort;
        pc.containerPort = containerPort;
        pc.useUDP = useUDP;
        ports.add(pc);
    }
}
