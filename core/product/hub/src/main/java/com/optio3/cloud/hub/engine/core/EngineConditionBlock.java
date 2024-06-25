/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core;

import java.util.List;

import com.google.common.collect.Lists;
import com.optio3.cloud.hub.engine.EngineExpression;
import com.optio3.cloud.hub.engine.EngineStatement;
import com.optio3.cloud.hub.engine.EngineValue;

public class EngineConditionBlock
{
    public EngineExpression<EngineValue> condition;

    public List<EngineStatement> statements = Lists.newArrayList();
}
