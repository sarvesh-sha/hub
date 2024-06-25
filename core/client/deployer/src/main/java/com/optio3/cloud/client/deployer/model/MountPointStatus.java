/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.deployer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class MountPointStatus
{
    public String  type;
    public String  name;
    public boolean readWrite;

    public String source;
    public String destination;
    public String driver;
    public String mode;

    public String propagation;

    //--//

    @JsonIgnore
    public boolean isVolume()
    {
        return "volume".equals(type);
    }
}
