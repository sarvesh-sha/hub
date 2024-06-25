/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.optio3.cloud.hub.model.asset.graph.AssetGraphBinding;

@JsonTypeName("CustomReportElementDeviceElementList")
public class CustomReportElementDeviceElementList extends CustomReportElement
{
    public AssetGraphBinding pointInput;
}
