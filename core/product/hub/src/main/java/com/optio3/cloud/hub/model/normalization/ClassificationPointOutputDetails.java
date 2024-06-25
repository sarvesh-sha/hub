/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Lists;
import com.optio3.protocol.model.EngineeringUnits;

public class ClassificationPointOutputDetails
{
    public String  id;
    public double  positiveScore;
    public double  negativeScore;
    public double  threshold;
    public boolean ignored;

    public EngineeringUnits assignedUnits;

    public String azureDigitalTwinModel;

    public int     samplingPeriod;
    public boolean noSampling;

    public ClassificationReason reason;

    public List<String> matchingDimensions = Lists.newArrayList();

    @JsonIgnore
    public double getTotalScore()
    {
        return positiveScore + negativeScore;
    }

    @JsonIgnore
    public boolean isAboveThreshold()
    {
        return getTotalScore() >= threshold;
    }
}
