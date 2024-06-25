/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.client.gateway.model;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.optio3.protocol.model.BaseAssetDescriptor;
import com.optio3.serialization.ObjectMappers;
import com.optio3.serialization.Reflection;
import com.optio3.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;

public abstract class GatewayEntity implements Comparable<GatewayEntity>
{
    public GatewayDiscoveryEntitySelector selectorKey;

    /**
     * The selector's value.
     */
    public String selectorValue;

    public void setSelectorValueAsObject(BaseAssetDescriptor value)
    {
        try
        {
            selectorValue = ObjectMappers.SkipNulls.writeValueAsString(value);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    public <T extends BaseAssetDescriptor> T getSelectorValueAsObject(Class<T> clz)
    {
        try
        {
            if (selectorValue == null)
            {
                return null;
            }

            return clz.cast(ObjectMappers.SkipNulls.readValue(selectorValue, BaseAssetDescriptor.class));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    //--//

    /**
     * If this entity has an associated sample, the time of sampling.
     */
    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public long timestampEpochSeconds;

    /**
     * For entities with sub-second sampling, this is the fractional part of the timestamp.
     * We split the timestamp into two fields, because JSON serialization of double generates bigger payloads.
     */
    @JsonInclude(value = JsonInclude.Include.NON_DEFAULT)
    public int timestampEpochMillis;

    @JsonIgnore
    public double getTimestampEpoch()
    {
        return timestampEpochSeconds + timestampEpochMillis * (1.0 / 1_000);
    }

    @JsonIgnore
    public void setTimestampEpoch(double timestampEpoch)
    {
        timestampEpochSeconds = (long) Math.floor(timestampEpoch);
        timestampEpochMillis = (int) ((timestampEpoch - timestampEpochSeconds) * 1_000);
    }

    /**
     * If not null, protocol-specific serialization of the state of the entity.
     */
    public String contents;

    public void setContentsAsObject(ObjectMapper mapper,
                                    Object value) throws
                                                  JsonProcessingException
    {
        contents = mapper.writeValueAsString(value);
    }

    public <T> T getContentsAsObject(ObjectMapper mapper,
                                     TypeReference<T> ref) throws
                                                           IOException
    {
        return contents != null ? mapper.readValue(contents, ref) : null;
    }

    //--//

    public int estimateSize()
    {
        int size = selectorKey.name()
                              .length();

        if (selectorValue != null)
        {
            size += selectorValue.length();
        }

        if (contents != null)
        {
            size += contents.length();
        }

        return size;
    }

    //--//

    @Override
    public boolean equals(Object o)
    {
        GatewayEntity that = Reflection.as(o, GatewayEntity.class);
        if (that != null)
        {
            return compareTo(that) == 0;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        int result = 1;

        result = hashCode(result, selectorKey);
        result = hashCode(result, selectorValue);
        result = hashCode(result, timestampEpochSeconds);

        return result;
    }

    private static int hashCode(int hash,
                                Object o)
    {
        return 31 * hash + (o == null ? 0 : o.hashCode());
    }

    @Override
    public int compareTo(GatewayEntity o)
    {
        int diff = StringUtils.compare(selectorKey.name(), o.selectorKey.name());

        if (diff == 0)
        {
            diff = StringUtils.compare(selectorValue, o.selectorValue);
        }

        if (diff == 0)
        {
            diff = Long.compare(timestampEpochSeconds, o.timestampEpochSeconds);
        }

        if (diff == 0)
        {
            diff = Integer.compare(timestampEpochMillis, o.timestampEpochMillis);
        }

        return diff;
    }

    //--//

    public Map<String, Integer> getContentsForObjectConfig(ObjectMapper mapper) throws
                                                                                IOException
    {
        return getContentsAsObject(mapper, new TypeReference<Map<String, Integer>>()
        {
        });
    }

    public void setContentsForObjectConfig(ObjectMapper mapper,
                                           Map<String, Integer> map) throws
                                                                     IOException
    {
        setContentsAsObject(mapper, map);
    }

    //--//

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        toString(sb);

        return sb.toString();
    }

    protected void toString(StringBuilder sb)
    {
        sb.append(selectorKey);
        sb.append(":");
        sb.append(selectorValue);

        ZonedDateTime time = TimeUtils.fromTimestampToUtcTime(timestampEpochSeconds);
        if (time != null)
        {
            sb.append(":");
            sb.append(TimeUtils.DEFAULT_FORMATTER_NO_MILLI.format(time));

            if (timestampEpochMillis != 0)
            {
                sb.append(".");
                sb.append(timestampEpochMillis);
            }
        }
    }
}
