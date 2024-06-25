/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.builder.model.deployment;

import java.time.ZonedDateTime;

public class DeploymentCellularSession
{
    public ZonedDateTime start;
    public ZonedDateTime end;
    public ZonedDateTime lastUpdated;
    public int           packetsDownloaded;
    public int           packetsUploaded;

    public String cellId;
    public String operator;
    public String operatorCountry;
    public String radioLink;
    public double estimatedLongitude;
    public double estimatedLatitude;
}
