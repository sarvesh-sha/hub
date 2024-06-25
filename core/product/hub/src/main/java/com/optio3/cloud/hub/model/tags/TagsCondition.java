/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.model.tags;

import java.util.BitSet;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.optio3.cloud.exception.InvalidArgumentException;
import com.optio3.cloud.hub.logic.tags.TagsQueryContext;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "__type")
@JsonSubTypes({ @JsonSubTypes.Type(value = TagsConditionBinary.class),
                @JsonSubTypes.Type(value = TagsConditionEquipmentClass.class),
                @JsonSubTypes.Type(value = TagsConditionIsAsset.class),
                @JsonSubTypes.Type(value = TagsConditionIsClassified.class),
                @JsonSubTypes.Type(value = TagsConditionIsEquipment.class),
                @JsonSubTypes.Type(value = TagsConditionLocation.class),
                @JsonSubTypes.Type(value = TagsConditionMetrics.class),
                @JsonSubTypes.Type(value = TagsConditionMetricsOutput.class),
                @JsonSubTypes.Type(value = TagsConditionPointClass.class),
                @JsonSubTypes.Type(value = TagsConditionTerm.class),
                @JsonSubTypes.Type(value = TagsConditionTermWithValue.class),
                @JsonSubTypes.Type(value = TagsConditionUnary.class) })
public abstract class TagsCondition
{
    public abstract void validate(StringBuilder path);

    public abstract BitSet evaluate(TagsQueryContext context);

    //--//

    public static void validate(TagsCondition query)
    {
        if (query == null)
        {
            throw new InvalidArgumentException("no query");
        }

        query.validate(new StringBuilder());
    }

    protected InvalidArgumentException validationFailure(StringBuilder path,
                                                         String field)
    {
        return validationFailure(path, field, " is missing");
    }

    protected InvalidArgumentException validationFailure(StringBuilder path,
                                                         String field,
                                                         String msg)
    {
        addValidationHop(path, field);
        path.append(msg);
        return new InvalidArgumentException(path.toString());
    }

    protected RuntimeException validationFailure(StringBuilder path,
                                                 String field,
                                                 String fmt,
                                                 Object... args)
    {
        return validationFailure(path, field, String.format(fmt, args));
    }

    protected void addValidationHop(StringBuilder path,
                                    String name)
    {
        if (name != null)
        {
            if (path.length() == 0)
            {
                path.append("<root>");
            }

            path.append("->");
            path.append(name);
        }
    }

    protected void validateChild(StringBuilder path,
                                 String name,
                                 TagsCondition obj)
    {
        if (obj == null)
        {
            throw validationFailure(path, name);
        }

        int pos = path.length();

        addValidationHop(path, name);

        obj.validate(path);

        path.setLength(pos);
    }
}
