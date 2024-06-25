/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.nio.file.Paths;

import com.optio3.cloud.builder.orchestration.tasks.deploy.DeploymentRole;
import com.optio3.cloud.builder.persistence.deployment.DeploymentHostRecord;
import com.optio3.cloud.client.deployer.model.ContainerConfiguration;
import com.optio3.cloud.client.deployer.model.DockerImageArchitecture;
import com.optio3.cloud.persistence.SessionHolder;

public enum DeploymentHostFlavor
{
    VirtualMachine()
            {
                private static final String c_prefix = "VM";

                @Override
                public boolean matchPrefix(String prefix)
                {
                    return c_prefix.equals(prefix);
                }

                @Override
                public String addPrefix(String id)
                {
                    return c_prefix + "-" + id;
                }

                @Override
                public String findUniqueName(SessionHolder sessionHolder,
                                             String hostId,
                                             boolean hasCellular)
                {
                    return DeploymentHostRecord.findUniqueName(sessionHolder, c_prefix);
                }

                @Override
                public int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                            DeploymentRole role,
                                            int availableMemory)
                {
                    switch (role)
                    {
                        case deployer:
                            return 300;

                        case gateway:
                            return 2000;

                        case hub:
                            if (availableMemory >= 16 * 1024)
                            {
                                return 8000;
                            }
                            else if (availableMemory >= 8 * 1024)
                            {
                                return 4000;
                            }
                            else if (availableMemory >= 4 * 1024)
                            {
                                return 1600;
                            }
                            else if (availableMemory >= 2 * 1024)
                            {
                                return 700;
                            }
                            else
                            {
                                return 200;
                            }

                        default:
                            return 0;
                    }
                }

                @Override
                public void fixupContainerConfig(DeploymentHostRecord rec_host,
                                                 DeploymentRole role,
                                                 ContainerConfiguration config)
                {
                    // Nothing to do.
                }

