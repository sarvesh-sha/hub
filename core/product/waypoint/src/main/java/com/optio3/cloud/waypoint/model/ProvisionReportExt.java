/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.waypoint.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ProvisionReportExt
{
    public com.optio3.cloud.client.builder.model.ProvisionReport info;
    public boolean                                               printed;
    public boolean                                               uploaded;

    @JsonIgnore
    public boolean reportedError;
}
