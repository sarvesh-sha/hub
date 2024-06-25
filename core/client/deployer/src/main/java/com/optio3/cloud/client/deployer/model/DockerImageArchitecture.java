/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

public enum DockerImageArchitecture
{
    UNKNOWN(false, false, 0, 0),
    X86(true, false, 0, 64),
    ARM(false, true, 7, 32), // Legacy value
    ARMv6(false, true, 6, 32),
    ARMv7(false, true, 7, 32),
    ARMv8(false, true, 8, 32),
    ARM64v8(false, true, 8, 64);

    private final boolean m_isIntel;
    private final boolean m_isArm;
    private final int     m_archRevision;
    private final int     m_registerWidth;

    DockerImageArchitecture(boolean isIntel,
                            boolean isArm,
                            int archRevision,
                            int registerWidth)
    {
        m_isIntel       = isIntel;
        m_isArm         = isArm;
        m_archRevision  = archRevision;
        m_registerWidth = registerWidth;
    }

    public boolean isIntel()
    {
        return m_isIntel;
    }

    public boolean isArm()
    {
        return m_isArm;
    }

    public boolean isArm32()
    {
        return isArm() && m_registerWidth == 32;
    }

    public boolean isArm64()
    {
        return isArm() && m_registerWidth == 64;
    }

    public int getArchRevision()
    {
        return m_archRevision;
    }

    public int getRegisterWidth()
    {
        return m_registerWidth;
    }

    public static boolean areCompatible(DockerImageArchitecture a,
                                        DockerImageArchitecture b)
    {
        if (a.getRegisterWidth() != b.getRegisterWidth())
        {
            return false;
        }

        if (a.isIntel() && b.isIntel())
        {
            return true;
        }

        if (a.isArm() && b.isArm())
        {
            return a.getArchRevision() == b.getArchRevision();
        }

        return false;
    }

    public static DockerImageArchitecture parse(String value)
    {
        if (value != null)
        {
            switch (value.toLowerCase())
            {
                case "x86":
                case "x86-64":
                case "amd64":
                    return DockerImageArchitecture.X86;

                case "arm":
                    return DockerImageArchitecture.ARM;

                case "armv6":
                    return DockerImageArchitecture.ARMv6;

                case "armv7":
                case "armv7l":
                    return DockerImageArchitecture.ARMv7;

                case "aarch32":
                    return DockerImageArchitecture.ARMv8;

                case "arm64":
                case "arm64v8":
                case "aarch64":
                    return DockerImageArchitecture.ARM64v8;
            }
        }

        return null;
    }
}
