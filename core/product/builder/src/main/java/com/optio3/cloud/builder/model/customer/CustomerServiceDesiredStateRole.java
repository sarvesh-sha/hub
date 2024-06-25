/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.customer;

public class CustomerServiceDesiredStateRole extends RoleAndArchitectureWithImage
{
    public boolean shutdown;
    public boolean shutdownIfDifferent;
    public boolean launch;
    public boolean launchIfMissing;
    public boolean launchIfMissingAndIdle;

    public void resetFlags()
    {
        shutdown               = false;
        shutdownIfDifferent    = false;
        launch                 = false;
        launchIfMissing        = false;
        launchIfMissingAndIdle = false;
    }

    public static CustomerServiceDesiredStateRole from(RoleAndArchitectureWithImage src)
    {
        var res = new CustomerServiceDesiredStateRole();
        res.role         = src.role;
        res.architecture = src.architecture;
        res.image        = src.image;
        return res;
    }
}