                @Override
                public boolean shouldCloneDefaultAgent()
                {
                    // Not needed, we don't use incremental downloads.
                    return false;
                }
            },
    RaspberryPI()
            {
                @Override
                public boolean matchPrefix(String prefix)
                {
                    return false;
                }

                @Override
                public String addPrefix(String id)
                {
                    return id;
                }

                @Override
                public String findUniqueName(SessionHolder sessionHolder,
                                             String hostId,
                                             boolean hasCellular)
                {
                    return DeploymentHostRecord.findUniqueName(sessionHolder, "SiteX");
                }

                @Override
                public int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                            DeploymentRole role,
                                            int availableMemory)
                {
                    switch (role)
                    {
                        case deployer:
                            return 128;

                        case gateway:
                            return 500;

                        case provisioner:
                            return 300;

                        case waypoint:
                            return 80;

                        default:
                            return 0;
                    }
                }

                @Override
                public void fixupContainerConfig(DeploymentHostRecord rec_host,
                                                 DeploymentRole role,
                                                 ContainerConfiguration config)
                {
                    switch (role)
                    {
                        case deployer:
                            config.addBind(Paths.get("/boot"), Paths.get("/optio3-boot"));
                            break;
                    }
                }

                @Override
                public boolean shouldCloneDefaultAgent()
                {
                    // Not needed, we don't use incremental downloads.
                    return false;
                }
            },
    Waypoint()
            {
                @Override
                public boolean matchPrefix(String prefix)
                {
                    return false;
                }

                @Override
                public String addPrefix(String id)
                {
                    return id;
                }

                @Override
                public String findUniqueName(SessionHolder sessionHolder,
                                             String hostId,
                                             boolean hasCellular)
                {
                    return DeploymentHostRecord.findUniqueName(sessionHolder, "Waypoint");
                }

                @Override
                public int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                            DeploymentRole role,
                                            int availableMemory)
                {
                    switch (role)
                    {
                        case deployer:
                            return 128;

                        case gateway:
                            return 500;

                        case provisioner:
                            return 300;

                        case waypoint:
                            return 80;

                        default:
                            return 0;
                    }
                }

                @Override
                public void fixupContainerConfig(DeploymentHostRecord rec_host,
                                                 DeploymentRole role,
                                                 ContainerConfiguration config)
                {
                    switch (role)
                    {
                        case deployer:

                            config.addBind(Paths.get("/boot"), Paths.get("/optio3-boot"));
                            break;
                    }
                }

                @Override
                public boolean shouldCloneDefaultAgent()
                {
                    // We use incremental downloads, best to be on an agent that doesn't get killed if offline.
                    return true;
                }
            },
    EdgeV1()
            {
                private static final String c_prefix = "EdgeV1";

                @Override
                public boolean matchPrefix(String prefix)
                {
                    return c_prefix.equals(prefix);
                }

                @Override
                public String addPrefix(String id)
                {
                    return c_prefix + "-" + id;
                }

                @Override
                public String findUniqueName(SessionHolder sessionHolder,
                                             String hostId,
                                             boolean hasCellular)
                {
                    return DeploymentHostRecord.findUniqueName(sessionHolder, c_prefix);
                }

                @Override
                public int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                            DeploymentRole role,
                                            int availableMemory)
                {
                    switch (role)
                    {
                        case deployer:
                            return 90;

                        case gateway:
                            return 150;

                        case provisioner:
                            return 300;

                        case waypoint:
                            return 80;

                        default:
                            return 0;
                    }
                }

                @Override
                public void fixupContainerConfig(DeploymentHostRecord rec_host,
                                                 DeploymentRole role,
                                                 ContainerConfiguration config)
                {
                    switch (role)
                    {
                        case deployer:
                            config.addBind(Paths.get("/optio3/boot"), Paths.get("/optio3-boot"));
                            break;
                    }
                }

                @Override
                public boolean shouldCloneDefaultAgent()
                {
                    // Don't clone agent, not enough memory.
                    return false;
                }
            },
    IntelV1()
            {
                private static final String c_prefix = "IntelV1";

                @Override
                public boolean matchPrefix(String prefix)
                {
                    return c_prefix.equals(prefix);
                }

                @Override
                public String addPrefix(String id)
                {
                    return c_prefix + "-" + id;
                }

                @Override
                public String findUniqueName(SessionHolder sessionHolder,
                                             String hostId,
                                             boolean hasCellular)
                {
                    return DeploymentHostRecord.findUniqueName(sessionHolder, c_prefix);
                }

                @Override
                public int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                            DeploymentRole role,
                                            int availableMemory)
                {
                    switch (role)
                    {
                        case deployer:
                            return 90;

                        case gateway:
                            return 150;

                        case waypoint:
                            return 80;

                        default:
                            return 0;
                    }
                }

                @Override
                public void fixupContainerConfig(DeploymentHostRecord rec_host,
                                                 DeploymentRole role,
                                                 ContainerConfiguration config)
                {
                    switch (role)
                    {
                        case deployer:
                            config.addBind(Paths.get("/optio3/boot"), Paths.get("/optio3-boot"));
                            break;
                    }
                }

                @Override
                public boolean shouldCloneDefaultAgent()
                {
                    // Not needed, we don't use incremental downloads.
                    return false;
                }
            };

    public abstract boolean matchPrefix(String prefix);

    public abstract String addPrefix(String id);

    public abstract String findUniqueName(SessionHolder sessionHolder,
                                          String hostId,
                                          boolean hasCellular);

    public abstract int getMaxHeapMemory(DeploymentHostRecord rec_host,
                                         DeploymentRole role,
                                         int availableMemory);

    public abstract void fixupContainerConfig(DeploymentHostRecord rec_host,
                                              DeploymentRole role,
                                              ContainerConfiguration config);

    public abstract boolean shouldCloneDefaultAgent();

    //--//

    public static DeploymentHostFlavor classifyHost(String hostId,
                                                    DockerImageArchitecture architecture,
                                                    boolean hasCellular)
    {
        int    prefixPos = hostId.indexOf('-');
        String prefix;

        if (prefixPos > 0)
        {
            prefix = hostId.substring(0, prefixPos);

            for (DeploymentHostFlavor value : values())
            {
                if (value.matchPrefix(prefix))
                {
                    return value;
                }
            }

            switch (architecture)
            {
                case X86:
                case ARM64v8:
                    return VirtualMachine;
            }

            return null;
        }
        else if (hasCellular)
        {
            return Waypoint;
        }
        else
        {
            switch (architecture)
            {
                case X86:
                case ARM64v8:
                    return VirtualMachine;
            }

            return RaspberryPI;
        }
    }
}
