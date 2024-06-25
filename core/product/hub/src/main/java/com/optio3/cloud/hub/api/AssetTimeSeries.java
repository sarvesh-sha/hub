/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.CborProvider;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipal;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubApplication;
import com.optio3.cloud.hub.logic.samples.SamplesCache;
import com.optio3.cloud.hub.model.customization.InstanceConfiguration;
import com.optio3.cloud.hub.model.dashboard.AggregationRequest;
import com.optio3.cloud.hub.model.dashboard.AggregationResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesLastValueRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesLastValueResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesMultiPropertyRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesMultiPropertyResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesNewValueRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesNewValueResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesRangeRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesRangeResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesSchemaRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesSchemaResponse;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesSinglePropertyRequest;
import com.optio3.cloud.hub.model.timeseries.TimeSeriesSinglePropertyResponse;
import com.optio3.cloud.hub.persistence.asset.DeviceElementRecord;
import com.optio3.cloud.hub.persistence.asset.DeviceElementSampleRecord;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.BoxingUtils;
import com.optio3.util.CollectionUtils;
import com.optio3.util.TimeUtils;
import io.swagger.annotations.Api;

@Api(tags = { "AssetTimeSeries" }) // For Swagger
@Optio3RestEndpoint(name = "AssetTimeSeries") // For Optio3 Shell
@Path("/v1/assets-timeseries")
@Optio3RequestLogLevel(Severity.Debug)
public class AssetTimeSeries
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    //--//

    @GET
    @Path("trim/{id}/{days}")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    public boolean trimSamples(@PathParam("id") String id,
                               @PathParam("days") int maxDays) throws
                                                               Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeviceElementRecord rec = sessionHolder.getEntityOrNull(DeviceElementRecord.class, id);
            if (rec == null)
            {
                return false;
            }

            RecordHelper<DeviceElementSampleRecord> helper = sessionHolder.createHelper(DeviceElementSampleRecord.class);

            if (maxDays > 0)
            {
                ZonedDateTime now            = TimeUtils.now();
                ZonedDateTime purgeThreshold = now.minus(maxDays, ChronoUnit.DAYS);

                rec.deleteSamplesOlderThan(helper, purgeThreshold);
            }
            else
            {
                rec.deleteSamplesOlderThan(helper, null);
            }

            sessionHolder.commit();

            return true;
        }
    }

    @GET
    @Path("compact/{id}")
    @RolesAllowed({ WellKnownRoleIds.Maintenance })
    @Produces(MediaType.TEXT_PLAIN)
    public String compactSamples(@PathParam("id") String id,
                                 @QueryParam("force") Boolean force) throws
                                                                     Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DeviceElementRecord rec_object = sessionHolder.getEntity(DeviceElementRecord.class, id);

            int archives;

            if (BoxingUtils.get(force, false))
            {
                archives = rec_object.compactTimeSeries(sessionHolder);
            }
            else
            {
                archives = rec_object.compactTimeSeriesIfNeeded(sessionHolder);
            }

            sessionHolder.commit();

            return String.format("Compacted into %d archives", archives);
        }
    }

    //--//

    @POST
    @Path("last-value")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public TimeSeriesLastValueResponse getLastValue(TimeSeriesLastValueRequest req)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.fetch(samplesCache);
    }

    @POST
    @Path("last-value-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public List<TimeSeriesLastValueResponse> getLastValueBatch(List<TimeSeriesLastValueRequest> reqs)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return CollectionUtils.transformInParallel(reqs, HubApplication.GlobalRateLimiter, (req) -> req.fetch(samplesCache));
    }

    //--//

    @POST
    @Path("new-values")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesNewValueResponse emitNewValues(@Context ContainerRequestContext requestContext,
                                                    TimeSeriesNewValueRequest req) throws
                                                                                   Exception
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.execute(samplesCache, (rec_element, pt, model) ->
        {
            InstanceConfiguration cfg       = m_sessionProvider.getServiceNonNull(InstanceConfiguration.class);
            CookiePrincipal       principal = CookiePrincipalAccessor.get(m_principalAccessor);

            return cfg.canAcceptNewSamples(principal, rec_element, pt, model);
        });
    }

    //--//

    @POST
    @Path("schema")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public TimeSeriesSchemaResponse getSchema(TimeSeriesSchemaRequest req)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.fetch(samplesCache);
    }

    @POST
    @Path("schema-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public List<TimeSeriesSchemaResponse> getSchemaBatch(List<TimeSeriesSchemaRequest> reqs)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return CollectionUtils.transformInParallel(reqs, HubApplication.GlobalRateLimiter, (req) ->
        {
            return req.fetch(samplesCache);
        });
    }

    //--//

    @POST
    @Path("request-range")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public TimeSeriesRangeResponse getRange(TimeSeriesRangeRequest req)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.fetch(samplesCache);
    }

    @POST
    @Path("request-range-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public List<TimeSeriesRangeResponse> getRangeBatch(List<TimeSeriesRangeRequest> reqs)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return CollectionUtils.transformInParallel(reqs, HubApplication.GlobalRateLimiter, (req) -> req.fetch(samplesCache));
    }

    //--//

    @POST
    @Path("request-single")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public TimeSeriesSinglePropertyResponse getValuesSingle(TimeSeriesSinglePropertyRequest req)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.fetch(samplesCache);
    }

    @POST
    @Path("request-single-batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public List<TimeSeriesSinglePropertyResponse> getValuesSingleBatch(List<TimeSeriesSinglePropertyRequest> reqs)
    {
        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return CollectionUtils.transformInParallel(reqs, HubApplication.GlobalRateLimiter, (req) -> req.fetch(samplesCache));
    }

    @POST
    @Path("request-multiple")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public TimeSeriesMultiPropertyResponse getValuesMultiple(TimeSeriesMultiPropertyRequest req)
    {
        if (req.specs == null || req.specs.isEmpty())
        {
            return null;
        }

        SamplesCache samplesCache = m_sessionProvider.getServiceNonNull(SamplesCache.class);

        return req.fetch(samplesCache);
    }

    //--//

    @POST
    @Path("aggregation")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON, CborProvider.APPLICATION_CBOR })
    public AggregationResponse aggregation(AggregationRequest req)
    {
    	System.out.println("Aggregation Request*****");
        return req.execute(m_sessionProvider);
    }
}
