/*
 * Copyright (C) 2017-, Optio3, Inc. All Rights Reserved.
 *
 * Proprietary & Confidential Information.
 */
package com.optio3.cloud.hub.api;

import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.hub.model.dashboard.DashboardConfiguration;
import com.optio3.cloud.hub.model.dashboard.DashboardDefinitionVersion;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionRecord;
import com.optio3.cloud.hub.persistence.dashboard.DashboardDefinitionVersionRecord;
import com.optio3.cloud.model.RawImport;
import com.optio3.cloud.model.TypedRecordIdentity;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.RecordLocked;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "DashboardDefinitionVersions" }) // For Swagger
@Optio3RestEndpoint(name = "DashboardDefinitionVersions") // For Optio3 Shell
@Path("/v1/dashboard-definition-versions")
public class DashboardDefinitionVersions
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<DashboardDefinitionVersion> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<DashboardDefinitionVersionRecord> helper = sessionHolder.createHelper(DashboardDefinitionVersionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, DashboardDefinitionVersionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardDefinitionVersion create(DashboardDefinitionVersion model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<DashboardDefinitionVersionRecord> helper_ver = sessionHolder.createHelper(DashboardDefinitionVersionRecord.class);
            RecordHelper<DashboardDefinitionRecord>        helper_def = sessionHolder.createHelper(DashboardDefinitionRecord.class);

            RecordLocked<DashboardDefinitionRecord> lock_dashboardDefinition = TypedRecordIdentity.getWithLockOrNull(helper_def, model.definition, 5, TimeUnit.SECONDS);
            DashboardDefinitionRecord               rec_dashboardDefinition  = lock_dashboardDefinition.get();

            DashboardDefinitionVersionRecord rec_predecessor = TypedRecordIdentity.getOrNull(helper_ver, model.predecessor);
            if (rec_predecessor == null)
            {
                rec_predecessor = rec_dashboardDefinition.getHeadVersion();
            }

            DashboardDefinitionVersionRecord rec_dashboardDefinitionVersion = DashboardDefinitionVersionRecord.newInstance(helper_ver, rec_dashboardDefinition, rec_predecessor, model, null, false);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_dashboardDefinitionVersion);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public DashboardDefinitionVersion get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, DashboardDefinitionVersionRecord.class, id);
    }

    @GET
    @Path("item/{id}/make-head")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardDefinitionVersion makeHead(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DashboardDefinitionVersionRecord rec = sessionHolder.getEntity(DashboardDefinitionVersionRecord.class, id);

            rec.makeHead();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/make-release")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardDefinitionVersion makeRelease(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DashboardDefinitionVersionRecord rec = sessionHolder.getEntity(DashboardDefinitionVersionRecord.class, id);

            rec.makeRelease();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{predId}/link/{succId}")
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardDefinitionVersion link(@PathParam("predId") String predId,
                                           @PathParam("succId") String succId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            DashboardDefinitionVersionRecord rec_pred = sessionHolder.getEntity(DashboardDefinitionVersionRecord.class, predId);
            DashboardDefinitionVersionRecord rec_succ = sessionHolder.getEntity(DashboardDefinitionVersionRecord.class, succId);

            rec_succ.setPredecessor(rec_pred);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pred);
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public DashboardConfiguration parseImport(RawImport rawImport)
    {
        return rawImport.validate(DashboardConfiguration.class);
    }
}
