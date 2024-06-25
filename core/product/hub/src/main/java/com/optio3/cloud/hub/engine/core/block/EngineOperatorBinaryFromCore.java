/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.engine.core.block;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.core.type.TypeReference;
import com.optio3.cloud.hub.engine.EngineValue;

@JsonSubTypes({ @JsonSubTypes.Type(value = EngineArithmeticOperatorBinary.class),
                @JsonSubTypes.Type(value = EngineArithmeticPercentageOperatorBinary.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryApproximateEquality.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryCompareEngineeringUnits.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryDateTimeModify.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryDateTimeRange.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryDateTimeRangeFromTime.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryDateTimeSetTimeZone.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryListContains.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryListGet.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryListJoin.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryLogicCompare.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryLogicOperation.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryLookupTableFilter.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryLookupTableLookup.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryLookupTableReplace.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryRegexMatch.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryRegexMatchCaseSensitive.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryRegexReplace.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryRegexTableReplace.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringConcat.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringEndsWith.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringIndexOf.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringIsMatch.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringSplit.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryStringStartsWith.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryWeeklyScheduleIsIncluded.class),
                @JsonSubTypes.Type(value = EngineExpressionBinaryWeeklyScheduleSetTimeZone.class),
                @JsonSubTypes.Type(value = EngineOperatorBinaryForStringSet.class) })
public abstract class EngineOperatorBinaryFromCore<To extends EngineValue, Ta extends EngineValue, Tb extends EngineValue> extends EngineOperatorBinary<To, Ta, Tb>
{
    protected EngineOperatorBinaryFromCore(Class<To> resultType)
    {
        super(resultType);
    }

    protected EngineOperatorBinaryFromCore(TypeReference<To> resultType)
    {
        super(resultType);
    }
}
