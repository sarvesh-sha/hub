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
import com.optio3.cloud.hub.model.report.ReportDefinitionVersion;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionRecord;
import com.optio3.cloud.hub.persistence.report.ReportDefinitionVersionRecord;
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

@Api(tags = { "ReportDefinitionVersions" }) // For Swagger
@Optio3RestEndpoint(name = "ReportDefinitionVersions") // For Optio3 Shell
@Path("/v1/report-definition-versions")
public class ReportDefinitionVersions
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    //--//

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<ReportDefinitionVersion> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<ReportDefinitionVersionRecord> helper = sessionHolder.createHelper(ReportDefinitionVersionRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, ReportDefinitionVersionRecord.getBatch(helper, ids));
        }
    }

    @POST
    @Path("create")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinitionVersion create(ReportDefinitionVersion model)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            RecordHelper<ReportDefinitionVersionRecord> helper_ver = sessionHolder.createHelper(ReportDefinitionVersionRecord.class);
            RecordHelper<ReportDefinitionRecord>        helper_def = sessionHolder.createHelper(ReportDefinitionRecord.class);

            RecordLocked<ReportDefinitionRecord> lock_reportDefinition = TypedRecordIdentity.getWithLockOrNull(helper_def, model.definition, 5, TimeUnit.SECONDS);
            ReportDefinitionRecord               rec_reportDefinition  = lock_reportDefinition.get();

            ReportDefinitionVersionRecord rec_predecessor = TypedRecordIdentity.getOrNull(helper_ver, model.predecessor);
            if (rec_predecessor == null)
            {
                rec_predecessor = rec_reportDefinition.getHeadVersion();
            }

            ReportDefinitionVersionRecord rec_reportDefinitionVersion = ReportDefinitionVersionRecord.newInstance(helper_ver, rec_reportDefinition, rec_predecessor, model, null, false);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_reportDefinitionVersion);
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public ReportDefinitionVersion get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, ReportDefinitionVersionRecord.class, id);
    }

    @GET
    @Path("item/{id}/make-head")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinitionVersion makeHead(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            ReportDefinitionVersionRecord rec = sessionHolder.getEntity(ReportDefinitionVersionRecord.class, id);

            rec.makeHead();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{id}/make-release")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinitionVersion makeRelease(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            ReportDefinitionVersionRecord rec = sessionHolder.getEntity(ReportDefinitionVersionRecord.class, id);

            rec.makeRelease();

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec);
        }
    }

    @GET
    @Path("item/{predId}/link/{succId}")
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinitionVersion link(@PathParam("predId") String predId,
                                        @PathParam("succId") String succId)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newSessionWithTransaction())
        {
            ReportDefinitionVersionRecord rec_pred = sessionHolder.getEntity(ReportDefinitionVersionRecord.class, predId);
            ReportDefinitionVersionRecord rec_succ = sessionHolder.getEntity(ReportDefinitionVersionRecord.class, succId);

            rec_succ.setPredecessor(rec_pred);

            sessionHolder.commit();

            return ModelMapper.toModel(sessionHolder, ModelMapperPolicy.Default, rec_pred);
        }
    }

    @POST
    @Path("parse-import")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ReportDefinitionVersion parseImport(RawImport rawImport)
    {
        return rawImport.validate(ReportDefinitionVersion.class);
    }
}
