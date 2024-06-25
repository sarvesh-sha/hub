/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.normalization;

import java.util.List;

import com.optio3.cloud.hub.engine.normalizations.NormalizationEngineExecutionStep;
import com.optio3.cloud.hub.model.common.LogLine;

public class NormalizationEvaluation
{
    public List<NormalizationEngineExecutionStep> steps;

    public List<LogLine> logEntries;
}
