/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.HubConfiguration;
import com.optio3.cloud.hub.engine.metrics.MetricsDefinitionDetails;
import com.optio3.cloud.hub.engine.metrics.MetricsEngineExecutionContext;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionVersion;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionVersionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.ModelSanitizerContext;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import com.optio3.util.CollectionUtils;
import io.swagger.annotations.Api;

@Api(tags = { "MetricsDefinitionVersions" }) // For Swagger
@Optio3RestEndpoint(name = "MetricsDefinitionVersions") // For Optio3 Shell
@Path("/v1/metrics-definition-versions")
public class MetricsDefinitionVersions
{
    @Inject
    private HubConfiguration m_cfg;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<MetricsDefinitionVersion> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<MetricsDefinitionVersionRecord> helper = sessionHolder.createHelper(MetricsDefinitionVersionRecord.class);

            return CollectionUtils.transformToList(MetricsDefinitionVersionRecord.getBatch(helper, ids), (rec) ->
            {
                MetricsEngineExecutionContext.validate(sessionHolder, rec);

                return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
            });
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionVersion create(MetricsDefinitionVersion model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<MetricsDefinitionRecord>        helper_def = sessionHolder.createHelper(MetricsDefinitionRecord.class);
            RecordHelper<MetricsDefinitionVersionRecord> helper_ver = sessionHolder.createHelper(MetricsDefinitionVersionRecord.class);

            RecordLocked<MetricsDefinitionRecord> lock_metricsDefinition = TypedRecordIdentity.getWithLockOrNull(helper_def, model.definition, 5, TimeUnit.SECONDS);
            MetricsDefinitionRecord               rec_metricsDefinition  = lock_metricsDefinition.get();

            MetricsDefinitionVersionRecord rec_predecessor = TypedRecordIdentity.getOrNull(helper_ver, model.predecessor);
            if (rec_predecessor == null)
            {
                rec_predecessor = rec_metricsDefinition.getHeadVersion();
            }

            MetricsDefinitionVersionRecord rec_metricsDefinitionVersion = MetricsDefinitionVersionRecord.newInstance(helper_ver, model, rec_metricsDefinition, rec_predecessor, null);

            MetricsEngineExecutionContext.validate(sessionHolder, rec_metricsDefinitionVersion);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_metricsDefinitionVersion);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public MetricsDefinitionVersion get(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            MetricsDefinitionVersionRecord rec_reportDefinition = sessionHolder.getEntity(MetricsDefinitionVersionRecord.class, id);

            MetricsEngineExecutionContext.validate(sessionHolder, rec_reportDefinition);

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_reportDefinition);
        }
    }

    @GET
    @Path("item/{id}/make-head")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionVersion makeHead(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            MetricsDefinitionVersionRecord rec = sessionHolder.getEntity(MetricsDefinitionVersionRecord.class, id);

            rec.makeHead();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/make-release")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionVersion makeRelease(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            MetricsDefinitionVersionRecord rec = sessionHolder.getEntity(MetricsDefinitionVersionRecord.class, id);

            rec.makeRelease();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{predId}/link/{succId}")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionVersion link(@PathParam("predId") String predId,
                                         @PathParam("succId") String succId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            MetricsDefinitionVersionRecord rec_pred = sessionHolder.getEntity(MetricsDefinitionVersionRecord.class, predId);
            MetricsDefinitionVersionRecord rec_succ = sessionHolder.getEntity(MetricsDefinitionVersionRecord.class, succId);

            rec_succ.setPredecessor(rec_pred);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pred);
        }
    }

    @GET
    @Path("item/{baseId}/squash/{finalId}")
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionVersion squash(@PathParam("baseId") String baseId,
                                           @PathParam("finalId") String finalId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<MetricsDefinitionVersionRecord> helper = sessionHolder.createHelper(MetricsDefinitionVersionRecord.class);

            MetricsDefinitionVersionRecord rec = MetricsDefinitionVersionRecord.squash(helper, baseId, finalId);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinitionDetails parseImport(RawImport rawImport)
    {
        MetricsDefinitionDetails details = rawImport.validate(MetricsDefinitionDetails.class);
        if (details != null)
        {
            try (var ctx = new ModelSanitizerContext.SimpleLazy(m_sessionProvider::newReadOnlySession))
            {
                details = ctx.processTyped(details);
            }
        }

        return details;
    }
}
