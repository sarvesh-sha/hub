/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.report;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.JsonNode;

@JsonTypeName("CustomReportElementRichText")
public class CustomReportElementRichText extends CustomReportElement
{
    public List<JsonNode> data;
    public String         backgroundColor;
}
