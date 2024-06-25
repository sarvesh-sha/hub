/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.asyncawait.bootstrap;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Locale;

import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;
import com.sun.tools.attach.spi.AttachProvider;

/**
 * To make it easier to use optio3-asyncawait during development,
 * this class implements the dynamic loading of a Java agent.
 * <br>
 * It does so by searching for a Hotspot VM and registering with it on-the-fly.
 * <p>
 * It's invoked indirectly by {@link Agent#loadAtRuntime}.
 */
class AgentLoader
{
    static void loadAgent(String agentJar,
                          String options)
    {
        try
        {
            VirtualMachine vm = getVirtualMachine();
            if (vm == null)
            {
                throw new RuntimeException("Cannot attach to this JVM. Add -javaagent:" + agentJar + " to the commandline");
            }

            try
            {
                vm.loadAgent(agentJar, options);
            }
            finally
            {
                vm.detach();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot attach to this jvm. Add -javaagent:" + agentJar + " to the commandline", e);
        }
    }

    private static VirtualMachine getVirtualMachine() throws
                                                      AttachNotSupportedException,
                                                      IOException
    {
        if (!VirtualMachine.list()
                           .isEmpty())
        {
            //
            // Tools jar is present
            //
            String pid = getPid();
            return VirtualMachine.attach(pid);
        }

        //
        // Tools jar is not present, let's see if it's an Oracle JVM.
        //
        String jvm = System.getProperty("java.vm.name")
                           .toLowerCase(Locale.ENGLISH);
        if (jvm.contains("hotspot") || jvm.contains("openjdk"))
        {
            try
            {
                Class<VirtualMachine> virtualMachineClass = selectVmImplementation();

                final AttachProvider attachProvider = new AttachProvider()
                {
                    @Override
                    public VirtualMachine attachVirtualMachine(String arg0)
                    {
                        return null;
                    }

                    @Override
                    public List<VirtualMachineDescriptor> listVirtualMachines()
                    {
                        return null;
                    }

                    @Override
                    public String name()
                    {
                        return null;
                    }

                    @Override
                    public String type()
                    {
                        return null;
                    }
                };

                Constructor<VirtualMachine> vmConstructor = virtualMachineClass.getDeclaredConstructor(AttachProvider.class, String.class);
                vmConstructor.setAccessible(true);
                VirtualMachine newVM = vmConstructor.newInstance(attachProvider, getPid());
                return newVM;
            }
            catch (UnsatisfiedLinkError e)
            {
                throw new RuntimeException("This jre doesn't support the native library for attaching to the jvm", e);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        // not a hotspot based virtual machine
        return null;
    }

    private static String getPid()
    {
        String nameOfRunningVM = ManagementFactory.getRuntimeMXBean()
                                                  .getName();
        int p = nameOfRunningVM.indexOf('@');
        return nameOfRunningVM.substring(0, p);
    }

    private static Class<VirtualMachine> selectVmImplementation() throws
                                                                  ClassNotFoundException
    {
        String vmClassName = inferClassForVmImplementation();

        @SuppressWarnings("unchecked") Class<VirtualMachine> clz = (Class<VirtualMachine>) AgentLoader.class.getClassLoader()
                                                                                                            .loadClass(vmClassName);

        return clz;
    }

    private static String inferClassForVmImplementation()
    {
        String os            = System.getProperty("os.name");
        String ososLowerCase = os.toLowerCase(Locale.ENGLISH);

        if (ososLowerCase.contains("win"))
        {
            return "sun.tools.attach.WindowsVirtualMachine";
        }

        if (ososLowerCase.contains("nix") || ososLowerCase.contains("nux") || ososLowerCase.indexOf("aix") > 0)
        {
            return "sun.tools.attach.LinuxVirtualMachine";
        }

        if (ososLowerCase.contains("mac"))
        {
            return "sun.tools.attach.BsdVirtualMachine";
        }

        if (ososLowerCase.contains("sunos") || ososLowerCase.contains("solaris"))
        {
            return "sun.tools.attach.SolarisVirtualMachine";
        }

        throw new RuntimeException("Cannott find a VM implementation for this operational system: " + ososLowerCase);
    }
}
