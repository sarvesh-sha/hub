/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.model.metrics.MetricsDefinition;
import com.optio3.cloud.hub.model.metrics.MetricsDefinitionFilterRequest;
import com.optio3.cloud.hub.persistence.metrics.MetricsDefinitionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "MetricsDefinitions" }) // For Swagger
@Optio3RestEndpoint(name = "MetricsDefinitions") // For Optio3 Shell
@Path("/v1/metrics-definitions")
public class MetricsDefinitions
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @POST
    @Path("filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<MetricsDefinitionRecord> getFiltered(MetricsDefinitionFilterRequest filters)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<MetricsDefinitionRecord> helper = sessionHolder.createHelper(MetricsDefinitionRecord.class);

            return MetricsDefinitionRecord.filter(helper, filters);
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<MetricsDefinition> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<MetricsDefinitionRecord> helper = sessionHolder.createHelper(MetricsDefinitionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, MetricsDefinitionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinition create(MetricsDefinition model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<MetricsDefinitionRecord> helper = sessionHolder.createHelper(MetricsDefinitionRecord.class);

            MetricsDefinitionRecord rec_metricsDefinition = MetricsDefinitionRecord.newInstance(model.sysId);

            ModelMapper.fromModel(sessionHolder, ModelMapperPolicy.Default, model, rec_metricsDefinition);

            helper.persist(rec_metricsDefinition);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_metricsDefinition);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public MetricsDefinition get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, MetricsDefinitionRecord.class, id);
    }

    @POST
    @Path("item/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults update(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun,
                                    MetricsDefinition model)
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            MetricsDefinitionRecord rec_metricsDefinition = validation.sessionHolder.getEntity(MetricsDefinitionRecord.class, id);

            if (validation.canProceed())
            {
                ModelMapper.fromModel(validation.sessionHolder, ModelMapperPolicy.Default, model, rec_metricsDefinition);
            }

            return validation.getResults();
        }
    }

    @DELETE
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public ValidationResults remove(@PathParam("id") String id,
                                    @QueryParam("dryRun") Boolean dryRun) throws
                                                                          Exception
    {
        try (ValidationResultsHolder validation = new ValidationResultsHolder(m_sessionProvider, dryRun, false))
        {
            RecordHelper<MetricsDefinitionRecord> helper = validation.sessionHolder.createHelper(MetricsDefinitionRecord.class);

            MetricsDefinitionRecord rec_metricsDefinition = helper.getOrNull(id);
            if (rec_metricsDefinition != null)
            {
                if (validation.canProceed())
                {
                    rec_metricsDefinition.remove(validation, helper);
                }
            }

            return validation.getResults();
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public MetricsDefinition parseImport(RawImport rawImport)
    {
        return rawImport.validate(MetricsDefinition.class);
    }
}
