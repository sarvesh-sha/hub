/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = TagsConditionUnaryNot.class) })
public abstract class TagsConditionUnary extends TagsCondition
{
    public TagsCondition a;
}
