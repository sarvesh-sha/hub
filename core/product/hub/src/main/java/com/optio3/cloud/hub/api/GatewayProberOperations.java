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

import com.google.common.collect.Lists;
import com.optio3.cloud.annotation.Optio3Dao;
import com.optio3.cloud.annotation.Optio3Principal;
import com.optio3.cloud.annotation.Optio3RequestLogLevel;
import com.optio3.cloud.annotation.Optio3RestEndpoint;
import com.optio3.cloud.authentication.WellKnownRoleIds;
import com.optio3.cloud.authentication.jwt.CookiePrincipalAccessor;
import com.optio3.cloud.hub.model.asset.GatewayProberOperation;
import com.optio3.cloud.hub.model.common.LogLine;
import com.optio3.cloud.hub.persistence.asset.GatewayAssetRecord;
import com.optio3.cloud.hub.persistence.prober.GatewayProberOperationRecord;
import com.optio3.cloud.model.TypedRecordIdentityList;
import com.optio3.cloud.model.ValidationResults;
import com.optio3.cloud.model.ValidationResultsHolder;
import com.optio3.cloud.persistence.LogEntryFilterRequest;
import com.optio3.cloud.persistence.LogRange;
import com.optio3.cloud.persistence.ModelMapper;
import com.optio3.cloud.persistence.ModelMapperPolicy;
import com.optio3.cloud.persistence.RecordHelper;
import com.optio3.cloud.persistence.SessionHolder;
import com.optio3.cloud.persistence.SessionProvider;
import com.optio3.logging.Severity;
import io.swagger.annotations.Api;

@Api(tags = { "GatewayProberOperations" }) // For Swagger
@Optio3RestEndpoint(name = "GatewayProberOperations") // For Optio3 Shell
@Path("/v1/gateway-prober-operations")
public class GatewayProberOperations
{
    @Optio3Dao
    private SessionProvider m_sessionProvider;

    @Optio3Principal
    private CookiePrincipalAccessor m_principalAccessor;

    @GET
    @Path("all/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public TypedRecordIdentityList<GatewayProberOperationRecord> getAll(@PathParam("id") String id)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayAssetRecord rec_gateway = sessionHolder.getEntity(GatewayAssetRecord.class, id);

            return TypedRecordIdentityList.toList(rec_gateway.getOperations());
        }
    }

    @POST
    @Path("batch")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<GatewayProberOperation> getBatch(List<String> ids)
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            RecordHelper<GatewayProberOperationRecord> helper = sessionHolder.createHelper(GatewayProberOperationRecord.class);

            return ModelMapper.toModels(sessionHolder, ModelMapperPolicy.Default, GatewayProberOperationRecord.getBatch(helper, ids));
        }
    }

    @GET
    @Path("item/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public GatewayProberOperation get(@PathParam("id") String id)
    {
        return ModelMapper.toModel(m_sessionProvider, ModelMapperPolicy.Default, GatewayProberOperationRecord.class, id);
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
            validation.checkAnyRoles(m_principalAccessor, WellKnownRoleIds.Maintenance);

            RecordHelper<GatewayProberOperationRecord> helper = validation.sessionHolder.createHelper(GatewayProberOperationRecord.class);

            GatewayProberOperationRecord rec = helper.getOrNull(id);
            if (rec != null)
            {
                rec.remove(validation, helper);
            }

            return validation.getResults();
        }
    }

    //--//

    @POST
    @Path("item/{id}/log/filter")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogRange> filterLog(@PathParam("id") String id,
                                    LogEntryFilterRequest filters) throws
                                                                   Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayProberOperationRecord rec = sessionHolder.getEntity(GatewayProberOperationRecord.class, id);

            try (var logHandler = GatewayProberOperationRecord.allocateLogHandler(sessionHolder, rec))
            {
                return logHandler.filter(filters);
            }
        }
    }

    @GET
    @Path("item/{id}/log")
    @Produces(MediaType.APPLICATION_JSON)
    @Optio3RequestLogLevel(Severity.Debug)
    public List<LogLine> getLog(@PathParam("id") String id,
                                @QueryParam("fromOffset") Integer fromOffset,
                                @QueryParam("toOffset") Integer toOffset,
                                @QueryParam("limit") Integer limit) throws
                                                                    Exception
    {
        try (SessionHolder sessionHolder = m_sessionProvider.newReadOnlySession())
        {
            GatewayProberOperationRecord rec = sessionHolder.getEntity(GatewayProberOperationRecord.class, id);

            List<LogLine> lines = Lists.newArrayList();

            try (var logHandler = GatewayProberOperationRecord.allocateLogHandler(sessionHolder, rec))
            {
                logHandler.extract(fromOffset, toOffset, limit, (item, offset) ->
                {
                    LogLine newLine = new LogLine();
                    newLine.lineNumber = offset;
                    newLine.copyFrom(item);
                    lines.add(newLine);
                });
            }

            return lines;
        }
    }
}
