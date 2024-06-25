/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

public class DeployerShutdownConfiguration
{
    public float turnOffVoltage;
    public float turnOnVoltage;

    public float turnOffDelaySeconds;
    public float turnOnDelaySeconds;

    //--//

    public static boolean equals(DeployerShutdownConfiguration a,
                                 DeployerShutdownConfiguration b)
    {
        if (a == null)
        {
            a = new DeployerShutdownConfiguration();
        }

        if (b == null)
        {
            b = new DeployerShutdownConfiguration();
        }

        // Allow for conversion rounding.
        if (!equalsFuzzy(a.turnOffVoltage, b.turnOffVoltage, 0.05f))
        {
            return false;
        }

        if (!equalsFuzzy(a.turnOnVoltage, b.turnOnVoltage, 0.05f))
        {
            return false;
        }

        if (!equalsFuzzy(a.turnOffDelaySeconds, b.turnOffDelaySeconds, 0.1f))
        {
            return false;
        }

        return equalsFuzzy(a.turnOnDelaySeconds, b.turnOnDelaySeconds, 0.1f);
    }

    private static boolean equalsFuzzy(float a,
                                       float b,
                                       float tolerance)
    {
        return Math.abs((Float.isNaN(a) ? 0 : a) - (Float.isNaN(b) ? 0 : b)) < tolerance;
    }
}
